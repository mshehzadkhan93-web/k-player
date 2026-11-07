package com.captain.kplayer.data

/**
 * Sample library data so every screen works out of the box without real files.
 * Sample videos use the W3C, Blender (via archive.org) and test-videos.co.uk
 * public samples; sample audio uses the public SoundHelix samples. Every URL
 * below was verified live (HTTP 200 + video/audio content-type, durations
 * probed with ffprobe) on 2026-11-07. NOTE: Google's gtv-videos-bucket
 * (commondatastorage.googleapis.com/gtv-videos-bucket/sample) now answers
 * 403 AccessDenied to anonymous callers — no sample may point at it.
 * Sizes, durations, resolutions and bitrates below are the real values of
 * the files served at those URLs, not the old bucket's.
 */
object SampleData {

    private const val W3C_BUCKET = "https://media.w3.org/2010/05"
    private const val ARCHIVE = "https://archive.org/download"
    private const val TEST_VIDEOS = "https://test-videos.co.uk/vids"

    val videos: List<VideoItem> = listOf(
        VideoItem(
            id = "bbb",
            title = "Big Buck Bunny",
            genre = "Animation • Comedy",
            durationMs = 596_000L,
            resolution = "HD",
            url = "$W3C_BUCKET/bunny/movie.mp4",
            thumbKey = "bbb",
            sizeText = "238 MB",
            sizeBytes = 249224577L,
            dateAddedMs = 1793932800000L,
            codec = "H.264 / AAC",
            bitrateText = "3.3 Mbps",
            fps = 24,
            audioTracks = listOf(
                AudioTrackInfo("English", "AAC", "5.1", isDefault = true),
                AudioTrackInfo("Hindi", "AAC", "Stereo"),
                AudioTrackInfo("Urdu", "AAC", "Stereo")
            ),
            subtitleTracks = listOf(
                SubtitleTrackInfo("English", "Embedded"),
                SubtitleTrackInfo("Hindi", "Embedded")
            ),
            resumeMs = 214_000L
        ),
        VideoItem(
            id = "sintel",
            title = "Sintel",
            genre = "Fantasy • Drama",
            durationMs = 888_000L,
            resolution = "HD",
            url = "$ARCHIVE/Sintel/sintel-2048-stereo_512kb.mp4",
            thumbKey = "sintel",
            sizeText = "74 MB",
            sizeBytes = 77410288L,
            dateAddedMs = 1793846400000L,
            codec = "H.264 / AAC",
            bitrateText = "0.7 Mbps",
            fps = 24,
            audioTracks = listOf(
                AudioTrackInfo("English", "AAC", "5.1", isDefault = true),
                AudioTrackInfo("Arabic", "AC3", "5.1"),
                AudioTrackInfo("Spanish", "AAC", "Stereo")
            ),
            subtitleTracks = listOf(
                SubtitleTrackInfo("English", "Embedded"),
                SubtitleTrackInfo("Arabic", "Embedded"),
                SubtitleTrackInfo("Spanish", "Embedded")
            ),
            resumeMs = 402_000L
        ),
        VideoItem(
            id = "tos",
            title = "Tears of Steel",
            genre = "Sci-Fi • Action",
            durationMs = 734_000L,
            resolution = "HD",
            url = "$ARCHIVE/Tears-of-Steel/tears_of_steel_720p.mp4",
            thumbKey = "tos",
            sizeText = "73 MB",
            sizeBytes = 76435802L,
            dateAddedMs = 1793760000000L,
            codec = "H.265 / AAC",
            bitrateText = "0.8 Mbps",
            fps = 24,
            audioTracks = listOf(
                AudioTrackInfo("English", "AAC", "5.1", isDefault = true),
                AudioTrackInfo("Hindi", "AAC", "Stereo")
            ),
            subtitleTracks = listOf(
                SubtitleTrackInfo("English", "Embedded")
            )
        ),
        VideoItem(
            id = "ed",
            title = "Elephants Dream",
            genre = "Surreal • Animation",
            durationMs = 654_000L,
            resolution = "HD",
            // File name contains a space; %20 is required in the URL.
            url = "$ARCHIVE/elephants-dream_202403/Elephants%20Dream.mp4",
            thumbKey = "ed",
            sizeText = "74 MB",
            sizeBytes = 78106846L,
            dateAddedMs = 1793673600000L,
            codec = "H.264 / AAC",
            bitrateText = "1.0 Mbps",
            fps = 24,
            audioTracks = listOf(
                AudioTrackInfo("English", "AAC", "Stereo", isDefault = true)
            ),
            subtitleTracks = listOf(
                SubtitleTrackInfo("English", "Embedded")
            ),
            resumeMs = 96_000L
        ),
        VideoItem(
            id = "jelly",
            title = "Jellyfish",
            genre = "Nature • Ocean",
            durationMs = 10_000L,
            resolution = "HD",
            url = "$TEST_VIDEOS/jellyfish/mp4/h264/1080/Jellyfish_1080_10s_1MB.mp4",
            thumbKey = "jelly",
            sizeText = "1 MB",
            sizeBytes = 1048435L,
            dateAddedMs = 1793587200000L,
            codec = "H.264 / AAC",
            bitrateText = "0.8 Mbps",
            fps = 30,
            audioTracks = listOf(
                AudioTrackInfo("English", "AAC", "Stereo", isDefault = true)
            ),
            subtitleTracks = emptyList()
        ),
        VideoItem(
            id = "bbb10",
            title = "Big Buck Bunny (10s Clip)",
            genre = "Animation • Comedy",
            durationMs = 10_000L,
            resolution = "HD",
            url = "$TEST_VIDEOS/bigbuckbunny/mp4/h264/1080/Big_Buck_Bunny_1080_10s_1MB.mp4",
            thumbKey = "bbb10",
            sizeText = "1 MB",
            sizeBytes = 1046987L,
            dateAddedMs = 1793500800000L,
            codec = "H.264 / AAC",
            bitrateText = "0.8 Mbps",
            fps = 60,
            audioTracks = listOf(
                AudioTrackInfo("English", "AAC", "Stereo", isDefault = true)
            ),
            subtitleTracks = emptyList()
        ),
        VideoItem(
            id = "sintel10",
            title = "Sintel (10s Clip)",
            genre = "Fantasy • Drama",
            durationMs = 10_000L,
            resolution = "HD",
            url = "$TEST_VIDEOS/sintel/mp4/h264/720/Sintel_720_10s_1MB.mp4",
            thumbKey = "sintel10",
            sizeText = "1 MB",
            sizeBytes = 1047954L,
            dateAddedMs = 1793414400000L,
            codec = "H.264 / AAC",
            bitrateText = "0.8 Mbps",
            fps = 24,
            audioTracks = listOf(
                AudioTrackInfo("English", "AAC", "Stereo", isDefault = true)
            ),
            subtitleTracks = emptyList()
        ),
        VideoItem(
            id = "bbtrailer",
            title = "Big Buck Bunny (Trailer)",
            genre = "Animation • Comedy",
            durationMs = 33_000L,
            resolution = "HD",
            url = "$W3C_BUCKET/bunny/trailer.mp4",
            thumbKey = "bbtrailer",
            sizeText = "11 MB",
            sizeBytes = 11053871L,
            dateAddedMs = 1793328000000L,
            codec = "H.264 / AAC",
            bitrateText = "2.7 Mbps",
            fps = 25,
            audioTracks = listOf(
                AudioTrackInfo("English", "AAC", "Stereo", isDefault = true)
            ),
            subtitleTracks = emptyList()
        )
    )

