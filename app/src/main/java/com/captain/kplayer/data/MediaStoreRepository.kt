package com.captain.kplayer.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scans real device media through MediaStore: the user's own downloaded
 * movies, videos and songs. Results are grouped into folders by directory.
 * Returns empty lists when nothing is found; the Library keeps the sample
 * data as fallback in that case.
 */
object MediaStoreRepository {

    data class ScanResult(
        val videos: List<VideoItem>,
        val tracks: List<TrackItem>,
        val folders: List<FolderItem>
    )

    suspend fun scan(context: Context, showHiddenFiles: Boolean = false): ScanResult =
        withContext(Dispatchers.IO) {
            val videos = scanVideos(context, showHiddenFiles)
            val tracks = scanAudio(context, showHiddenFiles)
            val folders = buildFolders(videos, tracks)
            ScanResult(videos, tracks, folders)
        }

    private fun scanVideos(context: Context, showHiddenFiles: Boolean): List<VideoItem> {
        val list = mutableListOf<VideoItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED
        )
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, sortOrder
        ) ?: return emptyList()
        cursor.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val nameCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val widthCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val mimeCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val bucketCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val dateCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val duration = it.getLong(durationCol)
                if (duration <= 0L) continue
                val displayName = it.getString(nameCol) ?: it.getString(titleCol) ?: "Video"
                val bucket = it.getString(bucketCol) ?: "Device"
                // Hidden files: dot-files and dot-folders are skipped unless
                // the user turned "Show hidden files" on in Settings.
                if (!showHiddenFiles &&
                    (displayName.startsWith(".") || bucket.startsWith("."))
                ) continue
                val width = it.getInt(widthCol)
                val height = it.getInt(heightCol)
                val mime = it.getString(mimeCol) ?: "video/mp4"
                val uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                ).toString()
                val title = it.getString(titleCol)?.takeIf { t -> t.isNotBlank() } ?: displayName
                val sizeBytes = it.getLong(sizeCol)
                val dateAddedMs = it.getLong(dateCol) * 1000L
                list.add(
                    VideoItem(
                        id = "local_video_$id",
                        title = title,
                        genre = bucket,
                        durationMs = duration,
                        resolution = resolutionLabel(width, height),
                        url = uri,
                        thumbKey = "local_video_$id",
                        sizeText = formatBytes(sizeBytes),
                        codec = mime.substringAfter("/").uppercase(),
                        bitrateText = "—",
                        fps = 0,
                        audioTracks = listOf(
                            AudioTrackInfo("Unknown", "AAC", "Stereo", isDefault = true)
                        ),
                        subtitleTracks = emptyList(),
                        isLocal = true,
                        sizeBytes = sizeBytes,
                        dateAddedMs = dateAddedMs
                    )
                )
            }
        }
        return list
    }

    private fun scanAudio(context: Context, showHiddenFiles: Boolean): List<TrackItem> {
        val list = mutableListOf<TrackItem>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, sortOrder
        ) ?: return emptyList()
        cursor.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val duration = it.getLong(durationCol)
                if (duration <= 0L) continue
                // Hidden files live under a dot-folder ("/.nomedia" style paths).
                if (!showHiddenFiles) {
                    val path = it.getString(dataCol) ?: ""
                    if (path.contains("/.")) continue
                }
                val duration = it.getLong(durationCol)
                if (duration <= 0L) continue
                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                ).toString()
                val title = it.getString(titleCol) ?: "Unknown title"
                list.add(
                    TrackItem(
                        id = "local_audio_$id",
                        title = title,
                        artist = it.getString(artistCol) ?: "Unknown artist",
                        album = it.getString(albumCol) ?: "Unknown album",
                        durationMs = duration,
                        url = uri,
                        artKey = "local_audio_$id",
                        isLocal = true
                    )
                )
            }
        }
        return list
    }

    private fun buildFolders(videos: List<VideoItem>, tracks: List<TrackItem>): List<FolderItem> {
        val counts = linkedMapOf<String, Int>()
        val paths = linkedMapOf<String, String>()
        // Folder grouping uses the genre field, which carries the MediaStore
        // bucket (directory) name for scanned files.
        for (video in videos) {
            val folder = video.genre
            counts[folder] = (counts[folder] ?: 0) + 1
            paths.putIfAbsent(folder, "/storage/$folder")
        }
        for (track in tracks) {
            val folder = track.album
            counts[folder] = (counts[folder] ?: 0) + 1
            paths.putIfAbsent(folder, "/storage/Music/$folder")
        }
        return counts.map { (name, count) ->
            FolderItem(name, paths[name] ?: "/storage/$name", count, "folder_$name")
        }.sortedByDescending { it.count }
    }

    private fun resolutionLabel(width: Int, height: Int): String {
        val maxSide = maxOf(width, height)
        return when {
            maxSide >= 3840 -> "4K"
            maxSide >= 2560 -> "QHD"
            maxSide >= 1280 -> "HD"
            else -> "SD"
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
            bytes >= 1_048_576L -> "%.0f MB".format(bytes / 1_048_576.0)
            bytes >= 1_024L -> "%.0f KB".format(bytes / 1_024.0)
            else -> "$bytes B"
        }
    }
}
