# FARIC Music Visualizer — START HERE

## LATEST CANDIDATE — v0.19.13 / build 102 — Cyber Shark profiler

- App/source SHA: `8f374445a0dc566a924c5a275f42c9914664eba9`.
- Android #489 PASS, run `37501072748`; Validate #820 PASS.
- Artifact: `FARIC-Music-Visualizer-v0.19.13-Debug`, id `11429668183`, digest `sha256:47d279c73ee4ab920c784a4f6a6491d823c4637a42834b32ef4864dfe30d46d4`.
- v0.19.12 phone performance PASS: projectM provider 5295 -> 107 ms; total 14925 -> 9915 ms (-33.57%); glReadPixels remained ~4.42 s, confirming successful overlap.
- v0.19.13 makes no intended visual change. It adds `Cyber Shark internals`: background, frame, FX, creature, wordmark.
- Next phone action: Termux 3 -> 10 -> 8; install v0.19.13; run the same warm-cache 3-second / 90-frame preview; paste the full copied timing block.
- Do not optimize Cyber Shark further until the sublayer numbers are measured.

## LATEST RESULT — v0.19.12 phone performance PASS — 2026-10-06

- 1080x1920 / 90 frames.
- projectM provider wait: **107 ms** versus 5295 ms baseline (-97.98%).
- projectM internals stayed effectively unchanged: queue 17, native 756, glReadPixels 4420, bitmap copy 100 ms.
- composition: 7829 ms.
- total: **9915 ms** versus 14925 ms baseline (-5010 ms / -33.57%, ~1.51x faster).
- projectM BGRA: yes.
- Composition leaders: Cyber Shark 4382 ms; projectM draw 1729 ms; HUD draw 1167 ms.
- Conclusion: the two-slot pipeline successfully overlaps projectM GPU->CPU readback with CPU composition. glReadPixels itself is still expensive but is mostly hidden from top-level wall time.
- Performance QA is PASS. Full visual/audio phone acceptance is not claimed because the user did not explicitly report orientation/colors/frame order/audio in the benchmark message.
- Next engineering step: add internal Cyber Shark timing (background / frame / FX / creature / wordmark) and optimize the measured dominant sublayer instead of guessing.

## LATEST RESUME — v0.19.12 / build 101 — 2026-10-06

**This block overrides the older NEW CHAT RESUME below.**

- App/source SHA: `86750315d89c6ebb3b332e912ddc92a1759b3cbb`.
- Android #488 PASS, run `37490177560`; Validate #816 PASS.
- Artifact: `FARIC-Music-Visualizer-v0.19.12-Debug`, id `11425346915`, digest `sha256:9e347528bef24ab8e5a9f706542ef7e5548222ba8ebe187703d70f3f58c57d03`.
- v0.19.11 clipboard action is phone-proven to copy the complete timing block because the user pasted the result directly into chat. Whether the timing dialog remained open after copying was not explicitly confirmed.
- Latest phone baseline before v0.19.12: 1080x1920, 90 frames; projectM 5295 ms; queue 11; native render 714; glReadPixels 4421; bitmap copy 87; composition 7468; Cyber Shark 4093; HUD draw 1191; total 14925 ms; projectM BGRA yes.
- v0.19.12 keeps the GLES2/synchronous glReadPixels pixel path but adds two reusable readback slots and pipelines projectM frame N+1 on the GL thread while CPU Canvas composes frame N.
- Measurement rule: glReadPixels accumulated time may remain near ~4.4 s; success is lower top-level `projectM:` provider wait and lower `total:` through overlap, with unchanged picture/audio/frame order.
- Next user step: Termux **3 -> 10 -> 8**, install v0.19.12, run the same warm-cache 3-second preview, paste the copied timing block, and visually verify colors/orientation/audio.
- Preserve 3-second current-position preview, full-song 0:00 start, 1080x1920-class / 30 FPS / H.264 + AAC, deterministic projectM timing, and immutable `PULSEDECK_CENTER_CALIBRATION`.

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
