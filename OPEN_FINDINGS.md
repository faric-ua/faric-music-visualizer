# Open Findings

## BUG-EXPORT-001 — full-song ends, application exits, no MP4 (new phone report 2026-10-09)

**User-confirmed symptom:** choosing **full-song** conversion rather than the 3-second proof makes a long rendering run; the song eventually stops playing, application exits/closes, and **no converted MP4 is found**. The exact crash stack and last progress stage were not captured, so do not assert exact root cause or conflate playback completion with render completion. Prior mitigation v0.19.53 only locked rotation and confirmed cancel; it did not solve storage-heavy finalize or process death.

**Source-confirmed risk removed by v0.19.57 / build146 PR #14:** previously the pipeline generated the full encoded video MP4 and AAC temporary audio, then **another full-length muxed MP4 in app cache, then copied that large result to MediaStore**. If space becomes insufficient near the end, this can fail after the expensive render, leaving no deliverable. For Android 10+, new direct seekable FD mux into pending `Movies/FARIC` creates no second complete muxed-cache file; pending record is published only on success and deleted on failure. Early free-space budget, finite AAC/mux cancellation, video-temp cleanup and stage-specific UI/logging also added. Pre-Android 10 keeps old fallback.

**Release:** merged app SHA `1c7a58f5b6f649127f37a3a1df546235737edd3f`. PR Validate #1168 PASS and Android #644 PASS. Main Validate #1169 PASS; main Android #645 **RUNNING at time of finding**; signed artifact and physical QA must be separately confirmed before claiming readiness.

**STATUS: OPEN / PHONE QA PENDING.** When signed main build146 passes, one focused phone test: complete an entire song, keep screen active, check visible progress transitions video→AAC→finalize, confirm completed MP4 actually appears in `Movies/FARIC`, plays video+audio and lasts entire track. A single pass/fail is sufficient. If app still exits, capture `AndroidRuntime FATAL EXCEPTION`, `FARIC-export`, device free space and stage; investigate real stack, not another speculative workaround.

**Separate architectural gap:** export still depends on Activity-owned GL and is not a fully process-death-resumable foreground service. `LIFE-ROT-001` remains open; no promises of surviving OS kill/force-stop while backgrounded.


## LIB-SYS-001 — system status/navigation bars hidden on Home and media browser (2026-10-09)

**Phone report, v0.19.54 / build 143:** User says the library/home categories work, but Android system status and navigation panels are missing, making device control awkward. Source-confirmed cause: `showLibrary()` and `showAllTracks()` called `enableImmersiveFullscreen()`, and `onWindowFocusChanged()` re-hid bars.

**Fix candidate in `main`: v0.19.55 / build 144, merged PR #12**, source SHA `f4daecb3c0d74971491eea0bb8e9c0db52cdad53`. Library/Tracks call `showMediaLibrarySystemBars()`; regained focus reasserts bars; safe area accounts for status/nav/gesture/cutout and keyboard IME. Existing Player and projectM immersive policy retained. **PHONE QA PENDING**; don't mark CLOSED merely from CI.

## LIB-SEARCH-001 — search typing apparently closes app near “HOR/горизонт” (2026-10-09)

**Phone report, v0.19.54 / build 143:** user pressed top search control and typed “горизонт” (possibly Latin H → O → R), around the R key the app closed. **No Android crash stacktrace supplied: exact root cause unverified.** Candidate mechanisms include synchronous ListView adapter rebuilding inside `TextWatcher.onTextChanged` during IME resize and stale list positions; these are *risks*, not a proven exception trace.

**Fix candidate in `main`: v0.19.55 / build 144, merged PR #12**. Debounces edits 180ms, moves list rebuilding off synchronous IME callbacks, cancels stale callbacks on detach, bounds-checks adapter indexes, caches group projection, logs `FARIC-library-search`, adds incremental HOR / Cyrillic query tests. **PHONE QA PENDING**. If crash reproduces, gather Android logcat filtered for `FATAL EXCEPTION`, `AndroidRuntime`, `FARIC-library-search`; investigate actual stack, not assumptions.

**QA scope for this batch:** after signed main build144 and single installation: Android status/nav bars visible in Home and All Tracks/Folders/Albums; keyboard with queries H, HO, HOR, HORIZON and «горизонт» (type normally and quickly), no app close; selecting track and returning Home restores system panels. User reported existing category headers working in build143; do not demand a full repeat of nine-category QA.


## PERF-NAV-003 — target navigation regression PHONE PASS (2026-10-09)

