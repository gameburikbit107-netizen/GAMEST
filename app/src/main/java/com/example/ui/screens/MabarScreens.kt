package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.SearchHistory
import com.example.data.StreamChannel
import com.example.ui.theme.*
import com.example.viewmodel.ChatMessage
import com.example.viewmodel.MabarViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Screen navigation states
sealed class Screen {
    object Explore : Screen()
    data class LiveStream(val streamId: String) : Screen()
    object BroadcasterStudio : Screen()
    object Wallet : Screen()
}

@Composable
fun MabarMainApp(
    viewModel: MabarViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Explore) }
    
    // Back navigation safety
    val onBack: () -> Unit = {
        currentScreen = when (currentScreen) {
            is Screen.LiveStream -> {
                viewModel.selectStream(null)
                Screen.Explore
            }
            Screen.BroadcasterStudio -> {
                viewModel.stopUserStream()
                Screen.Explore
            }
            Screen.Wallet -> Screen.Explore
            Screen.Explore -> Screen.Explore
        }
    }

    Box(modifier = modifier.fillMaxSize().background(SlateBackground)) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
            },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                is Screen.Explore -> ExploreScreen(
                    viewModel = viewModel,
                    onNavigateToStream = { id ->
                        val matchedStream = viewModel.streams.value.find { it.id == id }
                        viewModel.selectStream(matchedStream)
                        currentScreen = Screen.LiveStream(id)
                    },
                    onNavigateToStudio = { currentScreen = Screen.BroadcasterStudio },
                    onNavigateToWallet = { currentScreen = Screen.Wallet }
                )
                is Screen.LiveStream -> LivePlayerScreen(
                    viewModel = viewModel,
                    onBack = onBack
                )
                is Screen.BroadcasterStudio -> BroadcasterScreen(
                    viewModel = viewModel,
                    onBack = onBack
                )
                is Screen.Wallet -> WalletScreen(
                    viewModel = viewModel,
                    onBack = onBack
                )
            }
        }
    }
}

// 1. EXPLORE SCREEN (GAMING HUB)
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: MabarViewModel,
    onNavigateToStream: (String) -> Unit,
    onNavigateToStudio: () -> Unit,
    onNavigateToWallet: () -> Unit
) {
    val streams by viewModel.streams.collectAsStateWithLifecycle()
    val wallet by viewModel.wallet.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearchingMode by viewModel.isSearching.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    val keyboardController = LocalSoftwareKeyboardController.current

    val categories = listOf("Semua", "Mobile Legends", "PUBG Mobile", "Minecraft", "GTA V", "Horror", "Meme")

    // Filtered streaming list matching searching filter and tags category
    val filteredStreams = streams.filter { stream ->
        val matchTag = if (selectedCategory == "Semua") true else stream.category.contains(selectedCategory, ignoreCase = true) || stream.gameName.contains(selectedCategory, ignoreCase = true)
        val matchSearch = stream.streamerName.contains(searchQuery, ignoreCase = true) ||
                stream.streamTitle.contains(searchQuery, ignoreCase = true) ||
                stream.gameName.contains(searchQuery, ignoreCase = true)
        matchTag && matchSearch
    }

    Scaffold(
        containerColor = SlateBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateBackground)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header with App Identity Logo and Wallet Card right off the top!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.SportsEsports,
                                contentDescription = "Esports Logo",
                                tint = CyberPink,
                                modifier = Modifier.size(28.dp).padding(end = 4.dp)
                            )
                            Text(
                                text = "GAMES",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "T",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp,
                                    color = CyberCyan
                                )
                            )
                        }
                        Text(
                            text = "Game streaming hub Indonesia 🇮🇩",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    // Dynamic Wallet chip that goes to top up
                    Card(
                        onClick = onNavigateToWallet,
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier.testTag("wallet_top_bar")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccountBalanceWallet,
                                contentDescription = "Wallet",
                                tint = AccentGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Rp ${viewModel.formatDecimal(wallet?.balance ?: 0L)}",
                                fontSize = 12.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.AddCircle,
                                contentDescription = "Top Up",
                                tint = CyberCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Modern Search Field with suggestion interactions
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        viewModel.updateSearchQuery(it)
                        viewModel.setSearching(true)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_field"),
                    placeholder = { Text("Cari streamer, game, atau judul...", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari", tint = TextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty() || isSearchingMode) {
                            IconButton(onClick = {
                                viewModel.updateSearchQuery("")
                                viewModel.setSearching(false)
                                keyboardController?.hide()
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Format", tint = TextPrimary)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = LightSurface,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (searchQuery.isNotBlank()) {
                            viewModel.addSearchQueryToHistory(searchQuery)
                        }
                        viewModel.setSearching(false)
                        keyboardController?.hide()
                    })
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isSearchingMode) {
                // Persistent Search History UI overlay
                SearchSuggestLayout(
                    history = searchHistory,
                    onSelect = { query ->
                        viewModel.updateSearchQuery(query)
                        viewModel.setSearching(false)
                        keyboardController?.hide()
                    },
                    onDelete = { id -> viewModel.deleteSearchHistoryItem(id) },
                    onClearAll = { viewModel.clearSearchHistory() },
                    onClose = { viewModel.setSearching(false) }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp) // Offset for bottom float triggers
                ) {
                    // Category selector block
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(categories) { cat ->
                                val isSel = selectedCategory == cat
                                FilterChip(
                                    selected = isSel,
                                    onClick = { viewModel.selectCategory(cat) },
                                    label = { Text(cat, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberPurple,
                                        selectedLabelColor = TextPrimary,
                                        containerColor = DarkSurface,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSel,
                                        borderColor = LightSurface,
                                        selectedBorderColor = CyberCyan,
                                        borderWidth = 1.dp
                                    )
                                )
                            }
                        }
                    }

                    // "Sedang Populer" Horizontal scroll carousel of major Indonesian streamers
                    if (searchQuery.isEmpty() && selectedCategory == "Semua") {
                        item {
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = "🔥 Live Populer Indonesia",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )

                                val populars = streams.take(3)
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(populars) { stream ->
                                        BigStreamCarouselCard(
                                            stream = stream,
                                            onCardClick = { onNavigateToStream(stream.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Directory lists title
                    item {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Hasil Pencarian" else "Semua Siaran Live",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                        )
                    }

                    // If filtered empty, show beautiful empty state!
                    if (filteredStreams.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideogameAssetOff,
                                    contentDescription = "Empty",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(60.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Tidak ada siaran live ditemukan",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Coba gunakan kata kunci pencarian lain atau pilih kategori yang berbeda.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    } else {
                        // All remaining lists
                        items(filteredStreams) { stream ->
                            StreamItemCard(
                                stream = stream,
                                onCardClick = { onNavigateToStream(stream.id) }
                            )
                        }
                    }
                }
            }

            // High-End Floating Action Button to start user's own broadcast!
            ExtendedFloatingActionButton(
                onClick = onNavigateToStudio,
                containerColor = CyberPink,
                contentColor = TextPrimary,
                shape = RoundedCornerShape(24.dp),
                icon = { Icon(Icons.Filled.LiveTv, contentDescription = "Siarkan") },
                text = { Text("Mulai Siaran Anda", fontWeight = FontWeight.Bold) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("start_broadcasting_fab")
            )
        }
    }
}

// 2. SEARCH SUGGEST LAYOUT (SEARCH SCREEN MODE)
@Composable
fun SearchSuggestLayout(
    history: List<SearchHistory>,
    onSelect: (String) -> Unit,
    onDelete: (Int) -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = SlateBackground,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Riwayat Pencarian Anda",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (history.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("Hapus Semua", color = CyberPink, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (history.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada pencarian baru-baru ini.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(history) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(item.queryText) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "History",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = item.queryText,
                                    color = TextPrimary,
                                    fontSize = 14.sp
                                )
                            }
                            IconButton(
                                onClick = { onDelete(item.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete Item",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = LightSurface, thickness = 0.5.dp)
                    }
                }
            }

            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = LightSurface),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Tutup Pencarian", color = TextPrimary)
            }
        }
    }
}

