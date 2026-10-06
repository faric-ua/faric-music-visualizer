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


---

## 2026-10-05 — Raw projectM offline readback fast path — Android PASS

- Status: BUILD PASS / phone performance QA pending.
- App version metadata at this build: v0.19.2 / build 91.
- App/source commit: `73cd730ea9394dd550d3dd87d2de133dc0e7465e`.
- Android workflow: #464.
- Workflow run id: `37355612628`.
- Artifact: `FARIC-Music-Visualizer-v0.19.2-Debug`.
- Artifact id: `11364308392`.
- Artifact digest: `sha256:81ae4859e8a51e418427770e49ddd16453b2689404736af094412cbd883e0052`.
- Successfully completed in this build:
  - preserved the preview contract: 3-second MP4 starts from the current playback scrubber position;
  - removed the Kotlin per-pixel RGBA->ARGB loop from the dynamic offline projectM readback path;
  - removed the explicit `glFinish()` from that hot path and relies on synchronous `glReadPixels`;
  - copies the reusable GL RGBA buffer directly into the reusable Bitmap;
  - corrects GL vertical orientation and red/blue channel layout during composition instead of another CPU pixel pass;
  - keeps the v0.19.2 EGL -> MediaCodec Surface encoder path.
- Supporting CI:
  - Validate #759 — PASS on the same source commit.
- Evidence from user screen recording before this optimization:
  - a 3.008-second / 90-frame preview took roughly 28 seconds wall-clock to finish;
  - therefore remaining export performance was about 9x slower than realtime.
- Not yet proven:
  - physical-phone speedup after removing the projectM per-pixel loop;
  - orientation/color correctness of the raw GL correction path;
  - full-song stability.
- Resume exactly here:
  1. promote this optimization to the next build/version;
  2. build/sign exact release candidate;
  3. phone-test the same 3-second preview and compare wall-clock time against ~28 seconds.


---

## 2026-10-05 — v0.19.3 / build 92 — projectM raw readback acceleration — Android PASS

- Status: BUILD PASS / phone performance QA pending.
- App/source commit: `7add461579d259150f52e860d14ad6df226e2793`.
- Android workflow: #465.
- Workflow run id: `37356194982`.
- Artifact: `FARIC-Music-Visualizer-v0.19.3-Debug`.
- Artifact id: `11364403633`.
- Artifact digest: `sha256:0c15d5ae800ea295541a07b90837f79222e311e48481dd3c93366f71b9701cdc`.
- Successfully completed in this build:
  - preserves 3-second preview behavior: starts from the current playback scrubber position;
  - keeps full-song export fixed at 0:00 -> source end;
  - keeps EGL -> MediaCodec Surface encoding from v0.19.2;
  - removes the dynamic projectM Kotlin per-pixel RGBA->ARGB conversion loop;
  - removes the explicit `glFinish()` from the dynamic offline readback hot path;
  - copies reusable GL RGBA data directly into the reusable projectM Bitmap;
  - corrects vertical orientation and red/blue channel layout during composition draw.
- Supporting CI:
  - pre-release Validate #759 PASS and Android #464 PASS on implementation SHA `73cd730ea9394dd550d3dd87d2de133dc0e7465e`;
  - documentation Validate #763 PASS on `5e653e9baf8b90959eb5016c73efd5f9977c8386`.
- Baseline phone evidence before this build:
  - 3.008-second / 90-frame preview took about 28 seconds wall-clock.
- Phone QA:
  - rerun the same 3-second preview;
  - confirm it still starts at current scrubber position;
  - confirm image is upright and colors are correct;
  - measure wall-clock render time versus ~28 seconds;
  - confirm audio remains synchronized.
- Resume exactly here:
  1. install `FARIC-Music-Visualizer-v0.19.3-Debug`;
  2. rerun the same 3-second preview;
  3. report wall-clock time + whether orientation/colors/audio are correct;
  4. if PASS and materially faster, continue to full-song export.


---

## 2026-10-06 — v0.19.4 / build 93 — export geometry optimization — Android PASS

