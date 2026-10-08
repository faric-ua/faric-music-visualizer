# Open Findings

## BUG-UX-002 — Nested Visualizer settings closes Layers parent
- **Affected version:** v0.19.39 / build 128 and prior.
- **Evidence:** Phone video `606815.mp4` (~8s): player → PulseDeck tools → Layers → Visualizer ⚙. The Layers dialog disappears on opening the movable FG settings panel and does not return when the child is closed.
- **Expected:** Layers stays present under the child dialog; close X dismisses only FG settings and returns to the same Layers state/position without resetting the composition.
- **Root cause:** `MainActivity.showPulseDeckLayersDialog()` Visualizer-gear callback explicitly called `dialog.dismiss()` before showing projectM settings.
- **Status:** SOURCE FIX v0.19.40 / build 129; phone QA OPEN.

## BUG-UX-003 — projectM Back button missing and navigation stalls
- **Affected version:** v0.19.39 / build 128 and prior.
- **Evidence:** User screenshot `606817.jpg` shows no visible top-left back control in projectM. Phone video `606816.mp4` (~11s) illustrates delayed player ↔ projectM handoff.
- **Expected:** Visible, clickable PulseDeck Back on separate projectM screen; quick entry and exit, no UI-thread wait, no corrupted shared projectM renderer.
- **Confirmed code causes:** `ProjectMActivity` looks for `skin/pulsedeck_hud/utility/back.png` even though Gradle packages `skin/` as assets root, so the correct runtime path is `pulsedeck_hud/utility/back.png`. `MainActivity` explicitly called `releaseProjectMBlocking(1500)` on the UI thread before entering projectM.
- **Source changes:** Fix icon path, keep shared-GL release ordering but use `releaseProjectMThen` callback on entry, prevent repeat entry, and show a return status while queued release finishes.
- **Status:** SOURCE FIX v0.19.40 / build 129; phone QA OPEN. Renderer teardown / initialization and live preview frame pacing still require phone evidence; no unconditional smoothness claim.

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
- **Status:** IMPLEMENTED IN SOURCE for v0.19.39 / build 128; Android CI / phone QA must validate. Do not mark PASS until built and verified.

