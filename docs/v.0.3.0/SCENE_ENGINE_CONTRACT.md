# Scene Engine Contract

## Models

```text
SceneSpec
├── visualizerType
├── backgroundType
├── palette
├── intensity
└── changeAfterMs
```

## Runtime

```text
AudioCaptureAnalyzer
        ↓
    SceneSignal
        ↓
ReactiveSceneView ← SceneOrchestrator ← RandomSceneEngine
        ↓                    ↓
foreground renderer     background selection
        ↓                    ↓
          composed full-screen scene
```

## Separation rule

Audio features do not decide which scene exists.

Audio features only drive the currently selected scene.

Scene selection is handled by `SceneOrchestrator`.

This avoids visual instability and makes scene switching testable.

## AUTO behavior

Default interval target: 30 seconds.

On scene switch:

1. choose next foreground style;
2. choose next background style;
3. choose compatible palette;
4. start background transition;
5. keep current audio signal flow alive;
6. do not restart playback.

## Future external backgrounds

User-selected/AI files are represented as background assets and are decoded/preloaded separately from the foreground renderer.

A failed asset load must not break playback; the orchestrator falls back to a procedural background.
