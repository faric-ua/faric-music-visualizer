# FARIC UI lifecycle / rotation audit

Status: active release gate

This document defines the lifecycle contract for every screen, settings panel,
dialog and future floating-control editor in PulseDeck.

## Required rotation contract

After portrait ↔ landscape rotation, every UI surface must restore:

1. the same parent screen/window;
2. the same selected content/theme/item;
3. all user-edited values that were already committed;
4. vertical and horizontal scroll position where applicable;
5. the same playback state without restarting the track;
6. the same logical mode (for example Board theme, export format, projectM mode);
7. no automatic destructive or remote action caused only by rotation.

Rotation must not:
- jump back to Library unless Library was the active screen;
- reset Board transform/reaction values;
- select a different theme;
- restart an export, import, analysis or download;
- trigger a button action;
- duplicate background work;
- lose an open editor's committed values.

## Current screen audit

| Surface | Screen restore | Content/state | Position | Auto-action safety | Status |
| --- | --- | --- | --- | --- | --- |
| Library | restored by MainActivity saved state | playback stays in PlaybackController ViewModel | vertical + category horizontal scroll restored | no action on restore | CODE READY / PHONE PENDING |
| Now Playing | restored by MainActivity saved state | selected theme + playback state preserved | fixed layout; no scroll state needed | no action on restore | CODE READY / PHONE PENDING |
| Playback Themes | restored | selected theme preserved | vertical scroll restored | rotation does not select a theme | CODE READY / PHONE PENDING |
| Board Transform | restored | X/Y/size/rotation/opacity + group music reactions are persistent stores | settings scroll restored | rotation does not apply Reset/Fit/Done | CODE READY / PHONE PENDING |
| Export Lab | restored | selected theme + export aspect ratio restored | vertical scroll restored | export buttons are not auto-run | PARTIAL: offline-analysis result still needs lifecycle-safe ownership |
| projectM Activity | Activity recreates and persistent store restores mode/FG/auto/last preset | last preset/mode/foreground/autoplay persisted | no scrolling editor | preset/library background-work duplication still requires explicit phone audit | PARTIAL / PHONE PENDING |

## MainActivity implementation

MainActivity saves/restores:
- current Screen enum;
- selected PlaybackThemeId;
- ExportAspectRatio;
- active vertical ScrollView position;
- active horizontal HorizontalScrollView position.

Board settings are intentionally not duplicated in Bundle state:
- BoardTransformStore owns layout values;
- BoardGroupReactionStore owns group audio-motion values.

That gives one source of truth for both normal navigation and rotation restore.

## Mandatory audit for every new UI surface

Every new screen/dialog/editor must answer these before release:

- What identifies the active parent screen?
- What user values must survive recreation?
- Is there a scroll/list/carousel position to restore?
- Is any work currently running?
- Can recreation accidentally start that work twice?
- What happens when the window was open during rotation?
- Does Back after rotation return to the same logical parent?
- Does playback continue without a seek/restart?
- Does the restored surface perform zero actions until the user explicitly acts?

## Open lifecycle work

1. Move Export Lab offline-analysis ownership out of MainActivity so an Activity
   recreation cannot lose the completed/in-progress analysis state.
2. Audit projectM library install/index/queue work for duplicate-start behavior
   during Activity recreation.
3. Add instrumentation rotation tests once the current prototype UI stabilizes.
4. Apply this contract to future floating PulseDeck block editors and per-layer
   Board editors.
