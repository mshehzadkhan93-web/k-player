package com.captain.kplayer.data

/**
 * Sample library data so every screen works out of the box without real files.
 * Sample videos use Google's public sample bucket; sample audio uses the
 * public SoundHelix samples. Both are stable, freely usable test media.
 */
object SampleData {

    private const val VIDEO_BUCKET = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample"

    val videos: List<VideoItem> = listOf(
        VideoItem(
            id = "bbb",
            title = "Big Buck Bunny",
            genre = "Animation • Comedy",
            durationMs = 596_000L,
            resolution = "4K",
            url = "$VIDEO_BUCKET/BigBuckBunny.mp4",
            thumbKey = "bbb",
            sizeText = "1.2 GB",
            sizeBytes = 1288490189L,
            dateAddedMs = 1793932800000L,
            codec = "H.264 / AAC",
            bitrateText = "16.5 Mbps",
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
            resolution = "4K",
            url = "$VIDEO_BUCKET/Sintel.mp4",
            thumbKey = "sintel",
            sizeText = "1.8 GB",
            sizeBytes = 1932735283L,
            dateAddedMs = 1793846400000L,
            codec = "H.264 / AAC",
            bitrateText = "18.2 Mbps",
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
            resolution = "4K",
            url = "$VIDEO_BUCKET/TearsOfSteel.mp4",
            thumbKey = "tos",
            sizeText = "1.5 GB",
            sizeBytes = 1610612736L,
            dateAddedMs = 1793760000000L,
            codec = "H.265 / AAC",
            bitrateText = "17.8 Mbps",
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
            durationMs = 653_000L,
            resolution = "HD",
            url = "$VIDEO_BUCKET/ElephantsDream.mp4",
            thumbKey = "ed",
            sizeText = "890 MB",
            sizeBytes = 933232640L,
            dateAddedMs = 1793673600000L,
            codec = "H.264 / AAC",
            bitrateText = "11.4 Mbps",
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
            id = "blaze",
            title = "For Bigger Blazes",
            genre = "Short • Promo",
            durationMs = 15_000L,
            resolution = "HD",
            url = "$VIDEO_BUCKET/ForBiggerBlazes.mp4",
            thumbKey = "blaze",
            sizeText = "4 MB",
            sizeBytes = 4194304L,
            dateAddedMs = 1793587200000L,
            codec = "H.264 / AAC",
            bitrateText = "2.1 Mbps",
            fps = 30,
            audioTracks = listOf(
                AudioTrackInfo("English", "AAC", "Stereo", isDefault = true)
            ),
            subtitleTracks = emptyList()
        ),
        VideoItem(
            id = "escape",
            title = "For Bigger Escapes",
            genre = "Short • Promo",
            durationMs = 15_000L,
            resolution = "HD",
            url = "$VIDEO_BUCKET/ForBiggerEscapes.mp4",
            thumbKey = "escape",
            sizeText = "4 MB",
            sizeBytes = 4194304L,
            dateAddedMs = 1793500800000L,
            codec = "H.264 / AAC",
            bitrateText = "2.0 Mbps",
            fps = 30,
            audioTracks = listOf(
                AudioTrackInfo("English", "AAC", "Stereo", isDefault = true)
            ),
            subtitleTracks = emptyList()
        ),
        VideoItem(
            id = "fun",
            title = "For Bigger Fun",
            genre = "Short • Promo",
            durationMs = 60_000L,
            resolution = "HD",
            url = "$VIDEO_BUCKET/ForBiggerFun.mp4",
            thumbKey = "fun",
            sizeText = "18 MB",
            sizeBytes = 18874368L,
            dateAddedMs = 1793414400000L,
            codec = "H.264 / AAC",
            bitrateText = "2.4 Mbps",
            fps = 30,
            audioTracks = listOf(
                AudioTrackInfo("English", "AAC", "Stereo", isDefault = true)
            ),
            subtitleTracks = emptyList()
        ),
        VideoItem(
            id = "joyride",
            title = "For Bigger Joyrides",
            genre = "Short • Promo",
            durationMs = 15_000L,
            resolution = "HD",
            url = "$VIDEO_BUCKET/ForBiggerJoyrides.mp4",
            thumbKey = "joyride",
            sizeText = "4 MB",
            sizeBytes = 4194304L,
            dateAddedMs = 1793328000000L,
            codec = "H.264 / AAC",
            bitrateText = "2.0 Mbps",
            fps = 30,
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