    private const val AUDIO_BUCKET = "https://www.soundhelix.com/examples/mp3"

    val tracks: List<TrackItem> = listOf(
        TrackItem("t1", "Midnight Drive", "Neon Coast", "Afterglow", 284_000L, "$AUDIO_BUCKET/SoundHelix-Song-1.mp3", "t1"),
        TrackItem("t2", "Solar Winds", "Aurora Fields", "Starlight", 302_000L, "$AUDIO_BUCKET/SoundHelix-Song-2.mp3", "t2"),
        TrackItem("t3", "Glass Ocean", "Marina Blue", "Tides", 251_000L, "$AUDIO_BUCKET/SoundHelix-Song-3.mp3", "t3"),
        TrackItem("t4", "Velvet Sky", "Luna Park", "Night Drive", 318_000L, "$AUDIO_BUCKET/SoundHelix-Song-4.mp3", "t4"),
        TrackItem("t5", "Electric Dawn", "Pulsewave", "Currents", 269_000L, "$AUDIO_BUCKET/SoundHelix-Song-5.mp3", "t5"),
        TrackItem("t6", "Crystal Rain", "Aurora Fields", "Starlight", 294_000L, "$AUDIO_BUCKET/SoundHelix-Song-6.mp3", "t6"),
        TrackItem("t7", "Northern Lights", "Neon Coast", "Afterglow", 331_000L, "$AUDIO_BUCKET/SoundHelix-Song-7.mp3", "t7"),
        TrackItem("t8", "Slow Motion", "Marina Blue", "Tides", 276_000L, "$AUDIO_BUCKET/SoundHelix-Song-8.mp3", "t8")
    )

