# projectM Authoring Workflow

Status: active product/implementation contract.

## Purpose

The separate projectM TOP/ALL screen is an **authoring / preview screen**, not the final video recorder.

The normal workflow is:

1. Open projectM TOP/ALL.
2. Choose the preset pool and preview presets.
3. Choose the foreground sample.
4. Configure projectM background, FG Center and FG Edge FX.
5. Tune timing and music reaction.
6. Return to the main PulseDeck player to inspect the full composition.
7. Export from PulseDeck. Export must reproduce the stored settings deterministically against the music.

A small amount of preview jitter on the authoring screen is not automatically an export blocker. Export quality/timing is judged separately through the deterministic export pipeline.

## Current visual objects

### Visualizer group

- **projectM / BG**
  - projectM preset/background.
- **FARIC Reactive**
  - separate main-player reactive visualizer layer.
- **FG Center**
  - selected procedural foreground figure:
    - Pulse Rays
    - Orbit Rings
    - Spectrum Halo
    - Neon Emblem
    - Energy Core
    - Orbital Crown
    - Star Seed
    - Wave Idol
- **FG Edge FX**
  - perimeter/side flare effect, independent from FG Center.

## Automatic preset switching

The authoring screen must allow an explicit switch interval.

First supported values:
- **5 seconds**
- **10 seconds**
- **15 seconds**

The chosen value:
- persists between launches;
- is shown on the authoring screen;
- is used by AUTO mode for TOP/ALL;
- is used only for preview/authoring behavior unless export later explicitly adopts automated preset sequencing.

Changing the interval while AUTO is active reschedules the next change immediately.

## FG Center tuning

FG Center is an editable visual object, not a fixed effect.

Current tuning parameters:
- Scale
- Rotation
- Opacity
- Bass reaction gain
- Mid reaction gain
- High reaction gain
- Beat reaction gain

Changes apply live to the projectM preview and persist for the main player/export path.

Future expansion may add:
- X/Y position;
- per-sample tuning presets;
- stereo/pan reaction;
- attack/release;
- color palette;
- animation speed;
- additional music bands.

## FG Edge FX tuning

FG Edge FX is a separate editable object.

Current tuning parameters:
- Opacity
- Bass reaction gain
- High reaction gain
- Beat reaction gain

Future expansion may add:
- edge reach/width;
- tongue density;
- animation speed;
- color palette;
- per-edge masks;
- independent top/bottom/left/right enablement.

## Relationship to Shark/Panther editing

The goal is the same editing philosophy as Graphic Figures:
- each visual object has explicit visibility;
- visual properties are editable;
- music reaction is editable;
- saved state is reused by preview and export;
- objects remain conceptually separate even if the renderer combines them in one native pass.

Do not copy Shark-specific asset concepts onto projectM FG where they do not make sense. For example, FG Center is procedural and does not have a physical Creature bitmap, but it should still expose equivalent authoring controls such as scale, rotation, opacity and reaction.

## Preview lifecycle finding

Phone QA after the projectM activity lifecycle race fix:
- the destructive black/glitched return behavior is fixed;
- preview motion can still feel slightly jerky after opening/returning;
- this is currently classified as **preview smoothness / non-blocking** because final video is produced by Export, not screen recording.

Do not spend major time optimizing this preview jitter until:
- authoring controls are stable;
- deterministic export is verified;
- or the jitter becomes bad enough to prevent useful visual tuning.

## Persistence / recovery

These settings are project state and must survive:
- leaving the projectM screen;
- returning to the main player;
- app restart;
- composition Sets where applicable.

Source of truth:
- projectM preview state: `ProjectMStateStore`;
- main/export must read the same stored FG tuning.

## Phone QA

For the first tuning implementation:

1. Set AUTO to 5s, confirm preset changes roughly every 5 seconds.
2. Switch to 10s and 15s and confirm persistence after leaving/reopening.
3. Open FG TUNE -> FG Center:
   - change Scale;
   - change Rotation;
   - change Opacity;
   - exaggerate Bass/Beat reaction and confirm visible response.
4. Open FG TUNE -> FG Edge FX:
   - change Opacity;
   - exaggerate Bass/High/Beat reaction.
5. Return to the main player and confirm the same tuned result is used there.
6. Export only after the authoring result is visually accepted.


## Authoring panel UX refinement — 2026-10-08

Phone QA showed the first Center/Edge dialogs were visually correct but too large and modal: they covered the exact composition area the user was trying to tune.

Final authoring UX contract:
- use one **movable PulseDeck-styled projectM settings panel** instead of separate blocking Center/Edge dialogs;
- panel header is draggable and its last X/Y position persists;
- panel can be reset to its default position;
- panel is non-modal / non-dimming so the live composition remains visible behind it;
- tabs inside one panel: **AUTO / CENTER / EDGE FX**;
- Center/Edge parameter edits apply live without closing the panel;
- the same panel is available from the separate projectM authoring screen and from the main PulseDeck Layers panel;
- Main Layers -> Visualizer gets its own **⚙** button, analogous to the GF settings gear, so projectM/FG can be tuned while looking at the full composite player.

AUTO semantics are explicit:
- the previous screen could show `MANUAL` while the lower button still said `5s`; in that state no automatic change was expected, which was confusing;
- the lower control now shows **AUTO OFF** when manual mode is active and **AUTO 5s / AUTO 10s / AUTO 15s** when automatic switching is active;
- tapping the AUTO interval control while manual re-enables AUTO using the current interval;
- while AUTO is active, tapping cycles 5 -> 10 -> 15 -> 5;
- choosing 5/10/15 inside the movable AUTO tab always enables AUTO immediately.

