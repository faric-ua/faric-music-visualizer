# v0.6.1 — Standalone Hero Themes + Theme Picker

## What changed

Playback themes are now a real user-facing concept instead of documentation only.

Scene Lab opens a Playback Theme picker.

Currently selectable:
- Visualizer
- Neon Emblem
- Energy Core
- Orbital Crown
- Star Seed
- Wave Idol

Planned but shown as coming soon:
- Portrait Halo
- Glass Core
- Vinyl
- Cassette
- Poster

## Standalone renderer

Hero themes now render independently from projectM.

They use FARIC's live audio signal:
- amplitude;
- bass;
- mid;
- high;
- beat.

The first five standalone themes share one lightweight Canvas-based renderer but have different silhouettes and motion rules.

This is intentionally separate from the projectM compositor so future export can render the same theme without depending on a projectM preset.

## Theme behavior

### Neon Emblem
Organic energy contour around a center emblem.

### Energy Core
Dense center with bass expansion and beat shockwaves.

### Orbital Crown
Multiple rotating arcs with band-dependent motion.

### Star Seed
Six-point core with long bass/beat rays.

### Wave Idol
Organic symmetric silhouette driven mainly by mids/bass.

## Persistence

Selected Playback Theme is remembered across app restarts.

## Next

- phone visual review of all five Hero Themes;
- add favorite/hide behavior for themes;
- create deterministic time-based renderer contract;
- make Neon Emblem the first exportable scene;
- then add user logo / cover / background image/video.
