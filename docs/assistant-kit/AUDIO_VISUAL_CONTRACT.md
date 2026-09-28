# Audio / Visual Contract

## AudioFrame

Renderer-independent feature packet should eventually expose:
- timestamp;
- normalized amplitude;
- smoothed amplitude;
- low/bass energy;
- mid energy;
- high energy;
- spectral centroid or equivalent brightness;
- optional compact spectrum bins.

## BeatEvent

A beat/onset event contains:
- timestamp;
- confidence/strength;
- relevant band/energy hint when available.

Renderer must not infer a new beat independently from UI frame timing.

## Smoothing

Separate:
- fast attack;
- slower release;
- beat impulse decay.

This prevents nervous flicker while preserving punch.

## Latency

Audio analysis and visual response must be measured on a real phone.
Do not claim sync from code inspection alone.

## SceneState

SceneState combines:
- continuous energy;
- beat impulse;
- user sensitivity;
- preset parameters;
- palette;
- background motion.

The renderer consumes SceneState only.
