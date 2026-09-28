# v0.5.2 — stronger FARIC foreground + fast-start preset queue

## FARIC foreground

The three existing foreground samples stay available.

This release increases the movement range instead of replacing the samples:

- bass is now the strongest expansion driver;
- amplitude and beat produce a much larger scale/travel step;
- attack is faster, especially for bass;
- release is smoother so the foreground breathes rather than jitters;
- Orbit Rings and Spectrum Halo receive larger motion ranges.

A new edge-energy system adds solar-flare-like tongues around the screen perimeter.

Solar flare behavior:
- bass controls reach/size;
- beat creates short energetic eruptions;
- highs add fine turbulence/detail;
- flare strength differs by foreground sample.

## projectM startup optimization

Opening projectM no longer requires the GL thread to recursively scan thousands of .milk files before the first frame.

The app now:
1. immediately opens the last valid preset when possible;
2. indexes TOP or ALL on a background thread;
3. keeps CURRENT + 3 NEXT presets in a queue;
4. reads the next three files ahead on a background thread to warm filesystem cache;
5. loads individual prepared preset paths directly into projectM.

This does not pretend to precompile three projectM GPU programs ahead of time — libprojectM still parses/compiles the selected preset when it becomes active. The optimization removes directory scanning and file-I/O work from the critical first-render/NEXT path.

## AUTO / MANUAL ownership

FARIC now owns background timing.

- TOP / ALL => AUTO mode.
- AUTO advances every 18 seconds.
- NEXT => MANUAL mode and cancels AUTO.
- tapping the visualization uses the same manual NEXT path.
- TOP / ALL explicitly restore AUTO.

projectM's internal automatic switching remains locked so there is only one source of truth.

## Persistent state

The app remembers:
- TOP / ALL;
- AUTO / MANUAL;
- last preset path;
- selected FARIC foreground sample.

The remembered preset is used for fast first render before the larger catalog is rebuilt in the background.
