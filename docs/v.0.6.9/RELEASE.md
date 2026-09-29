# v0.6.9 — Layer-1 ring continuity hotfix

## Finding

The v0.6.8 live-signal hotfix improved sustained music response, but phone recording 333058.mp4 still shows a visual discontinuity in Neon Emblem.

The thin outer ring is currently implemented as a beat-only shockwave. Therefore:
- it can disappear completely between beat envelopes;
- its radius is derived from the current decaying beat value;
- a new stronger beat can reduce that radius again, creating an inward visual snap.

## Fix

Neon Emblem now has two persistent Layer-1 base rings:
- always present at low alpha;
- softly modulated by amplitude, bass and highs;
- slightly breathing over time;
- visually behind the main reactive emblem.

Beat shockwaves are now separate from those base rings.

Two shockwave slots alternate on beat events. Each wave:
- starts near the inner/base-ring region;
- progresses only outward;
- fades as it expands;
- can overlap the previous wave.

This removes the previous one-ring reset seam.

## Acceptance

Real-phone QA:
- Neon Emblem runs for at least 20 seconds;
- base ring(s) never fully disappear while playback is active;
- rapid consecutive beats do not make an existing wave jump inward;
- shockwaves remain readable but do not dominate quiet passages.
