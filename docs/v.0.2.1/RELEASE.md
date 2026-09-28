# v0.2.1 — PulseDeck fullscreen startup hotfix

## Reason

The first fullscreen/responsive v0.2.0 candidate installed on the phone but Android reported that FARIC Music Visualizer closed because of an error.

## Changes

- use AndroidX WindowCompat / WindowInsetsCompat for immersive mode;
- apply fullscreen only after the content view exists;
- guard immersive/inset operations so a device-specific insets failure cannot crash startup;
- keep the responsive card/PulseDock sizing work from v0.2.0;
- versionCode 3 / versionName 0.2.1.

## Acceptance

- app launches without Android crash dialog;
- fullscreen is active;
- transient system bars can be revealed by swipe;
- Library content is not clipped;
- local playback still works;
- Now Playing / PulseDock remain functional.
