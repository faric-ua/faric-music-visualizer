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
