# FARIC Music Video Creator — Product Roadmap

## Product goal

The simple user flow is the north star:

1. pick a song;
2. choose a visual style;
3. optionally choose artwork/logo/background video;
4. preview;
5. export a finished video;
6. share it to YouTube, TikTok, Shorts, Reels, or elsewhere.

FARIC must make this possible without requiring a desktop editor.

## Scene stack

```text
AUDIO
  |
  +-- Background
  |     video / image / projectM / generated loop / native scene
  |
  +-- Hero / Playback Theme
  |     Neon Emblem / Energy Core / Vinyl / Cassette / Glass Core / ...
  |
  +-- Reactive FX
  |     bass pulse / spectrum / particles / flares / shockwaves
  |
  +-- Metadata
        title / artist / timer / waveform / lyrics / logo

                         ↓

                 LIVE PREVIEW

                         ↓

                 VIDEO EXPORT
```

Each layer must be independently selectable.

## Theme catalog v1

Hero / modern:
- Neon Emblem
- Energy Core
- Portrait Halo
- Glass Core
- Orbital Crown
- Star Seed
- Wave Idol

Retro:
- Vinyl
- Cassette

Graphic:
- Poster / Typography

Reactive:
- FARIC / projectM layered Visualizer

The catalog is append-only unless a theme is technically broken.

## First user-facing workflow

### Pick music
- local file first;
- later local media library;
- optional imported metadata/artwork.

### Pick style
- large thumbnail grid;
- favorite / hide;
- recent styles;
- random style.

### Customize
- background image/video;
- cover/logo;
- palette;
- reaction intensity;
- metadata on/off;
- text placement preset.

### Preview
- same scene definition used by export;
- quick low-resolution preview allowed;
- accurate time/bass/beat response.

### Export
Initial targets:
- 9:16 1080x1920;
- 16:9 1920x1080;
- 1:1 1080x1080;
- 4:5 1080x1350;
- H.264 + AAC MP4;
- 30 fps first, 60 fps later;
- clip range or full track;
- progress / cancel / retry.

## Export architecture

Do not screen-record the UI.

FARIC export must be deterministic:

```text
audio file
   |
offline analysis cache
   |
time-indexed SceneSignal
   |
PlaybackTheme + background + FX
   |
frame renderer(t)
   |
MediaCodec / Media3 export pipeline
   |
audio mux
   |
MP4
```

Preview and export must evaluate the same scene state for the same timestamp.

## Media3 direction

The app already uses Media3 1.11.1.

The export implementation should evaluate Media3 Transformer + media3-effect first because the same version supports:
- H.264/AAC export;
- image and video inputs;
- compositions with multiple sequences;
- OpenGL video effects;
- progress and cancel;
- MP4 muxing.

FARIC's custom audio-reactive renderer still needs a deterministic frame-at-time contract, so Media3 is the container/export pipeline rather than the product's visual identity.

## Delivery phases

### Phase A — Theme engine foundation
- theme registry;
- scene project model;
- theme persistence;
- thumbnail/picker UI;
- independent background / theme / FX choices.

### Phase B — Hero Pack 1
- Neon Emblem;
- Energy Core;
- Orbital Crown;
- Star Seed;
- Wave Idol;
- tune bass/mid/high/beat mappings.

### Phase C — Playback themes
- Portrait Halo;
- Vinyl;
- Cassette;
- Glass Core;
- Poster.

### Phase D — Media inputs
- album art;
- user image;
- user logo;
- user background video;
- crop/fit/blur/color treatment.

### Phase E — Offline analysis
- decode PCM without microphone capture;
- FFT bands;
- beat/onset timeline;
- normalized energy envelope;
- cache analysis next to project.

### Phase F — Export MVP
- one theme;
- one background image/video;
- track metadata;
- H.264/AAC MP4;
- 9:16;
- progress/cancel.

### Phase G — Export product
- all aspect ratios;
- all themes;
- 60 fps;
- full-song export;
- project save/load;
- share sheet;
- reusable presets.

## Quality bar

A theme should be kept only if it has:
- a distinct silhouette;
- strong music response without constant jitter;
- readable composition;
- good performance;
- visually useful behavior on both calm and aggressive music.

Ratings should eventually apply to:
- projectM backgrounds;
- FARIC foreground samples;
- playback themes;
- user-created presets.

## Copyright / publishing note

FARIC can export whatever audio the user supplies, but publishing rights are the user's responsibility.

The app should not imply that loading a track grants rights to upload it publicly.
