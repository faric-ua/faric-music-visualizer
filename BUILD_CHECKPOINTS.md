# FARIC Music Visualizer — Build Checkpoints

## v0.19.49 / build 138 — grouped Hero gallery + ZIP import (PR CI PASS / PHONE QA PENDING)

- Pre-merge app/source: `dcd41b2ae2a477f9c6181ebb5edcdf62f4c13933`, PR #5.
- Validate #1092 PASS; Android #615 PASS, workflow run `37825625591`.
- Android artifact: `FARIC-Music-Visualizer-v0.19.49-Debug`, ID `11570559985`, expires 2026-10-11. PR artifact is a CI proof; after merge, verify the signed **main** build and record its exact SHA before asking for Termux install.
- Code: one expandable «Герої» family; compact 2-column image tiles for Shark/Panther + 10 imported user heroes; other themes remain independent. Safe SAF selection of Pictures2.zip, exact entry-name identity mapping, source PNG storage, thumbnail decoding off UI thread; live Scene Host preserved. No changes to immutable center calibration.
- **Artwork acceptance incomplete:** originals are composite design sheets. The imported preview is currently a flattened top-left emblem, not separated approved Frame/FX/Creature/Wordmark. This is a phone-visual-QA gate; no production art-quality PASS is claimed.
- **PHONE QA PENDING:** gallery expand/collapse, ZIP import 10/10, card previews and selection, smooth return to Player, Shark/Panther regressions, projectM bars, 3-second MP4.
- PERF-NAV-002 remains open pending phone QA; unrelated BUG-EXPORT-001 remains open.
- Single resume step: merge PR #5 after docs handoff, confirm signed main Android PASS, record final checkpoint, then Termux **3 → 10 → 8**.


## v0.19.48 / build 137 — Canonical Live Themes + projectM system bars (CI PASS / PHONE QA PENDING)

