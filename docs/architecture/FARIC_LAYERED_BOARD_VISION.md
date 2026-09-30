# FARIC Layered Board + PulseDeck Skin Vision

Status: **product direction captured from user discussion**  
Captured: 2026-09-30  
Purpose: extend the existing FARIC Music Visualizer / PulseDeck history without replacing earlier plans.

## 1. Product in one sentence

FARIC is an Android-first media player and music-video creator where music or video can be played live, visual scenes are composed on a configurable multi-layer **Board**, the app controls sit above that Board as a configurable **PulseDeck Skin**, and the same scene definition can be rendered deterministically into exported video.

## 2. Relationship to existing project history

This document does **not** replace:
- `docs/product/PRODUCT_VISION.md`;
- `docs/architecture/ARCHITECTURE.md`;
- `docs/visualizer/PLAYBACK_THEME_EXPORT_RESEARCH.md`;
- `docs/design/PLAYER_UI_BLUEPRINT.md`;
- `docs/design/pulsedeck/prototypes/PULSEDECK_SKIN_V1.svg`;
- `docs/visualizer/LOGO_GF_REFERENCE_PACK.md`.

It connects those previously separate ideas into one composition model.

## 3. Two planes: Board and Skin

```text
┌─────────────────────────────────────────────┐
│ PULSEDECK SKIN / LIVE UI OVERLAY            │
│ transport · queue · scene · EQ · effects   │
│ configurable block visibility / opacity     │
│ replaceable block skins                     │
├─────────────────────────────────────────────┤
│ LAYERED BOARD                               │
│                                             │
│  L4  metadata / waveform / brand overlays  │
│  L3  reactive FX / particles / glow        │
│  L2  hero / GF / logo / creature           │
│  L1  theme / center object                  │
│  L0  background image/video/projectM       │
│                                             │
└─────────────────────────────────────────────┘
```

### Board

The Board is the visual composition that can be previewed live and rendered to video.

Board content may contain:
- image or video backgrounds;
- projectM / procedural backgrounds;
- cover art;
- standalone playback themes;
- FARIC/FMV/FVMP hero/GF assets;
- modular GF parts: frame, creature, wordmark, optional FX;
- waveform, spectrum, particles, glow, flares;
- title, artist, timer, logo and other metadata;
- later user-defined overlays, masks and scene elements.

### PulseDeck Skin

The Skin is the interactive control layer above the Board.

Minimum customization contract captured from the user:
- show/hide a UI block;
- block opacity / transparency;
- replace a block skin / visual variant.

Future-friendly extensions may include position, scale, spacing and saved skin presets, but those are not required by this capture.

The Skin should stay logically separate from the rendered Board. This keeps exported videos clean by default. A future explicit “export this overlay” option can be added for individual blocks if needed.

## 4. Playback model

FARIC should support both:
- music playback;
- video playback.

Both feed the same scene/composition system where technically possible.

```text
Audio / Video Source
        │
        ├──► Playback Engine
        │
        ├──► Audio Analysis / SceneSignal
        │
        └──► Track / media metadata
                   │
                   ▼
              Layered Board
                   │
           ┌───────┴────────┐
           ▼                ▼
      Live Preview      Offline Export
```

## 5. Export model

The exported video is not a screen recording of the Android UI.

Target contract:

```text
media source
   + scene/project settings
   + offline audio analysis
   + deterministic scene-at-time renderer
                    │
                    ▼
               video frames
                    │
                    ▼
              H.264 / MP4
                    │
                    + audio mux
```

The same Board/project description should drive both live preview and export.

Existing project work already contains the foundation for:
- offline audio analysis;
- deterministic SceneSignal timeline;
- H.264 proof;
- AAC/audio mux proof;
- PlaybackTheme registry;
- projectM background integration;
- standalone Hero themes.

## 6. Proposed Board layer model

The exact implementation can evolve, but the product model should remain understandable as layers.

### L0 — Background

Examples:
- image;
- video;
- cover-derived background;
- projectM;
- procedural scene;
- generated loop later.

Primary reactions:
- slow bass/energy drive;
- palette changes;
- travel/flow speed;
- blur/light intensity.

### L1 — Playback Theme / center environment

Examples:
- PulseCore;
- Vinyl;
- Cassette;
- Glass Core;
- Neon Emblem;
- poster/typography environment.

### L2 — Hero / GF

Whole asset mode:
- one transparent ready-made emblem.

Modular mode:
- back/frame;
- creature/mascot;
- front wordmark;
- optional dedicated FX layer.

