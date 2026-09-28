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
- [ ] Increase foreground movement amplitude substantially.
- [ ] Make bass the strongest scale/expansion driver.
- [ ] Keep very fast bass/beat attack with smoother release.
- [ ] Increase ray/ring travel range without clipping the full scene.
- [ ] Add edge solar-flare energy tongues.
- [ ] Make flare motion react to bass + beat and texture/detail react to highs.
- [ ] Tune flare strength separately for Pulse Rays / Orbit Rings / Spectrum Halo.

### projectM startup / next-preset latency
- [ ] Stop scanning thousands of preset files on the GL thread when opening projectM.
- [ ] Build/cache a Kotlin preset catalog outside the render thread.
- [ ] Open immediately with the last valid preset when possible.
- [ ] Maintain CURRENT + 3 NEXT preset queue.
- [ ] Prefetch the next 3 preset files into memory/page cache off the GL thread.
- [ ] NEXT consumes one prepared item and immediately replenishes the queue.
- [ ] TOP / ALL rebuild the queue from their own filtered source pool.
- [ ] Tap on visualizer must use the same manual NEXT path, never bypass queue state.
- [ ] Preserve current track position while switching/rebuilding visualizer state.

### Persistent visualizer state
- [ ] Remember last background mode: TOP / ALL.
- [ ] Remember AUTO vs MANUAL state.
- [ ] Remember last background preset.
- [ ] Remember selected FARIC foreground sample.
- [ ] Restore state without blocking first render.

## v0.5.3 — preset ratings and filtering

### Rating model
- [ ] Add four states: NONE / UP / DOWN / HIDDEN.
- [ ] Add UI actions: 👍 / 👎 / −.
- [ ] 👍 increases future selection preference.
- [ ] 👎 keeps preset available but lowers future selection preference.
- [ ] − hides preset from active pools without deleting the file.
- [ ] Hidden preset immediately leaves the active queue.
- [ ] Ratings apply to the same preset across TOP and ALL modes.
- [ ] Persist all ratings across app restarts/upgrades.

### Selection policy
- [ ] HIDDEN weight = 0.
- [ ] UP presets get highest random-selection weight.
- [ ] NONE presets remain normal.
- [ ] DOWN presets remain selectable at low weight.
- [ ] Avoid immediate repeats.
- [ ] Keep a short recently-played history to reduce repetition.

### Management UI
- [ ] Show current preset name/id.
- [ ] Show current rating state.
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
