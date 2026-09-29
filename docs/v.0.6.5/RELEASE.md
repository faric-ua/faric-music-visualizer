# v0.6.5 — Offline audio analysis for deterministic export

## Goal

Move export reactivity away from live Android Visualizer capture.

A finished FARIC video must react to the exact same song timeline every render, even if the UI is not playing in real time.

## OfflineAudioAnalyzer

FARIC now decodes the selected local audio track through Android MediaExtractor + MediaCodec.

Decoded PCM is converted to mono and analyzed in overlapping 2048-sample windows with a 1024-sample hop.

Each window produces:
- amplitude / RMS;
- bass energy (35–180 Hz);
- mid energy (180–2000 Hz);
- high energy (2000–10000 Hz);
- adaptive beat impulse.

A radix-2 FFT is implemented inside FARIC, so this path does not depend on microphone/system Visualizer capture.

## Deterministic timeline

The full track becomes an OfflineAnalysisResult:
- duration;
- signal frame interval;
- ordered SceneSignal list;
- signalAt(timeMs).

That means export can ask:

```
signalAt(0 ms)
signalAt(33 ms)
signalAt(66 ms)
...
```

and render every output frame from the same stored song analysis.

## Export Lab

Export Lab now adds:
- "Проаналізувати трек офлайн";
- ready/running analysis state;
- signal-frame count.

After analysis is ready, PNG export proof uses the offline timeline value at the current track position instead of the live Visualizer signal.

This is the first real preview/export parity step.

## Track source

PlaybackController now exposes the current local track URI to the export pipeline.

Selecting a different track invalidates the previous in-memory analysis.

## Next

- phone-test MP3/M4A/other local formats;
- compare offline bass/beat feel with live response;
- persist/cache analysis results;
- use the timeline to render a short sequence;
- encode first H.264 video proof;
- mux audio afterward.
