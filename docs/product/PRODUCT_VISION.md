# Product Vision

FARIC Music Visualizer turns music playback into a full-screen reactive visual scene.

## Reference direction

The initial visual target is close to a vertical music visualizer:
- rich moving background;
- high-contrast central focal object;
- surrounding light/shape energy;
- strong response on kicks/snare/onsets;
- continuous low-amplitude motion between beats.

The reference video itself is not stored in Git.

## MVP

User selects:
1. a local music file;
2. a background image or video;
3. a visual preset.

The app plays the song and renders a synchronized scene.

## Visual signal model

Do not wire renderer directly to raw audio.

Use:
`PCM/features → AudioFrame → BeatEvent/EnergyState → SceneState → Renderer`

This keeps beat detection, renderer and UI independently testable.

## Background generation strategy

Phase 1:
- user image;
- user video;
- procedural gradients/shapes.

Phase 2:
- procedural shader scenes;
- palette extraction;
- preset randomization.

Phase 3:
- optional AI-generated image backgrounds;
- optional generated animation/video;
- cache generated assets locally;
- generation is never required for offline playback.

## Non-goals for first release

- social network;
- cloud account;
- music streaming service integration;
- full video editor;
- on-device generative video model.

## 2026-09-30 — expanded product direction

The original MVP remains historical foundation, but the product direction now explicitly expands into three connected capabilities:

1. **Media player** — play music and video.
2. **Layered visual Board** — compose backgrounds, themes, Hero/GF elements, reactive FX and metadata as independent configurable layers.
3. **Music-video export** — render the same Board/project deterministically to video instead of screen-recording the live Android UI.

Above the Board, the live app uses a separate **PulseDeck Skin** for playback and editing controls. The Skin itself is configurable at block level, beginning with:
- show/hide block;
- block opacity;
- replaceable block skin/variant.

The Board and Skin are intentionally separate concepts so the user can have a rich control surface while exporting a clean visual composition.

Canonical detailed capture:
`docs/architecture/FARIC_LAYERED_BOARD_VISION.md`
