# Current Handoff

## LATEST READY CANDIDATE — v0.19.21 / build 110 — projectM direct GPU FBO

- Exact APK source SHA: `95bcd5f65075c308dde8d7b41cb3db783038506d`.
- Android #499 PASS, run `37542386259`; Validate #839 PASS.
- Artifact: `FARIC-Music-Visualizer-v0.19.21-Debug`, id `11449526827`, digest `sha256:f19df4633bcf6e4b5ff8fa0ada957a027601f5e3d44c16bf02eec77f34786ce8`.
- v0.19.20 phone baseline: total 7258 ms, projectM provider 1716 ms, glReadPixels 4787 ms, composition 3228 ms.
- v0.19.21 backports projectM target-FBO rendering and recreates the guarded offline projectM instance directly inside the MediaCodec encoder EGL context.
- The same deterministic PCM/SceneSignal/frameIndex-fps feed now renders into an RGBA texture-backed FBO; encoder composition samples that texture directly.
- The guarded direct path performs no projectM GPU->CPU readback, Bitmap copy, or projectM Bitmap upload.
- v0.19.20 GPU glow/frame and the non-qualifying readback fallback remain.
- Timing should show `projectM BGRA: GPU direct`; old readback internals should disappear on the direct path.
- BUILD_CHECKPOINT recorded.
- Next phone action: Termux 3 -> 10 -> 8; same warm-cache 3-second / 90-frame preview; paste full timing and report any projectM/crop/color/foreground/glow/frame difference.

## LATEST READY CANDIDATE — v0.19.20 / build 109 — GPU Cyber Shark frame

- App/source SHA: `c1438b06a0b604bde457be2ef5d54ec40cf1216d`.
- Android #496 PASS, run `37535685093`; Validate #835 PASS.
- Artifact: `FARIC-Music-Visualizer-v0.19.20-Debug`, id `11446607008`, digest `sha256:12e3c8eddaec4bc17dfe3932a0932a146c5107a128a5f7fb507118352bbe050d`.
- v0.19.19 phone performance PASS: total 7224 ms; composition 4543 ms; Cyber Shark frame 1560 ms.
- v0.19.20 preserves z-order as lower background overlay -> GPU frame -> upper FX/creature/wordmark/effects/HUD overlay.
- Static frame texture is uploaded once; per-frame work updates only transformed quad + alpha.
- Direct GPU projectM and GPU glow paths are unchanged.
- New metric: `GPU frame`; `GPU overlay` includes lower + upper overlay uploads.
- BUILD_CHECKPOINT recorded.
- Expected qualifying result: Cyber Shark `frame: 0 ms`, nonzero `GPU frame`.
- Next phone action: Termux 3 -> 10 -> 8; same warm-cache 3-second / 90-frame preview; paste full timing and report any frame/projectM/glow visual difference.

## LATEST READY CANDIDATE — v0.19.19 / build 108 — GPU-scale real projectM framebuffer

- App/source SHA: `adf26b803daf283cda51faaed9ebe51cf855e7fd`.
- Android #495 PASS, run `37529043474`; Validate #833 PASS.
- Artifact: `FARIC-Music-Visualizer-v0.19.19-Debug`, id `11443104041`, digest `sha256:ce0b8115dcc431d96d5903947bb55ba0f792f312afd5d7dfcd90a4b14032cbfb`.
- v0.19.18 phone result: projectM draw 1710 ms, no GPU projectM metric, total 8275 ms.
- Root cause fixed: BALANCED_BACKGROUND renders at 0.78 scale (~842x1498 for 1080x1920), so v0.19.18's exact-size guard never activated.
- v0.19.19 gives the real-size offline projectM framebuffer its own EGL texture and GPU scale-to-fill + center-crop path.
- BGRA, layer-order and CPU fallback guards remain.
- Expected qualifying result: `projectM draw = 0 ms` and nonzero `GPU projectM`.
- BUILD_CHECKPOINT recorded.
- Next phone action: Termux 3 -> 10 -> 8; same warm-cache 3-second / 90-frame preview; paste full timing and report any orientation/color/crop/glow difference.

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

## LATEST HANDOFF — v0.19.12 / build 101 — 2026-10-06

**Authoritative current state. The older resume block below is historical.**

### Exact candidate

- App/source SHA: `86750315d89c6ebb3b332e912ddc92a1759b3cbb`.
- Android #488: PASS, run `37490177560`.
- Validate #816: PASS on the same app/source SHA.
- Artifact: `FARIC-Music-Visualizer-v0.19.12-Debug`.
- Artifact id: `11425346915`.
- Artifact digest: `sha256:9e347528bef24ab8e5a9f706542ef7e5548222ba8ebe187703d70f3f58c57d03`.
- BUILD_CHECKPOINT has been written.

### Change under test

v0.19.12 does not attempt to make `glReadPixels` itself faster. It overlaps projectM render/readback for frame N+1 on the GLSurfaceView GL thread with CPU Canvas composition/encoder submission of frame N. Two reusable readback ByteBuffer/Bitmap slots prevent the next GL readback from overwriting the Bitmap currently being composed.

This intentionally preserves GLES2, the BGRA fast path/fallback, frame-specific PCM and SceneSignal, deterministic `frameIndex / fps` timing, output resolution/FPS, H.264/AAC and preview/full-song timeline semantics.

### Latest phone baseline

Copied directly from the v0.19.11 timing dialog:
- 1080x1920 / 90 frames;
- projectM: 5295 ms;
- queue wait: 11 ms;
- native render: 714 ms;
- glReadPixels: 4421 ms;
- bitmap copy: 87 ms;
- composition: 7468 ms;
- Cyber Shark: 4093 ms;
- HUD draw: 1191 ms;
- encoder: 494 ms;
- audio: 544 ms;
- mux: 104 ms;
- save: 221 ms;
- total: 14925 ms;
- projectM BGRA: yes.

The successful paste proves the v0.19.11 clipboard action copies the complete timing block on the real phone. The separate requirement that the dialog remain open after copying was not explicitly confirmed.

### Phone acceptance now required

Use Termux **3 -> 10 -> 8**, install v0.19.12, then run the same warm-cache 3-second / 90-frame preview and paste the timing text.

Primary comparison:
- top-level `projectM:` provider wait;
- `total:` export wall-clock;
- composition timing for regression context.

Do not reject the optimization merely because accumulated projectM-internal `glReadPixels` remains near ~4.4 s; that work is expected to remain but should be overlapped. Also verify frame order, colors/orientation and audio remain correct.

No further performance code should be stacked on this candidate before phone measurement.

## NEW CHAT RESUME — 2026-10-06

**Authoritative current state. Historical handoff entries below are retained only for provenance.**

### Exact current build

- Release: **v0.19.11 / build 100**.
- App/source SHA: `5b9c00b45993a01fe0be0922de8714db1c758e97`.
- Android workflow: **#487 PASS**, run `37485263497`.
- Validate: **#813 PASS**.
- Artifact: `FARIC-Music-Visualizer-v0.19.11-Debug`.
- Artifact id: `11423002180`.
- Digest: `sha256:1b4ccb6c9619073f8629082819cc521abe8a98f766f87df2e3f853a8cf9bc897`.
- BUILD_CHECKPOINT for v0.19.11 already exists.

### What v0.19.11 does

The export timing dialog now has **«Копіювати текст»**. It copies the entire timing report to Android clipboard and intentionally does **not** dismiss the dialog. `OK` remains the explicit close action. There are no intended render/performance changes versus v0.19.10.

### Phone acceptance still pending

The user has **not yet phone-accepted v0.19.11**. The installed/tested screenshot immediately before this handoff is from v0.19.10.

Next user flow:
1. Termux **3 — Оновити репозиторій з GitHub**.
2. Termux **10 — Статус Android build**.
3. Termux **8 — Завантажити APK і відкрити папку**.
4. Install v0.19.11.
5. Run one 3-second preview.
6. Tap **«Копіювати текст»**.
7. Paste the copied block directly into chat.
8. Confirm the dialog remains open after copying.

Do not ask for another screenshot if clipboard copy works.

### Current performance evidence

v0.19.10 phone profile:
- projectM total: **5270 ms**;
- queue wait: **16 ms**;
- native projectM render: **730 ms**;
- **glReadPixels: 4361 ms**;
- bitmap copy: **97 ms**;
- composition: **7781 ms**;
- encoder: **528 ms**;
- audio: **522 ms**;
- mux: **99 ms**;
- save: **204 ms**;
- total: **15287 ms**.

Composition layers in that run:
- clear 80 ms;
- projectM draw 1651 ms;
- Cyber Shark 4314 ms;
- effects 448 ms;
- HUD update 28 ms;
- HUD draw 1255 ms.

Key diagnosis: **glReadPixels is the dominant projectM bottleneck** (~83% of projectM provider time). Do not spend the next optimization on queue wait, native render, bitmap copy, encoder, mux, or audio.

### Performance history that matters

- v0.19.3: raw RGBA readback removed Kotlin per-pixel conversion; ~16 s warm-cache preview.
- v0.19.8: BGRA fast path active on Galaxy A54; projectM Canvas draw **6471 → 1643 ms**, total **17008 → 15118 ms**.
- v0.19.9: avoided opaque full-screen Cyber Shark `saveLayerAlpha`; Cyber Shark **4676 → 3523 ms**, total **15118 → 14031 ms**.
- v0.19.10: profiler proved GPU→CPU `glReadPixels` is now the largest single readback cost.

### Next engineering step after v0.19.11 phone UX PASS

Research/implement a **safe reduction or elimination of synchronous per-frame glReadPixels** while preserving deterministic offline projectM frames and visual correctness. Prefer a measured path, e.g. direct GPU composition / shared texture or framebuffer into the encoder pipeline, or an asynchronous readback approach only if compatible with the current GLES/context architecture. Do not blindly lower final output FPS or quality.

### Contracts / guardrails

- 3-second preview: **current playback position → next 3 seconds**.
- Full-song export: **0:00 → end**, independent of player scrubber.
- Current output contract: 1080×1920-class, 30 FPS, H.264 + AAC.
- projectM timing is deterministic/offline.
- `PULSEDECK_CENTER_CALIBRATION` is immutable.
- User Termux path: **3 → 10 → 8**.
- Do not recommend item 9 when a build already exists or is running.
- After every successful Android build, immediately write `BUILD_CHECKPOINTS.md`.
- PulseDeck does not use the YTM Importer 3-visible-parts rule.

Project: FARIC Music Visualizer / PulseDeck

Verified:
- repository / Termux workflow;
- Android scaffold CI;
- stable signer pipeline;
- local audio playback works on the real phone;
- reference screenshots analyzed as interaction patterns;
- original FARIC PulseDeck design accepted by user.

Current release candidate:
- v0.10.0 modular PNG Skin Engine v1;
- Cyber Shark is the first live multi-layer Board theme;
- phone visual check confirms the resized/repositioned Cyber Shark is clearly better and usable as a temporary default;
- further transform controls and floating-control PulseDeck redesign remain TODO;
- package remains `com.saney.musicvisualizer`.

v0.2.0 implementation:
- Library landing shell;
- Library Worlds placeholders;
- persistent PulseDock mini-player;
- full Now Playing screen;
- reactive PulseCore scene;
- play/pause;
- seek;
- elapsed/total time;
- navigation/action placeholders for later modules.

Evidence boundary:
- local scanner / queue / EQ / YTM backend are NOT implemented yet;
- v0.2.0 UI is not phone-PASS until signed APK is installed and tested.

Next:
1. CI compile/test/build v0.2.0;
2. download exact signed APK;
3. phone QA Library → track → PulseDock → Now Playing;
4. test seek/play/pause/reactive scene;
5. test rotation and Back;
6. record findings before media-scanner work.


