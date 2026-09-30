# FARIC Music Visualizer — START HERE

Canonical entry point for every new assistant/session.

The repository must be sufficient to recover project context without relying on old chat memory.

## Mission

Build an Android-first music visualizer that reacts in real time to audio.

Primary visual concept:
`music → audio features → beat/energy events → scene state → renderer`

The app should support a portrait full-screen presentation inspired by music visualizer videos:
- animated or generated background;
- central cover/logo/orb;
- radial light/shape reaction;
- beat-synced pulses;
- smooth motion between beats.

The product direction now also includes a configurable multi-layer Board, a separate configurable PulseDeck Skin overlay, music/video playback, and deterministic video export. The detailed capture is in `docs/architecture/FARIC_LAYERED_BOARD_VISION.md`.

## Mandatory reading order

1. `CURRENT_HANDOFF.md`
2. `ACTIVE_PLAN.md`
3. `docs/product/PRODUCT_VISION.md`
4. `docs/architecture/ARCHITECTURE.md`
5. `docs/architecture/FARIC_LAYERED_BOARD_VISION.md`
6. `docs/visualizer/assets/ASSET_INDEX.md`
7. `docs/diagrams/FARIC_PROJECT_ATLAS.md`
8. `VISUALIZER_ASSISTANT_WORKFLOW.md`
9. `PROJECT_STATUS.md`
10. `BACKLOG.md`
11. `OPEN_FINDINGS.md`
12. `docs/assistant-kit/SIBLING_PROJECT_REFERENCE_RULE.md`
13. `docs/assistant-kit/DOCUMENTATION_DISCIPLINE.md`
14. `docs/assistant-kit/SYSTEM_BEHAVIOR_CONTRACT.md`
15. `docs/assistant-kit/UI_CONTRACT.md`
16. `docs/assistant-kit/AUDIO_VISUAL_CONTRACT.md`
17. `docs/assistant-kit/APK_BUILD_CONTRACT.md`
18. current release folder under `docs/v.*`

## Sources of truth

Prefer:
1. real phone behavior for visual timing/performance/lifecycle;
2. verified CI/build evidence;
3. current GitHub code;
4. current contracts/status docs;
5. historical release docs;
6. old chat memory.

Never call a visual sync behavior PASS without real playback evidence.

## Sibling projects

Read-only engineering references:
- `faric-ua/YTM`
- `faric-ua/renault-docs-android`
- `faric-ua/faric-music-graph`

Do not modify them while working on this project.
Do not reuse their signing keys, secrets or package IDs.
