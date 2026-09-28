# FARIC compositor foundation

## Current v0.5.0 composition

Stable libprojectM 4.1.7 stays the background engine. FARIC renders its own foreground immediately after projectM in the same OpenGL frame.

```text
projectM RenderFrame()
        ↓
background in default GL framebuffer
        ↓
FARIC foreground shader
        ↓
SurfaceView
```

## FARIC foreground v1

The first native foreground shader receives:

- amplitude;
- bass;
- mid;
- high;
- beat impulse;
- elapsed time;
- render resolution.

It draws a reactive pulse ring, radial rays, high-frequency sparkles and the FARIC orange/cyan accent blend.

Attack is intentionally fast while release is slower, so transients feel immediate.

## projectM API boundary

Stable v4.1.7 renders through the default framebuffer in its public C API. The upstream development branch has a user-FBO render entry point, but FARIC does not move to unreleased engine code only for that feature.

For the first compositor proof we use sequential rendering into the same framebuffer.

A later true offscreen compositor can use one of three paths:

1. a future stable projectM release with public user-FBO rendering;
2. a small pinned local upstream patch;
3. a managed copy of the projectM output into a FARIC-owned texture.

## Performance boundary

projectM remains the cheaper background layer using the balanced mobile profile from v0.4.2.

The FARIC foreground is one fullscreen shader pass with no per-frame texture allocation and no CPU-side geometry generation.
