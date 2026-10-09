# PulseDeck — full-song export and rotation/lifecycle audit

**Date:** 2026-10-09  
**Source baseline:** `main` at `89bc425a36f17212c6ce0241b9314a09c9673a81`, app source `ffc0591c0099e17410f15b7113cc572fc10d660d`, v0.19.52 / build 141.  
**Evidence:** user reports a full-song MP4 export with a progress window disappearing around halfway; rotation or playback ending may have coincided. They have **not** established which event occurred first. The prior `BUG-EXPORT-001` report at ~48% remains open.  
**Status:** SOURCE AUDIT COMPLETE / FIX NOT IMPLEMENTED / PHONE QA NOT TESTED. No new signed APK from this audit.

## Verified source facts (vs uncertain causes)

1. `MainActivity.exportCompositionVideo(fullSong=true)` snapshots source URI, offline analysis, duration, ratio and composition. For full-song it exports from 0 ms for analyzed duration; it does *not* use live player position as the render clock (MainActivity ~7727–7805).
2. `PlaybackController` on `STATE_ENDED` sets UI status to «Завершено» and emits a snapshot; there is no direct instruction to cancel MP4 export in that callback (PlaybackController ~124–132). Thus **playback ending alone is not a proven cause**. Concurrent live playback, native GL, decoder and encoder can still raise memory/resource pressure; no crash metrics yet.
3. The export's `android.app.Dialog`, `ProgressBar`, status `TextView`, `AtomicBoolean cancelled`, and `thread(name="faric-full-song-export")` are locally owned by `MainActivity` (`MainActivity` ~7834–8030). The dialog state is **not** part of `onSaveInstanceState` (~552–602). Standard orientation recreation discards the window; the worker keeps references to the old Activity, dialog and views.
4. `onDestroy()` unconditionally calls `ProjectMBridge.endOfflineExport()`, deletes the offline PCM-cache file and nulls associated references (~832–846). A still-running export may be using those resources or projectM frame callbacks: **direct lifecycle race risk**. `clearScreenRefs()` similarly calls `endOfflineExport()` and releases projectM export views (~9551–9580).
5. The export progress window is cancelable by Back (`setCancelable(true)`) and on cancel sets the job-local cancellation flag; no retained job registry restores it after rotation (~7839–7857). Progress updates and completion/failure UI are posted back to the *old* Activity and dialog (~8002–8018, ~8506–8565).
6. `FLAG_KEEP_SCREEN_ON` is set while exporting (~7996–8000); this reduces screen dimming but **does not protect against Activity recreation, forced config changes, process death, crashes or low-memory kills**.
7. `OfflineAudioAnalyzer` is itself launched from a raw Activity-owned thread; `offlineAnalysis`, `offlineAnalysisUri` and `offlineAnalysisRunning` are Activity fields and not saved. `docs/architecture/UI_LIFECYCLE_ROTATION_AUDIT.md` already lists this as partial.
8. A MediaStore publication uses `IS_PENDING` and performs a cleanup on failure in `ShortVideoExportProof`; temporary files are deleted in a `finally`. **This is not an export-job checkpoint/resume mechanism**. A process kill does not guarantee callback/finally.
9. No `MediaSessionService`/export foreground service/job worker is declared in `AndroidManifest.xml`. Playback uses a `ViewModel` but **export does not**.
10. Device/video evidence does not distinguish the three events: only dialog lost, worker interrupted, or process/app crashed. Do not assert which occurred without logs and output inspection.

## UI-wide surface inventory (source-level; none of these receive a blanket PHONE PASS)

| Surface | Current ownership / storage | Rotation risk | Required lifecycle contract |
| --- | --- | --- | --- |
| Export progress dialog (3s and full song) | Local `Dialog` + worker + callback in MainActivity | **CRITICAL**: lost UI; destroyed/recreated GL and PCM lifetime conflict | Job outside Activity; reattach a progress view showing exact job/state/progress; no duplicate export; explicit cancel only |
| Export Lab and offline analysis | Screen/theme/ratio restored; `offlineAnalysis*` Activity-local | **HIGH**: analysis lost, stale/duplicate work; refresh may release render resources | Retained analysis result and request identity; screen and selected settings restored |
| Export-folder chooser | Raw `Dialog` in MainActivity | **MEDIUM**: chooser closes, parent may return | Restore chooser over Export Lab, no file operation on recreation |
| Layers/options/composition-set dialogs | Raw `Dialog` + `PulseDeckDialogs` in MainActivity; some editable values persistent | **MEDIUM/HIGH**: dialog nesting/editing position may vanish | Persist dialog stack and draft input; dismiss child only; no auto-apply/delete |
| Generic `PulseDeckDialogs` action/single-choice/confirm/text entry | `Dialog(context)` created directly; no central lifecycle registry | **HIGH systemic**: popup is not rebuilt after recreation | Typed dialog-route + parent + selected state + text/scroll; explicit action only |
| projectM foreground/center/edge controls | `ProjectMActivity` dialogs and store; per-dialog active route not saved | **MEDIUM/HIGH**: popup lost; GL/library jobs may restart with recreation | Restore selected editor over same projectM mode; prevent duplicate native jobs |
| Main Library / Tracks / Now Playing / Theme / Board | MainActivity saved screen/theme/ratio/scroll, persistent scene overlays only within living Activity | **PARTIAL**: recreation rebuilds native view; open transient popups not retained | Restore screen, navigation stack and selection without duplicate playback or destructive action |
| Center Calibration | Separate Activity; current `onCreate` builds UI, no `onSaveInstanceState` override | **REQUIRES QA** for in-memory calibration overlay and draft state | Preserve edited center positions, active tools and parent navigation; immutable baseline untouched |
| Object Constructor / Template Constructor | Separate Activity `onCreate` builds UI, no explicit saved-state override | **REQUIRES QA** for selected object, draft controls, scroll/tool states | Restore selection, working edit and active tool with no reset/auto-save |

