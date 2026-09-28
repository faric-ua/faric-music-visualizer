# v0.5.2 Phone Test

## Fast start
- [ ] Open projectM with an already-installed library.
- [ ] First visualization appears without waiting for a full 4k/9k directory scan.
- [ ] Status eventually reports PRELOAD 3/3.
- [ ] Reopen projectM and confirm the remembered preset appears first.

## AUTO / MANUAL
- [ ] TOP starts AUTO switching.
- [ ] Wait >18 s and confirm one automatic switch.
- [ ] NEXT switches once and status becomes MANUAL.
- [ ] Wait >18 s and confirm no automatic switch.
- [ ] Tap visualization and confirm it follows the same manual NEXT path.
- [ ] ALL restores AUTO.
- [ ] TOP restores AUTO.

## Foreground
- [ ] Pulse Rays movement range is clearly larger than v0.5.1.
- [ ] Bass hit creates an immediate, larger expansion.
- [ ] Beat pulse decays naturally rather than snapping off.
- [ ] Solar-flare tongues are visible near screen edges.
- [ ] Flares grow on bass/beat and become more detailed on highs.
- [ ] Orbit Rings remains selectable.
- [ ] Spectrum Halo remains selectable.

## Persistence
- [ ] Choose ALL, MANUAL and a non-default FG sample.
- [ ] Leave/reopen projectM.
- [ ] Mode, manual state, remembered preset and FG sample are restored.

## Regression
- [ ] Playback position is not reset by visualizer changes.
- [ ] Screen-off playback still works.
- [ ] No native crash after 20 NEXT operations.
- [ ] 5-minute heat/smoothness check is acceptable.
