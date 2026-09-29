# v0.6.8 — Continuous Layer-1 live reactivity hotfix

## Finding

A phone screen recording supplied on 2026-09-29 shows the standalone Energy Core Layer 1 reacting strongly immediately after Play/resume, then settling into almost static motion after roughly 2–3 seconds while playback time continues advancing.

The recording has no audio track, so it is evidence of visible Layer-1 dynamics loss, not proof of audio/video sync.

## Cause addressed

The live foreground consumed absolute FFT band levels. After Android Visualizer's captured level settled, small ongoing musical changes occupied too little of the 0..1 renderer range. The initial silence → playback jump looked strongly reactive, but later bass/mid/high changes were visually compressed.

## Hotfix

`LiveSignalDynamics` now sits between `SpectrumMath` and `SceneSignal`.

It:
- keeps a slow local baseline for amplitude / bass / mid / high;
- expands small ongoing deviations into useful Layer-1 motion range;
- keeps the existing fast renderer attack / slower release;
- detects sustained silence and rebases on the next non-silent frame;
- avoids treating Pause → Play level restoration as a giant musical transient.

`AdaptiveBeatDetector` also resets its baseline on silence so the first resumed frame is not emitted as a fake beat.

## Scope

The shaping happens before `PlaybackController` fans out `SceneSignal`, so the same fix applies to:
- standalone Hero themes such as Energy Core;
- ReactiveSceneView;
- native FARIC foreground in the projectM compositor.

projectM's own PCM feed is unchanged.

## Acceptance

Phone QA must prove:
- Energy Core stays visibly reactive for at least 15 seconds;
- bass/beat response remains obvious after the first 3 seconds;
- Pause for at least 2 seconds → Play does not cause a multi-second false giant pulse;
- at least two other Layer-1 themes still react correctly.