- User explicitly answered **«Пасс»** after being asked to check **Player ↔ Home** and **Player → All Tracks → Back** in v0.19.52 / build 141. Targeted navigation PASS; mark **PERF-NAV-003 CLOSED for those specific routes**.
- This is **not** a claim that every transition, search/queue, GL identity, animation benchmark, export or rotation scenario passed. Other tests are not confirmed.
- **BUG-EXPORT-001 / LIFE-ROT-001 remain OPEN**: full-song export interruption around halfway and rotation-safe dialogs, background execution and resource ownership are separate issues.


## v0.19.48 / build 137 — Canonical Theme Host + visible system bars in projectM

- **User phone QA, build 136:** Shark ↔ Panther switches quickly, legacy projectM menu correct, Board and Theme browse return smooth. **FAIL remains** when changing from a layered GF theme to Neon/Energy/Core/Star/Wave/Vinyl/Cassette or back: legacy Theme Picker path calls `showNowPlaying()`, `clearScreenRefs()`, and recreates projectM. projectM authoring still hides Android **status + navigation bars**, contrary to original UI. Clips `607032.mp4`, `607033.mp4` and screenshot `607030.jpg` supplied in chat.
- **v0.19.48 source candidate:** unified live routing for all implemented Playback Themes, mounted `HeroThemeView` alongside `HeroBoardView` in the existing GF slot, switch visible central renderer without calling `clearScreenRefs` or constructing a new projectM view. Bitmapped Shark/Panther remain background-loaded; Neon Emblem, Energy Core, Orbital Crown, Star Seed, Wave Idol, Vinyl and Cassette switch an existing Canvas view; VISUALIZER shares the same return path. No schema migration, no alteration of immutable center calibration.
- Canonical catalog: duplicate Wave Idol entry removed while preserving its ID and original first description; registry unit tests assert unique theme IDs and expected implemented themes. Unimplemented Portrait Halo / Glass Core / Poster stay `СКОРО`.
- projectM overlay explicitly calls WindowInsetsController.show(systemBars), applies system bar/cutout padding to overlay controls only, and restores player immersive mode upon exit. onWindowFocusChanged honors projectM-specific bar policy, rather than immediately rehiding them.
- **Status: Validate #1089 PASS / Android #613 PASS / PHONE QA PENDING.** PR #4 merged into main at app/source `edd69d6cbf5c3abb4bc7859f5c54a5f9cdd7a3ff`. Android run `37813832199`, signed artifact `FARIC-Music-Visualizer-v0.19.48-Debug`, ID `11566826167` (expires 2026-10-11). Immutable checkpoint branch `checkpoint/pulsedeck-v0.19.48-2026-10-08`. Next Termux **3 → 10 → 8**, no build 9. Phone-test all live Themes, projectM both Android system bars, Board, 3s MP4. PERF-NAV-002 still OPEN pending physical validation; BUG-EXPORT-001 independent.


## v0.19.47 / build 136 — original projectM UI + live hero switching (CI PASS; PHONE QA PENDING)

- Code in main: `f5ce3e90a33122ee59c3599cc0830b29f08c8093`; PR #3 merged.
- Validate #1086 PASS; Android #611 PASS, run `37810162017`, artifact `FARIC-Music-Visualizer-v0.19.47-Debug`, id `11565016839`, expires 2026-10-11.
- Immutable checkpoint branch: `checkpoint/pulsedeck-v0.19.47-2026-10-08`.
- v0.19.46 phone findings: Board, projectM and Themes browsing smooth; projectM legacy menu was replaced and Shark ↔ Panther still jerked. v0.19.47 restores original projectM 4-row controls, status and ratings within persistent scene; loads GF assets off UI and swaps only HeroBoardView in-place. `FARIC-nav` reports assetDecode and UIApplyAndReturn milliseconds.
- **Phone QA PENDING.** Next: Termux **3 → 10 → 8** (do not press 9), install build 136. Test original projectM menu, Back/ratings/AUTO/FG; Shark ↔ Panther ×4 without visible stall or GL recreate; Board and Themes Back ×3; short MP4.
- PERF-NAV-002 remains PARTIAL until theme switch phone PASS; BUG-EXPORT-001 remains OPEN for full-song export.


## v0.19.47 / build 136 — original projectM UI and smooth hero switching

