# Visualizer Roadmap TODO — layered FARIC / projectM

This file is the working implementation queue for the current visualizer direction.

## Product contract

- Layer 2 = projectM background library.
- Layer 1 = FARIC foreground sample library.
- Foreground samples are append-only: existing samples stay available when new samples are added.
- Background ratings never delete the original `.milk` file.
- Manual NEXT disables automatic background switching.
- Pressing TOP or ALL explicitly enables automatic background switching again.

## v0.5.2 — Reactive FG + fast preset startup

### Foreground motion
- [x] Increase foreground movement amplitude substantially.
- [x] Make bass the strongest scale/expansion driver.
- [x] Keep very fast bass/beat attack with smoother release.
- [x] Increase ray/ring travel range without clipping the full scene.
- [x] Add edge solar-flare energy tongues.
- [x] Make flare motion react to bass + beat and texture/detail react to highs.
- [x] Tune flare strength separately for Pulse Rays / Orbit Rings / Spectrum Halo.

### projectM startup / next-preset latency
- [x] Stop scanning thousands of preset files on the GL thread when opening projectM.
- [x] Build/cache a Kotlin preset catalog outside the render thread.
- [x] Open immediately with the last valid preset when possible.
- [x] Maintain CURRENT + 3 NEXT preset queue.
- [x] Prefetch the next 3 preset files into memory/page cache off the GL thread.
- [x] NEXT consumes one prepared item and immediately replenishes the queue.
- [x] TOP / ALL rebuild the queue from their own filtered source pool.
- [x] Tap on visualizer must use the same manual NEXT path, never bypass queue state.
- [ ] Preserve current track position while switching/rebuilding visualizer state.

### Persistent visualizer state
- [x] Remember last background mode: TOP / ALL.
- [x] Remember AUTO vs MANUAL state.
- [x] Remember last background preset.
- [x] Remember selected FARIC foreground sample.
- [x] Restore state without blocking first render.

## v0.5.3 — preset ratings and filtering

### Rating model
- [x] Add four states: NONE / UP / DOWN / HIDDEN.
- [x] Add UI actions: 👍 / 👎 / −.
- [x] 👍 increases future selection preference.
- [x] 👎 keeps preset available but lowers future selection preference.
- [x] − hides preset from active pools without deleting the file.
- [x] Hidden preset immediately leaves the active queue.
- [x] Ratings apply to the same preset across TOP and ALL modes.
- [x] Persist all ratings across app restarts/upgrades.

### Selection policy
- [x] HIDDEN weight = 0.
- [x] UP presets get highest random-selection weight.
- [x] NONE presets remain normal.
- [x] DOWN presets remain selectable at low weight.
- [x] Avoid immediate repeats.
- [x] Keep a short recently-played history to reduce repetition.

### Management UI
- [x] Show current preset name/id.
- [x] Show current rating state.
- [ ] Add hidden-presets management/recovery screen later.
- [ ] Add liked-presets-only background mode later.

## v0.5.4+ — foreground library

- [ ] Add explicit foreground sample picker instead of only cycling FG.
- [ ] Keep Pulse Rays.
- [ ] Keep Orbit Rings.
- [ ] Keep Spectrum Halo.
- [ ] Add Solar Crown.
- [ ] Add Plasma Wings.
- [ ] Add Bass Shockwave.
- [ ] Add Particle Vortex.
- [ ] Add Neon Filaments.
- [ ] Add favorite foreground samples.
- [ ] Remember foreground sample per user preference.
- [ ] Later allow random foreground mode independent of background random mode.

## Performance / architecture

- [ ] Measure projectM load time separately from renderer FPS.
- [ ] Record per-preset switch latency.
- [ ] Mark consistently heavy presets.
- [ ] AUTO quality: BALANCED → ECO when sustained FPS/thermal pressure is poor.
- [ ] Replace Android Visualizer waveform PCM feed with direct Media3 PCM.
- [ ] Migrate playback to MediaSessionService for production background playback.
- [ ] Move to true offscreen projectM FBO/texture compositor when using a stable supported path.

## Acceptance rules

A release is not closed only because CI is green.

