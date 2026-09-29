# FARIC logo / GF reference pack

Status: reference contract captured from user-supplied images in the Music Graph project chat.

## User intent

The logo/GF family should support two production modes:

### 1. Whole GF
A single finished transparent foreground asset:
- frame / shield / ring / portal;
- creature / mascot;
- FARIC / FMV / FVMP wordmark;
- glow / splash / lightning / accent FX.

Use when the composition is fixed and only needs audio-reactive scale, glow, rotation or pulse.

### 2. Three-part layered GF
Preferred editable 3D-style composition with three independent replaceable layers:

1. **Back / frame**
   - shield, ring, portal, badge or background emblem;
   - slow parallax / rotation;
   - bass expansion and background glow.

2. **Middle / creature**
   - shark, dragon, griffin, panther, eagle, snake/cobra, etc.;
   - independent scale and parallax;
   - beat punch / small pose impulse / glow.

3. **Front / wordmark**
   - FARIC, FMV or FVMP;
   - user-replaceable text variant;
   - shallow 3D depth, glow and beat punch.

Optional FX should initially belong to one of these three layers so the first implementation stays simple. A dedicated fourth FX layer can be added later if needed.

## Visual direction captured from the references

- metallic / game-emblem / cyber-fantasy badge look;
- dark navy / black base;
- cyan / electric-blue glow;
- purple accents;
- optional orange/gold accent variant;
- strong beveled 3D wordmarks;
- shields, rings, portals and crests;
- creatures used as interchangeable mascots;
- compositions should work both as a complete badge and as separated components.

## Wordmark candidates

- **FARIC** — primary brand name.
- **FMV** — FARIC Music Visualizer.
- **FVMP** — user-provided alternate short mark; keep available as a visual variant.

## Source reference set

The following user-supplied images were used as the reference pack in the chat:

- 332872.jpg — shark emblem style reference.
- 333066.jpg — FARIC / FMV / FVMP with blue panther, eagle, cobra, rings and shields.
- 333069.jpg — FARIC / FMV / FVMP component sheets with dragon/griffin/shields/rings.
- 333070.jpg — separated ring + dragon + FVMP wordmark.
- 333071.jpg — FARIC/FMV/FVMP + creature/frame component variants.
- 333072.jpg — green-gold FARIC + dragon/griffin/shield compositions.
- 333073.jpg — component sheet: FARIC/FMV/FVMP, snake/griffin, frames.
- 333074.jpg — complete badge compositions and separated parts.
- 333075.jpg — alternate component combinations.
- 333076.jpg — complete badge examples with FARIC/FMV/FVMP.
- 333077.jpg — large FARIC wordmark + dragon + ornate shield.

## Production requirements for final app assets

Final assets should not use screenshot checkerboards as transparency.

Prepare real transparent PNG/WebP layers with:
- matching canvas dimensions;
- common pivot / center;
- enough transparent margin for beat scale;
- consistent depth order;
- no baked background unless the asset is intentionally a whole GF;
- optional tint-safe glow where practical.

## Audio-reactive behavior idea

For layered GF:
- frame reacts mainly to bass / amplitude with slow scale and glow;
- creature gets a sharper beat impulse plus subtle mid/high detail motion;
- wordmark gets a smaller beat punch, depth/parallax and glow;
- all three layers keep independent intensity multipliers.

This lets one animation preset be reused while changing only text, creature and frame.
