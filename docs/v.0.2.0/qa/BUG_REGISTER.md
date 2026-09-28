# v0.2.0 Bug Register

No findings yet.

## BUG-PD-001 — APK folder does not open after menu item 8

Status: FIXED IN SOURCE / PHONE RECHECK REQUIRED

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
