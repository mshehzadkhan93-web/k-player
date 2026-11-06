package com.captain.kplayer.ui.screens

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.captain.kplayer.data.VideoItem
import com.captain.kplayer.data.formatDuration
import com.captain.kplayer.ui.components.Badge
import com.captain.kplayer.ui.components.GlassCard
import com.captain.kplayer.ui.components.GlassProgressBar
import com.captain.kplayer.ui.components.RealThumbnail
import com.captain.kplayer.ui.navigation.Routes
import com.captain.kplayer.ui.theme.GradientBackground
import com.captain.kplayer.ui.theme.TextMuted
import com.captain.kplayer.ui.theme.TextPrimary
import com.captain.kplayer.ui.theme.TextSecondary
import com.captain.kplayer.viewmodel.LibraryViewModel
import com.captain.kplayer.viewmodel.PlayerViewModel

/**
 * Folder detail: lists every video in one folder (via
 * [LibraryViewModel.videosInFolder]) with real thumbnails, a back button
 * and tap-to-play. Reached from FoldersTab via "folder/{encodedPath}".
 */
@Composable
fun FolderDetailScreen(
    folderPath: String,
    navController: NavHostController,
    libraryVm: LibraryViewModel,
    playerVm: PlayerViewModel
) {
    val videos = remember(folderPath) { libraryVm.videosInFolder(folderPath) }
    val folderName = folderPath.substringAfterLast("/").ifBlank { folderPath }

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
                    text = folderName,
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Badge(text = "${videos.size} videos")
            }
            Text(
                text = folderPath,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 56.dp, bottom = 12.dp)
            )
            if (videos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No videos in this folder.",
                        color = TextSecondary,
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(videos, key = { it.id }) { video ->
                        FolderVideoRow(video) {
                            playerVm.playVideo(video, videos)
                            navController.navigate(Routes.PLAYER)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderVideoRow(video: VideoItem, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(128.dp)) {
                RealThumbnail(
                    video = video,
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
