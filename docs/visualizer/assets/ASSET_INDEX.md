# FARIC Visual Asset Index

Captured: 2026-09-30

This directory preserves the visual direction discussed for FARIC Music Visualizer / PulseDeck.

## Important distinction

Files committed here are **repository working previews / references**. They are intentionally smaller than the original chat/generation masters so the Git repository stays practical.

Production assets used by the app should later be regenerated/exported at the required resolution with real transparency, common pivots and layer-safe margins.

## User reference pack

Location: `references/user/`

Captured files:
- `333062.webp`
- `333063.webp`
- `333064.webp`
- `333065.webp`
- `333066.webp`
- `333069.webp`
- `333070.webp`
- `333071.webp`
- `333072.webp`
- `333074.webp`
- `333075.webp`
- `333076.webp`
- `333077.webp`
- `333096.webp`

These define the preferred visual family:
- gaming/emblem composition;
- metallic beveled wordmarks;
- creature mascot;
- shield/ring/portal frame;
- cyan/electric-blue glow;
- purple and orange/gold variants;
- whole-GF and modular-GF construction.

## Generated FARIC concepts

Location: `generated/faric/`

- `cyber-shark-v1.webp`
- `cyber-shark-v2.webp`
- `cyber-panther-v1.webp`
- `cyber-panther-v2.webp`
- `mecha-tiger.webp`
- `void-dragon-v1.webp`
- `void-dragon-v2.webp`
- `modular-set-v1.webp`

## Generated FMV concepts

Location: `generated/fmv/`

- `neon-griffin-v1.webp`
- `neon-griffin-v2.webp`
- `thunder-wolf.webp`
- `razor-raven.webp`
- `inferno-phoenix.webp`
- `modular-set-v1.webp`

## Generated FVMP concepts

Location: `generated/fvmp/`

- `plasma-cobra-v1.webp`
- `plasma-cobra-v2.webp`
- `plasma-cobra-alt.webp`
- `titan-scorpion.webp`
- `modular-set-v1.webp`

## UI reference

PulseDeck reference screenshot is stored separately under:
`docs/design/pulsedeck/references/PULSEDECK_UI_REFERENCE_332780.webp`

The canonical accepted design source remains:
`docs/design/pulsedeck/prototypes/PULSEDECK_SKIN_V1.svg`

## Production-ready target structure

When a concept graduates from reference into an app asset, use a folder like:

```text
hero-name/
├── full.webp            # ready-made whole GF
├── frame.webp           # back layer
├── creature.webp        # middle layer
├── wordmark.webp        # front layer
├── fx.webp              # optional fourth layer
└── manifest.json        # pivots, canvas, audio routing, version
```

A modular set should use matching canvas dimensions and common pivot coordinates so parts can be swapped without visual jumps.
