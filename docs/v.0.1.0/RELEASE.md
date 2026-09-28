# v0.1.0 — Foundation + First Reactive Scene

Target:
- local audio playback;
- deterministic audio feature stream;
- beat/energy model;
- central orb;
- radial pulse/glow;
- image/video background;
- phone QA.

## Current candidate

Source now contains:
- Android package `com.saney.musicvisualizer`;
- Media3 local playback;
- audio-session FFT capture through Android `Visualizer`;
- bass/mid/high feature extraction;
- adaptive bass/onset pulse prototype;
- first Canvas renderer.

## Evidence boundary

Nothing in this candidate is considered phone-PASS until Android CI compiles/tests/builds it and a project-specific signed APK is tested on the phone.
