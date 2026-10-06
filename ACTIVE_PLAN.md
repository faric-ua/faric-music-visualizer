# Active Plan

## CURRENT PRIORITY — v0.19.22 GPU creature + wordmark

- [x] v0.19.21 phone performance PASS: total 6483 ms.
- [x] Confirm projectM direct-FBO branch active: `projectM BGRA: GPU direct`.
- [x] Confirm old glReadPixels/readback metrics absent.
- [x] Identify current CPU composition targets: HUD draw 1162 ms; creature 450 ms; wordmark 430 ms.
- [x] Defer HUD GPU rewrite because its modular SCREEN/text/spectrum/progress path is higher-risk.
- [x] Reuse exact Cyber Shark bitmap-layer transform math for creature and wordmark.
- [x] Add dedicated static EGL textures for creature and wordmark.
- [x] Preserve z-order with separate FX and final effects+HUD overlays.
- [x] Add guarded fallback to v0.19.21 frame-only path.
- [x] Add `GPU creature` / `GPU wordmark` timings.
- [x] Bump v0.19.22 / build 111.
- [ ] Validate + Android PASS.
- [ ] Write BUILD_CHECKPOINT immediately after successful Android build.
- [ ] Phone same warm-cache 3-second / 90-frame preview.
- [ ] Require CPU creature = 0 ms and wordmark = 0 ms, with GPU creature/wordmark > 0.
- [ ] Compare composition / GPU overlay / encoder / total.
- [ ] Visual QA: creature and wordmark position, scale, rotation, opacity, reaction and z-order.

## CURRENT PRIORITY — phone-test v0.19.21 direct projectM FBO

- [x] v0.19.20 phone result: GPU frame PASS, wall-time neutral.
- [x] Diagnose new limiter: glReadPixels = 4787 ms; queue wait only 7 ms.
- [x] Reject deeper prefetch as the primary fix: producer throughput itself is the limit.
- [x] Backport target-FBO render entry point into pinned projectM 4.1.7.
- [x] Add JNI/ProjectMBridge target-FBO render call.
- [x] Add encoder-context projectM FBO + texture.
- [x] Recreate deterministic offline projectM in encoder EGL context for qualifying scene.
- [x] Feed same per-frame PCM + SceneSignal + frameIndex/fps clock.
- [x] Sample projectM FBO texture directly in encoder composition.
- [x] Preserve old readback pipeline as fallback for non-qualifying scenes.
- [x] Restore hidden Export Lab projectM instance after export.
- [x] v0.19.21 / build 110.
- [x] Final exact APK source SHA `95bcd5f65075c308dde8d7b41cb3db783038506d`.
- [x] Validate #839 PASS.
- [x] Android #499 PASS, run `37542386259`.
- [x] Artifact `FARIC-Music-Visualizer-v0.19.21-Debug`, id `11449526827`.
- [x] BUILD_CHECKPOINT recorded.
- [ ] Phone: Termux 3 -> 10 -> 8.
- [ ] Same warm-cache 3-second / 90-frame preview.
- [ ] Require timing text to show `projectM BGRA: GPU direct` and no glReadPixels block.
- [ ] Compare projectM / composition / encoder / total.
- [ ] Visual QA: projectM preset, orientation, crop, colors, foreground reaction, Cyber Shark glow/frame and z-order.

## CURRENT PRIORITY — phone-test v0.19.20 GPU Cyber Shark frame

- [x] v0.19.19 phone performance PASS: total 7224 ms.
- [x] projectM draw 1710 -> 0 ms; GPU projectM 134 ms.
- [x] Identify next single CPU bottleneck: Cyber Shark frame = 1560 ms.
- [x] Add exact GPU frame transform spec (center/size/rotation/alpha).
- [x] Split transparent composition around frame to preserve z-order.
- [x] Add static EGL frame texture + transformed premultiplied-alpha draw.
- [x] Keep direct GPU projectM and GPU glow unchanged.
- [x] Add `GPU frame` timing.
- [x] v0.19.20 / build 109.
- [x] Validate #835 PASS on `c1438b06a0b604bde457be2ef5d54ec40cf1216d`.
- [x] Android #496 PASS, run `37535685093`.
- [x] Artifact `FARIC-Music-Visualizer-v0.19.20-Debug`, id `11446607008`.
- [x] BUILD_CHECKPOINT recorded.
- [ ] Phone: Termux 3 -> 10 -> 8.
- [ ] Same warm-cache 3-second / 90-frame preview.
- [ ] Require Cyber Shark frame = 0 ms and GPU frame > 0 ms.
- [ ] Compare composition / encoder / GPU overlay / total.
- [ ] Visual QA: frame size, rotation, opacity, position and z-order; projectM/glow still correct.

## CURRENT PRIORITY — phone-test v0.19.19 GPU-scale projectM

- [x] v0.19.18 phone test: branch did not activate; projectM draw 1710 ms, no GPU projectM metric.
- [x] Confirm root cause: BALANCED_BACKGROUND renderScale = 0.78.
- [x] Remove exact framebuffer-size requirement.
- [x] Add dedicated variable-size projectM EGL texture.
- [x] Reproduce Canvas scale-to-fill + center-crop on GPU.
- [x] Preserve raw GL orientation and BGRA/layer-order guards.
- [x] v0.19.19 / build 108.
- [x] Validate #833 PASS on `adf26b803daf283cda51faaed9ebe51cf855e7fd`.
- [x] Android #495 PASS, run `37529043474`.
- [x] Artifact `FARIC-Music-Visualizer-v0.19.19-Debug`, id `11443104041`.
- [x] BUILD_CHECKPOINT recorded.
- [ ] Phone: Termux 3 -> 10 -> 8.
- [ ] Same warm-cache 3-second / 90-frame preview.
- [ ] Require projectM draw = 0 ms and GPU projectM > 0 ms in the qualifying scene.
- [ ] Compare composition / encoder / total.
- [ ] Visual QA: projectM orientation, colors, framing/crop; GPU glow still correct.

## CURRENT PRIORITY — phone-test v0.19.18 direct GPU projectM base

- [x] v0.19.17 phone performance PASS: total 8263 ms.
- [x] GPU glow reduced CPU glow 1662 -> 0 ms; GPU glow = 6 ms.
- [x] Identify next measured bottleneck: software projectM draw = 1708 ms.
- [x] Confirm offline ProjectMView uses exact export aspect dimensions.
- [x] Add guarded direct raw-projectM GPU base path with GL-orientation correction.
- [x] Keep CPU fallback for channel/dimension/layer-order cases that do not qualify.
- [x] Add `GPU projectM` timing.
- [x] v0.19.18 / build 107.
- [x] Validate #831 PASS on `655b367118787ca07d0d2654c56b12723005c3c6`.
- [x] Android #494 PASS, run `37523326093`.
- [x] Artifact `FARIC-Music-Visualizer-v0.19.18-Debug`, id `11440804926`.
- [x] BUILD_CHECKPOINT recorded.
- [ ] Phone: Termux 3 -> 10 -> 8.
- [ ] Same warm-cache 3-second / 90-frame preview.
- [ ] Compare projectM draw / GPU projectM / composition / encoder / total.
- [ ] Visual QA: projectM orientation, colors and framing; also re-check v0.19.17 GPU glow equivalence.

## CURRENT PRIORITY — phone-test v0.19.17 hybrid GPU glow

- [x] v0.19.16 phone result: glow composite 1396 ms, no improvement vs filtered 1397 ms.
- [x] Reject unfiltered CPU composite as performance no-op.
- [x] Split composition into base + transparent upper overlay.
- [x] Draw Cyber Shark radial glow on existing encoder EGL/GLES2 surface between the two CPU textures.
- [x] Preserve layer order and keep a correctness fallback.
- [x] Add GPU glow / GPU overlay timings.
- [x] v0.19.17 / build 106.
- [x] Validate #829 PASS on `5b1c14122d7f887b6f29f0e7391c59b9e9a7b2e6`.
- [x] Android #493 PASS, run `37519271112`.
- [x] Artifact `FARIC-Music-Visualizer-v0.19.17-Debug`, id `11437784670`.
- [x] BUILD_CHECKPOINT recorded.
- [ ] Phone: Termux 3 -> 10 -> 8.
- [ ] Same warm-cache 3-second / 90-frame preview.
- [ ] Compare composition / encoder / GPU glow / GPU overlay / total.
- [ ] Visual QA: glow color, brightness, radius, placement and ordering match previous output.

## CURRENT PRIORITY — phone-test v0.19.16 unfiltered glow

- [x] v0.19.15 phone profile received.
- [x] Glow 1663 ms = render 265 ms + composite 1397 ms.
- [x] Identified filtered software scaling as dominant glow cost.
- [x] Remove FILTER_BITMAP_FLAG from only the cached glow composite.
- [x] Keep 512x512 glow raster and all final output settings unchanged.
- [x] v0.19.16 / build 105.
- [x] Validate #826 PASS on `ae32d61ae81b178e8437adf9fdc85e037c6df189`.
- [x] Android #492 PASS, run `37514357810`.
- [x] Artifact `FARIC-Music-Visualizer-v0.19.16-Debug`, id `11436347682`.
- [x] BUILD_CHECKPOINT recorded.
- [ ] Phone: Termux 3 -> 10 -> 8.
- [ ] Same warm-cache 3-second / 90-frame preview.
- [ ] Compare glow composite / glow / Cyber Shark / composition / total.
- [ ] Visual QA: reject if rings, stair-stepping or obvious pixelation appear in the glow.

## CURRENT PRIORITY — phone-test v0.19.15 cached glow

- [x] v0.19.14 identified glow = 1755 ms of background = 1848 ms.
- [x] Implement reusable 512x512 dynamic glow raster + filtered final-size composite.
- [x] Preserve final output 1080x1920-class / 30 FPS.
- [x] v0.19.15 / build 104.
- [x] Validate #824 PASS on `9fbb48d5023a2e12de59356ed148fa04a3b34258`.
- [x] Android #491 PASS, run `37512222805`.
- [x] Artifact `FARIC-Music-Visualizer-v0.19.15-Debug`, id `11434899037`.
- [x] BUILD_CHECKPOINT recorded.
- [ ] Phone: Termux 3 -> 10 -> 8.
- [ ] Same warm-cache 3-second / 90-frame preview.
- [ ] Paste full timing block, especially glow / glow render / glow composite / total.
- [ ] Visual QA: glow remains smooth, same apparent brightness/radius and audio reaction.
- [ ] If performance PASS + visual PASS, move to frame layer (currently ~1145 ms); otherwise revise glow cache resolution/strategy.

## CURRENT PRIORITY — v0.19.15 cached radial glow

- [x] v0.19.14 phone profile: background 1848 ms, glow 1755 ms.
- [x] Confirmed glow is ~95% of background.
- [x] Replace direct full-size software RadialGradient with reusable 512x512 glow bitmap + filtered final-size composite.
- [x] Preserve 1080x1920-class final output, 30 FPS and all non-glow render paths.
- [x] Add glow render/composite sub-timings.
- [x] Bump v0.19.15 / build 104.
- [ ] Validate + Android PASS.
- [ ] Phone: same warm-cache 3-second / 90-frame preview.
- [ ] Paste timing block.
- [ ] Visually compare glow smoothness/brightness/shape against v0.19.14; reject if visible degradation is material.

