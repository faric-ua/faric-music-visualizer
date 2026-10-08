# Open Findings

## UX-BOARD-005 — Frame signed auto-spin and single-line BG/Glow chip

- **Source observation:** User screenshots `606829.jpg` and `606827.jpg`: Board → Frame has only manual `Поворот шару`; user requests an optional independent rotation about the Frame's own center, signed speed and OFF. Screenshot `606829.jpg` shows the narrow selector `BG/Glow` wrapping into two lines, clipped in the row.
- **Scope:** v0.19.42 / build 131.
- **Source change:** `BoardLayerTransform.autoRotationDegreesPerSecond` default 0, clamp ±180 °/s and stored per theme/layer; Frame-only editor slider 0=OFF, + clockwise/- counterclockwise. Independent manual rotation preserved. Live `HeroBoardView`, CPU/GPU export `CyberSharkExportRenderer` use the same timeline-based `autoRotationOffsetAt`; explicit FRAME frameReaction continuous baseline spin set to 0, beat reaction retained. Composition Set exports/imports `spinSpeed` for layer transforms.
- **UI:** BG/Glow selector width 112dp, text size 14sp, `setSingleLine(true)`; horizontal chip scroll remains.
- **Status:** SOURCE IMPLEMENTED; Validate #1033 PASS; Android #602 pending; **PHONE QA not performed**.
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
- **Status:** SOURCE + CI PASS on v0.19.41 / build 130 (Validate #1030, Android #600); **PHONE QA PENDING**. Test full report scroll, copy full text, and OK before closing finding.

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

