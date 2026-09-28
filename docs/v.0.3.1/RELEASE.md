# v0.3.1 — Instant Reactivity + Warp Background

## Phone finding

The first v0.3.0 scene engine was visually too soft:
- frequency-band changes arrived late;
- foreground movement was not strong enough;
- background and foreground felt like one shared animation;
- control glyphs were not visually centered;
- desired background direction is fast forward travel through stars / space.

## Reactivity contract

Foreground and background are separate reactive systems.

### Foreground
Driven primarily by:
- bass;
- mid;
- high;
- beat impulse.

Attack must feel immediate. Release may be smoother.

### Background
Driven by its own `backgroundDrive`:
- total amplitude;
- high-frequency energy;
- bass energy;
- beat impulse.

Background motion must continue independently of the foreground renderer.

## Analyzer changes

- Android Visualizer capture rate: maximum supported rate, not half-rate;
- band energy: peak + RMS instead of simple average;
- quiet FFT bins receive a mild nonlinear lift;
- bass/mid/high boundaries are explicit;
- beat cooldown/threshold tuned for faster response.

## Warp background

`WARP_STARFIELD` is now a first-class background.

Visual target:
- forward travel through space;
- stars stretch radially from a vanishing point;
- music energy controls travel speed and streak length;
- beats create short acceleration/brightness impulses;
- starfield remains behind the independent foreground visualizer.

Random selection is weighted toward WARP_STARFIELD, but immediate background repetition remains forbidden.