## CURRENT PRIORITY — phone-measure v0.19.14 background internals

- [x] v0.19.13 profile identified background as largest Cyber Shark sublayer: 1858 ms.
- [x] v0.19.14 / build 103 background subprofiler implemented.
- [x] Validate #822 PASS on `5a7ca6a16b3387e17704d8ab9f9aba5499ec3a42`.
- [x] Android #490 PASS, run `37510375626`.
- [x] Artifact `FARIC-Music-Visualizer-v0.19.14-Debug`, id `11435801494`.
- [x] BUILD_CHECKPOINT recorded.
- [ ] Phone: Termux 3 -> 10 -> 8.
- [ ] Same warm-cache 3-second / 90-frame preview.
- [ ] Paste full timing including background internals: setup/save / glow / arcs / particles / restore.
- [ ] Optimize only the largest measured component, preserving final output quality.

## CURRENT PRIORITY — v0.19.14 Cyber Shark background profiler

- [x] v0.19.13 phone sublayer profile received.
- [x] Cyber Shark 4487 ms = background 1858 + frame 1475 + FX 273 + creature 445 + wordmark 423 ms.
- [x] Background is the largest Cyber Shark sublayer.
- [x] Add background-only profiler: setup/save / glow / arcs / particles / restore.
- [x] Bump v0.19.14 / build 103.
- [ ] Validate + Android PASS.
- [ ] Phone same warm-cache 3-second / 90-frame preview.
- [ ] Paste timing including `background internals`.
- [ ] Optimize the largest measured background component; do not change output quality based on assumption.

## CURRENT PRIORITY — measure Cyber Shark sublayers on v0.19.13

- [x] v0.19.12 performance PASS: total 9915 ms, projectM provider 107 ms.
- [x] Added Cyber Shark sublayer profiler without intended visual changes.
- [x] v0.19.13 / build 102.
- [x] Validate #820 PASS on `8f374445a0dc566a924c5a275f42c9914664eba9`.
- [x] Android #489 PASS, run `37501072748`.
- [x] Artifact `FARIC-Music-Visualizer-v0.19.13-Debug`, id `11429668183`.
- [x] BUILD_CHECKPOINT recorded.
- [ ] Phone: install through Termux 3 -> 10 -> 8.
- [ ] Run same warm-cache 3-second / 90-frame preview.
- [ ] Paste full timing block including `Cyber Shark internals`.
- [ ] Optimize only the largest measured Cyber Shark sublayer.

## CURRENT PRIORITY — Cyber Shark sublayer profiler after v0.19.12 PASS

- [x] v0.19.12 phone performance benchmark received.
- [x] projectM provider wait 5295 -> 107 ms (-97.98%).
- [x] total 14925 -> 9915 ms (-33.57%, ~1.51x faster).
- [x] glReadPixels unchanged at ~4.42 s, confirming overlap works.
- [x] New dominant stage is composition 7829 ms.
- [x] Largest composition layer is Cyber Shark 4382 ms.
- [ ] Do not guess the next Cyber Shark optimization.
- [ ] Instrument Cyber Shark background / frame / FX / creature / wordmark separately.
- [ ] Build next diagnostic candidate and run the same 3-second / 90-frame phone preview.
- [ ] Optimize only the measured dominant Cyber Shark sublayer.
- [ ] Full visual/audio acceptance of v0.19.12 remains pending explicit user confirmation.

## CURRENT PRIORITY — v0.19.12 readback/composition overlap

- [x] v0.19.11 clipboard action copied the complete phone timing block into chat.
- [ ] Dialog-stays-open behavior after copy was not explicitly confirmed.
- [x] Phone baseline captured: 1080x1920 / 90 frames; projectM 5295 ms; glReadPixels 4421 ms; composition 7468 ms; total 14925 ms; BGRA yes.
- [x] Implement two-slot projectM offline readback cache.
- [x] Pipeline frame N+1 projectM render/readback while frame N is CPU-composited.
- [x] Bump to v0.19.12 / build 101.
- [x] Validate #816 PASS on `86750315d89c6ebb3b332e912ddc92a1759b3cbb`.
- [x] Android #488 PASS, run `37490177560`.
- [x] Artifact `FARIC-Music-Visualizer-v0.19.12-Debug`, id `11425346915`.
- [x] BUILD_CHECKPOINT recorded.
- [ ] Phone: Termux 3 -> 10 -> 8 and install v0.19.12.
- [ ] Phone: same warm-cache 3-second / 90-frame preview.
- [ ] Paste timing text and compare top-level projectM + total against 5295 / 14925 ms baseline.
- [ ] Verify picture, frame order, colors/orientation and audio.
- [ ] Do not stack another performance optimization until this measurement is accepted.

Expected profiler signature: accumulated `glReadPixels` can stay around the old ~4.4 s because this candidate overlaps work rather than eliminating readback. The win should appear primarily in provider wait and total wall-clock.

## CURRENT PRIORITY — NEW CHAT RESUME 2026-10-06

- [x] v0.19.11 / build 100 source complete.
- [x] Android #487 PASS on `5b9c00b45993a01fe0be0922de8714db1c758e97`.
- [x] Validate #813 PASS.
- [x] Artifact recorded in `BUILD_CHECKPOINTS.md`.
- [ ] **Phone QA v0.19.11:** install via Termux **3 → 10 → 8**.
- [ ] Run one 3-second preview.
- [ ] Tap **«Копіювати текст»**.
- [ ] Confirm full timing text is copied and dialog remains open.
- [ ] Paste copied timing text into chat; screenshots should no longer be needed.
- [ ] After clipboard UX PASS, continue performance work on **projectM GPU→CPU readback / glReadPixels**.
- [ ] Preserve current preview/full-song semantics and final 30 FPS quality.

Measured reason for next performance target:
`projectM 5270 ms = queue 16 + native render 730 + glReadPixels 4361 + bitmap copy 97`.
The dominant cost is **glReadPixels**. Do not optimize unrelated stages first.

## Verified foundation

- [x] Public repository + Termux workflow.
- [x] Baseline validation CI.
- [x] Android scaffold.
- [x] Stable signer workflow.
- [x] Local audio playback is real-phone confirmed by user.
- [x] Poweramp screenshots analyzed as interaction references only.
- [x] Original FARIC PulseDeck product language defined.
- [x] Shared standalone/YTM player architecture defined.

## v0.2.1 — PulseDeck fullscreen hotfix

- [x] Create v0.2.0 release/QA/diagram skeleton before feature code.
- [x] Replace temporary main screen with Library landing shell (source candidate d01c374).
- [x] Add persistent PulseDock mini-player (source candidate d01c374).
- [x] Add full Now Playing screen using PulseCore visualizer (source candidate d01c374).
- [x] Add seek + elapsed/total controls (source candidate d01c374).
- [x] Preserve file picker + playback + audio-reactive analysis in source candidate; phone regression still pending.
- [ ] CI build/test PASS for v0.2.1 source.
- [ ] Download signed v0.2.1 APK.
- [ ] Phone QA: app launches without crash in fullscreen; Library → pick track → PulseDock → Now Playing.
- [ ] Phone QA: seek/play/pause/reactive scene.
- [ ] Phone QA: rotation + Back lifecycle.
- [ ] Record findings and close v0.2.0 shell scope.

## Next player phases

- [ ] Local media scanner and durable library index.
- [ ] Library Worlds: tracks/folders/albums/artists/genres/years.
- [ ] Queue + previous/next + shuffle/repeat.
- [ ] Favorites and multi-select Action Shelf.
- [ ] Unified search.
- [ ] Tone Lab EQ foundation.
- [ ] Scene Lab background/preset controls.
- [ ] Versioned reusable player library for YTM.
- [ ] Official YouTube embedded backend inside YTM.

## Later

- [ ] Procedural shader backgrounds.
- [ ] Particles.
- [ ] AI-generated scenes.
- [ ] Recording/export.


### Crash hotfix notes

- [x] User reported startup crash after first immersive fullscreen implementation.
- [x] Replace direct platform insets APIs with AndroidX WindowCompat / WindowInsetsCompat.
- [x] Move immersive activation until after setContentView.
- [x] Guard fullscreen/inset handling so failure cannot crash the app.
- [x] Bump package version to 0.2.1 / versionCode 3.
- [ ] Install v0.2.1 on phone and confirm launch PASS.


## v0.3.0 — Random Visualizer Core

- [x] Define two-layer scene architecture: foreground visualizer + independent background.
- [x] Document controlled-random behavior and background source priority.
- [x] Create v0.3.0 release / QA / diagram skeleton.
- [x] Add SceneSpec / visualizer / background / palette models.
- [x] Add testable RandomSceneEngine with immediate-repeat prevention.
- [x] Add SceneOrchestrator with manual + timed switching.
- [x] Implement 3 foreground visualizer styles in source candidate: radial / wave ribbon / spectrum bars.
- [x] Implement 4 built-in procedural backgrounds in source candidate: aurora / neon mist / night grid / ember cloud.
- [x] Add ~850 ms scene/background crossfade.
- [x] Add manual Shuffle Scene action to Now Playing.
- [x] Keep local FFT/beat stream alive across scene changes by changing only SceneSpec/rendering.
- [ ] CI build/test PASS.
- [ ] Signed APK phone QA with 3 musically different tracks.
- [ ] Close v0.3.0 only after automatic + manual scene switching phone PASS.


Current v0.3.0 source commits:
- `da4bb5e` — random scene models/engine/orchestrator + tests; Android CI PASS.
- `925e716` — renderer integration, 3 foreground styles, 4 procedural backgrounds, Shuffle action, version 0.3.0; CI pending.


## v0.3.1 — Instant Reactivity + Warp Background

- [x] Record phone finding: v0.3.0 reaction is too soft/late.
- [x] Add permanent UI centering rule UI-001.
- [x] Increase Android FFT capture to maximum supported rate.
- [x] Replace averaged frequency energy with peak + RMS response.
- [x] Add fast attack / slower release foreground response.
- [x] Split foreground frequency response from independent background drive.
- [x] Add WARP_STARFIELD background with music-driven travel speed/streak length.
- [x] Weight random background selection toward WARP_STARFIELD without immediate repeats.
- [x] Increase visual range of radial / wave / spectrum renderers.
- [ ] CI PASS for v0.3.1.
- [ ] Phone QA: immediate bass/mid/high reaction.
- [ ] Phone QA: background travel clearly independent from foreground.
- [ ] Phone QA: transport/action/nav glyphs visually centered.


## v0.4.0 — projectM integration spike + screen-off playback

- [x] Research official libprojectM Android/JNI example and current 4.x API.
- [x] Pin libprojectM to stable v4.1.7 for the spike.
- [x] Add NDK/CMake build path for arm64-v8a.
- [x] Add projectM LAB fullscreen activity.
- [x] Feed Android Visualizer waveform into projectM as 16-bit mono PCM.
- [x] Bundle only small upstream test presets for integration validation.
- [x] Add projectM entry from Now Playing.
- [x] Remove Activity onStop() pause that stopped music on screen lock.
- [ ] CI native build PASS.
- [ ] Phone: screen-off playback PASS for 60+ seconds.
- [ ] Phone: projectM renders and reacts to audio.
- [ ] Decide GO / NO-GO for projectM as FARIC foreground engine.
- [ ] If GO: move long-lived playback into MediaSessionService.
- [ ] If GO: render projectM as foreground layer above FARIC background compositor.