Latest design/tooling update:
- FARIC PulseDeck skin V1 is stored in-repo as the canonical SVG prototype:
  `docs/design/pulsedeck/prototypes/PULSEDECK_SKIN_V1.svg`;
- Termux menu now contains:
  - `9 — Запустити Android build`;
  - `10 — Статус Android build`;
- build dispatch verifies a clean/synced branch before starting `.github/workflows/android.yml`.


## v0.2.1 startup-crash hotfix

Phone finding:
- v0.2.0 immersive build installed but closed with an Android crash dialog on launch.

Hotfix source:
- direct platform `WindowInsets` / `WindowInsetsController` calls removed from the fullscreen path;
- AndroidX `WindowCompat`, `WindowInsetsCompat`, and `WindowInsetsControllerCompat` used instead;
- immersive mode is applied after `setContentView`;
- fullscreen/inset operations are guarded with `runCatching`, so unsupported device behavior should not terminate startup;
- version bumped to `0.2.1` / versionCode 3.

Phone acceptance:
- pending exact signed v0.2.1 APK.


## v0.3.0 — Random Visualizer Core

User priority:
- visualizer quality is now the highest product-risk item;
- foreground visualizer and background must be independent layers;
- background should later support AI-generated, user-imported, and random source-pack assets.

Current target:
- prove a controlled random scene engine before adding more player/library breadth.

v0.3.0 first implementation sequence:
1. pure/testable scene models + random selector;
2. scene orchestrator;
3. multiple foreground render styles;
4. multiple procedural backgrounds + crossfade;
5. manual Shuffle Scene in Now Playing;
6. automatic interval switching;
7. phone QA before user/AI external background sources.


## v0.3.1 phone feedback

New product direction:
- frequency response must attack much faster;
- foreground and background must have independent reactive behavior;
- preferred default atmosphere is fast warp travel through stars;
- visible glyph centering is now a formal project UI rule, not a one-off fix.

Implementation target:
- max Android Visualizer capture rate;
- peak/RMS spectrum energy;
- asymmetric fast-attack response;
- WARP_STARFIELD;
- shared visual-centering rule UI-001.


## v0.4.0 projectM spike

Current experiment:
- libprojectM 4.1.7 via pinned CMake FetchContent;
- arm64-v8a only for the first phone spike;
- projectM LAB is intentionally separate from the FARIC scene compositor;
- Android Visualizer waveform feeds PCM into projectM only while LAB is active.

Background playback finding:
- music stopped on screen-off because MainActivity.onStop() explicitly paused the controller;
- that pause is removed in v0.4.0;
- production architecture still requires MediaSessionService for robust background playback and system media controls.


## v0.4.1 projectM library

Full preset strategy:
- complete Cream of the Crop library is downloaded on-device, not embedded in the APK;
- source is pinned to commit 0180df21f5e0bd39b9060cc5de420ed2f1f9e509;
- expected count is 9,795 .milk presets with category hierarchy preserved;
- MilkDrop texture pack is pinned to 6368812f27bc747b517218fbf89d21d59afce4d9.

Phone evaluation mode:
- TEST 40 is generated locally after full install;
- 8 presets each from Geometric, Particles, Supernova, Waveform and Hypnotic;
- ProjectM LAB defaults to TEST 40, while ВСІ switches to the whole local library.


## v0.4.2 architecture decision

- projectM is the background visual layer, not the foreground;
- future FARIC renderer is the foreground reactive layer;
- background may render below native resolution without reducing UI/foreground resolution;
- balanced baseline: 78% surface resolution, mesh 72x40, 60 FPS target metadata, 0.70 s soft cut;
- next optimization is AUTO quality based on sustained measured FPS and later thermal/battery signals.


## v0.5.0 compositor foundation

- projectM remains pinned to stable v4.1.7 and renders the background;
- FARIC now has a first native GLES foreground shader drawn after projectM in the same frame;
- PlaybackController forwards SceneSignal amplitude/bass/mid/high/beat to the native foreground;
- foreground v1 = pulse ring + radial rays + sparkle response;
- stable v4.1.7 C API has no public user-FBO render function, so true offscreen projectM texture composition is deferred rather than switching to unreleased upstream code.


## v0.5.1 layered controls

- full 9,795 projectM library stays installed;
- derived TOP pool contains every second sorted preset, target 4,898, preserving the full source library;
- TEST 40 remains stored but is no longer the primary UI mode;
- TOP / ВСІ recreate background playlist with automatic projectM switching enabled;
- NEXT calls projectM preset lock before programmatic next, so automatic switching stops until TOP or ВСІ is pressed;
- layer-1 foreground catalog is append-only: Pulse Rays retained; Orbit Rings and Spectrum Halo added;
- FG cycles foreground sample independently from the background playlist.


## v0.5.2 fast visualizer queue

- canonical working TODO is docs/visualizer/VISUALIZER_TODO.md;
- FG amplitude is increased substantially; bass is the dominant expansion driver;
- all three existing FG samples remain available and now include edge solar-flare energy with per-sample intensity;
- projectM first render can load a single remembered .milk directly instead of scanning the whole TOP/ALL directory on the GL thread;
- Kotlin owns a CURRENT + 3 NEXT queue; the next three files are read ahead off the render thread to warm filesystem cache;
- FARIC now owns 18s AUTO timing. NEXT or visualizer tap switches to MANUAL; TOP/ALL restore AUTO;
- SharedPreferences persist TOP/ALL, AUTO/MANUAL, last preset and foreground sample;
- this is file/path prefetch, not three GPU-precompiled projectM scenes; actual projectM parse/shader compilation still occurs when a preset becomes active;
- ratings/hidden persistence remain the next v0.5.3 block in VISUALIZER_TODO.md.


## v0.5.3 preset ratings

- persistent SharedPreferences rating store added for projectM presets;
- canonical preset identity is relative to Cream of the Crop/TOP/test roots, so TOP and ALL share ratings;
- controls: 👍 like, 👎 dislike, − hide;
- queue weights: UP=6, NONE=3, DOWN=1, HIDDEN=0;
- hidden preset is excluded from current/next queue but source .milk is never deleted;
- hidden-management/recovery screen remains TODO.


## v0.5.4 interaction latency

- phone finding: projectM actions could appear about 5 seconds after tap;
- main cause found in app code: rating actions rebuilt/rated thousands of presets synchronously on the main thread;
- queue creation now runs off-main; ratings are cached and stored in queue candidates;
- 👍/👎 update in-place; HIDDEN removes one item and advances without full rebuild;
- manual NEXT disables smooth dual-preset transition;
- FG selection uses atomic native state and no longer waits behind projectM GL load mutex;
- phone verification is required; if only background NEXT remains slow afterward, next diagnosis is per-preset projectM parse/shader compile latency.


## v0.6.0 music video creator foundation

- new product target: pick song → pick style/background → preview → export finished MP4;
- theme domain added under app/.../theme with 10 registered playback themes and export aspect profiles;
- projectM full 9,795 library is not inside the APK; it is downloaded/extracted after install into app disk storage;
- Library Index v2 replaces the old physical ~4,898 TOP duplicate with a 1,200-item index pointing at original files;
- ALL remains 9,795 through a persistent index; normal state checks no longer recursively count the full tree;
- migration deletes only the obsolete faric-top-half copy, never the original Cream library or ratings;
- five new FG hero samples: Neon Emblem, Energy Core, Orbital Crown, Star Seed, Wave Idol; old three remain;
- visual pack architecture documented for future versioned .faricpack delivery outside the APK;
- next product step: standalone PlaybackTheme picker/preview, then deterministic export spike;
- projectM may still have per-preset shader compile latency; measure it separately after phone test.


## v0.6.1 standalone hero themes

- Playback Theme picker is now user-facing in MainActivity;
- selected theme is persisted via PlaybackThemeStore;
- HeroThemeView renders Neon Emblem, Energy Core, Orbital Crown, Star Seed and Wave Idol independently of projectM;
- live PlaybackController SceneSignal drives HeroThemeView directly;
- Visualizer mode still uses ReactiveSceneView and can still open projectM LAB;
- planned Portrait Halo / Glass Core / Vinyl / Cassette / Poster are visible as coming-soon entries;
- next architecture task is to split live smoothing from deterministic frame-at-time evaluation so preview and export share the same scene math.


## v0.6.2 retro themes and projectM latency

- Vinyl and Cassette are now standalone selectable Playback Themes in HeroThemeView;
- current track title is rendered inside their labels; playback pause stops rotation;
- projectM loadPreset now returns native load duration in milliseconds;
- ProjectMPresetPerformanceStore smooths load time per canonical preset ID;
- >=1200ms is currently HEAVY; FAST/TOP filters learned heavy presets, ALL does not;
- LAB status exposes LOAD and HEAVY count for phone diagnosis;
- next milestone is deterministic scene-at-time evaluation and first MP4 export proof.


## v0.6.4 smooth projectM transitions

- FARIC now fades between projectM presets with a 170 ms fade-out and 320 ms fade-in.
- The next preset is still direct-loaded so LOAD timing remains meaningful and projectM dual-render soft-cut cost stays avoided.
- The transition veil reaches 88% opacity and stays over slow loads, making them look intentional instead of frozen.
- Repeated NEXT, TOP/ALL and rating actions are guarded during the transition; FG remains independent.
- Phone QA should verify manual NEXT, AUTO, and HEAVY preset transitions.


## v0.6.5 offline export analysis

- OfflineAudioAnalyzer decodes the selected local audio via MediaExtractor + MediaCodec.
- PCM is reduced to mono and analyzed with a 2048-point FFT / 1024-sample hop.
- Timeline contains amplitude, bass 35–180 Hz, mid 180–2000 Hz, high 2000–10000 Hz and adaptive beat strength.
- OfflineAnalysisResult.signalAt(timeMs) is now the deterministic export signal source.
- Export Lab can analyze the current track and PNG proof uses offline signal when available.
- Analysis is currently in-memory only and is invalidated when a new track is chosen.
- Next implementation step after phone verification: render a short frame sequence and encode H.264, then mux audio.


## v0.6.6 first H.264 proof

- Export Lab can now render a 3-second silent H.264 MP4 proof from OfflineAnalysisResult.
- Each frame uses exact signalAt(timestamp), not live Visualizer capture.
- Proof uses 15 fps and scales the long edge to <=960 px for first-device correctness testing.
- Runtime AVC encoder discovery supports YUV420 planar/semi-planar/flexible input.
- Output is written by MediaMuxer and published to Movies/FARIC.
- Audio is intentionally not present yet; next step is AAC/audio mux.


## v0.6.7 audible MP4 proof

- FARIC now transcodes the selected local audio range to AAC-LC 160 kbps and muxes it with the deterministic H.264 proof.
- Export proof remains 3 seconds / 15 fps / long edge <=960 px until phone correctness is confirmed.
- Audio decode accepts PCM16 and PCM float output, then encodes AAC before final AV mux.
- Final proof is published to Movies/FARIC and should contain the matching music segment from the same source timestamp range.
- Next phone checks: MP3/M4A compatibility, A/V sync, no initial silence, no color corruption, export duration.
- After PASS: move to 30 fps, higher resolution, arbitrary range, then full-track export.


## v0.6.8 continuous Layer-1 reactivity hotfix

Phone evidence:
- user screen recording on 2026-09-29 shows Energy Core reacting strongly for only about 2–3 seconds after Play/resume;
- playback time keeps advancing, but the Layer-1 core/rays settle into almost static size afterward;
- the recording itself contains no audio stream, so the visual finding proves loss of visible dynamics, not A/V sync.

Implementation:
- new `LiveSignalDynamics` sits between `SpectrumMath` and `SceneSignal`;
- it keeps a slow local baseline per band and expands small ongoing deviations into useful visual range;
- after several silent frames it rebases on the next non-silent frame instead of treating restart level as a huge transient;
- `AdaptiveBeatDetector` resets its baseline on silence so Pause → Play does not inject a fake beat;
- fix is shared by all live Layer-1 consumers, not hardcoded into Energy Core;
- version is `0.6.8` / versionCode 22.

