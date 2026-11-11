package com.captain.kplayer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.captain.kplayer.data.NamedPlaylist
import com.captain.kplayer.data.PlaylistRepository
import com.captain.kplayer.data.StreamItem
import com.captain.kplayer.data.formatDuration
import com.captain.kplayer.ui.components.AccentGradientButton
import com.captain.kplayer.ui.components.ArtImage
import com.captain.kplayer.ui.components.ArtKind
import com.captain.kplayer.ui.components.GlassButton
import com.captain.kplayer.ui.components.GlassCard
import com.captain.kplayer.ui.components.GlassTextField
import com.captain.kplayer.ui.components.NowPlayingBars
import com.captain.kplayer.ui.navigation.Routes
import com.captain.kplayer.ui.theme.GradientBackground
import com.captain.kplayer.ui.theme.LocalTextPrimary
import com.captain.kplayer.ui.theme.LocalTextSecondary
import com.captain.kplayer.viewmodel.LibraryViewModel
import com.captain.kplayer.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Playlist screen with two tabs:
 *
 * QUEUE — the live playback queue. Long-press an item, then drag it up
 * or down: the list reorders LIVE as the row crosses each neighbour
 * (previously it only reordered on drag end, and the gesture detector
 * was keyed by list index, so a mid-drag reorder restarted detection
 * and dropped the drag — now it is keyed by the stable item id).
 * The queue can also be saved as a named playlist.
 *
 * PLAYLISTS — named playlists persisted in DataStore
 * ([PlaylistRepository]): create, rename, delete, reorder items by
 * drag, remove items. Playing a playlist works for all-video and
 * all-audio playlists through the public PlayerViewModel APIs; a
 * mixed playlist plays its videos (or songs) and says so — a
 * single mixed queue needs a small PlayerViewModel hook (see the
 * v2.0 report) because startQueue is private.
 */
