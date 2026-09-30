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

## 2026-09-30 — layered Board architecture extension

The current codebase has grown beyond the first linear MVP diagram. The target architecture now separates **media**, **render composition**, **live controls**, and **offline export**:

```text
Music / Video
     │
     ├──────────────► Playback Engine ──────────────┐
     │                                             │
     ├──────────────► Audio Analysis ─► SceneSignal│
     │                                             │
     └──────────────► Metadata                     │
                                                   ▼
                                           ┌───────────────┐
                                           │ LAYERED BOARD │
                                           │ background    │
                                           │ theme         │
                                           │ Hero / GF     │
                                           │ reactive FX   │
                                           │ metadata      │
                                           └───────┬───────┘
                                                   │
                              ┌────────────────────┴───────────────────┐
                              ▼                                        ▼
                     Live preview/compositor                  Deterministic export
                              │                                        │
                              ▼                                        ▼
                     PulseDeck Skin overlay                    H.264 + audio mux
                     (controls only)                                  MP4
```

The PulseDeck Skin is a UI plane above the Board, not a baked Board layer by default.

Detailed product/architecture contract:
`docs/architecture/FARIC_LAYERED_BOARD_VISION.md`