## v0.4.1 — Full preset library + TEST 40

- [x] Add one-time downloader for pinned Cream of the Crop commit.
- [x] Preserve complete 9,795-preset category hierarchy on device.
- [x] Add pinned MilkDrop texture-pack downloader.
- [x] Add zip-slip-safe extraction and preset-count validation.
- [x] Add TEST 40 generator: 8 each from Geometric / Particles / Supernova / Waveform / Hypnotic.
- [x] Add LAB controls: TEST 40 / ВСІ / NEXT.
- [x] Add texture search path to projectM JNI bridge.
- [x] Keep tiny bundled presets only as first-download fallback.
- [ ] Android CI PASS for final v0.4.1 HEAD.
- [ ] Phone full-library download/install PASS.
- [ ] Phone TEST 40 visual-quality review.
- [ ] Phone full-library switching smoke.


## v0.4.2 — projectM background + mobile optimization

- [x] Lock projectM role to background visual layer.
- [x] Lock future FARIC renderer role to foreground layer.
- [x] Add projectM phone performance profiles.
- [x] Default projectM background render scale to 78%.
- [x] Default projectM mesh to 72x40.
- [x] Shorten projectM soft cuts to reduce dual-preset render cost.
- [x] Add render FPS telemetry to logcat.
- [ ] CI PASS for v0.4.2.
- [ ] Phone quality/performance comparison PASS.
- [ ] Add adaptive AUTO quality tier after phone data.
- [ ] Replace Visualizer waveform feed with direct Media3 PCM.
- [ ] Render projectM into compositor texture/FBO below FARIC foreground.


## v0.5.0 — First real compositor

- [x] Keep stable projectM 4.1.7 as background engine.
- [x] Feed FARIC SceneSignal to native projectM GL context.
- [x] Render FARIC foreground after projectM in the same GL frame.
- [x] Add foreground pulse ring / rays / high-frequency sparkles.
- [x] Use fast attack and slower release in native foreground response.
- [x] Document stable-vs-development projectM FBO API boundary.
- [ ] Android CI PASS for v0.5.0.
- [ ] Phone: background and foreground visibly independent.
- [ ] Phone: foreground audio reaction PASS.
- [ ] Phone: 5-minute smoothness/heat PASS.
- [ ] Design next 3–5 original FARIC foreground scenes.


## v0.5.1 — Layered sample controls

- [x] Keep complete 9,795 projectM library installed.
- [x] Add derived TOP half-pool (~4,898 presets) without deleting full library.
- [x] Keep TEST 40 stored for QA.
- [x] TOP and ВСІ enable automatic background switching.
- [x] NEXT locks projectM automatic preset switching and changes only manually afterward.
- [x] TOP or ВСІ explicitly re-enable automatic switching after manual mode.
- [x] Preserve Pulse Rays foreground sample.
- [x] Add Orbit Rings foreground sample.
- [x] Add Spectrum Halo foreground sample.
- [x] Add FG control to switch layer-1 sample without changing layer-2 background.
- [ ] Android CI PASS for v0.5.1.
- [ ] Phone verify TOP count and manual/auto transition contract.
- [ ] Phone review all 3 foreground samples.


## v0.5.2 — Strong FG + fast preset queue

- [x] Document layered visualizer TODO in docs/visualizer/VISUALIZER_TODO.md.
- [x] Increase FG movement amplitude and bass-driven expansion.
- [x] Add solar-flare edge energy driven by bass/beat/highs.
- [x] Keep all existing FG samples available.
- [x] Remove large projectM directory scan from GL first-render path.
- [x] Fast-start from last valid preset.
- [x] Add CURRENT + 3 NEXT queue and background file prefetch.
- [x] FARIC owns AUTO/MANUAL 18s switching; NEXT/tap disables AUTO.
- [x] Persist TOP/ALL, AUTO/MANUAL, last preset and FG sample.
- [ ] Android CI PASS for v0.5.2.
- [ ] Phone fast-start / PRELOAD 3/3 PASS.
- [ ] Phone FG amplitude + solar flare review.


## v0.5.3 — Persistent preset ratings

- [x] Add NONE / UP / DOWN / HIDDEN rating model.
- [x] Add 👍 / 👎 / − controls.
- [x] Persist ratings across app restarts.
- [x] Use canonical relative preset identity across TOP and ALL.
- [x] HIDDEN removes preset from queue without deleting .milk file.
- [x] Weight queue selection UP 6 / NONE 3 / DOWN 1 / HIDDEN 0.
- [x] Show rating and current preset name in LAB status.
- [ ] Android CI PASS for v0.5.3.
- [ ] Phone rating persistence PASS.
- [ ] Phone hide/no-delete behavior PASS.


## v0.6.0 — Music video creator foundation

- [x] Define product roadmap for song → style → preview → export.
- [x] Add 10-theme PlaybackTheme registry.
- [x] Add MusicVideoProject export profile model.
- [x] Add 5 new reactive hero-center FG samples.
- [x] Keep previous 3 FG samples.
- [x] ProjectM Library Index v2: original presets stored once.
- [x] Default projectM seed pool reduced to 1,200 indexed items.
- [x] ALL 9,795 remains available by index.
- [x] v0.5.x duplicated TOP directory removed on migration.
- [ ] Android CI PASS for v0.6.0.
- [ ] Phone migration PASS.
- [ ] Phone Hero Pack 1 visual review.
- [ ] Next: standalone Neon Emblem theme + theme picker.
- [ ] Next: deterministic export spike using Media3 Transformer/effect pipeline.


## v0.6.1 — Standalone Hero Themes

- [x] Playback Theme picker is reachable from Scene Lab and Now Playing.
- [x] Selected theme persists across restarts.
- [x] Five standalone Hero Themes render from live SceneSignal without projectM.
- [x] Visualizer remains selectable as legacy/reactive mode.
- [ ] Android CI PASS.
- [ ] Phone review: visual quality, response, heat, switching latency.
- [ ] Next: export-parity deterministic renderer contract.


## v0.6.2 — Retro themes + measured projectM latency

- [x] Vinyl theme is selectable and reactive.
- [x] Cassette theme is selectable and reactive.
- [x] Current track metadata is passed into standalone themes.
- [x] projectM native load time is measured and shown in LAB.
- [x] Heavy presets are learned and skipped from FAST/TOP.
- [ ] Android CI PASS.
- [ ] Phone test retro themes and projectM LOAD timings.
- [ ] Next: export MVP architecture and first MP4 proof.


## v0.6.4 — Smooth projectM transitions

- [x] Add FARIC-owned fade between projectM presets.
- [x] Keep projectM direct load for meaningful LOAD timing.
- [x] Mask heavy preset load behind a lightweight transition veil.
- [x] Block repeated NEXT/rating/mode changes during transition.
- [x] Keep FG switching independent.
- [ ] Android CI PASS.
- [ ] Phone manual NEXT transition PASS.
- [ ] Phone AUTO transition PASS.


## v0.6.5 — Offline export analysis

- [x] Add deterministic full-track PCM analysis.
- [x] Add FARIC radix-2 FFT path independent from Android Visualizer.
- [x] Build SceneSignal timeline for exact timestamps.
- [x] Export Lab can analyze current local track.
- [x] PNG proof consumes offline timeline when ready.
- [ ] Android CI PASS for v0.6.5.
- [ ] Phone analysis PASS on real MP3/M4A.
- [ ] Next: short deterministic H.264 clip.
- [ ] Then: audio mux → real MP4 music-video proof.


## v0.6.6 — First H.264 video proof

- [x] Offline timeline drives every proof frame.
- [x] Add H.264 MediaCodec encoder proof.
- [x] Add MediaMuxer MP4 output.
- [x] Add Export Lab 3-second video action.
- [x] Save proof under Movies/FARIC.
- [ ] Android CI PASS for v0.6.6.
- [ ] Phone H.264 playback/color PASS.
- [ ] Next: audio mux/AAC.


## v0.6.7 — Audible MP4 proof

- [x] Add AAC audio transcode for selected proof range.
- [x] Add H.264 + AAC final MP4 mux.
- [x] Expose 3-second MP4-with-audio action in Export Lab.
- [ ] Android CI PASS for v0.6.7.
- [ ] Phone A/V sync PASS.
- [ ] Next: 30 fps / 1080p tuning.


## v0.6.8 — Continuous Layer-1 live reactivity hotfix

Phone finding from the 2026-09-29 screen recording:
- [x] Reproduce that standalone Layer 1 reacts strongly only for roughly the first 2–3 seconds after Play/resume, then settles into almost static motion while playback continues.
- [x] Keep Android Visualizer capture active; fix the live signal mapping instead of adding theme-specific fake animation.
- [x] Add adaptive local contrast for amplitude / bass / mid / high so small ongoing musical changes stay visible after the initial signal level has settled.
- [x] Rebase the live dynamics after real silence so Pause → Play does not create a false full-scale startup burst.
- [x] Reset beat baseline on silence so the first resumed frame is not treated as a beat.
- [x] Apply the hotfix before SceneSignal fan-out, so Energy Core, other standalone Hero themes, ReactiveSceneView and native FARIC Layer 1 share the same corrected live signal.
- [x] Add unit coverage for sustained reactivity and silence/resume rebasing.
- [ ] Android CI PASS for v0.6.8.
- [ ] Phone: Energy Core remains visibly reactive for 15+ seconds.
- [ ] Phone: pause 2+ seconds → resume has no artificial 2–3 second giant pulse.
- [ ] Phone: verify at least two additional Layer-1 themes.


## v0.6.9 — Layer-1 ring continuity hotfix

Phone finding from 333058.mp4:
- [x] v0.6.8 keeps live Layer-1 response active much longer than before.
- [x] Identify remaining visual seam: the visible outer ring is currently a transient beat shockwave, so it disappears between beat envelopes.
- [x] Identify radius snap: a new stronger beat can restart the same shockwave at a smaller radius, creating a visible inward jump.
- [x] Add two persistent Neon Emblem base rings that remain visible between beats and still react softly to amplitude / bass / high.
- [x] Split beat shockwaves into two alternating slots so an outgoing ring can continue expanding while the next beat starts a new ring.
- [x] Make each shockwave radius strictly progress outward with its own phase instead of deriving radius from the decaying beat value.
- [ ] Android CI PASS for v0.6.9.
- [ ] Phone: base Layer-1 rings remain visible continuously for 20+ seconds.
- [ ] Phone: consecutive beats produce overlapping outward waves without an inward radius snap.
- [ ] Phone: confirm ring visibility on dark/quiet passages remains subtle, not overpowering.


## v0.6.10 — Beat ring hit-detection correction

User clarification after 333058.mp4:
- [x] The inward/reset jump of the ring is desirable and should remain.
- [x] The real defect is that some audible beats do not produce a Layer-1 ring at all.
- [x] Revert the v0.6.9 persistent base-ring interpretation.
- [x] Keep the original beat-ring reset/jump behavior.
- [x] Expand beat detection from bass-only to bass OR broadband transient energy.
- [x] Add transient-rise triggering so snare/mid/high-heavy impacts can create a visual beat even without a large bass spike.
- [x] Keep a short cooldown to avoid double-firing the same hit.
- [x] Add unit coverage for broadband hits and repeated rhythmic hits.
- [ ] Android CI PASS for v0.6.10.
- [ ] Phone: obvious beats trigger the ring much more consistently.
- [ ] Phone: desired inward/reset jump remains visible.
- [ ] Phone: no excessive false rings between beats.


