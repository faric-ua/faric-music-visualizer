# Current Handoff

Project: FARIC Music Visualizer / PulseDeck

Verified:
- repository / Termux workflow;
- Android scaffold CI;
- stable signer pipeline;
- local audio playback works on the real phone;
- reference screenshots analyzed as interaction patterns;
- original FARIC PulseDeck design accepted by user.

Current release:
- v0.2.1 PulseDeck fullscreen hotfix;
- source implementation candidate is being built/tested;
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