Next:
1. wait for Android CI;
2. install exact signed v0.6.8 APK;
3. play Energy Core for at least 15 seconds and confirm continuing beat/bass movement;
4. pause for at least 2 seconds, resume, and confirm there is no artificial startup-only burst;
5. smoke two other Layer-1 themes before closing the hotfix.


## v0.6.9 Layer-1 ring continuity

Phone evidence:
- v0.6.8 improved sustained audio response;
- new 333058.mp4 shows the remaining issue is ring continuity rather than complete signal loss;
- the outer cyan ring disappears between beats because it was implemented only as a transient shockwave;
- when a new beat arrives, the same beat-derived radius can jump inward, which reads as a transition seam.

Implementation:
- Neon Emblem now owns two always-present low-alpha base rings;
- those rings breathe/react continuously from amplitude, bass and highs;
- beat shockwaves are no longer the only visible outer rings;
- two independent shockwave slots alternate on beat events, allowing the previous wave to finish outward while a new wave begins;
- shockwave radius is monotonic from inner to outer radius using phase/easing;
- version is `0.6.9` / versionCode 23.

Next phone check:
1. install exact signed v0.6.9 APK;
2. Neon Emblem for 20+ seconds;
3. verify at least one outer/base ring is always present;
4. watch several fast beats for no inward jump/seam;
5. confirm rings remain subtle during quieter passages.


## v0.6.10 beat-ring detection correction

User clarified the v0.6.9 interpretation:
- the ring reset/jump is visually desirable;
- the problem is missed beat events, not ring continuity between beats.

Implementation:
- restore the pre-v0.6.9 Neon Emblem ring behavior;
- remove always-present base rings and overlapping shockwave slots introduced by the incorrect interpretation;
- `AdaptiveBeatDetector` now evaluates full `BandEnergy`, not bass alone;
- a hit can trigger from strong bass OR a sharp broadband transient using amplitude/mid/high;
- cooldown remains to prevent duplicate triggers for one impact;
- version is `0.6.10` / versionCode 24.

Phone acceptance:
1. play a rhythmically obvious section for 20+ seconds;
2. count visible missed rings on clear beats;
3. confirm the reset/jump remains;
4. confirm the detector is not firing rings continuously between beats.


## 2026-09-30 — persistent master-asset repository

Created private companion repository:
- `faric-ua/faric-music-visualizer-assets`

Purpose:
- preserve user-supplied image originals;
- preserve generated PNG masters;
- keep large image files under Git LFS;
- keep app/code repository lightweight;
- provide a stable phone path: `~/faric-music-visualizer-assets`.

Initial asset repository commit:
- `3ff67312193dcfef995bfee5d799d5fd27661f4d`
- message: `assets: initialize FARIC master asset archive`

The asset repository currently contains 34 LFS-tracked master entries plus manifest and production placeholders.

Main Termux menu now includes:
- 11 — connect / restore FARIC assets;
- 12 — update FARIC assets from GitHub;
- 13 — save FARIC assets to GitHub;
- 14 — FARIC assets status;
- 15 — open shell in FARIC assets.

Phone acceptance: **PASS**.
- main menu updated with items 11–15;
- asset repository connected on phone at `~/faric-music-visualizer-assets`;
- `Master-файлів: 34`;
- `LFS materialization: PASS`;
- phone now holds real LFS materialized master files, not only pointer stubs.


## 2026-09-30 — Cyber Shark production candidate

First modular Hero/GF production candidate prepared:
- id: `faric.cyber-shark.v1`;
- shared canvas: 1254 × 1254;
- layers: frame / fx / creature / wordmark;
- whole-GF fallback included;
- lossless WebP app copies included;
- manifest defines initial z-order and audio-reactive routing.

Default modular z-order:
`frame → fx → creature → wordmark`

Main Termux menu now also contains:
- `16 — Імпортувати production-pack з Downloads`

Phone import flow:
1. download `FARIC_PRODUCTION_PACK_CYBER_SHARK_V1.zip` to Downloads;
2. update main repo with menu item 3;
3. run menu item 16;
4. run menu item 13 to commit/push the imported production set to the private asset repository.


## 2026-09-30 — Termux menu production-pack import

Fixed the main FARIC Termux menu:
- item 16 is now present: `Імпортувати production-pack з Downloads`;
- item 3 now always re-execs `scripts/termux-menu.sh` after a successful GitHub update check, even when HEAD was already current;
- this guarantees the displayed menu is refreshed from the current checked-out script without requiring the user to close/reopen Termux manually.

Production-pack importer:
- `tools/termux/assets-import-production-pack.sh`
- imports the newest `FARIC_PRODUCTION_PACK_*.zip` from `~/storage/downloads`;
- validates ZIP paths;
- refuses to overwrite an existing production version;
- target is copied into `~/faric-music-visualizer-assets/production/.../`;
- next step after import is menu item 13 to push assets to GitHub.


## v0.7.0 — first layered Board Hero/GF

Implementation candidate:
- first live Board Hero/GF is `Cyber Shark`;
- `PlaybackThemeId.CYBER_SHARK` is selectable from Playback Themes / Scene Lab;
- `HeroBoardView` renders four logical layers in order:
  `frame → FX → creature → wordmark`;
- optimized app copies of frame / creature / wordmark are bundled under `drawable-nodpi`;
- FX is procedural in this first APK proof so it can react strongly without duplicating the full master FX asset in the app repository;
- full-resolution production masters remain in the private companion asset repository.

Current audio routing:
- frame: bass-dominant scale + slow rotation;
- FX: highs + beat control alpha/scale and reverse drift;
- creature: strongest beat punch, with small bass/mid support and vertical punch;
- wordmark: shorter beat punch with light high-frequency response.

Architecture:
- reusable `BoardLayerReaction` / `BoardLayerMotionEvaluator` added under `board/`;
- live `SceneSignal` fans out into `HeroBoardView` exactly like existing visualizers;
- Cyber Shark is intentionally NOT marked deterministic-export-ready yet. Export parity remains a separate acceptance step.

Release candidate:
- version `0.7.0`;
- versionCode `25`.

Phone acceptance:
1. update source from the Termux menu;
2. build/install exact signed v0.7.0 APK;
3. choose Scene Lab / Playback Themes → Cyber Shark;
4. verify the four visual responsibilities remain visually separable while music plays;
5. report whether bass/beat/high response is too weak, too strong, or visually colliding.


## v0.7.1 — Cyber Shark selection crash hotfix

Phone finding:
- v0.7.0 launches normally;
- selecting the new `Cyber Shark` layered GF causes the app to terminate immediately;
- existing themes are not implicated by this report.

Diagnosis boundary:
- CI compile/unit tests for v0.7.0 passed, so this is a runtime-only failure;
- the crash occurs at layered Board selection/creation;
- without phone logcat the exact exception is not yet proven;
- the highest-risk point was synchronous bitmap resource decoding during `HeroBoardView` construction.

Hotfix:
- bitmap decoding is now nullable/crash-safe instead of `requireNotNull`;
- decode uses explicit non-scaled ARGB_8888 options;
- a missing/failed layer falls back to a simple procedural marker instead of terminating the Activity;
- `MainActivity` now guards `HeroBoardView` construction and falls back to Neon Emblem with a toast if Board creation itself fails;
- version bumped to `0.7.1` / versionCode `26`.

Phone acceptance:
1. install exact signed v0.7.1 APK;
2. open Playback Themes / Scene Lab;
3. select Cyber Shark;
4. confirm app remains open;
5. report whether the actual shark/frame/FARIC assets render or whether fallback is shown.


## v0.7.1 phone finding — fallback-only Cyber Shark

Phone screenshot confirms the crash guard works, but the real image layers do not decode/render.

Observed:
- app stays open;
- theme label is `Cyber Shark`;
- center shows the cyan procedural fallback circle;
- procedural FX rings are visible;
- real frame / shark creature / FARIC wordmark are absent.

Interpretation:
- all three bundled bitmap resources are failing decode;
- this is not a Board routing problem;
- the fallback path is working exactly as designed and localizes the issue to the app-packaged WebP bytes.

Fix path:
- do not hand-create/transport the production WebP blobs through the GitHub connector;
- copy the already verified production WebPs directly from the phone's companion asset repository into the main app repo;
- Termux menu item 17 performs this sync, validates RIFF/WEBP headers, bumps to v0.7.2 / versionCode 27, commits and pushes.


## v0.7.2 phone finding — real Cyber Shark renders but is oversized

Phone video `376665.mp4` confirms:
- real Cyber Shark frame / creature / FARIC assets now render;
- layered Board path is alive;
- the Hero/GF is too large and too low for the current PulseDeck overlay;
- the FARIC wordmark visibly collides with / disappears behind the player card.

Immediate tuning candidate:
- v0.7.3;
- default Hero/GF center Y moved from 39% to 27.5% of screen height;
- default base size reduced from 92% to 66% of the minimum screen side.

Product requirement added:
- whole Hero/GF group controls: X / Y / scale / rotation / opacity / reset / Fit Safe Area;
- advanced per-layer overrides: frame / creature / wordmark / FX;
- settings persist and must be shared by preview and deterministic export.


## 2026-09-30 — floating-controls direction

User clarified that the current large Now Playing card is temporary.

Target:
- remove the enclosing card later;
- playback/seek/action controls float over the Board as separate configurable blocks;
- Board remains visually continuous behind controls;
- block visibility / opacity / skin / transform remain configurable;
- Hero/GF safe-area logic must eventually follow active floating blocks, not the current card rectangle.


## v0.7.3 phone check — improved default fit

Evidence:
- phone recording `376668.mp4` reviewed on 2026-09-30;
- Cyber Shark now renders at a much more usable size and sits clearly above the temporary Now Playing card;
- the real frame / creature / FARIC wordmark are visible together;
- user assessment: "Уже краще".

Important product interpretation:
- this is only a better temporary default;
- the current large Now Playing card is temporary scaffolding and will later be removed/reworked into floating controls;
- therefore Hero/GF sizing must not be permanently tuned around the current card boundary.

Next continuation point:
1. add persistent Board transform controls for whole Hero/GF: X / Y / size / rotation / opacity / reset / Fit Safe Area;
2. add advanced per-layer overrides for frame / creature / wordmark / FX;
3. keep transform state separate from audio-reactive motion;
4. later replace the monolithic player card with configurable floating PulseDeck blocks;
5. after live layout control is stable, continue toward deterministic preview/export parity for layered Board themes.

Do not reopen the earlier bitmap-decode diagnosis unless a new regression appears; the current phone evidence shows the real Cyber Shark layers are rendering.


## v0.7.4 — live Board transform controls

Implemented the first user-facing transform editor for layered Hero/GF themes.

Cyber Shark now has a context-sensitive `Board` action on Now Playing. It opens a live preview editor with:
- X position;
- Y position;
- size;
- rotation;
- opacity;
- Reset;
- Fit Safe Area;
- Done.

Persistence:
- transforms are stored per `PlaybackThemeId` in `BoardTransformStore`;
- returning to Now Playing restores the saved values;
- values are sanitized to safe ranges.

Renderer contract:
- transform moves/scales/rotates the whole GF group;
- layer-specific audio motion remains independent and is evaluated on top of the saved transform;
- opacity multiplies frame / FX / creature / wordmark together;
- background glow follows the Hero anchor position but is not faded with Hero opacity.

Current Cyber Shark defaults remain the v0.7.3 improved phone defaults:
- X 50%;
- Y 27.5%;
- size 66%;
- rotation 0°;
- opacity 100%.

Fit Safe Area v1 preset:
- X 50%;
- Y 25.5%;
- size 60%.

Release:
- v0.7.4;
- versionCode 29.

