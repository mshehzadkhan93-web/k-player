package com.captain.kplayer.ui.screens

import android.app.Activity
import android.content.IntentSender
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
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
import androidx.navigation.NavHostController
import com.captain.kplayer.data.PRIVATE_FOLDER_PATH
import com.captain.kplayer.data.VideoItem
import com.captain.kplayer.data.formatDuration
import com.captain.kplayer.data.toQueueEntry
import com.captain.kplayer.ui.components.AccentGradientButton
import com.captain.kplayer.ui.components.Badge
import com.captain.kplayer.ui.components.GlassCard
import com.captain.kplayer.ui.components.GlassProgressBar
import com.captain.kplayer.ui.components.RealThumbnail
import com.captain.kplayer.ui.navigation.Routes
import com.captain.kplayer.ui.theme.GradientBackground
import com.captain.kplayer.ui.theme.LocalTextMuted
import com.captain.kplayer.ui.theme.LocalTextPrimary
import com.captain.kplayer.ui.theme.LocalTextSecondary
import com.captain.kplayer.viewmodel.LibraryViewModel
import com.captain.kplayer.viewmodel.PlayerViewModel

/**
 * Folder detail: lists every video in one folder with real thumbnails, a
 * back button and tap-to-play. Reached from FoldersTab via
 * "folder/{encodedPath}". The list is reactive (collects the library
 * flows), so deletes/renames/watched marks show up immediately.
 *
 * Special case: [PRIVATE_FOLDER_PATH] shows the PIN-locked Private folder
 * instead of a real directory — a PIN gate is shown until it is unlocked
 * for this session, and its rows offer "Remove from Private".
 */
@Composable
fun FolderDetailScreen(
    folderPath: String,
    navController: NavHostController,
    libraryVm: LibraryViewModel,
    playerVm: PlayerViewModel
) {
    val context = LocalContext.current
    val isPrivate = folderPath == PRIVATE_FOLDER_PATH
    val allVisible by libraryVm.allVisibleVideos.collectAsState()
    val privateVideos by libraryVm.privateVideos.collectAsState()
    val unlocked by libraryVm.privateUnlocked.collectAsState()
    val hasPin by libraryVm.hasPin.collectAsState()

    // Reactive: recomputed whenever the underlying library lists change.
    val videos = remember(folderPath, allVisible, privateVideos) {
        if (isPrivate) privateVideos else libraryVm.videosInFolder(folderPath)
    }
    val folderName = if (isPrivate) "Private" else
        folderPath.substringAfterLast("/").ifBlank { folderPath }

    // v2.0 long-press menu + action dialogs (shared composables from HomeScreen).
    var menuVideo by remember { mutableStateOf<VideoItem?>(null) }
    var renameTarget by remember { mutableStateOf<VideoItem?>(null) }
    var deleteTarget by remember { mutableStateOf<VideoItem?>(null) }
    var infoTarget by remember { mutableStateOf<VideoItem?>(null) }
    var pinMode by remember { mutableStateOf<PinDialogMode?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }

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
                    text = folderName,
                    color = LocalTextPrimary.current,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isPrivate && unlocked) {
                    IconButton(
                        onClick = { libraryVm.lockPrivate() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Lock Private folder",
                            tint = LocalTextPrimary.current
                        )
                    }
                }
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
                    if (isPrivate) {
                        DropdownMenuItem(
                            text = { Text("Change PIN") },
                            onClick = {
                                showOverflowMenu = false
                                pinMode = PinDialogMode.CHANGE
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Lock now") },
                            onClick = {
                                showOverflowMenu = false
                                libraryVm.lockPrivate()
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Hide this folder") },
                            onClick = {
                                showOverflowMenu = false
                                libraryVm.hideFolder(folderPath)
                                Toast.makeText(
                                    context,
                                    "Folder hidden — restore it from the Folders ⋮ menu",
                                    Toast.LENGTH_SHORT
                                ).show()
                                navController.popBackStack()
                            }
                        )
                    }
                }
                Badge(text = "${videos.size} videos")
            }
            if (!isPrivate) {
                Text(
                    text = folderPath,
                    color = LocalTextSecondary.current,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 56.dp, bottom = 12.dp)
                )
            }
            if (isPrivate && !unlocked) {
                // PIN gate: nothing private is shown until unlocked.
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = if (hasPin) "Private folder is locked" else "Set up Private folder",
                                color = LocalTextPrimary.current,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (hasPin) {
                                    "Enter your PIN to see the videos you moved here."
                                } else {
                                    "Choose a 4-digit PIN first. Videos you move here are hidden from the main library."
                                },
                                color = LocalTextSecondary.current,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            AccentGradientButton(
                                text = if (hasPin) "Unlock" else "Set PIN",
                                onClick = {
                                    pinMode = if (hasPin) PinDialogMode.UNLOCK else PinDialogMode.SETUP
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else if (videos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isPrivate) "No videos in Private yet.\nLong-press a video and choose “Move to Private”." else "No videos in this folder.",
                        color = LocalTextSecondary.current,
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(videos, key = { it.id }, contentType = { "video_row" }) { video ->
                        FolderVideoRow(
                            video = video,
                            onClick = {
                                playerVm.playVideo(video, videos)
                                navController.navigate(Routes.PLAYER)
                            },
                            onLongPress = { menuVideo = video }
                        )
                    }
                }
            }
        }

        VideoActionsDialog(
            video = menuVideo,
            isPrivate = isPrivate,
            onDismiss = { menuVideo = null },
            onPlay = { video ->
                menuVideo = null
                playerVm.playVideo(video, videos)
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderVideoRow(video: VideoItem, onClick: () -> Unit, onLongPress: () -> Unit) {
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
