# K-Player

A premium, modern Android media player for videos and music — glassmorphism
design, hardware decoding, subtitles, equalizer and network streaming.

**Developed by Shehzad Khan**

## Features

- **Library** — videos, music, folders and network streams in one place.
  Your own downloaded movies, videos and songs are scanned from device
  storage with MediaStore (`READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO`, asked
  at runtime with a rationale). Sample media is shown only as a fallback
  when no permission is granted or no files are found.
- **Video player** — fullscreen landscape, tap to toggle glass controls,
  double-tap to seek (5s / 10s / 15s / 30s, configurable), long-press for
  2x speed, drag gestures for brightness (left), volume (right) and seek
  (horizontal) with on-screen overlays, playback speed 0.5x–2x, aspect
  ratio Fit / Fill / Zoom, screen lock, background play, picture-in-picture
  and A-B repeat.
- **Resume playback** — Continue Watching with the exact position, and
  auto-resume on reopen. Resume mode is configurable: Always ask, Always
  resume or Never (Always ask shows a "Resume from 03:34?" prompt).
- **Decoding** — HW / HW+ / SW decoder modes with a hardware-acceleration
  toggle. Plays qualities up to 4K and all common audio codecs the
  platform supports (AAC, AC3, E-AC3, MP3, FLAC, Vorbis, Opus) via Media3.
- **Audio** — audio track switching with codec / channel info (AAC 5.1,
  AC3…), audio boost up to 200% (LoudnessEnhancer), audio passthrough
  (raw bitstream to the receiver; equalizer and boost are bypassed while
  it is on), preferred audio language.
- **Subtitles** — embedded tracks plus external files (.srt / .vtt / .ass),
  sync delay in ±0.5s steps, encoding (UTF-8 / Auto with BOM detection),
  font, size, color and background. Online subtitle search is present in
  Settings; it is not wired to a provider yet, so it explains that and
  points at loading from a file instead.
- **Equalizer** — real 5-band equalizer (60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz)
  on Android's audio-effects framework, presets (Normal, Bass Boost,
  Vocal, Rock, Pop, Classical), radial bass-boost knob and on/off switch.
- **Music player** — rotating album art over a blurred album-art
  background, shuffle, repeat, sleep timer, equalizer and playlist.
- **Playlist / queue** — long-press an item, then drag it up or down to
  reorder; now-playing indicator, tap to play, remove per item.
- **Video info** — file name, size, resolution, codec, bitrate, frame rate
  and audio tracks.
- **5 themes** — Nebula (default: midnight blue to violet, cyan #22D3EE to
  pink #F472B6), Emerald, Sunset, Crimson and Ocean. Picking a theme in
  Settings > Appearance re-themes the whole app immediately (background
  gradient, accent gradient, nav color, progress bars, buttons) and the
  choice is persisted with DataStore.
- **Settings** — Playback (resume mode, double-tap seek, A-B repeat,
  remember speed, auto-rotate, orientation lock, PiP, background play,
  default speed), Decoder, Audio, Subtitles, Gestures (with sensitivity),
  Appearance, Interface (default view, default sort, show hidden files),
  Storage & Privacy (clear cache / watch history / recent streams with
  confirmation, storage usage), Kids Lock (locks the player UI;
  long-press the lock icon to unlock) and About.

## Screens

1. **Splash** — glass K logo with play triangle, glow, "K-Player",
   "by Shehzad", auto-navigates to Home after ~1.5s.
2. **Home / Library** — glass bottom nav (Videos, Music, Folders, Network,
   Settings). Videos: search, sort/filter, Continue Watching row, grid/list
   toggle, glass cards with thumbnail, duration badge, resolution badge and
   resume progress. Music: list + glass mini-player. Folders: folder list
   with counts. Network: stream URL input + Play + recent streams.
3. **Video Player** — fullscreen landscape with all the controls above.
4. **Audio Track sheet** — languages with codec/channel info + Off.
5. **Subtitles sheet** — languages + Off + load from file + style controls.
6. **Equalizer dialog** — 5 vertical sliders, presets, bass knob, on/off.
7. **Music Player** — rotating album art, glass controls, sleep timer.
8. **Playlist / Queue** — draggable glass list, now-playing indicator.
9. **Settings** — all groups listed above.
10. **Video Info sheet** — file name, size, resolution, codec, bitrate,
    frame rate, audio tracks.

## Tech

- Kotlin + Jetpack Compose (Material 3), single Activity + Navigation Compose
- Media3 ExoPlayer 1.2.1 for real video/audio playback
- ViewModels + DataStore (settings and resume positions persist)
- Coil for thumbnails (gradient art generated in code — no network images)
- Package `com.captain.kplayer`, minSdk 26, targetSdk 34, compileSdk 34

## Build

### Android Studio

1. Open the project folder in Android Studio.
2. Let Gradle sync (JDK 17, Gradle 8.7).
3. Run on a device or emulator (or Build → Build APK(s) for an APK).

### GitHub Actions (cloud build — no computer needed)

Push the project to a GitHub repository. The workflow in
`.github/workflows/build-apk.yml` (JDK 17, Gradle 8.7) runs
`./gradlew assembleDebug` and uploads the APK as the `app-debug`
artifact. Download it from the Actions run and install it straight on
your phone.

> Note: if you push with a classic personal access token, the token needs
> the `repo` **and** `workflow` scopes — without `workflow`, creating files
> under `.github/workflows/` fails with a 404.

## Project structure

```
app/src/main/java/com/captain/kplayer/
├── MainActivity.kt
├── data/            Models, SampleData, MediaStoreRepository
├── player/          PlayerFactory (ExoPlayer, HW/HW+/SW), EqualizerManager
├── ui/
│   ├── theme/       Color (5 themes), Theme (KPlayerTheme, GradientBackground)
│   ├── components/  GlassComponents, ThumbnailArt
│   ├── navigation/  NavGraph
│   └── screens/     Splash, Home, VideoPlayer, MusicPlayer, Playlist,
│                    Settings, AudioTrackSheet, SubtitleSheet,
│                    EqualizerDialog, VideoInfoSheet
└── viewmodel/       SettingsViewModel (DataStore), LibraryViewModel,
                    PlayerViewModel (ExoPlayer, queues, tracks, sleep timer)
```

## Notes

- The APK cannot be built in this sandbox (low RAM — Gradle fails), so the
  project is delivered as source plus the GitHub Actions workflow above.
- Online subtitle search has a UI but no provider wired in yet; the app
  says so honestly instead of faking results.
