package com.captain.kplayer.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.captain.kplayer.data.MediaStoreRepository
import com.captain.kplayer.data.PRIVATE_FOLDER_PATH
import com.captain.kplayer.data.QueueEntry
import com.captain.kplayer.data.StreamItem
import com.captain.kplayer.data.VideoItem
import com.captain.kplayer.data.formatDuration
import com.captain.kplayer.data.toQueueEntry
import com.captain.kplayer.ui.components.AccentGradientButton
import com.captain.kplayer.ui.components.ArtImage
import com.captain.kplayer.ui.components.ArtKind
import com.captain.kplayer.ui.components.Badge
import com.captain.kplayer.ui.components.GlassBottomBar
import com.captain.kplayer.ui.components.GlassCard
import com.captain.kplayer.ui.components.GlassDialog
import com.captain.kplayer.ui.components.GlassProgressBar
import com.captain.kplayer.ui.components.GlassTextField
import com.captain.kplayer.ui.components.HomeTab
import com.captain.kplayer.ui.components.RealThumbnail
import com.captain.kplayer.ui.navigation.Routes
import com.captain.kplayer.ui.theme.GradientBackground
import com.captain.kplayer.ui.theme.LocalAccent
import com.captain.kplayer.ui.theme.LocalTextMuted
import com.captain.kplayer.ui.theme.LocalTextPrimary
import com.captain.kplayer.ui.theme.LocalTextSecondary
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
    // Task C: the selected tab lives in LibraryViewModel (StateFlow), so it
    // survives HOME leaving composition on forward navigation — Back returns
    // to the tab the user was on instead of always resetting to Videos.
    val tab by libraryVm.selectedTab.collectAsState()
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
    // Loads persisted last-played stamps so Recently Played survives
    // restarts (idempotent; also persists future marks).
    LaunchedEffect(Unit) {
        libraryVm.attachStore(settingsVm.store)
        libraryVm.attachLibraryStore(context.applicationContext)
        // Resume positions are saved by the player while Home is not
        // observing them — reload them every time Home (re)appears so
        // Continue Watching and the thumbnail progress bars are current.
        libraryVm.refreshResumePositions()
    }

    // v2.0 long-press menu + action dialogs (shared with FolderDetail).
    var menuVideo by remember { mutableStateOf<VideoItem?>(null) }
    var renameTarget by remember { mutableStateOf<VideoItem?>(null) }
    var deleteTarget by remember { mutableStateOf<VideoItem?>(null) }
    var infoTarget by remember { mutableStateOf<VideoItem?>(null) }
    // Android 10+ may require a user tap to confirm a MediaStore delete.
    // The IntentSender launcher lives here; its result retries the pending
    // delete. The sender lambda is held in state (assigned once the
    // launcher exists) so the launcher's own result callback can use it
    // without a self-reference in its initializer.
    var confirmDeleteSender by remember { mutableStateOf<((IntentSender) -> Unit)?>(null) }
    val deleteConfirmLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            libraryVm.retryPendingDelete(
                context,
                onNeedsConfirmation = { sender -> confirmDeleteSender?.invoke(sender) },
                onResult = { _, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            Toast.makeText(context, "Delete cancelled", Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(deleteConfirmLauncher) {
        confirmDeleteSender = { sender ->
            deleteConfirmLauncher.launch(IntentSenderRequest.Builder(sender).build())
        }
    }
    // One delete entry point shared by the menu dialog and the confirm dialog.
    val requestDelete: (VideoItem) -> Unit = { video ->
        libraryVm.deleteVideo(
            context,
            video,
            onNeedsConfirmation = { sender -> confirmDeleteSender?.invoke(sender) },
            onResult = { _, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        )
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
                    color = LocalTextPrimary.current,
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
                        tint = LocalTextMuted.current
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (!hasPermission) {
                GlassCard(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Your media, in one place",
                            color = LocalTextPrimary.current,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Allow K-Player to read your videos and music so your downloaded files show up here.",
                            color = LocalTextSecondary.current,
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
                                color = LocalTextMuted.current,
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
                        settingsVm = settingsVm,
                        navController = navController,
                        showSampleHint = !usingDeviceMedia,
                        onLongPressVideo = { menuVideo = it }
                    )
                    HomeTab.MUSIC -> MusicTab(
                        libraryVm = libraryVm,
                        playerVm = playerVm,
                        navController = navController
                    )
                    HomeTab.FOLDERS -> FoldersTab(libraryVm = libraryVm, navController = navController)
                    HomeTab.NETWORK -> {
                        // Network opens as its own screen (same pattern as
                        // Settings below): the tab is deliberately NOT
                        // reset here, so Back returns to this tab.
                        LaunchedEffect(Unit) {
                            navController.navigate(Routes.NETWORK)
                        }
                    }
                    HomeTab.SETTINGS -> {
                        // Settings opens as its own screen. The tab is
                        // deliberately NOT reset here (Task C): Back from
                        // Settings returns to the tab it was opened from.
                        LaunchedEffect(Unit) {
                            navController.navigate(Routes.SETTINGS)
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
                        libraryVm.selectTab(selected)
                    }
                },
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
        // v2.0 long-press action dialogs (menu state lives above).
        VideoActionsDialog(
            video = menuVideo,
            onDismiss = { menuVideo = null },
            onPlay = { video ->
                menuVideo = null
                playerVm.playVideo(video, libraryVm.visibleVideos.value)
                navController.navigate(Routes.PLAYER)
            },
            onToggleWatched = { video ->
                libraryVm.toggleWatched(video)
                menuVideo = null
                Toast.makeText(
                    context,
                    if (video.isWatched) "Marked as unwatched" else "Marked as watched",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onMoveToPrivate = { video ->
                libraryVm.moveToPrivate(video)
                menuVideo = null
                Toast.makeText(context, "Moved to Private folder", Toast.LENGTH_SHORT).show()
            },
            onMoveOutOfPrivate = { video ->
                libraryVm.moveOutOfPrivate(video)
                menuVideo = null
                Toast.makeText(context, "Moved back to library", Toast.LENGTH_SHORT).show()
            },
            onRename = { video ->
                menuVideo = null
                renameTarget = video
            },
            onShare = { video ->
                menuVideo = null
                shareVideo(context, video)
            },
            onInfo = { video ->
                menuVideo = null
                infoTarget = video
            },
            onDelete = { video ->
                menuVideo = null
                deleteTarget = video
            }
        )
        renameTarget?.let { video ->
            RenameDialog(
                video = video,
                onDismiss = { renameTarget = null },
                onConfirm = { newName ->
                    renameTarget = null
                    libraryVm.renameVideo(context, video, newName) { ok ->
                        Toast.makeText(
                            context,
                            if (ok) "Renamed" else "Couldn't rename that video",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }
        deleteTarget?.let { video ->
            DeleteConfirmDialog(
                video = video,
                onDismiss = { deleteTarget = null },
                onConfirm = {
                    deleteTarget = null
                    requestDelete(video)
                }
            )
        }
        infoTarget?.let { video ->
            VideoInfoSheet(
                entry = video.toQueueEntry(),
                video = video,
                onDismiss = { infoTarget = null }
            )
        }
    }
}

@Composable
private fun VideosTab(
    libraryVm: LibraryViewModel,
    playerVm: PlayerViewModel,
    settingsVm: SettingsViewModel,
    navController: NavHostController,
    showSampleHint: Boolean,
    onLongPressVideo: (VideoItem) -> Unit
) {
    val videos by libraryVm.visibleVideos.collectAsState()
    val continueWatching by libraryVm.continueWatching.collectAsState()
    val recentlyPlayed by libraryVm.recentlyPlayed.collectAsState()
    val viewMode by libraryVm.viewMode.collectAsState()
    val filter by libraryVm.resolutionFilter.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }

    // Task G: Recently Played is a collapsing section — it folds up (height
    // animates to 0) once the All Videos list scrolls down, so the grid
    // gets the space, and expands again when scrolled back to the top.
    // The scroll state of the active list/grid drives it.
    // Scroll perf: the collapsed flag is a derivedStateOf, not a plain
    // read of firstVisibleItemScrollOffset in composition — the offset
    // changes on every scroll pixel, and reading it directly recomposed
    // this whole tab (and every visible card) on every pixel scrolled.
    // derivedStateOf only invalidates when the boolean itself flips.
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()
    val recentCollapsed by remember {
        derivedStateOf {
            if (viewMode == VideoViewMode.GRID) {
                gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0
            } else {
                listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
            }
        }
    }
    val playVideo: (VideoItem) -> Unit = { video ->
        playerVm.playVideo(video, videos)
        navController.navigate(Routes.PLAYER)
    }
    // Remembered so a VideosTab recomposition (e.g. a scroll-driven
    // collapse flip) does not hand the header item a new lambda and
    // force the header to recompose with it.
    val allVideosHeader: @Composable () -> Unit = remember(filter, viewMode, showSortMenu) {
        {
            AllVideosHeader(
                filter = filter,
                onFilterSelected = { libraryVm.setResolutionFilter(it) },
                viewMode = viewMode,
                showSortMenu = showSortMenu,
                onShowSortMenuChange = { showSortMenu = it },
                onSortSelected = { sort ->
                    libraryVm.setSortMode(sort)
                    settingsVm.setDefaultSort(sort.name.lowercase())
                    showSortMenu = false
                },
                onToggleView = {
                    libraryVm.toggleViewMode()
                    settingsVm.setDefaultView(
                        if (viewMode == VideoViewMode.GRID) "list" else "grid"
                    )
                },
                onMarkAllWatched = {
                    libraryVm.markAllWatched()
                    Toast.makeText(
                        navController.context,
                        "All videos marked as watched",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = recentlyPlayed.isNotEmpty() && !recentCollapsed,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                Text(
                    text = "Recently Played",
                    color = LocalTextPrimary.current,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(recentlyPlayed, key = { it.id }, contentType = { "recent_card" }) { video ->
                        RecentCard(video, onClick = { playVideo(video) }, onLongPress = { onLongPressVideo(video) })
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            if (viewMode == VideoViewMode.GRID) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    if (continueWatching.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            ContinueWatchingSection(
                                videos = continueWatching,
                                onPlay = playVideo,
                                onLongPress = onLongPressVideo
                            )
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        allVideosHeader()
                    }
                    if (showSampleHint) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = "Showing sample library — allow media access to see your files.",
                                color = LocalTextMuted.current,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                    items(videos, key = { it.id }, contentType = { "video_card" }) { video ->
                        VideoCard(video, onClick = { playVideo(video) }, onLongPress = { onLongPressVideo(video) })
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    if (continueWatching.isNotEmpty()) {
                        item {
                            ContinueWatchingSection(
                                videos = continueWatching,
                                onPlay = playVideo,
                                onLongPress = onLongPressVideo
                            )
                        }
                    }
                    item {
                        allVideosHeader()
                    }
                    if (showSampleHint) {
                        item {
                            Text(
                                text = "Showing sample library — allow media access to see your files.",
                                color = LocalTextMuted.current,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                    items(videos, key = { it.id }, contentType = { "video_row" }) { video ->
                        VideoRow(video, onClick = { playVideo(video) }, onLongPress = { onLongPressVideo(video) })
                    }
                }
            }
        }
    }
}

/** Continue Watching row, unchanged in behaviour — full-width section. */
@Composable
private fun ContinueWatchingSection(
    videos: List<VideoItem>,
    onPlay: (VideoItem) -> Unit,
    onLongPress: (VideoItem) -> Unit
) {
    Column {
        Text(
            text = "Continue Watching",
            color = LocalTextPrimary.current,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(videos, key = { it.id }, contentType = { "continue_card" }) { video ->
                ContinueCard(video, onClick = { onPlay(video) }, onLongPress = { onLongPress(video) })
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

/** "All Videos" header row: title, resolution filters, sort menu, view toggle. */
@Composable
private fun AllVideosHeader(
    filter: String,
    onFilterSelected: (String) -> Unit,
    viewMode: VideoViewMode,
    showSortMenu: Boolean,
    onShowSortMenuChange: (Boolean) -> Unit,
    onSortSelected: (VideoSort) -> Unit,
    onToggleView: () -> Unit,
    onMarkAllWatched: () -> Unit
) {
    var showOverflowMenu by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "All Videos",
            color = LocalTextPrimary.current,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.weight(1f))
        listOf("All", "HD", "4K").forEach { label ->
            val selected = filter == label
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clickable { onFilterSelected(label) }
            ) {
                Text(
                    text = label,
                    color = if (selected) LocalAccent.current.start else LocalTextMuted.current,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp)
                )
            }
        }
        IconButton(
            onClick = { onShowSortMenuChange(true) },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Sort,
                contentDescription = "Sort",
                tint = LocalTextPrimary.current
            )
        }
        DropdownMenu(
            expanded = showSortMenu,
            onDismissRequest = { onShowSortMenuChange(false) }
        ) {
            DropdownMenuItem(
                text = { Text("Name") },
                onClick = { onSortSelected(VideoSort.NAME) }
            )
            DropdownMenuItem(
                text = { Text("Date added") },
                onClick = { onSortSelected(VideoSort.DATE) }
            )
            DropdownMenuItem(
                text = { Text("Size") },
                onClick = { onSortSelected(VideoSort.SIZE) }
            )
            DropdownMenuItem(
                text = { Text("Duration") },
                onClick = { onSortSelected(VideoSort.DURATION) }
            )
        }
        IconButton(
            onClick = onToggleView,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = if (viewMode == VideoViewMode.GRID) {
                    Icons.Rounded.ViewList
                } else {
                    Icons.Rounded.GridView
                },
                contentDescription = "Toggle view",
                tint = LocalTextPrimary.current
            )
        }
        IconButton(
            onClick = { showOverflowMenu = true },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "More options",
                tint = LocalTextPrimary.current
            )
        }
        DropdownMenu(
            expanded = showOverflowMenu,
            onDismissRequest = { showOverflowMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Mark all as watched") },
                onClick = {
                    showOverflowMenu = false
                    onMarkAllWatched()
                }
            )
        }
    }
}

/** Resume progress as 0..1, safe when duration is unknown (0). */
internal fun resumeProgress(video: VideoItem): Float =
    if (video.durationMs > 0L) (video.resumeMs.toFloat() / video.durationMs).coerceIn(0f, 1f) else 0f

/** Shares a video with the system share sheet. */
internal fun shareVideo(context: android.content.Context, video: VideoItem) {
    val intent = Intent.createChooser(MediaStoreRepository.shareIntent(video), "Share video")
    runCatching { context.startActivity(intent) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoCard(video: VideoItem, onClick: () -> Unit, onLongPress: () -> Unit) {
    // Formatted once — the duration text is shown twice (badge + subtitle).
    val durationText = formatDuration(video.durationMs)
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
    ) {
        Box {
            RealThumbnail(
                video = video,
                modifier = Modifier.fillMaxWidth()
            )
            Badge(
                text = video.resolution,
                gradient = video.resolution == "4K",
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
            )
            if (video.isWatched) {
                Badge(
                    text = "Watched",
                    gradient = true,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                )
            }
            Badge(
                text = durationText,
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
            )
        }
        // Task G: tighter card text for the 3-per-row grid.
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = video.title,
                color = LocalTextPrimary.current,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${video.genre} • $durationText",
                color = LocalTextSecondary.current,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (video.resumeMs > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                GlassProgressBar(progress = resumeProgress(video))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoRow(video: VideoItem, onClick: () -> Unit, onLongPress: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(128.dp)) {
                RealThumbnail(
                    video = video,
                    modifier = Modifier.fillMaxWidth()
                )
                if (video.isWatched) {
                    Badge(
                        text = "Watched",
                        gradient = true,
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                    )
                }
                Badge(
                    text = formatDuration(video.durationMs),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    color = LocalTextPrimary.current,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.genre,
                    color = LocalTextSecondary.current,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${video.resolution} • ${video.sizeText}",
                    color = LocalTextMuted.current,
                    fontSize = 12.sp
                )
                if (video.resumeMs > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    GlassProgressBar(progress = resumeProgress(video))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContinueCard(video: VideoItem, onClick: () -> Unit, onLongPress: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .width(220.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
    ) {
        Box {
            RealThumbnail(
                video = video,
                modifier = Modifier.fillMaxWidth(),
                widthPx = 480,
                heightPx = 270
            )
            if (video.isWatched) {
                Badge(
                    text = "Watched",
                    gradient = true,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                )
            }
            Badge(
                text = formatDuration(video.durationMs),
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
            )
        }
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = video.title,
                color = LocalTextPrimary.current,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            GlassProgressBar(progress = resumeProgress(video))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Resume at ${formatDuration(video.resumeMs)}",
                color = LocalTextSecondary.current,
                fontSize = 11.sp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentCard(video: VideoItem, onClick: () -> Unit, onLongPress: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .width(220.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
    ) {
        Box {
            RealThumbnail(
                video = video,
                modifier = Modifier.fillMaxWidth(),
                widthPx = 480,
                heightPx = 270
            )
            if (video.isWatched) {
                Badge(
                    text = "Watched",
                    gradient = true,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                )
            }
            Badge(
                text = formatDuration(video.durationMs),
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
            )
        }
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = video.title,
                color = LocalTextPrimary.current,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Played ${formatLastPlayed(video.lastPlayedMs)}",
                color = LocalTextSecondary.current,
                fontSize = 12.sp
            )
        }
    }
}

/** Relative last-played label ("Just now", "5 min ago", "3h ago", "Yesterday", "4d ago"). */
private fun formatLastPlayed(lastPlayedMs: Long): String {
    if (lastPlayedMs <= 0L) return "Not played yet"
    val diff = System.currentTimeMillis() - lastPlayedMs
    if (diff < 0L) return "Just now"
    val minutes = diff / 60_000L
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 60 * 24 -> "${minutes / 60}h ago"
        minutes < 60 * 24 * 2 -> "Yesterday"
        else -> "${minutes / (60 * 24)}d ago"
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
        items(tracks, key = { it.id }, contentType = { "track_row" }) { track ->
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
                            color = LocalTextPrimary.current,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${track.artist} • ${track.album}",
                            color = LocalTextSecondary.current,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = formatDuration(track.durationMs),
                        color = LocalTextMuted.current,
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
                    color = LocalTextPrimary.current,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry.subtitle,
                    color = LocalTextSecondary.current,
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
                        Icons.Rounded.Pause
                    } else {
                        Icons.Rounded.PlayArrow
                    },
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = LocalTextPrimary.current,
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
                    tint = LocalTextPrimary.current
                )
            }
        }
    }
}

@Composable
private fun FoldersTab(libraryVm: LibraryViewModel, navController: NavHostController) {
    val context = LocalContext.current
    val folders by libraryVm.folders.collectAsState()
    val query by libraryVm.searchQuery.collectAsState()
    val privateVideos by libraryVm.privateVideos.collectAsState()
    val hiddenFolders by libraryVm.hiddenFolders.collectAsState()
    val hasPin by libraryVm.hasPin.collectAsState()
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showHiddenDialog by remember { mutableStateOf(false) }
    var pinMode by remember { mutableStateOf<PinDialogMode?>(null) }

    // Search applies to folders too (name or path).
    val shownFolders = if (query.isBlank()) {
        folders
    } else {
        folders.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.path.contains(query, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Folders",
                color = LocalTextPrimary.current,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { showOverflowMenu = true },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Folder options",
                    tint = LocalTextPrimary.current
                )
            }
            DropdownMenu(
                expanded = showOverflowMenu,
                onDismissRequest = { showOverflowMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Hidden folders") },
                    onClick = {
                        showOverflowMenu = false
                        showHiddenDialog = true
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (hasPin) "Change Private PIN" else "Set Private PIN") },
                    onClick = {
                        showOverflowMenu = false
                        pinMode = if (hasPin) PinDialogMode.CHANGE else PinDialogMode.SETUP
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            // PIN-locked Private folder — always first.
            item(contentType = { "private_folder_row" }) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { navController.navigate(Routes.folderRoute(PRIVATE_FOLDER_PATH)) }
                ) {
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
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Private",
                                color = LocalTextPrimary.current,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (hasPin) "PIN protected" else "Tap to set a PIN",
                                color = LocalTextSecondary.current,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Badge(text = "${privateVideos.size} items")
                    }
                }
            }
            items(shownFolders, key = { it.path }, contentType = { "folder_row" }) { folder ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { navController.navigate(Routes.folderRoute(folder.path)) }
                ) {
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
                                color = LocalTextPrimary.current,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = folder.path,
                                color = LocalTextSecondary.current,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Badge(text = "${folder.count} items")
                        IconButton(
                            onClick = {
                                libraryVm.hideFolder(folder.path)
                                Toast.makeText(
                                    context,
                                    "Folder hidden — restore it from the ⋮ menu",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VisibilityOff,
                                contentDescription = "Hide folder",
                                tint = LocalTextMuted.current
                            )
                        }
                    }
                }
            }
        }
    }

    if (showHiddenDialog) {
        HiddenFoldersDialog(
            hiddenFolders = hiddenFolders,
            onDismiss = { showHiddenDialog = false },
            onUnhide = { path ->
                libraryVm.unhideFolder(path)
                Toast.makeText(context, "Folder visible again", Toast.LENGTH_SHORT).show()
            }
        )
    }
    pinMode?.let { mode ->
        PinDialog(
            mode = mode,
            onDismiss = { pinMode = null },
            onUnlockAttempt = { pin, onResult -> libraryVm.verifyPin(pin, onResult) },
            onSetPin = { pin, onResult -> libraryVm.setPin(pin, onResult) },
            onChangePin = { oldPin, newPin, onResult ->
                libraryVm.changePin(oldPin, newPin, onResult)
            }
        )
    }
}

@Composable
private fun NetworkTab(
    libraryVm: LibraryViewModel,
    playerVm: PlayerViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current
    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val streams by libraryVm.streams.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Stream URL",
            color = LocalTextPrimary.current,
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
                    tint = LocalTextMuted.current
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
                // Bug 4: never crash on a bad URL — validate first, then
                // start playback inside try/catch and surface the error.
                val trimmed = url.trim()
                if (!playerVm.isValidStreamUrl(trimmed)) {
                    error = "Enter a valid http(s) URL."
                } else {
                    try {
                        if (libraryVm.addStream(trimmed)) {
                            val stream = StreamItem(
                                trimmed,
                                trimmed.substringAfterLast("/").ifBlank { "Stream" },
                                "Just now"
                            )
                            if (playerVm.playStream(stream)) {
                                navController.navigate(Routes.PLAYER)
                            } else {
                                // Task D: a failed start is not always a bad
                                // URL — say so when the player just isn't
                                // built yet, instead of blaming the link.
                                error = if (!playerVm.isPlayerReady) {
                                    "Player is still starting — try again in a moment."
                                } else {
                                    "Couldn't start that stream."
                                }
                            }
                        } else {
                            error = "Enter a valid http(s) URL."
                        }
                    } catch (e: Exception) {
                        error = "Couldn't start that stream."
                    }
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
            color = LocalTextPrimary.current,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(streams, key = { it.url }, contentType = { "stream_row" }) { stream ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        // Bug 4: a stale/broken recent stream must never crash.
                        try {
                            if (playerVm.isValidStreamUrl(stream.url) &&
                                playerVm.playStream(stream)
                            ) {
                                navController.navigate(Routes.PLAYER)
                            } else {
                                Toast.makeText(
                                    context,
                                    // Task D: don't blame the link when the
                                    // player simply isn't built yet.
                                    if (!playerVm.isPlayerReady) {
                                        "Player is still starting — try again in a moment."
                                    } else {
                                        "That stream link is no longer valid."
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Couldn't start that stream.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
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
                                color = LocalTextPrimary.current,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stream.url,
                                color = LocalTextSecondary.current,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = stream.addedLabel,
                            color = LocalTextMuted.current,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// v2.0 library dialogs — shared with FolderDetailScreen (same package).
// ------------------------------------------------------------------

/** One row inside the long-press action menu. */
@Composable
private fun MenuRow(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    danger: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (danger) com.captain.kplayer.ui.theme.PinkAccent else LocalTextPrimary.current,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = text,
            color = if (danger) com.captain.kplayer.ui.theme.PinkAccent else LocalTextPrimary.current,
            fontSize = 15.sp
        )
    }
}

/**
 * Long-press menu for any video: play, watched toggle, Private move,
 * rename, share, video info and delete. Rendered as a dialog (not a
 * DropdownMenu) so it needs no anchor and works from every card/row.
 */
@Composable
fun VideoActionsDialog(
    video: VideoItem?,
    onDismiss: () -> Unit,
    onPlay: (VideoItem) -> Unit,
    onToggleWatched: (VideoItem) -> Unit,
    onMoveToPrivate: (VideoItem) -> Unit,
    onMoveOutOfPrivate: (VideoItem) -> Unit,
    onRename: (VideoItem) -> Unit,
    onShare: (VideoItem) -> Unit,
    onInfo: (VideoItem) -> Unit,
    onDelete: (VideoItem) -> Unit,
    isPrivate: Boolean = false
) {
    if (video == null) return
    GlassDialog(onDismiss = onDismiss) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = video.title,
                color = LocalTextPrimary.current,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            MenuRow(Icons.Rounded.PlayArrow, "Play") { onPlay(video) }
            MenuRow(
                Icons.Rounded.Check,
                if (video.isWatched) "Mark as unwatched" else "Mark as watched"
            ) { onToggleWatched(video) }
            if (isPrivate) {
                MenuRow(Icons.Rounded.LockOpen, "Remove from Private") { onMoveOutOfPrivate(video) }
            } else {
                MenuRow(Icons.Rounded.Lock, "Move to Private") { onMoveToPrivate(video) }
            }
            MenuRow(Icons.Rounded.Edit, "Rename") { onRename(video) }
            MenuRow(Icons.Rounded.Share, "Share") { onShare(video) }
            MenuRow(Icons.Rounded.Info, "Video info") { onInfo(video) }
            MenuRow(Icons.Rounded.Delete, "Delete", danger = true) { onDelete(video) }
        }
    }
}

/** Rename dialog: a plain text field prefilled with the current title. */
@Composable
fun RenameDialog(
    video: VideoItem,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(video.title) }
    GlassDialog(onDismiss = onDismiss) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Rename video",
                color = LocalTextPrimary.current,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    focusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f),
                    focusedTextColor = LocalTextPrimary.current,
                    unfocusedTextColor = LocalTextPrimary.current,
                    cursorColor = LocalAccent.current.start
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                com.captain.kplayer.ui.components.GlassButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                AccentGradientButton(
                    text = "Rename",
                    onClick = { onConfirm(name.trim()) },
                    enabled = name.trim().isNotEmpty() && name.trim() != video.title,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Delete confirmation — the destructive action gets an explicit confirm step. */
@Composable
fun DeleteConfirmDialog(
    video: VideoItem,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    GlassDialog(onDismiss = onDismiss) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Delete video?",
                color = LocalTextPrimary.current,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "\"${video.title}\" will be permanently deleted from this device.",
                color = LocalTextSecondary.current,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                com.captain.kplayer.ui.components.GlassButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                AccentGradientButton(
                    text = "Delete",
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

enum class PinDialogMode { SETUP, UNLOCK, CHANGE }

/**
 * Private-folder PIN dialog. SETUP asks for a new PIN twice, UNLOCK asks
 * once, CHANGE asks for the current PIN plus a new one. The PIN is only
 * ever handled as a transient string here — it is hashed with a salt in
 * [com.captain.kplayer.data.LibraryStateStore] and never stored.
 */
@Composable
fun PinDialog(
    mode: PinDialogMode,
    onDismiss: () -> Unit,
    onUnlockAttempt: (String, (Boolean) -> Unit) -> Unit,
    onSetPin: (String, (Boolean) -> Unit) -> Unit,
    onChangePin: (String, String, (Boolean) -> Unit) -> Unit
) {
    var oldPin by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun pinField(
        value: String,
        onValueChange: (String) -> Unit,
        placeholder: String
    ): @Composable () -> Unit = {
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.length <= 4) onValueChange(it.filter { c -> c.isDigit() }) },
            placeholder = { Text(text = placeholder, color = LocalTextMuted.current, fontSize = 15.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                focusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f),
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f),
                focusedTextColor = LocalTextPrimary.current,
                unfocusedTextColor = LocalTextPrimary.current,
                cursorColor = LocalAccent.current.start
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }

    val title = when (mode) {
        PinDialogMode.SETUP -> "Set Private PIN"
        PinDialogMode.UNLOCK -> "Private folder is locked"
        PinDialogMode.CHANGE -> "Change Private PIN"
    }
    GlassDialog(onDismiss = onDismiss) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                color = LocalTextPrimary.current,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = when (mode) {
                    PinDialogMode.SETUP -> "Choose a 4-digit PIN. Videos in Private are hidden from the library until you unlock it."
                    PinDialogMode.UNLOCK -> "Enter your 4-digit PIN to see your private videos."
                    PinDialogMode.CHANGE -> "Enter your current PIN, then choose a new 4-digit PIN."
                },
                color = LocalTextSecondary.current,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            if (mode == PinDialogMode.CHANGE) {
                pinField(oldPin, { oldPin = it }, "Current PIN")()
                Spacer(modifier = Modifier.height(10.dp))
            }
            pinField(pin, { pin = it }, if (mode == PinDialogMode.CHANGE) "New PIN" else "4-digit PIN")()
            if (mode == PinDialogMode.SETUP) {
                Spacer(modifier = Modifier.height(10.dp))
                pinField(confirmPin, { confirmPin = it }, "Confirm PIN")()
            }
            error?.let {
                Text(
                    text = it,
                    color = com.captain.kplayer.ui.theme.PinkAccent,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                com.captain.kplayer.ui.components.GlassButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                AccentGradientButton(
                    text = when (mode) {
                        PinDialogMode.SETUP -> "Set PIN"
                        PinDialogMode.UNLOCK -> "Unlock"
                        PinDialogMode.CHANGE -> "Change"
                    },
                    onClick = {
                        when (mode) {
                            PinDialogMode.UNLOCK -> {
                                if (pin.length != 4) {
                                    error = "Enter your 4-digit PIN."
                                } else {
                                    onUnlockAttempt(pin) { ok ->
                                        if (ok) onDismiss() else error = "Wrong PIN. Try again."
                                    }
                                }
                            }
                            PinDialogMode.SETUP -> {
                                when {
                                    pin.length != 4 -> error = "PIN must be 4 digits."
                                    pin != confirmPin -> error = "PINs don't match."
                                    else -> onSetPin(pin) { ok ->
                                        if (ok) onDismiss() else error = "Couldn't save that PIN."
                                    }
                                }
                            }
                            PinDialogMode.CHANGE -> {
                                when {
                                    oldPin.length != 4 || pin.length != 4 ->
                                        error = "Both PINs must be 4 digits."
                                    else -> onChangePin(oldPin, pin) { ok ->
                                        if (ok) onDismiss()
                                        else error = "Wrong current PIN, or the new PIN is invalid."
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Lists hidden folders with an Unhide action per folder. */
@Composable
fun HiddenFoldersDialog(
    hiddenFolders: Set<String>,
    onDismiss: () -> Unit,
    onUnhide: (String) -> Unit
) {
    GlassDialog(onDismiss = onDismiss) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Hidden folders",
                color = LocalTextPrimary.current,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (hiddenFolders.isEmpty()) {
                Text(
                    text = "No folders are hidden. Hide a folder with the eye-off button on its row.",
                    color = LocalTextSecondary.current,
                    fontSize = 14.sp
                )
            } else {
                hiddenFolders.sorted().forEach { path ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = path.substringAfterLast("/").ifBlank { path },
                                color = LocalTextPrimary.current,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = path,
                                color = LocalTextMuted.current,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        com.captain.kplayer.ui.components.GlassButton(
                            text = "Unhide",
                            onClick = { onUnhide(path) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            AccentGradientButton(
                text = "Done",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
