# v0.6.3 — Export Lab deterministic frame proof

## Goal

Start the real preview/export path without screen recording.

This release proves that a selected FARIC theme can be rendered at an exact timestamp and exact output resolution independently from the live Android UI.

## Export Lab

Now Playing adds an Export action.

Export Lab supports:
- 9:16 — 1080x1920;
- 16:9 — 1920x1080;
- 1:1 — 1080x1080;
- 4:5 — 1080x1350.

The first proof exports a PNG frame into Pictures/FARIC.

This is intentionally not called finished video export yet.

## Deterministic renderer

The export frame receives:
- theme id;
- exact timestamp;
- track title/artist;
- duration/position;
- amplitude;
- bass;
- mid;
- high;
- beat.

Ready export-proof themes:
- Neon Emblem;
- Energy Core;
- Orbital Crown;
- Star Seed;
- Wave Idol;
- Vinyl;
- Cassette.

Vinyl and Cassette now have deterministic renderer implementations separate from the live View.

## Why this matters

The final MP4 exporter must not capture the UI.

The target pipeline remains:

audio → offline analysis → SceneSignal timeline → frame renderer(t) → H.264 → audio mux → MP4.

v0.6.3 validates the frame-renderer portion first.

## Next

- phone-check composition in all 4 aspect ratios;
- confirm exported PNG matches live theme closely;
- add offline audio analysis timeline;
- then encode a short H.264 clip;
- then mux original audio.