- Status: BUILD PASS / phone QA in progress.
- App/source commit: `c43f8f9b10d3a8c6969d3da8514a3509da350e55`.
- Android workflow: #469, attempt 3.
- Workflow run id: `37362315944`.
- Artifact: `FARIC-Music-Visualizer-v0.19.4-Debug`.
- Artifact id: `11384577679`.
- Artifact digest: `sha256:8bc92761876496e90df2b9decec283c50677cce8031d4ea07d7db1300a63dcb5`.
- Validate #776 attempt 3 — PASS on `5cb4b1d36d42ac5f99d6b5e3efbe4182ac4e011b`.
- Successfully completed in this build:
  - preserves the current-position 3-second preview contract;
  - preserves 30 FPS final output;
  - projectM manual export surface now matches the selected output geometry before applying the existing render scale;
  - keeps raw GL readback acceleration from v0.19.3;
  - keeps EGL -> MediaCodec Surface encoding from v0.19.2.
- Phone QA evidence received:
  - screen recording `447892.mp4` shows a cold-cache preview;
  - PCM preparation consumed roughly 12-13 seconds;
  - actual 3-second rendering then consumed roughly 15-16 seconds;
  - completion produced 1080x1920 / 90 frames.
- Interpretation:
  - this cold run is not directly comparable to the v0.19.3 ~16-second warm-cache baseline because v0.19.4 rebuilt the PCM cache first;
  - a second immediate preview on the same song without restarting the app is required for an apples-to-apples timing comparison.
- Resume exactly here:
  1. rerun the same 3-second preview immediately on the same track, without app restart and without rerunning offline analysis;
  2. confirm that PCM preparation is skipped;
  3. measure wall-clock render time from `Рендерю 3 секунди…` to `Готово`;
  4. upload the resulting 3-second MP4 to verify orientation/colors/audio/crop.


---

## 2026-10-06 — v0.19.5 / build 94 — export-stage timing diagnostics — Android PASS

- Status: BUILD PASS / phone QA evidence received and analysis pending.
- App/source commit: `44e8e6b17b9351a9799e6aaa080912f216d937bc`.
- Android workflow: #472, attempt 1.
- Workflow run id: `37401600257`.
- Artifact: `FARIC-Music-Visualizer-v0.19.5-Debug`.
- Artifact id: `11385169289`.
- Artifact digest: `sha256:5fe39a6870aa25c3c65dfba33765fc0e54f9ccd316faa59d0bdd89f72ea55f67`.
- Validate: #784 — PASS on the same source SHA.
- Successfully completed in this build:
  - preserves v0.19.4 export geometry and current-position 3-second preview semantics;
  - adds per-stage timings for projectM frame generation, composition, encoder submission, audio transcode, mux, publish/save, and total export wall-clock;
  - surfaces the timing breakdown in the completion UI for phone-side bottleneck diagnosis.
- Unverified at checkpoint time:
  - exact timing values from the phone recording;
  - whether projectM, composition, encoder, audio, mux, or save is the dominant stage.
- Single next resume step:
  - inspect phone evidence `447911.mp4` and `FARIC-preview-1791252342014.mp4`, record exact stage timings, verify output integrity, then choose the next optimization from measured bottleneck data.


---

## 2026-10-06 — v0.19.6 / build 95 — persistent export timing dialog — Android PASS

- Status: BUILD PASS / phone QA measured.
- App/source commit: `a4309529161e40bc0e8dca3bf1686ee4807bd608`.
- Android workflow: #474, attempt 1.
- Workflow run id: `37402845096`.
- Artifact: `FARIC-Music-Visualizer-v0.19.6-Debug`.
- Artifact id: `11385957232`.
- Artifact digest: `sha256:0668f1aec77d529d4e0e3183d30fccd9c8fd7e6f8c93c09343a5e530278a0306`.
- Validate #787 — PASS on the same source SHA.
- Successfully completed in this build:
  - replaces truncated timing Toast with a persistent result dialog;
  - preserves v0.19.5 stage timing instrumentation;
  - preserves v0.19.4 export geometry and current-position preview behavior.
- Phone QA evidence:
  - screen recording `447915.mp4`;
  - exported preview `447916.mp4`.