Phone acceptance must include:
- startup latency;
- three consecutive manual NEXT operations;
- automatic TOP mode;
- automatic ALL mode;
- manual lock after NEXT;
- foreground sample switching;
- screen-off playback;
- 5-minute heat/smoothness check.


## v0.5.4 — interaction latency hotfix

- [x] Move TOP/ALL queue construction off the main thread.
- [x] Cache persistent preset ratings in memory.
- [x] Store rating inside prepared queue candidates.
- [x] Stop rebuilding the whole queue for 👍 / 👎.
- [x] Hide one preset in-place instead of rebuilding 4k/9k candidates.
- [x] Make manual NEXT a direct non-smooth switch.
- [x] Make FG switching independent from projectM GL load queue/mutex.
- [ ] Phone verify sub-second button response.
- [ ] Measure actual heavy-preset compile/load latency after UI freeze is removed.


## Playback Theme Engine + video export

- [x] Create research notebook: docs/visualizer/PLAYBACK_THEME_EXPORT_RESEARCH.md.
- [x] Record Cassette / Vinyl / Portrait Halo references.
- [x] Record Glass Core / Crystal Pulse reference from 332877.mp4.
- [x] Record Neon Emblem / Energy Crown reference from 332878.mp4.
- [x] Create common PlaybackTheme contract.
- [x] Add theme registry and picker.
- [ ] Prototype Portrait Halo.
- [x] Prototype Vinyl.
- [x] Prototype Cassette.
- [ ] Prototype Glass Core / Crystal Pulse.
- [ ] Prototype Neon Emblem / Energy Crown.
- [ ] Support user image / album cover / logo / background video inputs.
- [ ] Build deterministic scene-at-time renderer for export.
- [ ] Add offline audio analysis for frame-perfect export.
- [ ] Add H.264 MP4 export with audio mux.
- [ ] Export 9:16 / 16:9 / 1:1 / 4:5.
- [ ] Preview/export parity test.


## v0.6.0 — Music Video Creator foundation

- [x] Add PlaybackTheme registry and common theme metadata.
- [x] Add MusicVideoProject model with 9:16 / 16:9 / 1:1 / 4:5 profiles.
- [x] Add Hero Pack 1 FG: Neon Emblem.
- [x] Add Hero Pack 1 FG: Energy Core.
- [x] Add Hero Pack 1 FG: Orbital Crown.
- [x] Add Hero Pack 1 FG: Star Seed.
- [x] Add Hero Pack 1 FG: Wave Idol.
- [x] Keep Pulse Rays / Orbit Rings / Spectrum Halo.
- [x] Replace physical ~4,898 TOP copy with persistent indexes.
- [x] Reduce default projectM pool to indexed 1,200 seed presets.
- [x] Keep ALL 9,795 available without duplicate preset files.
- [x] Remove normal startup recursive count of 9k preset files.
- [x] Document future .faricpack delivery model.
- [ ] Phone migration/storage PASS.
- [ ] Phone review all 8 FG samples.
- [ ] Measure per-preset projectM load/compile latency.
- [ ] Mark/avoid consistently heavy projectM presets.
- [ ] Add user-facing theme picker.
- [ ] Implement standalone Neon Emblem PlaybackTheme.
- [ ] Implement first deterministic export proof.


## v0.6.1 — Standalone Hero Themes

- [x] Persist selected playback theme.
- [x] Add user-facing Playback Theme picker.
- [x] Add standalone HeroThemeView independent from projectM.
- [x] Make Neon Emblem selectable in Now Playing.
- [x] Make Energy Core selectable in Now Playing.
- [x] Make Orbital Crown selectable in Now Playing.
- [x] Make Star Seed selectable in Now Playing.
- [x] Make Wave Idol selectable in Now Playing.
- [x] Keep original Visualizer selectable.
- [ ] Android CI PASS for v0.6.1.
- [ ] Phone visual review for five Hero Themes.
- [ ] Phone verify no playback restart when switching themes.
- [ ] Next: deterministic theme frame-state contract for export parity.


## v0.6.2 — Retro themes + measured projectM latency

