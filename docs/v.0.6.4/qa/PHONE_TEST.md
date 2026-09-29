# v0.6.4 Phone Test

## Smooth transition
- [ ] Open projectM and wait for PRELOAD 3/3.
- [ ] Press NEXT.
- [ ] Old preset fades away smoothly.
- [ ] New preset fades in smoothly.
- [ ] No abrupt hard cut is visible.
- [ ] Status still reports LOAD N ms.

## Heavy preset
- [ ] Find a preset with LOAD >= 1200 ms if possible.
- [ ] During its load, the old frozen frame is mostly hidden by the transition veil.
- [ ] New preset fades in after load completes.
- [ ] HEAVY learning still records the preset.

## AUTO
- [ ] TOP enables AUTO.
- [ ] Automatic transition uses the same smooth visual fade.
- [ ] NEXT switches to MANUAL afterward as before.

## Interaction
- [ ] Rapid repeated NEXT taps do not queue multiple transitions.
- [ ] FG can still be changed independently.
- [ ] 👍 / 👎 / − work normally after transition completes.