## Persistent master assets

- [x] Create private companion repository `faric-ua/faric-music-visualizer-assets`.
- [x] Configure image binaries for Git LFS.
- [x] Archive 34 master originals with manifest/provenance.
- [x] Add FARIC assets actions to the main Termux menu.
- [x] Phone clone/materialization PASS at `~/faric-music-visualizer-assets`.
- [x] Verify `Master-файлів: 34`.
- [x] Verify `LFS materialization: PASS`.
- [x] Promote first selected Hero/GF concept into a production candidate: Cyber Shark v1 · full / frame / creature / wordmark / fx / manifest.
- [x] Add first reusable Board layer reaction model and unit tests.
- [x] Connect Cyber Shark as the first live layered Board theme.
- [ ] Android CI PASS for v0.7.0.
- [ ] Phone: select Cyber Shark and verify frame / FX / creature / wordmark react independently.
- [ ] Phone: tune layer strengths after visual review.
- [ ] Next: deterministic export parity for bitmap/layered Board themes.


## v0.7.3 — Cyber Shark layout phone check

- [x] Real Cyber Shark layered assets render on phone.
- [x] Reduce default Hero/GF size and move it upward.
- [x] Phone evidence `376668.mp4`: layout is visibly improved and no longer dominated by the temporary player card.
- [x] Record that the current Now Playing card is temporary scaffolding.
- [ ] Add whole-Hero Board transform controls: X / Y / size / rotation / opacity / reset / Fit Safe Area.
- [ ] Persist Board transform values.
- [ ] Add advanced per-layer transform overrides.
- [ ] Redesign Now Playing into floating PulseDeck control blocks.
- [ ] Revisit safe-area defaults after the floating-control layout exists.
- [ ] Continue deterministic preview/export parity for layered Board themes after live transform controls are stable.


## v0.7.4 — Live Board transform editor

- [x] Add reusable `BoardTransform` model.
- [x] Persist transforms per playback theme.
- [x] Add live X / Y controls.
- [x] Add live size control.
- [x] Add live rotation control.
- [x] Add live opacity control.
- [x] Keep manual transform separate from audio-reactive layer motion.
- [x] Add Reset.
- [x] Add Fit Safe Area v1.
- [x] Add contextual Board action for layered GF themes.
- [x] Add unit coverage for transform bounds.
- [ ] Android CI PASS for v0.7.4.
- [ ] Phone verify live transform controls.
- [ ] Phone verify persistence after leaving/reopening Board editor.
- [ ] Next: advanced per-layer transform overrides for frame / creature / wordmark / FX.


## v0.7.5 — Gestures + extended audio motion

- [x] Keep existing frame / creature / wordmark / FX reactions unchanged.
- [x] Add one-finger X/Y drag in Board editor.
- [x] Add pinch-to-scale in Board editor.
- [x] Add two-finger rotation gesture.
- [x] Persist gesture edits through `BoardTransformStore`.
- [x] Add pass-through stereo PCM balance analyzer.
- [x] Extend live `SceneSignal` with `stereoPan`.
- [x] Add configurable whole-GF stereo left/right travel.
- [x] Add configurable whole-GF bass vertical float.
- [x] Add configurable whole-GF back/forth rotation sway.
- [x] Add all three motion controls to existing Board menu.
- [ ] Android CI PASS for v0.7.5.
- [ ] Phone verify gesture editing.
- [ ] Phone verify real stereo travel with stereo-heavy music.
- [ ] Tune defaults from phone video if needed.
- [ ] Later: add stereoPan to offline analysis for deterministic export parity.


## v0.7.6 — Rotation/lifecycle audit

- [x] Define one lifecycle/rotation contract for every current and future UI surface.
- [x] Restore Library after rotation.
- [x] Restore Now Playing after rotation.
- [x] Restore Playback Themes after rotation.
- [x] Restore Board Transform after rotation.
- [x] Restore Export Lab after rotation.
- [x] Preserve vertical scroll position.
- [x] Preserve horizontal scroll position.
- [x] Preserve selected theme.
- [x] Preserve export aspect ratio.
- [x] Keep Board transform/reaction values through their persistent stores.
- [x] Ensure rotation does not automatically trigger UI actions.
- [ ] Android CI PASS for v0.7.6.
- [ ] Phone lifecycle acceptance across all five MainActivity screens.
- [ ] Move Export Lab offline-analysis ownership to lifecycle-safe state holder.
- [ ] Explicit projectM rotation/background-work duplication audit.
- [ ] Add automated instrumentation rotation tests after UI structure stabilizes.


## v0.7.7 — Per-layer Board editor

- [x] Add persistent BoardLayerTransform model/store.
- [x] Add Frame transform overrides.
- [x] Add Creature/Shark transform overrides.
- [x] Add FARIC wordmark transform overrides.
- [x] Add FX transform overrides.
- [x] Preserve existing per-layer audio reactions.
- [x] Add layer selector in Board editor.
- [x] Route drag/pinch/twist gestures to selected layer.
- [x] Restore layer transforms in Now Playing.
- [x] Preserve selected editor layer across rotation.
- [x] Preserve selector/vertical scroll positions across rotation.
- [ ] Android CI PASS for v0.7.7.
- [ ] Phone verify independent layer transforms.
- [ ] Phone verify rotation while a non-group layer is selected.
- [ ] Next: expose per-layer audio-reaction tuning and/or lock/inherit controls after phone feedback.


## v0.7.8 — Compact Board panel

- [x] Limit Board settings panel to 40% of screen height.
- [x] Keep panel vertically scrollable.
- [x] Preserve live preview above the panel.
- [x] Make the 40% rule orientation-adaptive.
- [ ] Android CI PASS for v0.7.8.
- [ ] Phone verify portrait/landscape usability.


## v0.8.0 — Floating PulseDeck controls

- [x] Remove monolithic Now Playing player card.
- [x] Split metadata into its own floating block.
- [x] Split progress/seek into its own floating block.
- [x] Keep previous/play-next/repeat/shuffle as floating transport buttons.
- [x] Split theme/Board/projectM/export into a separate quick-actions block.
- [x] Remove Now Playing bottom navigation to reclaim scene space.
- [x] Add 6-second auto-hide.
- [x] Add double-tap show/hide on empty Board.
- [x] Preserve overlay hidden/visible state through rotation.
- [ ] Android CI PASS for v0.8.0.
- [ ] Phone verify all buttons remain reachable and readable.
- [ ] Phone verify double tap does not interfere with Board/visualizer gestures.
- [ ] Phone tune positions/spacing.
- [ ] Next: persistent per-block visibility / opacity / transform / skin settings.


## v0.8.1 — Concept-style main screen

- [x] Stop mockup iteration and implement the approved visual direction in real Android UI.
- [x] Keep Hero/GF as the swappable visual layer.
- [x] Add custom Canvas-drawn transport icon family.
- [x] Arrange transport as shuffle / previous / large play-pause / next / repeat.
- [x] Add translucent cyber-neon transport rail.
- [x] Add icon-only bottom action rail.
- [x] Add custom Theme / Board / Visualizer / Export icons.
- [x] Restore reactive waveform above progress.
- [x] Preserve auto-hide + double-tap visibility behavior.
- [x] Save approved concept reference under docs/design.
- [x] Android CI PASS for implementation commit before version bump.
- [ ] Android CI PASS for final v0.8.1 version bump.
- [ ] Phone visual acceptance against approved reference.
- [ ] Phone tune spacing/sizes/glow intensity after screenshot review.


## v0.8.2 — Reference-fidelity pass

- [x] Compare phone screenshot against approved main-screen reference.
- [x] Increase Hero/GF presentation scale without changing persisted Board transform.
- [x] Shift Hero presentation down slightly.
- [x] Add subtle cyan/orange Hero energy decoration.
- [x] Replace coarse waveform dots with 56 narrow bars.
- [x] Tighten metadata/progress/transport vertical stack.
- [x] Increase transport size hierarchy.
- [x] Increase lower action rail/icon sizes.
- [x] Add favorite and track-more controls.
- [ ] Android CI PASS for v0.8.2.
- [ ] Phone visual comparison with approved reference.
- [ ] Tune one more pass only from real phone screenshot if proportions still differ.


## v0.8.3 — Hero footprint correction

- [x] Normalize standalone Hero themes into the same main-screen visual slot.
- [x] Enlarge Neon Emblem specifically.
- [x] Enlarge Cyber Shark presentation without changing Board persistence.
- [x] Keep approved transport/action sizing from v0.8.2.
- [x] Re-align track favorite/more with metadata.
- [ ] Android CI PASS for v0.8.3.
- [ ] Phone compare both Cyber Shark and Neon Emblem against the same shell layout.


## v0.9.0 — Permanent HUD main skin

- [x] Separate permanent main-player skin from Cyber Shark / GF / visualizer content.
- [x] Implement code-only cyan/orange HUD background from supplied block references.
- [x] Add reactive F reactor with concentric rings and radial bars.
- [x] Add reactive cyan/orange energy ribbons and particles.
- [x] Use matching custom back/menu/favorite/track-more controls.
- [x] Match transport hierarchy to supplied 5-button block.
- [x] Upgrade transport/action rails to supplied HUD-panel language.
- [x] Keep lower action row icon-only.
- [x] Preserve auto-hide + double-tap + rotation visibility state.
- [x] Document the future layer stack (background content / skin / hero / chrome / foreground FX).
- [ ] Android CI PASS for final v0.9.0.
- [ ] Phone visual acceptance.
- [ ] Next: explicit layer-placement settings for existing visualizers/GF/video (behind/above skin).


## v0.10.0 — Modular PNG Skin Engine v1

- [x] Import modular PNG pack into GitHub.
- [x] Package repository skin directory as Android assets.
- [x] Parse `manifest.json` at runtime.
- [x] Render PNG layers by normalized x/y/width/z.
- [x] Swap play/pause PNG by playback state.
- [x] Route manifest actions to Android behavior.
- [x] Add PNG progress thumb and seek hit area.
- [x] Feed playback metadata/progress to the skin renderer.
- [x] Feed audio signal to manifest reactive layers.
- [x] Hide/show chrome independently from persistent background/reactor layers.
- [x] Preserve existing auto-hide/double-tap lifecycle contract.
- [ ] Android CI PASS for final v0.10.0.
- [ ] Phone visual acceptance against approved reference.
- [ ] Phone hit-target acceptance for all PNG buttons.
- [ ] Next: configurable layer placement for projectM/GF/video behind or above permanent skin.


## Resume point — v0.10.1 phone acceptance

- [x] v0.10.1 visual-compositing correction committed.
- [x] Android workflow PASS.
- [x] Validate workflow PASS.
- [ ] Update phone checkout from GitHub.
- [ ] Install latest v0.10.1 APK.
- [ ] Capture one fresh main-screen screenshot.
- [ ] Compare seams / reactor balance / vertical spacing / bottom empty area.
- [ ] Tune only manifest composition values first; avoid another architectural rewrite unless the screenshot proves it necessary.


## v0.11.0 — Reference-first master plate

