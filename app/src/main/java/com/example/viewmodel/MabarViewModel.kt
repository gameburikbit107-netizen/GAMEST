package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String,
    val senderName: String,
    val messageText: String,
    val isSystem: Boolean = false,
    val isDonation: Boolean = false,
    val donationAmount: Long = 0L,
    val donationGift: String = "",
    val isStreamer: Boolean = false,
    val isPremiumUser: Boolean = false
)

class MabarViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MabarRepository(application)
    private val TAG = "MabarViewModel"

    // Flows from database
    val searchHistory: StateFlow<List<SearchHistory>> = repository.searchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wallet: StateFlow<UserWallet?> = repository.walletFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val donations: StateFlow<List<DonationEvent>> = repository.donations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val streams: StateFlow<List<StreamChannel>> = repository.streams
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state
    private val _currentStream = MutableStateFlow<StreamChannel?>(null)
    val currentStream: StateFlow<StreamChannel?> = _currentStream.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Semua")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Live chat simulator
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    // Top alert for donations
    private val _activeAlert = MutableStateFlow<String?>(null)
    val activeAlert: StateFlow<String?> = _activeAlert.asStateFlow()

    // User's own streaming state
    private val _isUserStreaming = MutableStateFlow(false)
    val isUserStreaming: StateFlow<Boolean> = _isUserStreaming.asStateFlow()

    private val _ownStreamTitle = MutableStateFlow("")
    val ownStreamTitle: StateFlow<String> = _ownStreamTitle.asStateFlow()

    private val _ownStreamGame = MutableStateFlow("Mobile Legends")
    val ownStreamGame: StateFlow<String> = _ownStreamGame.asStateFlow()

    private val _ownStreamViewers = MutableStateFlow(0)
    val ownStreamViewers: StateFlow<Int> = _ownStreamViewers.asStateFlow()

    private val _ownStreamLikes = MutableStateFlow(0)
    val ownStreamLikes: StateFlow<Int> = _ownStreamLikes.asStateFlow()

    private val _ownStreamDuration = MutableStateFlow(0L) // in seconds
    val ownStreamDuration: StateFlow<Long> = _ownStreamDuration.asStateFlow()

    private val _receivedDonationsTotal = MutableStateFlow(0L)
    val receivedDonationsTotal: StateFlow<Long> = _receivedDonationsTotal.asStateFlow()

    private var chatSimulatorJob: Job? = null
    private var ownStreamTimerJob: Job? = null
    private var ownStreamViewersJob: Job? = null

    init {
        viewModelScope.launch {
            repository.initDefaultData()
        }
    }

    fun setSearching(searching: Boolean) {
        _isSearching.value = searching
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun addSearchQueryToHistory(query: String) {
        viewModelScope.launch {
            repository.addSearchEntry(query)
        }
    }

    fun deleteSearchHistoryItem(id: Int) {
        viewModelScope.launch {
            repository.removeSearchEntry(id)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
        }
    }

    // Top up wallet simulation
    fun topUpWallet(amount: Long) {
        viewModelScope.launch {
            repository.topUpWallet(amount)
        }
    }

    // Upgrade to Premium simulation
    fun upgradeToPremium(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val success = repository.purchasePremium()
            if (success) {
                onSuccess()
            } else {
                onError("Saldo Wallet tidak mencukupi untuk upgrade (Butuh Rp 49.000). Silakan melakukan Top Up terlebih dahulu!")
            }
        }
    }

    fun cancelPremiumSubscription() {
        viewModelScope.launch {
            repository.cancelPremium()
        }
    }

    // Join / Select Stream
    fun selectStream(stream: StreamChannel?) {
        _currentStream.value = stream
        _chatMessages.value = emptyList()
        _activeAlert.value = null
        chatSimulatorJob?.cancel()

        if (stream != null) {
            startChatSimulation(stream)
        }
    }

    // Donation Simulation Action
    fun simulateDonation(giftName: String, amount: Long, message: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val stream = _currentStream.value ?: return
        viewModelScope.launch {
            val success = repository.doDonation(stream.streamerName, giftName, amount, message)
            if (success) {
                onSuccess()
                triggerDonationVisuals(stream, giftName, amount, message)
            } else {
                onError("Saldo Wallet tidak mencukupi! Silakan Top Up terlebih dahulu.")
            }
        }
    }

    private fun triggerDonationVisuals(stream: StreamChannel, giftName: String, amount: Long, message: String) {
        // Increment visual likes/viewers on active screen slightly when donating
        _currentStream.value = stream.copy(likes = stream.likes + 250)

        // Add Donation system message to Chat
        val sender = wallet.value?.username ?: "GamerPro"
        val donMessage = ChatMessage(
            id = "don_${System.currentTimeMillis()}",
            senderName = sender,
            messageText = message,
            isDonation = true,
            donationAmount = amount,
            donationGift = giftName,
            isPremiumUser = wallet.value?.isPremium ?: false
        )
        _chatMessages.value = _chatMessages.value + donMessage

        // Trigger Overlay Alert
        _activeAlert.value = "🎉 $sender mengirim $giftName senilai Rp ${formatDecimal(amount)}!\n\"$message\""

        // Auto clear alert after 5s
        viewModelScope.launch {
            delay(5000)
            if (_activeAlert.value?.contains(sender) == true) {
                _activeAlert.value = null
            }
        }

        // Trigger Streamer reaction via Gemini in Chat
        viewModelScope.launch {
            delay(1500)
            val readingMsg = ChatMessage(
                id = "reading_${System.currentTimeMillis()}",
                senderName = stream.streamerName,
                messageText = "...sedang membaca donasi...",
                isStreamer = true
            )
            _chatMessages.value = _chatMessages.value + readingMsg

            val response = GeminiService.getStreamerReaction(
                streamerName = stream.streamerName,
                gameName = stream.gameName,
                userMessage = "",
                userDonationAmount = amount,
                userDonationMessage = message
            )

            // Replace readingMsg with actual response
            _chatMessages.value = _chatMessages.value.filter { it.id != readingMsg.id } + ChatMessage(
                id = "resp_${System.currentTimeMillis()}",
                senderName = stream.streamerName,
                messageText = response,
                isStreamer = true
            )
        }
    }

    // User sends chat message
    fun sendUserChatMessage(text: String) {
        val stream = _currentStream.value ?: return
        if (text.isBlank()) return

        val user = wallet.value?.username ?: "GamerGanteng"
        val userMsgObj = ChatMessage(
            id = "user_${System.currentTimeMillis()}",
            senderName = user,
            messageText = text,
            isPremiumUser = wallet.value?.isPremium ?: false
        )

        _chatMessages.value = _chatMessages.value + userMsgObj

        // Call Gemini response
        viewModelScope.launch {
            delay(1200)
            val readingMsg = ChatMessage(
                id = "read_${System.currentTimeMillis()}",
                senderName = stream.streamerName,
                messageText = "...sedang membaca chat...",
                isStreamer = true
            )
            _chatMessages.value = _chatMessages.value + readingMsg

            val response = GeminiService.getStreamerReaction(
                streamerName = stream.streamerName,
                gameName = stream.gameName,
                userMessage = text
            )

            _chatMessages.value = _chatMessages.value.filter { it.id != readingMsg.id } + ChatMessage(
                id = "resp_${System.currentTimeMillis()}",
                senderName = stream.streamerName,
                messageText = response,
                isStreamer = true
            )
        }
    }

    // Interactive Stream Chat Simulator
    private fun startChatSimulation(stream: StreamChannel) {
        val userNames = listOf(
            "bocil_kematian", "mario_mlbb", "lord_fajar", "anti_banned", "gita_esport",
            "gaming_boy", "putra_id", "sultan_mabar", "wibu_gaming", "pro_player",
            "gacor_99", "kang_siaran", "syahmi_gg", "mlbb_lovers", "gaby_cantik"
        )

        val comments = listOf(
            "GG banget mainnya bang!! 👍",
            "Mabar yuk bang, rank Epic buntu nih",
            "Gila sih item buildnya sakit bgt wkwk",
            "Bang coba liat chat dong, sapa bocah Caheum",
            "Dewa judi emang gacor 💥💥",
            "Kok heronya bisa gitu sih bang?",
            "Live terus sampe pagi bang!!",
            "Mending push lord bro, udah menit 15",
            "Ramein gesss donasinya biar heboh!",
            "Lemon kok makin dingin aja hahah",
            "Wah RE9 rilis kapan sih gasabar maininnya",
            "Windah is the best streamer memang!",
            "Darah tipis kabur dulu bang!! 🦖💨",
            "Build full damage sabi nih bang",
            "Kamera muka aman gess"
        )

        val gifts = listOf(
            Pair("☕ Kopi Susu", 5000L),
            Pair("🍔 Nasi Goreng", 15000L),
            Pair("👑 Mahkota Emas", 50000L),
            Pair("🚀 Roket Esport", 100000L)
        )

        chatSimulatorJob = viewModelScope.launch {
            // Pre-fill some messages
            val prefilled = (1..6).map {
                ChatMessage(
                    id = "init_$it",
                    senderName = userNames.random(),
                    messageText = comments.random(),
                    isPremiumUser = (1..100).random() < 20
                )
            }
            _chatMessages.value = prefilled

            while (true) {
                delay((1500..3500).random().toLong())
                
                // 15% chance of simulated viewer donating!
                val isSimulatedDonation = (1..100).random() <= 15
                val newMessage = if (isSimulatedDonation) {
                    val donor = userNames.random()
                    val gift = gifts.random()
                    val msg = listOf(
                        "Semangat terus live-nya bang!",
                        "Support dikit nih biar makin gila mainnya!",
                        "Sehat selalu bang Windah/Lemon!",
                        "Gaspol terus, mabar gass",
                        "Lumayan buat beli ketoprak bang wkwk"
                    ).random()

                    // Add alert for simulated donation to make the stream feel alive
                    _activeAlert.value = "🎉 $donor mengirim ${gift.first} senilai Rp ${formatDecimal(gift.second)}!\n\"$msg\""
                    
                    viewModelScope.launch {
                        delay(4000)
                        if (_activeAlert.value?.contains(donor) == true) {
                            _activeAlert.value = null
                        }
                    }

                    // Auto-reply simulation from Streamer inside chat
                    val streamerReply = when (stream.streamerName) {
                        "Windah Basudara" -> "WADIDAW!! Makasih banyak $donor atas ${gift.first}-nya! Semoga berkah dan sehat selalu ya brader! Mantap jaya!"
                        "Lemon MLBB" -> "Eh makasih ya $donor buat ${gift.first}-nya. Thank you."
                        "MiawAug" -> "Wah makasih banyak ya kak $donor donasi ${gift.first}-nya. Senang banget dibantuin beli pakan kucing mew, thank you!"
                        else -> "Terima kasih banyak $donor untuk ${gift.first} nya ya! Support kalian berharga banget!"
                    }

                    viewModelScope.launch {
                        delay(2000)
                        _chatMessages.value = _chatMessages.value + ChatMessage(
                            id = "reply_${System.currentTimeMillis()}",
                            senderName = stream.streamerName,
                            messageText = streamerReply,
                            isStreamer = true
                        )
                    }

                    ChatMessage(
                        id = "msg_${System.currentTimeMillis()}",
                        senderName = donor,
                        messageText = msg,
                        isDonation = true,
                        donationAmount = gift.second,
                        donationGift = gift.first,
                        isPremiumUser = (1..100).random() < 30
                    )
                } else {
                    ChatMessage(
                        id = "msg_${System.currentTimeMillis()}",
                        senderName = userNames.random(),
                        messageText = comments.random(),
                        isPremiumUser = (1..100).random() < 15
                    )
                }

                // Append and keep max 50 chat messages in history to save RAM
                _chatMessages.value = (_chatMessages.value + newMessage).takeLast(50)
            }
        }
    }

    // --- Broadcaster Studio Mode ---
    fun startUserStream(title: String, game: String, category: String) {
        _ownStreamTitle.value = title.ifBlank { "Mabar Gameplay Live Pro!" }
        _ownStreamGame.value = game
        _isUserStreaming.value = true
        _ownStreamViewers.value = 5
        _ownStreamLikes.value = 10
        _ownStreamDuration.value = 0L
        _receivedDonationsTotal.value = 0L
        _chatMessages.value = listOf(
            ChatMessage(
                id = "sys_start",
                senderName = "GAMEST System",
                messageText = "Siaran Anda telah dimulai! Menghubungkan ke server streaming...",
                isSystem = true
            )
        )

        // Start timer
        ownStreamTimerJob = viewModelScope.launch {
            while (_isUserStreaming.value) {
                delay(1000)
                _ownStreamDuration.value = _ownStreamDuration.value + 1
            }
        }

        // Start viewer inflation / simulator
        ownStreamViewersJob = viewModelScope.launch {
            val userNames = listOf(
                "raja_frags", "bocilML", "savage_queen", "anti_lose", "pro_esport",
                "gacor_slot", "mas_gaming", "gita_geming", "windah_clone", "lemon_sub"
            )
            val gamerTalks = listOf(
                "Gila sih skillnya jago bgt abang ini! 🔥",
                "Mabar dong bang, open room gabung!",
                "Device pake hp apa bang kok lancar bgt?",
                "Savagein bang buruan!",
                "Wih game kesukaan gua nih digasin",
                "Lag gak bang jaringannya?",
                "GG WP kawan-kawan!",
                "Goks lah push terus",
                "Mainnya rapih bener gilakk"
            )
            val gifts = listOf(
                Pair("☕ Kopi", 3000L),
                Pair("🍦 Es Krim", 8000L),
                Pair("🍕 Pizza Mabar", 25000L),
                Pair("🚀 Roket Gaming", 120000L)
            )

            // Register custom stream in Room so it's searchable!
            viewModelScope.launch {
                repository.startOwnStream(
                    title = _ownStreamTitle.value,
                    game = _ownStreamGame.value,
                    category = category,
                    streamerName = wallet.value?.username ?: "AsepGaming"
                )
            }

            while (_isUserStreaming.value) {
                delay((2000..4500).random().toLong())

                // Randomly increase viewers & likes
                _ownStreamViewers.value = _ownStreamViewers.value + (-2..6).random()
                if (_ownStreamViewers.value < 0) _ownStreamViewers.value = 1
                _ownStreamLikes.value = _ownStreamLikes.value + (5..20).random()

                // Update database
                viewModelScope.launch {
                    repository.updateStreamStats(
                        "user_custom_stream",
                        _ownStreamViewers.value,
                        _ownStreamLikes.value
                    )
                }

                // Random chats
                val isDonation = (1..100).random() <= 20 // 20% donation chance for the user!
                val chat = if (isDonation) {
                    val donor = userNames.random()
                    val gift = gifts.random()
                    val message = "Mantap livenya bro! Support dikit"
                    
                    _receivedDonationsTotal.value = _receivedDonationsTotal.value + gift.second
                    _activeAlert.value = "💸 $donor mendonasikan ${gift.first} senilai Rp ${formatDecimal(gift.second)} ke Anda!"
                    
                    viewModelScope.launch {
                        delay(4000)
                        if (_activeAlert.value?.contains(donor) == true) {
                            _activeAlert.value = null
                        }
                    }

                    ChatMessage(
                        id = "user_don_${System.currentTimeMillis()}",
                        senderName = donor,
                        messageText = "mendonasikan ${gift.first} Rp ${formatDecimal(gift.second)}: \"$message\"",
                        isDonation = true,
                        donationAmount = gift.second,
                        donationGift = gift.first
                    )
                } else {
                    ChatMessage(
                        id = "user_chat_${System.currentTimeMillis()}",
                        senderName = userNames.random(),
                        messageText = gamerTalks.random()
                    )
                }

                _chatMessages.value = (_chatMessages.value + chat).takeLast(40)
            }
        }
    }

    // Streamer replies or sends a chat message in Broadcaster mode
    fun sendBroadcasterMessage(text: String, replyToUser: String? = null) {
        if (text.isBlank()) return
        val streamerName = wallet.value?.username ?: "AsepGaming"
        
        val messageText = if (replyToUser != null) {
            "Membalas @$replyToUser: $text"
        } else {
            text
        }
        
        val newMsg = ChatMessage(
            id = "broadcaster_${System.currentTimeMillis()}",
            senderName = streamerName,
            messageText = messageText,
            isStreamer = true
        )
        
        _chatMessages.value = (_chatMessages.value + newMsg).takeLast(50)
        
        // Simulasikan balasan penonton jika me-reply user spesifik!
        if (replyToUser != null) {
            viewModelScope.launch {
                delay(1500)
                val reactiveReplies = listOf(
                    "WADIDAW!! Dibales bang $streamerName! Seneng bgt gilaaa 😂🔥",
                    "Anjaaay dibales! Sukses terus livenya bang!",
                    "Wah dapet respon dari idola, otw share livenya gess!",
                    "Oalah gitu ya bang, mantap infonya. Thank you!",
                    "Wih disapa streamer kece! Sering-sering lakuin mabar ya bang!"
                )
                val responseMsg = ChatMessage(
                    id = "viewer_react_${System.currentTimeMillis()}",
                    senderName = replyToUser,
                    messageText = reactiveReplies.random()
                )
                _chatMessages.value = (_chatMessages.value + responseMsg).takeLast(50)
            }
        }
    }

    fun stopUserStream() {
        _isUserStreaming.value = false
        chatSimulatorJob?.cancel()
        ownStreamTimerJob?.cancel()
        ownStreamViewersJob?.cancel()
        viewModelScope.launch {
            repository.stopOwnStream()
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatSimulatorJob?.cancel()
        ownStreamTimerJob?.cancel()
        ownStreamViewersJob?.cancel()
    }

    // Money formatter
    fun formatDecimal(num: Long): String {
        return java.text.NumberFormat.getIntegerInstance().format(num)
    }
}
