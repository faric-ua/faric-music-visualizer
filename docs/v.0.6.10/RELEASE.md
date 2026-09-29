# v0.6.10 — Beat ring trigger reliability

## Correction

The v0.6.9 interpretation was wrong.

The user wants the ring to jump/reset on a new beat. That motion looks good.

The actual defect is that some clearly audible beats do not create a ring at all.

## Cause addressed

Live beat detection previously used only:
- bass energy;
- one adaptive threshold.

A strong impact that is more broadband, mid-heavy or high-heavy can therefore sound like a clear beat but fail the visual beat gate.

## Fix

The original transient ring behavior is restored.

Beat detection now combines:
- bass energy;
- broadband energy from amplitude / mid / high;
- short-term positive energy rise.

A beat can fire from either:
- crossing the adaptive energy threshold;
- a sufficiently sharp transient rise.

A short cooldown prevents one impact from producing multiple ring events.

## Acceptance

Phone QA should verify:
- obvious musical beats produce rings much more consistently;
- the preferred reset/jump remains;
- no continuous false rings between actual beats.
