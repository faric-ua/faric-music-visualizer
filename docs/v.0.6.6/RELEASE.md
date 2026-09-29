# v0.6.6 — First deterministic H.264 video proof

## Goal

Render a real video sequence from the offline music-analysis timeline.

This is the first FARIC-generated MP4 video path that does not screen-record the Android UI.

## H.264 proof

Export Lab can now create a short 3-second H.264 proof after offline analysis is ready.

Current proof profile:
- 15 fps;
- maximum long edge 960 px;
- H.264 / AVC;
- MP4 container;
- generated from exact offline SceneSignal timestamps;
- saved to Movies/FARIC.

The proof begins at the current playback position.

## Rendering

For every output frame FARIC:
1. asks OfflineAnalysisResult.signalAt(frameTime);
2. creates ThemeInput from that exact signal;
3. renders the selected theme;
4. converts the frame to YUV420;
5. feeds Android MediaCodec H.264 encoder;
6. writes encoded samples through MediaMuxer.

Supported deterministic themes:
- Neon Emblem
- Energy Core
- Orbital Crown
- Star Seed
- Wave Idol
- Vinyl
- Cassette

## Encoder compatibility

FARIC discovers an available AVC encoder and supports:
- YUV420 planar;
- YUV420 semi-planar;
- YUV420 flexible.

The proof intentionally uses reduced resolution and 15 fps so the first phone test measures correctness before full-HD optimization.

## Current limitation

The v0.6.6 proof is VIDEO ONLY.

Original music is not muxed into the file yet.

Next step:
- add AAC audio path / mux;
- then extend clip duration;
- then move toward 1080p 30 fps and full-track export.

## Why this matters

The core export chain now exists:

offline audio analysis → deterministic frame render → H.264 → MP4.

The only major missing piece before a real shareable music-video proof is audio.
