# v0.4.0 — projectM integration spike + screen-off playback hotfix

## Goals

1. See projectM running on the real phone before investing in a full preset/browser integration.
2. Keep local audio playing when the display turns off.

## projectM spike

- libprojectM pinned to stable tag `v4.1.7`;
- built through Android NDK/CMake for `arm64-v8a`;
- separate fullscreen `projectM LAB` screen;
- four tiny upstream test presets bundled for integration validation;
- tap the projectM screen to switch preset;
- Android Visualizer waveform is converted to 16-bit mono PCM and fed into projectM while the lab is active.

This is an integration experiment, not the final FARIC compositor.

If projectM looks good on-device, the next phase is:
- render projectM as a foreground texture/layer;
- keep FARIC background engine independently underneath;
- add vetted/user-imported preset packs.

## Screen-off playback

Root cause found in FARIC code:
`MainActivity.onStop()` explicitly called `controller.pause()`.

Android calls `onStop()` when the screen locks, so the app itself paused the music.

v0.4.0 removes that pause.

### Boundary

This hotfix should keep playback alive through ordinary screen lock/Home while the Activity/process remains alive.

For production-grade long-lived background playback, the Player + MediaSession must move into a MediaSessionService. That migration remains required and is documented as the next playback architecture task.
