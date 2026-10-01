# PulseDeck center calibration

Purpose: establish authoritative center coordinates for the permanent main-page skin before rebuilding the visual composition.

## User review of center map v1

The following feedback is intentionally preserved exactly in meaning and is the source of truth for the next calibration pass.

### Incorrect centers
- C-01
- C-02
- C-04
- C-11
- C-12
- C-13
- C-14
- C-15
- C-17
- C-18
- C-19
- C-20

### Seem approximately correct
- C-05
- C-06
- C-07
- C-08
- C-10

### Not explicitly evaluated yet
- C-03
- C-09
- C-16

## Point identity contract

| ID | Object |
|---|---|
| C-01 | Back |
| C-02 | Header title |
| C-03 | Menu |
| C-04 | Hero reactor |
| C-05 | Favorite |
| C-06 | Track info |
| C-07 | Track more |
| C-08 | Waveform |
| C-09 | Progress |
| C-10 | Transport rail |
| C-11 | Shuffle |
| C-12 | Previous |
| C-13 | Play/Pause |
| C-14 | Next |
| C-15 | Repeat |
| C-16 | Quick-actions rail |
| C-17 | Theme |
| C-18 | Board |
| C-19 | Visualizer |
| C-20 | Export |

## Calibration workflow

The Android calibration page uses the real PulseDeck main-page renderer as its background.

For each point:
1. A drafting-style center target (crosshair + center circle) is shown.
2. Drag the target until its exact center sits on the visual center of the object.
3. Tap the target to save the point.
4. The page stores both pixel coordinates and normalized coordinates.
5. The next C-number becomes active automatically.
6. Swipe left/right to move between points without saving.
7. Long-press the target to copy all saved coordinates to the clipboard.
8. After C-20 is saved, the full coordinate block is copied automatically.

The calibration page is temporary engineering tooling. Its output becomes the new source of truth for the visual layout; old guessed center coordinates must not override accepted calibrated points.
