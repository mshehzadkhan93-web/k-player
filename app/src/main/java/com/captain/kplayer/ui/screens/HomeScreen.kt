package com.captain.kplayer.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.captain.kplayer.data.QueueEntry
import com.captain.kplayer.data.StreamItem
import com.captain.kplayer.data.VideoItem
import com.captain.kplayer.data.formatDuration
import com.captain.kplayer.ui.components.AccentGradientButton
import com.captain.kplayer.ui.components.ArtImage
import com.captain.kplayer.ui.components.ArtKind
import com.captain.kplayer.ui.components.Badge
import com.captain.kplayer.ui.components.GlassBottomBar
import com.captain.kplayer.ui.components.GlassCard
import com.captain.kplayer.ui.components.GlassProgressBar
import com.captain.kplayer.ui.components.GlassTextField
import com.captain.kplayer.ui.components.HomeTab
import com.captain.kplayer.ui.navigation.Routes
import com.captain.kplayer.ui.theme.GradientBackground
import com.captain.kplayer.ui.theme.LocalAccent
import com.captain.kplayer.ui.theme.TextMuted
import com.captain.kplayer.ui.theme.TextPrimary
import com.captain.kplayer.ui.theme.TextSecondary
import com.captain.kplayer.viewmodel.LibraryViewModel
import com.captain.kplayer.viewmodel.PlayerViewModel
import com.captain.kplayer.viewmodel.SettingsViewModel
import com.captain.kplayer.viewmodel.VideoSort
import com.captain.kplayer.viewmodel.VideoViewMode