- [x] Stop micro-compositing individual PNG rings/buttons at runtime.
- [x] Build a 20:9 reference-first master visual from the approved original artwork.
- [x] Prepare separate playing and paused master plates.
- [x] Add manifest-driven invisible hit zones for existing actions.
- [x] Add master-plate runtime mode with modular-skin fallback.
- [x] Disable auto-hide in master-plate mode for visual-baseline testing.
- [x] Add Termux menu item 19 for master-plate import.
- [ ] Import `PulseDeck_MasterPlate_v1.zip` on phone and push assets.
- [ ] Android CI PASS after asset import.
- [ ] Install and visually accept the master page.
- [ ] Only after acceptance: restore dynamic metadata/progress/reactivity one layer at a time without changing the accepted composition.


## v0.11.1 — phone center calibration

- [x] Preserve the user's good/wrong center assessment.
- [x] Add calibration page over the real PulseDeck renderer.
- [x] Add draggable drafting-style center target.
- [x] Save points by C-01…C-20 identity.
- [x] Auto-advance after saving.
- [x] Add previous/next navigation by horizontal swipe.
- [x] Add clipboard export of all saved coordinates.
- [x] Open calibration from the main-screen Menu button.
- [ ] Android CI PASS for v0.11.1 / build 45.
- [ ] User calibrates and returns coordinate block.
- [ ] Rebuild layout from accepted centers only.


## Resume point — v0.11.4 center calibration

- [x] Clean Blocks v2 visible on phone.
- [x] Preserve non-stretched uniform-scaling rule.
- [x] Preserve wrong/good C-point review states.
- [x] Add draggable center target.
- [x] Add floating draggable CENTER REMOTE.
- [x] Add 1/2/5/10/20 px step control.
- [x] Add PREV / SAVE / NEXT / RESET / COPY.
- [x] Persist panel position, step, current C-point and saved centers.
- [x] Validate PASS for v0.11.4.
- [ ] Android PASS for v0.11.4.
- [ ] Install v0.11.4 APK.
- [ ] Calibrate C-01…C-20 on phone.
- [ ] Paste exported normalized coordinates into chat.
- [ ] Rebuild final main-page geometry from accepted centers.
- [ ] After geometry acceptance, tune spacing/background behavior across aspect ratios without deforming the HUD.


## v0.11.6 — precision center calibration

- [x] Persist unsaved crosshair edit position per C-point.
- [x] Keep panel touches from changing the target.
- [x] Add live circular magnifier over the real skin.
- [x] Add 2x / 3x / 4x / 6x zoom.
- [x] Add S / M / L loupe diameter.
- [x] Add draggable loupe with persisted position.
- [x] Add FOLLOW mode.
- [x] Add FREEZE snapshot mode.
- [x] Preserve existing pixel-step remote controls.
- [ ] Validate PASS.
- [ ] Android PASS.
- [ ] Phone acceptance.
- [ ] Calibrate C-01…C-20 and paste COPY output.


## PulseDeck v0.13.0 — approved object centers + control scale pass

- [x] Save the phone-exported C-01…C-20 object template in `skin/pulsedeck_hud/object_templates/PulseDeck_object_template_centered_v1.json`.
- [x] Preserve exported dx/dy values verbatim; do not overwrite immutable `PULSEDECK_CENTER_CALIBRATION`.
- [x] Record the layout contract in `docs/design/PULSEDECK_OBJECT_LAYOUT_BASELINE.md`.
- [x] Scale C-17…C-20 quick-action icons to 150% around their accepted centers.
- [x] Scale Play to 170% around C-13.
- [x] Scale Pause to 170% height and effective 136% width around the same C-13 center.
- [x] Version candidate: v0.13.0 / build 64.
- [ ] Android workflow PASS for exact candidate HEAD.
- [ ] Phone visual acceptance of enlarged lower controls and Play/Pause.
- [ ] After phone PASS, freeze the scale pass as the approved visual baseline.


## PulseDeck v0.13.1 — S1-S6 grouped main page

- [x] Preserve approved phone-exported C-01…C-20 center template.
- [x] Map Object Constructor sections to S1…S6 from the approved cut map.
- [x] Add explicit GROUP / UNGROUP section editing.
- [x] Keep parent-child rail movement from double-applying child offsets.
- [x] Keep v0.13.0 icon scale contract.
- [x] Wire Repeat to real Media3 Repeat One behavior.
- [x] Confirm Play/Pause and Seek use real PlaybackController actions.
- [x] Confirm Theme / Board / Visualizer / Export route to existing product screens.
- [ ] Playback queue foundation before wiring Shuffle / Previous / Next.
- [ ] Favorite and Track More product behavior.
- [ ] Android workflow PASS for exact v0.13.1 candidate HEAD.
- [ ] Phone visual + GROUP/UNGROUP + action acceptance.


## PulseDeck v0.14.1 — seek thumb + frequency spectrum

- [x] Remove the moving progress-thumb PNG from runtime rendering.
- [x] Draw a clean Canvas seek thumb centered on the progress line.
- [x] Preserve the approved C-09 progress center offset.
- [x] Extend SceneSignal with live spectrum bins.
- [x] Derive 32 logarithmic FFT bands from approximately 60 Hz to 12 kHz.
- [x] Render each spectrum band from the bottom baseline upward, low frequencies left to high frequencies right.
- [x] Preserve approved C-08 waveform center offset.
- [ ] Exact-HEAD Android workflow PASS.
- [ ] Phone acceptance of seek thumb and frequency motion.


## PulseDeck v0.14.3 — phone visual corrections

- [x] Interpolate FFT bands that have no direct source bin, removing false holes in the mirrored spectrum.
- [x] Apply light three-band smoothing while preserving real spectral peaks.
- [x] Recenter play/pause inside S5 without moving approved C-13 or the transport rail.
- [x] Apply a small Play-only optical X correction; Pause stays geometrically centered.
- [x] Replace static progress-line PNG rendering with a Canvas gradient while preserving seek hit behavior.
- [x] Progress direction remains chronological left → right; visual color direction is cyan at track start → orange at track end.
- [ ] Exact-HEAD Android workflow PASS.
- [ ] Phone acceptance of spectrum continuity, Play centering and progress color direction.


## PulseDeck v0.14.4 — independent Play/Pause alignment

- [x] Restore the previously approved Pause transport-rail anchor; do not apply automatic optical correction to Pause.
- [x] Remove the runtime Play optical-offset guess.
- [x] Split C-13 editor state into C-13P Pause and C-13▶ Play while keeping the same S5 central rail/button.
- [x] Add SHOW PLAY / SHOW PAUSE in Object Constructor; switching also selects the matching editable position.
- [x] Persist, export and import Play/Pause offsets independently.
- [x] Migrate legacy C-13 coordinates as the initial value for both variants so existing alignment is not discarded.
- [ ] User centers C-13▶ Play on phone; C-13P Pause should already match the previously approved position.
- [ ] Exact-HEAD Android workflow PASS.


### C-13 phone-approved positions

- Pause remains independent: dx=-0.00277777761220932, dy=0.0025641026441007853.
- Play phone-approved from exported Object Constructor template: dx=0.0018518520519137383, dy=0.0025641026441007853.
- Do not reapply automatic optical compensation; runtime/editor must use the two saved positions independently.


## PulseDeck v0.15.0 — C-13 magnifier + real playback queue foundation

- [x] Expose independent renderer rects for the currently previewed Play/Pause icon so Object Constructor selection, axes, coordinates and magnifier target the actual icon.
- [x] Keep phone-approved Play/Pause offsets independent; no automatic optical compensation.
- [x] Replace single-file picker with Android multi-audio picker.
- [x] Add Media3 queue loading with URI + display-name metadata.
- [x] Wire Previous and Next to real Media3 queue navigation.
- [x] Wire Shuffle to Media3 shuffleModeEnabled.
- [x] Sync displayed track metadata when Media3 transitions between queue items.
- [x] Keep single-track load API as a one-item queue for compatibility.
- [ ] Phone verify C-13 magnifier and reported coordinates.
- [ ] Phone select 3+ tracks and verify Previous / Next / Shuffle.
- [ ] Exact-HEAD Android workflow PASS.


## PulseDeck v0.15.1 — approved main layout + live visualizers

- [x] Promote the phone-approved Object Constructor JSON to the main PulseDeck HUD asset.
- [x] Apply independent approved Play/Pause offsets when the main HUD loads.
- [x] Keep immutable PULSEDECK_CENTER_CALIBRATION unchanged; the approved file remains an object-offset layer.
- [x] Put ReactiveSceneView behind the modular PulseDeck HUD on Now Playing.
- [x] Make only the main HUD base canvas transparent so HUD assets remain above the live scene.
- [x] Reuse the existing SceneOrchestrator and SceneSignal pipeline.
- [x] Main page now supports the existing RADIAL / WAVE_RIBBON / SPECTRUM_BARS visualizers.
- [x] Existing Board action continues to shuffle the active scene on the main page.
- [ ] Exact-HEAD Android build/CI PASS.
- [ ] Phone verify approved object placement over live visualization.
- [ ] Phone verify Board cycles live scenes while audio is playing.


## PulseDeck v0.16.0 — canonical render layer stack

User-authorized implementation after layer-theory review.

- [x] Add explicit seven-slot PulseDeckLayerStack with stable Z-order 0..6.
- [x] Layer 0 = existing ReactiveScene visualizer renderer.
- [x] Layer 1 = independent BigEqualizerView driven by the shared SceneSignal.
- [x] Layer 2 = existing layered GF/HeroBoardView for Cyber Shark, preserving saved group/layer transforms and reactions.
- [x] Layer 3 = reserved GIF/Animation slot; no fake content added.
- [x] Layer 4 = independent transparent atmospheric FX renderer driven by the same SceneSpec/SceneSignal.
- [x] Layer 5 = approved PulseDeck HUD mounted unchanged as PULSEDECK_LOCKED.
- [x] Layer 6 = reserved Service Overlay slot for editor-only tools.
- [x] Split ReactiveSceneView render responsibilities so Layer 4 cannot paint an opaque background over Layers 1/2.
- [x] Keep PulseDeck object positions, approved offsets, scale and Z contract unchanged.
- [ ] Exact-HEAD Android workflow PASS.
- [ ] Phone verify visual Z-order and that locked PulseDeck geometry is unchanged.
- [ ] Phone verify Big EQ is above Visualizer and below GF/Effects/PulseDeck.
- [ ] Phone verify Cyber Shark GF remains editable with its existing frame/FX/creature/wordmark transforms.


## PulseDeck v0.16.1 — layer visibility controls

- [x] Add persistent visibility state for Layers 0–4.
- [x] Keep Layer 5 PulseDeck locked and forcibly visible.
- [x] Add PulseDeck tools → Шари / Layers.
- [x] Add presets: Visualizer only, Visualizer + Big EQ, Full composition.
- [x] Add manual toggles: Visualizer, Big Equalizer, GF, GIF/Animation, Effects.
- [x] Persist layer choices across Now Playing recreation/app restart.
- [x] Validate workflow PASS on implementation candidate.
- [x] Android workflow PASS on implementation candidate.
- [ ] Exact-HEAD CI after this documentation commit.
- [ ] Phone verify all three presets and manual toggles.
- [ ] Phone verify PulseDeck geometry remains unchanged and Layer 5 cannot be disabled.


