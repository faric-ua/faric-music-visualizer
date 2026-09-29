# v0.6.10 Bug Register

## BUG-001 — Some audible beats do not produce Layer-1 ring

Status: FIX CANDIDATE / PHONE QA PENDING

Observed:
- sustained reaction is improved in v0.6.8;
- the ring reset/jump itself is desirable;
- some obvious musical hits still have no corresponding ring.

Cause candidate:
- beat detector was bass-only and could miss broadband or mid/high-heavy transients.

Fix candidate:
- full-band impact detection;
- transient-rise fallback;
- preserved cooldown and original ring animation.
