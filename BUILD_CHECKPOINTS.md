# FARIC Music Visualizer — Build Checkpoints

Purpose: keep a short, durable recovery point after every successful Android build so a new session can immediately find the last known-good implementation.

## Rule

After every successful Android build / GitHub Actions Android PASS, before starting the next implementation step:

1. Append a checkpoint here.
2. Record the app version and versionCode/build number.
3. Record the exact app/source commit SHA that produced the APK.
4. Record the successful Android workflow run number and run id.
5. Record the artifact name/id when available.
6. State exactly what was successfully completed in that build.
7. State what is still unverified (especially phone QA).
8. State the single next resume step.
9. Update `CURRENT_HANDOFF.md` and `ACTIVE_PLAN.md` if their resume point changed.

A CI build checkpoint is not phone acceptance. Keep those evidence levels separate.

---

## 2026-10-05 — v0.19.1 / build 90 — Android PASS

- Status: BUILD PASS / phone QA pending.
- App/source commit: `fd4bf265436507d7126d390fc56824dca7e21e81`.
- Android workflow: #457.
- Workflow run id: `37335782559`.
- Artifact: `FARIC-Music-Visualizer-v0.19.1-Debug`.
- Artifact id: `11356342984`.
- Artifact digest: `sha256:6b9452ad8ac1a59df1c7e0537fc0c6307c1397e79b5c487e650e546ef29ac205`.
- Successfully completed in this build:
  - offline full-song export candidate;
  - explicit export timeline independent from live player position/state;
  - projectM 4.1.7 external frame-time compatibility backport;
  - per-frame projectM time driven by `frameIndex / fps`;
  - source PCM cache/feed for offline projectM rendering;
  - deterministic FARIC/GF/EQ/FX/HUD timeline composition;
  - full-song 0:00 → source end export action;
  - H.264 video + AAC audio mux from original source;
  - cancellable preparation/rendering and progress UI.
- Supporting CI:
  - Validate #736 PASS on the same release SHA;
  - later documentation HEAD Validate #740 PASS.
- Not yet proven:
  - physical-phone 3-second offline preview;
  - full-song 0:00 → end behavior;
  - long-song stability/performance;
  - cancellation cleanup;
  - no black frames/OOM/ANR.
- Resume exactly here:
  1. install the v0.19.1 build-90 artifact on the phone;
  2. run the 3-second offline preview;
  3. if PASS, run full-song export while the live player is paused or positioned away from 0:00.
