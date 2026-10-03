# PulseDeck Object Layout Baseline

Status: APPROVED CENTER TEMPLATE / SCALE PASS PENDING PHONE REVIEW
Date: 2026-10-03

## Source of truth

The approved phone-exported object-center template is stored at:

`skin/pulsedeck_hud/object_templates/PulseDeck_object_template_centered_v1.json`

It uses schema `pulsedeck-object-template-v1` and mode `modular-object-offsets`.
The exported dx/dy values are preserved as supplied by the phone and must not be
recalculated from the older center-calibration reference.

The immutable `PULSEDECK_CENTER_CALIBRATION` remains a separate geometric
reference. This object-template baseline is the accepted visual correction
layer on top of the renderer geometry.

## Scale contract

Scaling must preserve each object's approved center.

- C-17 Theme: 150% width / 150% height.
- C-18 Board: 150% width / 150% height.
- C-19 Visualizer: 150% width / 150% height.
- C-20 Export: 150% width / 150% height.
- C-13 Play state: 170% width / 170% height.
- C-13 Pause state: 170% height; width is 20% narrower than the 170% result,
  therefore effective width scale = 1.70 * 0.80 = 1.36.

Do not modify C-13 center when switching between Play and Pause.

## Constructor contract

Object Constructor is the precision editor for real HUD objects. Normal tap
selects only. Long press toggles group membership. Movement is performed only
through explicit MOVE controls. The selected primary object has full-screen
horizontal/vertical dashed center axes and live X/Y coordinates.

## Acceptance

The center template is accepted and frozen.
The scale changes require phone visual acceptance before they become the next
approved visual baseline.


## S1-S6 section grouping

Object Constructor sections follow the approved cut map:
- S1 HEADER: C-01 Back, C-02 Header title, C-03 Menu.
- S2 HERO / ENERGY: C-04 Hero reactor.
- S3 METADATA: C-05 Favorite, C-06 Track info, C-07 Track more.
- S4 WAVE / SEEK: C-08 Waveform, C-09 Progress.
- S5 TRANSPORT: C-10 Transport rail, C-11 Shuffle, C-12 Previous, C-13 Play/Pause, C-14 Next, C-15 Repeat.
- S6 QUICK ACTIONS: C-16 Quick-actions rail, C-17 Theme, C-18 Board, C-19 Visualizer, C-20 Export.

GROUP selects the complete section containing the current primary object.
UNGROUP leaves the primary object independently editable. Section grouping does
not rewrite the approved object-center template.

## Main-page action wiring

Current real actions: Back, Menu/tools, Seek, Play/Pause, Repeat One, Theme,
Board, Visualizer and Export. Favorite and Track More remain future product
features. Shuffle / Previous / Next require a real playback queue and must not
fake queue behavior while PlaybackController owns only one MediaItem.