Phone acceptance:
1. install exact signed v0.7.4 APK;
2. choose Cyber Shark;
3. tap `Board`;
4. move X/Y while music continues;
5. change size, rotation and opacity;
6. press Done and confirm values survive return to Now Playing;
7. reopen Board and confirm values persisted;
8. test Reset and Fit Safe Area.


## v0.7.5 — gestures + stereo/bass/sway group motion

User request:
- keep all existing Cyber Shark audio reactions;
- add direct manipulation gestures to the Board editor;
- add a small whole-GF left/right stereo movement;
- add a small whole-GF up/down bass float;
- add a small back/forth whole-GF rotation;
- expose these new motion strengths in the existing Board settings.

Implemented:
- one-finger drag changes X/Y;
- pinch changes whole-GF size;
- two-finger twist changes base rotation;
- gesture results persist through the same `BoardTransformStore` used by sliders;
- existing layer reactions remain unchanged and continue to run on top of the manual base transform.

New group music motion:
- `rotationSwayDegrees` — gentle signed back/forth rotation driven by current mid/bass energy;
- `stereoShiftFraction` — horizontal whole-GF displacement driven by true stereo left/right energy balance;
- `bassFloatFraction` — small vertical oscillation whose amplitude follows bass energy.

Stereo source:
- added `StereoBalanceAudioProcessor` to the Media3 PCM chain;
- it is pass-through: playback samples are copied unchanged;
- for stereo PCM16 it measures left/right RMS and produces a smoothed `stereoPan` in [-1, +1];
- `SceneSignal` now carries `stereoPan`;
- mono/non-PCM16 paths safely fall back to centered pan (0).

Board menu additions:
- "Плавний поворот";
- "Stereo L/R";
- "Bass ↑↓";
- Reset now resets both base transform and group-reaction settings.

Default subtle values:
- rotation sway: 2.4°;
- stereo travel: 3.5% of screen width at full pan;
- bass float: 1.8% of minimum screen side at full bass.

Release:
- v0.7.5;
- versionCode 30.

Phone acceptance:
1. install exact signed v0.7.5;
2. open Cyber Shark → Board;
3. drag with one finger;
4. pinch to resize;
5. twist with two fingers;
6. verify values persist after Done/reopen;
7. play a strongly stereo track and verify whole GF follows L/R without large jumps;
8. verify slight rotation sway and bass vertical float are visible but not dominant;
9. set each of the three new reaction sliders to 0 and confirm only the previous layer reactions remain.


## v0.7.6 — rotation/lifecycle hardening

User finding:
- Board settings/editor did not survive screen rotation correctly;
- requirement expanded from Board only to a global rule for every current and future UI surface.

Implemented in MainActivity:
- restore the same active screen after Activity recreation;
- preserve selected PlaybackThemeId;
- preserve ExportAspectRatio;
- preserve active vertical ScrollView position;
- preserve active horizontal HorizontalScrollView position;
- Board Transform reopens over the same layered theme;
- Board transform values already survive via BoardTransformStore;
- Board group audio-motion values already survive via BoardGroupReactionStore;
- rotation does not invoke Reset / Fit Safe Area / Done / export actions.

Current MainActivity surfaces covered:
- Library;
- Now Playing;
- Playback Themes;
- Board Transform;
- Export Lab.

Global lifecycle audit document added:
- `docs/architecture/UI_LIFECYCLE_ROTATION_AUDIT.md`.

Audit contract:
- same parent screen;
- same selected content/settings;
- same scroll/list position where applicable;
- playback continues;
- no automatic action or duplicated work caused by rotation.

Known follow-up findings from the audit:
- Export Lab offline-analysis result/running ownership is still Activity-local and needs lifecycle-safe ownership;
- projectM persistent mode/FG/auto/last preset exist, but install/index/queue background work still needs explicit duplicate-start rotation audit.

Release:
- v0.7.6;
- versionCode 31.

Phone acceptance:
1. rotate Library after scrolling;
2. rotate Now Playing while music plays;
3. rotate Playback Themes after scrolling;
4. open Cyber Shark → Board, change values, scroll inside settings, rotate, confirm same editor/value/scroll position;
5. rotate Export Lab after selecting a format and scrolling;
6. confirm no button/action runs by itself after any rotation.


## v0.7.7 — per-layer Board editor

Next layered-Board stage implemented after the v0.7.6 lifecycle contract.

Editor targets:
- whole GF;
- frame;
- shark / creature;
- FARIC wordmark;
- FX.

Whole-GF mode keeps the existing controls and reactions:
- X / Y;
- size;
- base rotation;
- opacity;
- music rotation sway;
- stereo L/R travel;
- bass up/down float;
- Fit Safe Area;
- Reset all.

Each individual layer now has persistent overrides:
- X offset;
- Y offset;
- scale;
- rotation;
- opacity.

Layer transforms are additive/multiplicative on top of whole-GF transform and existing audio reaction. Existing frame / creature / wordmark / FX music reactions remain active.

Gesture editing:
- selector chooses the current edit target;
- one-finger drag edits only the selected target;
- pinch edits selected target scale;
- two-finger twist edits selected target rotation;
- gesture-driven values are written back to the same persistent stores as sliders.

Lifecycle:
- selected layer editor target is saved/restored across rotation;
- vertical settings scroll and horizontal layer-selector scroll are also preserved;
- rotation must reopen the same Board target without triggering Reset/Fit/Done.

Persistence:
- BoardLayerTransformStore stores values per PlaybackThemeId + BoardLayerId;
- live Now Playing restores all per-layer overrides whenever Cyber Shark is opened.

Release:
- v0.7.7;
- versionCode 32.

Phone acceptance:
1. open Cyber Shark → Board;
2. choose Frame and move/resize/rotate it;
3. choose Shark and move it independently;
4. choose FARIC and change its position/size;
5. set FX opacity to 0 and back to 100%;
6. return to "Усе" and confirm whole-GF controls still move all layers together;
7. rotate while editing a non-group layer and confirm same layer + scroll + values restore;
8. press Done and reopen Board to confirm persistence.


## v0.7.8 — compact Board panel

Phone UX feedback:
- Board settings panel occupied too much vertical space and hid too much of the live GF preview.

Change:
- Board settings panel height is now 40% of the current screen height instead of a fixed 580 dp.
- The panel remains scrollable, so all existing controls are still available.
- The live Hero/GF preview gets roughly 60% of the screen for visual editing.
- Rotation keeps using the same percentage-based rule, so portrait and landscape adapt automatically.

Release:
- v0.7.8;
- versionCode 33.


## v0.8.0 — floating PulseDeck controls

User direction:
- remove the large Now Playing control card that covers the Board;
- split player UI into independent floating blocks;
- let controls visually hover over the scene;
- support a visualizer-first state where the scene is mostly unobstructed;
- later allow each block to be hidden, moved, resized, recolored/skinned and have opacity adjusted.

Implemented foundation:
- monolithic Now Playing card removed;
- metadata is now a compact floating block;
- seek/progress + time is a separate floating block;
- transport buttons are individual floating round controls;
- quick actions are a separate row of floating chips;
- old bottom navigation is removed from Now Playing;
- Board remains visible behind/between controls;
- floating player controls auto-hide after 6 seconds;
- double tap on empty Board space toggles controls visible/hidden;
- hidden/visible state is saved across Activity recreation/rotation.

Still intentionally deferred:
- per-block visibility configuration;
- per-block opacity;
- per-block X/Y/size;
- individual block skins / fully transparent block mode;
- a dedicated settings UI for those block options.

Release:
- v0.8.0;
- versionCode 34.

Phone acceptance:
1. install v0.8.0;
2. confirm the old large card is gone;
3. verify metadata, progress, transport and quick actions are visually separate;
4. wait 6+ seconds and verify controls disappear;
5. double tap empty Board to bring them back;
6. double tap again to hide;
7. rotate while hidden and while visible;
8. confirm playback continues and the Board remains much less obstructed.


## Assistant response / Termux handoff format

Standing project rule:
- future assistants should read `docs/workflow/ASSISTANT_RESPONSE_FORMAT.md`;
- when the user is expected to test/install something, finish the response with a short visible `TERMUX:` block and a short `ТЕСТ:` checklist;
- normal install flow: `3 → 10 → PASS → 8 → встановити APK`;
- manual build flow: `3 → 9 → 10 → PASS → 8 → встановити APK`;
- on FAIL: stop, do not install, ask for the result from menu item 10;
- do not hide required user actions inside long prose.


## v0.8.1 — concept-style PulseDeck main screen

User approved the generated visual direction and asked to stop iterating on mockups and implement the real app to match it.

Implemented:
- kept the current Hero/GF system as the swappable central visual layer;
- rebuilt the Now Playing shell around the approved concept;
- metadata stays centered and lightweight over the scene;
- restored a reactive waveform strip above seek/progress;
- transport controls now use custom Canvas-drawn FARIC icons instead of text/emoji glyphs;
- transport order is now: shuffle / previous / large play-pause / next / repeat;
- added translucent cyber-neon rails behind transport and quick actions;
- bottom quick actions are now icon-only with no labels;
- quick-action icon mapping:
  - palette = themes/skins;
  - node/board = Board/layout;
  - bars = projectM/visualizer;
  - export arrow = Export Lab;
- central play/pause remains the primary orange accent, surrounding controls use cyan with orange secondary details;
- existing auto-hide and double-tap show/hide behavior remains;
- current selected Hero/GF can still change independently of the permanent main shell.

New custom UI code:
- `ui/PulseDeckIconButton.kt`
- `ui/PulseDeckControlRail.kt`

Approved visual reference saved in repository:
- `docs/design/pulsedeck-main-screen-reference.jpg`

Release:
- v0.8.1
- versionCode 35

Phone acceptance:
1. confirm icon-only lower rail;
2. confirm transport order and large central play/pause;
3. verify custom icons are crisp and not font glyphs;
4. confirm Cyber Shark remains the swappable Hero/GF while controls stay in place;
5. wait for auto-hide, then double-tap to restore controls;
6. rotate and confirm control visibility state survives.


## v0.8.2 — reference-fidelity pass

Phone screenshot comparison against the approved reference showed that v0.8.1 had the right architecture but weak visual fidelity:
- Hero/GF was too small and too high;
- there was too much dead space between Hero and metadata;
- waveform looked like coarse dots instead of many fine bars;
- transport hierarchy was too small;
- lower quick-action rail was too flat/small;
- approved reference included track-level favorite/more controls and richer cyan/orange energy around the Hero.

Implemented:
- added presentation-only Hero tuning for Now Playing: larger Hero slot without mutating persisted Board transforms;
- shifted Hero presentation slightly downward;
- added subtle procedural cyan/orange HUD arcs and energy particles around the Hero;
- replaced 9 coarse waveform bars with 56 narrow reactive bars;
- raised metadata/progress stack and tightened vertical spacing;
- enlarged transport hierarchy:
  - shuffle/repeat 58dp;
  - previous/next 68dp;
  - play/pause 94dp;
- enlarged transport rail to 116dp;
- enlarged quick actions to 64dp and rail to 82dp;
- added track-level favorite and more buttons using the same custom FARIC icon renderer;
- preserved auto-hide, double tap, rotation visibility state, and swappable Hero/GF architecture.

Release:
- v0.8.2
- versionCode 36

Phone acceptance:
1. compare directly with docs/design/pulsedeck-main-screen-reference.jpg;
2. Hero should occupy substantially more of the upper screen;
3. dead zone between Hero and title should be much smaller;
4. waveform should read as many thin vertical bars;
5. large central play/pause should dominate transport;
6. lower icon rail should be larger and more like the reference;
7. favorite + track-more controls should appear around metadata;
8. verify auto-hide/double-tap/rotation still work.


## v0.8.3 — hero-footprint correction