@Composable
fun PlaylistScreen(
    navController: NavHostController,
    playerVm: PlayerViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { PlaylistRepository(context.applicationContext) }
    val libraryVm: LibraryViewModel = viewModel()
    val playlists by repo.playlists.collectAsState(initial = emptyList())

    var tab by remember { mutableStateOf(0) } // 0 = Queue, 1 = Playlists
    val queue by playerVm.queue.collectAsState()

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 48.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = LocalTextPrimary.current
                    )
                }
                Text(
                    text = "Playlist",
                    color = LocalTextPrimary.current,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (tab == 0) "${queue.size} items" else "${playlists.size} lists",
                    color = LocalTextSecondary.current,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (tab == 0) {
                    AccentGradientButton(
                        onClick = {},
                        text = "Queue",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    GlassButton(
                        onClick = { tab = 0 },
                        text = "Queue",
                        modifier = Modifier.weight(1f)
                    )
                }
                if (tab == 1) {
                    AccentGradientButton(
                        onClick = {},
                        text = "Playlists",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    GlassButton(
                        onClick = { tab = 1 },
                        text = "Playlists",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (tab == 0) {
                QueueTab(
                    playerVm = playerVm,
                    repo = repo,
                    onSaved = { tab = 1 }
                )
            } else {
                PlaylistsTab(
                    playlists = playlists,
                    repo = repo,
                    playerVm = playerVm,
                    libraryVm = libraryVm,
                    navController = navController
                )
            }
        }
    }
}

@Composable
private fun QueueTab(
    playerVm: PlayerViewModel,
    repo: PlaylistRepository,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val queue by playerVm.queue.collectAsState()
    val currentIndex by playerVm.currentIndex.collectAsState()
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragIndex by remember { mutableStateOf(-1) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    // One queue row is ~76dp tall (64dp row + 12dp spacing).
    val itemHeightPx = with(density) { 76.dp.toPx() }

    var saveName by remember { mutableStateOf("") }

    if (queue.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Queue is empty.\nPlay a video or song to build a queue.",
                color = LocalTextSecondary.current,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
        return
    }
    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(queue, key = { _, item -> item.id }) { index, item ->
            val isCurrent = index == currentIndex
            val isDragged = draggedId == item.id
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = if (isDragged) dragOffsetY else 0f
                        scaleX = if (isDragged) 1.02f else 1f
                        scaleY = if (isDragged) 1.02f else 1f
                        shadowElevation = if (isDragged) 16f else 0f
                    }
                    // Keyed by the STABLE item id (not the index): after
                    // a live reorder the item moves, but the gesture
                    // detector keeps running instead of restarting and
                    // dropping the drag mid-flight.
                    .pointerInput(item.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggedId = item.id
                                dragIndex = index
                                dragOffsetY = 0f
                            },
                            onDrag = { _, dragAmount ->
                                dragOffsetY += dragAmount.y
                                // Live reorder: swap as soon as the row
                                // crosses the next row's midpoint, and
                                // keep the leftover offset for the visual.
                                val delta = (dragOffsetY / itemHeightPx).roundToInt()
                                if (delta != 0) {
                                    val target = (dragIndex + delta)
                                        .coerceIn(0, queue.size - 1)
                                    if (target != dragIndex) {
                                        playerVm.moveQueueItem(dragIndex, target)
                                        dragIndex = target
                                        dragOffsetY -= delta * itemHeightPx
                                    }
                                }
                                // Auto-scroll when the dragged row nears
                                // the top or bottom edge of the list.
                                scope.launch {
                                    val info = listState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { it.key == draggedId }
                                    if (info != null) {
                                        val viewportEnd =
                                            listState.layoutInfo.viewportEndOffset
                                        when {
                                            info.offset < 120 ->
                                                listState.scrollBy(
                                                    (info.offset - 120).toFloat()
                                                )
                                            info.offset + info.size > viewportEnd - 120 ->
                                                listState.scrollBy(
                                                    (info.offset + info.size -
                                                        viewportEnd + 120).toFloat()
                                                )
                                        }
                                    }
                                }
                            },
                            onDragEnd = {
                                draggedId = null
                                dragIndex = -1
                                dragOffsetY = 0f
                            },
                            onDragCancel = {
                                draggedId = null
                                dragIndex = -1
                                dragOffsetY = 0f
                            }
                        )
                    },
                onClick = { playerVm.playQueueEntry(index) },
                radius = 16.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArtImage(
                        key = item.artKey,
                        kind = if (item.isVideo) ArtKind.VIDEO else ArtKind.MUSIC,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            color = LocalTextPrimary.current,
                            fontSize = 15.sp,
                            fontWeight = if (isCurrent) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Medium
                            },
                            maxLines = 1
                        )
                        Text(
                            text = item.subtitle +
                                if (item.durationMs > 0) {
                                    " • ${formatDuration(item.durationMs)}"
                                } else "",
                            color = LocalTextSecondary.current,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                    if (isCurrent) {
                        NowPlayingBars()
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(
                        imageVector = Icons.Rounded.DragHandle,
                        contentDescription = "Drag to reorder",
                        tint = LocalTextSecondary.current,
                        modifier = Modifier.size(22.dp)
                    )
                    IconButton(
                        onClick = { playerVm.removeQueueItem(index) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Remove",
                            tint = LocalTextSecondary.current,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        item {
            Text(
                text = "Long-press an item, then drag it up or down — the list reorders as you drag.",
                color = LocalTextSecondary.current,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassTextField(
                    value = saveName,
                    onValueChange = { saveName = it },
                    placeholder = "Save queue as playlist…",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val name = saveName.trim()
                        if (name.isEmpty()) {
                            Toast.makeText(
                                context, "Give the playlist a name first.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@IconButton
                        }
                        scope.launch {
                            repo.savePlaylist(name, queue)
                            saveName = ""
                            Toast.makeText(
                                context, "Saved “$name”.",
                                Toast.LENGTH_SHORT
                            ).show()
                            onSaved()
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Save queue as playlist",
                        tint = LocalTextPrimary.current,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun PlaylistsTab(
    playlists: List<NamedPlaylist>,
    repo: PlaylistRepository,
    playerVm: PlayerViewModel,
    libraryVm: LibraryViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var newName by remember { mutableStateOf("") }
    var expandedName by remember { mutableStateOf<String?>(null) }
    var renaming by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }

    // Drag state for reordering inside the expanded playlist.
    var draggedEntryId by remember { mutableStateOf<String?>(null) }
    var dragEntryIndex by remember { mutableStateOf(-1) }
    var dragEntryOffsetY by remember { mutableStateOf(0f) }
    val itemHeightPx = with(density) { 76.dp.toPx() }

    fun playPlaylist(playlist: NamedPlaylist) {
        if (playlist.entries.isEmpty()) {
            Toast.makeText(context, "This playlist is empty.", Toast.LENGTH_SHORT).show()
            return
        }
        // All-video playlist: playVideo builds exactly this queue
        // (first item + the rest, in order).
        val videos = playlist.entries.mapNotNull { libraryVm.videoById(it.id) }
        if (videos.size == playlist.entries.size) {
            playerVm.playVideo(videos.first(), videos)
            navController.navigate(Routes.PLAYER)
            return
        }
        // All-audio playlist: same via playTrack.
        val tracks = playlist.entries.mapNotNull { entry ->
            libraryVm.visibleTracks.value.firstOrNull { it.id == entry.id }
        }
        if (tracks.size == playlist.entries.size) {
            playerVm.playTrack(tracks.first(), tracks)
            navController.navigate(Routes.MUSIC)
            return
        }
        // Single stream.
        if (playlist.entries.size == 1 && playlist.entries.first().url.startsWith("http")) {
            val entry = playlist.entries.first()
            if (playerVm.playStream(StreamItem(entry.url, entry.title, "Just now"))) {
                navController.navigate(Routes.PLAYER)
            }
            return
        }
        // Mixed playlist: play the videos (or songs) it contains and say
        // so. A single mixed queue needs the PlayerViewModel hook noted
        // in the v2.0 report (startQueue is private).
        if (videos.isNotEmpty()) {
            playerVm.playVideo(videos.first(), videos)
            navController.navigate(Routes.PLAYER)
            Toast.makeText(
                context,
                "Playing the videos from this playlist.",
                Toast.LENGTH_SHORT
            ).show()
        } else if (tracks.isNotEmpty()) {
            playerVm.playTrack(tracks.first(), tracks)
            navController.navigate(Routes.MUSIC)
            Toast.makeText(
                context,
                "Playing the songs from this playlist.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                "This playlist can't be played yet — its items aren't in the library.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = "New playlist name",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val name = newName.trim()
                        if (name.isEmpty()) return@IconButton
                        scope.launch {
                            if (repo.createPlaylist(name)) {
                                newName = ""
                            } else {
                                Toast.makeText(
                                    context,
                                    "A playlist with that name already exists.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Create playlist",
                        tint = LocalTextPrimary.current,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
        if (playlists.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No playlists yet.\nSave the current queue or create one above.",
                        color = LocalTextSecondary.current,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        }
        items(playlists, key = { it.name }) { playlist ->
            val expanded = expandedName == playlist.name
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    expandedName = if (expanded) null else playlist.name
                },
                radius = 16.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (renaming == playlist.name) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    GlassTextField(
                                        value = renameText,
                                        onValueChange = { renameText = it },
                                        placeholder = "Playlist name",
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                if (repo.renamePlaylist(
                                                        playlist.name, renameText
                                                    )
                                                ) {
                                                    renaming = null
                                                    expandedName = renameText.trim()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "That name is taken or blank.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Save name",
                                            tint = LocalTextPrimary.current
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = playlist.name,
                                    color = LocalTextPrimary.current,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = "${playlist.entries.size} items" +
                                    if (playlist.entries.isNotEmpty()) {
                                        " • ${formatDuration(playlist.entries.sumOf { it.durationMs })}"
                                    } else "",
                                color = LocalTextSecondary.current,
                                fontSize = 13.sp
                            )
                        }
                        IconButton(
                            onClick = {
                                renaming = playlist.name
                                renameText = playlist.name
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Rename",
                                tint = LocalTextSecondary.current,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    repo.deletePlaylist(playlist.name)
                                    if (expandedName == playlist.name) expandedName = null
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete playlist",
                                tint = LocalTextSecondary.current,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { playPlaylist(playlist) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play playlist",
                                tint = LocalTextPrimary.current,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    if (expanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (playlist.entries.isEmpty()) {
                            Text(
                                text = "Empty — play something, then save the queue here.",
                                color = LocalTextSecondary.current,
                                fontSize = 13.sp
                            )
                        } else {
                            playlist.entries.forEachIndexed { entryIndex, entry ->
                                val isDragged = draggedEntryId == entry.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            translationY =
                                                if (isDragged) dragEntryOffsetY else 0f
                                        }
                                        .pointerInput(entry.id) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    draggedEntryId = entry.id
                                                    dragEntryIndex = entryIndex
                                                    dragEntryOffsetY = 0f
                                                },
                                                onDrag = { _, dragAmount ->
                                                    dragEntryOffsetY += dragAmount.y
                                                    val delta =
                                                        (dragEntryOffsetY / itemHeightPx)
                                                            .roundToInt()
                                                    if (delta != 0) {
                                                        val target =
                                                            (dragEntryIndex + delta).coerceIn(
                                                                0, playlist.entries.size - 1
                                                            )
                                                        if (target != dragEntryIndex) {
                                                            val from = dragEntryIndex
                                                            dragEntryIndex = target
                                                            dragEntryOffsetY -=
                                                                delta * itemHeightPx
                                                            scope.launch {
                                                                repo.moveInPlaylist(
                                                                    playlist.name, from, target
                                                                )
                                                            }
                                                        }
                                                    }
                                                },
                                                onDragEnd = {
                                                    draggedEntryId = null
                                                    dragEntryIndex = -1
                                                    dragEntryOffsetY = 0f
                                                },
                                                onDragCancel = {
                                                    draggedEntryId = null
                                                    dragEntryIndex = -1
                                                    dragEntryOffsetY = 0f
                                                }
                                            )
                                        }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ArtImage(
                                        key = entry.artKey,
                                        kind = if (entry.isVideo) {
                                            ArtKind.VIDEO
                                        } else {
                                            ArtKind.MUSIC
                                        },
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.title,
                                            color = LocalTextPrimary.current,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = entry.subtitle,
                                            color = LocalTextSecondary.current,
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.DragHandle,
                                        contentDescription = "Drag to reorder",
                                        tint = LocalTextSecondary.current,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                repo.removeFromPlaylist(
                                                    playlist.name, entry.id
                                                )
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Remove from playlist",
                                            tint = LocalTextSecondary.current,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
