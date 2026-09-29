# Playback Theme / Music Video Export Research

This document is the working notebook for FARIC playback themes, reference clips, and future music-video export.

## Product direction

FARIC should not be limited to a single visualizer.

The long-term model is:

```text
AUDIO
  |
  +-- Background layer
  |     album art / image / video / projectM / generated loop
  |
  +-- Playback Theme layer
  |     cassette / vinyl / portrait halo / glass object / logo energy
  |
  +-- Reactive FX layer
  |     spectrum / particles / glow / bass pulse / flares / distortion
  |
  +-- Metadata layer
        title / artist / timer / lyrics / branding

                ↓

        LIVE PREVIEW + VIDEO EXPORT
```

The same scene description should be usable for:
- live playback inside FARIC;
- deterministic offline video rendering;
- 9:16 / 1:1 / 4:5 / 16:9 export.

## Reference clip: 332877.mp4

Observed composition:
- vertical 9:16 frame;
- warm, blurred photographic/video background;
- rotating or morphing translucent glass/crystal object in the center;
- circular logo/brand element above the main title;
- track title and artist in the center stack;
- compact linear waveform beneath metadata;
- small CTA/subtitle below waveform;
- object lighting/glow changes while the background remains soft;
- composition is readable because all critical text stays near the safe center.

What FARIC should learn from it:
- a central 3D-like object can become the identity of a playback theme;
- background can be simple video/blur while the reactive object does the heavy visual work;
- metadata and waveform can remain stable while the object rotates/morphs;
- this is suitable for social export because it is vertically composed rather than cropped.

Proposed FARIC theme:
- name: Glass Core / Crystal Pulse;
- source art: user image / cover / generated texture;
- center: refractive pseudo-3D object;
- motion: slow rotation + beat-driven squash/stretch;
- bass: expansion and bloom;
- highs: edge sparkle / refraction;
- optional waveform and title stack.

## Reference clip: 332878.mp4

Observed composition:
- black/dark green vertical frame;
- sparse glowing particles in depth;
- central logo/emblem;
- organic neon ring/energy contour around logo;
- contour continuously changes shape;
- stronger deformations appear around beat/bass moments;
- digital timer underneath;
- very little text, so the logo and reactive ring own the scene;
- brief energy streaks/ribbons cross the center.

What FARIC should learn from it:
- a simple logo + reactive contour can look strong if motion quality is good;
- particles create depth cheaply;
- the contour should not just scale uniformly: it should deform locally;
- beat can trigger a short wave/ribbon crossing the center;
- this is a strong template for branding, labels, DJs, and track promos.

Proposed FARIC theme:
- name: Neon Emblem / Energy Crown;
- center image: logo, album art, monogram, or artist portrait crop;
- ring: waveform/spectrum hybrid with local deformation;
- particles: depth drift + beat burst;
- bass: global radius push;
- mids: contour wobble;
- highs: edge turbulence and spark density;
- beat: outward impulse + occasional ribbon shockwave.

## Existing themes already identified

1. Cassette
   - rotating reels;
   - customizable label/cover;
   - title + artist;
   - progress can drive reel/tape geometry.

2. Vinyl
   - spinning record;
   - center label with title/artist/cover;
   - tonearm movement;
   - subtle beat glow.

3. Portrait Halo
   - full-screen art;
   - circular spectrum around center artwork;
   - particles;
   - timer/metadata.

4. Glass Core / Crystal Pulse
   - rotating translucent central object;
   - warm or cinematic video background;
   - waveform + metadata.

5. Neon Emblem / Energy Crown
   - dark background;
   - center logo;
   - deforming neon contour;
   - particles and shockwave ribbons.

6. Poster / Typography
   - large art and text;
   - restrained audio-reactive accents.

7. FARIC Space
   - projectM or native space background;
   - FARIC reactive foreground.

## Theme engine contract

Every theme should expose a common set of inputs:

```kotlin
ThemeInput(
    title,
    artist,
    coverArt,
    logo,
    backgroundImage,
    backgroundVideo,
    durationMs,
    positionMs,
    amplitude,
    bass,
    mid,
    high,
    beat,
)
```

Every theme should support:
- preview;
- pause/resume;
- deterministic frame-at-time rendering;
- safe-area layout;
- palette override;
- foreground intensity;
- background intensity;
- metadata on/off;
- logo/cover source selection.

## Export architecture

Do not screen-record the live UI as the final export path.

Target architecture:

```text
Track audio
   |
Offline audio analysis
   |
Scene timeline + theme parameters
   |
Frame renderer at exact timestamp
   |
Video encoder
   |
Mux original/processed audio
   |
MP4
```

This gives:
- frame-perfect audio/reactive sync;
- identical output every render;
- export faster/slower than real-time depending on renderer;
- no dropped frames from phone UI load;
- clean 1080x1920 / 1920x1080 / square output.

Initial export formats:
- 9:16 — 1080x1920;
- 16:9 — 1920x1080;
- 1:1 — 1080x1080;
- 4:5 — 1080x1350.

Later:
- 1440p / 4K;
- 30 / 60 fps;
- short clip range or full song;
- burned-in lyrics;
- watermark/branding toggle.

## Reference implementations found

### PulseForge
Open-source local audio-visualizer editor with layered composition and deterministic MP4 export.

Useful ideas:
- one shared layer model;
- audio analysis separate from rendering;
- preset-driven scene stack;
- deterministic offline export;
- project save/load.

### spectral
Open-source audio-reactive video editor with preview and offline export sharing the same PixiJS runtime.

Useful idea:
- preview/export parity should be a hard architecture rule.

### Existing template ecosystems
Current commercial/free tools repeatedly use:
- circular logo spectrum;
- particle fields;
- neon tunnels;
- album cover pulse;
- vinyl record;
- cassette/VU;
- waveforms;
- pulse rings;
- liquid blobs;
- lightning;
- starfield tunnels.

This confirms these are useful baseline scene families, but FARIC should implement its own visual language and parameter system rather than cloning a template.

## FARIC differentiation

The goal is not "another visualizer template app".

FARIC-specific differentiators:
- independent background / playback-theme / reactive-FX layers;
- projectM as one possible background engine, not the whole product;
- append-only user-selectable FG sample library;
- per-track visual presets;
- ratings/favorites/hidden visual content;
- direct bass/mid/high/beat routing;
- eventual generated/AI artwork as an input, not as a required renderer;
- live player and export built from the same scene definition.

## Implementation backlog

### Playback Theme Engine
- [ ] Create theme interface/model.
- [ ] Add theme registry.
- [ ] Add selected-theme persistence.
- [ ] Add theme picker.
- [ ] Keep themes independent from projectM background selection.

### Theme prototypes
- [ ] Portrait Halo.
- [ ] Vinyl.
- [ ] Cassette.
- [ ] Glass Core / Crystal Pulse.
- [ ] Neon Emblem / Energy Crown.
- [ ] Poster / Typography.

### Media inputs
- [ ] Album cover source.
- [ ] User-selected image.
- [ ] User-selected background video.
- [ ] Logo/monogram source.
- [ ] Generated image source later.

### Export
- [ ] Define deterministic scene-at-time renderer contract.
- [ ] Offline FFT/band/onset analysis.
- [ ] Export frame pipeline.
- [ ] H.264/MP4 encoder path.
- [ ] Audio mux.
- [ ] 9:16 / 16:9 / 1:1 / 4:5.
- [ ] 30/60 fps.
- [ ] Short-range export.
- [ ] Full-song export.
- [ ] Export progress/cancel/retry.
- [ ] Thermal/battery guardrails.

### QA rules
- [ ] Preview and export must match visually at the same timestamp.
- [ ] Text stays inside format-specific safe areas.
- [ ] No dropped-reactivity frames in offline render.
- [ ] Track metadata remains readable over bright video backgrounds.
- [ ] Themes must remain usable without projectM installed/active.