Reason:
- phone comparison still looked far from the approved concept;
- the latest screenshot was using Neon Emblem, not Cyber Shark, so the swappable Hero slot itself needed normalization;
- compact standalone skins occupied too little of the upper visual area;
- transport/action rails still needed more visual weight.

Changes:
- standalone Hero themes now support presentation tuning independent of their internal drawing model;
- Neon Emblem main-screen footprint increased to 2.28x and moved into the same upper Hero zone used by the reference;
- other standalone themes get theme-specific presentation scales;
- Cyber Shark main-screen presentation increased to 1.32x without mutating saved Board transforms;
- favorite / track-more controls aligned with the metadata row after the vertical-stack adjustment;
- v0.8.2 enlarged transport/action sizing remains in place.

Release:
- v0.8.3
- versionCode 37


## v0.9.0 — permanent PulseDeck HUD main-page skin

User clarified the architecture:
- Cyber Shark or any other GF/visualizer is NOT the main page itself.
- The approved cyan/orange HUD is the permanent Android player skin.
- Existing visualizers, GF heroes, video and future effects will later be mounted as configurable layers either behind or above this permanent shell.
- Do not generate another mockup for this stage; implement the UI in Android code.

Reference blocks supplied by the user:
- 5-button transport HUD: shuffle / previous / large center play-pause / next / repeat;
- large cyan/orange circular F reactor;
- orange/cyan energy ribbons and particles;
- fine waveform + scrubber;
- top back / overflow / favorite / track-more circular controls;
- 4-button lower rail: palette / board / visualizer / export;
- empty transport/action HUD rail frames.

Implemented in code:
- new `PulseDeckMainSkinView` draws the permanent reactive main background;
- main skin uses cyan/orange energy ribbons, particles, concentric HUD rings, radial equalizer bars and central F core;
- Hero/GF/theme drawing is no longer used as the main-page background;
- playback metadata, waveform, seek/time, transport and quick actions remain real Android controls above the permanent skin;
- header back/menu now use the same custom `PulseDeckIconButton` family;
- transport layout matches the reference hierarchy: small shuffle/repeat, medium previous/next, dominant center play-pause;
- `PulseDeckControlRail` was upgraded with reference-style socket housings, cyan/orange traces and center emphasis;
- lower actions remain icon-only;
- favorite / track-more remain separate circular controls around metadata;
- auto-hide, double-tap show/hide and rotation visibility state remain;
- playback signal now feeds both the fine waveform and permanent HUD reactor.

Layering contract for the next stages:
1. BACKGROUND_CONTENT — video / projectM / scene / optional background visual.
2. MAIN_SKIN_BACKGROUND — permanent PulseDeck energy + reactor field.
3. HERO_CONTENT — optional GF / creature / artwork / logo.
4. PLAYER_CHROME — metadata, waveform, transport, quick actions.
5. FOREGROUND_FX — optional particles/lightning/overlays.

Current v0.9.0 implements the permanent skin/chrome foundation. Existing content-layer selection remains in the project and will be connected to explicit front/back layer placement next.

Release:
- v0.9.0
- versionCode 38

Phone acceptance:
1. main page should show the F reactor / cyan-orange HUD regardless of the previously selected GF;
2. no Cyber Shark should be required for the main page;
3. transport rail should visually resemble the supplied 5-button block;
4. lower four icon-only actions should remain consistent;
5. waveform and reactor should react to audio;
6. auto-hide and double-tap restore should still work;
7. rotation must preserve control visibility state.


## PulseDeck HUD modular PNG pack

Prepared from the approved generated HUD sheets:
- 27 separate alpha PNG assets;
- transport rail + shuffle/previous/play/pause/next/repeat;
- quick-actions rail + theme/board/visualizer/export;
- utility back/menu/favorite/track-more;
- waveform/progress line/progress thumb;
- F core/reactor frame;
- background energy waves/reactor energy ring/particle clusters;
- manifest.json + README.md.

The binary pack is distributed as `PulseDeckHUD_SkinPack_v1.zip`.
Expected SHA-256:
`82988f103fecf34a8d1cc6c72f7b2ed65c3e4c1707ecd12c7101e057912b88f4`

Repo support added:
- `tools/termux/import-pulsedeck-hud-skin.sh`;
- Termux menu item 18 imports the ZIP from Downloads, validates checksum/paths/files, commits `skin/pulsedeck_hud/`, and pushes the current branch.

Pending:
- user downloads the exact ZIP into Android Downloads and runs menu 18;
- after PASS, verify files on GitHub and start Skin Engine v1 integration.


## v0.10.0 — modular PNG Skin Engine v1

The modular PulseDeck HUD pack is now present in GitHub under:
- `skin/pulsedeck_hud/`
- 27 PNG assets;
- `manifest.json`;
- README and supporting folders.

Skin Engine v1 implementation:
- Android packages repository-level `skin/` directly as app assets;
- `PulseDeckMainSkinView` now parses `pulsedeck_hud/manifest.json`;
- layer x/y/width/z come from the manifest instead of hard-coded control geometry;
- PNG layers own the visual appearance of reactor, rails, transport, quick actions, utility controls, waveform and progress line;
- play/pause swaps between `transport/play.png` and `transport/pause.png`;
- manifest actions are hit-tested by the skin view and routed to existing Android behavior;
- seek is handled from the manifest progress-line hit area;
- progress thumb is rendered from its own PNG and follows playback progress;
- dynamic title/artist/status/time remain code-driven text above the PNG skin;
- audio-reactive manifest tags currently provide light scale/alpha reaction for ambient/bass/beat/spectrum layers;
- auto-hide now fades only the chrome layers (z >= 40), leaving the permanent background/reactor visible;
- double tap on empty skin space restores/hides controls;
- existing rotation-visible/hidden state remains owned by MainActivity.

Current action mapping:
- back → Library;
- menu → future skin settings;
- favorite / track_more → placeholders;
- shuffle / previous / next / repeat → existing queue placeholders;
- play_pause → live PlaybackController;
- theme → Theme Picker;
- board → Board / Scene behavior;
- visualizer → projectM;
- export → Export Lab;
- seek → live seek.

Release:
- v0.10.0
- versionCode 39

Phone acceptance:
1. confirm main HUD is visibly built from imported PNG assets;
2. confirm play/pause image changes correctly;
3. confirm every visible button hit target works;
4. confirm seek responds on the PNG progress line;
5. confirm auto-hide leaves reactor/background visible but hides chrome;
6. confirm double tap restores chrome;
7. rotate hidden and visible states and verify persistence;
8. compare final proportions against the approved PNG reference.


## Sleep checkpoint — 2026-10-01

Checkpoint state:
- release candidate: v0.10.1 / versionCode 40;
- main branch Android workflow: PASS;
- main branch Validate workflow: PASS;
- modular PulseDeck HUD pack is present under `skin/pulsedeck_hud/`;
- Skin Engine v1 is manifest-driven and uses the modular PNG blocks.

Last phone evidence before v0.10.1:
- v0.10.0 rendered the PNG skin successfully;
- controls and reactor were visible and functional;
- visual defects were obvious: rectangular seams between image layers, reactor composition too heavy, transport/quick-action stack too high, excessive empty space at the bottom.

v0.10.1 correction already committed and CI-passed:
- manifest-level `blend: screen`, `opacity`, `cropTop`, `cropBottom` support;
- energy/glow layers use screen compositing;
- particle layers were added around the reactor;
- duplicated reactor energy was reduced;
- reactor was moved slightly upward;
- transport rail moved lower;
- quick-actions rail moved lower;
- build version bumped to v0.10.1 / 40.

Important:
- v0.10.1 has NOT yet been visually accepted on phone.
- Next session starts with installing/testing v0.10.1 and sending one fresh main-screen screenshot.
- Do not redesign the architecture before that screenshot; first judge the actual v0.10.1 composition.


## v0.11.0 — reference-first master-plate reset

Reason:
- repeated runtime composition of tiny PNG rings/buttons produced visibly worse results than the original approved artwork;
- the user explicitly prioritized visual fidelity over animation/dynamic behavior for the next pass;
- the new rule is: reproduce the approved page first, then gradually restore dynamic behavior without changing the accepted visual baseline.

Architecture reset:
- new optional skin: `skin/pulsedeck_master/`;
- two large precomposed 20:9 master assets: playing + paused;
- one manifest with invisible normalized hit zones;
- `PulseDeckMainSkinView` prefers the master plate when present;
- modular `pulsedeck_hud` remains as fallback only;
- master-plate mode keeps chrome permanently visible; auto-hide is intentionally bypassed;
- Android actions still work through invisible hit zones;
- visual fidelity takes priority over reactive motion for this milestone.

Import workflow:
- package: `PulseDeck_MasterPlate_v1.zip`;
- SHA-256: `edf839ad45c09f1bb0b19f14e0a218dac428a21f0793d083c3b7454034a5cb0d`;
- Termux menu item 19 imports the pack into `skin/pulsedeck_master/`, commits, rebases and pushes.

Release:
- v0.11.0
- versionCode 44

Acceptance target:
- page should visually resemble the original approved reference much more closely than v0.10.x;
- no runtime recomposition of individual rail/button rings;
- transport and quick-action artwork should read as one coherent designed block;
- movement/reactivity/dynamic typography can be reintroduced after visual acceptance.


## v0.11.1 — center calibration tool

User center-map review preserved in `docs/design/PULSEDECK_CENTER_CALIBRATION.md`.

Known wrong centers from the user's review:
- C-01, C-02, C-04;
- C-11, C-12, C-13, C-14, C-15;
- C-17, C-18, C-19, C-20.

Centers that seem approximately correct:
- C-05, C-06, C-07, C-08, C-10.

Not explicitly evaluated yet:
- C-03, C-09, C-16.

Temporary phone calibration workflow:
- main-screen Menu opens `PulseDeckCenterCalibrationActivity`;
- the page renders the real PulseDeck skin as the background;
- a drafting-style crosshair is shown for one C-number at a time;
- drag the crosshair to the exact visual center;
- tap the crosshair to save normalized coordinates and advance;
- swipe left/right to change C-number without saving;
- long-press the crosshair to copy all saved coordinates;
- after C-20, all saved coordinates are copied automatically.

The accepted calibrated coordinates will become the source of truth for the next visual rebuild. Do not overwrite accepted calibrated points with old guessed values.


## Sleep checkpoint — v0.11.4 calibration remote

Checkpoint HEAD:
- `4176500fd0d57ffe5d6d73f5530c84b2da16ac25`

Release state:
- v0.11.4 / versionCode 48;
- Validate workflow: PASS;
- Android workflow for v0.11.4: still running at checkpoint time;
- last fully Android-PASS build before this: v0.11.3 / versionCode 47.

Current visual baseline:
- Clean Blocks v2 is active and visibly different on phone;
- reactor/transport/quick actions now come from the clean grouped-block approach;
- no more independent ring recomposition for transport/quick actions;
- the user accepted this as a better baseline for further calibration, but exact object centers are still pending.

Center review state preserved:
- wrong: C-01, C-02, C-03, C-04, C-11, C-12, C-13, C-14, C-15, C-17, C-18, C-19, C-20;
- seems approximately correct: C-05, C-06, C-07, C-08, C-09, C-10, C-16.

Calibration tooling now implemented:
- main-page Menu opens `PulseDeckCenterCalibrationActivity`;
- draggable drafting-style crosshair;
- floating draggable CENTER REMOTE;
- D-pad pixel movement;
- step cycle: 1 / 2 / 5 / 10 / 20 px;
- PREV / SAVE / NEXT / RESET / COPY;
- panel position and step persisted;
- current point and saved coordinates persisted;
- output includes normalized coordinates for screen-independent layout work.

Important responsive-layout rule:
- the skin design canvas remains authoritative;
- never stretch X and Y independently;
- use uniform scaling to preserve circles/proportions;
- store accepted centers as normalized coordinates;
- on different aspect ratios, adapt background/crop/outer breathing room rather than deforming the HUD geometry.