## PulseDeck v0.16.3 — spring press feedback

- [x] Keep existing cyan/white pressed ring.
- [x] While a control is held: scale its rendered object to 80% of its approved baseline size.
- [x] On release: spring to 150% of baseline, then settle back to 100%.
- [x] Add a soft cyan neon backing glow behind the active control.
- [x] Preserve original hit zones and approved object coordinates.
- [x] Leaving the hit zone while still holding cancels back to 100% without the release overshoot.
- [x] Validate workflow PASS.
- [x] Android workflow PASS.
- [ ] Phone verify press/hold/release feel on transport, quick actions, Back and Menu.


## PulseDeck v0.17.0 — Layer 5 subsystem split + Photo Reactor

User-authorized Layer 5 refactor. Classic geometry remains available and unchanged.

- [x] Formalize internal PulseDeck Layer 5 subsystems:
  - 5.0 Atmosphere
  - 5.1 Reactor
  - 5.2 Track UI
  - 5.3 Transport
  - 5.4 Quick Actions
  - 5.5 Navigation
- [x] Keep Classic as the default Layer 5 upper composition.
- [x] Add Photo Reactor variant based on the user-provided 376926 image source.
- [x] In Photo Reactor mode, replace only 5.0 Atmosphere + 5.1 Reactor.
- [x] Skip classic hero_core/F only in Photo Reactor mode; Classic remains untouched.
- [x] Keep 5.2 Track UI, 5.3 Transport, 5.4 Quick Actions and 5.5 Navigation unchanged in both variants.
- [x] Add subtle music-reactive scale/glow to Photo Reactor without changing PulseDeck control geometry.
- [x] Add persistent tools selector: PulseDeck tools → Верх Layer 5 / Reactor → Classic / Photo Reactor · 376926.
- [x] Validate workflow PASS on implementation candidate.
- [x] Android workflow PASS on implementation candidate.
- [ ] Phone compare Classic vs Photo Reactor.
- [ ] Phone verify Photo Reactor crop/scale, lower Layer 5 alignment, and no F-core leakage.


## PulseDeck v0.18.0 — Over-visualization layer split

User-authorized correction: the 447504 artwork is not part of PulseDeck. It is a separate independently switchable layer directly above the base visualizer.

- [x] Expand canonical render stack from 0..6 to 0..7.
- [x] Layer 0 = Visualizer; independent persistent visibility.
- [x] Layer 1 = Надвізуалізація / Over-visualization; independent persistent visibility.
- [x] Layer 1 uses user artwork 447504 as a full-screen center-crop overlay with 5% overscan per edge.
- [x] Store the active artwork as `skin/pulsedeck_hud/over_visualization/over_visualization_447504.webp` and load it through the repository-level `skin/` Android assets source.
- [x] Shift Big Equalizer → Layer 2, GF → Layer 3, GIF → Layer 4, Effects → Layer 5, PulseDeck 🔒 → Layer 6, Service Overlay → Layer 7.
- [x] Renumber PulseDeck internal subsystem codes from 5.x to 6.x without moving approved PulseDeck geometry.
- [x] Retire the old Photo Reactor variant/path from active PulseDeck rendering.
- [x] Update persistent layer keys/compatibility mapping and presets for the new stack.
- [x] Add preset: Visualizer + Надвізуалізація.
- [x] Keep PulseDeck Layer 6 forcibly visible and not user-toggleable.
- [x] Update the canonical layer architecture document to the implemented 0..7 contract.
- [ ] Exact-HEAD Android workflow PASS for v0.18.0.
- [ ] Phone verify Layer 0 can be toggled independently of Layer 1.
- [ ] Phone verify Layer 1 can be toggled independently of Layer 0.
- [ ] Phone verify 447504 full-screen crop/overscan on the target device and no exposed image edges.
- [ ] Phone verify Big EQ / GF / GIF / Effects / PulseDeck Z-order after the +1 shift.
- [ ] Phone verify locked PulseDeck geometry remains unchanged.

## PulseDeck v0.18.1 — 447504 is the actual Layer 1 image

Correction after user review:
- [x] Keep Layer 0 = Visualizer and Layer 1 = Надвізуалізація as independent toggleable layers.
- [x] Layer 1 content is the complete 447504 artwork, not a PulseDeck fragment and not an effects-only blend.
- [x] Render 447504 as a normal full-screen image with alpha 255; remove SCREEN blend/transparency.
- [x] Preserve center-crop with 5% overscan per edge.
- [x] Keep all higher layers unchanged: 2 Big Equalizer, 3 GF, 4 GIF, 5 Effects, 6 PulseDeck 🔒, 7 Service.
- [ ] Exact-HEAD Android workflow PASS for v0.18.1.
- [ ] Phone verify: with Layer 1 ON, the complete 447504 image is visible full-screen.
- [ ] Phone verify: Layer 1 OFF reveals Layer 0 Visualizer independently.
- [ ] Phone verify: Layer 0 OFF + Layer 1 ON still shows the complete 447504 image.

## PulseDeck v0.18.2 — hierarchical layer/object controls

User finding: the Layers panel behaved inconsistently and some layers appeared to jump or cover other layers.

Root causes found:
- [x] The old AlertDialog tried to use `setSingleChoiceItems()` and `setMultiChoiceItems()` in the same list; the second list configuration replaced the first and preset selection dismissed/reopened the dialog, causing visible jumping.
- [x] Layer 3 GF rendered an opaque full-screen black background, so enabling GF could hide Layers 0–2 and make layer toggles look broken.
- [x] PulseDeck child controls use parent rail rectangles for local coordinates; object hiding must preserve parent geometry or children can jump.

Implementation:
- [x] Replace the old layer list/preset dialog with one stable custom scrollable hierarchy. No dismiss/reopen on toggle.
- [x] Show every canonical Layer 0–7 in the control panel.
- [x] Keep Layer 6 PulseDeck container locked/visible, but expose its internal subsystems 6.0–6.5.
- [x] Expose individual PulseDeck manifest objects plus dynamic HUD objects for independent ON/OFF control.
- [x] Expose Layer 3 GF objects: background/glow, frame, FX, creature, wordmark.
- [x] Persist every object visibility state in `pulsedeck_layers`.
- [x] Restore persisted GF and PulseDeck object visibility when Now Playing is recreated.
- [x] Keep hidden PulseDeck parent geometry resolved so child objects do not shift when a parent rail is hidden.
- [x] Remove the opaque full-screen fill from Layer 3 GF; retain only transparent/local glow/HUD decoration.
- [x] Remove obsolete preset-dialog code from the active UI path.
- [x] Bump candidate to v0.18.2 / versionCode 81.
- [ ] Exact-HEAD Validate PASS for v0.18.2.
- [ ] Exact-HEAD Android build PASS for v0.18.2.
- [ ] Phone: Layers panel opens once and stays stable while toggling many entries.
- [ ] Phone: Layer 0–5 parent toggles work without unexpected redraw/jump.
- [ ] Phone: GF object toggles work independently and no longer hide lower layers with a black fullscreen fill.
- [ ] Phone: PulseDeck groups 6.0–6.5 and every listed object toggle independently.
- [ ] Phone: hiding transport/quick-action rails does not move their still-enabled child icons.
- [ ] Phone: rotation/app recreation preserves parent-layer and object visibility state.

## PulseDeck v0.18.3 — draggable compact layers panel

User finding from phone screenshot: the layers/object panel covered almost the whole composition, making it impossible to see what each toggle changed.

- [x] Replace the tall near-fullscreen control surface with a floating panel occupying ~30% of the physical screen height.
- [x] Keep a wide readable panel width so layer/object names do not become unusably narrow.
- [x] Remove background dimming.
- [x] Make the panel draggable by its header across the screen.
- [x] Allow touches outside the panel to reach the underlying Now Playing screen.
- [x] Persist the panel X/Y position across reopen/recreation.
- [x] Add a reset-to-center control in the panel header.
- [x] Keep layer/object toggles live; the panel no longer closes or rebuilds on each toggle.
- [x] Compact labels and spacing so more controls fit in the 30%-height viewport.
- [x] Bump candidate to v0.18.3 / versionCode 82.
- [ ] Exact-HEAD Validate PASS.
- [ ] Exact-HEAD Android workflow PASS.
- [ ] Phone: drag the panel to top/middle/bottom and verify it remains inside screen bounds.
- [ ] Phone: toggle layers/objects while the underlying composition remains visible.
- [ ] Phone: close/reopen panel and verify saved position is restored.

## PulseDeck v0.18.4 — Layer 0 projectM + GF background transforms

Phone findings after v0.18.3:
- GF `background / glow` could be toggled but had no independent position/scale/rotation controls.
- Selecting a projectM visualizer in the Visualizer screen did not return that selected preset to the main PulseDeck Layer 0.
- GF was still coupled to the mutually-exclusive playback-theme selection, which conflicts with the new independent layer architecture.

Implementation:
- [x] Add `BoardLayerId.BACKGROUND` and persist the same transform model as other GF parts: X/Y, scale, rotation, opacity.
- [x] Apply Background/Glow transform to the actual Layer 3 procedural glow/arcs/particles renderer.
- [x] Add `BG/Glow` to Board Transform selector.
- [x] Add quick controls for every GF object: Center, 100%, Reset.
- [x] Add a direct `GF / Background · позиція / масштаб` entry from the Layers panel.
- [x] Keep all current production GF objects wired into Layer 3: Background/Glow, Frame, FX, Creature, Wordmark.
- [x] Decouple the current production GF set (Cyber Shark) from the selected playback theme so Layer 3 can coexist with Layer 0.
- [x] Layer 0 is now a composite container with two independently toggleable objects:
  - projectM selected preset background;
  - FARIC reactive visualizer foreground.
- [x] Return the last selected projectM preset from the Visualizer screen to main PulseDeck Layer 0.
- [x] Remove the old `selectedThemeId == VISUALIZER` gate from the FARIC SceneOrchestrator so Layer 0 keeps reacting while GF is enabled.
- [x] Add layer/object configuration export to JSON, including GF transforms/reaction and projectM preset id.
- [x] Bump candidate to v0.18.4 / versionCode 83.
- [x] Exact-HEAD Validate PASS — run #652 on `b81e81743b5f`.
- [x] Exact-HEAD Android workflow PASS — run #387 on `b81e81743b5f`; unit tests, APK build, signer/zipalign verification and artifact upload all PASS.
- [ ] Phone: choose a projectM preset, Back, verify it appears in Layer 0 on the main page.
- [ ] Phone: Layer 0 projectM and FARIC reactive subobjects toggle independently.
- [ ] Phone: GF and Layer 0 run together; selecting/editing GF no longer disables the visualizer.
- [ ] Phone: BG/Glow X/Y, zoom, rotation and opacity all change only GF background/glow.
- [ ] Phone: export Layers JSON and verify file is created.

## PulseDeck v0.18.5 — live GF editor composition + export folders

Phone findings:
- Layer 3 parent could be OFF while its saved child object checkboxes still looked active, which made the panel state visually misleading.
- Entering Board Transform / BG-Glow editing destroyed the lower visualization preview; GF had to be positioned against the real Layer 0/1/2 composition instead.
- Export Lab saved PNG to Pictures/FARIC and MP4+AAC to Movies/FARIC but provided no direct way to open those folders.

