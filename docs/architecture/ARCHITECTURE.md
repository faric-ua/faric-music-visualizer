# Architecture

```text
Audio Source
   |
   v
Playback Engine
   |
   v
Audio Analyzer
(amplitude / bands / onset)
   |
   +----> AudioFrame stream
   |
   v
Beat Engine
   |
   +----> BeatEvent / EnergyState
   |
   v
Scene Controller
(preset + smoothing + palette)
   |
   v
Renderer
(background + center + radial FX + particles)
   |
   v
Full-screen Android UI
```

## Planned Android stack

- Kotlin;
- AndroidX / lifecycle-aware state;
- Media3 for playback;
- renderer starts simple and measurable;
- advanced shader/GPU path added after MVP evidence.

Do not choose advanced rendering APIs only because they look impressive.
Prove timing, lifecycle and thermal behavior on the phone.

## Modules/concepts

- `playback`: transport, track state, position;
- `analysis`: audio feature extraction;
- `beat`: onset/beat/energy model;
- `scene`: renderer-independent visual state;
- `render`: Canvas/GPU implementation;
- `background`: image/video/procedural providers;
- `settings`: persistent user tuning;
- `export`: future recording/rendering.
