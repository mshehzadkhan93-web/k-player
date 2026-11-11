package com.captain.kplayer.data

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
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

    // ------------------------------------------------------------------
    // v2.0 library actions: delete / rename / share
    // ------------------------------------------------------------------

    /** Result of [deleteVideo]. */
    sealed interface DeleteResult {
        /** The row (and the underlying file, for MediaStore rows) is gone. */
        data object Deleted : DeleteResult

        /**
         * Android 10+ refused the delete without explicit user consent.
         * Launch [intentSender] with StartIntentSenderForResult and retry
         * the delete when the result is RESULT_OK.
         */
        data class NeedsUserConfirmation(val intentSender: IntentSender) : DeleteResult

        /** The delete failed for another reason (message is user-facing). */
        data class Failed(val message: String) : DeleteResult
    }

    /**
     * Deletes a video. For on-device files (content:// MediaStore URIs)
     * the MediaStore row is removed, which also removes the underlying
     * file. For anything else (sample/stream URLs) there is nothing on
     * disk to delete, so [DeleteResult.Deleted] is returned and the caller
     * just drops the item from the in-memory library.
     */
    suspend fun deleteVideo(context: Context, video: VideoItem): DeleteResult =
        withContext(Dispatchers.IO) {
            val uri = runCatching { Uri.parse(video.url) }.getOrNull()
                ?: return@withContext DeleteResult.Failed("That video has no valid file to delete.")
            if (uri.scheme != "content") return@withContext DeleteResult.Deleted
            try {
                val rows = context.contentResolver.delete(uri, null, null)
                if (rows > 0) DeleteResult.Deleted
                else DeleteResult.Failed("Couldn't delete that video — it may already be gone.")
            } catch (e: RecoverableSecurityException) {
                val sender = e.userAction?.actionIntent?.intentSender
                if (sender != null) DeleteResult.NeedsUserConfirmation(sender)
                else DeleteResult.Failed("Android needs your permission to delete this file.")
            } catch (_: SecurityException) {
                DeleteResult.Failed("K-Player doesn't have permission to delete this file.")
            } catch (_: Exception) {
                DeleteResult.Failed("Couldn't delete that video. Try again.")
            }
        }

    /**
     * Renames a video in MediaStore (TITLE + DISPLAY_NAME, keeping the
     * original file extension). Returns false when the rename failed or
     * the video is not an on-device file — the caller then renames only
     * the in-memory copy. [newName] is the display name without extension.
     */
    suspend fun renameVideo(context: Context, video: VideoItem, newName: String): Boolean =
        withContext(Dispatchers.IO) {
            val uri = runCatching { Uri.parse(video.url) }.getOrNull()
                ?: return@withContext false
            if (uri.scheme != "content") return@withContext false
            val trimmed = newName.trim()
            if (trimmed.isEmpty()) return@withContext false
            try {
                // Keep the original extension so the file stays playable.
                val currentName = queryDisplayName(context, uri)
                val extension = currentName
                    ?.takeIf { it.contains(".") }
                    ?.substringAfterLast(".", "")
                    ?.takeIf { it.length in 2..5 }
                    ?: video.title.substringAfterLast(".", "")
                        .takeIf { it.length in 2..5 && video.title.contains(".") }
                val displayName =
                    if (extension != null && !trimmed.endsWith(".$extension", ignoreCase = true)) {
                        "$trimmed.$extension"
                    } else trimmed
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.TITLE, trimmed)
                    put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                }
                context.contentResolver.update(uri, values, null, null) > 0
            } catch (_: Exception) {
                false
            }
        }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(
                uri, arrayOf(MediaStore.Video.Media.DISPLAY_NAME), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    )
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Builds a share intent for a video: a content:// file is shared as a
     * stream (with read permission granted to the receiver), anything else
     * is shared as a link. The caller wraps it in
     * Intent.createChooser(...) before starting it.
     */
    fun shareIntent(video: VideoItem): Intent {
        val uri = runCatching { Uri.parse(video.url) }.getOrNull()
        return if (uri != null && uri.scheme == "content") {
            Intent(Intent.ACTION_SEND).apply {
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, video.url)
                putExtra(Intent.EXTRA_SUBJECT, video.title)
            }
        }
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
