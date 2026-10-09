# PulseDeck — Local Library categories: read-only preflight

**Date:** 2026-10-09  
**Status:** DESIGN / NO APP CODE CHANGED; do not implement or release until build 141 PHONE QA for `PERF-NAV-003` is resolved.

## Verified baseline

- v0.19.51 introduced `LocalMusicLibrary.scan()` with MediaStore `_ID`, `TITLE`, `ARTIST`, `ALBUM`, `DURATION`. It returns local content URIs, sorted tracks, and searches title/artist/album.
- `LocalMusicBrowser` performs background scanning, filtered `ListView`, and hands the selected *visible filtered queue* to the existing playback engine. It currently scans again when a new browser is created.
- v0.19.52 uses a retained `persistentSceneRoot` and overlay for Library/Tracks when an active scene exists; this must remain true for new categories. Code+CI PASS only, phone QA still pending.
- Library category cards exist in Home but are not functional yet. Do not represent them as shipped.

## Suggested implementation order — each independently testable

1. **Albums (read-only):** extend local track metadata with MediaStore `ALBUM_ID` (where available) and album-artist information. Group by a stable album key rather than title alone (different albums may share a name). Open album → tracks → choose track → existing Media3 queue. Preserve Back stack and retained GL.
2. **Artists (read-only):** use stable artist identification if the platform supplies it; decide how to handle compilation or multi-artist credits. Artist → albums/tracks → select track, again with the same playback owner.
3. **Folders (read-only):** prefer MediaStore relative-path metadata where supported; handle absent/legacy path metadata explicitly and keep SAF manually imported files distinct. Do not infer a trustworthy writable filesystem path from a media URI.
4. **Collections (user-created):** separate durable local model for named collections, membership, ordering, duplicate policy, rename/delete confirmations and migration. Do not silently alter MediaStore or treat virtual Collections as ordinary MediaStore albums.

## Technical constraints

- Extend one shared read-only MediaStore query and one indexed data model rather than re-scanning on every category drill-down. Consider a bounded in-memory catalog cache with explicit refresh/invalidation and off-main-thread scanning.
- Keep one playback engine and one retained Scene Host; navigating category → detail → track must never call `clearScreenRefs()`, `releaseProjectMBlocking()`, or create a second projectM preview.
- Keep permission prompts explicit; denial must leave SAF/manual picker working. Avoid remote reads and write permissions for category browsing.
- Unknown title, artist, album and folder metadata need graceful labeled fallbacks. Stable IDs, tracks with missing metadata, name collisions and rotated screen states need tests.
- Preserve search query, scroll position and logical navigation on overlay transitions when practical; no spontaneous audio seek or track selection.
- No changes to `PULSEDECK_CENTER_CALIBRATION`, Hero assets or MP4 export pipeline in this workstream.

## Unit/behavior test targets

- Same album title by different artists; compilation album; missing album; duplicate display names.
- Consistent grouping/search under case differences; large library sorting and fast scroll; selection produces expected queue/next track.
- Permission denied then granted; no music; deleted files; scan error; category return navigation.
- Player → Library → Albums → Album → Player, and repeated Back: same scene identities via `FARIC-nav`, no black frame or audible restart.
- Device QA on the signed build after CI. Validate menu transitions physically; never claim performance PASS on JVM/unit tests alone.

## Start gate and next step

The user is currently installing/testing v0.19.52 / build 141. Await their **PASS/FAIL** for navigation. If FAIL: diagnose `PERF-NAV-003` first. If PASS: begin the smallest Albums slice in a feature branch, with a behavior contract, precise file list, tests, CI, phone QA and updated handoff. No new APK was produced by this document.
