package com.example.ui.main

import android.content.Context
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.BlueAccentDark
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.VideoEntity
import com.example.data.database.AudioEntity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.compose.animation.core.*
import com.example.ui.viewmodel.ScanState
import com.example.ui.viewmodel.VideoPlayerViewModel
import com.example.ui.viewmodel.SortType
import android.net.Uri
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.BottomSheetDefaults
import com.example.ui.components.PlayerSettingsDialog
import com.example.ui.components.VerticalEqualizerSlider
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import com.example.data.settings.PlayerSettings
import android.widget.Toast
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring

import kotlinx.coroutines.launch
import androidx.compose.foundation.LocalIndication
import android.content.Intent
import androidx.compose.ui.draw.shadow
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import com.example.util.DownloadWorker
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MediaDashboardScreen(
    viewModel: VideoPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // SAF Backup Manual Document Picker
    val safPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        // Handle imported manual video URI
        uri?.let {
            viewModel.importMediaFromUri(context, it)
        }
    }
    
    val allVideos by viewModel.allVideos.collectAsStateWithLifecycle()
    val recentVideos by viewModel.recentVideos.collectAsStateWithLifecycle()
    val favoriteVideos by viewModel.favoriteVideos.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    // Scanned audio streams from Room
    val allAudios by viewModel.allAudios.collectAsStateWithLifecycle()
    val recentAudios by viewModel.recentAudios.collectAsStateWithLifecycle()
    val favoriteAudios by viewModel.favoriteAudios.collectAsStateWithLifecycle()
    val playingAudio by viewModel.playingAudio.collectAsStateWithLifecycle()

    // 5-Tab Selection State (matching the design image)
    var currentSection by remember { mutableStateOf("Video") }
    var showDashboardSettings by remember { mutableStateOf(false) }

    // Audio Player State & Setup
    val audioPlayer = remember { com.example.util.MediaCacheManager.buildExoPlayer(context) }
    var audioSessionIdState by remember(audioPlayer) { mutableIntStateOf(audioPlayer.audioSessionId) }
    var eqEnabled by remember { mutableStateOf(PlayerSettings.getEqualizerEnabled(context)) }

    DisposableEffect(audioPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    viewModel.playNextAudio()
                }
            }
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                audioSessionIdState = audioSessionId
            }
        }
        audioPlayer.addListener(listener)
        onDispose {
            audioPlayer.removeListener(listener)
            audioPlayer.release()
        }
    }

    DisposableEffect(audioPlayer, eqEnabled, audioSessionIdState) {
        var eq: android.media.audiofx.Equalizer? = null
        if (eqEnabled && audioSessionIdState != android.media.AudioManager.AUDIO_SESSION_ID_GENERATE) {
            try {
                eq = android.media.audiofx.Equalizer(0, audioSessionIdState).apply {
                    enabled = true
                    val numBands = numberOfBands.toInt().coerceAtMost(5)
                    for (i in 0 until numBands) {
                        val bandValDb = PlayerSettings.getEqualizerBand(context, i)
                        val milliBelLevel = (bandValDb * 100).toInt().toShort()
                        val levelRange = bandLevelRange
                        if (levelRange != null && levelRange.size >= 2) {
                            val clampedLevel = milliBelLevel.coerceIn(levelRange[0], levelRange[1])
                            setBandLevel(i.toShort(), clampedLevel)
                        } else {
                            setBandLevel(i.toShort(), milliBelLevel)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        onDispose {
            eq?.release()
        }
    }

    var isAudioPlaying by remember { mutableStateOf(false) }
    var audioPosition by remember { mutableLongStateOf(0L) }
    var audioDuration by remember { mutableLongStateOf(0L) }
    var transitionTriggeredForTrack by remember { mutableStateOf("") }

    LaunchedEffect(audioPlayer, playingAudio) {
        while (true) {
            isAudioPlaying = audioPlayer.isPlaying
            audioPosition = audioPlayer.currentPosition
            audioDuration = audioPlayer.duration.coerceAtLeast(0L)

            // Seamless transitions: start next track 500ms before end of current track
            if (PlayerSettings.getSeamlessTransitions(context) &&
                isAudioPlaying &&
                audioDuration > 2000L &&
                audioPosition >= (audioDuration - 500L) &&
                transitionTriggeredForTrack != (playingAudio?.uriString ?: "")
            ) {
                transitionTriggeredForTrack = playingAudio?.uriString ?: ""
                viewModel.playNextAudio()
            }

            kotlinx.coroutines.delay(100)
        }
    }

    LaunchedEffect(playingAudio) {
        transitionTriggeredForTrack = ""
        eqEnabled = PlayerSettings.getEqualizerEnabled(context)
        audioPlayer.stop()
        playingAudio?.let { audio ->
            try {
                audioPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(audio.uriString)))
                audioPlayer.prepare()
                audioPlayer.seekTo(audio.lastPlayedPosition)
                audioPlayer.playWhenReady = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Expand view settings of full screen audio player sheet
    var showFullAudioPlayerSheet by remember { mutableStateOf(false) }

    // Loading layout/sort preferences
    var displayViewMode by remember { mutableStateOf(PlayerSettings.getViewMode(context)) }
    var showOnlyFavoritesSetting by remember { mutableStateOf(PlayerSettings.getShowOnlyFavorites(context)) }
    var playbackActionSetting by remember { mutableStateOf(PlayerSettings.getPlaybackAction(context)) }
    var sortFieldSetting by remember { mutableStateOf(PlayerSettings.getSortField(context)) }
    var sortOrderSetting by remember { mutableStateOf(PlayerSettings.getSortOrder(context)) }
    var groupBySetting by remember { mutableStateOf(PlayerSettings.getGroupBySetting(context)) }

    LaunchedEffect(displayViewMode) { PlayerSettings.setViewMode(context, displayViewMode) }
    LaunchedEffect(showOnlyFavoritesSetting) { PlayerSettings.setShowOnlyFavorites(context, showOnlyFavoritesSetting) }
    LaunchedEffect(playbackActionSetting) { PlayerSettings.setPlaybackAction(context, playbackActionSetting) }
    LaunchedEffect(sortFieldSetting) { PlayerSettings.setSortField(context, sortFieldSetting) }
    LaunchedEffect(sortOrderSetting) { PlayerSettings.setSortOrder(context, sortOrderSetting) }
    LaunchedEffect(groupBySetting) { PlayerSettings.setGroupBySetting(context, groupBySetting) }
    LaunchedEffect(sortFieldSetting, sortOrderSetting) {
        val type = when (sortFieldSetting) {
            "name", "file_name" -> SortType.NAME
            "added_date" -> SortType.DATE_ADDED
            "size" -> SortType.FILE_SIZE
            else -> SortType.NAME
        }
        val ascending = (sortOrderSetting == "asc")
        viewModel.updateSort(type, ascending)
    }
    var quickPlayVideo by remember { mutableStateOf<VideoEntity?>(null) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }
    
    // Keep track of collapsed states of groups (e.g. key = groupKey, value = true if collapsed)
    val collapsedGroups = remember { mutableStateMapOf<String, Boolean>() }

    // Equalizer State Variables (More Tab)
    var eq60Hz by remember { mutableFloatStateOf(0f) }
    var eq230Hz by remember { mutableFloatStateOf(2f) }
    var eq910Hz by remember { mutableFloatStateOf(5f) }
    var eq4kHz by remember { mutableFloatStateOf(-1f) }
    var eq14kHz by remember { mutableFloatStateOf(3f) }
    var isEqSecEnabled by remember { mutableStateOf(true) }
    var baseBoostVal by remember { mutableFloatStateOf(44f) }
    var surroundDepthVal by remember { mutableFloatStateOf(65f) }

    // Determine target storage API permissions
    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_VIDEO
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && PlayerSettings.getAutoRescan(context)) {
            viewModel.scanLocalVideos()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            viewModel.scanLocalVideos()
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.handlePickedVideoUri(context, it)
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            viewModel.handlePickedFolderUri(context, it)
        }
    }

    // Filtered Video selection
    val rawList = allVideos
    val favoritesFiltered = remember(rawList, showOnlyFavoritesSetting) {
        if (showOnlyFavoritesSetting) {
            rawList.filter { it.isFavorite }
        } else {
            rawList
        }
    }
    val searchedList = remember(favoritesFiltered, searchQuery) {
        if (searchQuery.isBlank()) {
            favoritesFiltered
        } else {
            favoritesFiltered.filter { 
                it.title.contains(searchQuery, ignoreCase = true) || 
                it.folderName.contains(searchQuery, ignoreCase = true) 
            }
        }
    }
    val filteredVideos = remember(searchedList, sortFieldSetting, sortOrderSetting) {
        val sorted = when (sortFieldSetting) {
            "name", "file_name" -> {
                if (sortOrderSetting == "asc") searchedList.sortedBy { it.title.lowercase() }
                else searchedList.sortedByDescending { it.title.lowercase() }
            }
            "length" -> {
                if (sortOrderSetting == "asc") searchedList.sortedBy { it.duration }
                else searchedList.sortedByDescending { it.duration }
            }
            "added_date" -> {
                if (sortOrderSetting == "asc") searchedList.sortedBy { it.addedDate }
                else searchedList.sortedByDescending { it.addedDate }
            }
            "size" -> {
                if (sortOrderSetting == "asc") searchedList.sortedBy { it.size }
                else searchedList.sortedByDescending { it.size }
            }
            else -> searchedList
        }
        sorted
    }

    val groupedVideos = remember(filteredVideos, groupBySetting) {
         when (groupBySetting) {
             "folder" -> filteredVideos.groupBy { it.folderName }
             "name" -> filteredVideos.groupBy { it.title.firstOrNull()?.uppercaseChar()?.toString() ?: "#" }
             else -> emptyMap()
         }
    }

    var selectedVideoForInfo by remember { mutableStateOf<VideoEntity?>(null) }
    var selectedFolderForActions by remember { mutableStateOf<Pair<String, List<VideoEntity>>?>(null) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .drawBehind {
                        // Sleek tech accent divider
                        drawLine(
                            color = BlueAccent.copy(alpha = 0.2f),
                            start = androidx.compose.ui.geometry.Offset(0f, size.height),
                            end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Text(
                                "SYS.ACT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = when(currentSection) {
                                "Video" -> "VIDEOS"
                                "Audio" -> "MUSIC"
                                "Browse" -> "FILES"
                                "Playlists" -> "PLAYLISTS"
                                else -> "SETTINGS"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconButton(
                            onClick = {
                                isSearchExpanded = !isSearchExpanded
                                if (!isSearchExpanded) {
                                    viewModel.updateSearchQuery("")
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (isSearchExpanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Toggle search panel",
                                tint = if (isSearchExpanded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { isSortMenuExpanded = true },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort videos",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            DropdownMenu(
                                expanded = isSortMenuExpanded,
                                onDismissRequest = { isSortMenuExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Name (A-Z)") },
                                    leadingIcon = {
                                        if (sortFieldSetting == "name" && sortOrderSetting == "asc") {
                                            Icon(Icons.Default.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    modifier = Modifier.testTag("sort_name_asc"),
                                    onClick = {
                                        sortFieldSetting = "name"
                                        sortOrderSetting = "asc"
                                        isSortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Name (Z-A)") },
                                    leadingIcon = {
                                        if (sortFieldSetting == "name" && sortOrderSetting == "desc") {
                                            Icon(Icons.Default.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    modifier = Modifier.testTag("sort_name_desc"),
                                    onClick = {
                                        sortFieldSetting = "name"
                                        sortOrderSetting = "desc"
                                        isSortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Date Added (Newest)") },
                                    leadingIcon = {
                                        if (sortFieldSetting == "added_date" && sortOrderSetting == "desc") {
                                            Icon(Icons.Default.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    modifier = Modifier.testTag("sort_date_desc"),
                                    onClick = {
                                        sortFieldSetting = "added_date"
                                        sortOrderSetting = "desc"
                                        isSortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Date Added (Oldest)") },
                                    leadingIcon = {
                                        if (sortFieldSetting == "added_date" && sortOrderSetting == "asc") {
                                            Icon(Icons.Default.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    modifier = Modifier.testTag("sort_date_asc"),
                                    onClick = {
                                        sortFieldSetting = "added_date"
                                        sortOrderSetting = "asc"
                                        isSortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("File Size (Largest)") },
                                    leadingIcon = {
                                        if (sortFieldSetting == "size" && sortOrderSetting == "desc") {
                                            Icon(Icons.Default.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    modifier = Modifier.testTag("sort_size_desc"),
                                    onClick = {
                                        sortFieldSetting = "size"
                                        sortOrderSetting = "desc"
                                        isSortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("File Size (Smallest)") },
                                    leadingIcon = {
                                        if (sortFieldSetting == "size" && sortOrderSetting == "asc") {
                                            Icon(Icons.Default.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    modifier = Modifier.testTag("sort_size_asc"),
                                    onClick = {
                                        sortFieldSetting = "size"
                                        sortOrderSetting = "asc"
                                        isSortMenuExpanded = false
                                    }
                                )
                            }
                        }

                        IconButton(
                            onClick = { showDashboardSettings = true },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Configure dashboard settings",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                if (showDashboardSettings) {
                    PlayerSettingsDialog(
                        displayViewMode = displayViewMode,
                        onViewModeChanged = { displayViewMode = it },
                        showOnlyFavoritesSetting = showOnlyFavoritesSetting,
                        onShowOnlyFavoritesChanged = { showOnlyFavoritesSetting = it },
                        playbackActionSetting = playbackActionSetting,
                        onPlaybackActionChanged = { playbackActionSetting = it },
                        sortFieldSetting = sortFieldSetting,
                        sortOrderSetting = sortOrderSetting,
                        onSortChanged = { field, order ->
                            sortFieldSetting = field
                            sortOrderSetting = order
                        },
                        groupBySetting = groupBySetting,
                        onGroupByChanged = { groupBySetting = it },
                        onFolderPickerLaunch = { folderPickerLauncher.launch(null) },
                        onDocumentPickerLaunch = { documentPickerLauncher.launch(arrayOf("video/*")) },
                        onDeepScan = {
                            if (hasPermission) {
                                viewModel.scanDeepLocalVideos()
                            } else {
                                permissionLauncher.launch(storagePermission)
                            }
                        },
                        onAutomatedScan = {
                            if (hasPermission) {
                                viewModel.scanLocalVideos()
                            } else {
                                permissionLauncher.launch(storagePermission)
                            }
                        },
                        onDismiss = { showDashboardSettings = false }
                    )
                }

                AnimatedVisibility(
                    visible = isSearchExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Search your media collection...", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("video_search_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF141416),
                            unfocusedContainerColor = Color(0xFF141416),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                }
            }
        },
        bottomBar = {
            Column(modifier = Modifier.background(Color(0xFF000000))) {
                if (playingAudio != null && !showFullAudioPlayerSheet) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFullAudioPlayerSheet = true }
                            .background(Color(0xFF121212))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            color = if (isAudioPlaying) Color(0xFF3B82F6).copy(alpha = 0.2f) else Color(0xFF1A1A1A)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = if (isAudioPlaying) Color(0xFF3B82F6) else Color(0xFF9CA3AF)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playingAudio!!.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = playingAudio!!.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF9CA3AF),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (playingAudio!!.isHiResLossless) {
                                    Text(
                                        text = "Hi-Res Lossless",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                        color = Color(0xFF14B8A6), // Premium Teal color for Hi-res audio
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(Color(0xFF0F2D2A), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                } else if (playingAudio!!.isLossless) {
                                    Text(
                                        text = "Lossless",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                        color = Color(0xFFBAC0C4),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(Color(0xFF222528), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        val playPauseInteractionSource = remember { MutableInteractionSource() }
                        val isPlayPausePressed by playPauseInteractionSource.collectIsPressedAsState()
                        
                        val nextInteractionSource = remember { MutableInteractionSource() }
                        val isNextPressed by nextInteractionSource.collectIsPressedAsState()

                        val playPauseScaleState = remember { Animatable(1f) }
                        LaunchedEffect(isAudioPlaying) {
                            playPauseScaleState.animateTo(
                                targetValue = 0.85f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                            )
                            playPauseScaleState.animateTo(
                                targetValue = 1.0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                            )
                        }

                        val nextScaleState = remember { Animatable(1f) }
                        val nextRotationState = remember { Animatable(0f) }
                        LaunchedEffect(playingAudio?.uriString) {
                            if (playingAudio != null) {
                                launch {
                                    nextScaleState.animateTo(
                                        targetValue = 0.82f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                                    )
                                    nextScaleState.animateTo(
                                        targetValue = 1.0f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                                    )
                                }
                                launch {
                                    nextRotationState.animateTo(
                                        targetValue = 20f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                                    )
                                    nextRotationState.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                                    )
                                }
                            }
                        }

                        val interactivePlayPauseScale by animateFloatAsState(
                            targetValue = if (isPlayPausePressed) 0.82f else playPauseScaleState.value,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                        )

                        val interactiveNextScale by animateFloatAsState(
                            targetValue = if (isNextPressed) 0.82f else nextScaleState.value,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                        )

                        IconButton(
                            onClick = {
                                if (isAudioPlaying) {
                                    audioPlayer.pause()
                                } else {
                                    audioPlayer.play()
                                }
                            },
                            interactionSource = playPauseInteractionSource,
                            modifier = Modifier.graphicsLayer {
                                scaleX = interactivePlayPauseScale
                                scaleY = interactivePlayPauseScale
                            }
                        ) {
                            Icon(
                                imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { viewModel.playNextAudio() },
                            interactionSource = nextInteractionSource,
                            modifier = Modifier.graphicsLayer {
                                scaleX = interactiveNextScale
                                scaleY = interactiveNextScale
                                rotationZ = nextRotationState.value
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Track",
                                tint = Color.White
                            )
                        }

                        IconButton(onClick = { viewModel.closeAudioPlayer() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close player",
                                tint = Color(0xFF9CA3AF)
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = {
                            if (audioDuration > 0) audioPosition.toFloat() / audioDuration.toFloat()
                            else 0f
                        },
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = Color(0xFF3B82F6),
                        trackColor = Color(0xFF262626)
                    )
                }

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .drawBehind {
                            // Thin futuristic neon divider at the top
                            drawLine(
                                color = BlueAccent.copy(alpha = 0.4f),
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                ) {
                    NavigationBarItem(
                        selected = (currentSection == "Video"),
                        onClick = { currentSection = "Video" },
                        label = { Text("Video", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        icon = {
                            Icon(
                                imageVector = if (currentSection == "Video") Icons.Filled.Movie else Icons.Outlined.Movie,
                                contentDescription = "Video Tab"
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    NavigationBarItem(
                        selected = (currentSection == "Audio"),
                        onClick = { currentSection = "Audio" },
                        label = { Text("Audio", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Audio Tab"
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    NavigationBarItem(
                        selected = (currentSection == "Browse"),
                        onClick = { currentSection = "Browse" },
                        label = { Text("Browse", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Browse Tab"
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    NavigationBarItem(
                        selected = (currentSection == "Playlists"),
                        onClick = { currentSection = "Playlists" },
                        label = { Text("Playlists", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = "Playlists Tab"
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    NavigationBarItem(
                        selected = (currentSection == "More"),
                        onClick = { currentSection = "More" },
                        label = { Text("More", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "More Tab"
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Scanning HUD indicator
                AnimatedVisibility(
                    visible = scanState == ScanState.SCANNING,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Scanning device storage for multimedia tracks...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Render Active Section Contextually
                when (currentSection) {
                    "Video" -> {
                        if (filteredVideos.isEmpty()) {
                            EmptyStateLayout(
                                selectedTab = 0,
                                searchQuery = searchQuery,
                                hasPermission = hasPermission,
                                onRequestPermission = { permissionLauncher.launch(storagePermission) },
                                onPickFolder = { folderPickerLauncher.launch(null) }
                            )
                        } else {
                            val onItemClick = { video: VideoEntity ->
                                when (playbackActionSetting) {
                                    "play" -> {
                                        viewModel.selectVideo(video, listOf(video))
                                    }
                                    "play_all" -> {
                                        viewModel.selectVideo(video, filteredVideos)
                                    }
                                    "add_queue" -> {
                                        viewModel.addToPlayQueue(video)
                                        Toast.makeText(context, "Added to play queue: ${video.title}", Toast.LENGTH_SHORT).show()
                                    }
                                    "insert_next" -> {
                                        viewModel.insertNext(video)
                                        Toast.makeText(context, "Inserted as next track: ${video.title}", Toast.LENGTH_SHORT).show()
                                    }
                                    else -> {
                                        viewModel.selectVideo(video, filteredVideos)
                                    }
                                }
                            }

                            if (recentVideos.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Continue Watching",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                androidx.compose.foundation.lazy.LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(recentVideos.take(5), key = { it.uriString }) { video ->
                                        Box(modifier = Modifier.width(160.dp)) {
                                            VideoItemGrid(
                                                video = video,
                                                onClick = { onItemClick(video) },
                                                onLongClick = { quickPlayVideo = video },
                                                onFavoriteToggle = { viewModel.toggleFavorite(video) },
                                                onInfoRequested = { selectedVideoForInfo = video }
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            if (groupBySetting != "none" && groupedVideos.isNotEmpty()) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Global controls to Expand / Collapse All folders
                                    item(key = "group_controls") {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Grouped ${if (groupBySetting == "folder") "by Folder" else "by Alphabet"}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.Gray,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                TextButton(
                                                    onClick = {
                                                        groupedVideos.keys.forEach { key ->
                                                            collapsedGroups[key] = false
                                                        }
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ExpandMore,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp).padding(end = 4.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                     )
                                                     Text(
                                                         text = "Expand All",
                                                         fontSize = 11.sp,
                                                         color = MaterialTheme.colorScheme.primary,
                                                         fontWeight = FontWeight.Bold
                                                     )
                                                 }
                                                 TextButton(
                                                     onClick = {
                                                         groupedVideos.keys.forEach { key ->
                                                             collapsedGroups[key] = true
                                                         }
                                                     },
                                                     contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                 ) {
                                                     Icon(
                                                         imageVector = Icons.Default.ExpandLess,
                                                         contentDescription = null,
                                                         modifier = Modifier.size(16.dp).padding(end = 4.dp),
                                                         tint = Color.LightGray
                                                     )
                                                     Text(
                                                         text = "Collapse All",
                                                         fontSize = 11.sp,
                                                         color = Color.LightGray,
                                                         fontWeight = FontWeight.Bold
                                                     )
                                                 }
                                             }
                                         }
                                     }

                                    groupedVideos.forEach { (groupKey, groupVideos) ->
                                        val isCollapsed = collapsedGroups[groupKey] ?: false
                                        
                                        item(key = "header_${groupKey}") {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .combinedClickable(
                                                        onClick = {
                                                            collapsedGroups[groupKey] = !isCollapsed
                                                        },
                                                        onLongClick = {
                                                            if (groupBySetting == "folder") {
                                                                selectedFolderForActions = Pair(groupKey, groupVideos)
                                                            }
                                                        }
                                                    )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = if (groupBySetting == "folder") Icons.Default.Folder else Icons.Default.SortByAlpha,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Text(
                                                            text = groupKey,
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically) { Surface(
                                                        color = Color.Black,
                                                        shape = RoundedCornerShape(10.dp)
                                                    ) {
                                                        Text(
                                                            text = "${groupVideos.size} items",
                                                            color = Color.Gray,
                                                            fontSize = 11.sp,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    IconButton(
                                                        onClick = { collapsedGroups[groupKey] = !isCollapsed },
                                                        modifier = Modifier.size(36.dp).testTag("folder_expand_collapse_${groupKey}")
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                                            contentDescription = if (isCollapsed) "Expand folder" else "Collapse folder",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    }
                                                }
                                            }
                                        }

                                        if (!isCollapsed) {
                                            if (displayViewMode == "grid") {
                                            val chunked = groupVideos.chunked(2)
                                            items(chunked) { chunk ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    chunk.forEach { video ->
                                                        VideoItemGrid(
                                                            video = video,
                                                            onClick = { onItemClick(video) },
                                                            onLongClick = { quickPlayVideo = video },
                                                            onFavoriteToggle = { viewModel.toggleFavorite(video) },
                                                            onInfoRequested = { selectedVideoForInfo = video },
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }
                                                    if (chunk.size == 1) {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        } else {
                                            items(groupVideos, key = { it.uriString }, contentType = { "video_row" }) { video ->
                                                VideoItemRow(
                                                    video = video,
                                                    onClick = { onItemClick(video) },
                                                    onLongClick = { quickPlayVideo = video },
                                                    onFavoriteToggle = { viewModel.toggleFavorite(video) },
                                                    onInfoRequested = { selectedVideoForInfo = video }
                                                )
                                            }
                                        }
                                        }
                                    }
                                }
                            } else {
                                if (displayViewMode == "grid") {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp),
                                        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(filteredVideos, key = { it.uriString }, contentType = { "video_grid" }) { video ->
                                            VideoItemGrid(
                                                video = video,
                                                onClick = { onItemClick(video) },
                                                onLongClick = { quickPlayVideo = video },
                                                onFavoriteToggle = { viewModel.toggleFavorite(video) },
                                                onInfoRequested = { selectedVideoForInfo = video },
                                                modifier = Modifier.animateItem()
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp),
                                        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(filteredVideos, key = { it.uriString }, contentType = { "video_row" }) { video ->
                                            VideoItemRow(
                                                video = video,
                                                onClick = { onItemClick(video) },
                                                onLongClick = { quickPlayVideo = video },
                                                onFavoriteToggle = { viewModel.toggleFavorite(video) },
                                                onInfoRequested = { selectedVideoForInfo = video },
                                                modifier = Modifier.animateItem()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "Audio" -> {
                        val filteredAudios = remember(allAudios, searchQuery) {
                            if (searchQuery.isBlank()) allAudios
                            else allAudios.filter { it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true) }
                        }

                        if (filteredAudios.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("No audio tracks found", color = Color.White)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Try scanning for files or importing music files.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredAudios, key = { it.uriString }, contentType = { "audio_row" }) { audio ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.selectAudio(audio, filteredAudios) },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                                )
                                            )
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (audio.isLossless) Color(0xFF14B8A6).copy(alpha = 0.1f)
                                                        else Color(0xFF1A1A1A)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MusicNote,
                                                    contentDescription = null,
                                                    tint = if (audio.isLossless) Color(0xFF14B8A6) else Color(0xFF9CA3AF)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(16.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = audio.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = audio.artist,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFF9CA3AF),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = audio.format.uppercase(),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                        color = Color(0xFF9CA3AF),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    if (audio.isHiResLossless) {
                                                        Text(
                                                            text = "Hi-Res Lossless",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                                            color = Color(0xFF14B8A6),
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier
                                                                .background(Color(0xFF0F2D2A), RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    } else if (audio.isLossless) {
                                                        Text(
                                                            text = "Lossless",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                                            color = Color(0xFFBAC0C4),
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier
                                                                .background(Color(0xFF141A1D), RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = audio.durationFormatted,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.LightGray
                                                )
                                                IconButton(
                                                    onClick = { viewModel.toggleAudioFavorite(audio) }
                                                ) {
                                                    Icon(
                                                        imageVector = if (audio.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                        tint = if (audio.isFavorite) Color.Red else Color.LightGray,
                                                        contentDescription = "Toggle favorite"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "Browse" -> {
                        val losslessCount = remember(allAudios) { allAudios.count { it.isLossless } }
                        val hiresCount = remember(allAudios) { allAudios.count { it.isHiResLossless } }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141416))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Media Statistics", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Total Video Tracks: ${allVideos.size}", color = Color.LightGray)
                                        Text("Total Audio Tracks: ${allAudios.size}", color = Color.LightGray)
                                        Text("Lossless Quality: $losslessCount", color = Color(0xFFBAC0C4))
                                        Text("Hi-Res Lossless: $hiresCount", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            item {
                                Text("Storage & Scanner Shortcuts", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable { folderPickerLauncher.launch(null) },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF14B8A6), modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text("Import Directory", color = Color.White, fontWeight = FontWeight.Bold)
                                            Text("Browse with system directory tool", color = Color(0xFF9CA3AF), style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }

                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable { viewModel.scanLocalVideos() },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF14B8A6), modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text("Quick Automatic Storage Rescan", color = Color.White, fontWeight = FontWeight.Bold)
                                            Text("Scan media folders instantly", color = Color(0xFF9CA3AF), style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "Playlists" -> {
                        var downloadUrl by remember { mutableStateOf("") }
                        val downloaderPrefs = remember { context.getSharedPreferences("downloader_prefs", Context.MODE_PRIVATE) }
                        var downloadLocationUri by remember { 
                            mutableStateOf(downloaderPrefs.getString("preferred_location_uri", "") ?: "") 
                        }
                        var setAsPreferredLocation by remember { mutableStateOf(false) }
                        var isCustomLocation by remember { mutableStateOf(downloadLocationUri.isNotEmpty()) }
                        var expandedQuality by remember { mutableStateOf(false) }
                        var selectedQuality by remember { mutableStateOf("1080p") }

                        val qualityMap = remember {
                            mapOf(
                                "Best Available" to "best",
                                "8K (4320p)" to "bestvideo[height<=4320]+bestaudio/best",
                                "4K (2160p)" to "bestvideo[height<=2160]+bestaudio/best",
                                "2K (1440p)" to "bestvideo[height<=1440]+bestaudio/best",
                                "1080p 60fps" to "bestvideo[height<=1080][fps>=60]+bestaudio/best",
                                "1080p" to "bestvideo[height<=1080]+bestaudio/best",
                                "720p 60fps" to "bestvideo[height<=720][fps>=60]+bestaudio/best",
                                "720p" to "bestvideo[height<=720]+bestaudio/best",
                                "Audio Only" to "bestaudio"
                            )
                        }

                        val downloadLocationLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.OpenDocumentTree()
                        ) { uri ->
                            uri?.let {
                                try {
                                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    context.contentResolver.takePersistableUriPermission(it, takeFlags)
                                } catch (e: Exception) { e.printStackTrace() }
                                downloadLocationUri = it.toString()
                                isCustomLocation = true
                            }
                        }

                        val workInfos by WorkManager.getInstance(context)
                            .getWorkInfosByTagFlow("ytdlp_download")
                            .collectAsState(initial = emptyList())

                        val activeWorkInfo = remember(workInfos) {
                            workInfos.firstOrNull { !it.state.isFinished } ?: workInfos.firstOrNull()
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Text(
                                    "MHDXBILAL DOWNLOAD ENGINE", 
                                    style = MaterialTheme.typography.titleMedium, 
                                    color = Color(0xFF00F3FF), 
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }

                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F11)),
                                    border = BorderStroke(1.dp, Color(0xFF1F1F23))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            "Paste Media Stream / Video URL", 
                                            style = MaterialTheme.typography.bodyMedium, 
                                            color = Color.LightGray, 
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Row containing text field and quality selector
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = downloadUrl,
                                                onValueChange = { downloadUrl = it },
                                                placeholder = { Text("Paste URL here...", color = Color.Gray) },
                                                singleLine = true,
                                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color(0xFF00F3FF),
                                                    unfocusedBorderColor = Color(0xFF2E2E35),
                                                    focusedContainerColor = Color(0x1500F3FF),
                                                    unfocusedContainerColor = Color(0x0AFFFFFF)
                                                ),
                                                modifier = Modifier.weight(0.68f)
                                            )

                                            // Quality Select Dropdown
                                            Box(
                                                modifier = Modifier
                                                    .weight(0.32f)
                                                    .height(56.dp)
                                                    .background(
                                                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                                            colors = listOf(Color(0xFF00E5FF), Color(0xFFBD00FF))
                                                        ),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable { expandedQuality = true }
                                                    .padding(1.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color(0xFF0F0F12), RoundedCornerShape(11.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center,
                                                        modifier = Modifier.padding(horizontal = 8.dp)
                                                    ) {
                                                        Text(
                                                            text = selectedQuality,
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f),
                                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                        )
                                                        Icon(
                                                            imageVector = Icons.Default.ArrowDropDown,
                                                            contentDescription = "Quality Options",
                                                            tint = Color(0xFF00F3FF),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }

                                                DropdownMenu(
                                                    expanded = expandedQuality,
                                                    onDismissRequest = { expandedQuality = false },
                                                    modifier = Modifier
                                                        .background(Color(0xFF0F0F13))
                                                        .border(1.dp, Color(0xFF2E2E35), RoundedCornerShape(8.dp))
                                                ) {
                                                    val qualities = listOf(
                                                        "Best Available",
                                                        "8K (4320p)",
                                                        "4K (2160p)",
                                                        "2K (1440p)",
                                                        "1080p 60fps",
                                                        "1080p",
                                                        "720p 60fps",
                                                        "720p",
                                                        "Audio Only"
                                                    )
                                                    qualities.forEach { q ->
                                                        DropdownMenuItem(
                                                            text = {
                                                                Text(
                                                                    text = q,
                                                                    color = if (selectedQuality == q) Color(0xFF00F3FF) else Color.LightGray,
                                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.SemiBold
                                                                )
                                                            },
                                                            onClick = {
                                                                selectedQuality = q
                                                                expandedQuality = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(18.dp))

                                        // Custom Pill Segment Storage & Switch Toggle Layout
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .weight(0.58f)
                                                    .height(40.dp)
                                                    .background(Color(0xFF09090B), RoundedCornerShape(20.dp))
                                                    .border(1.dp, Color(0xFF1E1E24), RoundedCornerShape(20.dp))
                                                    .padding(2.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .background(
                                                            brush = if (!isCustomLocation) {
                                                                androidx.compose.ui.graphics.Brush.linearGradient(
                                                                    colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.15f), Color(0xFFBD00FF).copy(alpha = 0.15f))
                                                                )
                                                            } else {
                                                                androidx.compose.ui.graphics.Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent))
                                                            },
                                                            shape = RoundedCornerShape(18.dp)
                                                        )
                                                        .border(
                                                            width = if (!isCustomLocation) 1.dp else 0.dp,
                                                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                                                colors = listOf(Color(0xFF00E5FF), Color(0xFFBD00FF))
                                                            ),
                                                            shape = RoundedCornerShape(18.dp)
                                                        )
                                                        .clickable {
                                                            isCustomLocation = false
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        "Internal Memory",
                                                        color = if (!isCustomLocation) Color(0xFF00F3FF) else Color.Gray,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1
                                                    )
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .background(
                                                            brush = if (isCustomLocation) {
                                                                androidx.compose.ui.graphics.Brush.linearGradient(
                                                                    colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.15f), Color(0xFFBD00FF).copy(alpha = 0.15f))
                                                                )
                                                            } else {
                                                                androidx.compose.ui.graphics.Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent))
                                                            },
                                                            shape = RoundedCornerShape(18.dp)
                                                        )
                                                        .border(
                                                            width = if (isCustomLocation) 1.dp else 0.dp,
                                                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                                                colors = listOf(Color(0xFF00E5FF), Color(0xFFBD00FF))
                                                            ),
                                                            shape = RoundedCornerShape(18.dp)
                                                        )
                                                        .clickable {
                                                            isCustomLocation = true
                                                            if (downloadLocationUri.isEmpty()) {
                                                                downloadLocationLauncher.launch(null)
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        "Choose Custom...",
                                                        color = if (isCustomLocation) Color(0xFF00F3FF) else Color.Gray,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Row(
                                                modifier = Modifier.weight(0.42f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                CyberSwitch(
                                                    checked = setAsPreferredLocation,
                                                    onCheckedChange = { setAsPreferredLocation = it }
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    "Set as preferred location",
                                                    color = Color.LightGray,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 2
                                                )
                                            }
                                        }

                                        if (isCustomLocation && downloadLocationUri.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            val cleanUriDisplay = try {
                                                android.net.Uri.parse(downloadLocationUri).lastPathSegment ?: downloadLocationUri
                                            } catch(e: Exception) { downloadLocationUri }
                                            Text(
                                                "Target URI: $cleanUriDisplay",
                                                color = Color(0xFF00FFCC),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(24.dp))

                                        // Real progress info
                                        var progressVal = 0
                                        var speedVal = "0 B/s"
                                        var statusTxt = "Engine Standby"
                                        val activeState = activeWorkInfo != null && !activeWorkInfo.state.isFinished

                                        activeWorkInfo?.let { work ->
                                            progressVal = work.progress.getInt(DownloadWorker.KEY_PROGRESS, 0)
                                            speedVal = work.progress.getString(DownloadWorker.KEY_SPEED) ?: "Calculating"
                                            statusTxt = work.progress.getString(DownloadWorker.KEY_STATUS_TEXT) ?: "Initiating connection..."
                                        }

                                        // Monospace percentage / speed display panel
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (activeState) "$progressVal%  |  $speedVal" else if (activeWorkInfo != null && activeWorkInfo.state == androidx.work.WorkInfo.State.SUCCEEDED) "100% COMPLETE" else "STANDBY READY",
                                                color = if (activeState) Color(0xFF00F3FF) else Color.LightGray,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                letterSpacing = 1.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Breathtaking Slanted Striped Gradient Progress Bar
                                        androidx.compose.foundation.Canvas(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(24.dp)
                                                .border(BorderStroke(1.2.dp, Color(0xFF2E2E35)), RoundedCornerShape(12.dp))
                                                .clip(RoundedCornerShape(12.dp))
                                        ) {
                                            val barWidth = size.width
                                            val barHeight = size.height
                                            
                                            // 1. Dark background
                                            drawRoundRect(
                                                color = Color(0xFF0C0C0E),
                                                size = size
                                            )
                                            
                                            // 2. Active fill width
                                            val currentProgress = if (activeState) progressVal else if (activeWorkInfo != null && activeWorkInfo.state == androidx.work.WorkInfo.State.SUCCEEDED) 100 else 0
                                            val fillWidth = barWidth * (currentProgress / 100f)
                                            
                                            if (fillWidth > 0f) {
                                                val clipPath = androidx.compose.ui.graphics.Path().apply {
                                                    addRoundRect(
                                                        androidx.compose.ui.geometry.RoundRect(
                                                            rect = androidx.compose.ui.geometry.Rect(0f, 0f, fillWidth, barHeight),
                                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barHeight / 2)
                                                        )
                                                    )
                                                }
                                                
                                                drawContext.canvas.save()
                                                drawContext.canvas.clipPath(clipPath)
                                                
                                                // Draw linear gradient cyan-to-magenta bar
                                                val barGradient = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                    colors = listOf(Color(0xFF00F3FF), Color(0xFFFF007F)),
                                                    startX = 0f,
                                                    endX = barWidth
                                                )
                                                
                                                drawRect(
                                                    brush = barGradient,
                                                    size = androidx.compose.ui.geometry.Size(fillWidth, barHeight)
                                                )
                                                
                                                // Draw retro slanted stripes
                                                val stripeWidth = 6f
                                                val stripeGap = 16f
                                                var xOffset = -barHeight
                                                while (xOffset < fillWidth + barHeight) {
                                                    val stripePath = androidx.compose.ui.graphics.Path().apply {
                                                        moveTo(xOffset, barHeight)
                                                        lineTo(xOffset + stripeWidth, barHeight)
                                                        lineTo(xOffset + stripeWidth + 10f, 0f)
                                                        lineTo(xOffset + 10f, 0f)
                                                        close()
                                                    }
                                                    drawPath(
                                                        path = stripePath,
                                                        color = Color(0xFF000000).copy(alpha = 0.3f)
                                                    )
                                                    xOffset += stripeWidth + stripeGap
                                                }
                                                drawContext.canvas.restore()
                                            }
                                        }

                                        if (activeState || (statusTxt != "Engine Standby" && statusTxt.isNotEmpty())) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = statusTxt,
                                                color = Color.Gray,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.fillMaxWidth(),
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(24.dp))

                                        // DOWNLOAD glowing trigger button
                                        val isReady = downloadUrl.trim().isNotEmpty() && (!isCustomLocation || downloadLocationUri.isNotEmpty())

                                        Button(
                                            onClick = {
                                                if (isReady && !activeState) {
                                                    if (setAsPreferredLocation && isCustomLocation) {
                                                        downloaderPrefs.edit()
                                                            .putString("preferred_location_uri", downloadLocationUri)
                                                            .apply()
                                                    }
                                                    
                                                    val formatArg = qualityMap[selectedQuality] ?: "best"
                                                    
                                                    val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                                                        .setInputData(
                                                            workDataOf(
                                                                DownloadWorker.KEY_URL to downloadUrl.trim(),
                                                                DownloadWorker.KEY_DESTINATION_URI to if (isCustomLocation) downloadLocationUri else "",
                                                                DownloadWorker.KEY_FORMAT to formatArg
                                                            )
                                                        )
                                                        .addTag("ytdlp_download")
                                                        .build()
                                                    WorkManager.getInstance(context).enqueue(request)
                                                }
                                            },
                                            enabled = isReady && !activeState,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.Transparent,
                                                disabledContainerColor = Color.Transparent
                                            ),
                                            border = BorderStroke(
                                                width = 1.5.dp,
                                                color = if (isReady && !activeState) Color(0xFF00F3FF) else Color(0xFF1E1E24)
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp)
                                                .shadow(
                                                    elevation = if (isReady && !activeState) 8.dp else 0.dp,
                                                    spotColor = Color(0xFF00F3FF),
                                                    ambientColor = Color(0xFF00F3FF)
                                                )
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (activeState) Icons.Default.HourglassEmpty else Icons.Default.Download,
                                                    contentDescription = "Trigger Download",
                                                    tint = if (isReady && !activeState) Color(0xFF00F3FF) else Color.DarkGray,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (activeState) "DOWNLOADING..." else "DOWNLOAD",
                                                    color = if (isReady && !activeState) Color.White else Color.Gray,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    letterSpacing = 1.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "More" -> {
                        var autoRescanVal by remember { mutableStateOf(PlayerSettings.getAutoRescan(context)) }
                        var seamlessTransitionsVal by remember { mutableStateOf(PlayerSettings.getSeamlessTransitions(context)) }
                        var isEqEnabled by remember { mutableStateOf(PlayerSettings.getEqualizerEnabled(context)) }
                        var eqValuesMore by remember {
                            mutableStateOf(
                                listOf(
                                    PlayerSettings.getEqualizerBand(context, 0),
                                    PlayerSettings.getEqualizerBand(context, 1),
                                    PlayerSettings.getEqualizerBand(context, 2),
                                    PlayerSettings.getEqualizerBand(context, 3),
                                    PlayerSettings.getEqualizerBand(context, 4)
                                )
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Card 1: Media Library (Matching user image description)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Media Library",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFF3B82F6),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Media library folders
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { folderPickerLauncher.launch(null) }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF9CA3AF))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Media library folders", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text("Select directories to include in the media library", color = Color(0xFF9CA3AF), style = MaterialTheme.typography.bodySmall)
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF9CA3AF))
                                    }

                                    HorizontalDivider(color = Color(0xFF262626))

                                    // Auto rescan
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.YoutubeSearchedFor, contentDescription = null, tint = Color(0xFF9CA3AF))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Auto rescan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text("Automatically scan device for new or deleted media at application startup", color = Color(0xFF9CA3AF), style = MaterialTheme.typography.bodySmall)
                                        }
                                        Switch(
                                            checked = autoRescanVal,
                                            onCheckedChange = {
                                                autoRescanVal = it
                                                PlayerSettings.setAutoRescan(context, it)
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color(0xFF3B82F6),
                                                checkedTrackColor = Color(0xFF3B82F6).copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                }
                            }

                            // Card 2: Playback Options (Seamless transition)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Playback settings",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFF9CA3AF),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Loop, contentDescription = null, tint = Color(0xFF9CA3AF))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Seamless transitions", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text("Enable seamless gapless transitions between track playback structures", color = Color(0xFF9CA3AF), style = MaterialTheme.typography.bodySmall)
                                        }
                                        Switch(
                                            checked = seamlessTransitionsVal,
                                            onCheckedChange = {
                                                seamlessTransitionsVal = it
                                                PlayerSettings.setSeamlessTransitions(context, it)
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color(0xFF3B82F6),
                                                checkedTrackColor = Color(0xFF3B82F6).copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                }
                            }

                            // Card 3: Internal Sound Equalizer (Graphic equalizers)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Internal Sound Equalizer", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                            Text("Fine-tune acoustic frequency details", style = MaterialTheme.typography.bodySmall, color = Color(0xFF9CA3AF))
                                        }
                                        Switch(
                                            checked = isEqEnabled,
                                            onCheckedChange = {
                                                isEqEnabled = it
                                                PlayerSettings.setEqualizerEnabled(context, it)
                                                eqEnabled = it
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color(0xFF3B82F6),
                                                checkedTrackColor = Color(0xFF3B82F6).copy(alpha = 0.5f)
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "Acoustic Equalizer Presets",
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    // Horizontally Scrollable Preset Selectors
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val presets = listOf(
                                            "Flat" to listOf(0f, 0f, 0f, 0f, 0f),
                                            "Bass Boost" to listOf(10f, 7f, 1f, 0f, -3f),
                                            "Vocal" to listOf(-4f, 1f, 8f, 7f, 2f),
                                            "Rock" to listOf(6f, 3f, -2f, 3f, 7f),
                                            "Jazz" to listOf(4f, 2f, -3f, 2f, 5f),
                                            "Classical" to listOf(4f, 2f, -1f, 3f, 4f)
                                        )
                                        presets.forEach { (presetName, presetVals) ->
                                            val isCurrentPreset = eqValuesMore.map { "%.1f".format(it) } == presetVals.map { "%.1f".format(it) }
                                            Button(
                                                onClick = {
                                                    if (isEqEnabled) {
                                                        eqValuesMore = presetVals
                                                        presetVals.forEachIndexed { idx, valDb ->
                                                            PlayerSettings.setEqualizerBand(context, idx, valDb)
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isCurrentPreset) Color(0xFF3B82F6) else Color(0xFF1A1A1A)
                                                ),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.height(32.dp),
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Text(presetName, fontSize = 12.sp, color = if (isCurrentPreset) Color.Black else Color.White, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "5-Band Graphic Equalizer",
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    // Graphic Equalizer row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, Color(0xFF262626), RoundedCornerShape(12.dp))
                                            .background(Color(0xFF000000), RoundedCornerShape(12.dp))
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        val frequencies = listOf("60 Hz", "230 Hz", "910 Hz", "4 kHz", "14 kHz")
                                        frequencies.forEachIndexed { index, bandFreqLabel ->
                                            VerticalEqualizerSlider(
                                                value = eqValuesMore[index],
                                                enabled = isEqEnabled,
                                                onValueChange = { newVal ->
                                                    val updated = eqValuesMore.toMutableList()
                                                    updated[index] = newVal
                                                    eqValuesMore = updated
                                                    PlayerSettings.setEqualizerBand(context, index, newVal)
                                                },
                                                label = bandFreqLabel
                                            )
                                        }
                                    }
                                }
                            }

                            // Card 4: Acoustic Special Enhancements
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF141416))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Acoustic Special Enhancements", style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text("Bass Boost Depth: ${String.format("%.0f", baseBoostVal)}%", color = Color(0xFF9CA3AF), style = MaterialTheme.typography.bodySmall)
                                    Slider(
                                        value = baseBoostVal,
                                        onValueChange = { if (isEqEnabled) baseBoostVal = it },
                                        valueRange = 0f..100f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFF3B82F6),
                                            activeTrackColor = Color(0xFF3B82F6),
                                            inactiveTrackColor = Color(0xFF262626)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text("Virtualizer Depth: ${String.format("%.0f", surroundDepthVal)}%", color = Color(0xFF9CA3AF), style = MaterialTheme.typography.bodySmall)
                                    Slider(
                                        value = surroundDepthVal,
                                        onValueChange = { if (isEqEnabled) surroundDepthVal = it },
                                        valueRange = 0f..100f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFF3B82F6),
                                            activeTrackColor = Color(0xFF3B82F6),
                                            inactiveTrackColor = Color(0xFF262626)
                                        )
                                    )
                                }
                            }

                            // Card 5: Audio Quality Specifications
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF141416))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Audio Quality Specifications", style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("• Hi-Res Lossless: WAV or FLAC containing sample rate >48kHz (up to 192kHz) and bitdepth ≥ 24-bit.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("• Lossless: WAV, FLAC, or ALAC containing CD specification 16-bit / 44.1kHz sample structures.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedVideoForInfo != null) {
        VideoDetailSheet(
            video = selectedVideoForInfo!!,
            onDismiss = { selectedVideoForInfo = null },
            onDeleteSelected = {
                viewModel.deleteVideo(it.uriString)
                selectedVideoForInfo = null
            }
        )
    }

    if (selectedFolderForActions != null) {
        FolderActionsDialog(
            folderName = selectedFolderForActions!!.first,
            videosCount = selectedFolderForActions!!.second.size,
            onPlayAll = {
                viewModel.playFolderPlaylist(com.example.ui.viewmodel.FolderPlaylist(selectedFolderForActions!!.first, selectedFolderForActions!!.second))
                selectedFolderForActions = null
            },
            onQueueAll = {
                viewModel.queueFolderPlaylist(com.example.ui.viewmodel.FolderPlaylist(selectedFolderForActions!!.first, selectedFolderForActions!!.second))
                selectedFolderForActions = null
            },
            onExclude = {
                viewModel.excludeFolder(selectedFolderForActions!!.first)
                selectedFolderForActions = null
            },
            onHide = {
                viewModel.hideFolder(selectedFolderForActions!!.first)
                selectedFolderForActions = null
            },
            onDismiss = { selectedFolderForActions = null }
        )
    }

    if (quickPlayVideo != null) {
        QuickPlayPopupDialog(
            video = quickPlayVideo!!,
            onFullScreen = {
                val targetVideo = quickPlayVideo!!
                quickPlayVideo = null
                viewModel.selectVideo(targetVideo, filteredVideos)
            },
            onFavoriteToggle = {
                viewModel.toggleFavorite(quickPlayVideo!!)
            },
            onDeleteSelected = {
                viewModel.deleteVideo(quickPlayVideo!!.uriString)
                quickPlayVideo = null
            },
            onDismiss = { quickPlayVideo = null }
        )
    }

    if (showFullAudioPlayerSheet && playingAudio != null) {
        AudioPlayerSheet(
            audio = playingAudio!!,
            audioPlayer = audioPlayer,
            isAudioPlaying = isAudioPlaying,
            audioPositionProvider = { audioPosition },
            audioDuration = audioDuration,
            onDismiss = { showFullAudioPlayerSheet = false },
            viewModel = viewModel
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerSheet(
    audio: AudioEntity,
    audioPlayer: ExoPlayer,
    isAudioPlaying: Boolean,
    audioPositionProvider: () -> Long,
    audioDuration: Long,
    onDismiss: () -> Unit,
    viewModel: VideoPlayerViewModel
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .drawBehind {
                    // Heavily blurred dynamic artwork representation overlay (opacity 12%)
                    val mainColor = if (audio.isLossless) Color(0xFF14B8A6) else primaryColor
                    val secondColor = if (audio.isLossless) primaryColor else Color(0xFF14B8A6)
                    
                    val brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            mainColor.copy(alpha = 0.14f),
                            secondColor.copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 3f),
                        radius = size.width * 0.9f
                    )
                    drawRect(brush = brush)
                }
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant glowing record indicator / Vinyl artwork replica
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .drawBehind {
                        drawCircle(
                            color = if (isAudioPlaying) primaryColor.copy(alpha = 0.12f) else Color.Transparent,
                            radius = size.width / 2 + 16.dp.toPx()
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Vinyl outer rings
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .clip(RoundedCornerShape(85.dp))
                        .background(Color(0xFF0A0A0A)),
                    contentAlignment = Alignment.Center
                ) {
                    // Center label
                    Surface(
                        modifier = Modifier
                            .size(70.dp),
                        color = Color(0xFF1A1A1A),
                        shape = RoundedCornerShape(35.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = if (audio.isLossless) Color(0xFF14B8A6) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Song labels
            Text(
                text = audio.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = audio.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Lossless spec representation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (audio.isHiResLossless) {
                    Text(
                        text = "Hi-Res Lossless",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF14B8A6),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color(0xFF0F2D2A), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                } else if (audio.isLossless) {
                    Text(
                        text = "Lossless",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFBAC0C4),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color(0xFF141A1D), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                } else {
                    Text(
                        text = "Standard Audio",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        modifier = Modifier
                            .background(Color(0xFF1A1A1A), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Format identifier badge (FLAC, MP3, etc)
                Text(
                    text = audio.format.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier
                        .background(Color(0xFF3B82F6).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            AudioSeekControls(
                audioDuration = audioDuration,
                audioPositionProvider = audioPositionProvider,
                audioPlayer = audioPlayer
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Audio Player Controls buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Favorite Button
                IconButton(
                    onClick = { viewModel.toggleAudioFavorite(audio) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (audio.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle favorite",
                        tint = if (audio.isFavorite) Color.Red else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Skip Previous button
                IconButton(
                    onClick = { viewModel.playPreviousAudio() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Play / Pause round action button
                Surface(
                    onClick = {
                        if (isAudioPlaying) {
                            audioPlayer.pause()
                        } else {
                            audioPlayer.play()
                        }
                    },
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = Color(0xFF3B82F6)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Skip Next button
                IconButton(
                    onClick = { viewModel.playNextAudio() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Close Audio Player fully or Dismiss sheet
                IconButton(
                    onClick = {
                        viewModel.closeAudioPlayer()
                        onDismiss()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close player",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Tech Specifications Box details
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Stream Details",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Frequency", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("${audio.sampleRate / 1000.0} kHz", style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Precision", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("${audio.bitDepth} bit", style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Format Codec", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(audio.format.uppercase(), style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("File size", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(audio.sizeFormatted, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateLayout(
    selectedTab: Int,
    searchQuery: String,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onPickFolder: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = Color(0xFF141416),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val icon = when {
                        searchQuery.isNotEmpty() -> Icons.Default.SearchOff
                        selectedTab == 1 -> Icons.Outlined.History
                        selectedTab == 2 -> Icons.Outlined.FavoriteBorder
                        else -> Icons.Outlined.VideoLibrary
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val title = when {
                searchQuery.isNotEmpty() -> "No matching results"
                selectedTab == 1 -> "No playback history"
                selectedTab == 2 -> "No favorites added yet"
                !hasPermission -> "Storage permission required"
                else -> "No videos scanned"
            }

            val desc = when {
                searchQuery.isNotEmpty() -> "We couldn't check any videos representing \"$searchQuery\"."
                selectedTab == 1 -> "Play some video files, we'll track resume logs inside Recents."
                selectedTab == 2 -> "Tap the heart indicator on any item to save your shortcuts."
                !hasPermission -> "Permit scanning so MhdxBilal can list video indexes automatically."
                else -> "Select specific folders or run an automated Storage scan."
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (!hasPermission && searchQuery.isEmpty() && selectedTab == 0) {
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enable Access")
                }
            } else if (searchQuery.isEmpty()) {
                Button(
                    onClick = onPickFolder,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F1F24))
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color.LightGray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Media Directory", color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoItemRow(
    video: VideoEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onInfoRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extension = remember(video.title) {
        val dotIndex = video.title.lastIndexOf('.')
        if (dotIndex != -1 && dotIndex < video.title.length - 1) {
            val ext = video.title.substring(dotIndex + 1).lowercase()
            if (ext == "mp4" || ext == "mkv" || ext == "avi" || ext == "webm" || ext == "mov" || ext == "3gp") {
                ext.uppercase()
            } else {
                "VIDEO"
            }
        } else {
            "MP4"
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                )
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .tilt3DInteractive(interactionSource)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .testTag("video_row_card_${video.title}")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Raised 3D-like glass-bordered play thumbnail
                Surface(
                    color = Color(0xFF141416),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.White.copy(alpha = 0.02f)
                            )
                        )
                    ),
                    modifier = Modifier
                        .size(width = 64.dp, height = 48.dp)
                        .graphicsLayer {
                            shadowElevation = 4f
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val modelSrc = if (!video.thumbnailPath.isNullOrBlank()) {
                            video.thumbnailPath
                        } else {
                            video.path ?: video.uriString
                        }
                        coil.compose.AsyncImage(
                            model = modelSrc,
                            contentDescription = "Video Thumbnail",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        // Top start: format badge pill
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(3.dp),
                            contentAlignment = Alignment.TopStart
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(3.dp)
                            ) {
                                Text(
                                    text = extension,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                )
                            }
                        }

                        // Bottom end: duration badge
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(3.dp),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.82f),
                                shape = RoundedCornerShape(3.dp)
                            ) {
                                Text(
                                    text = video.durationFormatted,
                                    fontSize = 8.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = video.sizeFormatted,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 11.sp
                        )

                        if (video.path != null) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Text(
                                text = "Local File",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Favorite switch button
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite status toggle",
                        tint = if (video.isFavorite) Color.Red else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onInfoRequested) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Details",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Beautiful Resume progress line if video was played recently
            if (video.lastPlayedPosition > 0 && video.duration > 0) {
                val progressFraction = (video.lastPlayedPosition.toFloat() / video.duration.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color(0xFF1F1F1F))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailSheet(
    video: VideoEntity,
    onDismiss: () -> Unit,
    onDeleteSelected: (VideoEntity) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101012),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.DarkGray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Multimedia Properties",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            DetailRow(label = "Filename", value = video.title)
            DetailRow(label = "Length", value = video.durationFormatted)
            DetailRow(label = "File size", value = video.sizeFormatted)
            DetailRow(label = "Last Saved Playback Pos", value = if (video.lastPlayedPosition > 0L) formatTime(video.lastPlayedPosition) else "Unplayed / New Start")
            DetailRow(label = "Storage Scheme", value = if (video.path != null) "MediaStore Device" else "Scoped Document URI")
            if (video.path != null) {
                DetailRow(label = "Absolute Path", value = video.path)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onDeleteSelected(video) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("delete_source_btn")
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete from Index")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = Color.White, maxLines = 3, overflow = TextOverflow.Ellipsis)
        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun FolderPlaylistItemRow(
    folder: com.example.ui.viewmodel.FolderPlaylist,
    viewModel: VideoPlayerViewModel,
    onPlayFolder: () -> Unit,
    onQueueFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101012)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${folder.videos.size} videos",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                IconButton(onClick = onPlayFolder) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Folder",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onQueueFolder) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = "Queue Folder",
                        tint = Color.LightGray
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(bottom = 12.dp))
                    folder.videos.forEach { video ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.selectVideo(video, folder.videos)
                                }
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = video.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = video.durationFormatted,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyFoldersStateLayout(
    onImportFolder: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = Color(0xFF141416),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No folders identified",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Run an automated check or tap below to select and import any device folder as a playlist.",
                style = androidx.compose.ui.text.TextStyle(
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onImportFolder,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Folder Playlist")
            }
        }
    }
}

@Composable
fun FolderActionsDialog(
    folderName: String,
    videosCount: Int,
    onPlayAll: () -> Unit,
    onQueueAll: () -> Unit,
    onExclude: () -> Unit,
    onHide: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF161619),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = folderName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$videosCount media items",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF222225))

                // Options
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FolderActionItem(
                        icon = Icons.Default.PlayArrow,
                        title = "Play All Videos",
                        description = "Start playback of all videos in this folder consecutively",
                        color = Color.White,
                        onClick = onPlayAll
                    )

                    FolderActionItem(
                        icon = Icons.Default.PlaylistAdd,
                        title = "Queue All Videos",
                        description = "Add all videos in this folder to the end of the queue",
                        color = Color.White,
                        onClick = onQueueAll
                    )

                    FolderActionItem(
                        icon = Icons.Default.VisibilityOff,
                        title = "Hide Folder from Library",
                        description = "Temporarily hide this folder's items from the dashboards",
                        color = Color.White,
                        onClick = onHide
                    )

                    FolderActionItem(
                        icon = Icons.Default.DeleteForever,
                        title = "Exclude Folder from Media Library",
                        description = "Add to excluded scanning directories and delete from DB",
                        color = Color(0xFFFF5252),
                        onClick = onExclude
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Dismiss", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun FolderActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (color == Color.White) MaterialTheme.colorScheme.primary else color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(text = description, color = Color.Gray, fontSize = 11.sp)
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoItemGrid(
    video: VideoEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onInfoRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extension = remember(video.title) {
        val dotIndex = video.title.lastIndexOf('.')
        if (dotIndex != -1 && dotIndex < video.title.length - 1) {
            val ext = video.title.substring(dotIndex + 1).lowercase()
            if (ext == "mp4" || ext == "mkv" || ext == "avi" || ext == "webm" || ext == "mov" || ext == "3gp") {
                ext.uppercase()
            } else {
                "VIDEO"
            }
        } else {
            "MP4"
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                )
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .tilt3DInteractive(interactionSource)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .testTag("video_grid_card_${video.title}")
    ) {
        Column {
            // Raised 3D-like glass-bordered play thumbnail
            Surface(
                color = Color(0xFF141416),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                border = BorderStroke(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .graphicsLayer {
                        shadowElevation = 4f
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val modelSrc = if (!video.thumbnailPath.isNullOrBlank()) {
                        video.thumbnailPath
                    } else {
                        video.path ?: video.uriString
                    }
                    coil.compose.AsyncImage(
                        model = modelSrc,
                        contentDescription = "Video Thumbnail",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    
                    // Top start: format badge pill
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                text = extension,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Bottom end: duration badge
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.82f),
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                text = video.durationFormatted,
                                fontSize = 9.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Title and description row area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.heightIn(min = 36.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = video.sizeFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontSize = 11.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onFavoriteToggle,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite status toggle",
                                tint = if (video.isFavorite) Color.Red else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = onInfoRequested,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Details",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Progress bar
            if (video.lastPlayedPosition > 0 && video.duration > 0) {
                val progressFraction = (video.lastPlayedPosition.toFloat() / video.duration.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color(0xFF1F1F1F))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplaySettingsBottomSheet(
    displayViewMode: String,
    showOnlyFavorites: Boolean,
    playbackAction: String,
    sortField: String,
    sortOrder: String,
    groupBy: String,
    onViewModeChanged: (String) -> Unit,
    onShowOnlyFavoritesChanged: (Boolean) -> Unit,
    onPlaybackActionChanged: (String) -> Unit,
    onSortChanged: (String, String) -> Unit,
    onGroupByChanged: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161618),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.DarkGray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // Section 1: Display settings
            Text(
                text = "Display settings",
                color = MaterialTheme.colorScheme.primary, // Orange/Accent
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Display in list / grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onViewModeChanged(if (displayViewMode == "list") "grid" else "list")
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (displayViewMode == "list") Icons.Default.List else Icons.Default.GridView,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = if (displayViewMode == "list") "Display in list" else "Display in grid",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Show only favourites
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onShowOnlyFavoritesChanged(!showOnlyFavorites)
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (showOnlyFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (showOnlyFavorites) Color.Red else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Show only favourites",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Checkbox(
                    checked = showOnlyFavorites,
                    onCheckedChange = { onShowOnlyFavoritesChanged(it) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = Color.LightGray
                    )
                )
            }

            // Playback action
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { dropdownExpanded = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Playback action",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Videos",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val actionLabel = when (playbackAction) {
                            "play" -> "Play"
                            "play_all" -> "Play all"
                            "add_queue" -> "Add to play queue"
                            "insert_next" -> "Insert next"
                            else -> "Play all"
                        }
                        Text(
                            text = actionLabel,
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.background(Color(0xFF242426))
                ) {
                    val actions = listOf(
                        "play" to "Play",
                        "play_all" to "Play all",
                        "add_queue" to "Add to play queue",
                        "insert_next" to "Insert next"
                    )
                    actions.forEach { (actionKey, labelStr) ->
                        DropdownMenuItem(
                            text = { Text(labelStr, color = Color.White) },
                            onClick = {
                                onPlaybackActionChanged(actionKey)
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(16.dp))

            // Section: Preferences
            Text(
                text = "Preferences",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Grouping settings list dropdown
            var groupDropdownExpanded by remember { mutableStateOf(false) }
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { groupDropdownExpanded = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GroupWork,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Group videos",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Organize library structure",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val groupLabel = when (groupBy) {
                            "none" -> "Don't Group"
                            "folder" -> "By Folder"
                            "name" -> "By Name (A-Z)"
                            else -> "Don't Group"
                        }
                        Text(
                            text = groupLabel,
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                }

                DropdownMenu(
                    expanded = groupDropdownExpanded,
                    onDismissRequest = { groupDropdownExpanded = false },
                    modifier = Modifier.background(Color(0xFF242426))
                ) {
                    val groupOptions = listOf(
                        "none" to "Don't Group",
                        "folder" to "By Folder",
                        "name" to "By Name (A-Z)"
                    )
                    groupOptions.forEach { (optionKey, labelStr) ->
                        DropdownMenuItem(
                            text = { Text(labelStr, color = Color.White) },
                            onClick = {
                                onGroupByChanged(optionKey)
                                groupDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(16.dp))

            // Section 2: Sort by...
            Text(
                text = "Sort by...",
                color = MaterialTheme.colorScheme.primary, // Orange/Accent
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Row helper
            SortOptionItem(
                icon = Icons.Default.SortByAlpha,
                title = "Name",
                fieldKey = "name",
                sortField = sortField,
                sortOrder = sortOrder,
                options = listOf(
                    "asc" to "A -> Z",
                    "desc" to "Z -> A"
                ),
                onSortChanged = onSortChanged
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

            SortOptionItem(
                icon = Icons.Default.Description,
                title = "File name",
                fieldKey = "file_name",
                sortField = sortField,
                sortOrder = sortOrder,
                options = listOf(
                    "asc" to "A -> Z",
                    "desc" to "Z -> A"
                ),
                onSortChanged = onSortChanged
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

            SortOptionItem(
                icon = Icons.Default.Timer,
                title = "Length",
                fieldKey = "length",
                sortField = sortField,
                sortOrder = sortOrder,
                options = listOf(
                    "asc" to "Shortest first",
                    "desc" to "Longest first"
                ),
                onSortChanged = onSortChanged
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

            SortOptionItem(
                icon = Icons.Default.CalendarToday,
                title = "Insertion date",
                fieldKey = "added_date",
                sortField = sortField,
                sortOrder = sortOrder,
                options = listOf(
                    "asc" to "Oldest first",
                    "desc" to "Newest first"
                ),
                onSortChanged = onSortChanged
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

            SortOptionItem(
                icon = Icons.Default.SdStorage,
                title = "Size",
                fieldKey = "size",
                sortField = sortField,
                sortOrder = sortOrder,
                options = listOf(
                    "asc" to "Smallest first",
                    "desc" to "Largest first"
                ),
                onSortChanged = onSortChanged
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun QuickPlayPopupDialog(
    video: VideoEntity,
    onFullScreen: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDeleteSelected: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(true) }
    var isLooping by remember { mutableStateOf(true) }
    var playSpeed by remember { mutableStateOf(1.0f) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var aiSummaryText by remember { mutableStateOf("") }
    var isLoadingSummary by remember { mutableStateOf(false) }

    // local ExoPlayer instance for quick view with mute enabled by default
    val exoPlayer = remember {
        com.example.util.MediaCacheManager.buildExoPlayer(context).apply {
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
            val mediaItem = androidx.media3.common.MediaItem.fromUri(video.uriString)
            setMediaItem(mediaItem)
            prepare()
            volume = 0f
            playWhenReady = true
        }
    }

    LaunchedEffect(Unit) {
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(isLooping) {
        exoPlayer.repeatMode = if (isLooping) {
            androidx.media3.common.Player.REPEAT_MODE_ALL
        } else {
            androidx.media3.common.Player.REPEAT_MODE_OFF
        }
    }

    LaunchedEffect(playSpeed) {
        exoPlayer.setPlaybackSpeed(playSpeed)
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) exoPlayer.play() else exoPlayer.pause()
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    // Interactive 3D Touch Peek Dialog Layout with multiple action shortcuts
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1: 3D Touch Video Preview Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        shadowElevation = 15f
                    }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "3D Touch Peek Preview",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Simulated 3D Video Screen with rounded boundaries
                    Surface(
                        color = Color.Black,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .graphicsLayer {
                                shadowElevation = 10f
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            androidx.compose.ui.viewinterop.AndroidView(
                                factory = { ctx ->
                                    androidx.media3.ui.PlayerView(ctx).apply {
                                        player = exoPlayer
                                        useController = false
                                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Glass shine diagonal highlight
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .drawBehind {
                                        val brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.08f),
                                                Color.Transparent,
                                                Color.White.copy(alpha = 0.03f)
                                            )
                                        )
                                        drawRect(brush = brush)
                                    }
                            )

                            // Muted status watermarking indicator
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                contentAlignment = Alignment.BottomStart
                            ) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isMuted) "MUTED PREVIEW" else "SOUND ON",
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = video.title,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${video.sizeFormatted} • ${video.durationFormatted}",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Inline controls row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { 
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            isPlaying = !isPlaying 
                        }) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                contentDescription = "Toggle play-pause state",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(onClick = { 
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            isMuted = !isMuted 
                        }) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "Mute audio",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(onClick = { 
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            isLooping = !isLooping 
                        }) {
                            Icon(
                                imageVector = Icons.Default.Loop,
                                contentDescription = "Loop switcher",
                                tint = if (isLooping) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            playSpeed = if (playSpeed == 1.0f) 1.5f else if (playSpeed == 1.5f) 2.0f else 1.0f
                        }) {
                            Surface(
                                color = Color(0xFF1E1E22),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${playSpeed}x",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Display generator summary box if requested and calculated
                    if (aiSummaryText.isNotEmpty() || isLoadingSummary) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = Color(0xFF1B1B1F),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp).offset(y = 1.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (isLoadingSummary) {
                                    Text(
                                        text = "Synthesizing AI context synopsis...",
                                        color = MaterialTheme.colorScheme.secondary,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    Text(
                                        text = aiSummaryText,
                                        color = Color(0xFFCBD5E1),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Card 2: 3D Touch iOS-Style Contextual Shortcuts Menu
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        shadowElevation = 12f
                    }
            ) {
                Column {
                    // Item 1: Fullscreen launcher
                    ShortcutMenuItem(
                        icon = Icons.Default.Fullscreen,
                        title = "Open Full Screen Player",
                        subtitle = "Launch standard playback flow",
                        iconColor = MaterialTheme.colorScheme.primary,
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onFullScreen()
                        }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                    // Item 2: Favorite Trigger Toggle
                    ShortcutMenuItem(
                        icon = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        title = if (video.isFavorite) "Remove from Favorites" else "Mark as Favorite",
                        subtitle = if (video.isFavorite) "Exclude from starred index" else "Keep handy in favorites tab",
                        iconColor = if (video.isFavorite) Color.Red else Color.White.copy(alpha = 0.7f),
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onFavoriteToggle()
                        }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                    // Item 4: Delete File Shortcut
                    ShortcutMenuItem(
                        icon = Icons.Default.Delete,
                        title = "Delete File Permanently",
                        subtitle = "Erase video file from local device",
                        iconColor = Color.Red,
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onDeleteSelected()
                        }
                    )
                }
            }
        }
    }
}

// Reusable iOS style Menu Button representing the modern 3D touch look
@Composable
fun ShortcutMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Physics 3D Interactive Touch simulation tilt Modifier
@Composable
fun Modifier.tilt3DInteractive(
    interactionSource: MutableInteractionSource,
    maxTilt: Float = 6f,
    scalePercent: Float = 0.96f
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    var width by remember { mutableStateOf(0f) }
    var height by remember { mutableStateOf(0f) }

    var lastTouchX by remember { mutableStateOf(0f) }
    var lastTouchY by remember { mutableStateOf(0f) }

    val animatedRotationX by animateFloatAsState(
        targetValue = if (isPressed && width > 0 && height > 0) {
            val touchY = if (lastTouchY == 0f) height / 2 else lastTouchY
            ((touchY - height / 2) / (height / 2)) * -maxTilt
        } else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 160f),
        label = "rotX"
    )
    val animatedRotationY by animateFloatAsState(
        targetValue = if (isPressed && width > 0 && height > 0) {
            val touchX = if (lastTouchX == 0f) width / 2 else lastTouchX
            ((touchX - width / 2) / (width / 2)) * maxTilt
        } else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 160f),
        label = "rotY"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) scalePercent else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 220f),
        label = "scale"
    )

    return this
        .onGloballyPositioned { coordinates ->
            width = coordinates.size.width.toFloat()
            height = coordinates.size.height.toFloat()
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = { offset ->
                    lastTouchX = offset.x
                    lastTouchY = offset.y
                    tryAwaitRelease()
                }
            )
        }
        .graphicsLayer {
            rotationX = animatedRotationX
            rotationY = animatedRotationY
            scaleX = animatedScale
            scaleY = animatedScale
            cameraDistance = 12f * density
        }
}

@Composable
fun SortOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    fieldKey: String,
    sortField: String,
    sortOrder: String,
    options: List<Pair<String, String>>,
    onSortChanged: (String, String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.width(140.dp)
        ) {
            options.forEach { (orderKey, labelStr) ->
                val isSelected = (sortField == fieldKey && sortOrder == orderKey)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSortChanged(fieldKey, orderKey) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = labelStr,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AudioSeekControls(
    audioDuration: Long,
    audioPositionProvider: () -> Long,
    audioPlayer: androidx.media3.exoplayer.ExoPlayer
) {
    val currentAudioPosition = audioPositionProvider()

    // Seek progress bar / slider implementation
    var sliderPosition by remember(currentAudioPosition) { mutableFloatStateOf(currentAudioPosition.toFloat()) }
    var isUserSeeking by remember { mutableStateOf(false) }

    Slider(
        value = if (isUserSeeking) sliderPosition else currentAudioPosition.toFloat(),
        onValueChange = {
            isUserSeeking = true
            sliderPosition = it
        },
        onValueChangeFinished = {
            audioPlayer.seekTo(sliderPosition.toLong())
            isUserSeeking = false
        },
        valueRange = 0f..audioDuration.toFloat().coerceAtLeast(1f),
        colors = SliderDefaults.colors(
            thumbColor = Color(0xFF3B82F6),
            activeTrackColor = Color(0xFF3B82F6),
            inactiveTrackColor = Color(0xFF262626)
        ),
        modifier = Modifier.fillMaxWidth()
    )

    // Current Time / Duration Text indicators
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val currentSecs = if (isUserSeeking) sliderPosition.toLong() / 1000 else currentAudioPosition / 1000
        val totalSecs = audioDuration / 1000
        Text(
            text = String.format("%02d:%02d", currentSecs / 60, currentSecs % 60),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Text(
            text = String.format("%02d:%02d", totalSecs / 60, totalSecs % 60),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
fun CyberSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val bias by androidx.compose.animation.core.animateFloatAsState(targetValue = if (checked) 1f else -1f)
    val alignment = remember(bias) { androidx.compose.ui.BiasAlignment(horizontalBias = bias, verticalBias = 0f) }
    
    Box(
        modifier = Modifier
            .width(52.dp)
            .height(26.dp)
            .background(
                color = if (checked) Color(0xFF00F3FF).copy(alpha = 0.15f) else Color(0xFF141416),
                shape = RoundedCornerShape(13.dp)
            )
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    color = if (checked) Color(0xFF00F3FF) else Color(0xFF2E2E35)
                ),
                shape = RoundedCornerShape(13.dp)
            )
            .clickable { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (checked) Arrangement.Start else Arrangement.End
        ) {
            Text(
                text = if (checked) "ON" else "OFF",
                color = if (checked) Color(0xFF00F3FF) else Color.Gray,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
        Box(
            modifier = Modifier
                .align(alignment)
                .size(20.dp)
                .background(
                    color = if (checked) Color(0xFF00F3FF) else Color.Gray,
                    shape = RoundedCornerShape(10.dp)
                )
        )
    }
}
