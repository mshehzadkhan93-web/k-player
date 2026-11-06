package com.captain.kplayer.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.captain.kplayer.data.FolderItem
import com.captain.kplayer.data.MediaStoreRepository
import com.captain.kplayer.data.SampleData
import com.captain.kplayer.data.StreamItem
import com.captain.kplayer.data.TrackItem
import com.captain.kplayer.data.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class VideoSort { NAME, DATE, SIZE, DURATION, RESOLUTION }
enum class VideoViewMode { GRID, LIST }

/** Library state: videos, music, folders and network streams. */
class LibraryViewModel : ViewModel() {

    private val allVideos = MutableStateFlow(SampleData.videos)
    private val allTracks = MutableStateFlow(SampleData.tracks)
    private val allFolders = MutableStateFlow(SampleData.folders)
    private val _streams = MutableStateFlow(SampleData.recentStreams)

    /** True when the library shows real device files instead of samples. */
    private val _usingDeviceMedia = MutableStateFlow(false)
    val usingDeviceMedia: StateFlow<Boolean> = _usingDeviceMedia

    /**
     * Scans the device with MediaStore. When real videos or songs are found
     * they replace the sample library; otherwise the samples stay as fallback.
     * Hidden (dot) files are skipped unless [showHiddenFiles] is on.
     */
    fun scanDeviceMedia(context: Context, showHiddenFiles: Boolean = false) {
        viewModelScope.launch {
            val result = MediaStoreRepository.scan(context.applicationContext, showHiddenFiles)
            if (result.videos.isNotEmpty()) {
                allVideos.value = result.videos
            }
            if (result.tracks.isNotEmpty()) {
                allTracks.value = result.tracks
            }
            if (result.folders.isNotEmpty()) {
                allFolders.value = result.folders
            }
            _usingDeviceMedia.value = result.videos.isNotEmpty() || result.tracks.isNotEmpty()
        }
    }

    val streams: StateFlow<List<StreamItem>> = _streams

    val searchQuery = MutableStateFlow("")
    val sortMode = MutableStateFlow(VideoSort.NAME)
    val viewMode = MutableStateFlow(VideoViewMode.GRID)
    val resolutionFilter = MutableStateFlow("All")

    /** Videos the user has started watching, for the Continue Watching row. */
    val continueWatching: StateFlow<List<VideoItem>> =
        allVideos
            .map { videos -> videos.filter { it.resumeMs > 0 } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Videos after search text, resolution filter and sort are applied. */
    val visibleVideos: StateFlow<List<VideoItem>> = combine(
        allVideos, searchQuery, sortMode, resolutionFilter
    ) { videos, query, sort, filter ->
        var list = videos
        if (query.isNotBlank()) {
            list = list.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.genre.contains(query, ignoreCase = true)
            }
        }
        if (filter != "All") {
            list = list.filter { it.resolution == filter }
        }
        when (sort) {
            VideoSort.NAME -> list.sortedBy { it.title }
            VideoSort.DATE -> list.sortedByDescending { it.dateAddedMs }
            VideoSort.SIZE -> list.sortedByDescending { it.sizeBytes }
            VideoSort.DURATION -> list.sortedByDescending { it.durationMs }
            VideoSort.RESOLUTION -> list.sortedByDescending { it.resolution }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SampleData.videos)

    val visibleTracks: StateFlow<List<TrackItem>> = combine(
        allTracks, searchQuery
    ) { tracks, query ->
        if (query.isBlank()) tracks else tracks.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SampleData.tracks)

    val folders: StateFlow<List<FolderItem>> = allFolders

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSortMode(sort: VideoSort) {
        sortMode.value = sort
    }

    /** Applies the persisted Interface defaults. Safe to call repeatedly. */
    fun applyDefaults(defaultView: String, defaultSort: String) {
        viewMode.value = if (defaultView == "list") VideoViewMode.LIST else VideoViewMode.GRID
        sortMode.value = when (defaultSort) {
            "date" -> VideoSort.DATE
            "size" -> VideoSort.SIZE
            "duration" -> VideoSort.DURATION
            "name" -> VideoSort.NAME
            else -> VideoSort.NAME
        }
    }

    /** Clears in-memory resume progress (watch history) for the session. */
    fun clearWatchHistory() {
        allVideos.value = allVideos.value.map { it.copy(resumeMs = 0L) }
    }

    /** Empties the recent streams list. */
    fun clearRecentStreams() {
        _streams.value = emptyList()
    }

    fun toggleViewMode() {
        viewMode.value =
            if (viewMode.value == VideoViewMode.GRID) VideoViewMode.LIST else VideoViewMode.GRID
    }

    fun setResolutionFilter(filter: String) {
        resolutionFilter.value = filter
    }

    /** Adds a stream URL to the recent list when it looks like a URL. */
    fun addStream(url: String): Boolean {
        val trimmed = url.trim()
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return false
        val title = trimmed.substringAfterLast("/").ifBlank { trimmed }
        _streams.value = listOf(
            StreamItem(trimmed, title, "Just now")
        ) + _streams.value.filter { it.url != trimmed }
        return true
    }

    fun videoById(id: String): VideoItem? = allVideos.value.firstOrNull { it.id == id }
}
