# v0.4.1 — Full projectM preset library + FARIC TEST 40

## Full local preset library

ProjectM LAB can now install the complete pinned Cream of the Crop library.

- preset source commit: `0180df21f5e0bd39b9060cc5de420ed2f1f9e509`;
- expected preset count: 9,795 `.milk` files;
- category hierarchy is preserved on the phone;
- related texture pack source commit: `6368812f27bc747b517218fbf89d21d59afce4d9`;
- full library is downloaded once into app-internal storage;
- the install marker is written only after preset-count validation succeeds.

The full preset pack is intentionally not baked into the APK.

## TEST 40

For visual evaluation FARIC creates a deterministic 40-preset subset after the full library is installed:

- 8 Geometric;
- 8 Particles;
- 8 Supernova;
- 8 Waveform;
- 8 Hypnotic.

This is the default ProjectM LAB mode for phone testing.

## LAB controls

- `TEST 40` — focused 40-preset review pack;
- `ВСІ` — complete local 9,795-preset library;
- `NEXT` — next preset;
- tap on the visualization — next preset.

## First-run flow

If the full library is missing:

1. LAB renders the small bundled fallback presets.
2. FARIC downloads the pinned Cream of the Crop archive.
3. FARIC extracts and validates the complete preset set.
4. FARIC downloads and extracts the pinned MilkDrop texture pack.
5. FARIC builds TEST 40.
6. LAB automatically switches to TEST 40.

## Rendering contract

The JNI bridge now passes a texture search directory into libprojectM so presets with external MilkDrop texture references can resolve those assets.
