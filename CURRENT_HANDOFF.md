# Current Handoff

Project: FARIC Music Visualizer / PulseDeck

Verified:
- repository / Termux workflow;
- Android scaffold CI;
- stable signer pipeline;
- local audio playback works on the real phone;
- reference screenshots analyzed as interaction patterns;
- original FARIC PulseDeck design accepted by user.

Current release candidate:
- v0.8.0 floating PulseDeck controls foundation;
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