The existing `docs/architecture/UI_LIFECYCLE_ROTATION_AUDIT.md` provides the global contract. This report distinguishes **source risk** from proven phone behavior and should not be used to claim every surface fails.

## Expected user contract

- Export owns a durable **operation ID** and immutable source/timeline/config snapshot. Playing, pausing, finishing or switching the live song does not mutate its input.
- One job at a time unless concurrency is explicitly supported. Duplicate taps/recreation cannot start another encoder, native projectM bridge or output.
- Progress state (`PREPARING / RENDERING / AUDIO / MUX / SAVING / COMPLETED / FAILED / CANCELLED`), percent, track name and failure reason are job properties, not properties of a Dialog instance.
- Rotation restores the same parent Export Lab and an attached progress surface at the latest committed percent. Close/minimize of progress UI **does not cancel** the export. Cancel requires an explicit action and confirmation. Back means «повернутися до процесу / згорнути» per UX contract, not silent job loss.
- Long-running processing continues while UI is temporarily stopped/rotated; foreground execution + appropriately classified visible notification for supported Android versions should be considered. **A retained `ViewModel` alone does not guarantee survival after process death.**
- Native projectM renderer and temporary PCM/cache have an explicit owner; `Activity.onDestroy` must never release job-owned resources mid-render. The export renderer must not be a view tied to the destroyed Activity.
- Output is atomic: no successful file/notification before complete mux+publish; cancellation/failure clean pending/temp outputs. On process death, mark INTERRUPTED and offer safe retry; checkpoint/resume only if renderer supports reliable persistence (do not pretend that frame reprocessing resumes seamlessly).
- Result appears over the completed status screen, closes independently, and survives rotation without re-exporting; history/report remains accessible even if user closes it.

## Implementation plan (separate code branch after current phone QA)

**P0 — Export job ownership (must precede claims of protection):**
1. Extract immutable `ExportRequest` from Activity: `sourceUri`, permission lifetime, analyzed content identity/hash, timeline 0..duration, theme/layers/config snapshot, aspect ratio/fps, output target, id.
2. Extract `ExportCoordinator` with serialized job state and cancellation; expose immutable progress to Activity; enforce single-start contract and record FAILED/CANCELLED separately.
3. Separate native GL/projectM export rendering from the MainActivity view; centralize ownership and PCM cache cleanup at *job* completion rather than Activity destruction.
4. Add background-safe execution with required Android foreground-service permissions/type, notification and persistent operation record after checking API-level restrictions. Avoid scheduling duplicate jobs via WorkManager and foreground service simultaneously.
5. Replace local progress `Dialog` with lifecycle-bound UI reattachment; retained route/status/result; no cancellation on system Back or rotation. Explicit Cancel + confirmation and dismiss/minimize semantics.
6. Instrument `FARIC-export` with export id, stage, percent, Activity recreation, GL owner, memory/thermal hints, result; make heavy full-song failure diagnosable.

**P1 — all-popup/overlay restoration:** introduce a typed `UiSurfaceState` (parent screen, surface kind, selected item, draft form data, scroll positions, dialog nesting order) saved in the appropriate `SavedStateHandle`/store. Apply to every existing custom dialog and panel; restore strictly as **read-only rendering** without action callbacks executing. For projectM tune dialogs, store and restore proper mode and selected editor. Add tests for child-only dismiss and Back parent continuity.

**P2 — resilience:** persist job terminal history and diagnose process death; explicit retry after interruption; no bogus “100%” until published successfully; verify temp MediaStore cleanup.

## Acceptance matrix (phone is authoritative for performance)

1. Export full song → rotate twice (portrait ↔ landscape) during render → same job and progress, no second export, final MP4 playable with correct duration/audio.
2. Export full song → let player song finish / pause/seek/Next during export → job continues independently, no UI closure.
3. Export full song → briefly lock/unlock or switch app → resume visible progress and complete (subject to OS foreground-execution restrictions).
4. Export → Back/close progress UI → operation still running, accessible through Export Lab/notification; Cancel → confirmation → one cancellation and no fake successful MP4.
5. Export → rotate at prepare/PCM/render/mux/save and completion/result phases → no duplicate callbacks, result over same Export Lab.
6. Rotate each of: generic choice, confirmation, text editor, Layers nested dialog, folder chooser, projectM controls, Board editor, Template/Object Constructor, Calibration → same parent and draft state; no accidental action triggered.
7. Kill/restart process under controlled instrumentation → no silent “success”; interrupted task classified, no corrupt published output, retry explicit.
8. Record stage logs, memory, resolution and song length if failure around 48% recurs. Avoid repetitive long exports without diagnostics.

## Ship gate

**BLOCKED** on architecture + code + tests + Android signed CI PASS + phone QA. No user-facing promise that this is fixed in build 141. Keep `BUG-EXPORT-001` OPEN; record `LIFE-ROT-001` as systemic finding. Independently finish `PERF-NAV-003` phone QA without changing the installed APK.
