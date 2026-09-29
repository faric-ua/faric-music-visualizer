# v0.6.2 — Vinyl + Cassette + projectM heavy-preset telemetry

## Playback Themes

Two retro themes are now selectable alongside Hero Themes.

### Vinyl
- rotating record;
- visible grooves;
- reactive bass/beat glow;
- rotating center label;
- current track title is rendered on the label;
- playback pause stops the rotation.

### Cassette
- animated reels;
- track title on the cassette label;
- reactive size/glow accents;
- beat indicator;
- playback pause stops reel rotation.

The original Visualizer and five Hero Themes remain selectable.

## projectM performance diagnosis

projectM preset switching now records actual native load time.

The LAB status can show:
- LOAD N ms;
- HEAVY count.

A preset whose smoothed load reaches 1200 ms is treated as heavy.

FAST/TOP excludes previously measured heavy presets.
ALL still keeps them available.

This separates two different performance issues:
- FARIC UI/queue work;
- actual projectM preset parse/shader load time.

## Library

Library Index v2 remains:
- FAST/TOP = indexed 1,200 seed pool;
- ALL = indexed 9,795;
- source preset files stored once;
- no old 4,898-file duplicate pool.

## Next

- phone-review Vinyl and Cassette;
- collect real LOAD timings from slow projectM presets;
- tune HEAVY threshold after phone data;
- then begin deterministic export proof with one standalone theme.