Phone QA of build 135: Board, Themes browse and projectM overlay navigation are smooth. Remaining issue: Shark ↔ Panther swap causes a severe hitch. User asks to restore old projectM layout. Candidate build 136 restores its original four control rows, status, ratings, auto interval and icon Back in the existing overlay. Theme assets are decoded on worker and swapped in place without rebuilding native projectM. CI/phone acceptance pending; keep PERF-NAV-002 open for hero change. See CURRENT_HANDOFF.md.

## ARCHITECTURE CANDIDATE — v0.19.46 / build 135 (persistent Scene Host)

- User videos `606999.mp4` (Playback Themes ↔ Player transitions) and `607000.mp4` (3s exported MP4) reinforce that screen navigation should not own GL lifecycle. Most recent 1080×1920 / 90-frame report: **5699 ms**, projectM 790, composition 2005, HUD draw 1097; export timing is not navigation latency.
- Decision and acceptance contract: `docs/architecture/PULSEDECK_PERSISTENT_SCENE_HOST.md`. **Single native projectM view and one live PulseDeckLayerStack** on foreground playback. Board and Theme Picker draw menu overlays; projectM editor uses in-Activity controls, no native release or second Activity for ordinary installed projectM. Legacy Activity kept for initial library setup fallback.
- **CI PASS / PHONE QA PENDING.** PR #2 merged into main at app/source `6ce0a33a893edc78b1126e98ff5cf39181f9daaa`. Validate #1081 PASS; Android #607 PASS (run `37804612518`), artifact `FARIC-Music-Visualizer-v0.19.46-Debug` id `11562008086` (expires 2026-10-11). Immutable source branch `checkpoint/pulsedeck-v0.19.46-2026-10-08`. **Next: Termux 3 → 10 → 8 (do not launch build 9), install build 135 and physically test.** Do not call navigation PASS yet.
- QA: Player ↔ Board x3 (preserve same rendered background), Player ↔ projectM editor with app/system Back x3 (same native instance, tuned FG/NEXT), open/cancel Theme Picker x3, prefs/Frame/Center survive, 3-second export regression. Full-song export bug remains separate and OPEN.


## SOURCE CANDIDATE — v0.19.45 / build 134 (projectM exit GL ownership)

- **New phone evidence on v0.19.44 / 133:** projectM → app Back FAIL; projectM → Android system Back FAIL; Board and Frame/Center NOT TESTED; 3-second MP4 PASS (1080×1920, 90 frames, total 5789 ms). PERF-NAV-002 remains OPEN.
- **Audit:** ProjectMActivity.exitToPlayer nulled projectMView before Android onPause; old continuous GLSurfaceView may render against destroyed singleton bridge. This is a code-level race risk, not a phone-confirmed root cause.
- **v0.19.45 source:** keep exiting view until release callback, explicitly GL-pause before finish; add volatile release guard to skip old renderer onDrawFrame/onSurfaceChanged; log exit release/pause timings under FARIC-nav.
- **CI / PHONE QA: PENDING.** No claim of improved smoothness until tested. Validate + Android build on new main commit, then Termux 3 → 10 → 8. Test both projectM Back paths 3 times, Board, Frame/Center retention, 3s MP4. BUG-EXPORT-001 still OPEN.


## PERF-NAV-002 — v0.19.44 candidate (build 133): targeted navigation ownership fix

- **Source implementation (not yet phone-accepted):** `MainActivity.deferMainProjectMRebuild` ensures Board ↔ Player's shared projectM bridge is destroyed on the original GL thread **before** the new screen/replacement bridge is created; the Android UI thread no longer waits inside `releaseProjectMBlocking` on this specific route. Rapid repeat rebuild requests are coalesced to their latest destination.
- **Separate projectM Activity return:** instead of calling `showNowPlaying()` and rebuilding all PulseDeck/HUD/Board layers on ActivityResult, restore only the projectM visualizer slot in the preserved `PulseDeckLayerStack`; then update render state. First app launch and missing stack retain the old full rendering fallback.
- **Diagnostics:** `FARIC-nav` reports queued and completed Board/player transition times and projectM slot restoration; `FARIC-projectM` logs native release queue/destroy and native create times, first rendered frame.
- **Limit:** MainActivity's generic `clearScreenRefs()` still blocks on other transitions (Library/Export/Theme); projectM native initialization and SurfaceView attach can still cause delays. No claim of freeze-free performance until phone QA.
- **Status:** **SOURCE + CI PASS:** Validate #1075 and Android #604, v0.19.44 / build 133; **PHONE QA PENDING**, perf issue remains OPEN until navigation retest.
- **Acceptance:** 1) Repeat Board ↔ Player 3 times, no visible freeze/black scene and maintained projectM background; 2) repeat player → projectM → app Back and Android system Back 3 times, background restored and HUD stable; 3) long/signed Frame and Center spin unaffected; 4) 3-second MP4 proof regression, no full-song export until separately diagnosed.