Exit lifecycle refinement:
- do not block the UI thread waiting for the GL renderer to be destroyed;
- Back starts a queued GL release and finishes the activity from the release callback;
- this preserves the no-race ownership fix while avoiding the visible UI freeze caused by `releaseProjectMBlocking()` on the main thread.

Phone QA:
1. Open projectM settings and drag the panel to all screen sides; confirm preview remains visible and interactive enough to judge changes.
2. Close/reopen and confirm panel position is remembered.
3. Set MANUAL with NEXT; confirm button says AUTO OFF.
4. Tap AUTO OFF; confirm it becomes AUTO <current>s and presets start changing.
5. Verify 5s/10s/15s timing.
6. Open Layers on the main player; Visualizer row must show a gear; open it and tune Center/Edge over the full composite.
7. Back from projectM using both PulseDeck Back and system Back; there should be no blocking/frozen pause before return and no late renderer corruption.


## Export sequencing TODO

The 5 / 10 / 15 second AUTO interval currently governs the projectM authoring/browser preview.

The user's intended final workflow is broader: after tuning the composition, **Export should reproduce the authored behavior**, including timed projectM preset changes when AUTO is enabled.

Do not silently claim this is already implemented.

Required future export behavior:
- when AUTO is OFF: export the selected/current projectM preset as today;
- when AUTO is ON: deterministic export changes projectM presets on the stored 5/10/15 second boundaries;
- preset order must be deterministic for a given export run / Composition Set;
- transitions must use export timeline time, never wall-clock preview time;
- output must remain reproducible even when the live player is paused;
- Composition Sets must preserve the AUTO flag, interval and enough preset-sequence state to reproduce the intended result.

Status: TODO after current movable-panel / AUTO phone QA.


## v0.19.39 individual reset and auto-spin — implementation contract

- Numeric fields in the movable projectM settings panel use the shared **minus / numeric value / plus** row.
- **Tap the numeric value** to reset **only that parameter** to its default. The existing **Скинути Center / Скинути Edge FX** group buttons remain unchanged. The reset uses the same live persistence callback as the +/- controls.
- **FG Center → Автообертання** controls angular velocity in **degrees per second** in the inclusive range **-180 .. +180**, with **5°/s** steps. **0°/s** is OFF. Negative speed reverses direction.
- The manual **Поворот** angle remains independent from the ongoing animated rotation.
- Auto spin persists in `ProjectMStateStore`, Composition Sets and the main player authoring panel; the native foreground shader receives a frame-timeline-derived modulo-360 spin angle to avoid precision loss and preserve export timeline determinism.
- Behavior requires physical phone validation: tap an altered value -> only it resets; choose + speed -> FG Center rotates; choose - speed -> opposite direction; choose 0 -> stops; save/reload a Composition Set; test three-second Export proof.
- This is separate from projectM background AUTO preset changes (5/10/15s). That deterministic preset-sequence Export feature remains TODO.
- A user-reported full-song export app exit at ~48% remains an independent HIGH-priority open issue. Do not characterize it as resolved by this UI update. See `OPEN_FINDINGS.md` BUG-EXPORT-001.


## v0.19.40 nested-panel / navigation lifecycle phone contract

- In main player open PulseDeck tools → Layers → Visualizer ⚙. The parent Layers dialog must **remain open** underneath the movable projectM panel; closing projectM X returns to Layers with its selections and position unchanged.
- Separate projectM screen loads the physical Back icon via its correct packaged assets path `pulsedeck_hud/utility/back.png`, not `skin/pulsedeck_hud/utility/back.png`. System Back and physical Back use the same exit path.
- Player → projectM no longer calls `releaseProjectMBlocking(1500)` on the main thread. It queues release and navigates on callback; repeat taps are ignored while transfer is pending to avoid double launch.
- ProjectM → player still waits for async GL release **before** finishing to prevent destruction of the newly built main renderer. The pending state is visible. This is not proof that native renderer initialization has zero visible delay.
- Phone QA must repeat both directions, both Back methods and nested panel reopening, and verify background is not corrupted on return.
- Video evidence: `606815.mp4` nested menu, `606816.mp4` activity transitions, `606817.jpg` missing Back.

## Phone QA — signed FG Center auto-spin and per-value reset (2026-10-08)

User reported **5/6 PASS on v0.19.39**, including tap Scale value to 1.00x without changing other parameters; +30°/s spin, -30°/s reverse, 0°/s stopped; persistence on panel reopen / grouped Composition Set test; main-player spin and successful 3-second MP4 export. **Back untested for v0.19.39**, remains pending independently. v0.19.41 contains these controls and adds a fixed-footer export report (CI PASS, phone pending). This evidence does not establish deterministic 5/10/15s projectM preset switching during full-song export.

## v0.19.42 — Board Frame auto spin contract

- Frame layer has an independent saved signed angular velocity -180°/s..+180°/s with default 0. Static manual `Поворот шару` stays independent; signed velocity does not move other layers. Frame's pre-existing 1.15°/s baseline auto drift is replaced by user-authored velocity; beat rotation remains independent.
- Saved state and Composition Sets store `spinSpeed` for each layer. The same `BoardLayerTransform.autoRotationOffsetAt(timeSeconds)` helper is used in live CPU canvas, export CPU Canvas and export GPU direct path. Timeline rotation is modulo 360 degrees.
- Selector `BG/Glow` uses an independently wider, one-line tab and retains horizontal scroll.
- On recent phone evidence, top-left app Back in projectM is now visible; navigation stalls are still reported. A subsequent targeted perf/lifecycle investigation is required rather than claiming prior v0.19.40 navigation fix complete.