/** Home / Library screen with the glass bottom navigation. */
@Composable
fun HomeScreen(
    navController: NavHostController,
    libraryVm: LibraryViewModel,
    playerVm: PlayerViewModel,
    settingsVm: SettingsViewModel
) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(HomeTab.VIDEOS) }
    val query by libraryVm.searchQuery.collectAsState()
    val usingDeviceMedia by libraryVm.usingDeviceMedia.collectAsState()
    val currentEntry by playerVm.currentEntry.collectAsState()
    val isPlaying by playerVm.isPlaying.collectAsState()
    val position by playerVm.positionMs.collectAsState()
    val duration by playerVm.durationMs.collectAsState()
    val showHiddenFiles by settingsVm.showHiddenFiles.collectAsState()
    val defaultView by settingsVm.defaultView.collectAsState()
    val defaultSort by settingsVm.defaultSort.collectAsState()

    val permissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    var hasPermission by remember {
        mutableStateOf(
            permissions.all {
                ContextCompat.checkSelfPermission(context, it) ==
                    PackageManager.PERMISSION_GRANTED
            }
        )
    }
    var permissionRequested by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasPermission = result.values.all { it }
        permissionRequested = true
        if (hasPermission) libraryVm.scanDeviceMedia(context, showHiddenFiles)
    }
    // Re-scans when the "Show hidden files" setting changes, and applies the
    // persisted default view / sort order from Settings > Interface.
    LaunchedEffect(showHiddenFiles) {
        if (hasPermission) libraryVm.scanDeviceMedia(context, showHiddenFiles)
    }
    LaunchedEffect(defaultView, defaultSort) {
        libraryVm.applyDefaults(defaultView, defaultSort)
    }

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 48.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "K-Player",
                    color = TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                if (usingDeviceMedia) {
                    Badge(text = "Your files", gradient = true)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            GlassTextField(
                value = query,
                onValueChange = { libraryVm.setSearchQuery(it) },
                placeholder = "Search videos, music, folders…",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = TextMuted
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (!hasPermission) {
                GlassCard(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Your media, in one place",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Allow K-Player to read your videos and music so your downloaded files show up here.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AccentGradientButton(
                            text = "Allow access",
                            onClick = { permissionLauncher.launch(permissions) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (permissionRequested) {
                            Text(
                                text = "Permission was denied. You can enable it anytime in App settings.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                when (tab) {
                    HomeTab.VIDEOS -> VideosTab(
                        libraryVm = libraryVm,
                        playerVm = playerVm,
                        navController = navController,
                        showSampleHint = !usingDeviceMedia
                    )
                    HomeTab.MUSIC -> MusicTab(
                        libraryVm = libraryVm,
                        playerVm = playerVm,
                        navController = navController
                    )
                    HomeTab.FOLDERS -> FoldersTab(libraryVm = libraryVm)
                    HomeTab.NETWORK -> NetworkTab(
                        libraryVm = libraryVm,
                        playerVm = playerVm,
                        navController = navController
                    )
                    HomeTab.SETTINGS -> {
                        // Settings opens as its own screen.
                        LaunchedEffect(Unit) {
                            navController.navigate(Routes.SETTINGS)
                            tab = HomeTab.VIDEOS
                        }
                    }
                }
            }
            if (tab == HomeTab.MUSIC && currentEntry != null) {
                MiniPlayer(
                    entry = currentEntry!!,
                    isPlaying = isPlaying,
                    positionMs = position,
                    durationMs = duration,
                    playerVm = playerVm,
                    onClick = { navController.navigate(Routes.MUSIC) }
                )
            }
            GlassBottomBar(
                selected = tab,
                onSelect = { selected ->
                    if (selected == HomeTab.SETTINGS) {
                        navController.navigate(Routes.SETTINGS)
                    } else {
                        tab = selected
                    }
                },
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun VideosTab(
    libraryVm: LibraryViewModel,
    playerVm: PlayerViewModel,
    navController: NavHostController,
    showSampleHint: Boolean
) {
    val videos by libraryVm.visibleVideos.collectAsState()
    val continueWatching by libraryVm.continueWatching.collectAsState()
    val sortMode by libraryVm.sortMode.collectAsState()
    val viewMode by libraryVm.viewMode.collectAsState()
    val filter by libraryVm.resolutionFilter.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (continueWatching.isNotEmpty()) {
            Text(
                text = "Continue Watching",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(continueWatching, key = { it.id }) { video ->
                    ContinueCard(video) {
                        playerVm.playVideo(video, videos)
                        navController.navigate(Routes.PLAYER)
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "All Videos",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            listOf("All", "HD", "4K").forEach { label ->
                val selected = filter == label
                Box(
                    modifier = Modifier
                        .padding(end = 4.dp)
                ) {
                    androidx.compose.foundation.clickable {
                        libraryVm.setResolutionFilter(label)
                    }
                    Text(
                        text = label,
                        color = if (selected) LocalAccent.current.start else TextMuted,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp)
                    )
                }
            }
            IconButton(
                onClick = { showSortMenu = true },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Sort,
                    contentDescription = "Sort",
                    tint = TextPrimary
                )
            }
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Name") },
                    onClick = {
                        libraryVm.setSortMode(VideoSort.NAME)
                        settingsVm.setDefaultSort("name")
                        showSortMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Date added") },
                    onClick = {
                        libraryVm.setSortMode(VideoSort.DATE)
                        settingsVm.setDefaultSort("date")
                        showSortMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Size") },
                    onClick = {
                        libraryVm.setSortMode(VideoSort.SIZE)
                        settingsVm.setDefaultSort("size")
                        showSortMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Duration") },
                    onClick = {
                        libraryVm.setSortMode(VideoSort.DURATION)
                        settingsVm.setDefaultSort("duration")
                        showSortMenu = false
                    }
                )
            }
            IconButton(
                onClick = {
                    libraryVm.toggleViewMode()
                    settingsVm.setDefaultView(
                        if (viewMode == VideoViewMode.GRID) "list" else "grid"
                    )
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (viewMode == VideoViewMode.GRID) {
                        Icons.Rounded.ViewList
                    } else {
                        Icons.Rounded.GridView
                    },
                    contentDescription = "Toggle view",
                    tint = TextPrimary
                )
            }
        }
        if (showSampleHint) {
            Text(
                text = "Showing sample library — allow media access to see your files.",
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        if (viewMode == VideoViewMode.GRID) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(videos, key = { it.id }) { video ->
                    VideoCard(video) {
                        playerVm.playVideo(video, videos)
                        navController.navigate(Routes.PLAYER)
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(videos, key = { it.id }) { video ->
                    VideoRow(video) {
                        playerVm.playVideo(video, videos)
                        navController.navigate(Routes.PLAYER)
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoCard(video: VideoItem, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Box {
            ArtImage(
                key = video.thumbKey,
                kind = ArtKind.VIDEO,
                isVideo = true,
                modifier = Modifier.fillMaxWidth()
            )
            Badge(
                text = video.resolution,
                gradient = video.resolution == "4K",
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
            )
            Badge(
                text = formatDuration(video.durationMs),
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
            )
        }
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = video.title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${video.genre} • ${formatDuration(video.durationMs)}",
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (video.resumeMs > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                GlassProgressBar(progress = video.resumeMs.toFloat() / video.durationMs)
            }
        }
    }
}

@Composable
private fun VideoRow(video: VideoItem, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(128.dp)) {
                ArtImage(
                    key = video.thumbKey,
                    kind = ArtKind.VIDEO,
                    isVideo = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Badge(
                    text = formatDuration(video.durationMs),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.genre,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${video.resolution} • ${video.sizeText}",
                    color = TextMuted,
                    fontSize = 12.sp
                )
                if (video.resumeMs > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    GlassProgressBar(progress = video.resumeMs.toFloat() / video.durationMs)
                }
            }
        }
    }
}

@Composable
private fun ContinueCard(video: VideoItem, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.width(220.dp), onClick = onClick) {
        Box {
            ArtImage(
                key = video.thumbKey,
                kind = ArtKind.VIDEO,
                isVideo = true,
                modifier = Modifier.fillMaxWidth()
            )
            Badge(
                text = formatDuration(video.durationMs),
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
            )
        }
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = video.title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            GlassProgressBar(progress = video.resumeMs.toFloat() / video.durationMs)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Resume at ${formatDuration(video.resumeMs)}",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun MusicTab(
    libraryVm: LibraryViewModel,
    playerVm: PlayerViewModel,
    navController: NavHostController
) {
    val tracks by libraryVm.visibleTracks.collectAsState()
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 0.dp)
    ) {
        items(tracks, key = { it.id }) { track ->
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    playerVm.playTrack(track, tracks)
                    navController.navigate(Routes.MUSIC)
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArtImage(
                        key = track.artKey,
                        kind = ArtKind.MUSIC,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${track.artist} • ${track.album}",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = formatDuration(track.durationMs),
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayer(
    entry: QueueEntry,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    playerVm: PlayerViewModel,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArtImage(
                key = entry.artKey,
                kind = if (entry.isVideo) ArtKind.VIDEO else ArtKind.MUSIC,
                modifier = Modifier.size(48.dp)
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    text = entry.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry.subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (durationMs > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    GlassProgressBar(progress = positionMs.toFloat() / durationMs)
                }
            }
            IconButton(
                onClick = { playerVm.togglePlay() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) {
                        androidx.compose.material.icons.Icons.Rounded.Pause
                    } else {
                        Icons.Rounded.PlayArrow
                    },
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = TextPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            IconButton(
                onClick = { playerVm.playNext() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun FoldersTab(libraryVm: LibraryViewModel) {
    val folders by libraryVm.folders.collectAsState()
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(folders, key = { it.name }) { folder ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                LocalAccent.current.brush,
                                androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = folder.name,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = folder.path,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Badge(text = "${folder.count} items")
                }
            }
        }
    }
}

@Composable
private fun NetworkTab(
    libraryVm: LibraryViewModel,
    playerVm: PlayerViewModel,
    navController: NavHostController
) {
    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val streams by libraryVm.streams.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Stream URL",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(10.dp))
        GlassTextField(
            value = url,
            onValueChange = { url = it; error = null },
            placeholder = "Paste a stream URL (HLS, DASH, MP4…)",
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Public,
                    contentDescription = null,
                    tint = TextMuted
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        AccentGradientButton(
            text = "Play Stream",
            icon = Icons.Rounded.PlayArrow,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val trimmed = url.trim()
                if (libraryVm.addStream(trimmed)) {
                    playerVm.playStream(
                        StreamItem(trimmed, trimmed.substringAfterLast("/").ifBlank { "Stream" }, "Just now")
                    )
                    navController.navigate(Routes.PLAYER)
                } else {
                    error = "Enter a valid http(s) URL."
                }
            }
        )
        error?.let {
            Text(
                text = it,
                color = com.captain.kplayer.ui.theme.PinkAccent,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Recent streams",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(streams, key = { it.url }) { stream ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        playerVm.playStream(stream)
                        navController.navigate(Routes.PLAYER)
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Public,
                            contentDescription = null,
                            tint = LocalAccent.current.start,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stream.title,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stream.url,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = stream.addedLabel,
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
