# Active Plan

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
