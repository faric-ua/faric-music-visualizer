# v0.2.1 Bug Register

## BUG-PD-003 — Startup crash after immersive fullscreen change

Status: FIXED IN SOURCE / PHONE RECHECK REQUIRED

Observed:
- signed v0.2.0 candidate installed;
- app closed on launch and Android showed the crash-recovery dialog.

Hotfix:
- replace direct platform insets path with AndroidX compatibility APIs;
- apply immersive mode after setContentView;
- guard fullscreen and safe-area application.

Phone acceptance:
- pending.