- Exact measured timings for a 3-second / 90-frame export:
  - projectM: 1896 ms;
  - composition: 11808 ms;
  - encoder: 435 ms;
  - audio: 424 ms;
  - mux: 112 ms;
  - save: 215 ms;
  - total: 15575 ms.
- Output verification:
  - 1080x1920;
  - 30 FPS;
  - 90 frames;
  - 3.000 s container duration;
  - H.264 High ~12.4 Mbps;
  - AAC LC stereo 48 kHz ~160 kbps;
  - orientation/colors/composition appear correct.
- Performance conclusion:
  - composition is the dominant bottleneck (~75.8% of total wall-clock);
  - projectM is secondary (~12.2%);
  - encoder/audio/mux/save are minor.
- Single next resume step:
  - instrument `CompositionExportRenderer` internally (projectM draw / background layers / GF-Cyber-Shark / PulseDeck-HUD / overlays or equivalent actual render groups) and run one more 3-second preview before choosing the next optimization.


---

## 2026-10-06 — v0.19.7 / build 96 — composition layer profiler — Android PASS

- Status: BUILD PASS / phone QA measured.
- App/source commit: `a5f7ac9c4baa6d670afe1879f217838ff51b5a7d`.
- Android workflow: #478, attempt 1.
- Workflow run id: `37403965479`.
- Artifact: `FARIC-Music-Visualizer-v0.19.7-Debug`.
- Artifact id: `11386582510`.
- Artifact digest: `sha256:d498a4ba8b6d0fa2d210f6eff0c236fe37f62d2c4e1c3e62b3f9d6d27c31872c`.
- Validate #794 — PASS on the same source SHA.
- Successfully completed in this build:
  - preserves all prior export behavior and output geometry;
  - profiles CompositionExportRenderer internally by layer/group.
- Phone QA evidence:
  - screen recording `447926.mp4`;
  - exported preview `447927.mp4`.
- Exact 3-second / 90-frame timing:
  - projectM frame generation: 2063 ms;
  - composition: 12998 ms;
  - encoder: 453 ms;
  - audio: 475 ms;
  - mux: 140 ms;
  - save: 199 ms;
  - total: 17008 ms.
- Composition layer timing:
  - clear: 85 ms;
  - projectM draw: 6471 ms;
  - FARIC reactive: 0 ms;
  - overlay: 0 ms;
  - Big EQ: 0 ms;
  - Cyber Shark: 4691 ms;
  - effects: 450 ms;
  - HUD update: 26 ms;
  - HUD draw: 1270 ms.
- Output verification:
  - 1080x1920;
  - 30 FPS;
  - 90 frames;
  - H.264 High ~12.45 Mbps;
  - AAC LC stereo 48 kHz ~160.8 kbps;
  - visual orientation/colors/composition appear correct.
- Performance conclusion:
  - projectM draw and Cyber Shark are the two dominant composition costs;
  - projectM draw likely pays for per-frame software Canvas scaling + R/B ColorMatrix + vertical flip;
  - Cyber Shark is second-largest and should be optimized after projectM draw.
- Single next resume step:
  - implement a safe BGRA-capable offline readback path so compatible GPUs can feed ARGB_8888 without the per-frame R/B ColorMatrix, keeping the current RGBA + ColorMatrix fallback for unsupported devices; then measure one 3-second preview again.


---

## 2026-10-06 — v0.19.8 / build 97 — projectM BGRA fast path — Android PASS

- Status: BUILD PASS / phone QA measured.
- App/source commit: `83512d24b74962ea7a93757577149b0c0e6aa7f8`.
- Android workflow: #483, attempt 1.
- Workflow run id: `37405470037`.
- Artifact: `FARIC-Music-Visualizer-v0.19.8-Debug`.
- Artifact id: `11387256864`.
- Artifact digest: `sha256:8703188f1a9377ae9c1ac36a4117ee358f186312c5873f5e9ecc8ccc4f4897f3`.
- Validate #802 — PASS on the same source SHA.
- Successfully completed in this build:
  - uses GL_EXT_read_format_bgra when supported;
  - bypasses the projectM R/B ColorMatrix in Canvas when BGRA readback succeeds;
  - preserves RGBA + ColorMatrix fallback for unsupported/failed BGRA readback.
