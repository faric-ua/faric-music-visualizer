# FARIC Player — Shared Architecture

## Objective

One player UX, two playback backends:

```text
                   Player UI
                      |
              PlayerController
                      |
          +-----------+-----------+
          |                       |
 LocalPlaybackBackend      YouTubePlaybackBackend
    Media3/ExoPlayer          IFrame Player API
          |                       |
 local files / queue      video IDs / playlist IDs
```

## Planned module boundaries

### player-model
Pure Kotlin models:
- MediaIdentity;
- TrackMetadata;
- QueueItem;
- PlaybackState;
- RepeatMode;
- SourceCapabilities.

### player-core
Playback-neutral controller:
- queue state;
- previous/next;
- shuffle;
- repeat;
- seek;
- state restoration;
- media transitions.

### player-ui
Reusable Android UI:
- PulseDock;
- Now Playing;
- Library track list shell;
- Queue;
- player actions;
- capability-aware controls.

### visualizer-engine
Local-audio analysis:
- amplitude;
- FFT bands;
- beat/onset;
- SceneState;
- renderers.

### app
Standalone FARIC Music Visualizer / Player.

## Backend contract

A backend reports capabilities rather than making UI assumptions.

Example:

```text
canSeek
canSetVolume
canShuffle
canRepeat
canAnalyzePcm
canShowVideo
canPlayInBackground
```

The UI hides or changes behavior when a backend does not support a capability.

## Local backend

Media3 / ExoPlayer:
- local Uri playback;
- queue;
- exact position/duration;
- audio session;
- local FFT/Visualizer analysis;
- future MediaSession/notification.

## YouTube backend for YTM

Use official YouTube embedded playback based on video/playlist IDs.

YTM already owns YouTube identities, playlists, account/auth and API workflows.
The embedded player must consume those IDs, not replace YTM's data layer.

Expected flow:

```text
YTM playlist/project
    ↓
videoId / playlistId
    ↓
YouTubePlaybackBackend
    ↓
shared PlayerController
    ↓
PulseDock / Now Playing
```

## Important limitation

YouTube Data API metadata is not an audio-stream API.

Do not design around extracting direct audio/video stream URLs.

The official IFrame Player API supports:
- loading/cueing video IDs and playlists;
- play/pause;
- seek;
- next/previous;
- shuffle;
- loop;
- player events.

For embedded YouTube playback, full local PCM/FFT access is not assumed.
Therefore Scene Lab uses capability-aware modes:
- local source → true audio-reactive mode;
- YouTube embed → video mode / ambient visual mode unless a lawful, supported analysis source exists.

## YTM integration rule

Do not copy the whole standalone app into YTM.

Preferred path after the player API stabilizes:
1. keep player contracts reusable;
2. package shared modules as an Android library/AAR;
3. version the player library;
4. YTM consumes an exact tested version;
5. YTM-specific code only maps its playlist/project models into `MediaIdentity` / `QueueItem`.

This prevents two diverging player implementations.
