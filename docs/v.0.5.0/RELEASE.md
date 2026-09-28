# v0.5.0 — First real FARIC compositor

## Goal

Prove the final visual direction on the phone:

- projectM = background;
- FARIC = foreground;
- both react independently to the same track.

## Implementation

projectM renders first. A native GLES foreground shader renders immediately after it and blends additively over the projectM frame.

The foreground receives the existing FARIC audio analysis signal:

`amplitude / bass / mid / high / beat`

## Foreground v1

- pulse ring;
- radial rays;
- high-frequency sparkles;
- orange/cyan FARIC palette;
- fast attack / slower release.

This is not the final foreground art. It is the first compositor proof for evaluating the actual two-layer product.

## Engine choice

libprojectM remains pinned to stable v4.1.7. The stable public C API does not expose the user-FBO method present on upstream development, so v0.5.0 keeps stable projectM and composes sequentially in the same framebuffer.

## Next after phone PASS

- foreground style selector;
- foreground opacity/intensity control;
- 3–5 original FARIC foreground scenes;
- true offscreen projectM texture composition when the integration path is stable.
