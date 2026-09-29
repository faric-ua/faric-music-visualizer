# v0.6.8 Bug Register

## BUG-001 — Layer 1 reacts mainly during the first ~2–3 seconds

Status: FIX CANDIDATE / PHONE QA PENDING

Observed:
- Energy Core expands strongly after Play/resume;
- after roughly 2–3 seconds the core/rays become almost static;
- playback position continues advancing;
- the same startup-like burst appears again after Pause → Play.

Evidence:
- phone screen recording supplied by the user on 2026-09-29;
- recording duration is about 11.3 seconds;
- recording contains video only, no audio stream.

Fix candidate:
- shared adaptive live band contrast;
- silence-aware rebase;
- beat baseline reset on silence.

Do not close until 15+ second real-phone playback and Pause → Play both pass.
