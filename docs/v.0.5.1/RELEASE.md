# v0.5.1 — TOP half-pool + manual background lock + foreground samples

## Background library modes

The complete projectM library remains installed and is not deleted.

FARIC now derives a deterministic half-size background pool called `ТОП`:

- full library: 9,795 presets;
- TOP pool target: 4,898 presets;
- TOP takes every second preset from the sorted full library while preserving relative subfolders;
- TEST 40 also remains stored for QA and is not deleted.

The intent is to reduce the normal automatic background search space without losing access to the complete library.

## Background auto/manual contract

`ТОП` and `ВСІ` start background autoplay.

Pressing `NEXT` switches the background once and locks automatic projectM preset transitions.

After manual NEXT, repeated changes happen only through NEXT/tap until the user explicitly presses `ТОП` or `ВСІ` again.

This uses projectM preset locking, so programmatic NEXT still works while automatic soft/hard transitions are disabled.

## Foreground sample catalog

The existing FARIC foreground is preserved as sample 1:

- Pulse Rays

Two additional first-layer samples are added:

- Orbit Rings
- Spectrum Halo

The `FG` button cycles the first-layer sample without changing the projectM background.

This establishes the future contract that layer 1 samples are a persistent selectable catalog rather than replacing older samples.
