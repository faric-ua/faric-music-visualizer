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


---

## 2026-10-05 — Fast export surface pipeline — Android PASS

- Status: BUILD PASS / phone performance QA pending.
- App version metadata at this build: v0.19.1 / build 90.
- App/source commit: `9d19c8b3017aa3b39657fdc844f0464d3d4cf716`.
- Android workflow: #459.
- Workflow run id: `37348700831`.
- Artifact: `FARIC-Music-Visualizer-v0.19.1-Debug`.
- Artifact id: `11361767355`.
- Artifact digest: `sha256:c827cd4784e5d6a3a728fb53f308d6aaae9b446c80292b2249facbe2088709ab`.
- Successfully completed in this build:
  - added EGL-backed MediaCodec input-surface bridge;
  - removed the per-frame `Bitmap.getPixels -> IntArray -> Kotlin ARGB->YUV420` conversion from the video hot path;
  - offline frame timestamps are submitted explicitly to the encoder surface;
  - composition still uses the same deterministic offline timeline and active-layer renderer;
  - H.264 remains hardware MediaCodec + AAC source-audio mux.
- Supporting CI:
  - Validate #746 — PASS on the same source commit.
- Not yet proven:
  - physical-phone export speedup;
  - orientation correctness of EGL-uploaded frame;
  - full-song stability and thermal behavior;
  - whether projectM framebuffer readback is now the dominant bottleneck.
- Resume exactly here:
  1. keep this commit as the known-good Surface-encoder checkpoint;
  2. remove avoidable per-frame projectM readback allocations;
  3. bump/release the combined optimization as v0.19.2 only after the next Android PASS.


---

## 2026-10-05 — Fast export + reusable projectM readback — Android PASS

- Status: BUILD PASS / phone performance QA pending.
- App version metadata at this build: v0.19.1 / build 90.
- App/source commit: `09b0346310ef2164e0c565fac0866f2775d717ce`.
- Android workflow: #461.
- Workflow run id: `37349349348`.
- Artifact: `FARIC-Music-Visualizer-v0.19.1-Debug`.
- Artifact id: `11362525336`.
- Artifact digest: `sha256:98b9590a23afafdd8a96e0b4edaf99779a6e26a4af94f522e4062003b11f62c5`.
- Successfully completed in this build:
  - keeps the EGL -> MediaCodec Surface encoder path from Android #459;
  - removes repeated projectM offline readback allocation of the large direct RGBA buffer;
  - removes repeated allocation of the projectM pixel IntArray;
  - reuses one projectM readback Bitmap during offline export instead of allocating/recycling one every frame;
  - clears the reusable readback cache when projectM is released.
- Supporting CI:
  - Validate #749 — PASS on the same source commit.
- Not yet proven:
  - physical-phone speedup and exact export factor versus realtime;
  - output orientation/color correctness;
  - several-minute thermal/memory stability;
  - whether synchronous `glFinish + glReadPixels + RGBA->ARGB` conversion is now the remaining dominant bottleneck.
- Resume exactly here:
  1. promote the combined optimization to v0.19.2 / build 91;
  2. build/sign that exact version;
  3. phone-test 3-second preview and then the same full song used for v0.19.1 timing.


---

## 2026-10-05 — v0.19.2 / build 91 — Fast full-song export — Android PASS

- Status: BUILD PASS / phone QA pending.
- App/source commit: `bcab5fc8a1910581627fc82737ab0528e976b702`.
- Android workflow: #462.
- Workflow run id: `37350082893`.
- Artifact: `FARIC-Music-Visualizer-v0.19.2-Debug`.
- Artifact id: `11361439127`.
- Artifact digest: `sha256:4549cdb9198d1abd8b8f2ca741146da547875051d2c30497a95ebce053438f75`.
- Successfully completed in this build:
  - promoted the fast offline export pipeline to v0.19.2 / versionCode 91;
  - final video frames enter H.264 through EGL -> MediaCodec input Surface;
  - explicit offline frame timestamps are preserved with `eglPresentationTimeANDROID`;
  - removed CPU `Bitmap.getPixels -> IntArray -> Kotlin ARGB->YUV420` conversion;
  - projectM offline export reuses its large RGBA readback buffer, IntArray and Bitmap;
  - deterministic 0:00 -> end timeline, source PCM/projectM feed, FARIC/GF/EQ/FX/HUD composition and AAC source-audio mux remain intact.
- Supporting CI:
  - pre-release combined source Validate #749 — PASS on `09b0346310ef2164e0c565fac0866f2775d717ce`;
  - pre-release Android #461 — PASS on the same source;
  - documentation HEAD Validate #753 — PASS on `3e12cc87ed5e0d3ffa52eb13637660c70557b698`.
- Not yet proven on phone:
  - 3-second preview orientation/color correctness after EGL upload;
  - A/V sync;
  - actual full-song speedup compared with v0.19.1;
  - cancellation cleanup;
  - long-song heat/memory/black-frame stability.
- Resume exactly here:
  1. install `FARIC-Music-Visualizer-v0.19.2-Debug`;
  2. run the 3-second MP4 preview and verify orientation/colors/audio;
  3. if PASS, export the same full song that was slow in v0.19.1 and compare wall-clock time;
  4. if still too slow, profile/replace synchronous projectM `glFinish + glReadPixels + RGBA->ARGB` readback.
