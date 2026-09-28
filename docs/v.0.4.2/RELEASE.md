# v0.4.2 — projectM background architecture + mobile baseline

## Architecture decision

projectM is now the FARIC background visual engine.

FARIC-owned rendering will become the foreground layer.

This reverses the earlier spike assumption that projectM might become the foreground.

## Mobile optimization baseline

The projectM background now uses a balanced phone profile:

- Surface buffer rendered at 78% of view resolution and scaled by SurfaceView;
- projectM per-pixel mesh reduced to 72x40;
- target FPS metadata set to 60;
- soft preset transitions shortened to 0.70 seconds;
- EGL context remains preserved across temporary pause/resume;
- frame-rate samples are logged every 120 rendered frames.

The optimization is deliberately applied to projectM only. FARIC foreground/UI will remain full resolution.

## Why

projectM documentation explicitly warns that very large per-pixel meshes can severely reduce performance and gives 48x32 as a low-end example. The standalone frontend uses 96x54 as its normal mesh. FARIC therefore starts between those values at 72x40.

Shorter soft cuts also reduce the time spent rendering two presets simultaneously during transitions.