    val folders: List<FolderItem> = listOf(
        FolderItem("Movies", "/storage/Movies", 45, "movies"),
        FolderItem("Downloads", "/storage/Download", 128, "downloads"),
        FolderItem("Music", "/storage/Music", 312, "music"),
        FolderItem("Camera", "/DCIM/Camera", 89, "camera"),
        FolderItem("WhatsApp Video", "/WhatsApp/Media/Video", 57, "whatsapp"),
        FolderItem("Documentaries", "/storage/Documentaries", 23, "docs")
    )

    /**
     * Sample network streams. Every URL below was verified live (HTTP 200,
     * correct playlist/media content-type) on 2026-11-07. NOTE: the Google
     * gtv-videos-bucket MP4s (e.g. BigBuckBunny.mp4) are NO LONGER public —
     * the bucket now answers 403 AccessDenied to anonymous callers — so no
     * sample stream may point at it. The BipBop entry below replaces the
     * dead Big Buck Bunny MP4 stream; it is also visibly different content
     * from every library video, so tapping a stream can no longer be
     * mistaken for playing a library video.
     */
    val recentStreams: List<StreamItem> = listOf(
        StreamItem(
            "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            "Mux Test Stream (HLS)",
            "Yesterday"
        ),
        StreamItem(
            "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
            "Tears of Steel (HLS)",
            "2 days ago"
        ),
        StreamItem(
            "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8",
            "BipBop (Apple HLS)",
            "Last week"
        )
    )

    /** Audio languages offered in the Audio Track sheet. */
    val audioLanguages = listOf("English", "Hindi", "Urdu", "Arabic", "Spanish", "French", "German")

    /** Subtitle languages offered in the Subtitles sheet. */
    val subtitleLanguages = listOf("English", "Hindi", "Urdu", "Arabic", "Spanish", "French")

    /** Equalizer presets: five band levels in dB for 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz. */
    val equalizerPresets: Map<String, List<Float>> = mapOf(
        "Normal" to listOf(0f, 0f, 0f, 0f, 0f),
        "Bass Boost" to listOf(9f, 6f, 2f, 0f, -2f),
        "Vocal" to listOf(-3f, 0f, 4f, 7f, 3f),
        "Rock" to listOf(7f, 4f, -2f, 3f, 6f),
        "Pop" to listOf(3f, 5f, 6f, 3f, 1f),
        "Classical" to listOf(2f, 1f, 0f, 3f, 6f)
    )
}
