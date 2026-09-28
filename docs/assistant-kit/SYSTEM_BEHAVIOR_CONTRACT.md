# System Behavior Contract

## Playback ownership

Exactly one logical playback session owns a selected track.

Rotation/recreation must not:
- start the track twice;
- create two analyzers;
- create two renderer loops;
- reset position without user intent.

## Visual state

Persist semantic settings:
- selected scene;
- sensitivity;
- smoothing;
- background source;
- center style;
- effect toggles.

Transient frame timing does not need persistence.

## Background/foreground

When app goes to background:
- follow explicit playback policy;
- renderer may pause to save resources;
- returning must reconnect to the same logical playback/analyzer state.

## Long operations

Generated background requests/export jobs must have durable state outside a transient dialog.

Closing a result dialog must not destroy completed output.
