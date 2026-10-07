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

## First implementation slice

1. Make live single-choice selector capable of staying open and updating selection in place.
2. Use persistent selector for Cyber Shark / Cyber Panther.
3. Add independent persistent visibility state for `FG Center` and `FG Edge FX`.
4. Split the native foreground shader output so center and edge components can be toggled independently.
5. Add visible controls to the projectM FG screen for Center and Edge.
6. Preserve existing projectM preset selection and FG sample selection.
7. Continue Panther creature diagnostics after this slice builds.

## Phone QA for first slice

- Open GF selector; switch Shark → Panther → Shark → Panther without selector closing.
- Verify current selection mark changes in the open selector.
- Open projectM screen:
  - FG Center ON + Edge ON;
  - FG Center OFF + Edge ON;
  - FG Center ON + Edge OFF;
  - both OFF.
- Cycling FG sample must not automatically re-enable Center/Edge.
- Restart app/projectM screen and verify Center/Edge visibility state persists.
- Capture Panther screen after repeated live switching.

