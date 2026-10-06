# FARIC Music Visualizer — START HERE

## LATEST READY CANDIDATE — v0.19.18 / build 107 — direct GPU projectM base

- App/source SHA: `655b367118787ca07d0d2654c56b12723005c3c6`.
- Android #494 PASS, run `37523326093`; Validate #831 PASS.
- Artifact: `FARIC-Music-Visualizer-v0.19.18-Debug`, id `11440804926`, digest `sha256:d9e50aa1069b216f12a80f86f7e8043112ef210ba321760e7c16c0bbd5cbcacf`.
- v0.19.17 phone performance PASS: total 8263 ms, composition 6028 ms, projectM draw 1708 ms, GPU glow 6 ms.
- v0.19.18 directly uses the exact-size raw offline projectM framebuffer as the encoder GPU base only when BGRA is correct and lower pre-Cyber-Shark layers are disabled; otherwise CPU fallback remains.
- Raw GL orientation is handled in texture coordinates rather than Canvas.
- New profiler metric: `GPU projectM`.
- GPU Cyber Shark glow path from v0.19.17 is unchanged.
- BUILD_CHECKPOINT recorded.
- Next phone action: Termux 3 -> 10 -> 8; same warm-cache 3-second / 90-frame preview; paste full timing and report any projectM orientation/color/framing or glow difference.

## LATEST READY CANDIDATE — v0.19.17 / build 106 — hybrid GPU Cyber Shark glow

- App/source SHA: `5b1c14122d7f887b6f29f0e7391c59b9e9a7b2e6`.
- Android #493 PASS, run `37519271112`; Validate #829 PASS.
- Artifact: `FARIC-Music-Visualizer-v0.19.17-Debug`, id `11437784670`, digest `sha256:de0c9e01f5a6d780e7691eef077ed7fb5484f598573355914f069046f513389b`.
- v0.19.16 baseline: total 9450 ms; composition 7379 ms; CPU glow 1662 ms, including 1396 ms destination composite.
- v0.19.17 splits CPU composition into base + transparent upper overlay and draws the Cyber Shark radial glow on the encoder EGL/GLES2 surface between them.
- New profiler metrics: `GPU glow` and `GPU overlay`.
- Final 1080x1920-class / 30 FPS export, H.264/AAC, projectM overlap pipeline and layer geometry remain unchanged.
- BUILD_CHECKPOINT recorded.
- Next phone action: Termux 3 -> 10 -> 8; same warm-cache 3-second / 90-frame preview; paste full timing and report any difference in glow color/brightness/radius/placement or layer ordering.

## LATEST READY CANDIDATE — v0.19.16 / build 105 — unfiltered cached glow

- App/source SHA: `ae32d61ae81b178e8437adf9fdc85e037c6df189`.
- Android #492 PASS, run `37514357810`; Validate #826 PASS.
- Artifact: `FARIC-Music-Visualizer-v0.19.16-Debug`, id `11436347682`, digest `sha256:4f33e9699f20ea41519a5bb2429b469152198551762647f666a4f38b37a4324c`.
- v0.19.15 phone result: glow 1663 ms = render 265 + filtered composite 1397 ms; total 9454 ms.
- Diagnosis: software filtered scaling is now the dominant glow operation.
- v0.19.16 keeps the same 512x512 dynamic glow cache and final output geometry, but removes `FILTER_BITMAP_FLAG` from the glow composite only.
- No changes to final 1080x1920-class / 30 FPS output, projectM, arcs, particles, frame, FX, creature, wordmark, HUD, encoder or audio.
- BUILD_CHECKPOINT is recorded.
- Next phone action: Termux 3 -> 10 -> 8; same warm-cache 3-second / 90-frame preview; paste full timing and report whether the radial glow still looks smooth. Reject if rings, stair-stepping or obvious pixelation are visible.

## LATEST READY CANDIDATE — v0.19.15 / build 104 — cached radial glow

- App/source SHA: `9fbb48d5023a2e12de59356ed148fa04a3b34258`.
- Android #491 PASS, run `37512222805`; Validate #824 PASS.
- Artifact: `FARIC-Music-Visualizer-v0.19.15-Debug`, id `11434899037`, digest `sha256:438f9205045f24767fec592cdf10d2824bcdfae66fb6abdd25bacdfab7e3d1ea`.
- v0.19.14 phone baseline: total 9510 ms; composition 7591 ms; Cyber Shark 4160 ms; background 1848 ms; glow 1755 ms.
- v0.19.15 replaces only the full-size software radial glow rasterization with a reusable 512x512 ARGB glow texture and filtered final-size draw.
- Final export remains 1080x1920-class / 30 FPS. No changes to projectM, frame, FX, creature, wordmark, HUD, encoder or audio.
- New phone metrics: `glow render` and `glow composite`.
- Next: Termux 3 -> 10 -> 8; same warm-cache 3-second / 90-frame preview; paste timing and confirm glow visual quality.

## LATEST CANDIDATE — v0.19.15 / build 104 — cached Cyber Shark glow

- v0.19.14 phone diagnosis: background 1848 ms, of which radial glow alone is 1755 ms (~95%).
- v0.19.15 keeps final 1080x1920-class / 30 FPS output and all existing Cyber Shark geometry/reactions.
- The smooth radial glow is rasterized each frame into one reusable 512x512 ARGB bitmap, then bilinearly composited at the original output radius. No frame/FPS reduction and no changes to arcs, particles, frame, creature, wordmark, projectM, HUD or encoder.
- New profiler fields: `glow render` and `glow composite`.
- Phone visual QA is required because the glow's internal raster resolution changed even though final output resolution did not.

## LATEST CANDIDATE — v0.19.14 / build 103 — background profiler

- App/source SHA: `5a7ca6a16b3387e17704d8ab9f9aba5499ec3a42`.
- Android #490 PASS, run `37510375626`; Validate #822 PASS.
- Artifact: `FARIC-Music-Visualizer-v0.19.14-Debug`, id `11435801494`, digest `sha256:fd17fd1949b01a0d0ff0eedb08072322c3ca2389af710985857a839434eeab3b`.
- v0.19.13 phone profile: Cyber Shark 4487 ms = background 1858 + frame 1475 + FX 273 + creature 445 + wordmark 423.
- v0.19.14 makes no intended visual change. It splits background into setup/save, glow, arcs, particles and restore.
- Next phone action: Termux 3 -> 10 -> 8; same warm-cache 3-second / 90-frame preview; paste the full timing block including `background internals`.
- After that, optimize only the largest measured background component.

## LATEST RESULT — v0.19.13 Cyber Shark sublayer profile — 2026-10-06

- Phone: 1080x1920 / 90 frames.
- projectM provider: 105 ms; glReadPixels 4458 ms; BGRA yes.
- composition: 7971 ms; total: 10069 ms.
- Cyber Shark: 4487 ms.
- Cyber Shark internals:
  - background 1858 ms;
  - frame 1475 ms;
  - FX 273 ms;
  - creature 445 ms;
  - wordmark 423 ms.
- Result is stable versus v0.19.12 total 9915 ms; profiler overhead/regression is small enough for diagnosis.
- Background is the largest Cyber Shark sublayer (~41% of Cyber Shark), with frame second (~33%).
- Next candidate v0.19.14 adds background-only subprofiling for setup/save, radial glow, arcs, particles and restore; no intended visual change.

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