Resume point:
1. wait for / confirm Android PASS for v0.11.4;
2. install latest APK;
3. open Center Calibration from top-right Menu;
4. calibrate C-01…C-20 using the floating remote;
5. COPY and paste the resulting coordinate block into chat;
6. rebuild the main-page geometry from accepted calibrated centers only.


## v0.11.6 — stable draft centers + magnifier

Calibration stability:
- every crosshair move is persisted immediately as a per-point draft;
- panel interaction no longer resets the unsaved editing position;
- point load priority is draft -> saved -> baseline;
- RESET clears only the current point's saved + draft position.

Magnifier:
- live circular loupe renders the real PulseDeck skin under the target;
- MAG ON/OFF;
- ZOOM cycles 2x / 3x / 4x / 6x;
- SIZE cycles S / M / L = 120 / 180 / 240 dp;
- FOLLOW keeps the loupe near the target;
- dragging the loupe disables FOLLOW and stores its screen position;
- FREEZE stores a snapshot around the current target;
- loupe position, zoom, size, follow and freeze settings persist.

Remote:
- existing D-pad + 1/2/5/10/20 px step remains;
- PREV / SAVE / NEXT / RESET / COPY remain;
- panel itself remains draggable and touch-isolated from crosshair placement.

Release:
- v0.11.6
- versionCode 50

Phone acceptance:
1. place target roughly;
2. tap/use remote without the target jumping or reverting;
3. use 1 px step for fine alignment;
4. verify magnifier 2x/3x/4x/6x;
5. verify S/M/L loupe sizes;
6. drag loupe; target must not move;
7. FREEZE/UNFREEZE;
8. rotate and verify draft target + loupe settings survive.


## v0.11.7 — guide lines + grouped calibration remote

New calibration guides:
- live dashed crosshair axes remain on the main canvas;
- the magnifier now draws its own dashed X/Y axes on top of magnified content, clipped exactly to the loupe circle;
- V GUIDE stores a persistent vertical guide at the current target X;
- H GUIDE stores a persistent horizontal guide at the current target Y;
- CLEAR removes both fixed guides;
- fixed guides stay in place while the target moves and survive recreation through SharedPreferences.

Remote organization:
- controls are visually grouped into MOVE / MAGNIFIER / GUIDES / ACTIONS tiles;
- guide controls are placed below magnifier controls and above action controls;
- existing target draft persistence, D-pad, step, magnifier and save/copy logic remain.

Release:
- v0.11.7
- versionCode 51

Phone acceptance:
1. magnifier shows dashed axes all the way to its circular edge;
2. V GUIDE stays at its X while target moves;
3. H GUIDE stays at its Y while target moves;
4. CLEAR removes both guides;
5. panel groups are visually separated and all controls still capture touch independently from the target.


## PulseDeck v0.13.0 — approved object centers + control scale pass

- [x] Save the phone-exported C-01…C-20 object template in `skin/pulsedeck_hud/object_templates/PulseDeck_object_template_centered_v1.json`.
- [x] Preserve exported dx/dy values verbatim; do not overwrite immutable `PULSEDECK_CENTER_CALIBRATION`.
- [x] Record the layout contract in `docs/design/PULSEDECK_OBJECT_LAYOUT_BASELINE.md`.
- [x] Scale C-17…C-20 quick-action icons to 150% around their accepted centers.
- [x] Scale Play to 170% around C-13.
- [x] Scale Pause to 170% height and effective 136% width around the same C-13 center.
- [x] Version candidate: v0.13.0 / build 64.
- [ ] Android workflow PASS for exact candidate HEAD.
- [ ] Phone visual acceptance of enlarged lower controls and Play/Pause.
- [ ] After phone PASS, freeze the scale pass as the approved visual baseline.


## PulseDeck v0.14.0 — modular main page candidate

- Final phone-exported C-01…C-20 layout promoted to the repository baseline.
- Main `PulseDeckMainSkinView` loads the approved object-template offsets from assets.
- Now Playing explicitly uses modular HUD mode; the old master plate is no longer the main-page renderer.
- S1…S6 grouping remains available in Object Constructor via GROUP / UNGROUP.
- Approved scale contract remains: C17…C20 150%, Play 170%, Pause 170% height / 136% width.
- Main-page real actions: Back, Menu/tools, Seek, Play/Pause, Repeat One, Theme, Board, Visualizer, Export.
- Shuffle / Previous / Next remain blocked on real playback queue foundation; no fake behavior.
- Candidate version: v0.14.0 / build 66.
- Pending: exact-HEAD Android workflow PASS and phone acceptance of the assembled main page.


## PulseDeck v0.14.2 — mirrored live spectrum + chrome visibility

- Static C-08 waveform PNG is intentionally hidden on the modular main page; it remains an asset/reference, not the live equalizer.
- Live FFT spectrum is mirrored from screen center toward both edges.
- Each side uses 36 thin bars (72 visible bars total), with low frequencies nearest center and higher frequencies toward the edges.
- Bars grow upward from a common baseline and use a height gradient: cool white at baseline, warm mid, red at the peak.
- C-08 approved center offset remains the anchor for the live spectrum.
- Auto-hide default is now NEVER while HUD visual work is in progress.
- PulseDeck tools → Автоприховування persists one of: Не ховати / Ховати тільки керування / Ховати керування + нижню панель.
- S5 transport and S6 quick actions have independent runtime visibility; metadata/progress/live spectrum stay visible.
- Candidate version: v0.14.2 / build 68.
- Pending: exact-HEAD Android workflow PASS and phone visual acceptance.


## v0.18.0 — over-visualization split

Recovered/confirmed on 2026-10-04:
- canonical app repository remains `faric-ua/faric-music-visualizer`;
- the previous attempt did not lose the implementation: current main already contains the new 8-slot `PulseDeckLayerStack` and `OverVisualizationView`;
- target stack is now:
  `0 Visualizer → 1 Надвізуалізація → 2 Big Equalizer → 3 GF → 4 GIF → 5 Effects → 6 PulseDeck 🔒 → 7 Service`;
- Layer 0 and Layer 1 have separate persistent visibility controls;
- `447504` is stored as `skin/pulsedeck_hud/over_visualization/over_visualization_447504.webp` and is loaded by `OverVisualizationView`;
- the Android app includes the repository-level `skin/` directory through `assets.srcDir("../skin")`, so this binary WebP is packaged without a duplicate copy under `app/src/main/assets`;
- Layer 1 uses center-crop + 5% overscan per edge + SCREEN blend;
- old PulseDeck Photo Reactor logic was removed from the active path;
- PulseDeck internal subsystem numbering is now 6.0–6.5, matching locked Layer 6;
- the old layer architecture document was stale and has now been updated to the implemented 0..7 contract.

Next evidence gate:
1. Android workflow PASS for exact v0.18.0 HEAD;
2. install exact signed APK on the phone;
3. verify Layer 0 and Layer 1 independently ON/OFF;
4. verify 447504 crop/overscan on the physical display;
5. verify all higher layers preserve the new +1 Z-order and PulseDeck geometry is unchanged.

## v0.18.1 — Layer 1 artwork correction

User clarified that 447504 itself must be the full Layer 1 content.

Corrected:
- Layer 0 remains the independently toggleable Visualizer.
- Layer 1 remains independently toggleable, but now renders the complete 447504 image normally at full opacity.
- Removed SCREEN blend behavior that treated the image as a transparent effects overlay.
- Center-crop + 5% per-edge overscan stays.
- Higher Z-order remains unchanged: 2 Big Equalizer → 3 GF → 4 GIF → 5 Effects → 6 PulseDeck 🔒 → 7 Service.

Next: exact-head Android build, then phone verification of Layer 1 ON/OFF independently from Layer 0.

## v0.18.2 — Layers panel rebuild

The layer-control problem was traced to both UI and rendering issues.

Current control contract:
- Layer 0 Visualizer — parent ON/OFF.
- Layer 1 Надвізуалізація / 447504 — parent ON/OFF.
- Layer 2 Big Equalizer — parent ON/OFF.
- Layer 3 GF — parent ON/OFF plus independent GF objects: background/glow, frame, FX, creature, wordmark.
- Layer 4 GIF/Animation — parent slot shown; currently empty.
- Layer 5 Effects — parent ON/OFF.
- Layer 6 PulseDeck 🔒 — container remains locked ON, but internal groups 6.0 Atmosphere, 6.1 Reactor, 6.2 Track UI, 6.3 Transport, 6.4 Quick Actions, 6.5 Navigation and their objects are independently toggleable.
- Layer 7 Service Overlay — shown as service slot.

Important fixes:
- old single-choice + multi-choice AlertDialog collision removed;
- toggles no longer dismiss/reopen the panel;
- GF Layer 3 no longer paints an opaque fullscreen black background;
- hidden PulseDeck parent rails still resolve geometry, preventing child icons from jumping;
- object visibility persists and is reapplied on recreation.

Candidate: v0.18.2 / build 81.
Next gate: exact-head Validate + Android PASS, then physical-phone QA of nested toggles and lifecycle persistence.

## v0.18.3 — draggable layers panel

Phone UX correction:
- the Layers/Object panel is now a floating control surface about 30% of screen height rather than a near-fullscreen dialog;
- it can be dragged anywhere on screen by the header;
- background dimming is removed, so visual changes are visible immediately;
- the panel does not close/reopen when toggles change;
- X/Y position persists;
- header includes reset-to-center and close controls;
- outside-panel touches are allowed to reach Now Playing;
- layer/object hierarchy from v0.18.2 remains unchanged.

Candidate: v0.18.3 / build 82.

## v0.18.4 — Layer 0 + GF independence

Phone feedback drove the next composition correction.

Current contract:
- Layer 0 Visualizer is a composite layer:
  - `projectm` = the last preset selected in the Visualizer/projectM screen;
  - `faric_reactive` = FARIC ReactiveScene foreground.
- Returning from projectM automatically enables Layer 0 + projectM and rebuilds Now Playing with the selected preset.
- SceneOrchestrator is no longer gated by PlaybackThemeId.VISUALIZER; Layer 0 and Layer 3 can run simultaneously.
- Layer 3 GF always mounts the current production Cyber Shark set independently from playback-theme selection.
- Layer 3 exposes five current renderable GF objects: Background/Glow, Frame, FX, Creature, Wordmark.
- Background/Glow now has the same persistent per-object transform contract as other GF objects: X, Y, scale, rotation, opacity.
- Board Transform includes BG/Glow plus Center / 100% / Reset quick controls.
- Layers panel includes direct GF Background settings entry.
- Layers panel can export a JSON snapshot of layer visibility, object visibility, GF transforms/reaction and projectM preset id.

Asset boundary:
- the app currently has one production-ready modular GF set in code: Cyber Shark.
- the wider FARIC/FMV/FVMP master archive remains reference/LFS material; those masters are not yet separated into app-ready transparent frame/creature/wordmark/FX packs, so they must not be pretended to be live Layer 3 assets.

Candidate: v0.18.4 / build 83.
Next gate: exact-head Android PASS, then phone verification of projectM return-to-main, simultaneous Layer 0 + GF, BG/Glow transforms, and JSON export.

v0.18.4 CI evidence:
- Validate #652 — PASS on app/source SHA `b81e81743b5fd628fdfae9fbcb70841dcbe600db`.
- Android #387 — PASS on the same app/source SHA.
- Unit tests, debug APK build, stable signer verification, zipalign/badging checks and artifact upload all completed successfully.
- Later documentation-only commits do not change APK source; the Termux APK downloader may legitimately select run #387 for the current docs-only HEAD.

## v0.18.5 — edit GF against the real visualization

Phone evidence clarified two UX/runtime issues.

