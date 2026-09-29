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
