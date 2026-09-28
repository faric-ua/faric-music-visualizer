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
- [ ] Add SceneSpec / visualizer / background / palette models.
- [ ] Add testable RandomSceneEngine with immediate-repeat prevention.
- [ ] Add SceneOrchestrator with manual + timed switching.
- [ ] Implement 3 foreground visualizer styles.
- [ ] Implement 4 built-in procedural backgrounds.
- [ ] Add background crossfade.
- [ ] Add manual Shuffle Scene action to Now Playing.
- [ ] Keep local FFT/beat stream alive across scene changes.
- [ ] CI build/test PASS.
- [ ] Signed APK phone QA with 3 musically different tracks.
- [ ] Close v0.3.0 only after automatic + manual scene switching phone PASS.
