# v0.6.9 Bug Register

## BUG-001 — Neon Emblem outer ring disappears / snaps between beats

Status: FIX CANDIDATE / PHONE QA PENDING

Observed in 333058.mp4:
- v0.6.8 sustained reactivity is improved;
- thin outer Layer-1 ring is not continuously present;
- transitions can look stitched because the same beat-derived ring state is reused.

Root cause:
- outer ring was only a transient beat shockwave;
- drawing stops when beat envelope reaches its threshold;
- new beats can increase beat strength and therefore move the radius back inward.

Fix candidate:
- persistent base rings;
- two independent overlapping shockwave phases;
- monotonic outward radius per wave.