- Phone QA evidence:
  - screen recording `447934.mp4`;
  - exported preview `447935.mp4`.
- Exact timing:
  - projectM generation/readback: 5318 ms;
  - composition: 8077 ms;
  - encoder: 450 ms;
  - audio: 396 ms;
  - mux: 93 ms;
  - save: 124 ms;
  - total: 15118 ms.
- Result dialog: `projectM BGRA: yes`.
- Composition breakdown:
  - clear: 77 ms;
  - projectM draw: 1643 ms;
  - FARIC reactive: 0 ms;
  - overlay: 0 ms;
  - Big EQ: 0 ms;
  - Cyber Shark: 4676 ms;
  - effects: 428 ms;
  - HUD update: 25 ms;
  - HUD draw: 1222 ms.
- Comparison with v0.19.7:
  - projectM draw 6471 -> 1643 ms (~74.6% reduction);
  - composition 12998 -> 8077 ms (~37.9% reduction);
  - combined projectM generation + draw 8534 -> 6961 ms (~18.4% reduction);
  - total 17008 -> 15118 ms (~11.1% reduction).
- Output verification:
  - 1080x1920;
  - 30 FPS;
  - 90 frames;
  - video duration 3.000 s / audio-container duration 3.008 s;
  - H.264 High ~12.43 Mbps;
  - AAC LC stereo 48 kHz ~160.9 kbps;
  - colors/orientation/composition appear correct.
- Performance conclusion:
  - BGRA fast path is valid and materially reduces Canvas projectM draw cost;
  - projectM readback/generation became more expensive, partially offsetting the gain;
  - Cyber Shark (~4.7 s) is now the largest single composition target.
- Single next resume step:
  - optimize Cyber Shark background/compositing first, starting with avoiding full-screen saveLayerAlpha when effective background opacity is 1.0, then retest one 3-second preview.


---

## 2026-10-06 — v0.19.9 / build 98 — Cyber Shark opaque saveLayer optimization — Android PASS

- Status: BUILD PASS / phone QA measured.
- App/source commit: `5bd6945f43fb3f4348085e008ff490a20468c9ab`.
- Android workflow: #485, attempt 1.
- Workflow run id: `37406936003`.
- Artifact: `FARIC-Music-Visualizer-v0.19.9-Debug`.
- Artifact id: `11387855203`.
- Artifact digest: `sha256:2141bf82509426cd8a27d74b8a17cdc9cf15157ce6d465904aa020dbf1ab01c4`.
- Validate #807 — PASS on the same source SHA.
- Successfully completed in this build:
  - keeps projectM BGRA fast path active;
  - Cyber Shark background avoids full-screen `saveLayerAlpha` when effective alpha is fully opaque;
  - partial-opacity path remains unchanged.
- Phone QA evidence:
  - result screenshot `1791257969388.jpeg`.
- Exact timing:
  - projectM generation/readback: 5005 ms;
  - composition: 6860 ms;
  - projectM BGRA: yes;
  - encoder: 539 ms;
  - audio: 498 ms;
  - mux: 103 ms;
  - save: 240 ms;
  - total: 14031 ms.
- Composition layers:
  - clear: 58 ms;
  - projectM draw: 1640 ms;
  - FARIC reactive: 0 ms;
  - overlay: 0 ms;
  - Big EQ: 0 ms;
  - Cyber Shark: 3523 ms;
  - effects: 432 ms;
  - HUD update: 24 ms;
  - HUD draw: 1176 ms.
- Comparison with v0.19.8:
  - Cyber Shark 4676 -> 3523 ms (~24.7% reduction);
  - composition 8077 -> 6860 ms (~15.1% reduction);
  - total 15118 -> 14031 ms (~7.2% reduction);
  - projectM BGRA remains active and projectM draw is essentially unchanged (1643 -> 1640 ms).
- Performance conclusion:
  - Cyber Shark saveLayer optimization is effective and visually safe;
  - largest remaining single stage is now projectM generation/readback (~5.0 s), followed by Cyber Shark (~3.5 s), projectM draw (~1.64 s), HUD draw (~1.18 s).
