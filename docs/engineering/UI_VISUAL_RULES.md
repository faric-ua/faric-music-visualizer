# FARIC UI Visual Rules

These rules are implementation constraints, not suggestions.

## Rule UI-001 — visual centering, not font-baseline centering

Primary player controls and navigation icons must look centered to the eye.

Do not rely on a Unicode glyph's default font metrics for final alignment.

Required for every icon-bearing control:
- fixed or measurable icon box;
- parent gravity = CENTER;
- textAlignment = CENTER when text glyphs are temporarily used;
- includeFontPadding = false for TextView-based icons;
- no accidental asymmetric padding;
- at phone QA, inspect visual center, not only layout bounds.

Preferred for final controls:
- vector drawable or custom Canvas icon with a known viewport;
- Unicode glyphs are prototypes only.

A play triangle may require optical centering because its visual mass is not geometrically symmetric.

## Rule UI-002 — button consistency

Controls in the same family must share:
- icon box size;
- touch target size;
- baseline/center behavior;
- stroke width;
- corner/radius system.

No individual button gets one-off padding merely to hide a layout problem.

## Rule UI-003 — phone acceptance

A control family is not considered PASS from CI alone.

Phone QA must include:
- 100% display scale;
- normal system font size;
- fullscreen mode;
- screenshot review of top actions, transport row, action tiles, PulseDock and bottom navigation.

If an icon is visibly off-center, fix the shared component/rule first rather than patching one screen.