## PHONE QA — v0.19.43 / build 132 — 3 PASS / 2 FAIL (2026-10-08)

**User-submitted results (unambiguous):**
1. **PASS** — 3-second export opens finished report; both `Копіювати текст` and `OK` available.
2. **PASS** — Copy successfully copies statistics, **does not dismiss report**.
3. **PASS** — OK dismisses only report; music player remains active.
4. **FAIL** — player → projectM → Back: observable freezes and/or black/paused scene during transition.
5. **FAIL** — player → Board → Back: observable freezes or recreation of visualizer.

**Acceptance:** Export report lifecycle fixed and phone-accepted in v0.19.43; no need for the user to rerun the same 3-second report test in next APK unless regression. **Navigation PERF-NAV-002 remains unresolved and is the next highest-priority engineering step.** User's message did not isolate exact frame time or differentiate black frame from pause; don't overstate that aspect.

**Code path grounded at v0.19.43:** `MainActivity.clearScreenRefs()` calls `ProjectMView.releaseProjectMBlocking()` on the main thread for Export/Main, and both `showNowPlaying()`/`showBoardTransform()` reconstruct a new ProjectMView and compositor; separate projectM exit calls async destroy+finish then `ActivityResult` rebuilds main. The blocking release has a 1500ms default timeout but native initialization and EGL reattachment may also contribute.

**Next action:** PROFILE/FIX with safe ownership barriers: 1) instrument step-level UI timings: release wait, GL destroy, renderer initialization, first frame, SurfaceView attach; 2) minimize creation/teardown on Board↔player transition (consider persistent owner + reparent only after confirming EGL behavior), don't race two owners of shared ProjectMBridge; 3) separately shorten projectM Activity exit/return; 4) CI Android PASS and repeated phone QA routes both directions. **Do not mark performance PASS on unit tests alone.**

## UX-EXPORT-006 — Copy report closes unexpectedly before OK

- **Observed on phone (2026-10-08):** user successfully copied full 3-second export report at 1080×1920, 90 frames, total 5976ms, but window closed immediately. They expected Copy to leave the report open for an explicit OK.
- **Existing cause:** generic `PulseDeckDialogs` dismisses after every action, including Copy.
- **Fix source:** v0.19.43 build 132: opt-in `keepOpenOnSecondary = true` in export result; per-action `dismissOnClick` false on Copy, true on OK; previous default kept for every other caller. Maintains fixed footer and scrolling result introduced v0.19.41.
- **Status:** **PHONE QA PASS / CLOSED in v0.19.43 build 132.** User verified Copy keeps report open and copies text, OK dismisses only report, 3-second export report buttons accessible (3/3 PASS).
- **Acceptance:** copy full report → dialog remains and buttons stay usable → OK closes; other dialogs remain unchanged.
- **Metric evidence:** projectM 791ms; composition 1935ms; encoder 873ms; GPU overlay 664ms; Cyber Shark 359ms; HUD draw 1051ms; total 5976ms. This short export does not close BUG-EXPORT-001 (full-song exit around 48%).

## UX-BOARD-005 — Frame signed auto-spin and single-line BG/Glow chip

- **Source observation:** User screenshots `606829.jpg` and `606827.jpg`: Board → Frame has only manual `Поворот шару`; user requests an optional independent rotation about the Frame's own center, signed speed and OFF. Screenshot `606829.jpg` shows the narrow selector `BG/Glow` wrapping into two lines, clipped in the row.
- **Scope:** v0.19.42 / build 131.
- **Source change:** `BoardLayerTransform.autoRotationDegreesPerSecond` default 0, clamp ±180 °/s and stored per theme/layer; Frame-only editor slider 0=OFF, + clockwise/- counterclockwise. Independent manual rotation preserved. Live `HeroBoardView`, CPU/GPU export `CyberSharkExportRenderer` use the same timeline-based `autoRotationOffsetAt`; explicit FRAME frameReaction continuous baseline spin set to 0, beat reaction retained. Composition Set exports/imports `spinSpeed` for layer transforms.
- **UI:** BG/Glow selector width 112dp, text size 14sp, `setSingleLine(true)`; horizontal chip scroll remains.
- **Status:** SOURCE + CI **PASS**: Validate #1033, Android #602 / build 131, signed APK artifact 11525215496. **Phone QA 5/6 PASS on v0.19.42** (Frame ±/0 speed, isolation, persistence/Composition Set, BG/Glow one-line, 3s MP4). Navigation NOT TESTED on this build; prior stalls OPEN.
- **Next phone QA:** Change Frame manual rotation to 60° and auto speed +30/−30/0°/s, confirm only Frame moves; save/reopen board and Composition Set; test 3s export preview with auto speed and observe frame rotation. Check BG/Glow label all on one line.