- Single next resume step:
  - split projectM generation/readback timing into native render, glReadPixels, Bitmap.copyPixelsFromBuffer, and queue/wait overhead before changing the readback path again.


---

## 2026-10-06 — v0.19.10 / build 99 — projectM offline stage profiler — Android PASS

- Status: BUILD PASS / phone QA measured.
- App/source commit: `722534375a25f973e396e7b99139dd0263406e5a`.
- Android workflow: #486, attempt 1.
- Workflow run id: `37410199916`.
- Artifact: `FARIC-Music-Visualizer-v0.19.10-Debug`.
- Artifact id: `11389315330`.
- Artifact digest: `sha256:b97b503ec5e0defeb6f03b1e66c7978ea34839b3af1e07933954e7a0cbe25255`.
- Validate #811 — PASS on the same source SHA.
- Successfully completed in this build:
  - preserves BGRA projectM fast path;
  - profiles projectM offline generation into queue wait, native render, glReadPixels, and Bitmap copy.
- Phone QA exact timing:
  - projectM total: 5270 ms;
  - queue wait: 16 ms;
  - native render: 730 ms;
  - glReadPixels: 4361 ms;
  - bitmap copy: 97 ms;
  - composition: 7781 ms;
  - encoder: 528 ms;
  - audio: 522 ms;
  - mux: 99 ms;
  - save: 204 ms;
  - total: 15287 ms.
- Composition layers:
  - clear: 80 ms;
  - projectM draw: 1651 ms;
  - FARIC reactive: 0 ms;
  - overlay: 0 ms;
  - Big EQ: 0 ms;
  - Cyber Shark: 4314 ms;
  - effects: 448 ms;
  - HUD update: 28 ms;
  - HUD draw: 1255 ms.
- Performance conclusion:
  - glReadPixels is the dominant projectM bottleneck: ~82.8% of projectM provider time and ~28.5% of total export wall-clock;
  - queue wait, native render, and Bitmap copy are comparatively small;
  - next projectM optimization should target GPU->CPU readback itself, not native projectM rendering.
- Phone UX finding:
  - timing dialog should have a `Копіювати текст` action so results can be pasted directly into chat without screenshots.
- Single next resume step:
  - ship the copy-to-clipboard timing-dialog action as a separate build, then pursue glReadPixels optimization independently.


---

## 2026-10-06 — v0.19.11 / build 100 — copy export timing text — Android PASS

- Status: BUILD PASS / phone QA for the new clipboard button still pending.
- App/source commit: `5b9c00b45993a01fe0be0922de8714db1c758e97`.
- Android workflow: #487, attempt 1.
- Workflow run id: `37485263497`.
- Artifact: `FARIC-Music-Visualizer-v0.19.11-Debug`.
- Artifact id: `11423002180`.
- Artifact digest: `sha256:1b4ccb6c9619073f8629082819cc521abe8a98f766f87df2e3f853a8cf9bc897`.
- Validate #813 — PASS on the same source SHA.
- Successfully completed in this build:
  - export timing result dialog now has a persistent `Копіювати текст` button;
  - the action copies the full timing block to Android clipboard;
  - copying does not dismiss the dialog;
  - `OK` remains the explicit close action;
  - no intentional render/performance behavior changes versus v0.19.10.
- Last accepted performance diagnosis from v0.19.10:
  - projectM total 5270 ms;
  - queue wait 16 ms;
  - native render 730 ms;
  - glReadPixels 4361 ms;
  - bitmap copy 97 ms;
  - total export 15287 ms;
  - glReadPixels is the dominant projectM bottleneck.
- Unverified phone QA:
  - install v0.19.11;
  - run one 3-second preview;
  - tap `Копіювати текст`;
  - paste the copied block into chat and confirm the dialog stays open.
- Single next resume step:
  - phone-accept the copy-to-clipboard UX, then continue performance work specifically on GPU->CPU projectM readback / `glReadPixels`.

---

## 2026-10-06 — v0.19.12 / build 101 — projectM readback/composition overlap — Android PASS