Example heroes already explored:
- Cyber Shark;
- Cyber Panther;
- Mecha Tiger;
- Void Dragon;
- Neon Griffin;
- Thunder Wolf;
- Razor Raven;
- Inferno Phoenix;
- Plasma Cobra;
- Titan Scorpion.

### L3 — Reactive FX

Examples:
- radial spectrum;
- waveform;
- particles;
- lightning;
- splash/energy contour;
- shockwaves;
- bloom/glow;
- beat impulses.

### L4 — Metadata / branding

Examples:
- title;
- artist;
- timer;
- progress;
- album art;
- FARIC/FMV/FVMP logo;
- optional lyrics later.

## 7. Board element parameters

Every Board element should eventually have a shared minimum parameter model where applicable:

```text
enabled
opacity
position
scale
rotation
z-order / layer
blend mode (later)
palette/tint
reactivity amount
bass amount
mid amount
high amount
beat amount
```

Not every control must be exposed in the first UI. The point is to keep the scene format extensible.

## 8. Skin block model

The UI should be made from configurable blocks rather than one baked screenshot.

Examples:
- top brand/header;
- Library / Tone Lab / Scene Lab shortcuts;
- transport cluster;
- seek/progress block;
- quick actions;
- PulseDock mini-player;
- bottom navigation.

At minimum each block can expose:

```text
visible: true/false
opacity: 0..1
skinId: selected visual skin
```

This lets the same player become minimal, full-control, translucent or visually themed without rebuilding the Board.

## 9. Saved project concept

A saved FARIC project should be able to remember:
- selected media;
- Board aspect ratio;
- Board layers and their order;
- selected background/theme/GF;
- layer parameters;
- audio-reactive routing;
- export profile;
- selected PulseDeck skin and block configuration.

This is the bridge between “music player”, “visualizer” and “music-video creator”.

## 10. Preview/export parity rule

Hard rule:

> At the same project timestamp and with the same deterministic inputs, the Board shown in preview and the Board rendered for export should match visually within the renderer’s documented quality differences.

The PulseDeck Skin itself is not part of that parity requirement unless a UI block is explicitly marked exportable.

## 11. Current design baseline

The accepted UI identity remains PulseDeck V1:
- black/graphite foundation;
- orange + cyan accents;
- central PulseCore;
- luminous spectrum/waves;
- rounded glass-like surfaces;
- PulseDock;
- bottom navigation.

Canonical file:
`docs/design/pulsedeck/prototypes/PULSEDECK_SKIN_V1.svg`

## 12. Asset-library direction

Visual references and generated hero concepts belong under:
`docs/visualizer/assets/`

They are separated into:
- user references;
- generated FARIC concepts;
- generated FMV concepts;
- generated FVMP concepts;
- future production-ready layered sets.

Repository preview copies are reference assets, not necessarily final production resolution.

## 13. Near-term implementation sequence

1. Preserve and index visual references.
2. Keep PulseDeck Skin V1 immutable as the accepted baseline.
3. Define serializable Board/Layer model.
4. Make current PlaybackTheme renderer fit that Board model.
5. Add one modular Hero/GF set as proof: frame + creature + wordmark.
6. Add block-level PulseDeck skin settings: visible / opacity / skinId.
7. Make live preview use the same Board description as deterministic export.
8. Add project save/load only after the Board contract is stable enough to version.


## 14. Board transform controls

Phone evidence from the first Cyber Shark layered Board proof showed that a valid Hero/GF can still be unusable if its default scale or position collides with the live PulseDeck controls.

Therefore transform controls are a required part of the Board contract, not optional polish.

### Group-level transform

Every visual group, especially Hero/GF, should expose at minimum:

```text
enabled
x
y
scale
rotation
opacity
```

The common case is moving/resizing the whole Hero/GF while preserving internal layer registration.

Recommended user-facing controls:
- horizontal position;
- vertical position;
- size;
- rotation;
- opacity;
- reset;
- Fit Safe Area.

### Layer-level transform

Advanced mode should allow independent overrides for:
- frame;
- creature;
- wordmark;
- FX.

Each layer can override:

```text
offsetX
offsetY
scale
rotation
opacity
```

These manual transforms are applied before/around the audio-reactive motion envelope, so user layout and music reaction remain separate concepts.

### Safe-area rule

The Board renderer must know the currently occupied PulseDeck overlay region. A default Hero/GF preset should fit within the available visual area and must not assume the entire physical screen is free.

The first Cyber Shark phone proof established this requirement after the emblem and wordmark visibly collided with the player card.

### Persistence

Transforms belong to the saved project/theme state and should survive:
- theme switches;
- app restart;
- preview/export handoff.

Preview and export must use the same saved transform values.