## PERF-NAV-002 — Existing transitions still pause / freeze

- **Source evidence:** User reported after v0.19.40 fix: visible top-left app Back has returned (PASS), but main player ↔ separate projectM and screen-to-screen visualizer transitions still visibly pause/freeze. Current screenshots `606831.jpg` and `606827.jpg` confirm the visible Back and functioning editors but cannot time the freeze.
- **Confirmed technical path:** `MainActivity.clearScreenRefs()` synchronously calls `ProjectMView.releaseProjectMBlocking()` for projectM export and main views on the UI thread, then `showNowPlaying()/showBoardTransform()` rebuilds native GL and the entire composite layer tree. The earlier v0.19.40 change only removed blocking GL release on the main-player → separate projectM entry path; it did not change this general screen-reset path or costly GL initialization. Precise delay distribution on the device remains unmeasured.
- **Status:** OPEN / **not fixed by v0.19.42**. Do not close issue based on presence of Back icon.
- **Next:** targeted lifecycle architecture/performance pass: instrument time of release, host reattach, native surface initialization, first rendered frame and UI responsiveness; choose safe persistent GL renderer across Board ↔ Now Playing and separate Activity transition without unsafe competing GL contexts. Require phone video/timings after fixing.

## BUG-UX-004 — export timing report hides Copy / OK buttons