- Status: BUILD PASS / phone performance QA pending.
- App/source commit: `86750315d89c6ebb3b332e912ddc92a1759b3cbb`.
- Android workflow: #488, attempt 1.
- Workflow run id: `37490177560`.
- Artifact: `FARIC-Music-Visualizer-v0.19.12-Debug`.
- Artifact id: `11425346915`.
- Artifact digest: `sha256:9e347528bef24ab8e5a9f706542ef7e5548222ba8ebe187703d70f3f58c57d03`.
- Validate #816 — PASS on the same app/source SHA.
- Successfully completed in this build:
  - preserves the existing deterministic GLES2 projectM offline renderer and synchronous `glReadPixels` pixel contract;
  - adds two reusable projectM readback ByteBuffer/Bitmap slots;
  - pipelines frames so projectM render/readback for frame N+1 can run on the GL thread while CPU Canvas composition/encoder submission processes frame N;
  - preserves frame-specific PCM, SceneSignal, projectM frame time, BGRA fast path, 1080x1920-class output and 30 FPS;
  - does not intentionally change visuals, preview/full-song timeline semantics, encoder format or audio mux.
- Baseline phone timing from v0.19.11 before this optimization:
  - 1080x1920, 90 frames;
  - projectM provider wall time: 5295 ms;
  - projectM internals: queue 11 ms, native render 714 ms, glReadPixels 4421 ms, bitmap copy 87 ms;
  - composition: 7468 ms;
  - Cyber Shark: 4093 ms;
  - HUD draw: 1191 ms;
  - total: 14925 ms;
  - projectM BGRA: yes.
- Expected measurement behavior:
  - accumulated projectM internal `glReadPixels` time may remain near the previous ~4.4 s because the readback operation itself is not yet removed;
  - success is primarily measured by lower top-level projectM provider wait and lower total export wall-clock, because readback should overlap CPU composition;
  - output orientation, colors, frame order and A/V sync must remain unchanged.
- Not yet proven on phone:
  - real overlap/speedup on Galaxy A54;
  - no frame corruption or slot overwrite;
  - no frame-order/pacing regression;
  - same visual orientation/colors/audio and deterministic preview behavior.
- Single next resume step:
  1. install v0.19.12 via Termux `3 -> 10 -> 8`;
  2. run the same warm-cache 3-second / 90-frame preview;
  3. paste the copied timing block;
  4. compare top-level `projectM:`, `composition:`, `total:` and verify the picture/audio are correct.

---

## 2026-10-06 — v0.19.12 phone performance PASS

- Candidate: v0.19.12 / build 101.
- App/source SHA: `86750315d89c6ebb3b332e912ddc92a1759b3cbb`.
- Phone benchmark: 1080x1920, 90 frames.
- Exact timing:
  - projectM provider wait: 107 ms;
  - projectM internals: queue 17 ms, native render 756 ms, glReadPixels 4420 ms, bitmap copy 100 ms;
  - composition: 7829 ms;
  - encoder: 449 ms;
  - audio: 456 ms;
  - mux: 105 ms;
  - save: 196 ms;
  - total: 9915 ms;
  - projectM BGRA: yes.
- Composition layers:
  - clear 104 ms;
  - projectM draw 1729 ms;
  - FARIC reactive 0 ms;
  - overlay 0 ms;
  - Big EQ 0 ms;
  - Cyber Shark 4382 ms;
  - effects 418 ms;
  - HUD update 25 ms;
  - HUD draw 1167 ms.
- Comparison with the v0.19.11 phone baseline:
  - projectM provider wait 5295 -> 107 ms (-97.98%);
  - total 14925 -> 9915 ms (-5010 ms, -33.57%);
  - wall-clock speedup ~1.51x;
  - 3-second export factor improved from ~4.98x realtime to ~3.31x realtime;
  - glReadPixels stayed essentially unchanged: 4421 -> 4420 ms, confirming the gain comes from overlap rather than a different readback cost.
- Performance conclusion:
  - two-slot readback/composition pipelining works on the Galaxy A54 exactly as intended;
  - projectM synchronous readback is almost fully hidden behind CPU composition at provider level;
  - composition is now the dominant wall-clock bottleneck;
  - Cyber Shark is the largest measured composition layer at 4382 ms, followed by projectM draw 1729 ms and HUD draw 1167 ms.
