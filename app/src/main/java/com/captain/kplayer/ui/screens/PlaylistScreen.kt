package com.captain.kplayer.ui.screens

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.captain.kplayer.data.formatDuration
import com.captain.kplayer.ui.components.ArtImage
import com.captain.kplayer.ui.components.ArtKind
import com.captain.kplayer.ui.components.GlassCard
import com.captain.kplayer.ui.components.NowPlayingBars
import com.captain.kplayer.ui.theme.GradientBackground
import com.captain.kplayer.ui.theme.TextPrimary
import com.captain.kplayer.ui.theme.TextSecondary
import com.captain.kplayer.viewmodel.PlayerViewModel
import kotlin.math.roundToInt

/**
 * Playlist / queue: draggable glass list items (long-press an item, then
 * drag it up or down to reorder), a now-playing indicator, tap to play
 * and a remove button per item.
 */
@Composable
fun PlaylistScreen(
    navController: NavHostController,
    playerVm: PlayerViewModel
) {
    val queue by playerVm.queue.collectAsState()
    val currentIndex by playerVm.currentIndex.collectAsState()
    val isPlaying by playerVm.isPlaying.collectAsState()
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    // One queue row is ~76dp tall (64dp row + 12dp spacing).
    val itemHeightPx = with(density) { 76.dp.toPx() }

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
                        tint = TextPrimary
                    )
                }
                Text(
                    text = "Playlist",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${queue.size} items",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Queue is empty.\nPlay a video or song to build a queue.",
                        color = TextSecondary,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(queue, key = { _, item -> item.id }) { index, item ->
                        val isCurrent = index == currentIndex
                        val isDragged = draggedIndex == index
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    translationY = if (isDragged) dragOffsetY else 0f
                                    scaleX = if (isDragged) 1.02f else 1f
                                    scaleY = if (isDragged) 1.02f else 1f
                                    shadowElevation = if (isDragged) 16f else 0f
                                }
                                .pointerInput(index) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggedIndex = index
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { _, dragAmount ->
                                            dragOffsetY += dragAmount.y
                                        },
                                        onDragEnd = {
                                            val target = (index +
                                                (dragOffsetY / itemHeightPx)
                                                    .roundToInt()
                                                ).coerceIn(0, queue.size - 1)
                                            if (target != index) {
                                                playerVm.moveQueueItem(index, target)
                                            }
                                            draggedIndex = null
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            draggedIndex = null
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
                                        color = TextPrimary,
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
                                        color = TextSecondary,
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
                                    tint = TextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                IconButton(
                                    onClick = { playerVm.removeQueueItem(index) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Remove",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Text(
                            text = "Long-press an item, then drag it up or down to reorder.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
