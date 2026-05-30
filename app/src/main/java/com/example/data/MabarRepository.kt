package com.example.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MabarRepository(context: Context) {
    private val db: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "mabar_database"
    ).fallbackToDestructiveMigration().build()

    private val dao = db.mabarDao()

    val searchHistory: Flow<List<SearchHistory>> = dao.getSearchHistory()
    val walletFlow: Flow<UserWallet?> = dao.getWalletFlow()
    val donations: Flow<List<DonationEvent>> = dao.getDonations()
    val streams: Flow<List<StreamChannel>> = dao.getAllStreams()

    suspend fun initDefaultData() = withContext(Dispatchers.IO) {
        // Init wallet if empty
        val currentWallet = dao.getWalletDirect()
        if (currentWallet == null) {
            dao.updateWallet(UserWallet(id = 1, username = "GamerPro99", balance = 500000L))
        }

        // Init preset streamers if empty
        val count = dao.getPresetStreamsCount()
        if (count == 0) {
            val presets = listOf(
                StreamChannel(
                    id = "stream_windah",
                    streamerName = "Windah Basudara",
                    streamTitle = "MENGALAHKAN BOS TERKUAT DI GAME INI! LIVE SEKARANG!! 🦖💥",
                    gameName = "GTA V / Retro",
                    category = "GTA V",
                    viewerCount = 38900,
                    likes = 145000,
                    avatarColorHex = "#FF4500"
                ),
                StreamChannel(
                    id = "stream_lemon",
                    streamerName = "Lemon MLBB",
                    streamTitle = "SOLO RANK PAKE HERO MARKSAMN BURIK JADI META S100! 🍋",
                    gameName = "Mobile Legends",
                    category = "Mobile Legends",
                    viewerCount = 12400,
                    likes = 56200,
                    avatarColorHex = "#FFD700"
                ),
                StreamChannel(
                    id = "stream_jess",
                    streamerName = "JessNoLimit",
                    streamTitle = "BAGI-BAGI SKIN SKIN MYTHIC DAN HP LIVE SEKARANG UNTUK VIEWER! 💎",
                    gameName = "Mobile Legends",
                    category = "Mobile Legends",
                    viewerCount = 24100,
                    likes = 189000,
                    avatarColorHex = "#FF1493"
                ),
                StreamChannel(
                    id = "stream_miaw",
                    streamerName = "MiawAug",
                    streamTitle = "RESIDENT EVIL CO-OP BARENG SELLER NYA! SEREM LUCU!! 🐈💀",
                    gameName = "Resident Evil",
                    category = "Horror",
                    viewerCount = 15300,
                    likes = 94000,
                    avatarColorHex = "#1E90FF"
                ),
                StreamChannel(
                    id = "stream_luthfi",
                    streamerName = "Luthfi Halimawan",
                    streamTitle = "REAKSI MEME VIEWER INDONESIA YANG SANGAT BIJAKSANA! 🤣🗿",
                    gameName = "Reacting / Meme",
                    category = "Meme",
                    viewerCount = 9800,
                    likes = 45000,
                    avatarColorHex = "#32CD32"
                ),
                StreamChannel(
                    id = "stream_tara",
                    streamerName = "Tara Arts",
                    streamTitle = "GAMEPLAY DETROITS BECOME HUMAN VERSI CAKUNG CAWAN! 🎭🎲",
                    gameName = "Detroits",
                    category = "Story",
                    viewerCount = 4200,
                    likes = 21000,
                    avatarColorHex = "#8A2BE2"
                )
            )
            for (stream in presets) {
                dao.insertStream(stream)
            }
        }
    }

    suspend fun addSearchEntry(query: String) {
        if (query.isNotBlank()) {
            dao.insertSearch(SearchHistory(queryText = query.trim()))
        }
    }

    suspend fun removeSearchEntry(id: Int) {
        dao.deleteSearchById(id)
    }

    suspend fun clearSearchHistory() {
        dao.clearSearchHistory()
    }

    suspend fun doDonation(streamerName: String, giftType: String, amount: Long, message: String): Boolean {
        val wallet = dao.getWalletDirect() ?: UserWallet()
        if (wallet.balance >= amount) {
            val newBalance = wallet.balance - amount
            dao.updateWallet(wallet.copy(balance = newBalance))
            dao.insertDonation(
                DonationEvent(
                    streamerName = streamerName,
                    donorName = wallet.username,
                    giftType = giftType,
                    amount = amount,
                    message = message
                )
            )
            return true
        }
        return false
    }

    suspend fun topUpWallet(amount: Long) {
        val wallet = dao.getWalletDirect() ?: UserWallet()
        dao.updateWallet(wallet.copy(balance = wallet.balance + amount))
    }

    suspend fun purchasePremium(): Boolean {
        val wallet = dao.getWalletDirect() ?: UserWallet()
        val cost = 49000L // 49k IDR per month or static upgrade
        if (wallet.balance >= cost) {
            val newBalance = wallet.balance - cost
            dao.updateWallet(wallet.copy(balance = newBalance, isPremium = true))
            return true
        }
        return false
    }

    suspend fun cancelPremium() {
        val wallet = dao.getWalletDirect() ?: UserWallet()
        dao.updateWallet(wallet.copy(isPremium = false))
    }

    suspend fun startOwnStream(title: String, game: String, category: String, streamerName: String): StreamChannel {
        // Delete previous custom stream if any
        dao.deleteCustomUserStreams()
        
        val customStream = StreamChannel(
            id = "user_custom_stream",
            streamerName = streamerName,
            streamTitle = title,
            gameName = game,
            category = category,
            viewerCount = 0,
            likes = 0,
            avatarColorHex = "#00FFFF",
            isCustomUserStream = true
        )
        dao.insertStream(customStream)
        return customStream
    }

    suspend fun updateStreamStats(streamId: String, viewers: Int, likes: Int) {
        val stream = dao.getStreamById(streamId)
        if (stream != null) {
            dao.insertStream(stream.copy(viewerCount = viewers, likes = likes))
        }
    }

    suspend fun stopOwnStream() {
        dao.deleteCustomUserStreams()
    }
}