- **Affected version:** v0.19.40 / build 129 and earlier.
- **Evidence:** Phone screenshot `1791424966546.jpeg` shows `Експорт завершено` with many timing lines but neither `Копіювати текст` nor `OK` visible at bottom. User also provided a completed 3-second MP4 `606819.mp4` (1080×1920, 30fps, 90 frames on timing screenshot) for separate export proof.
- **Expected:** Styled result dialog remains within the visible viewport; only long timing message scrolls; heading and both buttons remain fixed and tappable, including at the bottom of report.
- **Root cause:** generic `PulseDeckDialogs.showMessage` only wraps messages longer than 900 chars in a fixed 420dp ScrollView; dialog window itself uses WRAP_CONTENT, so large reports can exceed the screen and push action buttons out of view.
- **Source fix:** v0.19.41 / build 130 opts the export result into a capped-height dialog; message uses a weight=1 `ScrollView`; fixed actions remain outside that scroll area. Other smaller app dialogues retain previous sizing.
- **Status:** SOURCE + CI PASS on v0.19.41 / build 130 (Validate #1030, Android #600); **PHONE PARTIALLY ACCEPTED**: Copy/OK visible, user successfully copied long report on build 131. Copy dismisses dialog; user agrees to retain it. Full scrolling to last line / separate OK tap not explicitly confirmed.

## BUG-UX-002 — Nested Visualizer settings closes Layers parent
- **Affected version:** v0.19.39 / build 128 and prior.
- **Evidence:** Phone video `606815.mp4` (~8s): player → PulseDeck tools → Layers → Visualizer ⚙. The Layers dialog disappears on opening the movable FG settings panel and does not return when the child is closed.
- **Expected:** Layers stays present under the child dialog; close X dismisses only FG settings and returns to the same Layers state/position without resetting the composition.
- **Root cause:** `MainActivity.showPulseDeckLayersDialog()` Visualizer-gear callback explicitly called `dialog.dismiss()` before showing projectM settings.
- **Status:** source in v0.19.40 / build 129 — Validate #1028 and Android #599 PASS; **phone QA OPEN**.

## BUG-UX-003 — projectM Back button missing and navigation stalls
- **Affected version:** v0.19.39 / build 128 and prior.
- **Evidence:** User screenshot `606817.jpg` shows no visible top-left back control in projectM. Phone video `606816.mp4` (~11s) illustrates delayed player ↔ projectM handoff.
- **Expected:** Visible, clickable PulseDeck Back on separate projectM screen; quick entry and exit, no UI-thread wait, no corrupted shared projectM renderer.
- **Confirmed code causes:** `ProjectMActivity` looks for `skin/pulsedeck_hud/utility/back.png` even though Gradle packages `skin/` as assets root, so the correct runtime path is `pulsedeck_hud/utility/back.png`. `MainActivity` explicitly called `releaseProjectMBlocking(1500)` on the UI thread before entering projectM.
- **Source changes:** Fix icon path, keep shared-GL release ordering but use `releaseProjectMThen` callback on entry, prevent repeat entry, and show a return status while queued release finishes.
- **Status:** source in v0.19.40 / build 129 — Validate #1028 and Android #599 PASS; **phone QA OPEN**. Renderer teardown / initialization and live preview frame pacing still require phone evidence; no unconditional smoothness claim.

## BUG-EXPORT-001 — Full-song export loses app/process at ~48%

- **Affected version:** v0.19.38 / build 127 (phone report, 2026-10-08).
- **Evidence:** User started full-song export; progress reached roughly 48%. The same song was playing in the main player, and when playback reached the end, the whole app appeared to close. No crash log or reproduction video has been supplied.
- **Expected:** Offline export continues independently of live playback through 100%, yields a playable MP4, and the app remains responsive. A player reaching STATE_ENDED must not implicitly cancel or destroy export.
- **Actual:** App reportedly exited at ~48%; whether this was Java crash, native crash, Android low-memory kill, or Activity exit is **unknown**.
- **Severity:** HIGH (potential export data loss / unusable full-song export).
- **Status:** OPEN — evidence required; **NOT FIXED**.
- **Code inspection:** `MainActivity.exportCompositionVideo()` starts a separate thread and uses the initial current-track URI / analyzed duration. `PlaybackController.onPlaybackStateChanged(STATE_ENDED)` only updates its status; no direct app-exit path identified there. Concurrent live playback, projectM GLES, and full-resolution export may increase resource pressure, but OOM and lifecycle race remain hypotheses, not established root causes.
- **Next diagnostics:** Collect Android crash/process-death evidence (`FATAL EXCEPTION`, `Fatal signal`, `lowmemorykiller`, `am_kill`, `OutOfMemoryError`) at reproduction. Preserve song length, export resolution/fps, thermal state, available storage and whether Music playback was active; check if a partial video exists in `Movies/FARIC`.
- **Controlled reproduction:** First 3-second preview, then full song with playback PAUSED; if that succeeds, compare full song with simultaneous playback. Avoid repeating high-cost full-song exports without logs.

## PERF-PLAYER-001 — Playback/preview jerkiness and asymmetric entrances

- **Affected version:** v0.19.38 / build 127; older preview smoothness finding.
- **Evidence:** User reports continued short freezes and visual objects entering from opposite sides / unevenly. A video is referenced in the message but no video file is available in this conversation to inspect frame-by-frame.
- **Expected:** Consistent smooth transitions and predictable object motion in live preview and exported output; independent diagnostics for frame pacing vs authored motion.
- **Actual:** Subjective jerkiness and irregular direction observed by user, not quantified from video or frame timestamps.
- **Severity:** MEDIUM (visual QA blocker), potentially HIGH if exported video is affected.
- **Status:** OPEN — request original video and exact build/screen; **NOT FIXED**.
- **Next diagnostics:** Obtain MP4 or screen recording, note whether preview, exported MP4, or both; inspect frame timing, projectM transitions, compositor asset layers, and performance counters before attempting gesture/animation changes.

## UX-FG-001 — Tap individual numerical value to reset; auto spin Center

- **Affected version:** v0.19.38 / build 127.
- **Evidence:** User asks to tap numeric value between minus/plus and reset only that parameter to default. Also requests Center rotation around its axis with adjustable speed/direction.
- **Expected:** Value tap changes only that parameter; group Reset retains existing behavior. Auto spin uses °/s, 0=off, +/- controls direction, preview and export share the same saved tuning.
- **Actual:** Before v0.19.39, only group-wide reset; rotation was manual angle only.
- **Severity:** UX improvement.
- **Status:** **PHONE ACCEPTED for 5/6 v0.19.39 checks** by user (numeric reset, spin +30°/s / -30°/s / off, persisted speed and grouped Composition Set test, main-player spin / 3s export). Back was NOT TESTED on v0.19.39; separate earlier Back regression and v0.19.40 navigation work remain OPEN. Export auto preset sequencing still TODO.

