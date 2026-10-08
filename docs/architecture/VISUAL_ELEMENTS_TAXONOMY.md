# FARIC Visual Elements — taxonomy proposal (2026-10-08)

**Status:** UX / architecture proposal, pending user approval for actual menu labels. This file does not change runtime code or existing saved composition formats.

## Problem

Existing `GraphicFigureCatalog` (GF) has `CYBER_SHARK` and `CYBER_PANTHER` and each pack bundles creature/mascot, frame, optional FX and wordmark. Calling all of that "GF" makes it hard to distinguish an illustrated character from procedural audio-reactive geometric figures.

## Recommended visible vocabulary

| User-facing category (UA) | Working English | Meaning | Examples |
|---|---|---|---|
| **Герої** | **Heroes** | Illustrated / animated identities and mascot packs | Cyber Shark, Cyber Panther, future dragon |
| **Аудіоформи** | **Audioforms** | Procedural shapes deforming, glowing, rotating or pulsing with music | bass-reactive blob/ring, circular waveform, liquid shape |
| **Оформлення** | **Decor** | Non-character composition assets that may accompany a hero or audioform | frame, logo, badge, styled background decorations |
| **Ефекти** | **FX** | Overlay/post/compositor effects | particles, rays, glow, flashes |

**Separate, not a subcategory of those four:** `Visualizer / projectM` represents a background/preset engine and its own foreground Center / Edge FX. The audioform engine may share the same audio-feature model without pretending to be projectM or its preset gallery. Likewise, `Board` is the composition workspace rather than a visual element category.

## Legacy data compatibility — mandatory

- Keep existing internal `GraphicFigureCatalog`, `PlaybackThemeId.CYBER_SHARK`, `CYBER_PANTHER`, `BoardLayerId`, old JSON/PREF keys and composition sets as-is in this proposal phase.
- Do not blindly rename `GF` to `Audioforms` in code: **today GF is a hero pack**, not a procedural audioform.
- On migration, use visible `Герої` as the user-facing mapping for existing GF packs; `Аудіоформи` must have its own catalog/model/runtime type and layer ID after designing backwards-compatible persistence.
- Preserve existing user's Cyber Shark / Cyber Panther settings, transforms, assets, frame spin and Composition Sets.
- The first implementation should be additive: new audioform objects with a distinct stable identifier, owner, UI editor and deterministic export renderer. Leave Hero and projectM editor independent.

## Recommended first audioform

**Audioform 01: Reactive Morph Ring** — editable procedural ring/blob with bass-driven radius expansion, low-mid-driven contour undulation, high-frequency shimmer, beat/onset impulse and time-based phase rotation. Independent from Hero illustration. The user can place a Hero above/inside the ring, or use it alone.

The detailed research/testing contract is at `docs/visualizer/AUDIOFORMS_VIDEO_REFERENCE_WORKFLOW.md`. Exact behavior and appearance must be grounded in actual user-supplied videos before replicating it.

## Rollout

1. Agree on vocabulary and place menu labels; no destructive migration.
2. In a separate reference-analysis ChatGPT chat, collect several videos and written specifications, saving approved patterns in GitHub docs.
3. Implement one Audioform proof separately from Hero packs after current high-priority navigation/renderer lifecycle work is stable.
4. Support live preview, saved composition, per-object edit controls, deterministic 3-second and full export QA.
5. Gradually migrate *visible UI terminology only*, with compatibility tests and no automatic rename of legacy stored data.

## Current unrelated work

At 2026-10-08, latest CI-successful app is v0.19.43 / build 132, Android #603 PASS. Its Copy/OK result dialog phone QA is still pending. `PERF-NAV-002` (screen transition freeze), `BUG-EXPORT-001` (full-song exit ~48%) and `PERF-PLAYER-001` remain open; taxonomy/research must not claim to fix any of them.