- [x] Add standalone Vinyl playback theme.
- [x] Add standalone Cassette playback theme.
- [x] Render current track title inside retro themes.
- [x] Stop vinyl/reel rotation on pause.
- [x] Record native projectM preset load latency.
- [x] Persist smoothed per-preset load time.
- [x] Mark presets >=1200ms as HEAVY.
- [x] Exclude learned HEAVY presets from FAST/TOP while keeping ALL intact.
- [ ] Android CI PASS for v0.6.2.
- [ ] Phone review Vinyl/Cassette.
- [ ] Collect real LOAD distribution and tune heavy threshold.
- [ ] Next: deterministic export proof.


## v0.6.5 — Offline export audio analysis

- [x] Expose current local track URI to export pipeline.
- [x] Decode local audio with MediaExtractor + MediaCodec.
- [x] Convert decoded PCM to mono.
- [x] Add overlapping 2048 FFT / 1024 hop analysis.
- [x] Produce amplitude / bass / mid / high bands.
- [x] Add adaptive beat impulse timeline.
- [x] Add OfflineAnalysisResult.signalAt(timeMs).
- [x] Wire offline analysis into Export Lab.
- [x] Use offline signal for deterministic PNG export proof when available.
- [ ] Phone-test MP3/M4A decoding.
- [ ] Add persistent analysis cache.
- [ ] Render short frame sequence from offline timeline.
- [ ] Encode first H.264 clip.
- [ ] Mux original audio into MP4.


## v0.6.6 — First H.264 video proof

- [x] Render short frame sequence from offline timeline.
- [x] Discover AVC encoder at runtime.
- [x] Support YUV420 planar / semi-planar / flexible input.
- [x] Convert deterministic Canvas frames to encoder YUV.
- [x] Encode 3-second H.264 proof at 15 fps.
- [x] Write MP4 through MediaMuxer.
- [x] Publish proof to Movies/FARIC.
- [ ] Phone-test color correctness on Samsung.
- [ ] Phone-test all aspect ratios.
- [ ] Add AAC audio path / mux.
- [ ] Increase export profile toward 1080p 30 fps.
- [ ] Extend from 3-second proof to arbitrary clip range/full song.


## v0.6.7 — Audible MP4 proof

- [x] Transcode selected source audio range to AAC-LC.
- [x] Support decoded PCM16 and PCM float input.
- [x] Mux AAC audio with deterministic H.264 proof video.
- [x] Publish final MP4 with sound to Movies/FARIC.
- [x] Keep visual timeline and audio clip on the same source range.
- [ ] Phone verify MP3 source.
- [ ] Phone verify M4A/AAC source.
- [ ] Phone verify A/V sync.
- [ ] Phone verify color format.
- [ ] Raise proof to 30 fps after correctness PASS.
- [ ] Raise toward 1080p after performance PASS.
- [ ] Add arbitrary clip range.
- [ ] Add full-song export.


## v0.6.8 — continuous live Layer-1 dynamics

- [x] Reproduce startup-only live response from phone recording.
- [x] Add shared adaptive live band contrast before Layer-1 SceneSignal fan-out.
- [x] Preserve small bass/mid/high changes after the absolute FFT level settles.
- [x] Rebase after sustained silence instead of amplifying Pause → Play as a fake transient.
- [x] Reset beat baseline on silence.
- [x] Add unit tests for long-running pulses and resume behavior.
- [ ] Phone verify 15+ seconds of continuous Energy Core response.
- [ ] Phone verify Pause → Play.
- [ ] Phone smoke Neon Emblem + one additional Hero theme.


## v0.6.9 — Neon Emblem ring continuity

- [x] Separate persistent Layer-1 rings from transient beat shockwaves.
- [x] Keep two low-alpha base rings visible between beats.
- [x] React base rings continuously to amplitude / bass / high.
- [x] Replace beat-derived radius reversal with monotonic shockwave phase.
- [x] Allow two beat waves to overlap instead of reusing one radius state.
- [ ] Phone verify 20+ seconds without full ring disappearance.
- [ ] Phone verify no inward snap on closely spaced beats.
- [ ] Phone verify visual weight stays subtle.


## v0.6.10 — beat-ring trigger reliability

