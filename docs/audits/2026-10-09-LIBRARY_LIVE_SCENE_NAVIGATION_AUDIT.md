# PulseDeck — аудит безперервності візуалізації під час навігації

**Date:** 2026-10-09
**Finding:** `PERF-NAV-003 — Library/Tracks ↔ Player destroys the live scene`
**Source baseline:** v0.19.51 / build 140, main app SHA `1874325cf66f694246d02329d8ca4c1700a130c3`.
**Evidence:** user's phone clip `607184.mp4` (1080×2340, ~3.35s, Home ↔ Player), direct user feedback and code-level audit of `MainActivity.kt`.
**Severity:** high UX/performance; audio continues but visualization switches involve visible hitches.
**Observed state:** **FAIL (navigation)**. This is not a claim of playback failure.

## User-confirmed phone QA of v0.19.51 / 140

- **PASS:** functional local media library, choosing a track and playing it; other inspected functionality reported PASS.
- **FAIL:** after playing, moving to Home/library or track selection and returning to Now Playing causes perceivable stutter.
- **NOT TESTED:** 3-second MP4 export. User explicitly postpones export testing until later Hero/export work.
- **DEFERRED:** cleanup or reworking Hero artwork. Preserve existing images/skin without further changes.

## Audited source of the hitch (v0.19.51)

1. `MainActivity.showLibrary()` calls `sceneOrchestrator.stop()` and `clearScreenRefs()` **for ordinary navigation**.
2. `MainActivity.showAllTracks()` does the same, including when player audio is already active.
3. `clearScreenRefs()` sets `persistentSceneRoot = null`, calls `projectMMainView.releaseProjectMBlocking()`, pauses and discards the old projectM surface, and drops `pulseDeckLayerStack`, `heroBoardView`, `heroThemeView` and HUD refs.
4. `showNowPlaying()` consequently misses the retained overlay fast path and creates a fresh root, projectM native view, rendering stack, GF bitmap layers and HUD. This matches the reported hitch.
5. `showLibrary()` additionally calls `attachExportProjectMPreview(root)`, which can construct a separate manually rendered `ProjectMView` for the Home screen; do not create that while the real live view is retained.
6. Existing `THEME_PICKER` and `BOARD_TRANSFORM` already have persistent-root overlay return paths; Library/Tracks were omitted from the same lifecycle contract.

**Evidence distinction:** a short screen recording demonstrates the user-visible flow and ongoing playback, but cannot prove native GL object identity or exact frame drop count. The object teardown/recreation above is directly visible in source. Measure on device before claiming jank resolved.

## Required lifecycle contract — no new GL on menu navigation

- While a current playback scene exists, **Player → Library → All Tracks/Search → Library/Player**, **Player → Library → Player** and **Player → Tracks → Player** must retain the *exact same* `persistentSceneRoot`, `PulseDeckLayerStack`, `ProjectMView`/native engine, visualizer state, selected theme, Board layers/positions, and audio session. Library/Tracks are replaceable opaque overlays on that root.
- Browsing, searching, filtering, granting/denying permission, Back and opening/closing an overlay must **not** trigger `clearScreenRefs`, `releaseProjectMBlocking`, new `ProjectMView`, reloading GF, starting another player, track seek or playlist reset. Audio may continue; no automatic track selection.
- MediaStore scanner may update its own list asynchronously and a fresh list UI can be built. **UI redraw of menu rows is allowed**, recreation of the running visual scene is not.
- Native GL can be paused for Android application lifecycle (`onStop`, lock/rotation/process death) separately; menu navigation inside the foreground Activity must not destroy it. Do not promise native persistence across process death.
- Selecting a *different* track should update the existing Media3 queue and reactive signal, but **reuse the already mounted visualizer**.
- UI should not spend render resources on duplicate offscreen projectM previews; no second native bridge while the live player owns it.
- Do not change immutable `PULSEDECK_CENTER_CALIBRATION`, Hero source art, export engine, signer or audio file content.

## Instrumentation / acceptance

Use `FARIC-nav` with identity logs (e.g. `System.identityHashCode()`) for root/GL/stack before and after each transition, paired with elapsed milliseconds for UI-only overlay operations. No full-screen/GL releases along covered routes.

Phone tests (mark each PASS or FAIL only after observation):
1. **Playing → Home → player (×5)**: track position keeps advancing, same theme and reactive animation resume without a visible hitch; no black screen, native view identity unchanged.
2. **Playing → Home → All Tracks → Home → player**: repeated navigation keeps the same scene, no projectM teardown or extra native GL instance.
3. **Search → select another track → player → Home → player**: new song takes effect in same scene; Next/queue works, no duplicated playback.
4. **Tracks/Library → Android Back**: one logical screen back, not app exit while an active overlay; no accidental autoplay.
5. **Miniplayer/paused track**: player scene stays mounted even if audio is paused; no repeat decode solely because it was hidden.
6. **Regression:** Board and Theme overlays still behave as previously accepted. 3-second MP4 stays NOT TESTED per user's instruction; do not call it PASS.

## Closeout conditions and deferments

- Code review + CI success alone = **CI PASS**, not navigation **PHONE PASS**.
- `PERF-NAV-003` closes only after user's phone PASS of scene continuity on the target build.
- `PERF-NAV-002` remains tracked for earlier cross-theme/navigation problems; `BUG-EXPORT-001` remains open and independent.
- Hero cleanup and export testing postponed at the user's explicit request.

## Implementation in candidate v0.19.52 / build 141

- Branch `fix/v0.19.52-library-live-scene`; source patch before final merge/build PASS.
- Added `mediaMenuOverlay` and reusable media-screen mount helper in `MainActivity`: Home/Tracks rebuild only an opaque overlay; the existing `persistentSceneRoot` and `PulseDeckLayerStack` stay mounted.
- The `showLibrary` and `showAllTracks` fast paths skip `clearScreenRefs()` and `sceneOrchestrator.stop()`; Home skips `attachExportProjectMPreview()` whenever an active scene is retained.
- Back or miniplayer return in `showNowPlaying` removes the overlay directly, rather than releasing/recreating the native view.
- The active-scene conditions in `updateSceneOrchestratorState()` and `updateProjectMRenderState()` include live Library/Tracks overlays. The same native projectM stays owned by Player.
- Opening Scene Lab from an active Library overlay first returns to the retained Player and then enters the already supported Theme overlay lifecycle.
- Added `FARIC-nav` logs with `sceneRoot`, `projectM`, and layer-stack identity values for before/after navigation. Do not treat hash identity logs alone as timing proof.
- **No user Phone PASS yet.** Confirm the absence of stalls/black frames on the installed build before closing.