Layer panel:
- parent layer remains the master switch;
- child object states are still persisted independently;
- when a parent such as L3 is OFF, child controls are now dimmed/disabled instead of appearing fully active.

Board Transform:
- no longer opens an isolated HeroBoardView over black;
- now reconstructs the real lower composition below GF: L0 Visualizer + L1 447504 + L2 Big Equalizer, using the same persisted visibility state;
- L3 is forced visible only inside the editor so the user can position it;
- individual GF object visibility still matches live state;
- projectM/FARIC reactive rendering remains active in Board Transform;
- preview uses the full physical viewport, matching Now Playing geometry.

Export Lab:
- new "Відкрити папку експорту" button;
- PNG: Pictures/FARIC;
- video with audio: Movies/FARIC (MP4 container with AAC audio, not MP3).

Candidate: v0.18.5 / build 84.
Next gate: exact-head CI then phone QA of L3 hierarchy, live BG/Glow positioning over visualization, and folder opening.

## v0.18.6 — reactive Cyber Shark proof export

UI:
- L3 keeps the parent checkbox and now has a compact gear button at the right edge for Background/Glow transform settings.
- the old full-width `GF / Background · позиція / масштаб` row is removed.
- Export folder chooser is now a custom styled PulseDeck dialog with:
  - `Кадри PNG · Pictures/FARIC`;
  - `Відео зі звуком · Movies/FARIC`;
  - close X + explicit `Скасувати`;
  - system Back/outside touch also dismisses.

Export:
- Cyber Shark is now a deterministic proof-capable scene.
- renderer uses the real Cyber Shark frame / creature / wordmark resources plus reactive procedural GF background/FX.
- saved GF group transform/reaction, each BoardLayerTransform and each GF child visibility are applied to PNG/MP4 proof.
- Export Lab chooses Cyber Shark as the proof source whenever Layer 3 GF is enabled, regardless of the current Theme Picker id.
- 3-second MP4 proof reacts to offline analysis and contains AAC audio in the MP4 container.

Scope boundary:
- this proof is the current GF/Cyber Shark renderer, not yet a full flattening of every PulseDeck layer. projectM / Layer 1 / Big EQ / locked PulseDeck HUD are not yet composited into this deterministic proof path.

Candidate: v0.18.6 / build 85.
Next gate: exact-head CI, then phone comparison of live GF vs PNG/MP4 proof.

## v0.18.7 — composition export v1

The export path has moved from single-theme/GF proof to an active-layer composition renderer.

Current deterministic export stack:
- L0 FARIC reactive visualizer — included when parent + `faric_reactive` are ON.
- L0 projectM — detected and shown in Export Lab, but not yet rendered; native GL capture remains the only current gap.
- L1 447504 — included with the live center-crop/overscan contract.
- L2 Big Equalizer — included and driven by offline signal.
- L3 Cyber Shark GF — included with saved group/per-object transforms, visibility and audio reaction.
- L4 GIF — currently empty.
- L5 Effects — included as transparent atmospheric SceneSpec FX.
- L6 PulseDeck — included from modular skin with the 29 persisted object switches, audio state, metadata and progress.

PNG and MP4 use the same `CompositionExportRenderer`.
MP4 remains a 3-second proof at the existing proof resolution/FPS path with H.264 video + AAC audio.

Candidate: v0.18.7 / build 86.
Phone acceptance should compare the exported composition against the live layer menu configuration. projectM mismatch is expected until the dedicated native-GL export step.

## v0.18.9 — phone acceptance

CI:
- Validate #694 — PASS.
- Android #421 — PASS.
- Exact app/source SHA: `0f8dd2445970`.

Phone evidence from exported `447712.mp4`:
- 3.008 s, 1080×1920, 30 FPS;
- H.264 video ~12.4 Mbps;
- AAC stereo 48 kHz ~160 kbps;
- Layer 0 contains the captured projectM image;
- Layer 3 Cyber Shark and higher reactive/HUD elements continue changing during the clip;
- projectM itself remains static across the clip, which matches the current snapshot-bridge contract.

Interpretation:
- v0.18.9 snapshot bridge = PASS for MP4 inclusion.
- Remaining projectM gap is now specifically dynamic frame-by-frame projectM export, not missing projectM composition.
- PNG exact-moment verification is still open.

## v0.19.0 — dynamic projectM export candidate

The projectM export path is no longer limited to one static GL snapshot for MP4.

Current flow:
1. Now Playing → Export still captures one exact live projectM snapshot for PNG/fallback.
2. Export Lab creates its own live projectM GLSurfaceView using the same selected preset.
3. When the user starts the 3-second MP4 export, FARIC samples that live GL framebuffer at 30 FPS in real time.
4. Frames are cached temporarily as JPEG quality 95 files rather than retained as raw Bitmaps.
5. The H.264 composition pass reads frame N from that sequence and composites it as L0 below the normal FARIC/GF/FX/PulseDeck layers.
6. Temporary frame files are removed after export.

Important architecture note:
- this is true moving projectM video in the exported MP4, but it is a **live GL capture pipeline**, not yet a deterministic offscreen projectM clock;
- the selected preset receives the normal live projectM PCM/signal feed while capture is running;
- static snapshot remains a fallback if the live export surface is unavailable.

Release candidate: v0.19.0 / build 89.
Phone acceptance is required before calling dynamic projectM export closed.

## v0.19.1 — offline full-song export candidate

This changes the export contract fundamentally: the final video is no longer driven by whatever the live player happens to be doing.

Full-song flow:
1. User performs offline analysis for the selected local song.
2. `Експортувати всю пісню MP4` fixes the export range to 0:00 → source end.
3. If projectM is active, FARIC decodes/caches mono PCM from the source file and resets projectM to a clean export state.
4. Every video frame sets projectM to the explicit export time `frameIndex / 30`, feeds the matching PCM slice + offline SceneSignal, renders GL, reads that frame and composites the normal layers.
5. FARIC/GF/EQ/FX/HUD are rendered from the same offline song timestamp.
6. The finished H.264 stream is muxed with AAC transcoded from the original source audio, not captured from device playback.

The main player may be playing or paused and may be at any position; it is not the export clock and its PCM is suppressed from projectM while Export Lab owns the manual engine.

Current quality target remains 1080p-class / 30 FPS / H.264 + AAC.

Compatibility:
- FARIC still pins projectM 4.1.7.
- One narrow build-time patch backports the external frame-time API that is intended upstream for projectM 4.2.
- Stable-upgrade procedure is documented in `PROJECTM_4_2_MIGRATION.md`; do not track moving upstream 4.2 code.

CI foundation:
- Validate #735 — PASS on `9123cbfbf61d`.
- Android #456 — PASS on the same SHA, including native build and signed APK verification.
- Validate #738 — PASS on documentation HEAD `387401c29190b3553c823e1c72b17c32d41f98a4`.
- Android #457 — PASS on v0.19.1 app/source SHA `fd4bf265436507d7126d390fc56824dca7e21e81`.
- Signed/debug phone artifact: `FARIC-Music-Visualizer-v0.19.1-Debug` (artifact id `11356342984`); later commits after the release SHA are documentation-only.

Release candidate: v0.19.1 / build 90.
Next gate: phone testing only — 3-second offline preview first, then full-song 0:00→end export, cancellation and long-song stability.


## v0.19.2 — fast export candidate

v0.19.1 proved the correct offline full-song timeline but exposed a production performance problem: every Full HD frame was copied to an IntArray and converted from ARGB to YUV420 in Kotlin before MediaCodec, while projectM also allocated large readback objects every frame.

v0.19.2 keeps the same deterministic behavior but changes the hot path:
1. The normal composition is still rendered from the explicit offline song timestamp.
2. The final ARGB Bitmap is uploaded through an EGL texture to a MediaCodec input Surface.
3. `eglPresentationTimeANDROID` gives each encoded frame the exact offline `frameIndex / fps` timestamp.
4. The old `Bitmap.getPixels -> IntArray -> manual ARGB/YUV420` conversion is removed.
5. projectM offline readback reuses its large RGBA buffer, pixel array and Bitmap instead of allocating/recycling them every frame.

Known-good pre-version-bump source:
- Validate #749 — PASS on `09b0346310ef2164e0c565fac0866f2775d717ce`.
- Android #461 — PASS on the same SHA.

Release candidate: v0.19.2 / build 91.
Next gate: exact-head CI, then phone A/B timing using the same song that was slow on v0.19.1. First confirm the 3-second preview orientation/colors, then measure full-song export speed.


### v0.19.2 CI closeout

- Android #462 — PASS on v0.19.2 app/source `bcab5fc8a1910581627fc82737ab0528e976b702`.
- Signed/debug artifact: `FARIC-Music-Visualizer-v0.19.2-Debug`, artifact id `11361439127`.
- Artifact digest: `sha256:4549cdb9198d1abd8b8f2ca741146da547875051d2c30497a95ebce053438f75`.
- Validate #753 — PASS on documentation HEAD `3e12cc87ed5e0d3ffa52eb13637660c70557b698`.

Current gate is phone QA only. Do not change export architecture again before checking:
1. 3-second preview orientation/colors/audio;
2. same-song v0.19.1 vs v0.19.2 full-export wall-clock time;
3. Cancel behavior;
4. several-minute stability.

If speed is still unacceptable, the next performance target is projectM synchronous framebuffer readback (`glFinish + glReadPixels + RGBA->ARGB`), not the H.264 YUV path that v0.19.2 already removed.


## v0.19.3 — projectM raw readback candidate

User phone evidence showed a 3.008-second / 90-frame preview taking roughly 28 seconds wall-clock, even after v0.19.2 removed the final ARGB->YUV CPU conversion. This identified projectM readback as the next bottleneck.

v0.19.3 preserves preview semantics exactly: the 3-second preview starts from the current playback scrubber position. Do not change this to 0:00.

Optimization:
- dynamic offline projectM frames no longer run a Kotlin loop over every RGBA pixel;
- the reusable GL buffer is copied directly to the reusable Bitmap;
- no explicit `glFinish()` is issued before the dynamic offline `glReadPixels`;
- vertical flip and R/B correction are applied at composition draw time;
- final video encoding still uses EGL -> MediaCodec Surface from v0.19.2.

Known-good pre-release source: `73cd730ea9394dd550d3dd87d2de133dc0e7465e`.
- Validate #759 — PASS.
- Android #464 — PASS.

Release candidate: v0.19.3 / build 92.
Next phone metric: rerun the same 3-second preview and compare wall-clock time against the previous ~28 seconds.


### v0.19.3 CI closeout

- Android #465 — PASS on exact v0.19.3 app/source `7add461579d259150f52e860d14ad6df226e2793`.
- Artifact: `FARIC-Music-Visualizer-v0.19.3-Debug`, id `11364403633`.
- Artifact digest: `sha256:0c15d5ae800ea295541a07b90837f79222e311e48481dd3c93366f71b9701cdc`.
- Validate #763 — PASS on the v0.19.3 documentation handoff SHA `5e653e9baf8b90959eb5016c73efd5f9977c8386`.

Current gate is phone QA only. The 3-second preview semantics are locked: start from current scrubber position, render the next 3 seconds. The performance baseline to beat is ~28 seconds wall-clock for a 3.008-second / 90-frame preview.


### v0.19.3 phone timing evidence

User screen recording `447864.mp4` confirms the same 3-second preview now finishes in roughly 16 seconds wall-clock. The previous v0.19.2 baseline was roughly 28 seconds.

Result:
- performance improvement: about 43% less wall-clock time;
- effective render speed: still only about 0.19x realtime / ~5.3x slower than realtime;
- preview semantics remain current-position based;
- export completes and publishes a 1080x1920 / 90-frame result.

This is a meaningful PASS for the raw-readback optimization, but NOT a production-speed PASS. Next performance target: reduce the cost of offline projectM rendering/readback itself without changing preview semantics or the 30 fps final video contract.