- [x] Preserve ring reset/jump on each detected beat.
- [x] Remove persistent base-ring experiment from v0.6.9.
- [x] Detect beat from bass or broadband transient.
- [x] Add derivative/rise trigger for sharp non-bass impacts.
- [x] Keep duplicate-hit cooldown.
- [x] Add repeated-hit and broadband-hit tests.
- [ ] Phone verify fewer missed rings on obvious beats.
- [ ] Phone verify no excessive false positives.


## PulseDeck floating-control redesign

Current Now Playing player card is **temporary scaffolding**.

Target UI direction:
- [x] Remove the large monolithic Now Playing card that currently covers the lower part of the Board.
- [x] Keep playback controls visually floating over the scene instead of placing them inside one opaque/outlined container.
- [x] Transport controls, seek/progress, quick actions and metadata are split into separate floating blocks.
- [ ] Each block can have its own skin/background, including fully transparent / no-card mode.
- [ ] Preserve touch targets and readability even when the block background is hidden.
- [x] Let the Board visually continue behind and between floating controls.
- [ ] Recalculate Hero/GF safe area for the floating-control layout instead of treating the current card boundary as permanent.
- [ ] Allow PulseDeck blocks to be individually hidden, repositioned, resized, recolored/skinned and have opacity adjusted.
- [x] Add visualizer-first auto-hide for player blocks; double tap on empty Board toggles them.
- [ ] Keep full-control mode available for normal player use.
- [ ] Persist the selected PulseDeck layout/skin configuration.
- [ ] Keep live UI controls separate from exported Board content by default.


## Layered Board / Hero-GF transforms

- [x] Whole-GF live X position.
- [x] Whole-GF live Y position.
- [x] Whole-GF live size.
- [x] Whole-GF live rotation.
- [x] Whole-GF live opacity.
- [x] Persist whole-GF transform per playback theme.
- [x] Reset preset.
- [x] Fit Safe Area v1 preset.
- [ ] Phone acceptance for v0.7.4 controls.
- [x] Per-layer transform override: frame.
- [x] Per-layer transform override: creature.
- [x] Per-layer transform override: wordmark.
- [x] Per-layer transform override: FX.
- [ ] UI to return individual layer override to inherited/group value.
- [ ] Serialize transforms into saved MusicVideoProject.
- [ ] Use the same transforms in deterministic export.


## Board gesture editing and group audio motion

- [x] One-finger drag for whole-GF X/Y.
- [x] Pinch for whole-GF size.
- [x] Two-finger twist for whole-GF base rotation.
- [x] Persist gesture changes with slider changes.
- [x] Group rotation sway setting.
- [x] Group stereo L/R movement setting.
- [x] Group bass up/down float setting.
- [x] Live stereo balance from PCM playback chain.
- [ ] Phone tune gesture feel.
- [ ] Phone tune default rotation sway amplitude.
- [ ] Phone tune default stereo travel amplitude.
- [ ] Phone tune default bass float amplitude.
- [ ] Add offline stereo balance to export analysis.
- [x] Advanced per-layer gesture selection.
- [ ] Per-layer locking / inherit toggle.


## Global UI lifecycle / rotation

- [x] Add a shared lifecycle/rotation contract for all current/future UI windows.
- [x] MainActivity restores the active screen after rotation.
- [x] Restore vertical/horizontal scroll position where applicable.
- [x] Preserve selected playback theme.
- [x] Preserve Board transform/reaction settings.
- [x] Preserve Export Lab aspect ratio.
- [ ] Phone-audit Library rotation.
- [ ] Phone-audit Now Playing rotation while playing.
- [ ] Phone-audit Playback Themes rotation and scroll position.
- [ ] Phone-audit Board Transform rotation, content and scroll position.
- [ ] Phone-audit Export Lab rotation, selected format and scroll position.
- [ ] Move offline-analysis state/work out of Activity-local ownership.
- [ ] Audit projectM install/index/queue against duplicate work on rotation.
- [ ] Require lifecycle acceptance for every new modal/editor/floating block.


