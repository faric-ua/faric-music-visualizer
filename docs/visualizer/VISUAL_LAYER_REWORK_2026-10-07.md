# Visual Layer Rework — 2026-10-07

Status: active design + implementation contract.

This document records the decisions recovered from phone QA videos `606648.mp4` and `606649.mp4` and the conversation around Cyber Panther, Graphic Figures, projectM and FARIC foreground effects.

## Why this exists

The current product has several visual concepts that became mixed together:
- projectM preset/background rendering;
- FARIC projectM foreground figures selected by the `FG` button;
- the always-on edge/perimeter flare effect that was added around those foreground figures;
- the separate main-player FARIC Reactive layer;
- Graphic Figures (GF) such as Cyber Shark and Cyber Panther;
- child layers inside a GF: Background/Glow, Frame, FX, Creature, Wordmark.

The user needs these concepts to be visible in the UI as independent controls instead of being implicitly coupled.

## Phone evidence

### Video 606648 — Cyber Panther
Observed:
- Cyber Panther can be selected in the GF selector.
- The UI reports `Cyber Panther · GF увімкнено`.
- The Layers panel reports all 5 Panther child layers enabled.
- The visible result still does not show a recognizable Panther creature.
- The Panther wordmark does appear.

Conclusion:
- this is no longer a simple parent-layer visibility bug;
- Panther selection/state is working far enough to expose Panther-specific labels/wordmark;
- the remaining bug is in Panther creature asset/render/transform/alpha behavior and must be diagnosed separately.

### Video 606649 — projectM FG screen
Observed:
- projectM preset/background is one visual source;
- the selectable FARIC FG figure in the center is another visual source;
- a third visual component exists at the screen perimeter: the orange/white edge flare/tongue effect;
- the edge effect is currently mixed into the same foreground shader as the selected center FG.

Shader confirmation:
- center figure intensity is selected by `uMode`;
- perimeter effect is calculated separately as `solarFlares`;
- final alpha/color currently combine `intensity + solarFlares`.

Conclusion:
- Center FG and Edge FX are already logically separable in code and should become separately toggleable.

## Canonical terminology

Use these user-facing terms unless a later design explicitly changes them.

### Visualizer
One user-facing group.

Children:
1. **projectM** — preset/background visualization.
2. **FARIC Reactive** — existing main-player reactive scene layer.
3. **FG Center** — selected projectM foreground figure: Pulse Rays, Orbit Rings, Spectrum Halo, Neon Emblem, Energy Core, Orbital Crown, Star Seed, Wave Idol.
4. **FG Edge FX** — perimeter/side flare effect currently mixed into the foreground shader.

Renderer implementation may use separate internal slots/passes, but the UI should expose understandable objects rather than renderer implementation details.

### Graphic Figures (GF)
Separate user-facing group from Visualizer.

Variants:
- Cyber Shark
- Cyber Panther
- future GF variants

Children of the selected GF:
- Background / Glow
- Frame
- FX
- Creature
- Wordmark

Do not call projectM foreground figures `Graphic Figures`. They are **FG Center** samples. GF is reserved for Shark/Panther-style layered art.

## Interaction rule — live selectors stay open

For selectors used to compare visual variants:
- selecting a value applies it immediately;
- the selector must remain open;
- preview updates behind the selector;
- the selector closes only through an explicit Close/Cancel/X action;
- the current selection indicator should update in-place;
- parent tooling should not disappear merely because a visual variant changed.

This applies first to Cyber Shark / Cyber Panther and should be reused for similar live visual selectors.

## Target layer model

User-facing hierarchy:

```
Visualizer
  projectM
  FARIC Reactive
  FG Center
  FG Edge FX

Graphic Figures
  [Cyber Shark | Cyber Panther | ...]
  Background / Glow
  Frame
  FX
  Creature
  Wordmark
```

Implementation detail:
- projectM preset/background and FARIC FG are currently composed inside ProjectMView/native GL;
- FG Center and FG Edge FX should first get independent shader visibility flags;
- a later render-pass split may promote them to fully independent z-orderable compositor layers if needed.

## Asset integrity finding — Panther

Repository byte-level audit found a concrete root cause for the missing creature:

- `cyber_panther_wordmark.webp` is a valid RIFF/WEBP file.
- the previously committed `cyber_panther_creature.webp`, `cyber_panther_frame.webp`, `cyber_panther_fx.webp`, and `cyber_panther_full.webp` did **not** contain valid RIFF/WEBP headers; Android could not reliably decode them.
- this explains why Panther-specific state/wordmark appeared while the recognizable Panther creature did not.

First visible recovery step:
- `cyber_panther_creature.webp` has been replaced by a valid physical 256×256 WebP asset with a clearly recognizable neon Panther head.
- the invalid Frame is now skipped instead of being replaced by the confusing cyan diagnostic circle.
- FX may still use the existing procedural fallback until its physical file is replaced.
- Frame/FX/full physical assets remain a separate cleanup task; do not call the Panther pack fully repaired until those files are replaced/validated.

