# v0.2.0 Bug Register

No findings yet.

## BUG-PD-001 — APK folder does not open after menu item 8

Status: FIX ITERATED / PHONE RECHECK REQUIRED

Observed:
- APK downloads and checksum verification complete;
- menu item 8 may return to Termux without visibly opening the destination folder.

Cause:
- the previous fallback passed `android.provider.extra.INITIAL_URI` as a String (`--es`) instead of a Uri;
- `termux-open` may also report success for a directory without a useful folder UI on some Android/file-manager combinations.

Fix:
- open Android `ACTION_OPEN_DOCUMENT_TREE` first;
- pass the exact build folder as a document Uri using `--eu`;
- keep `termux-open` only as a secondary fallback.

Fix commit:
- `4ead65c29560d520e5b5d4d79953f575f288386d`

Phone acceptance:
- pending.


Follow-up 2026-09-28:
- phone screenshot confirmed the first fix opened Android's folder-selection UI with the button "Використовувати цю папку";
- this is not the desired behavior for APK handoff;
- opener changed from `ACTION_OPEN_DOCUMENT_TREE` to `ACTION_VIEW` with directory MIME `vnd.android.document/directory`;
- follow-up fix commit: `ac11347ddf9841dccfaa5051bc917b38bd2fca1b`.

Follow-up path correction:
- project artifacts belong under `/storage/emulated/0/Documents/FARIC-Music-Visualizer/`;
- v0.1.0 was already stored under the project's `packages/` tree;
- future APK downloads now use:
  `/storage/emulated/0/Documents/FARIC-Music-Visualizer/packages/v<version>/`;
- fix commit: `e9959a370a702a0bec6b748a63a3665cbe41fdb0`.

Second phone follow-up:
- screenshot showed Samsung My Files opening only the `Documents` root, with `FARIC-Music-Visualizer` visible but not entered;
- generic document-provider directory VIEW is therefore insufficient on this device;
- downloader now tries Samsung My Files explicitly with a `file://` folder VIEW before generic fallbacks;
- fix commit: `17df2b758c96f7fc9591f2994ff3b737d3839e10`.