### v0.7.7 phone acceptance
- [ ] Independently move Frame.
- [ ] Independently move Shark.
- [ ] Independently move FARIC.
- [ ] Independently transform FX.
- [ ] Verify layer opacity can fully hide a layer.
- [ ] Verify whole-GF transform still composes with layer overrides.
- [ ] Verify selected layer + scroll survive rotation.


## Board settings panel dismissal

- [ ] Tap on empty live-preview area outside the Board settings panel to close/dismiss the panel.
- [ ] Add an explicit × close button to the Board settings panel/header.
- [ ] Closing the panel must not reset Board transforms, selected layer, reaction settings, or playback state.
- [ ] After dismissal, remain on the same Cyber Shark / layered GF playback screen.
- [ ] Rotation lifecycle must preserve whether the Board settings panel is open or closed.


## Board settings panel dismissal UX

- [ ] Tap on empty space outside the Board settings panel to close the panel and return to the live visualizer.
- [ ] Add an explicit × close button to the Board settings panel.
- [ ] Closing by outside tap or × must behave exactly like a non-destructive close: keep all already-saved Board/layer settings and never trigger Reset/Fit/Done side effects.
- [ ] Rotation/lifecycle restore must preserve whether the Board editor was open; closing it remains an explicit user action only.


## Per-control session revert

- [ ] Add a small red revert/reset icon at the end of every adjustable Board setting row (for example each slider).
- [ ] The icon must revert only that one control, not the whole Board, layer, or theme.
- [ ] Revert target is the value that specific control had when the current Board settings session was opened.
- [ ] If the user changes a slider multiple times during the same open settings session, pressing its revert icon restores the original entry value from the moment the menu was opened.
- [ ] Reverting one control must not change any other slider or setting.
- [ ] The revert icon is session-scoped, not a factory-default reset.
- [ ] Closing the Board settings panel ends the session; the next time the panel is opened, the new entry values become the new per-control revert baseline.
- [ ] Gesture-driven edits must participate in the same rule: if a gesture changes X/Y/size/rotation for the currently edited target, the related per-control revert icon restores that control to its value at menu entry.
- [ ] Rotation must preserve the current session baseline for every control, so rotating the device does not redefine the revert target.


### v0.8.0 floating controls foundation
- [x] Replace the old monolithic Now Playing card with separate metadata, progress, transport and quick-action blocks.
- [x] Keep transport buttons individually floating.
- [x] Remove the bottom navigation bar from Now Playing to reclaim Board area.
- [x] Auto-hide floating player blocks after 6 seconds of inactivity.
- [x] Double tap empty Board space to show/hide the floating player blocks.
- [x] Preserve hidden/visible state across rotation.
- [ ] Phone-tune vertical spacing between floating blocks.
- [ ] Add per-block visibility controls.
- [ ] Add per-block opacity controls.
- [ ] Add per-block position/size controls.
- [ ] Add per-block skin/background selection including fully transparent mode.
- [ ] Decide whether header is part of the same hideable block system or remains persistent.
- [ ] Add lifecycle-safe settings UI for floating block configuration.


### v0.8.1 approved-main-screen implementation
- [x] Implement approved concept direction in the actual app.
- [x] Replace generic transport glyphs with custom-drawn FARIC icons.
- [x] Use approved transport order and size hierarchy.
- [x] Convert lower quick actions to icon-only controls.
- [x] Add matching neon rail treatment.
- [x] Save approved reference image in repository.
- [ ] Phone compare layout proportions with `docs/design/pulsedeck-main-screen-reference.jpg`.
- [ ] Tune icon sizes and rail heights after phone screenshot.
- [ ] Add per-block hide/opacity/position/size settings later.


### v0.8.2 reference-fidelity acceptance
- [ ] Hero/GF fills upper composition more like approved reference.
- [ ] No large dead space between Hero and metadata.
- [ ] Fine multi-bar waveform matches reference direction.
- [ ] Central play/pause is visually dominant.
- [ ] Previous/next and shuffle/repeat have correct hierarchy.
- [ ] Bottom four icon-only actions are large and evenly distributed.
- [ ] Favorite and track-more controls are present around metadata.
- [ ] Auto-hide / double-tap / rotation behavior remains correct.
