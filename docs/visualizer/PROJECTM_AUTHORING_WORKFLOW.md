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

