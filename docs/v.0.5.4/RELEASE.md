# v0.5.4 — projectM interaction latency hotfix

## Phone finding

The projectM screen could feel frozen for several seconds after NEXT / rating / FG actions.

Root cause was mostly application-side work, not just projectM rendering:

- rating actions rebuilt the whole 4k/9k queue on the main thread;
- queue selection repeatedly recomputed SHA-256 rating keys and read SharedPreferences across thousands of candidates;
- FG switching was queued behind projectM GL work and could wait behind a heavy preset load.

## Fixes

- rating lookup now has an in-memory cache;
- queue candidates store their rating once instead of reading persistence during every weighted pick;
- UP/DOWN update the active queue in place;
- HIDDEN removes only that item from the active queue and advances without rebuilding thousands of entries;
- TOP/ALL queue preparation is performed on a worker thread;
- manual NEXT uses a hard/direct transition instead of a smooth dual-preset transition;
- FG sample selection no longer waits in the GL event queue;
- native FG sample ID is atomic and no longer waits on the projectM render/load mutex.

## Expected behavior

UI buttons should acknowledge immediately.

A particularly complex projectM preset can still take measurable time to parse/compile when it becomes active. If that remains visible after this hotfix, measure per-preset load latency and mark/skip consistently heavy presets instead of blocking the UI.
