# Visual Asset Quality Contract

## GF modular asset quality and optional illustrated half-hero (updated 2026-10-09)

**User clarification overrides the earlier mandatory-for-everyone rule.** A source hero pack can have **four or five independent art elements**:

1. `frame` — separate frame, reusable with a different creature;
2. `fx` — separate effect/glow assets;
3. `creature` — the original/full hero;
4. `wordmark` — separate logo/lettering asset, **freely reusable with ANY other hero or frame**;
5. **optional** `creature-half` — only for packs where the user chooses to have an independently illustrated upper-half hero that fits within a frame.

Do **not** generate `creature-half` for every pack automatically. Where requested, the assistant must **draw/reconstruct** the half-body intentionally and complete missing anatomy/armor/lighting in matching style; a crop, mask or simply splitting the existing character is **not** an acceptable newly illustrated fifth element. It is a standalone transparent asset, not fused into the frame.

The source wordmarks/texts include **FARIC**, **FMV**, **FVMP** (verified in the existing catalog). Keep each as its **own reusable transparent graphic in a shared wordmark library**, even if it also belongs to an original themed hero package. The library must also permit cross-pack mixing of frames, effects and heroes. A saved composition records asset identities and per-instance transforms, not destructive edits to original files. Do not restrict a wordmark to the character it originally accompanied. `full` (flattened whole) and `preview` (thumbnail) are **derived outputs**, not members of the four/five editable components.

All reused layers must satisfy the Cyber Shark standard: lossless transparent files, individually clean edges, sufficient resolution, and correct registered alignment. Record provenance/owner pack per asset without restricting its reuse. The current `UserHeroPack.kt` importer still assumes a **fixed** `frame/fx/creature/wordmark/full/preview` pack and does not implement the optional fifth element or general mix-and-match library; **this is a design requirement, not an installed feature**. Wait for the ORIGINAL `Pictures2.zip` before judging/drawing source-specific assets; the prior `FARIC-Heroes-Prepared-v0.19.50.zip` is not final-quality source.

Full behavior contract: [GF_MODULAR_ASSET_LIBRARY_CONTRACT.md](../architecture/GF_MODULAR_ASSET_LIBRARY_CONTRACT.md).

Status: hard project rule.

## Gold standard

**Cyber Shark is the visual-quality benchmark for all future app-owned production graphics.**

The goal is not minimum file size. The goal is clean, premium-looking artwork on the phone and in exported video.

When there is a trade-off between visual quality and a somewhat larger asset, choose visual quality unless there is a measured runtime/memory problem that justifies a change.

## Applies to

This rule applies by default to all production visual assets we create or add, including:
- Graphic Figures (GF): Shark, Panther and future variants;
- Creature, Frame, FX, Wordmark and Background/Glow layers;
- logos and wordmarks;
- visualizer overlays and decorative art;
- full-screen/background art;
- any raster asset that is scaled, composited or exported into video.

Small system icons/thumbnails may use a different size appropriate to their role, but they must still have clean edges and no visible compression artifacts.

## Raster quality target

For layered GF-style square assets:
- use a high-resolution physical source, normally **at least 1024×1024**;
- **Cyber Shark's 1254×1254 lossless assets are the preferred reference size/quality** when practical;
- keep all layers in one GF pack on the same canvas size/alignment so they composite predictably;
- do not ship a 256×256 production layer if it will be enlarged on screen/export;
- preserve the original high-resolution physical file in the repository.

For non-square assets:
- preserve the correct aspect ratio;
- use enough source pixels that the asset is normally downscaled, not enlarged, at target phone/export sizes.

## Encoding

Preferred:
1. **Lossless WebP (VP8L)** for raster art with transparency;
2. PNG when lossless WebP is not appropriate.

Do not use lossy WebP/JPEG-style compression for transparent production art when it creates halos, dirty matte edges, ringing or softened contours.

Do not downsample or recompress an approved asset merely to reduce APK size unless:
- a real size/memory/performance problem was measured;
- the optimized version is visually indistinguishable in phone QA and export QA.

## Alpha / edges

Production art must have:
- real transparent background where transparency is intended;
- no black/grey/colored matte fringe;
- no visible rectangle around the object;
- no dirty semi-transparent edge caused by bad segmentation/compression;
- no accidental opaque pixels outside the intended artwork;
- clean antialiasing comparable to Cyber Shark.

## Physical source of truth

All production graphics must exist as physical files in the repository.

Do not rely on:
- screenshots as the only source;
- Base64 blobs embedded in application code;
- temporary generated previews;
- procedural stand-ins pretending to be the final physical artwork.

Procedural fallback is allowed only as an explicitly temporary fallback/diagnostic path.

## Validation before calling an asset ready

Before an image pack is considered production-ready:
1. file decodes successfully;
2. file header/format is valid;
3. dimensions are appropriate for the target;
4. transparency/alpha is present when required;
5. alpha coverage is non-empty;
6. visual edges are inspected against dark and bright backgrounds;
7. no matte/halo/rectangle is visible;
8. phone preview looks at least as clean as the Cyber Shark reference;
9. exported video preserves the same quality;
10. the physical source remains committed in the project.

## Review rule

A new visual asset is not approved merely because it renders.

It is approved only when the user can look at it on the phone and say the visual quality is acceptable relative to Cyber Shark.

If a new asset is visibly worse than Shark, treat that as an asset-quality bug, not an acceptable optimization.

## Panther migration

Current Panther artwork must be rebuilt/validated against this contract.

Specifically:
- replace low-resolution/lossy wordmark art with Shark-grade high-resolution lossless art;
- replace/validate Frame, FX and full-pack physical files;
- keep the current valid Panther Creature review asset only if its final visual quality meets the Shark benchmark after phone review.

