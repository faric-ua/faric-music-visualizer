# PulseDeck GF — modular cross-pack asset library and composer

**Status:** user-confirmed product/asset contract, 2026-10-09. **NOT implemented** as a general Android editor or shipped production pack.

## Intent

Instead of choosing only a fixed Shark, Panther or one of the ten `Pictures2.zip` hero combinations, the user can **assemble their own Graphic Figure (GF)** from individually selectable, re-usable art assets, including assets originally associated with different heroes. This happens **inside Layer 3 GF**, without modifying the global PulseDeck layer stack or immutable `PULSEDECK_CENTER_CALIBRATION`.

## Source art types versus composed layer instances

| Art type | Expected source form | Can borrow from other packs? | Required in a standard four-part source pack? |
|---|---|---|---|
| `frame` | Frame only, with correctly transparent interior | **Yes** | Yes |
| `fx` | Separate effects, particles, glows | **Yes** | Yes |
| `creature` | Full original hero/character | **Yes** | Yes |
| `wordmark` | Independent lettering/logo (e.g., **FARIC**, **FMV**, **FVMP**) | **Yes** | Yes |
| `creature-half` | **Newly illustrated** upper-half rendition designed to sit in a frame | **Yes** | **No. Optional on chosen characters only** |

The optional fifth part **is NOT automatically required for all ten characters**. When requested, the assistant must **DRAW/extend/reconstruct** its body/armor/lighting and silhouette as new standalone transparent art, not merely crop, mask or halve the original `creature` image. The source hero's identity/design should remain recognizable. The original four-part form is valid on its own.

`full` (flattened source composition) and `preview` (thumbnail) are useful **derived/rendered files** but **not** two extra editable art types. Existing Cyber Shark GF additionally supports `background/glow` as a production sublayer; the four/five count is not a constraint on the global engine's layer count or a demand to remove Background/Glow.

## Wordmark semantics

- Each brand graphic/lettering is stored as **an independent visual asset in a shared wordmark catalog**.
- The original themed pack may include its matching original wordmark and store `sourcePackId`/credit. **That association is informational only, not an exclusive parent.**
- A user can select **FARIC** from a Shark theme, **FMV** from Wolf, **FVMP** from Cobra, or another suitable lettering independently of the currently selected frame/creature.
- Preserve the physical source wordmark art, alpha and typography; do not reconstruct it from a font or conflate different logos without permission.
- First stage: independent **graphic wordmark** (image with transparency), not an assertion that arbitrary editable text/font tooling already exists.

## Reusable asset registry (target schema; illustrative, not current Kotlin implementation)

```text
GFAsset
  assetId: stable immutable string
  type: FRAME | FX | CREATURE | CREATURE_HALF | WORDMARK | BACKGROUND
  sourcePackId: original hero or source collection (provenance only)
  originalName / originalFileHash
  file: lossless alpha artwork path
  intrinsicWidth / intrinsicHeight / registration metadata
  tags / optional preferred placement

GFComposition
  compositionId / userTitle
  instances: [
    { instanceId, assetId, visible, x, y, scale, rotation, opacity, zOrder, maskOrClip? },
    ...
  ]
  optional originalPackId / derivedPreview
```

Different `GFComposition` objects may reference the **same** `GFAsset` without copying or mutating its source bytes. A composition owns *placement*, an asset owns *pixels and provenance*. Do not require frame/creature/wordmark `sourcePackId` equality; do not hardcode one asset of each type as a universal restriction (the user should be able to build a personal combination, including hiding or layering parts). Any grouping, clipping inside a chosen frame, position/scale/rotation/opacity, and asset layering must save and restore consistently.

## Example custom GF

```text
My custom GF — Layer 3
  frame       = Cyber Shark / Frame
  fx          = Inferno Phoenix / FX
  creature    = Thunder Wolf / Creature
  wordmark    = Plasma Cobra collection / FVMP
  creatureHalf= absent (four-art example)

Another custom GF
  frame       = Mecha Panther / Frame
  fx          = Cyber Shark / FX
  creature    = Void Dragon / Creature
  wordmark    = FARIC
  creatureHalf= optional newly drawn Void Dragon upper-half
```

Both the source/full themed GF and user-composed presets remain available. Asset removal or pack migration must not silently replace existing user presets; ensure references are resolved or clearly flagged missing.

## Quality, persistence and rendering acceptance

1. Source-specific assets reconstructed from the user's original `Pictures2.zip`, not the previously rough `FARIC-Heroes-Prepared-v0.19.50.zip`. The original archive is not available in the current session or known GitHub repo.
2. Independent, clean transparent alpha edges; matching registration/resolution, quality at least comparable to Cyber Shark. No contamination by leftover original frame or lettering on a hero-only file.
3. `creature-half` truly illustrated **only where applicable**; visible anatomy is completed and the half-body correctly fits a selected frame.
4. Shared catalog lists all available frames, FX, creatures and wordmarks; each can be selected across source packs and preserves attribution metadata.
5. Changing the wordmark or frame does **not** reset the hero, export settings, selected track, native GL or playback.
6. Custom composition persists independent asset references, order, masks, transforms and visibility; survives navigation and normal app recreation; preview and video export follow the same saved composition.
7. `PULSEDECK_CENTER_CALIBRATION` and the locked PulseDeck HUD remain unchanged.
8. **User's visual PASS is required** before calling the physical artwork production quality; Android CI PASS alone is not art PASS.

## Actual implementation boundary (2026-10-09)

Existing `UserHeroPack.kt` contains a fixed `frame/fx/creature/wordmark/full/preview` import model and files organized under each original hero. Existing Cyber Shark has independently controllable GF sublayers, but there is **no general cross-pack asset composer** and no `creature-half` implementation. This contract is **future work**, not a statement that the current APK already provides the complete feature.

Original source ZIP is required to create the desired new art. No artwork or Android code modifications were made in this specification step.