Implementation:
- [x] Nested object controls now follow their parent layer visually: when L0/L3/etc. is OFF, its child controls are disabled/dimmed while preserving saved child ON/OFF values.
- [x] Refactor Layer 0 construction into a reusable composition builder.
- [x] Board Transform now previews the real lower stack: Layer 0 Visualizer, Layer 1 Over-visualization and Layer 2 Big Equalizer with their persisted visibility.
- [x] Board Transform forces only Layer 3 container visible for editing while preserving each GF child object's saved visibility.
- [x] projectM and FARIC reactive visualizer continue rendering while Screen.BOARD_TRANSFORM is active.
- [x] Remove root safe-area padding from Board Transform preview so GF↔Visualizer registration matches Now Playing full viewport.
- [x] Add Export Lab button "Відкрити папку експорту" with Pictures/FARIC and Movies/FARIC choices.
- [x] Use Android DocumentsProvider folder URI with document-tree fallback.
- [x] Bump candidate to v0.18.5 / versionCode 84.
- [ ] Exact-HEAD Validate PASS.
- [ ] Exact-HEAD Android workflow PASS.
- [ ] Phone: L3 OFF dims GF child controls; L3 ON re-enables them without losing child states.
- [ ] Phone: Board → BG/Glow keeps Layer 0 visualization visible and aligned exactly as on Now Playing.
- [ ] Phone: projectM keeps animating and GF remains music-reactive while editing.
- [ ] Phone: Export Lab → Відкрити папку експорту opens Pictures/FARIC and Movies/FARIC.

## PulseDeck v0.18.6 — GF export proof + compact control UX

Phone feedback after v0.18.5:
- the full-width GF Background/Glow settings button consumed too much room inside the 30%-height layer panel;
- the export-folder chooser looked like a raw system dialog rather than PulseDeck UI;
- Cyber Shark / Layer 3 GF could be configured reactively but Export Lab still reported the active scene as unsupported.

Implementation:
- [x] Move GF Background/Glow settings into a compact side gear button on the L3 row; remove the large nested settings pill.
- [x] Replace the raw export-folder AlertDialog with a styled PulseDeck dialog: dark rounded panel, PNG / video destination buttons, close control, explicit Cancel; Android Back/outside touch still dismisses it.
- [x] Add deterministic Cyber Shark export renderer using the real modular frame / creature / wordmark resources.
- [x] Reuse current saved GF group transform, group reaction, per-object transforms and per-object visibility during proof rendering.
- [x] Make Cyber Shark PNG proof music-reactive from the current/offline signal.
- [x] Make Cyber Shark 3 s MP4 proof music-reactive from offline analysis and keep AAC audio muxed into H.264 MP4.
- [x] Export source now follows the active Layer 3 GF when L3 is enabled, even if Theme Picker currently points at another/unsupported theme.
- [x] Export Lab identifies that source as `Layer 3 GF · Cyber Shark`.
- [x] Bump candidate to v0.18.6 / versionCode 85.
- [ ] Exact-HEAD Validate PASS.
- [ ] Exact-HEAD Android workflow PASS.
- [ ] Phone: L3 row gear opens BG/Glow editor without wasting vertical panel space.
- [ ] Phone: export-folder chooser matches PulseDeck styling; PNG and Movies buttons open expected folders; Cancel/Back dismiss.
- [ ] Phone: with L3 GF enabled, PNG proof button is enabled and exports current Cyber Shark pose/visibility.
- [ ] Phone: after offline analysis, MP4 proof button is enabled and exports a 3 s reactive Cyber Shark clip with audio.
- [ ] Compare proof against live GF: object positions/scales/visibility should match closely. Full Layer 0/1/2/6 stack export remains a later composition-renderer step.

## PulseDeck v0.18.7 — deterministic composition export v1

Goal: export the same active PulseDeck composition instead of a standalone GF proof.

Implemented:
- [x] Add deterministic FARIC Layer 0 renderer for RADIAL / WAVE_RIBBON / SPECTRUM_BARS using explicit offline time + signal.
- [x] Add deterministic Layer 1 renderer for 447504 with the same center-crop + 5% per-edge overscan contract.
- [x] Add deterministic Layer 2 Big Equalizer renderer.
- [x] Reuse Cyber Shark deterministic Layer 3 renderer with current GF group transform/reaction, per-object transforms and visibility.
- [x] Render Layer 5 atmospheric FX using the current SceneSpec + offline signal.
- [x] Render Layer 6 PulseDeck HUD from the modular skin, applying all 29 persisted object visibility switches plus track/progress/audio state.
- [x] Preserve canonical Z-order in export: L0 → L1 → L2 → L3 → L5 → L6. L4 is still empty.
- [x] PNG and 3 s MP4 now use the same CompositionExportRenderer.
- [x] Export Lab shows the active layer summary and no longer depends on the old theme whitelist.
- [x] Export filenames use `FARIC-Composition-...`.
- [x] MP4 keeps offline signal analysis + H.264 + AAC audio.
- [x] Explicitly flag the remaining gap: native projectM GLSurfaceView is not yet available in deterministic offline composition export.
- [x] Use a main-looper GestureDetector handler so the offscreen PulseDeck HUD renderer can be safely constructed for export work.
- [x] Bump candidate to v0.18.7 / versionCode 86.
- [ ] Exact-head Validate PASS.
- [ ] Exact-head Android workflow PASS.
- [ ] Phone: export PNG with L1/L2/L3/L5/L6 combinations and compare against live composition.
- [ ] Phone: export 3 s MP4 and verify reactive FARIC visualizer + EQ + GF + FX + HUD.
- [ ] Phone: verify disabled PulseDeck child objects stay absent in exported PNG/MP4.
- [ ] Phone: verify 9:16 / 16:9 / 1:1 / 4:5 composition geometry.
- [ ] Next: native GL projectM frame capture/offscreen export so L0 projectM can join the deterministic stack.

## PulseDeck v0.18.8 — high-quality composition proof

- [x] Raise proof export from 540×960 / 15 FPS to 1080p-class / 30 FPS.
- [x] Raise H.264 bitrate floor for neon/high-detail content.
- [x] Validate #689 — PASS.
- [x] Android #416 — PASS.
- [x] Phone MP4 quality check — PASS: 1080×1920, 30 FPS, ~12.4 Mbps H.264 + AAC.

## PulseDeck v0.18.9 — projectM GL snapshot bridge

Goal: close the largest remaining visual gap in composition export without pretending projectM is already deterministic frame-by-frame.

Implemented:
- [x] Capture the live projectM framebuffer from the real GLSurfaceView with `glReadPixels`.
- [x] Flip the OpenGL framebuffer vertically and convert RGBA to Android Bitmap.
- [x] Capture projectM before opening Export when L0/projectM is visible.
- [x] Composite the captured projectM frame at the bottom of Layer 0.
- [x] Center-crop the captured framebuffer to 9:16 / 16:9 / 1:1 / 4:5.
- [x] Keep FARIC reactive, L2, L3, L5 and L6 animated over the captured projectM snapshot.
- [x] Avoid prematurely recycling an export snapshot still referenced by background rendering.
- [x] Candidate: v0.18.9 / versionCode 88.
- [x] Validate #694 — PASS on `0f8dd2445970`.
- [x] Android #421 — PASS on `0f8dd2445970`.
- [x] Phone MP4: real projectM image is present in Layer 0 under GF/HUD.
- [x] Phone MP4: projectM remains static as designed while GF/HUD/reactive layers change over 3 seconds.
- [x] Phone MP4: high-quality path remains 1080×1920 / 30 FPS / H.264 ~12.4 Mbps + AAC.
- [ ] Phone PNG: verify captured projectM frame matches the exact live moment.
- [ ] Next: true frame-by-frame/offscreen projectM rendering for animated projectM MP4.

## PulseDeck v0.19.0 — dynamic projectM MP4 capture

Goal: replace the static projectM frame in MP4 with a real sequence of live OpenGL frames.

Implementation:
- [x] Add blocking framebuffer sampling to `ProjectMView`; GL reads stay on the real GLSurfaceView render thread.
- [x] Add ordered/blocking projectM teardown before switching between Now Playing and Export GL surfaces.
- [x] Keep an export-only live projectM GLSurfaceView running behind Export Lab when L0/projectM is enabled.
- [x] Export Lab reports `L0 projectM live` when the live capture surface is available.
- [x] Before MP4 encoding, capture a projectM sequence at 30 FPS for the requested proof duration.
- [x] Pace capture in real time so the projectM internal clock advances naturally rather than being sampled as fast as the CPU can encode.
- [x] Store intermediate projectM frames as JPEG quality 95 cache files to avoid holding ~90 full-resolution Bitmaps in RAM.
- [x] Feed one captured projectM frame into each `CompositionExportRenderer` video frame; static GL snapshot remains fallback.
- [x] Delete temporary projectM capture files after success or failure.
- [x] Preserve the existing 1080p-class / 30 FPS / H.264 high-quality composition path with AAC audio.
- [x] Candidate: v0.19.0 / versionCode 89.
- [ ] Exact-head Validate PASS.
- [ ] Exact-head Android PASS.
- [ ] Phone: Export Lab shows `projectM live`.
- [ ] Phone: 3 s MP4 shows projectM visibly changing frame-to-frame under GF/HUD.
- [ ] Phone: audio and projectM motion feel synchronized around the export start position.
- [ ] Phone: no OOM, ANR, black projectM frames or GL teardown race when entering/leaving Export repeatedly.
- [ ] Compare 9:16 quality against v0.18.9 static-snapshot MP4.
- [ ] After phone PASS: consider a true fully-offscreen deterministic projectM renderer; v0.19.0 deliberately uses live GL capture because libprojectM currently advances on its own realtime clock.

## PulseDeck v0.19.1 — offline full-song timeline export

Goal: export the song from 0:00 to the end independently of the live player, with projectM and all deterministic layers driven by the same offline timeline.