// 3. LIVE STREAM VISUAL CAROUSEL COMPONENETS
@Composable
fun BigStreamCarouselCard(
    stream: StreamChannel,
    onCardClick: () -> Unit
) {
    Card(
        onClick = onCardClick,
        modifier = Modifier
            .width(280.dp)
            .height(160.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Simulated game banner gradient visual
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(android.graphics.Color.parseColor(stream.avatarColorHex)).copy(alpha = 0.5f),
                                SlateBackground
                            )
                        )
                        drawRect(brush = brush)
                    }
            )

            // Live badge top corner left, viewer count top right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(LiveRed)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "LIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RemoveRedEye,
                            contentDescription = "Viewer",
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${stream.viewerCount / 1000}K",
                            fontSize = 9.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Bottom title metadata
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = stream.streamTitle,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(stream.avatarColorHex)))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stream.streamerName,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun StreamItemCard(
    stream: StreamChannel,
    onCardClick: () -> Unit
) {
    Card(
        onClick = onCardClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(0.5.dp, LightSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colorful profile game badge container
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .drawBehind {
                        val brush = Brush.linearGradient(
                            colors = listOf(
                                Color(android.graphics.Color.parseColor(stream.avatarColorHex)),
                                SlateBackground
                            )
                        )
                        drawRect(brush = brush)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SportsEsports,
                    contentDescription = "Game",
                    tint = TextPrimary.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Information block
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stream.gameName,
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // LIVE pulsing badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(LiveRed)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${java.text.NumberFormat.getIntegerInstance().format(stream.viewerCount)} nonton",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stream.streamTitle,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(stream.avatarColorHex)))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stream.streamerName,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// 4. ACTIVE LIVE PLAYS SCREEN (PLAYER SIMULATOR)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivePlayerScreen(
    viewModel: MabarViewModel,
    onBack: () -> Unit
) {
    val stream by viewModel.currentStream.collectAsStateWithLifecycle()
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val activeAlert by viewModel.activeAlert.collectAsStateWithLifecycle()
    val wallet by viewModel.wallet.collectAsStateWithLifecycle()

    var showDonateSheet by remember { mutableStateOf(false) }
    var chatInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    if (stream == null) {
        onBack()
        return
    }

    Scaffold(
        containerColor = SlateBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(stream!!.avatarColorHex)))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(stream!!.streamerName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(stream!!.gameName, fontSize = 11.sp, color = CyberCyan)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Stream Video box (the simulated gaming frame)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                // RUN THE CANVAS SIMULATED GAMEPLAY
                SimulatedGameStream(stream!!.gameName)

                // Flash overlay alert if donation made
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .align(Alignment.TopCenter)
                ) {
                    AnimatedVisibility(
                        visible = activeAlert != null,
                        enter = fadeIn() + slideInVertically { -20 },
                        exit = fadeOut() + slideOutVertically { -20 }
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CyberPink.copy(alpha = 0.9f)),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Stars,
                                    contentDescription = "Alert Spark",
                                    tint = AccentGold,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = activeAlert ?: "",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Top Floating Badge: Live, view count, and likes tracker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .align(Alignment.BottomStart),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(LiveRed)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.RemoveRedEye, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${viewModel.formatDecimal(stream!!.viewerCount.toLong())} penonton", color = Color.White, fontSize = 9.sp)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = CyberPink, modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${viewModel.formatDecimal(stream!!.likes.toLong())} suka", color = Color.White, fontSize = 9.sp)
                        }
                    }
                }
            }

            // Stream Title bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(12.dp)
            ) {
                Text(
                    text = stream!!.streamTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // SPONSOR ADS BANNER (Disabled for PREMIUM)
            val isPremiumUser = wallet?.isPremium == true
            if (!isPremiumUser) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("sponsor_ad_banner"),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, LightSurface.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .background(AccentGold.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "SPONSOR",
                                    color = AccentGold,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Mulai belanja di GAMEST Cup & Jaket Esport Diskon 50%!",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("premium_enabled_banner"),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("👑 ", fontSize = 12.sp)
                            Text(
                                "Fitur Premium Aktif: Bebas Iklan & Diskon Donasi 20% Terpasang!",
                                color = AccentGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // LIVE INTERACTIVE CHAT SCREEN
            Surface(
                color = SlateBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val listState = rememberLazyListState()
                
                // AutoScroll to latest messages
                LaunchedEffect(messages.size) {
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { msg ->
                        ChatBubbleRow(msg) { targetMsg ->
                            if (!targetMsg.isSystem) {
                                chatInput = "Balas @${targetMsg.senderName} "
                            }
                        }
                    }
                }
            }

            // Control panel: write chat or open donate panel!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .navigationBarsPadding()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Donate visual trigger
                IconButton(
                    onClick = { showDonateSheet = true },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CyberPink)
                        .testTag("donate_button_trigger")
                ) {
                    Icon(
                        imageVector = Icons.Filled.CardGiftcard,
                        contentDescription = "Kirim Donasi",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { chatInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    placeholder = { Text("Ketik pesan chat...", color = TextSecondary, fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = LightSurface,
                        focusedContainerColor = SlateBackground,
                        unfocusedContainerColor = SlateBackground,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(20.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (chatInput.isNotBlank()) {
                            viewModel.sendUserChatMessage(chatInput)
                            chatInput = ""
                            keyboardController?.hide()
                        }
                    })
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (chatInput.isNotBlank()) {
                            viewModel.sendUserChatMessage(chatInput)
                            chatInput = ""
                            keyboardController?.hide()
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CyberCyan)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Kirim", tint = SlateBackground, modifier = Modifier.size(18.dp))
                }
            }
        }

        // SHEET OVERLAY: DONASI (SIMULATION DIALOG)
        if (showDonateSheet) {
            DonateOverlaySheet(
                viewModel = viewModel,
                streamerName = stream!!.streamerName,
                walletBalance = wallet?.balance ?: 0L,
                onDismiss = { showDonateSheet = false }
            )
        }
    }
}

// 5. CHAT BUBBLE RENDERING COMPONENT
@Composable
fun ChatBubbleRow(
    msg: ChatMessage,
    onReplyClick: ((ChatMessage) -> Unit)? = null
) {
    if (msg.isSystem) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberCyan.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = msg.messageText,
                    fontSize = 11.sp,
                    color = CyberCyan,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val clickableModifier = if (onReplyClick != null) {
        Modifier.clickable { onReplyClick(msg) }
    } else {
        Modifier
    }

    if (msg.isDonation) {
        // High Contrast donation card inside chat
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .then(clickableModifier),
            colors = CardDefaults.cardColors(containerColor = CyberPink.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, CyberPink.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Stars, contentDescription = "Gift", tint = AccentGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${msg.senderName} mendonasikan ${msg.donationGift}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "Rp ${java.text.NumberFormat.getIntegerInstance().format(msg.donationAmount)}",
                        fontWeight = FontWeight.ExtraBold,
                        color = AccentGold,
                        fontSize = 13.sp
                    )
                }
                if (msg.messageText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"${msg.messageText}\"",
                        fontSize = 12.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        return
    }

    // Default chat comment
    val borderStroke = if (msg.isPremiumUser) BorderStroke(1.dp, AccentGold.copy(alpha = 0.3f)) else null
    val backgroundBrush = if (msg.isPremiumUser) {
        Brush.horizontalGradient(listOf(AccentGold.copy(alpha = 0.08f), Color.Transparent))
    } else {
        SolidColor(Color.Transparent)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .background(backgroundBrush, RoundedCornerShape(4.dp))
            .let { 
                if (borderStroke != null) it.border(borderStroke, RoundedCornerShape(4.dp)) else it
            }
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (msg.isPremiumUser) {
            Text(
                text = "👑 ",
                fontSize = 11.sp,
                modifier = Modifier.padding(end = 2.dp)
            )
        }

        val nameColor = if (msg.isStreamer) {
            CyberPink
        } else if (msg.isPremiumUser) {
            AccentGold
        } else {
            CyberCyan
        }
        
        Text(
            text = "${msg.senderName}: ",
            color = nameColor,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            modifier = Modifier.padding(end = 4.dp)
        )

        Text(
            text = msg.messageText,
            color = if (msg.isStreamer) AccentGold else TextPrimary,
            fontSize = 13.sp,
            fontWeight = if (msg.isStreamer || msg.isPremiumUser) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// 6. DETAILED DONATE BOTTOM SIMULATION SHEET Overlay
@Composable
fun DonateOverlaySheet(
    viewModel: MabarViewModel,
    streamerName: String,
    walletBalance: Long,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedGiftIdx by remember { mutableStateOf(0) }
    var donationMessage by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var donationSuccessState by remember { mutableStateOf(false) }

    val giftsList = listOf(
        Triple("☕ Kopi Susu", 5000L, "Ekspresi: Terbantu bermain"),
        Triple("🍟 Nasi Goreng", 15000L, "Ekspresi: Semakin kenyang game"),
        Triple("👑 Mahkota Esports", 50000L, "Ekspresi: Sebut nama kencang!"),
        Triple("🚀 Roket Gaming", 120000L, "Ekspresi: Teriak Heboh Banget!"),
        Triple("🐉 Naga Terbang", 250000L, "Ekspresi: Heboh + Sebut Abadi")
    )

    Surface(
        color = Color.Black.copy(alpha = 0.75f),
        modifier = Modifier
            .fillMaxSize()
            .clickable { onDismiss() }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Sheet container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(DarkSurface)
                    .clickable(enabled = false) { }
                    .padding(20.dp)
                    .navigationBarsPadding()
            ) {
                // Pull bar
                Box(
                    modifier = Modifier
                        .size(40.dp, 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(LightSurface)
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kirim Donasi ke $streamerName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Wallet balance display
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(LightSurface)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saldo Wallet Anda:", color = TextSecondary, fontSize = 12.sp)
                    }
                    Text(
                        text = "Rp ${viewModel.formatDecimal(walletBalance)}",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Pilih Produk Donasi (Gift):", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(8.dp))

                // Lazy select cards of gifts
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(giftsList) { idx, gift ->
                        val isSel = selectedGiftIdx == idx
                        val borderColor = if (isSel) CyberPink else LightSurface
                        
                        Card(
                            onClick = { selectedGiftIdx = idx },
                            border = BorderStroke(1.5.dp, borderColor),
                            colors = CardDefaults.cardColors(containerColor = SlateBackground),
                            modifier = Modifier.width(110.dp).height(100.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(gift.first, fontSize = 18.sp, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Rp ${viewModel.formatDecimal(gift.second)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGold
                                )
                                Text(
                                    text = gift.third,
                                    fontSize = 8.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom message field
                OutlinedTextField(
                    value = donationMessage,
                    onValueChange = { donationMessage = it },
                    placeholder = { Text("Tulis pesan dukungan untuk streamer...", color = TextSecondary, fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("donation_message_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = LightSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMsg!!, color = LiveRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Pay Button
                Button(
                    onClick = {
                        val sel = giftsList[selectedGiftIdx]
                        val isUserPremium = viewModel.wallet.value?.isPremium == true
                        val finalAmount = if (isUserPremium && sel.second >= 25000L) (sel.second * 0.8).toLong() else sel.second
                        val displayGiftName = if (isUserPremium && sel.second >= 25000L) "${sel.first} (Disc VIP)" else sel.first

                        viewModel.simulateDonation(
                            giftName = displayGiftName,
                            amount = finalAmount,
                            message = donationMessage.ifBlank { "Semangat terus live-nya!" },
                            onSuccess = {
                                donationSuccessState = true
                                errorMsg = null
                                // Auto dismiss after delay
                                coroutineScope.launch {
                                    delay(1000)
                                    onDismiss()
                                }
                            },
                            onError = { err ->
                                errorMsg = err
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_donation_payment"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberPink),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    if (donationSuccessState) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Donasi Berhasil Terkirim!", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        val isUserPremium = viewModel.wallet.value?.isPremium == true
                        val baseAmount = giftsList[selectedGiftIdx].second
                        val finalAmount = if (isUserPremium && baseAmount >= 25000L) (baseAmount * 0.8).toLong() else baseAmount

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Kirim & Bayar Rp ${viewModel.formatDecimal(finalAmount)}",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (isUserPremium && baseAmount >= 25000L) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(-20% VIP)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AccentGold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 7. BROADCASTER SCREEN / STUDIO MODE
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BroadcasterScreen(
    viewModel: MabarViewModel,
    onBack: () -> Unit
) {
    val isLive by viewModel.isUserStreaming.collectAsStateWithLifecycle()
    val streamTitle by viewModel.ownStreamTitle.collectAsStateWithLifecycle()
    val streamGame by viewModel.ownStreamGame.collectAsStateWithLifecycle()
    val viewers by viewModel.ownStreamViewers.collectAsStateWithLifecycle()
    val likes by viewModel.ownStreamLikes.collectAsStateWithLifecycle()
    val duration by viewModel.ownStreamDuration.collectAsStateWithLifecycle()
    val receivedEarnings by viewModel.receivedDonationsTotal.collectAsStateWithLifecycle()
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val activeAlert by viewModel.activeAlert.collectAsStateWithLifecycle()

    var inputTitle by remember { mutableStateOf("Mabar Santuy - Main Seru!") }
    var selectedGameIdx by remember { mutableStateOf(0) }
    var selectedQualityIdx by remember { mutableStateOf(0) }
    val gamesList = listOf("Mobile Legends", "PUBG Mobile", "Minecraft", "GTA V", "Free Fire")
    
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var replyToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var broadcasterChatInput by remember { mutableStateOf("") }
    val wallet by viewModel.wallet.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = SlateBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("GAMEST Creator Studio 🎙️", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Kembali", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = SlateBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!isLive) {
                // FORM TO START live stream
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.RecordVoiceOver,
                        contentDescription = "Studio logo",
                        tint = CyberPink,
                        modifier = Modifier.size(72.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Siarkan Game Anda",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Simulasikan live streaming game buatan Anda sendiri demi meraih penonton dan donasi virtual!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, LightSurface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Judul Siaran Anda:", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = inputTitle,
                                onValueChange = { inputTitle = it },
                                placeholder = { Text("Tulis judul live stream...") },
                                modifier = Modifier.fillMaxWidth().testTag("studio_stream_title_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = LightSurface,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Pilih Game yang Dimainkan:", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                itemsIndexed(gamesList) { idx, game ->
                                    val isSel = selectedGameIdx == idx
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { selectedGameIdx = idx },
                                        label = { Text(game, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CyberCyan,
                                            selectedLabelColor = SlateBackground,
                                            containerColor = SlateBackground,
                                            labelColor = TextSecondary
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))

                            Text("Kualitas Resolusi & Framerate:", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))

                            val isPremium = wallet?.isPremium == true
                            val context = LocalContext.current
                            val qualityOptions = if (isPremium) {
                                listOf("720p @ 30 FPS (Standard)", "1080p @ 60 FPS (FHD - VIP 👑)", "4K @ 60 FPS (Ultra HD - VIP 👑)")
                            } else {
                                listOf("720p @ 30 FPS (Standard)", "1080p @ 60 FPS (Premium 🔒)", "4K @ 60 FPS (Premium 🔒)")
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                qualityOptions.forEachIndexed { qIdx, option ->
                                    val isOptionSel = selectedQualityIdx == qIdx
                                    val isLocked = !isPremium && qIdx > 0

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isOptionSel) CyberCyan.copy(alpha = 0.15f) else Color.Transparent)
                                            .border(
                                                1.dp,
                                                if (isOptionSel) CyberCyan else LightSurface.copy(alpha = 0.5f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                if (!isLocked) {
                                                    selectedQualityIdx = qIdx
                                                } else {
                                                    android.widget.Toast.makeText(context, "Fitur ini khusus anggota GAMEST PREMIUM 👑. Upgrade terlebih dahulu di menu Wallet!", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = option,
                                            fontSize = 11.sp,
                                            fontWeight = if (isOptionSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isLocked) TextSecondary else TextPrimary
                                        )
                                        if (isOptionSel) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Terpilih", tint = CyberCyan, modifier = Modifier.size(16.dp))
                                        } else if (isLocked) {
                                            Icon(Icons.Default.Lock, contentDescription = "Terkunci", tint = LiveRed, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Request permission warning
                    if (!cameraPermissionState.status.isGranted) {
                        Text(
                            text = "💡 Aplikasi akan meminta izin kamera agar bisa menampilkan muka Anda sebagai overlay streamer di pojok layar!",
                            fontSize = 11.sp,
                            color = AccentGold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (cameraPermissionState.status.isGranted) {
                                viewModel.startUserStream(
                                    title = inputTitle,
                                    game = gamesList[selectedGameIdx],
                                    category = gamesList[selectedGameIdx]
                                )
                            } else {
                                cameraPermissionState.launchPermissionRequest()
                                // Still start stream anyway if they refuse camera
                                viewModel.startUserStream(
                                    title = inputTitle,
                                    game = gamesList[selectedGameIdx],
                                    category = gamesList[selectedGameIdx]
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("start_live_button"),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text("MULAI SIARAN LIVE SEKARANG ⚡", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            } else {
                // BROADCASTER IS LIVE PANEL VISUALS
                // Layout consists of: CameraX Feed preview in top half or top right, stats, and user chat logger
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.4f)
                            .background(Color.Black)
                    ) {
                        // CAMERA FEED OR BACKUP ICON
                        if (cameraPermissionState.status.isGranted) {
                            CameraPreviewFeed()
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.VideocamOff, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                                Text("Izin Kamera tidak diberikan. Menampilkan grafis game simulator.", color = TextSecondary, modifier = Modifier.padding(top = 60.dp), fontSize = 11.sp)
                            }
                        }

                        // Video simulated game overlay underneath
                        Box(
                            modifier = Modifier
                                .size(140.dp, 80.dp)
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .border(1.dp, CyberCyan, RoundedCornerShape(4.dp))
                                .clip(RoundedCornerShape(4.dp))
                        ) {
                            SimulatedGameStream(streamGame)
                        }

                        // Top-left status card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .align(Alignment.TopStart),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(LiveRed)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = formatDuration(duration),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            // Received money total indicator
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AccentGold)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = SlateBackground, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Rp ${viewModel.formatDecimal(receivedEarnings)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SlateBackground
                                    )
                                }
                            }
                        }

                        // Top donation congrats overlay
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .align(Alignment.BottomCenter)
                        ) {
                            AnimatedVisibility(
                                visible = activeAlert != null,
                                enter = fadeIn() + slideInVertically { -20 },
                                exit = fadeOut() + slideOutVertically { -20 }
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = AccentGold),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = activeAlert ?: "",
                                        color = SlateBackground,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(10.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Stats row in intermediate
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Penonton Saat Ini", fontSize = 10.sp, color = TextSecondary)
                            Text("$viewers", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Suka", fontSize = 10.sp, color = TextSecondary)
                            Text("$likes", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberPink)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Game", fontSize = 10.sp, color = TextSecondary)
                            Text(streamGame, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }

                    // Chat messages area (interactive clicks to reply)
                    Surface(
                        color = SlateBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.5f)
                    ) {
                        val scrollState = rememberLazyListState()
                        LaunchedEffect(messages.size) {
                            if (messages.isNotEmpty()) {
                                scrollState.animateScrollToItem(messages.size - 1)
                            }
                        }

                        LazyColumn(
                            state = scrollState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(messages) { msg ->
                                ChatBubbleRow(msg) { targetMsg ->
                                    if (!targetMsg.isSystem && targetMsg.senderName != wallet?.username) {
                                        replyToMessage = targetMsg
                                    }
                                }
                            }
                        }
                    }

                    // Interactive Streamer Reply bar (New chat input for Broadcaster)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        if (replyToMessage != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                                    .background(CyberCyan.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Membalas @${replyToMessage!!.senderName}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberCyan
                                    )
                                    Text(
                                        text = replyToMessage!!.messageText,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { replyToMessage = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Batal",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val keyboardController = LocalSoftwareKeyboardController.current
                            OutlinedTextField(
                                value = broadcasterChatInput,
                                onValueChange = { broadcasterChatInput = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("broadcaster_reply_input"),
                                placeholder = { Text("Balas atau sapa penonton...", color = TextSecondary, fontSize = 13.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = LightSurface,
                                    focusedContainerColor = SlateBackground,
                                    unfocusedContainerColor = SlateBackground,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(20.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = {
                                    if (broadcasterChatInput.isNotBlank()) {
                                        viewModel.sendBroadcasterMessage(
                                            broadcasterChatInput,
                                            replyToMessage?.senderName
                                        )
                                        broadcasterChatInput = ""
                                        replyToMessage = null
                                        keyboardController?.hide()
                                    }
                                })
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    if (broadcasterChatInput.isNotBlank()) {
                                        viewModel.sendBroadcasterMessage(
                                            broadcasterChatInput,
                                            replyToMessage?.senderName
                                        )
                                        broadcasterChatInput = ""
                                        replyToMessage = null
                                        keyboardController?.hide()
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(CyberCyan)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Kirim", tint = SlateBackground, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Stop button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = { viewModel.stopUserStream() },
                            colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("stop_live_button"),
                            shape = RoundedCornerShape(23.dp)
                        ) {
                            Text("AKHIRI SIARAN STREAMING 🛑", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// 8. WALLET SCREEN (TOP UP DEPOSIT & HISTORY SIMULATOR)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    viewModel: MabarViewModel,
    onBack: () -> Unit
) {
    val wallet by viewModel.wallet.collectAsStateWithLifecycle()
    val donationsSent by viewModel.donations.collectAsStateWithLifecycle()
    
    var showTopUpDialog by remember { mutableStateOf(false) }
    var customTopUpAmount by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        containerColor = SlateBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Dompet Virtual GAMEST Wallet 💸", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = SlateBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Main Balance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total Saldo Tersedia", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Rp ${viewModel.formatDecimal(wallet?.balance ?: 0L)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AccentGold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showTopUpDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("top_up_button_action"),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Icon(Icons.Default.AddCard, contentDescription = null, tint = SlateBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lakukan Top Up Saldo", fontWeight = FontWeight.ExtraBold, color = SlateBackground)
                    }
                }
            }

            // GAMEST Premium Section
            Spacer(modifier = Modifier.height(16.dp))
            val context = LocalContext.current
            val isPremium = wallet?.isPremium == true

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("premium_upgrade_card"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPremium) SlateBackground.copy(alpha = 0.5f) else DarkSurface
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    2.dp, 
                    if (isPremium) AccentGold else CyberPink.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "👑 GAMEST PREMIUM",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = if (isPremium) AccentGold else CyberPink
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (isPremium) {
                                Box(
                                    modifier = Modifier
                                        .background(AccentGold, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "AKTIF",
                                        color = SlateBackground,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        Text(
                            text = "Rp 49.000 / mgg",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text(
                        text = "Aktifkan untuk menikmati keuntungan eksklusif berikut:",
                        fontSize = 12.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    val benefits = listOf(
                        "🌟 Badge Premium Mahkota & Username Emas di Chat",
                        "🚫 Bebas Iklan Sponsor (Tanpa Jeda Ads)",
                        "⚡ Diskon 20% untuk Semua Pengiriman Gift Senilai Rp 25k+",
                        "🎙️ Mode Siaran Ultra HD 4K & 60FPS di Creator Studio"
                    )
                    
                    benefits.forEach { benefit ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isPremium) AccentGold else CyberPink,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = benefit,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!isPremium) {
                        Button(
                            onClick = {
                                viewModel.upgradeToPremium(
                                    onSuccess = {
                                        android.widget.Toast.makeText(context, "Selamat! Anda resmi menjadi GAMEST PREMIUM 👑", android.widget.Toast.LENGTH_LONG).show()
                                    },
                                    onError = { errMsg ->
                                        android.widget.Toast.makeText(context, errMsg, android.widget.Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPink),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("upgrade_premium_btn"),
                            shape = RoundedCornerShape(19.dp)
                        ) {
                            Text(
                                "Upgrade Sekarang (Rp 49.000)", 
                                fontWeight = FontWeight.Bold, 
                                color = TextPrimary,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                viewModel.cancelPremiumSubscription()
                                android.widget.Toast.makeText(context, "Langganan Premium dibatalkan.", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LiveRed),
                            border = BorderStroke(1.dp, LiveRed.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("cancel_premium_btn"),
                            shape = RoundedCornerShape(19.dp)
                        ) {
                            Text(
                                "Batalkan Berlangganan Premium", 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 13.sp,
                                color = LiveRed
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Riwayat Transaksi Terakhir", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)

            Spacer(modifier = Modifier.height(10.dp))

            if (donationsSent.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada transaksi donasi yang tercatat.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(donationsSent) { don ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ArrowOutward, contentDescription = "Debet", tint = LiveRed, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Kirim Donasi: ${don.giftType}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("Streamer: ${don.streamerName}", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                                Text("- Rp ${viewModel.formatDecimal(don.amount)}", fontWeight = FontWeight.Bold, color = LiveRed, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // TOP UP POPUP SHEET (SANDBOX DIALOG)
        if (showTopUpDialog) {
            AlertDialog(
                onDismissRequest = { showTopUpDialog = false },
                title = { Text("Lakukan Isi Ulang (Top Up)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Silakan klik salah satu nominal deposit atau masukkan nominal kustom secara langsung di bawah ini:",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val quickValues = listOf(10000L, 50000L, 100000L, 500000L)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickValues.forEach { valNum ->
                                InputChip(
                                    selected = false,
                                    onClick = {
                                        viewModel.topUpWallet(valNum)
                                        showTopUpDialog = false
                                    },
                                    label = { Text("Rp ${valNum / 1000}rb", fontSize = 10.sp) },
                                    colors = InputChipDefaults.inputChipColors(labelColor = CyberCyan)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = customTopUpAmount,
                            onValueChange = { customTopUpAmount = it },
                            placeholder = { Text("Masukkan nominal kustom...") },
                            modifier = Modifier.fillMaxWidth().testTag("custom_top_up_amount_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = LightSurface,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val parsed = customTopUpAmount.toLongOrNull() ?: 0L
                            if (parsed > 0) {
                                viewModel.topUpWallet(parsed)
                            }
                            showTopUpDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                    ) {
                        Text("Tambahkan Saldo", color = SlateBackground, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTopUpDialog = false }) {
                        Text("Batalkan", color = TextSecondary)
                    }
                },
                containerColor = DarkSurface
            )
        }
    }
}

// Helper: Format duration from seconds to MM:SS
private fun formatDuration(sec: Long): String {
    val m = sec / 60
    val s = sec % 60
    return String.format("%02d:%02d", m, s)
}

// 9. CameraX PreviewView holder
@SuppressLint("ClickableViewAccessibility")
@Composable
fun CameraPreviewFeed() {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                val cameraProviderProvider = ProcessCameraProvider.getInstance(ctx)
                cameraProviderProvider.addListener({
                    try {
                        val cameraProvider = cameraProviderProvider.get()
                        
                        val preview = androidx.camera.core.Preview.Builder().build()
                        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA // Front camera for stream simulation!

                        preview.surfaceProvider = this.surfaceProvider

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview
                        )
                    } catch (exc: Throwable) {
                        Log.e("CameraPreviewFeed", "Error binding camera preview: ${exc.message}", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
    )
}

// Automated retro style shooting game loop in Compose Canvas
@Composable
fun SimulatedGameStream(gameName: String) {
    // Ticker to animate the arcade gameplay
    val infiniteTransition = rememberInfiniteTransition(label = "gameplay_ticker")
    val frameCount by infiniteTransition.animateValue(
        initialValue = 0,
        targetValue = 360,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ticker"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Draw deep galactic black/purple space
        drawRect(color = Color(0xFF0F0B1E))

        // Draw animated stars
        for (i in 1..25) {
            val starX = (i * 137 + frameCount * 2) % size.width
            val starY = (i * 224 + frameCount) % size.height
            val radius = if (i % 3 == 0) 3f else 2f
            drawCircle(
                color = Color.White.copy(alpha = if (i % 2 == 0) 0.8f else 0.4f),
                radius = radius,
                center = Offset(starX, starY)
            )
        }

        // Gameplay drawing based on Category/GameName
        if (gameName.contains("Mobile Legends", ignoreCase = true) || gameName.contains("Free Fire", ignoreCase = true)) {
            // Draw an arena layout (simulated MOBA minimap / lanes)
            val pathColor = Color(0xFF00FFCC).copy(alpha = 0.2f)
            drawLine(color = pathColor, start = Offset(0f, 0f), end = Offset(size.width, size.height), strokeWidth = 10f)
            drawLine(color = pathColor, start = Offset(size.width, 0f), end = Offset(0f, size.height), strokeWidth = 10f)

            // Draw player heroes as glowing circles
            val heroX = size.width / 2f + (frameCount * 1.5f).coerceAtMost(size.width / 3f) - (size.width / 6f)
            val heroY = size.height / 2f + (frameCount * 1.2f).coerceAtMost(size.height / 3f) - (size.height / 6f)
            
            // Player Hero (glowing blue)
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = 22f,
                center = Offset(heroX, heroY)
            )
            // Player weapon ring
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.3f),
                radius = 35f,
                style = Stroke(width = 2f),
                center = Offset(heroX, heroY)
            )

            // Enemy Hero (glowing red)
            val enemyX = size.width * 0.7f
            val enemyY = size.height * 0.3f
            drawCircle(
                color = Color(0xFFFF1744),
                radius = 18f,
                center = Offset(enemyX, enemyY)
            )

            // Dynamic laser shot representation
            if ((frameCount / 30) % 2 == 0) {
                drawLine(
                    color = Color(0xFFFF8A00),
                    start = Offset(heroX, heroY),
                    end = Offset(enemyX, enemyY),
                    strokeWidth = 4f
                )
                // Blast effect
                drawCircle(
                    color = Color(0xFFFFA500),
                    radius = 12f,
                    center = Offset(enemyX, enemyY)
                )
            }
        } else if (gameName.contains("Resident Evil", ignoreCase = true) || gameName.contains("Horror", ignoreCase = true)) {
            // Dark horror corridor simulation
            drawRect(color = Color(0xFF07050A))
            
            // Flickering flashlight beam
            val flashlightAlpha = if ((frameCount / 15) % 2 == 0) 0.15f else 0.25f
            val path = Path().apply {
                moveTo(size.width * 0.2f, size.height * 0.8f) // Flashlight source
                lineTo(size.width * 0.8f, size.height * 0.2f)
                lineTo(size.width * 0.95f, size.height * 0.5f)
                close()
            }
            drawPath(path = path, color = Color(0xFFFFFFB3).copy(alpha = flashlightAlpha))

            // Draw a zombie silhouette inside flashlight area
            val zombieX = size.width * 0.85f - (frameCount % 60) * 1.5f
            val zombieY = size.height * 0.35f
            drawCircle(
                color = Color(0xFF32CD32).copy(alpha = 0.7f),
                radius = 15f,
                center = Offset(zombieX, zombieY)
            )
            // Zombie hands
            drawLine(
                color = Color(0xFF228B22),
                start = Offset(zombieX, zombieY),
                end = Offset(zombieX - 20f, zombieY + 4f),
                strokeWidth = 3f
            )

            // Shotgun blast animation
            if (frameCount % 50 in 1..8) {
                drawCircle(
                    color = Color(0xFFFFFFE0),
                    radius = 28f,
                    center = Offset(size.width * 0.2f, size.height * 0.8f)
                )
                drawLine(
                    color = Color.Yellow,
                    start = Offset(size.width * 0.2f, size.height * 0.8f),
                    end = Offset(zombieX, zombieY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }
        } else {
            // Default Space Retro Shooter style gameplay drawing
            val px = size.width / 2f + (Math.sin(frameCount * Math.PI / 180f) * (size.width / 3f)).toFloat()
            val py = size.height * 0.75f

            // Draw player ship
            val shipPath = Path().apply {
                moveTo(px, py - 20f)
                lineTo(px - 18f, py + 12f)
                lineTo(px + 18f, py + 12f)
                close()
            }
            drawPath(path = shipPath, color = Color(0xFF00FFCC))

            // Shooting lasers
            val laserY = py - 20f - ((frameCount * 8) % (py - 30f))
            if (laserY > 30f) {
                drawRect(
                    color = Color(0xFFFF0D80),
                    topLeft = Offset(px - 2f, laserY),
                    size = Size(4f, 18f)
                )
            }

            // Alien invader
            val enemyX = size.width / 2f + (Math.cos(frameCount * 1.5f * Math.PI / 180f) * (size.width / 4f)).toFloat()
            val enemyY = size.height * 0.25f

            drawCircle(
                color = Color(0xFF8E00FF),
                radius = 22f,
                center = Offset(enemyX, enemyY)
            )
            // Draw alien eye
            drawCircle(
                color = Color.White,
                radius = 6f,
                center = Offset(enemyX, enemyY)
            )
            drawCircle(
                color = Color.Black,
                radius = 2f,
                center = Offset(enemyX, enemyY)
            )
        }
    }
}