- Evidence boundary:
  - performance benchmark is PASS;
  - user did not explicitly report visual orientation/colors/frame-order/audio status in the same message, so full visual/audio phone acceptance remains unclaimed.
- Single next engineering step:
  - instrument Cyber Shark export internally (background / frame / FX / creature / wordmark) before changing rendering behavior, then optimize the measured dominant sublayer.

---

## 2026-10-06 — v0.19.13 / build 102 — Cyber Shark sublayer profiler — Android PASS

- Status: BUILD PASS / phone diagnostic QA pending.
- App/source commit: `8f374445a0dc566a924c5a275f42c9914664eba9`.
- Android workflow: #489, attempt 1.
- Workflow run id: `37501072748`.
- Artifact: `FARIC-Music-Visualizer-v0.19.13-Debug`.
- Artifact id: `11429668183`.
- Artifact digest: `sha256:47d279c73ee4ab920c784a4f6a6491d823c4637a42834b32ef4864dfe30d46d4`.
- Validate #820 — PASS on the same app/source SHA.
- Purpose:
  - preserve the successful v0.19.12 two-slot projectM overlap pipeline;
  - add timing only, with no intended visual/render behavior change;
  - split Cyber Shark export time into background, frame, FX, creature and wordmark.
- Phone baseline immediately before this build:
  - v0.19.12 total 9915 ms;
  - projectM provider wait 107 ms;
  - composition 7829 ms;
  - Cyber Shark 4382 ms;
  - projectM draw 1729 ms;
  - HUD draw 1167 ms.
- Success criterion:
  - same output behavior and roughly comparable total/composition timing;
  - result text contains `Cyber Shark internals` with five sublayer timings;
  - choose the next optimization from the largest measured sublayer rather than guessing.
- Single next resume step:
  - install v0.19.13 via Termux 3 -> 10 -> 8, run the same warm-cache 3-second / 90-frame preview, copy the timing text and paste it into chat.

---

## 2026-10-06 — v0.19.13 phone diagnostic profile

- App/source SHA: `8f374445a0dc566a924c5a275f42c9914664eba9`.
- Phone: 1080x1920 / 90 frames.
- projectM: 105 ms; composition: 7971 ms; total: 10069 ms; BGRA: yes.
- projectM internals: queue 12 ms; native 733 ms; glReadPixels 4458 ms; bitmap copy 96 ms.
- Cyber Shark: 4487 ms.
- Cyber Shark internals: background 1858 ms; frame 1475 ms; FX 273 ms; creature 445 ms; wordmark 423 ms.
- Performance conclusion: background is the largest measured Cyber Shark sublayer; frame is second.
- Next: profile background internals before modifying its visuals or quality.

---

## 2026-10-06 — v0.19.14 / build 103 — Cyber Shark background profiler — Android PASS

- Status: BUILD PASS / phone diagnostic QA pending.
- App/source commit: `5a7ca6a16b3387e17704d8ab9f9aba5499ec3a42`.
- Android workflow: #490, attempt 1.
- Workflow run id: `37510375626`.
- Artifact: `FARIC-Music-Visualizer-v0.19.14-Debug`.
- Artifact id: `11435801494`.
- Artifact digest: `sha256:fd17fd1949b01a0d0ff0eedb08072322c3ca2389af710985857a839434eeab3b`.
- Validate #822 — PASS on the same app/source SHA.
- Purpose:
  - preserve the v0.19.12 overlap pipeline and v0.19.13 Cyber Shark profiler;
  - add timing only inside Cyber Shark background;
  - split background into setup/save, radial glow, arcs, particles and restore.
- Last phone baseline from v0.19.13:
  - total 10069 ms;
  - composition 7971 ms;
  - Cyber Shark 4487 ms;
  - background 1858 ms;
  - frame 1475 ms;
  - FX 273 ms;
  - creature 445 ms;
  - wordmark 423 ms.
- No intended visual/render quality change in this build.
- Single next resume step:
  - install v0.19.14 via Termux 3 -> 10 -> 8, run the same warm-cache 3-second / 90-frame preview, copy the full timing text including `background internals`, and paste it into chat.