Architecture:
- [x] Keep projectM pinned to stable libprojectM 4.1.7.
- [x] Backport only the 4.2-style external frame-time API into 4.1.7 through a build-time compatibility patch.
- [x] projectM export uses explicit `frameIndex / fps` time instead of wall-clock time.
- [x] FARIC native foreground compositor uses the same explicit export time and export-frame delta.
- [x] Suppress live-player PCM/signal while the manual projectM export engine is active.
- [x] Decode the source audio into a reusable mono 16-bit PCM cache for projectM.
- [x] Feed each projectM export frame only the PCM window and offline signal belonging to that song interval.
- [x] Reset/recreate projectM before every export so the same preset starts from a clean 0.0-second export timeline.
- [x] Keep the Export Lab projectM surface in manual/WHEN_DIRTY mode; no realtime draw loop is required during offline rendering.
- [x] Generalize the MP4 encoder from fixed 3 seconds to a requested duration and FPS.
- [x] Keep the existing 3-second button as a fast preview.
- [x] Add `Експортувати всю пісню MP4`: start=0, duration=full source duration.
- [x] Full-song export is independent of the main player's current position and play/pause state.
- [x] Mux the original source audio range into AAC after video rendering; do not record speaker/system playback.
- [x] Add progress dialog, keep-screen-on and explicit Cancel.
- [x] Make PCM preparation and video rendering cancellable.
- [x] Reuse 1080p-class / 30 FPS high-quality H.264 composition path.
- [x] Document stable projectM 4.2 migration in `PROJECTM_4_2_MIGRATION.md`.
- [x] Repair and apply the narrow frame-time backport using `git apply --unidiff-zero`.
- [x] Validate #735 — PASS on `9123cbfbf61d`.
- [x] Android #456 — PASS on `9123cbfbf61d` (unit tests, native/CMake build, APK, signer/zipalign verification and artifact upload all PASS).
- [x] Candidate bumped to v0.19.1 / versionCode 90.
- [x] Exact-head Validate PASS for v0.19.1 — Validate #738 PASS on docs HEAD `387401c29190b3553c823e1c72b17c32d41f98a4`.
- [x] Exact-head Android PASS for v0.19.1 app/source — Android #457 PASS on release SHA `fd4bf265436507d7126d390fc56824dca7e21e81`; later commits are documentation-only. Artifact: `FARIC-Music-Visualizer-v0.19.1-Debug`.
- [ ] Phone: 3-second preview works with projectM offline clock and does not wait for realtime capture.
- [ ] Phone: full-song export starts at 0:00 even when the player is paused or currently near the middle/end.
- [ ] Phone: exported MP4 duration matches source duration and audio begins at 0:00.
- [ ] Phone: projectM reacts throughout the song and higher layers stay synchronized with the same timeline.
- [ ] Phone: Cancel stops a long export without leaving a corrupt published MP4 or stale cache.
- [ ] Phone: no black projectM frames, OOM or ANR on a several-minute song.
- [ ] Performance follow-up: replace CPU Bitmap→ARGB→YUV path with a MediaCodec input Surface/EGL path if long-song export is too slow.

projectM 4.2 policy:
- do not move FARIC to an unreleased/moving projectM branch;
- keep 4.1.7 + the narrow frame-time backport until an official stable >=4.2.0 is published and passes Android/arm64 regression;
- migration/rollback checklist lives in `PROJECTM_4_2_MIGRATION.md`.


## PulseDeck v0.19.2 — Fast full-song export pipeline

Goal: preserve the deterministic v0.19.1 offline timeline while removing the largest CPU/GC bottlenecks from full-song export.

Implementation:
- [x] Replace byte-buffer YUV encoder input with MediaCodec input Surface.
- [x] Add EGL bridge that uploads the composited ARGB Bitmap directly to the H.264 encoder Surface.
- [x] Submit explicit frame presentation timestamps through `eglPresentationTimeANDROID`.
- [x] Remove per-frame `Bitmap.getPixels -> IntArray -> Kotlin ARGB->YUV420` conversion.
- [x] Reuse projectM offline RGBA direct readback buffer.
- [x] Reuse projectM offline pixel IntArray.
- [x] Reuse one projectM offline readback Bitmap across export frames.
- [x] Validate #749 — PASS on combined optimization source `09b0346310ef2164e0c565fac0866f2775d717ce`.
- [x] Android #461 — PASS on the same source commit.
- [x] Promote candidate to v0.19.2 / versionCode 91.
- [x] Exact-head Validate PASS for v0.19.2 documentation HEAD — Validate #753 PASS on `3e12cc87ed5e0d3ffa52eb13637660c70557b698`.
- [x] Android PASS for v0.19.2 app/source — Android #462 PASS on `bcab5fc8a1910581627fc82737ab0528e976b702`; later HEAD changes are documentation-only.
- [ ] Phone: 3-second preview orientation/colors/audio sync remain correct.
- [ ] Phone: compare full-song wall-clock export time against v0.19.1 on the same track.
- [ ] Phone: cancel leaves no corrupt published MP4.
- [ ] Phone: several-minute export completes without OOM/ANR/black projectM frames.
- [ ] Measure remaining bottleneck; synchronous projectM `glFinish + glReadPixels + RGBA->ARGB` is the expected next target if export remains too slow.


### Preview behavior contract

- The 3-second MP4 preview MUST start at the current playback scrubber/player position.
- It MUST render the next 3 seconds from that position (clamped only by source end).
- Do not change the preview to always start at 0:00.
- Full-song export remains independent and MUST start at 0:00.


## PulseDeck v0.19.3 — projectM raw readback acceleration

Goal: reduce the remaining projectM bottleneck seen on-phone after v0.19.2 while preserving all export behavior.

Contract:
- 3-second preview starts at the current playback scrubber position and renders the next 3 seconds.
- Full-song export starts at 0:00 and is independent from live player position/state.

Implementation:
- [x] Remove dynamic offline projectM Kotlin per-pixel RGBA->ARGB conversion loop.
- [x] Remove explicit `glFinish()` from dynamic offline readback hot path.
- [x] Copy reusable GL RGBA buffer directly into reusable Bitmap.
- [x] Correct raw GL vertical orientation during composition.
- [x] Correct R/B channel layout during composition.
- [x] Keep EGL -> MediaCodec Surface output introduced in v0.19.2.
- [x] Pre-release Validate #759 — PASS on `73cd730ea9394dd550d3dd87d2de133dc0e7465e`.
- [x] Pre-release Android #464 — PASS on the same source.
- [x] Promote to v0.19.3 / build 92.
- [x] Exact release Android PASS for v0.19.3 / build 92 — Android #465 PASS on `7add461579d259150f52e860d14ad6df226e2793`.
- [x] Validate PASS for v0.19.3 documentation — #763 PASS on `5e653e9baf8b90959eb5016c73efd5f9977c8386`; later checkpoint docs only.
- [ ] Phone: 3-second preview starts from current scrubber position.
- [ ] Phone: orientation/colors remain correct.
- [x] Phone: 3-second preview wall-clock improved from ~28 s to ~16 s on v0.19.3 (about 43% faster), but is still ~5.3x slower than realtime.
- [ ] Phone: verify audio sync.
- [ ] Phone: if preview speed is acceptable, repeat full-song test.


## PulseDeck v0.19.4 — projectM export-geometry optimization

Goal: reduce projectM offline work by rendering only the pixels needed for the selected output aspect ratio.

Contract:
- 3-second preview still starts from the current playback scrubber position.
- Final video remains 30 FPS.
- Full-song export remains 0:00 -> end.
- No intentional quality reduction.

Implementation:
- [x] Add fixed manual export geometry support to `ProjectMView`.
- [x] Export Lab projectM surface now uses the selected output dimensions/aspect ratio instead of the full phone-screen geometry.
- [x] Keep the existing projectM performance profile scale after applying output geometry.
- [x] Promote to v0.19.4 / build 93.
- [x] Android PASS for exact v0.19.4 source — #469 attempt 3 PASS on `c43f8f9b10d3a8c6969d3da8514a3509da350e55`.
- [x] Validate #776 attempt 3 — PASS on `5cb4b1d36d42ac5f99d6b5e3efbe4182ac4e011b`.
- [ ] Phone: same 3-second preview starts at current scrubber position.
- [ ] Phone: image composition/crop/orientation/colors remain correct.
- [x] Phone warm-cache retest: v0.19.4 render remains ~15.5-16 s for 3 s output, effectively unchanged from the v0.19.3 ~16 s baseline. Geometry optimization is visually correct but did not materially improve wall-clock speed.
- [ ] If still too slow, move projectM readback off the synchronous per-frame CPU path rather than lowering final 30 FPS.


### v0.19.5 phone QA / v0.19.6 next
- v0.19.5 output: PASS for 1080x1920, 30 FPS, 90 frames, 3.008 s and visual orientation/colors/composition.
- Exact timing values were hidden because Android truncated the multi-line Toast.
- v0.19.6 changes timing results to a persistent dialog.
- Next: install v0.19.6 after Android PASS and rerun one 3-second preview.


### v0.19.6 measured bottleneck
- Phone timing dialog: projectM 1896 ms; composition 11808 ms; encoder 435 ms; audio 424 ms; mux 112 ms; save 215 ms; total 15575 ms.
- Composition is ~75.8% of total and is now the clear dominant bottleneck.
- v0.19.7 / build 96 instruments composition internally: clear, projectM draw, FARIC reactive, overlay, big EQ, Cyber Shark, effects, HUD update, HUD draw.
- Next phone action after Android PASS: one 3-second preview and send the result dialog.


### v0.19.7 composition-layer profile
- Phone evidence: `447926.mp4` + `447927.mp4`.
- Total: 17008 ms; composition: 12998 ms.
- Composition layers: clear 85; projectM draw 6471; reactive 0; overlay 0; Big EQ 0; Cyber Shark 4691; effects 450; HUD update 26; HUD draw 1270 ms.
- Dominant costs: projectM draw first, Cyber Shark second.
- v0.19.8 / build 97 adds a safe GL_EXT_read_format_bgra fast path. Compatible GPUs bypass the per-frame R/B ColorMatrix; unsupported GPUs keep the established RGBA + ColorMatrix fallback.
- Result dialog reports `projectM BGRA: yes` or `fallback`.
- Next phone test after Android PASS: one warm-cache 3-second preview; compare projectM draw, composition, and total against v0.19.7.


### v0.19.8 phone result / v0.19.9 next
- v0.19.8 phone evidence: `447934.mp4` + `447935.mp4`.
- BGRA fast path active: `projectM BGRA: yes`.
- v0.19.8 exact timing: projectM 5318; composition 8077; encoder 450; audio 396; mux 93; save 124; total 15118 ms.
- Composition layers: clear 77; projectM draw 1643; Cyber Shark 4676; effects 428; HUD update 25; HUD draw 1222 ms.
- Compared with v0.19.7: projectM draw -74.6%, composition -37.9%, total -11.1%; colors/orientation remain correct.
- v0.19.9 / build 98 optimizes Cyber Shark by bypassing full-screen saveLayerAlpha when effective opacity is 100%, preserving the previous path for partial opacity.
- Next: wait for Android #485 PASS, checkpoint it, then phone test one 3-second preview and compare Cyber Shark/composition/total against v0.19.8.


### v0.19.9 phone result / v0.19.10 next
- v0.19.9 phone timing: projectM 5005; composition 6860; encoder 539; audio 498; mux 103; save 240; total 14031 ms.
- Composition layers: clear 58; projectM draw 1640; Cyber Shark 3523; effects 432; HUD update 24; HUD draw 1176 ms.
- Compared with v0.19.8: Cyber Shark 4676 -> 3523 ms (~24.7% faster); composition 8077 -> 6860 ms (~15.1% faster); total 15118 -> 14031 ms (~7.2% faster).
- Largest remaining stage is projectM generation/readback (~5.0 s).
- v0.19.10 / build 99 splits projectM provider cost into queue wait, native render, glReadPixels, and Bitmap.copyPixelsFromBuffer.
- Next phone test after Android PASS: one warm-cache 3-second preview and send the timing dialog.


### v0.19.10 result / v0.19.11 UX utility
- v0.19.10 confirms glReadPixels is the dominant projectM cost: 4361 ms of 5270 ms projectM time.
- v0.19.11 / build 100 adds a persistent `Копіювати текст` button to the export timing dialog.
- Copy action places the complete timing text on the Android clipboard and keeps the dialog open; `OK` remains the explicit close action.
- No performance/export rendering behavior changes in v0.19.11.
- Next phone QA after Android PASS: open a 3-second result, tap `Копіювати текст`, paste the copied block into chat.