This replacement is intentionally a phone-review candidate: the user should see it first and decide whether the visual direction is acceptable before the rest of the Panther physical pack is regenerated around it.

## Panther bug contract

Current status: OPEN.

Required diagnostics/fixes:
1. Verify `cyber_panther_creature.webp` decodes.
2. Record bitmap dimensions and alpha/opaque coverage at load time.
3. Verify Board transform and Creature transform are not placing/scaling the creature out of the visible area.
4. Verify Creature alpha after motion × group opacity × layer opacity is non-zero.
5. Compare Panther creature bounds against Shark creature bounds.
6. If the physical creature asset is malformed, replace/re-segment the physical file; do not silently substitute Shark art.
7. Keep physical Panther assets in the repository as the source of truth.

Acceptance:
- selecting Cyber Panther while Creature is enabled shows a clearly recognizable Panther without requiring app restart;
- switching Shark ↔ Panther repeatedly works live;
- chooser remains open during switching;
- export timing reports the selected GF correctly.

## UI layout/text-fit contract

All new controls follow `UI_STYLE_CONTRACT.md`.

Additional requirement:
- live visual selector rows and layer toggles must fit Ukrainian labels without clipping;
- controls use wrap-content/minimum-touch-height behavior;
- visual selector state must remain readable after repeated switching.

## First implementation slice — implemented in v0.19.33 / build 122

1. Live single-choice selector stays open and updates selection in place.
2. Cyber Shark / Cyber Panther uses the persistent live selector.
3. HeroBoardView reloads the selected GF live without rebuilding the Now Playing screen.
4. projectM foreground is split into independent `FG Center` and `FG Edge FX` visibility.
5. projectM preset/background has its own `BG` visibility as a third independent component.
6. The projectM screen exposes `BG`, `CENTER`, and `EDGE FX` controls.
7. Main Layers → Visualizer exposes `projectM`, `FARIC Reactive`, `FG Center`, and `FG Edge FX`.
8. Sets persist/restore the component visibility model.
9. Panther creature physical asset was repaired/replaced for phone review; remaining Panther physical pack integrity is still open.

## Phone QA for first slice

- Open GF selector; switch Shark → Panther → Shark → Panther without selector closing.
- Verify current selection mark changes in the open selector.
- Open projectM screen and independently test:
  - BG ON/OFF;
  - FG Center ON/OFF;
  - FG Edge FX ON/OFF;
  - all meaningful combinations, including BG OFF + Center ON + Edge OFF.
- Cycling FG sample must not automatically re-enable Center/Edge.
- Restart app/projectM screen and verify Center/Edge visibility state persists.
- Capture Panther screen after repeated live switching.


## Selector lifecycle refinement — 2026-10-08

Phone QA showed that keeping the Shark/Panther chooser open after every selection is useful for debugging but not the desired final interaction.

Final rule for the GF picker:
- the **parent Layers / Шари та об'єкти panel stays open**;
- tapping Cyber Shark or Cyber Panther applies the choice immediately;
- the **child GF chooser closes after the selection**;
- returning to the parent Layers panel must show the newly selected GF name immediately;
- the generic child row is labeled **Creature** to avoid stale Shark/Panther text while the parent panel remains alive.

The shared dialog helper may still support persistent live selectors for future tools that truly need side-by-side rapid comparison.

## Benchmark gate

Do **not** request the 3-second / 90-frame export benchmark while Panther is still visually unresolved.

Current performance numbers remain useful engineering baselines, but they are not the final Panther reference.

The next Panther benchmark is required only after:
1. Panther is visibly recognizable on the phone;
2. Shark/Panther selection state is correct;
3. BG / FG Center / FG Edge FX toggles are verified;
4. the Panther production asset direction is accepted at Shark-grade quality.

Then run the standard benchmark:
- 1080×1920;
- 3 seconds;
- 90 frames;
- capture timing text plus exported video.


## Panther asset correction — 2026-10-08

Latest phone video still shows Cyber Shark artwork after selecting Cyber Panther.

A fresh byte-level check of the current repository confirms:
- `cyber_panther_wordmark.webp` is a valid RIFF/WEBP file;
- the current `cyber_panther_creature.webp` is **not** a valid RIFF/WEBP payload;
- therefore Panther Creature cannot be treated as repaired yet.

This supersedes the earlier note that the temporary Panther Creature review asset was valid.

Until a Shark-grade Panther creature is redrawn and committed as a real valid high-resolution image:
- Panther visual QA remains OPEN;
- do not request a Panther performance benchmark;
- do not interpret the selector label/toast as proof that Panther artwork rendered.

