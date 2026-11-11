package com.captain.kplayer.data

/** One audio track inside a video file. */
data class AudioTrackInfo(
    val language: String,
    val codec: String,
    val channels: String,
    val isDefault: Boolean = false
)

/** One subtitle track inside a video file. */
data class SubtitleTrackInfo(
    val language: String,
    val source: String
)

data class VideoItem(
    val id: String,
    val title: String,
    val genre: String,
    val durationMs: Long,
    val resolution: String,
    val url: String,
    val thumbKey: String,
    val sizeText: String,
    val codec: String,
    val bitrateText: String,
    val fps: Int,
    val audioTracks: List<AudioTrackInfo>,
    val subtitleTracks: List<SubtitleTrackInfo>,
    val resumeMs: Long = 0L,
    /** True when the file lives on this device (MediaStore scan). */
    val isLocal: Boolean = false,
    /** File size in bytes (0 when unknown) — used by the Size sort order. */
    val sizeBytes: Long = 0L,
    /** Epoch millis the file was added — used by the Date sort order. */
    val dateAddedMs: Long = 0L,
    /** Epoch millis the video was last played (0 = never) — Recently Played. */
    val lastPlayedMs: Long = 0L,
    /** User-marked watched flag (persisted in LibraryStateStore) — v2.0 library. */
    val isWatched: Boolean = false
)

/** Route/folder key for the PIN-locked Private folder (not a real directory). */
const val PRIVATE_FOLDER_PATH = "__private__"

data class TrackItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val url: String,
    val artKey: String,
    /** True when the file lives on this device (MediaStore scan). */
    val isLocal: Boolean = false
)

data class FolderItem(
    val name: String,
    val path: String,
    val count: Int,
    val artKey: String
)

data class StreamItem(
    val url: String,
    val title: String,
    val addedLabel: String
)

/** One entry in the playback queue. Works for video, audio and streams. */
data class QueueEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val durationMs: Long,
    val isVideo: Boolean,
    val url: String,
    val artKey: String
)

fun VideoItem.toQueueEntry(): QueueEntry = QueueEntry(
    id = id,
    title = title,
    subtitle = genre,
    durationMs = durationMs,
    isVideo = true,
    url = url,
    artKey = thumbKey
)

fun TrackItem.toQueueEntry(): QueueEntry = QueueEntry(
    id = id,
    title = title,
    subtitle = artist,
    durationMs = durationMs,
    isVideo = false,
    url = url,
    artKey = artKey
)

fun StreamItem.toQueueEntry(): QueueEntry = QueueEntry(
    id = "stream_${url.hashCode()}",
    title = title,
    subtitle = "Network stream",
    durationMs = 0L,
    isVideo = true,
    url = url,
    artKey = "stream"
)

/** Formats milliseconds as mm:ss or h:mm:ss. */
fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
