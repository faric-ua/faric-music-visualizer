# UI Contract

## Primary screen

Full-screen visualizer.

Normal playback view minimizes chrome.
Controls appear on tap and can auto-hide.

Required:
- play/pause;
- seek;
- previous/next when queue exists;
- preset selector;
- background selector;
- sensitivity/settings;
- exit/back.

## Visual hierarchy

1. background;
2. ambient motion/effects;
3. central orb/cover/logo;
4. beat-reactive radial effects;
5. controls overlay.

## Orientation

Portrait is primary.
Landscape remains supported and must not be broken to hide layout problems.

## Accessibility

- controls use safe insets;
- critical targets about 48dp+;
- color is not the only state cue;
- controls remain readable over bright backgrounds.