- App/source: `edd69d6cbf5c3abb4bc7859f5c54a5f9cdd7a3ff` (PR #4 merged).
- Validate #1089 PASS; Android #613 PASS, workflow run `37813832199`, artifact `FARIC-Music-Visualizer-v0.19.48-Debug`, ID `11566826167`, expires 2026-10-11.
- Immutable checkpoint branch: `checkpoint/pulsedeck-v0.19.48-2026-10-08`.
- Code/CI: live central theme switching across Shark, Panther, Neon, Energy, Orbital, Star, Wave, Vinyl, Cassette and Visualizer without full Scene Host teardown. Separate HeroThemeView mounted in GF layer. projectM authoring explicitly shows Android status/navigation bars, restores immersive player on exit. Wave Idol catalog deduplicated. Registry uniqueness unit test PASS.
- Phone QA NOT YET DONE: check bars on projectM and after Back, all theme transitions without black/freezes, Board, projectM preset/ratings and short MP4. PERF-NAV-002 remains open, full-song BUG-EXPORT-001 independent.
- Termux next: **3 → 10 → 8**; do not start build 9.


## v0.19.47 / build 136 — original projectM UI + live hero switching (CI PASS; PHONE QA PENDING)

- Code in main: `f5ce3e90a33122ee59c3599cc0830b29f08c8093`; PR #3 merged.
- Validate #1086 PASS; Android #611 PASS, run `37810162017`, artifact `FARIC-Music-Visualizer-v0.19.47-Debug`, id `11565016839`, expires 2026-10-11.
- Immutable checkpoint branch: `checkpoint/pulsedeck-v0.19.47-2026-10-08`.
- v0.19.46 phone findings: Board, projectM and Themes browsing smooth; projectM legacy menu was replaced and Shark ↔ Panther still jerked. v0.19.47 restores original projectM 4-row controls, status and ratings within persistent scene; loads GF assets off UI and swaps only HeroBoardView in-place. `FARIC-nav` reports assetDecode and UIApplyAndReturn milliseconds.
- **Phone QA PENDING.** Next: Termux **3 → 10 → 8** (do not press 9), install build 136. Test original projectM menu, Back/ratings/AUTO/FG; Shark ↔ Panther ×4 without visible stall or GL recreate; Board and Themes Back ×3; short MP4.
- PERF-NAV-002 remains PARTIAL until theme switch phone PASS; BUG-EXPORT-001 remains OPEN for full-song export.


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

## v0.19.46 / build 135 — persistent Scene Host: CI PASS / PHONE QA PENDING

- Exact app/source SHA: `6ce0a33a893edc78b1126e98ff5cf39181f9daaa`; PR #2 source commit `a501718966e505a3efd4c5221e74e0c8441e6d4e`.
- Validate #1081 PASS; Android #607 PASS, run `37804612518`.
- Artifact: `FARIC-Music-Visualizer-v0.19.46-Debug`, id `11562008086`, expires 2026-10-11 (UTC). Immutable checkpoint: `checkpoint/pulsedeck-v0.19.46-2026-10-08`.
- **Code/CI accomplished:** normal installed projectM authoring uses in-player overlay, Board/editor and Theme browser open over retained live render stack; Android compiled, APK signed. **No PHONE QA yet; performance fix not accepted.**
- Next resume: Termux **3 → 10 → 8**, install build 135, phone QA navigation 3x both Back paths, Board, Theme browse/cancel, FG/AUTO/prefs, 3s MP4 and confirm no extra `FARIC-projectM` native create on navigation.

---

## v0.19.45 / build 134 — CI PASS / PHONE QA NOT ACCEPTED (2026-10-08)

- App source: `0ca786f236ab988b8186fc550af3b9877416c91c`.
- Validate #1079 PASS. Android #605 PASS, run `37798305659`, artifact `FARIC-Music-Visualizer-v0.19.45-Debug` id `11560057074`.
- Code-level GL exit guard and explicit view pause compiled and built successfully. No claim of smooth projectM navigation on the phone.
- Latest user's 5699ms short export lacks an explicit build attribution; do not assume it proves phone navigation performance for v0.19.45.
- Next resume: persistent Scene Host v0.19.46 branch with PR CI and targeted phone testing.

---

## 2026-10-08 — v0.19.44 / build 133 — partial GL navigation fix — Android PASS

- Status: **Validate PASS / Android PASS / Phone QA PENDING**.
- Exact source SHA `cf3a517695a591df933137b767956358a1458a59`; Validate #1075 PASS (run 37784827455); Android #604 PASS (run 37784827579).
- Artifact `FARIC-Music-Visualizer-v0.19.44-Debug`, id `11554186369`, SHA256 ZIP `sha256:7c12ef288310e9f2cc386184f2773ae45f54b0cfdaed995a9e3b30c5e0bdb733`, expires 2026-10-11.
- Fix candidate: Board/Player async GL destroy callback before rebuild; separate projectM return restores visualizer slot alone; tagged release/create timing. No extra feature or changed controls.
- Previous v0.19.43: 3/3 export-report PASS; 2/2 nav FAIL. Build 133 nav improvements **must still pass PHONE QA**. Generic other-screen blocking and native-init pauses may persist.
- Recovery branch: `checkpoint/pulsedeck-v0.19.44-2026-10-08`; detail: `docs/checkpoints/2026-10-08-v0.19.44-nav-handoff.md`.
- Next: Termux **3 → 10 → 8**; phone test 3× Board↔Player, 3× projectM↔Player with app Back and system Back; confirm no missing background; short export optional. **Do not repeat the already accepted Copy/OK test unnecessarily.**

## 2026-10-08 — v0.19.43 / build 132 — Copy keeps export report open — Android PASS

- Status: Validate PASS / Android PASS / **PHONE QA 3 PASS / 2 FAIL** (export report lifecycle PASS; two transitions FAIL).
- Source SHA `364f39627f7bb41c7315021b91f484b72ebdc63c`; Validate #1037 PASS (run `37720314968`); Android #603 PASS (run `37720314972`).
- Artifact `FARIC-Music-Visualizer-v0.19.43-Debug` id `11526295056`, ZIP SHA256 `sha256:7088bf4da7c7128f0fe2466b579a42d5462a0c919c81e91ab73e3a9305e1a8f9`. Expires 2026-10-11.
- UI change: Copy performance report does not close dialog; OK still closes. No export engine changes.
- User-provided earlier 3s report: 1080×1920, 90 frames, total 5976ms, GPU direct; copy worked but auto-dismissed on prior build. **This is not v0.19.43 phone acceptance.**
- V0.19.42 Frame 5/6 tests previously PASS; navigation untested in that set; earlier navigation pauses remain PERF-NAV-002 OPEN. Full song exit ~48% BUG-EXPORT-001 OPEN.
- Immutable source branch `checkpoint/pulsedeck-v0.19.43-2026-10-08`; detail `docs/checkpoints/2026-10-08-v0.19.43-export-copy-lifecycle.md`.
- Resume: **Do NOT repeat export Copy/OK QA; 3/3 PASS**. Next engineering target PERF-NAV-002, after new APK repeat projectM and Board transitions.

### Phone QA v0.19.43: 3 PASS / 2 FAIL

- PASS: 3s export report with fixed Copy/OK; Copy retains dialog; OK closes only report.
- FAIL: Player → separate projectM → Back freezes/pauses; Player → Board → Back freezes/reinitializes visualizer.
- PERF-NAV-002 OPEN. No new build was made by this phone result.

## 2026-10-08 — v0.19.42 / build 131 — Frame auto spin, BG/Glow single line — Android PASS

- Status: BUILD PASS / **5/6 PHONE QA PASS**, navigation NOT TESTED on build 131.
- Source SHA `d3bfbeb20fcc3168dc52db038d76f06841d3bc71`. Android #602 PASS (run `37718078044`); Validate #1033 PASS (run `37718078010`).
- Signed APK artifact `FARIC-Music-Visualizer-v0.19.42-Debug`, id `11525215496`, SHA256 ZIP `63afaed32576c43ecdb4a06ca1bd91b64f1153b7b0c41ebd1461f70b86935b29`.
- Features: Frame-only ±180°/s optional auto-spin 0=off, manual rotation independent, live and CPU/GPU export angle in shared helper, per-layer store and Composition Sets, widened single-line BG/Glow selector.
- Tests: BoardLayerTransform signed spin / clamp unit tests PASS; APK signer and upload PASS. **Phone proof 5/6 PASS**: Frame signed rotation and stop, persistence/Composition Set, BG/Glow one line, 3s MP4 and visible Copy/OK. The remaining navigation check is NOT TESTED and prior lag remains OPEN. User-export report total 5976ms (1080×1920, 90 frames), projectM 791ms, composition 1935ms, GPU direct, overlay 664ms, HUD draw 1051ms. Copy closes dialog; user accepts.
- Phone findings carried forward: physical projectM Back icon appeared, but UI transitions still freeze; PERF-NAV-002 OPEN. v0.19.41 export Copy/OK scrolling still awaits phone acceptance.
- Other open issues: BUG-EXPORT-001 full-song app exit at ~48%; irregular frame pacing PERF-PLAYER-001.
- Recovery branch: `checkpoint/pulsedeck-v0.19.42-2026-10-08`; detailed recovery: `docs/checkpoints/2026-10-08-v0.19.42-frame-spin.md`.
- Next action: phone 5/6 PASS logged. No rebuild needed; tackle open PERF-NAV-002 transitions, preserving accepted Frame behavior.

## 2026-10-08 — v0.19.41 / build 130 — export report fixed footer — Android PASS

- Status: BUILD PASS / phone QA pending.
- App/source SHA: `08c78cd419e658841495d92b96b12f19b6f35e14`.
- Validate #1030 PASS, run `37716816321`.
- Android #600 PASS, run `37716816279`.
- Artifact: `FARIC-Music-Visualizer-v0.19.41-Debug`, id `11523783977`, ZIP digest `sha256:e59b295363e7eceb4e73f2f115a478b57c2583e4e6c1c537f476604d1e148010`.
- Contents: bounded-height long export timing report with scrollable only message, always-visible title / Copy / OK buttons; other dialogs unaffected.
- Regression evidence from **older v0.19.39**: 5/6 phone checks PASS (numeric individual reset, signed Center auto-spin, persistence, main player + 3s export); Back test NOT TESTED for v0.19.39; prior v0.19.38 transition video reported FAIL.
- This release's phone QA not yet done; v0.19.40 nested Layers / visual Back / transition tests also still pending.
- Full-song 48% crash BUG-EXPORT-001 remains open/unfixed.
- Frozen recovery branch: `checkpoint/pulsedeck-v0.19.41-2026-10-08`.
- Resume: `docs/checkpoints/2026-10-08-v0.19.41-export-report.md`; Termux 3 → 10 → 8; phone testing above.

## 2026-10-08 — v0.19.40 / build 129 — nested Layers and projectM Back — Android PASS

- Status: BUILD PASS / phone QA PENDING.
- Source commit: `1cc7c889b7b79657f1d3958bf86d7e7ce09a2014`.
- Validate #1028: PASS, run `37716105314`.
- Android #599: PASS, run `37716105287`.
- Artifact: `FARIC-Music-Visualizer-v0.19.40-Debug`, id `11524315208`, digest `sha256:a6f421292678f2ba1669b1d38bd250728ab7b5ec71d393d43f72d4d7c470a8eb`.
- Verified: Unit tests, native Android compile, debug APK signing/verification, artifact upload.
- Changes: preserve Layers parent under projectM ⚙, fix Back icon asset path, async player→projectM GL release, exit feedback.
- Phone proof still required for visual Back button, modal parent state, repeated navigation both ways and frame pacing. This build does NOT assert all lag fixed.
- Preserves v0.19.39 per-parameter reset and signed Center auto-spin.
- Open independent defects: BUG-EXPORT-001 app exit at ~48% of full-song export; PERF-PLAYER-001 irregular visual motion.
- Recovery branch: `checkpoint/pulsedeck-v0.19.40-2026-10-08`.
- Resume: `docs/checkpoints/2026-10-08-v0.19.40-layers-navigation.md`, then Termux **3 → 10 → 8**, phone QA.

## 2026-10-08 — v0.19.38 / build 127 — movable projectM authoring — Android PASS

- Status: BUILD PASS / phone QA partially accepted (5/6 PASS, Back pending).
- App/source commit: `ca295375dfcba0765dd1761978123cfe4eb5ba4d`.
- Validate workflow: #994 — PASS.
- Android workflow: #591 — PASS.
- Workflow run id: `37706775447`.
- Artifact: `FARIC-Music-Visualizer-v0.19.38-Debug`.
- Artifact id: `11519777909`.
- Artifact digest: `sha256:3dd2964c231e0c5e373c4b08bcdaafef16ff1272232f73b922827f58f940cba3`.
- Artifact size: 11,319,954 bytes.
- Successfully completed in this build:
  - projectM authoring settings moved to one PulseDeck-styled movable/non-dimming panel;
  - tabs: AUTO / CENTER / EDGE FX;
  - panel X/Y position persists and can be reset;
  - main Layers -> Visualizer has a projectM/FG settings gear opening the same tuning panel over the composite player;
  - AUTO UX is explicit: AUTO OFF in manual mode, AUTO 5s/10s/15s when active; tapping while manual enables AUTO;
  - Center/Edge tuning remains live and persisted;
  - Back uses non-blocking queued GL release callback instead of blocking the UI thread while preserving renderer ownership ordering;
  - v0.19.37 tuning/export/Composition Set persistence remains included.
- Phone QA evidence (user report 2026-10-08): **5/6 PASS, 1 NOT TESTED**.
  - PASS: ⚙ FG draggable panel.
  - PASS: Center scale changes immediately visible without dismissing panel.
  - PASS: panel X/Y position retained after close/reopen.
  - PASS: AUTO 5s visibly switches preview presets.
  - PASS: main player Layers -> Visualizer -> ⚙ enables live tuning over composite.
  - NOT TESTED: on-screen Back and Android system Back; return must avoid UI freeze and projectM renderer dropout.
  - 10s/15s timing was not separately reported as tested; AUTO timed Export remains not implemented.
- Explicit open TODO:
  - deterministic Export sequencing for AUTO 5/10/15 is not implemented yet; current interval controls projectM authoring/browser preview.
  - Panther remains a separate unfinished Shark-grade art task.
- Recovery checkpoint:
  - `docs/checkpoints/2026-10-08-v0.19.38-projectm-authoring.md`.
- Resume exactly here:
  1. On already installed build 127, test on-screen projectM Back and Android system Back, ideally several open/return cycles.
  2. Verify player background remains visible without a frozen pause or corruption.
  3. Record PASS/FAIL; only after Back PASS close v0.19.38 phone QA, then choose deterministic AUTO export sequencing or Panther art.


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

---

## 2026-10-06 — v0.19.14 phone background profile

- App/source SHA: `5a7ca6a16b3387e17704d8ab9f9aba5499ec3a42`.
- Phone: 1080x1920 / 90 frames.
- projectM 105 ms; composition 7591 ms; total 9510 ms; BGRA yes.
- projectM internals: queue 13 ms; native render 738 ms; glReadPixels 4412 ms; bitmap copy 96 ms.
- Cyber Shark 4160 ms.
- Cyber Shark internals:
  - background 1848 ms;
  - setup/save 1 ms;
  - glow 1755 ms;
  - arcs 61 ms;
  - particles 28 ms;
  - restore 0 ms;
  - frame 1145 ms;
  - FX 239 ms;
  - creature 492 ms;
  - wordmark 423 ms.
- Diagnosis: radial glow alone is ~95% of Cyber Shark background and ~42% of the entire Cyber Shark layer.
- Next: replace full-resolution software radial-gradient rasterization with a reusable 512x512 glow texture while keeping final 1080x1920 output and 30 FPS unchanged.

---

## 2026-10-06 — v0.19.15 / build 104 — cached Cyber Shark glow — Android PASS

- Status: BUILD PASS / phone performance + visual QA pending.
- App/source commit: `9fbb48d5023a2e12de59356ed148fa04a3b34258`.
- Android workflow: #491, attempt 1.
- Workflow run id: `37512222805`.
- Artifact: `FARIC-Music-Visualizer-v0.19.15-Debug`.
- Artifact id: `11434899037`.
- Artifact digest: `sha256:438f9205045f24767fec592cdf10d2824bcdfae66fb6abdd25bacdfab7e3d1ea`.
- Validate #824 — PASS on the same app/source SHA.
- Implemented:
  - preserves the v0.19.12 two-slot projectM overlap pipeline;
  - preserves final 1080x1920-class / 30 FPS output and existing encoder/audio path;
  - replaces direct full-size software Cyber Shark RadialGradient rasterization with a reusable 512x512 ARGB glow bitmap;
  - glow keeps the same dynamic bass/high/beat alpha response and normalized radius, then uses filtered final-size composition;
  - arcs, particles, frame, FX, creature, wordmark, projectM and HUD are unchanged;
  - adds `glow render` and `glow composite` timings.
- Phone baseline from v0.19.14:
  - total 9510 ms;
  - composition 7591 ms;
  - Cyber Shark 4160 ms;
  - background 1848 ms;
  - glow 1755 ms;
  - arcs 61 ms;
  - particles 28 ms;
  - frame 1145 ms.
- Phone acceptance:
  - compare glow/background/Cyber Shark/composition/total timings;
  - visually check glow smoothness, brightness, radius/shape and animation response;
  - reject this optimization if visual degradation is materially visible.
- Single next resume step:
  - install v0.19.15 via Termux 3 -> 10 -> 8, run the same warm-cache 3-second / 90-frame preview, paste the timing block, and report whether the glow looks the same/smooth.

---

## 2026-10-06 — v0.19.15 phone cached-glow profile

- App/source SHA: `9fbb48d5023a2e12de59356ed148fa04a3b34258`.
- Phone: 1080x1920 / 90 frames.
- projectM 107 ms; composition 7541 ms; total 9454 ms; BGRA yes.
- projectM internals: queue 16 ms; native render 737 ms; glReadPixels 4502 ms; bitmap copy 101 ms.
- Cyber Shark 4113 ms.
- Cyber Shark background 1749 ms.
- Glow 1663 ms = render 265 ms + composite 1397 ms.
- Frame 1166 ms; FX 246 ms; creature 498 ms; wordmark 439 ms.
- Comparison with v0.19.14:
  - total 9510 -> 9454 ms (~0.6% faster);
  - glow 1755 -> 1663 ms (~5.2% faster);
  - rasterization became cheap, but filtered software bitmap composite alone costs 1397 ms.
- Conclusion: the 512x512 cache is not enough while FILTER_BITMAP_FLAG scaling remains active. The dominant glow cost is now destination composite/filtering, not gradient generation.
- Visual acceptance of the 512x512 cached glow was not explicitly reported by the user in the timing message.
- Next: keep the same 512x512 cache and disable bitmap filtering for a controlled performance/visual experiment.

---

## 2026-10-06 — v0.19.16 / build 105 — unfiltered cached glow — Android PASS

- Status: BUILD PASS / phone performance + visual QA pending.
- App/source commit: `ae32d61ae81b178e8437adf9fdc85e037c6df189`.
- Android workflow: #492, attempt 1.
- Workflow run id: `37514357810`.
- Artifact: `FARIC-Music-Visualizer-v0.19.16-Debug`.
- Artifact id: `11436347682`.
- Artifact digest: `sha256:4f33e9699f20ea41519a5bb2429b469152198551762647f666a4f38b37a4324c`.
- Validate #826 — PASS on the same app/source SHA.
- Implemented:
  - keeps the v0.19.15 reusable 512x512 dynamic Cyber Shark glow cache;
  - removes `FILTER_BITMAP_FLAG` only from the cached glow composite;
  - preserves final 1080x1920-class / 30 FPS output and all other projectM/Cyber Shark/HUD/encoder/audio behavior.
- Baseline from v0.19.15:
  - total 9454 ms;
  - composition 7541 ms;
  - Cyber Shark 4113 ms;
  - background 1749 ms;
  - glow 1663 ms;
  - glow render 265 ms;
  - glow composite 1397 ms.
- Acceptance rule:
  - performance: glow composite / glow / Cyber Shark / composition / total should materially decrease;
  - visual: reject if the enlarged 512x512 glow shows obvious blockiness, rings, stair-stepping or other visible quality loss.
- Single next resume step:
  - install v0.19.16 via Termux 3 -> 10 -> 8, run the same warm-cache 3-second / 90-frame preview, paste the timing block, and report whether the glow still looks smooth.

---

## 2026-10-06 — v0.19.16 phone unfiltered-glow result — PERFORMANCE NO-OP

- App/source SHA: `ae32d61ae81b178e8437adf9fdc85e037c6df189`.
- Phone: 1080x1920 / 90 frames.
- projectM 104 ms; composition 7379 ms; total 9450 ms.
- Cyber Shark 3927 ms.
- Background 1754 ms.
- Glow 1662 ms = render 265 ms + composite 1396 ms.
- Frame 1042 ms; FX 214 ms; creature 463 ms; wordmark 439 ms.
- Compare v0.19.15:
  - total 9454 -> 9450 ms (effectively unchanged);
  - glow composite 1397 -> 1396 ms;
  - glow total 1663 -> 1662 ms.
- Conclusion: removing FILTER_BITMAP_FLAG does not reduce the target cost. The large software destination composite itself is the bottleneck.
- The lower Cyber Shark total came mainly from normal frame-layer run variance, not from the glow change.
- Visual QA for unfiltered scaling was not explicitly reported; the candidate is rejected on performance grounds regardless.
- Next: move only the radial glow between CPU base/overlay passes onto the existing encoder EGL/GLES2 surface.

---

## 2026-10-06 — v0.19.17 / build 106 — hybrid GPU Cyber Shark glow — Android PASS

- Status: BUILD PASS / phone performance + visual QA pending.
- App/source commit: `5b1c14122d7f887b6f29f0e7391c59b9e9a7b2e6`.
- Android workflow: #493, attempt 1.
- Workflow run id: `37519271112`.
- Artifact: `FARIC-Music-Visualizer-v0.19.17-Debug`.
- Artifact id: `11437784670`.
- Artifact digest: `sha256:de0c9e01f5a6d780e7691eef077ed7fb5484f598573355914f069046f513389b`.
- Validate #829 — PASS on the same app/source SHA.
- Successfully compiled/tested:
  - split CPU composition into a base pass and a transparent upper pass;
  - preserve all pre-Cyber-Shark layers below the glow and all later layers above it;
  - export Cyber Shark radial-glow geometry/alpha as a GPU spec;
  - draw the radial glow on the existing encoder EGL/GLES2 surface between base and upper-overlay textures;
  - use premultiplied-alpha blending for the Android Bitmap overlay;
  - keep a full-CPU correctness fallback if a GPU glow spec is unavailable;
  - add `GPU glow` and `GPU overlay` timing fields.
- Final export resolution/FPS, H.264/AAC behavior, projectM two-slot overlap pipeline and PulseDeck calibration geometry are unchanged.
- Phone baseline from v0.19.16:
  - total 9450 ms;
  - composition 7379 ms;
  - Cyber Shark 3927 ms;
  - background 1754 ms;
  - CPU glow 1662 ms = render 265 + composite 1396 ms.
- Phone acceptance:
  - performance: compare composition, encoder, GPU glow, GPU overlay and total;
  - visual: verify glow color, brightness, radius, placement, audio reaction and correct ordering below frame/FX/creature/wordmark/effects/HUD.
- Single next resume step:
  - install v0.19.17 via Termux 3 -> 10 -> 8, run the same warm-cache 3-second / 90-frame preview, paste the full timing block and report any visible glow/order difference.

---

## 2026-10-06 — v0.19.17 phone hybrid-GPU-glow result — PERFORMANCE PASS

- App/source SHA: `5b1c14122d7f887b6f29f0e7391c59b9e9a7b2e6`.
- Phone: 1080x1920 / 90 frames.
- projectM provider 90 ms; composition 6028 ms; encoder 606 ms; total 8263 ms.
- projectM internals: queue 13 ms; native 768 ms; glReadPixels 4814 ms; bitmap copy 98 ms.
- GPU glow 6 ms; GPU upper-overlay upload/draw 252 ms.
- Cyber Shark 2388 ms:
  - background 95 ms;
  - CPU glow 0 ms;
  - arcs 57 ms;
  - particles 30 ms;
  - frame 1170 ms;
  - FX 232 ms;
  - creature 449 ms;
  - wordmark 430 ms.
- Other composition: projectM draw 1708 ms; effects 424 ms; HUD draw 1243 ms.
- Compare v0.19.16:
  - total 9450 -> 8263 ms (-1187 ms / ~12.6%);
  - composition 7379 -> 6028 ms (-1351 ms / ~18.3%);
  - CPU glow 1662 -> 0 ms;
  - encoder 427 -> 606 ms (+179 ms), while the GPU glow itself costs only 6 ms.
- Compare v0.19.11 baseline: total 14925 -> 8263 ms (~44.6% faster in wall time).
- Performance status: PASS.
- Visual equivalence was not explicitly reported in the timing message; visual QA for v0.19.17 remains pending.
- Next measured bottleneck: software Canvas projectM draw = 1708 ms.

---

## 2026-10-06 — v0.19.18 / build 107 — direct GPU projectM base — Android PASS

- Status: BUILD PASS / phone performance + visual QA pending.
- App/source commit: `655b367118787ca07d0d2654c56b12723005c3c6`.
- Android workflow: #494, attempt 1.
- Workflow run id: `37523326093`.
- Artifact: `FARIC-Music-Visualizer-v0.19.18-Debug`.
- Artifact id: `11440804926`.
- Artifact digest: `sha256:d9e50aa1069b216f12a80f86f7e8043112ef210ba321760e7c16c0bbd5cbcacf`.
- Validate #831 — PASS on the same app/source SHA.
- Implemented:
  - keeps v0.19.17 hybrid GPU Cyber Shark glow unchanged;
  - when the tested composition has no lower FARIC/overlay/Big-EQ layers, BGRA readback is correct and the projectM frame exactly matches export dimensions, the raw offline projectM Bitmap is uploaded directly as the encoder GPU base;
  - raw GL framebuffer uses unflipped GL texture coordinates, avoiding the CPU Canvas vertical-flip/draw path;
  - all non-qualifying configurations keep the established CPU fallback;
  - adds `GPU projectM` timing.
- Phone baseline from v0.19.17:
  - total 8263 ms;
  - composition 6028 ms;
  - projectM draw 1708 ms;
  - encoder 606 ms;
  - GPU glow 6 ms;
  - GPU overlay 252 ms.
- Phone acceptance:
  - projectM draw should become 0 ms in the tested qualifying scene;
  - `GPU projectM` should appear;
  - compare composition / encoder / total;
  - visually verify projectM orientation, colors and framing;
  - also verify the v0.19.17 GPU glow remains visually equivalent.
- Single next resume step:
  - install v0.19.18 via Termux 3 -> 10 -> 8, run the same warm-cache 3-second / 90-frame preview, paste the full timing block and report any visual orientation/color/glow issue.

---

## 2026-10-06 — v0.19.18 phone direct-GPU-projectM result — GUARD DID NOT ACTIVATE

- App/source SHA: `655b367118787ca07d0d2654c56b12723005c3c6`.
- Phone: 1080x1920 / 90 frames.
- projectM 91 ms; composition 5999 ms; encoder 623 ms; total 8275 ms.
- GPU glow 6 ms; GPU overlay 256 ms.
- projectM draw remained 1710 ms and no `GPU projectM` line appeared.
- Therefore the v0.19.18 direct-GPU-projectM branch did not activate.
- Root cause confirmed in code:
  - export ProjectMView uses `BALANCED_BACKGROUND`;
  - `BALANCED_BACKGROUND.renderScale = 0.78f`;
  - the 1080x1920 base export geometry therefore produces an offline framebuffer of about 842x1498 after `roundToInt()`;
  - v0.19.18 incorrectly required projectM Bitmap dimensions to equal 1080x1920 exactly.
- Performance is effectively unchanged from v0.19.17 (8263 -> 8275 ms).
- Next: allocate a dedicated projectM GPU texture at the actual framebuffer size and scale/center-crop it on GPU to match the established Canvas behavior.

---

## 2026-10-06 — v0.19.19 / build 108 — GPU-scale real projectM framebuffer — Android PASS

- Status: BUILD PASS / phone performance + visual QA pending.
- App/source commit: `adf26b803daf283cda51faaed9ebe51cf855e7fd`.
- Android workflow: #495, attempt 1.
- Workflow run id: `37529043474`.
- Artifact: `FARIC-Music-Visualizer-v0.19.19-Debug`.
- Artifact id: `11443104041`.
- Artifact digest: `sha256:ce0b8115dcc431d96d5903947bb55ba0f792f312afd5d7dfcd90a4b14032cbfb`.
- Validate #833 — PASS on the same app/source SHA.
- Implemented:
  - keeps v0.19.17 GPU Cyber Shark glow path;
  - removes v0.19.18 exact projectM-frame-size guard;
  - uses a dedicated EGL texture sized to the actual offline projectM framebuffer;
  - first qualifying frame allocates/uploads that texture, later frames reuse it via `texSubImage2D`;
  - GPU texture coordinates reproduce established Canvas scale-to-fill + center-crop behavior;
  - raw GL vertical orientation remains handled in texture coordinates;
  - BGRA correctness and lower-layer ordering guards remain;
  - CPU fallback remains for non-qualifying scenes.
- Root cause fixed:
  - `BALANCED_BACKGROUND.renderScale = 0.78f`;
  - 1080x1920 export therefore produces about 842x1498 offline projectM, so v0.19.18's exact-size guard could never pass.
- Phone baseline from v0.19.18:
  - total 8275 ms;
  - composition 5999 ms;
  - projectM draw 1710 ms;
  - encoder 623 ms;
  - GPU glow 6 ms;
  - GPU overlay 256 ms.
- Acceptance:
  - qualifying scene should show `projectM draw: 0 ms`;
  - `GPU projectM` must appear;
  - compare composition / encoder / total;
  - visually verify projectM orientation, colors and center-crop/framing;
  - re-check GPU glow visual equivalence.
- Single next resume step:
  - install v0.19.19 via Termux 3 -> 10 -> 8, run the same warm-cache 3-second / 90-frame preview, paste full timing and report any projectM/glow visual difference.

---

## 2026-10-06 — v0.19.19 phone GPU-projectM result — PERFORMANCE PASS

- App/source SHA: `adf26b803daf283cda51faaed9ebe51cf855e7fd`.
- Phone: 1080x1920 / 90 frames.
- projectM provider 632 ms; composition 4543 ms; encoder 586 ms; total 7224 ms.
- GPU projectM 134 ms; GPU glow 7 ms; GPU overlay 259 ms.
- projectM internals: queue 7 ms; native render 757 ms; glReadPixels 4802 ms; bitmap copy 114 ms.
- Composition layers:
  - clear 0 ms;
  - projectM draw 0 ms;
  - Cyber Shark 2809 ms;
  - effects 417 ms;
  - HUD update 25 ms;
  - HUD draw 1178 ms.
- Cyber Shark internals:
  - background 99 ms;
  - CPU glow 0 ms;
  - frame 1560 ms;
  - FX 255 ms;
  - creature 459 ms;
  - wordmark 423 ms.
- Compare v0.19.18:
  - total 8275 -> 7224 ms (-1051 ms / ~12.7%);
  - composition 5999 -> 4543 ms (-1456 ms / ~24.3%);
  - projectM draw 1710 -> 0 ms;
  - GPU projectM = 134 ms.
- Compare v0.19.11 baseline: total 14925 -> 7224 ms (~51.6% faster wall time).
- Performance status: PASS.
- Visual equivalence still requires explicit phone confirmation for projectM orientation/colors/framing and GPU glow.
- Next measured CPU bottleneck: Cyber Shark frame = 1560 ms; HUD draw = 1178 ms.

---

## 2026-10-06 — v0.19.20 / build 109 — GPU Cyber Shark frame — Android PASS

- Status: BUILD PASS / phone performance + visual QA pending.
- App/source commit: `c1438b06a0b604bde457be2ef5d54ec40cf1216d`.
- Android workflow: #496, attempt 1.
- Workflow run id: `37535685093`.
- Artifact: `FARIC-Music-Visualizer-v0.19.20-Debug`.
- Artifact id: `11446607008`.
- Artifact digest: `sha256:12e3c8eddaec4bc17dfe3932a0932a146c5107a128a5f7fb507118352bbe050d`.
- Validate #835 — PASS on the same app/source SHA.
- Successfully compiled/tested:
  - exact GPU frame transform spec using the same center/size/rotation/opacity math as CPU Canvas;
  - lower transparent pass keeps Cyber Shark background arcs/particles below frame;
  - static Cyber Shark frame bitmap is drawn on its own EGL texture between lower and upper transparent passes;
  - upper transparent pass keeps FX/creature/wordmark/effects/HUD above frame;
  - premultiplied-alpha blending and linear texture filtering are preserved;
  - direct GPU projectM and GPU glow paths remain unchanged;
  - CPU fallbacks remain for non-qualifying scenes;
  - new `GPU frame` timing is surfaced.
- Phone baseline from v0.19.19:
  - total 7224 ms;
  - composition 4543 ms;
  - projectM draw 0 ms;
  - GPU projectM 134 ms;
  - GPU glow 7 ms;
  - GPU overlay 259 ms;
  - Cyber Shark frame 1560 ms;
  - HUD draw 1178 ms.
- Phone acceptance:
  - Cyber Shark `frame` should become 0 ms;
  - `GPU frame` should appear and be nonzero;
  - `GPU overlay` will include two transparent uploads and may rise;
  - compare composition / encoder / total;
  - visually verify frame size, rotation, opacity, position and exact z-order;
  - re-check projectM orientation/colors/framing and GPU glow.
- Single next resume step:
  - install v0.19.20 via Termux 3 -> 10 -> 8, run the same warm-cache 3-second / 90-frame preview, paste the full timing block and report any frame/projectM/glow visual difference.

---

## 2026-10-07 — v0.19.20 phone GPU-frame result — GPU FRAME PASS / WALL-TIME NEUTRAL

- App/source SHA: `c1438b06a0b604bde457be2ef5d54ec40cf1216d`.
- Phone: 1080x1920 / 90 frames.
- projectM provider 1716 ms; composition 3228 ms; encoder 862 ms; total 7258 ms.
- GPU projectM 135 ms; GPU glow 6 ms; GPU frame 12 ms; GPU overlay 529 ms.
- projectM internals: queue wait 7 ms; native render 833 ms; glReadPixels 4787 ms; bitmap copy 103 ms.
- Composition:
  - projectM draw 0 ms;
  - Cyber Shark 1384 ms;
  - Cyber Shark frame 0 ms;
  - FX 274 ms;
  - creature 537 ms;
  - wordmark 470 ms;
  - effects 443 ms;
  - HUD draw 1147 ms.
- Compare v0.19.19:
  - composition 4543 -> 3228 ms (-1315 ms / ~28.9%);
  - Cyber Shark frame 1560 -> 0 ms;
  - GPU frame = 12 ms;
  - GPU overlay 259 -> 529 ms because the transparent composition is now split into lower + upper uploads;
  - projectM provider 632 -> 1716 ms because shorter CPU composition no longer hides the synchronous readback;
  - total 7224 -> 7258 ms (effectively unchanged).
- Conclusion:
  - GPU-frame implementation itself is successful;
  - further CPU-layer migration will not materially lower wall time while projectM still performs ~4.8 s of synchronous GPU->CPU readback;
  - the next real bottleneck is `glReadPixels`, not HUD/frame/creature.
- Visual equivalence still requires explicit phone confirmation.

---

## 2026-10-07 — v0.19.21 / build 110 — projectM direct GPU FBO — Android PASS

- Status: BUILD PASS / phone performance + visual QA pending.
- Exact app/source commit that produced the APK: `95bcd5f65075c308dde8d7b41cb3db783038506d`.
- Android workflow: #499, attempt 1.
- Workflow run id: `37542386259`.
- Artifact: `FARIC-Music-Visualizer-v0.19.21-Debug`.
- Artifact id: `11449526827`.
- Artifact digest: `sha256:f19df4633bcf6e4b5ff8fa0ada957a027601f5e3d44c16bf02eec77f34786ce8`.
- Validate #839 — PASS on the same final app/source SHA.
- Earlier build attempts #497/#498 did not produce artifacts; both exposed patch-location/format issues and were repaired before this successful SHA.
- Successfully compiled and linked:
  - FARIC backport of projectM 4.1.7 target-framebuffer rendering;
  - JNI + ProjectMBridge FBO render entry point;
  - projectM creation inside the MediaCodec encoder EGL context for the guarded export path;
  - direct render into a texture-backed FBO with deterministic frameIndex/fps time and the same PCM/SceneSignal feed;
  - direct sampling of that projectM texture during encoder composition;
  - no Bitmap/readback requirement on the guarded direct path;
  - v0.19.20 GPU glow + GPU frame path remains intact;
  - legacy readback path remains as fallback for non-qualifying compositions;
  - hidden Export Lab projectM renderer is recreated after export.
- Phone baseline from v0.19.20:
  - total 7258 ms;
  - projectM provider 1716 ms;
  - projectM internals: native render 833 ms, glReadPixels 4787 ms, bitmap copy 103 ms;
  - composition 3228 ms;
  - encoder 862 ms;
  - GPU projectM 135 ms; GPU glow 6 ms; GPU frame 12 ms; GPU overlay 529 ms.
- Phone acceptance:
  - timing should report `projectM BGRA: GPU direct`;
  - the old `projectM internals` block / `glReadPixels` should be absent on this path;
  - compare projectM / composition / encoder / total;
  - visually verify the exact projectM preset, colors, orientation, scale/crop, FARIC foreground reaction, Cyber Shark glow/frame and z-order.
- Single next resume step:
  - install v0.19.21 via Termux 3 -> 10 -> 8, run the same warm-cache 3-second / 90-frame preview, paste the full timing block and report any visual difference.

---

## 2026-10-07 — v0.19.21 phone direct-FBO result — PERFORMANCE PASS / VISUAL QA PENDING

- Phone: 1080x1920 / 90 frames.
- projectM BGRA: GPU direct.
- projectM provider: 808 ms.
- composition: 3255 ms.
- encoder: 597 ms.
- GPU projectM: 13 ms.
- GPU glow: 6 ms.
- GPU frame: 11 ms.
- GPU overlay: 394 ms.
- audio: 432 ms; mux: 101 ms; save: 115 ms.
- total: 6483 ms.
- CPU composition:
  - Cyber Shark 1282 ms;
  - background 130 ms;
  - frame 0 ms;
  - FX 256 ms;
  - creature 450 ms;
  - wordmark 430 ms;
  - effects 428 ms;
  - HUD update 24 ms;
  - HUD draw 1162 ms.
- Compare v0.19.20:
  - total 7258 -> 6483 ms (-775 ms / -10.7%);
  - projectM provider 1716 -> 808 ms (-908 ms / -52.9%);
  - encoder 862 -> 597 ms (-265 ms / -30.7%);
  - composition 3228 -> 3255 ms (+27 ms / +0.8%, effectively unchanged);
  - GPU projectM 135 -> 13 ms.
- The old projectM readback block is absent, confirming the direct-FBO path and removal of glReadPixels from the guarded export path.
- Performance conclusion: direct projectM FBO is accepted.
- Overall visual acceptance remains pending because this phone report contained timings only.
- New measured CPU target: Cyber Shark creature + wordmark = 880 ms combined; HUD draw = 1162 ms remains the largest individual layer.

---

## 2026-10-07 — v0.19.22 / build 111 — GPU creature + wordmark — Android PASS

- Status: BUILD PASS / phone performance + visual QA pending.
- Exact app/source commit that produced the APK: `1bf20367c15f109b934980bbccc64a2d74c59bea`.
- Validate #841 — PASS, run `37544681053`.
- Android #500 — PASS, run `37544681027`.
- Artifact: `FARIC-Music-Visualizer-v0.19.22-Debug`.
- Artifact id: `11450845435`.
- Artifact digest: `sha256:c48e0a57ebbb45011f3a07f47aa3049fbd0aa400a5df53d47deea6dc3aa4bd0a`.
- Baseline phone result is v0.19.21:
  - total 6483 ms;
  - projectM 808 ms;
  - composition 3255 ms;
  - encoder 597 ms;
  - Cyber Shark 1282 ms;
  - creature 450 ms;
  - wordmark 430 ms;
  - HUD draw 1162 ms.
- v0.19.22 changes:
  - dedicated EGL textures for Cyber Shark creature and wordmark;
  - exact existing BoardLayerMotion/group/layer transforms reused for GPU quads;
  - z-order preserved: background -> frame -> FX -> creature -> wordmark -> effects -> HUD;
  - FX and final effects+HUD are split into separate transparent overlays;
  - v0.19.21 direct-projectM FBO, GPU glow and GPU frame remain;
  - guarded fallback retains v0.19.21 frame-only composition if advanced GPU-layer prerequisites fail.
- New timing metrics: `GPU creature`, `GPU wordmark`.
- Phone acceptance target:
  - CPU creature = 0 ms;
  - CPU wordmark = 0 ms;
  - GPU creature > 0 ms;
  - GPU wordmark > 0 ms;
  - compare composition / GPU overlay / encoder / total;
  - visually verify creature/wordmark position, size, rotation, opacity, reaction and z-order.
---

## 2026-10-07 — v0.19.22 phone result — PERFORMANCE + VISUAL PASS

- Exact APK source SHA: `1bf20367c15f109b934980bbccc64a2d74c59bea`.
- Phone: 1080x1920 / 90 frames.
- User accepted visual QA as PASS.
- projectM 672 ms; composition 2306 ms; encoder 684 ms; audio 437 ms; mux 93 ms; save 158 ms.
- Total: 5451 ms.
- projectM path: `projectM BGRA: GPU direct`.
- GPU timings: projectM 10 ms; glow 5 ms; frame 11 ms; creature 10 ms; wordmark 6 ms; overlay 503 ms.
- CPU composition: Cyber Shark 356 ms; effects 433 ms; HUD update 23 ms; HUD draw 1081 ms.
- Cyber Shark internals: background 111 ms; frame 0 ms; FX 235 ms; creature 0 ms; wordmark 0 ms.
- v0.19.22 acceptance targets were met: creature and wordmark migrated off CPU to GPU, while direct projectM FBO / GPU glow / GPU frame remained active.
- Compare v0.19.21 total 6483 -> 5451 ms (-1032 ms / -15.9%).
- Compare v0.19.11 baseline total 14925 -> 5451 ms (~63.5% lower, ~2.74x faster).
- Status: PHONE-ACCEPTED / CURRENT KNOWN GOOD.
- Next: audit/generalize converter/export behavior for alternate layer sets, then profile HUD internals before changing HUD rendering.
---

## 2026-10-07 — v0.19.23 / build 112 — generalized GF compositor + Cyber Panther — Android PASS

- Status: BUILD PASS / phone QA pending.
- Exact APK/source commit: `7f2b3373ef96cdcde33f40117c1cd9c44807d97a`.
- Validate workflow: #857, run `37553402776` — PASS.
- Android workflow: #509, run `37553402797` — PASS.
- Artifact: `FARIC-Music-Visualizer-v0.19.23-Debug`.
- Artifact id: `11454285483`.
- Artifact digest: `sha256:2dbdb8318cd07f2dbd3038cdbcb0e687187101e37be3de518267d3bc019fe417`.
- Successfully completed in code/build:
  - generalized direct-projectM EGL compositor with optional glow/frame/creature/wordmark stages;
  - removed the old all-or-nothing frame + creature + wordmark dependency for the advanced GF path;
  - preserved ordered CPU overlay slots and existing correctness fallbacks;
  - added persistent GF selection in Layer 3;
  - added Cyber Panther as a distinct second GF using Background/Glow + FX + Creature only;
  - Cyber Panther has no frame and no wordmark by design, exercising a different layer set;
  - Panther creature art is a static procedural bitmap generated once and eligible for the GPU creature texture path;
  - Layer 3 object controls and Board editor are filtered to the selected GF's supported layers;
  - transform/reaction stores remain separated by GF theme id.
- Reference phone-known-good remains v0.19.22: 1080x1920 / 90 frames, total 5451 ms.
- Phone acceptance still required:
  1. Cyber Shark regression preview;
  2. Cyber Panther live visual/reaction check;
  3. Cyber Panther 3-second / 90-frame export with direct projectM;
  4. require GPU creature > 0 and GPU frame/wordmark = 0 for Panther;
  5. test one reduced Panther layer combination, such as Background OFF + FX + Panther.
- Single next resume step:
  - install v0.19.23 via Termux 3 -> 10 -> 8 and perform the Shark regression followed by the Panther tests above.

### Phone evidence update — v0.19.23 Cyber Panther

- Technical export result: PASS; visual QA still pending.
- 1080x1920 / 90 frames: total 6270 ms; projectM 814 ms; composition 2535 ms; encoder 908 ms.
- Direct projectM confirmed: `projectM BGRA: GPU direct`.
- GPU timings: projectM 13 ms; glow 5 ms; creature 14 ms; overlay 701 ms.
- CPU GF: total 412 ms; background 126 ms; FX 270 ms; creature/frame/wordmark all 0 ms.
- Expected Panther signature is present: creature on GPU, no frame/wordmark stages.
- Remaining acceptance: visual Panther check plus one reduced-layer export combination.

