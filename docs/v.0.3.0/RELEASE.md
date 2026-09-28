# v0.3.0 — Random Visualizer Core

## Product reason

PulseDeck only becomes a distinctive product if the visual scene feels alive instead of looking like a fixed player skin.

v0.3.0 establishes the scene engine as a first-class subsystem:

- foreground visualizer is independent from background;
- scene selection is controlled random, not chaotic random;
- background selection is controlled independently;
- the same scene engine can later be reused by the standalone app and YTM player shell.

## Two-layer contract

### Layer A — foreground visualizer

Music-reactive rendering:

- radial pulse;
- spectrum bars/rays;
- wave ribbon;
- later: particles / shader scenes.

### Layer B — background

Atmosphere behind the foreground:

- procedural / bundled background;
- user-imported image;
- AI-generated image cache;
- later: short loop video.

Foreground and background are selected independently and combined into one `SceneSpec`.

## v0.3.0 MVP

- `SceneSpec` model;
- visualizer/background/palette enums;
- controlled random engine with immediate-repeat prevention;
- at least 3 foreground visualizer styles;
- at least 4 built-in procedural backgrounds;
- manual Shuffle Scene action;
- automatic scene change interval;
- background crossfade;
- keep local FFT/beat input;
- keep PulseDeck playback controls.

## Controlled random rules

- never repeat the same foreground style immediately if alternatives exist;
- never repeat the same background immediately if alternatives exist;
- scene changes happen on a bounded timer, not on every beat;
- beat/FFT drives animation inside the selected scene;
- later policy options: track start / 30s / manual / hybrid.

## Background source priority

Future AUTO mode:

1. user-imported background pack;
2. AI cache;
3. bundled/procedural source pack.

A missing source must gracefully fall back to the next source.

## Storage contract

Planned external project storage:

```text
Documents/FARIC-Music-Visualizer/
└── backgrounds/
    ├── user/
    ├── ai-cache/
    └── video/
```

Bundled/procedural sources remain inside the app package.

## Non-goals in first v0.3.0 candidate

- AI generation inside the app;
- arbitrary video-loop playback;
- full Scene Lab settings UI;
- persistence of favorite presets;
- YTM integration.

Those come after the random scene engine is proven visually on the phone.
