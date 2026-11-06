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
import com.captain.kplayer.ui.components.HomeTab
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
     * The resume store (same kplayer_resume DataStore as the resume
     * positions), attached once from Home so last-played stamps are
     * persisted and reloaded across restarts.
     */
    private var settingsStore: SettingsStore? = null
    private var lastPlayedById: Map<String, Long> = emptyMap()

    /**
     * Attaches the store and loads every saved played_<id> timestamp into
     * the in-memory library. Idempotent — later calls are ignored.
     */
    fun attachStore(store: SettingsStore) {
        if (settingsStore != null) return
        settingsStore = store
        viewModelScope.launch {
            store.allLastPlayed.collect { played ->
                applyLastPlayed(played)
            }
        }
    }

    /** Applies persisted timestamps to the videos currently in the library. */
    private fun applyLastPlayed(played: Map<String, Long>) {
        lastPlayedById = played
        if (played.isEmpty()) return
        allVideos.value = allVideos.value.map { video ->
            val ts = played[video.id]
            if (ts != null && ts > video.lastPlayedMs) video.copy(lastPlayedMs = ts) else video
        }
    }

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
                // A fresh scan replaces the list — re-apply the persisted
                // last-played stamps so Recently Played survives the scan.
                applyLastPlayed(lastPlayedById)
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

    /**
     * Task C: the selected home tab lives here, not in a composable's
     * remember — HOME leaves composition on every forward navigation
     * (player, folder, music player, Settings), which wiped a remembered
     * tab and always dropped the user back on Videos. Kept in the
     * ViewModel, Back returns to the tab the user was on (Folders after a
     * folder, Music after the music player, Network after a stream,
     * whatever tab Settings was opened from).
     */
    private val _selectedTab = MutableStateFlow(HomeTab.VIDEOS)
    val selectedTab: StateFlow<HomeTab> = _selectedTab

    fun selectTab(tab: HomeTab) {
        _selectedTab.value = tab
    }

    val searchQuery = MutableStateFlow("")
    val sortMode = MutableStateFlow(VideoSort.NAME)
    val viewMode = MutableStateFlow(VideoViewMode.GRID)
    val resolutionFilter = MutableStateFlow("All")

    /** Videos the user has started watching, for the Continue Watching row. */
    val continueWatching: StateFlow<List<VideoItem>> =
        allVideos
            .map { videos -> videos.filter { it.resumeMs > 0 } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Recently Played: videos the user has played, most recently played
     * first, max 10. A video counts as played when it has a last-played
     * stamp ([markVideoPlayed]) or a saved resume position. The row needs
     * title + last-played time, both on [VideoItem].
     */
    val recentlyPlayed: StateFlow<List<VideoItem>> =
        allVideos
            .map { videos ->
                videos.filter { it.lastPlayedMs > 0L || it.resumeMs > 0L }
                    .sortedByDescending { it.lastPlayedMs }
                    .take(10)
            }
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
        allVideos.value = allVideos.value.map {
            it.copy(resumeMs = 0L, lastPlayedMs = 0L)
        }
        settingsStore?.let { store ->
            viewModelScope.launch { store.clearLastPlayed() }
        }
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

    /**
     * Records that a video was just played, stamping it with the current
     * time so it surfaces in [recentlyPlayed]. Call when playback starts.
     * Builds on the existing in-memory watch-history state (resumeMs);
     * no second store is introduced.
     */
    fun markVideoPlayed(videoId: String) {
        val now = System.currentTimeMillis()
        allVideos.value = allVideos.value.map {
            if (it.id == videoId) it.copy(lastPlayedMs = now) else it
        }
        // Persist the stamp in the same resume store so Recently Played
        // survives app restarts.
        settingsStore?.let { store ->
            viewModelScope.launch { store.saveLastPlayed(videoId, now) }
        }
    }

    /**
     * Videos in one folder, for the folder-detail screen. [folderPath] is
     * a [FolderItem.path] ("/storage/<bucket>") or a plain bucket name.
     * Folders are grouped by the MediaStore bucket, which is carried on
     * [VideoItem.genre] (see MediaStoreRepository).
     */
    fun videosInFolder(folderPath: String): List<VideoItem> {
        val bucket = folderPath.substringAfterLast("/").ifBlank { folderPath }
        return allVideos.value.filter { it.genre == bucket || it.genre == folderPath }
    }
}