## v0.19.4 — export geometry performance candidate

v0.19.3 phone evidence: the same 3-second / 90-frame preview improved from ~28 seconds to ~16 seconds after the raw projectM readback optimization.

v0.19.4 targets wasted projectM pixels. Export Lab now requests a manual projectM surface matching the selected output geometry (9:16 / 16:9 / 1:1 / 4:5) before applying the existing projectM render scale, instead of inheriting the whole phone-screen geometry. This preserves 30 FPS and the deterministic timeline while reducing render/readback work that would otherwise be cropped away.

Release candidate: v0.19.4 / build 93.
Phone baseline to beat: ~16 seconds for the same 3-second preview.


### v0.19.4 CI runner hold

Implementation and release metadata are ready on main:
- v0.19.4 / build 93;
- export projectM surface matches the selected output geometry before applying the existing render scale;
- 3-second preview contract remains current scrubber position -> next 3 seconds;
- v0.19.3 phone baseline remains ~16 seconds for 3.008 seconds / 90 frames.

Current infrastructure gate:
- Android #468 for release source was queued and later cancelled while retrying the same workflow concurrency group.
- Android #467 attempt 2 (geometry implementation SHA `2cfda7ccef77c4449ae60bb151d1d0e9905ee3c8`) was manually rerun and remains queued with no runner assigned.
- Validate jobs are likewise queued/pending; no build/test failure has been reported.
- Do NOT create a v0.19.4 BUILD_CHECKPOINT until an Android run actually completes PASS.

Prepared separately without changing main:
- branch `perf/export-stage-timing`;
- head `5a48bf33b938858f715b2f7925bd309d25f8e37a`;
- measures projectM frame-provider time, composition time, and encoder-submit time for the 3-second preview and exposes those numbers in the completion toast.
- Use this only if v0.19.4 geometry optimization is still too slow after phone timing.

Resume:
1. check Android #467 attempt 2 / current main Android workflow state;
2. if PASS, record BUILD_CHECKPOINT immediately;
3. ensure exact v0.19.4 / build 93 signed artifact exists;
4. phone A/B the same 3-second preview against the ~16-second v0.19.3 baseline.


### CI failure diagnosis — 2026-10-05

The apparent failures around v0.19.4 are infrastructure startup failures, not source/build failures.

Confirmed:
- Android #467 attempt 2, SHA `2cfda7ccef77c4449ae60bb151d1d0e9905ee3c8`:
  - run-level conclusion: failure;
  - build job conclusion: cancelled;
  - `runner_id = 0`, empty runner name;
  - `steps = []`;
  - billable Ubuntu time: 0 ms;
  - queued/start window lasted about 15 minutes before GitHub ended it.
- Validate #775, SHA `c5dc167a96ab70341142d13b0ffb0f510a2f5903`:
  - run-level conclusion: failure;
  - validate job conclusion: cancelled;
  - `runner_id = 0`, empty runner name;
  - `steps = []`;
  - billable Ubuntu time: 0 ms;
  - also lasted about 15 minutes without acquiring a runner.

Therefore neither run reached Checkout, tests, Gradle, Android build, signing, or artifact upload. There is currently no evidence of a v0.19.4 code/compile/test failure.

Do not mark v0.19.4 BUILD PASS until a real Android run receives a runner and completes successfully.


### v0.19.4 phone QA — cold-cache observation

User recording `447892.mp4` starts with PCM preparation and therefore cannot be compared directly with the prior v0.19.3 warm-cache timing.

Observed:
- PCM preparation: roughly 12-13 seconds;
- transition to `Рендерю 3 секунди…`;
- render phase: roughly 15-16 seconds;
- completion: 1080x1920 / 90 frames.

Required next phone test: immediately rerun the same 3-second preview on the same track without restarting the app or rerunning offline analysis. The second run should reuse PCM cache and give an apples-to-apples comparison with the v0.19.3 ~16-second render baseline.

Also upload the finished 3-second MP4 for orientation/color/audio/crop inspection.


### v0.19.4 phone QA — warm-cache result

Evidence:
- screen recording: `447899.mp4`;
- exported preview: `FARIC-preview-1791251406632.mp4`.

Observed warm-cache path:
- PCM preparation is only a brief transition;
- `Рендерю 3 секунди…` starts at roughly 2 s in the screen recording;
- completion appears at roughly 18 s;
- effective render wall-clock is about 15.5-16 s.

Conclusion:
- no meaningful speed improvement versus v0.19.3 (~16 s);
- export-geometry optimization is visually correct but is not the dominant bottleneck.

Output verification:
- 1080x1920;
- 30 FPS;
- 90 video frames;
- video duration 3.000 s;
- container/audio duration 3.008 s;
- H.264 High, ~12.45 Mbps;
- AAC LC stereo 48 kHz, ~160.8 kbps;
- preview starts at the current playback position (~0:31) and advances to ~0:34;
- orientation/colors/crop appear correct.

Next resume step:
use the prepared `perf/export-stage-timing` instrumentation to measure projectM, composition, encoder submit, audio transcode, mux, publish, and total times before choosing the next optimization.


### v0.19.5 phone QA — timing UI finding

Evidence:
- screen recording: `447911.mp4`;
- exported preview: `FARIC-preview-1791252342014.mp4`.

Observed:
- first run after installing v0.19.5 rebuilt PCM cache (~12-13 s);
- export phase remained roughly ~16-17 s wall-clock;
- output verified as 1080x1920, 30 FPS, 90 frames, 3.008 s, H.264 + AAC 48 kHz stereo;
- orientation, colors, moving projectM, HUD/layer composition, and current-position preview behavior appear correct.

Diagnostic UI defect:
- v0.19.5 correctly computes per-stage timings internally;
- Android system Toast truncates the multi-line result after the first visible lines;
- therefore exact projectM/composition/encoder/audio/mux/save/total values could not be read from the phone recording.

Current candidate:
- v0.19.6 / build 95;
- source SHA `a4309529161e40bc0e8dca3bf1686ee4807bd608`;
- replaces the truncated Toast with a persistent `AlertDialog` containing the full timing breakdown;
- Validate #787 PASS;
- Android #474 pending at time of this handoff update.

Resume:
1. wait for Android #474 PASS;
2. record BUILD_CHECKPOINT immediately;
3. user workflow: Termux 3 -> 10 -> 8;
4. install v0.19.6 and run one 3-second preview;
5. send a screenshot/recording of the result dialog; use exact numbers to choose the next performance optimization.


### v0.19.6 exact stage profile

Phone evidence:
- `447915.mp4` screen recording;
- `447916.mp4` exported preview.

Exact export timings shown by the v0.19.6 result dialog:
- projectM frame generation: 1896 ms;
- composition: 11808 ms;
- encoder submit: 435 ms;
- audio transcode: 424 ms;
- mux: 112 ms;
- save/publish: 215 ms;
- total: 15575 ms.

Share of total:
- composition ~75.8%;
- projectM ~12.2%;
- encoder ~2.8%;
- audio ~2.7%;
- mux ~0.7%;
- save ~1.4%;
- unclassified setup/finalization ~4.4%.

Conclusion: stop optimizing encoder/audio/mux for now. Composition is the dominant bottleneck.

Current candidate:
- v0.19.7 / build 96;
- source SHA `a5f7ac9c4baa6d670afe1879f217838ff51b5a7d`;
- adds internal composition timing for clear, projectM draw, FARIC reactive, overlay, Big Equalizer, Cyber Shark, effects, PulseDeck update, and PulseDeck draw;
- next: wait for Android #478 PASS, checkpoint it, then phone test one 3-second preview.


### v0.19.7 layer-level bottleneck confirmed

Phone evidence:
- screen recording `447926.mp4`;
- exported preview `447927.mp4`.

Exact timing:
- projectM generation 2063 ms;
- composition 12998 ms;
- encoder 453 ms;
- audio 475 ms;
- mux 140 ms;
- save 199 ms;
- total 17008 ms.

Composition breakdown:
- clear 85 ms;
- projectM draw 6471 ms;
- FARIC reactive 0 ms;
- overlay 0 ms;
- Big EQ 0 ms;
- Cyber Shark 4691 ms;
- effects 450 ms;
- HUD update 26 ms;
- HUD draw 1270 ms.

Current candidate:
- v0.19.8 / build 97;
- source SHA `83512d24b74962ea7a93757577149b0c0e6aa7f8`;
- uses BGRA readback on GPUs advertising `GL_EXT_read_format_bgra`;
- compatible path lets ARGB_8888 receive B,G,R,A bytes directly and bypasses the software R/B ColorMatrix during projectM Canvas draw;
- fallback remains the v0.19.7 RGBA + ColorMatrix path;
- result dialog shows whether BGRA fast path was active.
- Android #483 / Validate #802 are the exact CI runs to watch.

Resume:
1. wait for Android #483 PASS;
2. checkpoint immediately;
3. user Termux 3 -> 10 -> 8;
4. install v0.19.8;
5. run one warm-cache 3-second preview;
6. compare projectM draw / composition / total and verify colors/orientation;
7. if BGRA is active and projectM draw drops materially, optimize Cyber Shark next.


### Current resume point — v0.19.9 Cyber Shark optimization

v0.19.8 phone result:
- `projectM BGRA: yes`;
- projectM generation/readback 5318 ms;
- composition 8077 ms;
- projectM draw 1643 ms;
- Cyber Shark 4676 ms;
- effects 428 ms;
- HUD draw 1222 ms;
- total 15118 ms.
- Output `447935.mp4` is visually correct and technically 1080x1920 / 30 FPS / 90 frames / 3.008 s.

Current candidate:
- v0.19.9 / build 98;
- source SHA `5bd6945f43fb3f4348085e008ff490a20468c9ab`;
- change: in CyberSharkExportRenderer.drawBackground(), use ordinary canvas.save() when effective layer opacity is 255 instead of allocating a full-screen saveLayerAlpha; partial-opacity path is unchanged.
- Validate #807 PASS.
- Android #485 pending at this handoff point.

Resume:
1. wait for Android #485 PASS;
2. write BUILD_CHECKPOINT immediately;
3. user Termux 3 -> 10 -> 8;
4. install v0.19.9;
5. run one warm-cache 3-second preview;
6. compare Cyber Shark / composition / total against v0.19.8 values 4676 / 8077 / 15118 ms;
7. if Cyber Shark remains dominant, profile/optimize its background vs bitmap layers vs FX next.


### Current resume point — v0.19.10 projectM offline profiler

v0.19.9 is phone-accepted for performance:
- projectM 5005 ms;
- composition 6860 ms;
- projectM draw 1640 ms;
- Cyber Shark 3523 ms;
- HUD draw 1176 ms;
- total 14031 ms.
- Cyber Shark opaque saveLayer optimization reduced Cyber Shark by ~24.7% and total by ~7.2%.

Current candidate:
- v0.19.10 / build 99;
- adds projectM offline internals: queue wait, native render, glReadPixels, Bitmap copy;
- no intentional visual/export behavior change;
- after build PASS, one warm-cache 3-second preview is enough to identify the next projectM optimization.


### Current resume point — v0.19.11 copy timing text

v0.19.10 phone result identified the current performance target:
- projectM total 5270 ms;
- queue wait 16 ms;
- native render 730 ms;
- glReadPixels 4361 ms;
- bitmap copy 97 ms;
- total export 15287 ms.
glReadPixels is the dominant GPU->CPU readback bottleneck.

Current candidate:
- v0.19.11 / build 100;
- adds `Копіювати текст` to the export result dialog;
- copies the complete timing block into Android clipboard;
- copy button does not dismiss the dialog;
- no render/performance changes.
Resume after Android PASS: user uses Termux 3 -> 10 -> 8, installs v0.19.11, runs one 3-second preview, taps `Копіювати текст`, and pastes the result directly into chat. After UX PASS, continue with glReadPixels optimization.
