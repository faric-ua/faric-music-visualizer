# v0.6.4 — Smooth projectM preset transitions

## Goal

Make NEXT / AUTO transitions between projectM presets feel intentional and smooth without reintroducing expensive projectM dual-preset soft cuts.

## Transition model

FARIC now owns the visible transition:

1. fade a dark veil in for 170 ms;
2. load the next preset directly;
3. keep the veil over the expensive load if needed;
4. fade the new preset in over 320 ms.

The veil reaches 88% opacity rather than full black, so the transition keeps some visual continuity.

## Why not projectM soft-cut

projectM's own smooth transition can render two presets during the cut and makes performance diagnosis harder.

FARIC therefore:
- uses a direct projectM preset load;
- measures the actual LOAD time;
- masks that load with a lightweight UI fade;
- keeps HEAVY preset learning intact.

This means a 200 ms preset should feel nearly continuous, while a 1200+ ms preset looks like a deliberate held transition instead of a frozen frame.

## Controls

During a preset transition:
- repeated NEXT taps are ignored;
- TOP/ALL changes are ignored until the transition completes;
- rating actions are ignored until the transition completes;
- FG remains independent.

## Phone acceptance

Check manual NEXT, AUTO, and hidden-preset advance.
