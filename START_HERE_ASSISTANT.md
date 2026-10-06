# FARIC Music Visualizer — START HERE

## NEW CHAT RESUME — 2026-10-06

**Read this block first. It overrides stale historical sections below.**

- Project/repo: `faric-ua/faric-music-visualizer`, branch `main`.
- Current release: **v0.19.11 / build 100**.
- Exact app/source SHA: `5b9c00b45993a01fe0be0922de8714db1c758e97`.
- Android #487: **PASS**; Validate #813: **PASS**.
- Artifact: `FARIC-Music-Visualizer-v0.19.11-Debug`, id `11423002180`, digest `sha256:1b4ccb6c9619073f8629082819cc521abe8a98f766f87df2e3f853a8cf9bc897`.
- v0.19.11 change: export timing dialog now has **«Копіювати текст»**. It copies the complete timing block to Android clipboard and must keep the dialog open. Phone acceptance of this button is still pending.
- User's normal Termux path is **3 → 10 → 8**: update repo → Android build status → download APK/open folder. Do not tell the user to use item 9 unless a new build actually needs to be dispatched.
- Immediate user action in the next chat: install v0.19.11 via **3 → 10 → 8**, run one 3-second preview, tap **«Копіювати текст»**, paste the result into chat, and confirm the dialog stays open.
- Last accepted performance diagnosis (v0.19.10): projectM total **5270 ms** = queue wait **16**, native render **730**, **glReadPixels 4361**, bitmap copy **97**. Total export **15287 ms**.
- Therefore the next engineering target after clipboard UX PASS is specifically **GPU→CPU projectM readback / glReadPixels**, not native projectM render.
- Last composition profile: projectM draw ~**1651 ms**, Cyber Shark ~**4314 ms**, effects ~**448 ms**, HUD draw ~**1255 ms**.
- Preserve export contracts: 3-second preview starts at the current playback scrubber position; full-song export starts at 0:00; final target remains 1080×1920-class / 30 FPS / H.264 + AAC. Do not reduce final FPS/quality just to improve speed without explicit discussion.
- Keep `PULSEDECK_CENTER_CALIBRATION` immutable. Template Constructor work must not modify the calibration baseline.
- After every successful Android build of our work, write `BUILD_CHECKPOINTS.md` immediately before proceeding.
- This is PulseDeck/music visualizer, **not YTM importer**; do not apply the YTM visible 3-part work rule here.

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
2. `BUILD_CHECKPOINTS.md`
3. `ACTIVE_PLAN.md`
4. `docs/product/PRODUCT_VISION.md`
5. `docs/architecture/ARCHITECTURE.md`
6. `docs/architecture/FARIC_LAYERED_BOARD_VISION.md`
7. `docs/visualizer/assets/ASSET_INDEX.md`
8. `docs/diagrams/FARIC_PROJECT_ATLAS.md`
9. `VISUALIZER_ASSISTANT_WORKFLOW.md`
10. `PROJECT_STATUS.md`
11. `BACKLOG.md`
12. `OPEN_FINDINGS.md`
13. `docs/assistant-kit/SIBLING_PROJECT_REFERENCE_RULE.md`
14. `docs/assistant-kit/DOCUMENTATION_DISCIPLINE.md`
15. `docs/assistant-kit/SYSTEM_BEHAVIOR_CONTRACT.md`
16. `docs/assistant-kit/UI_CONTRACT.md`
17. `docs/assistant-kit/AUDIO_VISUAL_CONTRACT.md`
18. `docs/assistant-kit/APK_BUILD_CONTRACT.md`
19. current release folder under `docs/v.*`

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
