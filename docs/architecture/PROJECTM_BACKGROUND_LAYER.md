# projectM as FARIC background layer

## Locked direction

projectM is the animated **background visual layer**.

FARIC will add its own **foreground reactive layer** later.

```text
Music PCM
   |
   +---------------------------+
   |                           |
projectM                    FARIC analysis
   |                           |
background texture          foreground renderer
   |                           |
   +----------- compositor ----+
               |
           PulseDeck UI
```

## Responsibilities

### projectM background

- MilkDrop preset library;
- large-scale motion, plasma, geometry, particles and texture-driven scenes;
- random/playlist preset selection;
- intentionally allowed to render below native screen resolution because it is a background layer.

### FARIC foreground

- our own visual identity;
- precise bass/mid/high transient response;
- foreground particles/rays/objects;
- track/album/AI/user artwork interactions;
- full-resolution UI-safe rendering.

## Composition rule

projectM must not own the final UI surface long-term.

The target compositor renders projectM into an offscreen OpenGL texture/FBO and then composites FARIC foreground above it. projectM documents support rendering to a texture; the integration must keep the final PulseDeck controls outside projectM.

## Mobile performance rule

Because projectM is a background layer, background quality is intentionally lower than foreground/UI quality.

Default phone profile:

- render scale: 78%;
- projectM mesh: 72x40;
- target preset FPS metadata: 60;
- soft transition: 0.70 s;
- projectM rendering stops when its visual surface is not visible.

Profiles:

- ECO: 62%, mesh 48x32, 30 FPS target;
- BALANCED: 78%, mesh 72x40, 60 FPS target;
- QUALITY: 100%, mesh 96x54, 60 FPS target.

## Next optimization step

Add AUTO quality selection using measured frame time:

- drop one quality tier when sustained FPS is below target;
- only raise a tier after a long stable period;
- remember heavy presets and avoid them in battery/thermal mode.

## Audio path

Current spike uses Android Visualizer waveform capture converted to PCM.

Production target is direct decoded PCM from Media3/TeeAudioProcessor so projectM receives cleaner stereo data without depending on Android Visualizer capture.
