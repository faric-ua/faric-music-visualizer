# FARIC Visual Asset Index

Captured: 2026-09-30

This directory preserves the visual direction discussed for FARIC Music Visualizer / PulseDeck.

## Repository storage policy

The Git repository keeps **lightweight visual catalogs and canonical design references** so project history remains visible without filling Git with many multi-megabyte generation masters.

Current committed visual catalogs:
- `catalogs/USER_GF_REFERENCE_CONTACT_SHEET.webp` — real thumbnail contact sheet of the user-supplied GF/logo references;
- `catalogs/GENERATED_GF_CATALOG.webp` — real thumbnail contact sheet of the generated FARIC/FMV/FVMP concepts;
- `../../design/pulsedeck/references/PULSEDECK_UI_REFERENCE_332780.webp` — PulseDeck UI reference;
- `../../diagrams/FARIC_PROJECT_ATLAS_PREVIEW.webp` — compact project/asset overview.

The logical source list below is the stable index. Full-resolution production masters should be exported individually only when a concept is promoted into an app-ready asset.


## Master originals

The individual master JPG/PNG files are **not all committed to Git yet**. The repository currently stores lightweight preview/contact-sheet copies for browsing and documentation.

The current chat/session master set has been inventoried separately:
- `MASTER_ASSET_MANIFEST.md` — exact filenames, byte sizes and SHA-256 hashes;
- `MASTER_ASSET_STORAGE.md` — long-term storage and phone-sync policy.

Current inventoried master set: **34 files / 61.82 MiB**.

The long-term goal is that master files live in a persistent Git-accessible asset archive (preferably a separate asset repository or Git LFS-backed storage), so they can be pulled to the phone and never depend on chat-session storage.

## User reference source set

Recorded source IDs:
- `333062`
- `333063`
- `333064`
- `333065`
- `333066`
- `333069`
- `333070`
- `333071`
- `333072`
- `333074`
- `333075`
- `333076`
- `333077`
- `333096`

Shared visual DNA:
- gaming / emblem composition;
- metallic beveled wordmark;
- creature / mascot;
- shield / ring / portal frame;
- cyan / electric-blue glow;
- purple and orange/gold variants;
- whole-GF and modular-GF construction.

## Generated FARIC concepts

- Cyber Shark v1 / v2
- Cyber Panther v1 / v2
- Mecha Tiger
- Void Dragon v1 / v2
- modular FARIC serpent/dragon concept

## Generated FMV concepts

- Neon Griffin v1 / v2
- Thunder Wolf
- Razor Raven
- Inferno Phoenix
- modular FMV griffin concept

## Generated FVMP concepts

- Plasma Cobra variants
- Titan Scorpion
- modular FVMP void/cosmic serpent concept

## UI reference

PulseDeck reference:
`docs/design/pulsedeck/references/PULSEDECK_UI_REFERENCE_332780.webp`

Canonical accepted editable baseline:
`docs/design/pulsedeck/prototypes/PULSEDECK_SKIN_V1.svg`

## Promotion path: concept → production Hero/GF

When a concept graduates from visual reference into an app asset, create a dedicated folder:

```text
hero-name/
├── full.webp            # complete whole GF
├── frame.webp           # back layer
├── creature.webp        # middle layer
├── wordmark.webp        # front layer
├── fx.webp              # optional fourth layer
└── manifest.json        # canvas / pivots / audio routing / version
```

Recommended contract:
- same canvas dimensions across modular parts;
- common pivot/origin;
- safe transparent margins;
- stable z-order;
- explicit audio-reactive routing;
- source/provenance note;
- versioned manifest.

This lets the app swap text, creature and frame independently without visual jumps.
