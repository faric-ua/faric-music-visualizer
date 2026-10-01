# PulseDeck HUD skin source pack

This folder contains the first modular PNG skin decomposition for PulseDeck.

All functional UI pieces are separate alpha PNG assets. The Android skin engine
should treat `manifest.json` as the layout/action contract and should not redraw
the visual look in Canvas.

Notes:
- `transport/play.png` is derived from the generated pause button so both states
  share the same shell/glow style.
- `hero/reactor_frame.png` and `hero/F_core.png` are split from the generated
  reactor sheet.
- `progress/progress_line.png` has the generated thumb removed; the thumb is
  available separately as `progress/progress_thumb.png`.
- Coordinates in `manifest.json` are normalized 0..1 and are starter values for
  phone tuning.
