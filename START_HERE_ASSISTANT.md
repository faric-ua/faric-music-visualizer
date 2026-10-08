# FARIC Music Visualizer — START HERE


## v0.19.49 / build 138 — expandable «Герої» & Pictures2.zip (CI PASS / PHONE QA PENDING)

- New PR #5 groups Cyber Shark/Panther and 10 new user-provided PNG themes in a two-column thumbnail grid. Neon/Energy/Orbital/Star/Wave/Vinyl/Cassette/Visualizer remain separate. Group expand and import do not intentionally recreate the live Scene Host.
- Artwork input: user `Pictures2.zip` (10 transparent 1122x1402 composite art sheets). Explicit Android SAF ZIP import stores originals in app-private files; **the APK does not yet bundle this ZIP**. New figures initially use a cropped single-emblem preview, not approved modular Frame/FX/Creature/Wordmark. Full asset separation and Cyber Shark-grade visuals remain separate open art QA.
- Code/PR source `dcd41b2ae2a477f9c6181ebb5edcdf62f4c13933`; Validate #1092 PASS and Android #615 PASS, run `37825625591`, artifact `FARIC-Music-Visualizer-v0.19.49-Debug` id `11570559985` (expires 2026-10-11).
- **Next:** finish PR #5 / verify signed Android build on `main`, write release checkpoint; only then Termux **3 → 10 → 8**. Phone QA: Hero group expand, Pictures2 ZIP 10/10, previews/selection, fast scene return, legacy Shark/Panther, projectM system bars and 3-second MP4. No Phone QA PASS yet. PERF-NAV-002 and BUG-EXPORT-001 remain open.


## v0.19.48 / build 137 — Canonical Theme Host + visible system bars in projectM

- **User phone QA, build 136:** Shark ↔ Panther switches quickly, legacy projectM menu correct, Board and Theme browse return smooth. **FAIL remains** when changing from a layered GF theme to Neon/Energy/Core/Star/Wave/Vinyl/Cassette or back: legacy Theme Picker path calls `showNowPlaying()`, `clearScreenRefs()`, and recreates projectM. projectM authoring still hides Android **status + navigation bars**, contrary to original UI. Clips `607032.mp4`, `607033.mp4` and screenshot `607030.jpg` supplied in chat.
- **v0.19.48 source candidate:** unified live routing for all implemented Playback Themes, mounted `HeroThemeView` alongside `HeroBoardView` in the existing GF slot, switch visible central renderer without calling `clearScreenRefs` or constructing a new projectM view. Bitmapped Shark/Panther remain background-loaded; Neon Emblem, Energy Core, Orbital Crown, Star Seed, Wave Idol, Vinyl and Cassette switch an existing Canvas view; VISUALIZER shares the same return path. No schema migration, no alteration of immutable center calibration.
- Canonical catalog: duplicate Wave Idol entry removed while preserving its ID and original first description; registry unit tests assert unique theme IDs and expected implemented themes. Unimplemented Portrait Halo / Glass Core / Poster stay `СКОРО`.
- projectM overlay explicitly calls WindowInsetsController.show(systemBars), applies system bar/cutout padding to overlay controls only, and restores player immersive mode upon exit. onWindowFocusChanged honors projectM-specific bar policy, rather than immediately rehiding them.
- **Status: Validate #1089 PASS / Android #613 PASS / PHONE QA PENDING.** PR #4 merged into main at app/source `edd69d6cbf5c3abb4bc7859f5c54a5f9cdd7a3ff`. Android run `37813832199`, signed artifact `FARIC-Music-Visualizer-v0.19.48-Debug`, ID `11566826167` (expires 2026-10-11). Immutable checkpoint branch `checkpoint/pulsedeck-v0.19.48-2026-10-08`. Next Termux **3 → 10 → 8**, no build 9. Phone-test all live Themes, projectM both Android system bars, Board, 3s MP4. PERF-NAV-002 still OPEN pending physical validation; BUG-EXPORT-001 independent.


## v0.19.47 / build 136 — original projectM UI + live hero switching (CI PASS; PHONE QA PENDING)

- Code in main: `f5ce3e90a33122ee59c3599cc0830b29f08c8093`; PR #3 merged.
- Validate #1086 PASS; Android #611 PASS, run `37810162017`, artifact `FARIC-Music-Visualizer-v0.19.47-Debug`, id `11565016839`, expires 2026-10-11.
- Immutable checkpoint branch: `checkpoint/pulsedeck-v0.19.47-2026-10-08`.
- v0.19.46 phone findings: Board, projectM and Themes browsing smooth; projectM legacy menu was replaced and Shark ↔ Panther still jerked. v0.19.47 restores original projectM 4-row controls, status and ratings within persistent scene; loads GF assets off UI and swaps only HeroBoardView in-place. `FARIC-nav` reports assetDecode and UIApplyAndReturn milliseconds.
- **Phone QA PENDING.** Next: Termux **3 → 10 → 8** (do not press 9), install build 136. Test original projectM menu, Back/ratings/AUTO/FG; Shark ↔ Panther ×4 without visible stall or GL recreate; Board and Themes Back ×3; short MP4.
- PERF-NAV-002 remains PARTIAL until theme switch phone PASS; BUG-EXPORT-001 remains OPEN for full-song export.


## v0.19.47 / build 136 — original projectM UI and smooth hero switching

Phone QA of build 135: Board, Themes browse and projectM overlay navigation are smooth. Remaining issue: Shark ↔ Panther swap causes a severe hitch. User asks to restore old projectM layout. Candidate build 136 restores its original four control rows, status, ratings, auto interval and icon Back in the existing overlay. Theme assets are decoded on worker and swapped in place without rebuilding native projectM. CI/phone acceptance pending; keep PERF-NAV-002 open for hero change. See CURRENT_HANDOFF.md.

## PERMANENT RESPONSE FORMAT — READ FIRST

Before replying about FARIC Music Visualizer releases, builds, Termux or phone QA, read [`ASSISTANT_RESPONSE_CONTRACT.md`](ASSISTANT_RESPONSE_CONTRACT.md). Finish relevant answers with **(1) exact Termux menu steps** and **(2) 3–6 short APK phone tests (action → expected result)**. This is a repository-wide user requirement and survives new chats; never substitute a long changelog for the phone-test checklist.

## ARCHITECTURE CANDIDATE — v0.19.46 / build 135 (persistent Scene Host)

- User videos `606999.mp4` (Playback Themes ↔ Player transitions) and `607000.mp4` (3s exported MP4) reinforce that screen navigation should not own GL lifecycle. Most recent 1080×1920 / 90-frame report: **5699 ms**, projectM 790, composition 2005, HUD draw 1097; export timing is not navigation latency.
- Decision and acceptance contract: `docs/architecture/PULSEDECK_PERSISTENT_SCENE_HOST.md`. **Single native projectM view and one live PulseDeckLayerStack** on foreground playback. Board and Theme Picker draw menu overlays; projectM editor uses in-Activity controls, no native release or second Activity for ordinary installed projectM. Legacy Activity kept for initial library setup fallback.
- **CI PASS / PHONE QA PENDING.** PR #2 merged into main at app/source `6ce0a33a893edc78b1126e98ff5cf39181f9daaa`. Validate #1081 PASS; Android #607 PASS (run `37804612518`), artifact `FARIC-Music-Visualizer-v0.19.46-Debug` id `11562008086` (expires 2026-10-11). Immutable source branch `checkpoint/pulsedeck-v0.19.46-2026-10-08`. **Next: Termux 3 → 10 → 8 (do not launch build 9), install build 135 and physically test.** Do not call navigation PASS yet.
- QA: Player ↔ Board x3 (preserve same rendered background), Player ↔ projectM editor with app/system Back x3 (same native instance, tuned FG/NEXT), open/cancel Theme Picker x3, prefs/Frame/Center survive, 3-second export regression. Full-song export bug remains separate and OPEN.

## SOURCE CANDIDATE — v0.19.45 / build 134 (projectM exit GL ownership)

- **New phone evidence on v0.19.44 / 133:** projectM → app Back FAIL; projectM → Android system Back FAIL; Board and Frame/Center NOT TESTED; 3-second MP4 PASS (1080×1920, 90 frames, total 5789 ms). PERF-NAV-002 remains OPEN.
- **Audit:** ProjectMActivity.exitToPlayer nulled projectMView before Android onPause; old continuous GLSurfaceView may render against destroyed singleton bridge. This is a code-level race risk, not a phone-confirmed root cause.
- **v0.19.45 source:** keep exiting view until release callback, explicitly GL-pause before finish; add volatile release guard to skip old renderer onDrawFrame/onSurfaceChanged; log exit release/pause timings under FARIC-nav.
- **CI / PHONE QA: PENDING.** No claim of improved smoothness until tested. Validate + Android build on new main commit, then Termux 3 → 10 → 8. Test both projectM Back paths 3 times, Board, Frame/Center retention, 3s MP4. BUG-EXPORT-001 still OPEN.

## NEW SOURCE CANDIDATE — v0.19.44 / build 133 (navigation timing and GL release)

- Parent app release v0.19.43 / build 132 had **phone QA 3 PASS/2 FAIL**: export report Copy/OK PASS; player ↔ projectM and player ↔ Board transitions FAIL. QA is saved in `docs/checkpoints/2026-10-08-v0.19.43-export-copy-lifecycle.md`.
- Code candidate **v0.19.44 build 133**: async GL release before Board/Player screen rebuild, coalescing repeat navigation; separate projectM return restores only visualizer layer in the existing player stack (not full PulseDeck/HUD recreation); timing diagnostics in `FARIC-nav` and `FARIC-projectM`.
- This is a **targeted partial fix**: generic screen teardown, EGL attach and native initialization may still pause, and unrelated full-song export bug is not fixed. **Do not mark PERF-NAV-002 PASS until phone confirms.**
- **CI SUCCESS:** Validate #1075 PASS / Android #604 PASS. Exact APK source `cf3a517695a591df933137b767956358a1458a59`; artifact `FARIC-Music-Visualizer-v0.19.44-Debug` id `11554186369`; immutable branch `checkpoint/pulsedeck-v0.19.44-2026-10-08`. **Phone QA PENDING.** Full instructions `docs/checkpoints/2026-10-08-v0.19.44-nav-handoff.md`. **Termux 3 → 10 → 8**; do not launch build 9. Test Board/player and projectM/player Back routes repeatedly; no lost background.
- Canonical docs rule still applies; no new visual features or user-facing menu labels were introduced.

## PHONE QA — v0.19.43 / build 132: 3 PASS / 2 FAIL

- **Phone-accepted:** 3-second export report opens with Copy/OK; Copy copies without dismissing; OK closes report only — **3/3 PASS**. No repeated QA needed for unchanged report code.
- **Phone-FAILED:** main player → projectM → Back and main player → Board → Back — **2/2 FAIL**, freezes/visualizer recreation. **PERF-NAV-002 is the next engineering priority**; no fix or new APK yet.
- Exact shipped build remains **v0.19.43 / 132**, Android #603 PASS, source `364f39627f7bb41c7315021b91f484b72ebdc63c`. Current `main` may contain docs-only edits. The latest APK artifact expires **2026-10-11**, but previously downloaded APK remains in Android Documents.
- **Next:** instrument and address synchronous `MainActivity.clearScreenRefs()` GL release and repeated projectM initialization. Preserve shared singleton safety. CI + phone tests before closing PERF-NAV-002. See `OPEN_FINDINGS.md`.

## DOCUMENTATION SYNC / RECOVERY — mandatory contract (2026-10-08)

- **Authoritative plan:** `docs/user/DOCUMENTATION_SYNC_CONTRACT.md`. This is the persistent and explicitly scoped commitment for future chats: keep the guide updated with each user-facing change, safely preserve completed documents, and work toward fully automated documentation delivery.
- **Already working:** main Termux **3** synchronizes committed tracked GitHub files; main menu **20** opens Documentation. Markdown source lives in `docs/user/FARIC_USER_GUIDE_UK.md` and survives chat loss if committed. **No assistant continues working in the background after a chat stops.**
- **Still NOT automatic:** published screenshot-rich PDF/DOCX/HTML remain from chat/Downloads and do not update via `3`. `20 → 1` can generate HTML from Markdown only when pre-imported HTML is absent; do not call HTML freshness guaranteed with an imported older copy.
- **Next planned engineering work — DOCSYNC-001:** reproducible CI builds HTML/PDF/DOCX, manifest + SHA256, durable GitHub publishing, read-only safe Termux sync during item **3** with unchanged menu 1–20, preserves previous docs, checks QA. **DOCSYNC-002:** CI/release documentation freshness guard. These are explicit TODOs, not shipped.
- User-identified cosmetic issue: stretched Shark cover in older PDF must be corrected with aspect-ratio-safe rendering when PDF/DOCX rebuilt.
- **Handoff rule:** at each release and before chat migration link this contract and update version / app SHA / guide revision / CI and phone QA. No false claim of full sync or remote durability for files only attached to chat.

## TERMUX DOCUMENTATION SUBMENU — 2026-10-08

- In `scripts/termux-menu.sh` the single new **`20 — Documentation →`** entry opens `tools/termux/documentation-menu.sh`. Main menu options **1–19 remain unchanged**. Android APK source/version is unchanged (still v0.19.43 / build 132).
- Nested menu: 1 HTML (offline), 2 PDF, 3 DOCX, 4 canonical Markdown guide in GitHub, 5 other project documents in nested list, 6 one-time import of previously delivered ZIP or separate guide files from Android Downloads, 7 local documents folder, 0 Back.
- ZIP import: `tools/termux/import-documentation.py`, local destination `/storage/emulated/0/Documents/FARIC-Music-Visualizer/documentation/v0.19.43/`. User needs to download `FARIC_User_Guide_UK_v0.19.43_COMPLETE.zip` from conversation into Download before first offline use; **binaries are not automatically downloaded from GitHub**. Don't imply they are available just because Markdown is committed.
- Source of truth: `docs/user/FARIC_USER_GUIDE_UK.md`; user guide operation: `docs/user/TERMUX_DOCUMENTATION_MENU.md`; guide update contract: `docs/user/README.md`.
- Open cosmetic finding: PDF cover Shark illustration has visibly stretched proportions on user screenshot `1791460405967.jpeg`. Future PDF/DOCX reissue should preserve image aspect ratio; **not fixed by menu change**.
- Test gate: `Validate` shell syntax + ZIP path-traversal smoke; phone QA of Termux submenu still needed. No Android build required.

## USER MANUAL / USER GUIDE — canonical documentation (2026-10-08)

- The user's comprehensive Ukrainian manual is **`docs/user/FARIC_USER_GUIDE_UK.md`**; entrypoint/update rules **`docs/user/README.md`**.
- Edition 1.0 covers Android v0.19.43/build 132: local file selection and PulseDeck playback, working themes, Layers/Board GF, projectM Center/Edge/AUTO, Composition Sets, Export Lab PNG/3s MP4/full-song status, supported ratios, saved files, common bugs, and pending features. It has user-friendly routes and a term glossary.
- Two user-downloadable artifacts **PDF (11 pages with clickable contents and 4 prior-build test screenshots) and editable DOCX** were created in the originating conversation; they are not stored as binary repo assets. GitHub canonical Markdown embeds existing repository reference illustrations and is the update source.
- **From now on, for user-facing feature changes/update releases update the guide and `docs/user/README.md`**. Do not silently mark planned features as working, or phone-untested features as accepted. When regenerating PDF/DOCX, render and inspect.
- Keep technical Termux developer menu commands out of Android user-facing guide unless explicitly placed in a separate developer appendix. Current known defects: PERF-NAV-002, BUG-EXPORT-001, PERF-PLAYER-001.

## NEW VIDEO RESEARCH TRACK — Heroes vs Audioforms (2026-10-08)

- Proposed user-visible categories: **Герої (Heroes)** = Cyber Shark / Cyber Panther illustrated theme packs; **Аудіоформи (Audioforms)** = new independent bass/beat-deforming shapes; **Оформлення (Decor)** = frames/logos; **Ефекти (FX)** = particles/glow. Keep projectM as its own visualizer engine. This is a **naming/architecture proposal pending approval**, not a code rename.
- Read `docs/architecture/VISUAL_ELEMENTS_TAXONOMY.md` before changing any GF/Board labeling. Existing GF ids, `PlaybackThemeId`, JSON prefs/Composition Sets MUST remain backwards-compatible.
- User wants **two parallel ChatGPT chats** in the same project: this main chat continues implementation/CI/Termux/phone QA; a dedicated **Audioforms Reference Lab** chat accepts each original video, analyzes actual movement/audio and writes approved concepts to `docs/visualizer/AUDIOFORMS_VIDEO_REFERENCE_WORKFLOW.md`. No source edits/builds from research chat without explicit approval.
- Video not yet attached in this new user turn; do not claim to have seen its central morphing circle. Likely candidate to investigate: audio-reactive radial contour with bass radius expansion, mid distortion, high shimmer and beat impulse; validate after viewing source.
- Current build is still **v0.19.43 / build 132 (Android #603 PASS)**; phone QA for Copy → stays open → OK still PENDING. Navigation freeze PERF-NAV-002 and full-song crash BUG-EXPORT-001 OPEN. This research track has not fixed them.
- Assistant cannot inspect user's remaining image generation quota or next reset time; user can try ChatGPT Images / provide limit popup, then request actual generation when ready.

## CURRENT SOURCE CANDIDATE — v0.19.43 / build 132 — Copy stays open until OK

- Phone-reported 3-second export timing: 1080×1920, 90 frames, **5976ms total**, projectM 791ms, composition 1935ms, GPU direct, encoder 873ms, GPU overlay 664ms, HUD draw 1051ms. Export result text was successfully copied, but the dialog dismissed automatically on Copy.
- User expected `Копіювати текст` to copy without dismissal, and `OK` to close. Source **v0.19.43 (build 132)** now does that: `PulseDeckDialogs.Action.dismissOnClick` defaults true; only export report `showMessage(keepOpenOnSecondary=true)` keeps its Copy action open. Other dialogs retain existing behavior.
- Validate **#1037 PASS**, Android **#603 PASS**, exact APK source `364f39627f7bb41c7315021b91f484b72ebdc63c`; artifact `FARIC-Music-Visualizer-v0.19.43-Debug`, id `11526295056`. **Phone QA PENDING**. Immutable checkpoint: `checkpoint/pulsedeck-v0.19.43-2026-10-08`; full resume: `docs/checkpoints/2026-10-08-v0.19.43-export-copy-lifecycle.md`.
- **Next:** Termux **3 → 10 → 8** (Android #603 PASS; no new build) → install build 132 → 3-second export → scroll → Copy → report remains visible → OK dismisses. Prior v0.19.42 Frame / BG-Glow 5/6 PASS remain accepted; navigation smoothness still OPEN.
- Independently OPEN: BUG-EXPORT-001 (~48% full-song export app exit) and PERF-NAV-002 (transition freezes) — no fix for either here.

## PHONE QA — 2026-10-08 — v0.19.42 / build 131

- **5/6 PASS**: Frame auto-spin +30°/s / −30°/s / 0°/s, Frame only, unchanged manual angle; Board and Composition Set persistence; BG/Glow one line; 3-second spinning-Frame MP4 and visible Copy/OK.
- **1/6 NOT TESTED**: navigation player ↔ projectM / both Back buttons. Prior stutter remains PERF-NAV-002 OPEN; separate app Back icon was already visible.
- New user-export report: **1080×1920, 90 frames, 5976ms**, projectM 791ms, composition 1935ms, encoder 873ms, GPU direct; GPU overlay 664ms, HUD draw 1051ms. Full metrics in `docs/checkpoints/2026-10-08-v0.19.42-frame-spin.md`.
- Copy report works but auto-closes in build 131. Follow-up source v0.19.43 changes Copy to **keep the dialog open** until OK; phone QA still pending. Full scrolling and OK tap were not individually tested. Full-song export crash remains OPEN.
- No rebuild required to record results. **Next engineering priority: PERF-NAV-002** (shared projectM lifecycle stalls).

## CURRENT SOURCE CANDIDATE — v0.19.42 / build 131 — Frame rotation and BG/Glow tab

- User screenshots: `606829.jpg` Frame slider view, `606831.jpg` visible app Back, `606827.jpg` Board Frame editor; auto-spin requested and BG/Glow chip is two lines.
- Implemented: signed Frame-only `autoRotationDegreesPerSecond` control -180..180°/s (0 OFF), manual rotation unchanged; persisted per layer/theme and Composition Set; applied in HeroBoardView and CPU/GPU export using shared time-based rotation helper; BG/Glow selector enlarged and single-line.
- Build evidence: Validate #1033 **PASS**; Android #602 **PASS** (run `37718078044`) from SHA `d3bfbeb20fcc3168dc52db038d76f06841d3bc71`. Signed artifact `FARIC-Music-Visualizer-v0.19.42-Debug` id `11525215496`, expires 2026-10-11. **Phone QA 5/6 PASS; navigation NOT TESTED on v0.19.42**. Recovery: `docs/checkpoints/2026-10-08-v0.19.42-frame-spin.md` / `checkpoint/pulsedeck-v0.19.42-2026-10-08`.
- Phone result from previous navigation fix: physical projectM Back button **now visible**, but transition freeze **still FAIL/open**. Root cause candidates: UI-thread synchronous `clearScreenRefs` GL release and re-creation, native renderer startup. See `OPEN_FINDINGS.md` PERF-NAV-002. No fix to navigation in this version.
- Retain pending v0.19.41 fixed-footer export report phone QA, and BUG-EXPORT-001 full-song exit ~48%.
- Next: **Termux 3 → 10 → 8** (no repeat build); install build 131; verify BG/Glow one line, signed Frame speed incl OFF/independent manual angle, settings/Composition Set persistence, 3-second video. Track separate unresolved navigation stall.
- Follow permanent format `ASSISTANT_RESPONSE_CONTRACT.md`.

## CURRENT SOURCE CANDIDATE — v0.19.41 / build 130 — fixed export report actions

- **Observed bug:** screenshot `1791424966546.jpeg`: export result's timing report extends off screen; buttons `Копіювати текст` and `OK` are hidden. A separate completed 3-second exported video `606819.mp4` (1080×1920, 30fps; report says 90 frames and total 5754ms) confirms short export works, **not** full-song stability.
- **Source change:** `PulseDeckDialogs.showMessage(scrollableMessage = true)` opt-in used by `MainActivity.showExportTimingResult`; report message occupies a weighted ScrollView in a bounded-height dialog, title and Copy/OK action footer remain fixed outside scroll. Generic small app dialogs remain unchanged.
- **Status:** Validate #1030 **PASS**, Android #600 **PASS**, source `08c78cd419e658841495d92b96b12f19b6f35e14`; artifact `FARIC-Music-Visualizer-v0.19.41-Debug`, ID `11523783977`. **Phone QA pending**. This change fixes only report UX, not full-song crash. Recovery branch: `checkpoint/pulsedeck-v0.19.41-2026-10-08`.
- **Next:** Build is already PASS. Termux **3 → 10 → 8** (no rebuild); install build 130, run 3-second export, scroll the report while Copy / OK stay visible, test both buttons. Retest v0.19.40 nested Layers / Back findings separately.
- **Open:** BUG-EXPORT-001 full-song export app exit ~48% (unconfirmed root cause); PERF-PLAYER-001 irregular movement; projectM Back/lifecycle remains unaccepted until phone QA. See `OPEN_FINDINGS.md`.
- **Contract:** `ASSISTANT_RESPONSE_CONTRACT.md`: Termux menu 3 / 10 / 8 after CI PASS, then 3-6 specific phone tests.

## PHONE QA EVIDENCE — older v0.19.39 / build 128 (user report 2026-10-08)

- **5/6 PASS**: tap Scale value → 1.00x reset only; +30°/s spin; -30°/s reversed and 0°/s stopped; speed persistence on reopen (combined test included Composition Set save/load); main player Center spin and 3s MP4 export.
- **1/6 NOT TESTED on this build**: on-screen / Android system Back.
- Do **not** override earlier v0.19.38 Back transition video report of pauses, and do not call the v0.19.40 fix accepted until a post-fix test.
- v0.19.41 short export screenshot: 1080×1920, 90 frames, total 5754ms was produced on an **older APK**, not phone evidence for fixed Copy/OK.
- New build recovery: `docs/checkpoints/2026-10-08-v0.19.41-export-report.md`.

## CURRENT SOURCE CANDIDATE — v0.19.40 / build 129 — nested panel and navigation fix

- Video findings: `606815.mp4` demonstrates Layers parent closing on Visualizer ⚙; `606816.mp4` shows navigation delays; `606817.jpg` confirms missing visual Back control.
- Source fixes: (1) preserve Layers parent when FG settings opens, (2) load projectM Back asset at correct packaged path, (3) replace 1.5-second UI-thread `releaseProjectMBlocking` on entering projectM with queued GL release + callback, (4) show brief return status, maintain shared renderer ordering.
- Existing v0.19.39 reset-on-number and FG Center spin controls remain included.
- Status: **Validate #1028 PASS, Android #599 PASS** on source `1cc7c889b7b79657f1d3958bf86d7e7ce09a2014`; APK `FARIC-Music-Visualizer-v0.19.40-Debug` artifact id `11524315208`. **Phone QA pending**; full smoothness not claimed. Recovery: `docs/checkpoints/2026-10-08-v0.19.40-layers-navigation.md` and `checkpoint/pulsedeck-v0.19.40-2026-10-08`.
- Other open bugs: BUG-EXPORT-001 (reported full-song export process exit at ~48%, root cause unconfirmed) and PERF-PLAYER-001 (live/export motion irregularity still needs separate timing analysis).
- Next: Termux **3 → 10 → 8**, install build 129; phone-check nested Layers → ⚙ → X, visible Back, repeated player ↔ projectM transitions and renderer stability. Exact menu format: `ASSISTANT_RESPONSE_CONTRACT.md`.

## CURRENT SOURCE CANDIDATE — v0.19.39 / build 128 — individual FG reset + auto-spin

- **Source feature commit:** `951506c33070bc46c2c6615d72000f93a657d72d` (subsequent documentation-only commits may advance main).
- **Changes implemented:** tap each numeric FG value to reset only that field; Center adds signed auto-spin speed -180..+180 °/s, 0=off, default 0; saved to projectM state and Composition Sets; same shader applies in preview and deterministic frame-time export.
- **Validation:** CI Validate #1022 PASS on the feature commit. Android #598 triggered automatically; **do not claim Android PASS or APK ready until that run completes successfully.**
- **Open user-reported blocker:** BUG-EXPORT-001 — existing build 127 reportedly exits at ~48% of full-song export near concurrent player end. Root cause **unknown**; separate from UI changes. Need crash/process-death diagnostics and controlled export reproductions; no export fix is claimed.
- **Open visual issue:** PERF-PLAYER-001 — jerkiness / asymmetric entrances. User refers to a video but there is no attached video file in this turn. Need actual video for frame-level diagnosis.
- **Previous release:** v0.19.38 / build 127 had 5/6 phone checks PASS; system Back / return smoothness now **reported FAIL** on v0.19.38 video evidence; v0.19.40 needs retest.
- **Phone QA after Android #598 PASS:** (1) tap modified Scale/Opacity value → individual reset; (2) Center auto-spin +30/-30/0°/s → both directions and stop; (3) restart/reopen and Composition Set save/load persist speed; (4) tune in main player and confirm live full composite; (5) run 3s MP4 export proof; (6) test both Back paths. **Do not demand a repeated full-song export without obtaining crash logs first.**
- **Single next action:** Check Android #598 status and compile result, inspect build logs on failure; when PASS, record a new `BUILD_CHECKPOINT` and present exact Termux 3 → 10 → 8 steps. User installs and performs targeted phone QA.

Source details: `docs/visualizer/PROJECTM_AUTHORING_WORKFLOW.md`; findings: `OPEN_FINDINGS.md`; reply format: `ASSISTANT_RESPONSE_CONTRACT.md`.

## CURRENT CANDIDATE — v0.19.38 / build 127 — movable projectM authoring panel

Source of truth:
- `docs/visualizer/PROJECTM_AUTHORING_WORKFLOW.md`

Phone evidence / decisions:
- [x] v0.19.37 Center and Edge tuning controls work and visually match PulseDeck.
- [x] First tuning dialogs are too large/modal for practical visual tuning because they cover the composition.
- [x] projectM Back no longer corrupts the main renderer, but the blocking release still causes a short visible freeze before exit.
- [x] AUTO 5/10/15 UX was ambiguous: screenshot showed `MANUAL` while the lower button still showed `5s`; interval alone did not re-enable AUTO.

Implemented in v0.19.38:
- [x] New reusable `ProjectMSettingsPanel`: PulseDeck-styled, movable, non-dimming, non-modal, persistent X/Y position.
- [x] One composite settings panel with tabs: **AUTO / CENTER / EDGE FX**.
- [x] Center/Edge edits remain live while the panel stays open and can be dragged away from the area being judged.
- [x] Separate projectM authoring screen opens the movable panel from **⚙ FG**.
- [x] Main player Layers -> Visualizer now has its own **⚙** button, analogous to the GF gear, opening the same projectM tuning panel over the full composite player.
- [x] AUTO button semantics are explicit:
  - manual state -> `AUTO OFF`;
  - active -> `AUTO 5s / 10s / 15s`;
  - tapping while manual re-enables AUTO at the current interval;
  - tapping while AUTO cycles 5 -> 10 -> 15;
  - selecting 5/10/15 in the settings panel enables AUTO immediately.
- [x] Back release is now asynchronous/non-blocking on the UI thread: finish happens only after queued GL release completes, preserving the renderer race fix without freezing the UI thread.
- [x] Existing FG tuning persistence/export/Composition Set integration remains intact.
- [x] Validate #994 PASS on release SHA `ca295375dfcba0765dd1761978123cfe4eb5ba4d`.
- [x] Android #591 PASS · artifact `FARIC-Music-Visualizer-v0.19.38-Debug` · id `11519777909` · digest `sha256:3dd2964c231e0c5e373c4b08bcdaafef16ff1272232f73b922827f58f940cba3`.
- [ ] Phone QA: 5/6 user-reported PASS; **Back lifecycle remains untested**. AUTO 5s preview confirmed; 10s/15s and AUTO export sequencing not independently confirmed.
- [ ] Panther remains a separate unfinished Shark-grade art task; do not benchmark Panther yet.

Phone QA — user-reported results (2026-10-08, v0.19.38 / build 127):
- [x] **PASS** — **⚙ FG** panel drags with finger.
- [x] **PASS** — changing **Center scale** gives immediate visible result without closing panel.
- [x] **PASS** — panel position persists after close/reopen.
- [x] **PASS** — **AUTO 5s** visibly changes preview visualization approximately every 5 seconds.
- [x] **PASS** — main player **Шари → Visualizer → ⚙** allows editing over full composition.
- [ ] **NOT TESTED** — **Back lifecycle**: leave projectM using on-screen Back, return and then use Android system Back; ideally repeat several times. Expect prompt return, no frozen pause and no missing/black/glitched projectM visual when main player resumes.

Release status: **5/6 phone checks PASS; phone QA remains OPEN until Back is verified**. The user's reported results do not independently establish AUTO 10s/15s timing, Rotation, or deterministic timed switching during Export (which remains TODO). Do not trigger another build just for this QA note.

Next immediate user action: test both Back paths on the **already installed build 127** and report PASS/FAIL.


## CURRENT CANDIDATE — v0.19.37 / build 126 — projectM authoring controls

Source of truth:
- `docs/visualizer/PROJECTM_AUTHORING_WORKFLOW.md`

Phone finding:
- [x] projectM lifecycle glitch is fixed.
- [x] Preview can still feel slightly jerky after activity handoff; treat as non-blocking preview smoothness for now because final output is generated by Export, not screen recording.

Implemented:
- [x] AUTO preset interval is user-selectable: 5s / 10s / 15s.
- [x] Auto interval persists and immediately reschedules when AUTO is active.
- [x] projectM authoring screen adds `FG TUNE`.
- [x] FG Center live tuning:
  - Scale
  - Rotation
  - Opacity
  - Bass reaction
  - Mid reaction
  - High reaction
  - Beat reaction
- [x] FG Edge FX live tuning:
  - Opacity
  - Bass reaction
  - High reaction
  - Beat reaction
- [x] Native shader now receives Center transform/opacity/audio gains and Edge opacity/audio gains.
- [x] Main player and export preview read the same stored FG tuning.
- [x] Composition Sets now persist/restore projectM FG tuning and the auto interval.
- [ ] Phone QA controls and visual response.
- [ ] Later: add X/Y position, attack/release, color/palette and finer per-sample tuning if useful.

Phone QA:
1. Confirm `v0.19.37 · build 126`.
2. AUTO: cycle 5s -> 10s -> 15s and observe timing.
3. FG TUNE -> Center: change Scale/Rotation/Opacity/Bass/Beat.
4. FG TUNE -> Edge FX: change Opacity/Bass/High/Beat.
5. Back to main player and verify the same tuning remains.
6. Save a Composition Set, change tuning, reload the Set and verify restoration.
7. No Panther benchmark yet; Panther asset remains a separate unfinished art task.


## HARD VISUAL ASSET RULE — Cyber Shark quality benchmark

- Read and follow `docs/visualizer/ASSET_QUALITY_CONTRACT.md` for every new production image/graphic asset.
- Cyber Shark is the visual-quality benchmark: clean edges, high-resolution physical source, lossless transparency, no matte/halo/compression dirt.
- Prefer quality over APK/file size unless a measured runtime/memory issue requires optimization.
- Do not ship low-resolution production art that must be enlarged on phone/export.
- New GF/overlay/wordmark assets are not considered ready until phone/export QA is visually comparable to Cyber Shark.


## READY FOR PHONE REVIEW — v0.19.33 / build 122

Source/APK commit: `3bbf2f726679f17fadf8c9c331a0816c343806f2`.

Design/source of truth:
- `docs/visualizer/VISUAL_LAYER_REWORK_2026-10-07.md`

What this candidate changes:
- persistent live Cyber Shark / Cyber Panther selector: selection applies immediately and the chooser stays open until explicit **Закрити**;
- HeroBoardView switches GF assets/transforms/reactions live;
- Panther root cause identified: most previously committed Panther WebP files were corrupt/non-RIFF; the creature physical file has been replaced by a valid 256×256 WebP review asset;
- missing Panther Frame no longer renders the confusing cyan diagnostic circle;
- projectM screen now exposes three independent visual components: **BG**, **CENTER**, **EDGE FX**;
- main Layers → Visualizer exposes four independent user objects: **projectM**, **FARIC Reactive**, **FG Center**, **FG Edge FX**;
- projectM internal GL slot remains alive when BG is off but Center/Edge is on, so FG can be viewed without the projectM preset/background;
- component visibility persists and Composition Sets restore the new state.

Panther pack status:
- creature: valid physical review asset;
- wordmark: valid existing physical asset;
- frame/fx/full: old files are still flagged for physical replacement/validation; FX can use the existing procedural fallback; missing Frame is skipped.
- therefore this is a **visual direction / pipeline QA candidate**, not final Panther art lock.

Phone QA:
1. confirm start screen says `v0.19.33 · build 122`;
2. open GF picker, switch Shark ↔ Panther several times without closing it;
3. decide whether the new visible Panther direction is acceptable;
4. open projectM screen and independently toggle **BG / CENTER / EDGE FX**;
5. open Layers → Visualizer and verify projectM / FARIC Reactive / FG Center / FG Edge FX separately;
6. send a short video/screenshot; only after visible Panther is accepted do the export benchmark.


## v0.19.32 / build 121 — live GF selector + FG split

Read first:
- `docs/visualizer/VISUAL_LAYER_REWORK_2026-10-07.md`

Current implementation target:
- GF selector stays open and switches Shark/Panther live.
- HeroBoardView reloads GF assets/transforms/reactions without rebuilding Now Playing.
- projectM foreground is split into independently persisted `FG Center` and `FG Edge FX` visibility.
- ProjectM FG screen has separate Center/Edge controls.
- Main Layers -> Visualizer exposes projectM, FARIC Reactive, FG Center, FG Edge FX.
- Panther creature load logs alpha coverage; if the modular creature is effectively empty, the physical Panther full-pack file is used as a diagnostic fallback.
- If Panther is still not visibly recognizable after build 121, do not keep changing visibility logic: inspect/re-segment the physical Panther creature asset/bounds.

Phone QA order:
1. install v0.19.32 / build 121;
2. open GF picker and switch Shark ↔ Panther several times without closing it;
3. verify Panther on screen;
4. open projectM screen and test Center/Edge combinations;
5. verify Layers toggles FG Center/FG Edge FX;
6. only then collect Panther export benchmark.


## v0.19.30 / build 119 — visible installed-version marker

- Start/library screen now shows `v<versionName> · build <versionCode>` in a small muted label so phone screenshots immediately identify the installed APK.
- Current expected label: `v0.19.30 · build 119`.
- Later, if desired, expose this label through the normal object visibility system instead of removing the diagnostic marker.
- GraphicFigureCatalog currently contains both selectable GF ids: Cyber Shark and Cyber Panther. After installing build 119, verify the styled GF picker can switch to Cyber Panther before collecting the Panther benchmark.
- Android #537 is the build to use for this candidate; do not use an earlier in-between artifact merely because it completed first.


## HARD UI RULE — style inheritance

- Read and follow `UI_STYLE_CONTRACT.md` for every new app-owned UI element.
- New dialogs, menus, buttons, panels, cards and tools must inherit styling/behavior from the nearest existing parent or sibling component before inventing anything new.
- Do not introduce stock grey Android/Material UI into PulseDeck-owned flows when an existing PulseDeck custom-dialog/control family can be reused.
- Style inheritance includes interaction behavior: visible actions must be tappable, reachable and lifecycle-safe.


## LATEST READY CANDIDATE — v0.19.26 / build 115 — recovery + Sets + split Visualizer

- Status: CI PASS / phone QA pending.
- Exact APK/source SHA: `438f179cc08e305c7f8a6119191142de2cd98f91`.
- Validate #886 — PASS, run `37661790260`.
- Android #525 — PASS, run `37661789962`.
- Artifact: `FARIC-Music-Visualizer-v0.19.26-Debug`, id `11501991325`, digest `sha256:6479a72716822fe47135c956cd73bef8f8381b71b9c0692bcc8a1106e4b66615`.
- Emergency UI recovery is now implemented: on NOW_PLAYING, hold one finger still for ~10 seconds anywhere on screen to open recovery even if every HUD/menu entry was hidden. Actions: restore essential menu access or reset all HUD controls.
- Named Composition Sets are implemented under PulseDeck tools -> `Сети / Sets`: save current, load, overwrite, rename, delete. Set payload uses the existing layer config snapshot and includes top-level visibility, selected GF, GF child visibility/transforms/group reaction and projectM state/path when available.
- Visualizer stack is split into two independently toggleable top-level layers: L0 `Visualizer / projectM` and L1 `Visualizer / FARIC Reactive`; previous visibility is migrated once from the old combined VISUALIZER state.
- Remaining layers shift one slot deeper in the live stack while preserving relative order.
- v0.19.25 GF visibility isolation and generalized timing label fixes remain included.
- Immediate phone recovery/test: install v0.19.26; if menus are still hidden from persisted old state, hold ~10 seconds on NOW_PLAYING and choose `Відновити доступ до меню`. Then verify Layers shows separate projectM/FARIC rows and verify Sets save/load.
- Normal Termux path now that CI is already complete: `3 -> 10 -> 8`.


## LATEST FIX CANDIDATE — v0.19.25 / build 114 — GF visibility isolation

- Status: code committed; Android build/phone QA pending.
- Release HEAD: `3e24462c30b34cde8a111f3febd1870ea8ab5406`.
- v0.19.24 phone export showed no Panther creature/frame: `GPU wordmark: 12 ms`, but no `GPU creature` and no `GPU frame`; CPU FX remained active at 268 ms.
- Root cause: Graphic Figure object visibility used one shared preference key for all GF themes, so hidden Shark/Panther layer state leaked across figure selection.
- Fix: GRAPHIC_FIGURES visibility is now namespaced by GF theme id. Legacy global visibility is preserved only for Cyber Shark; a newly selected Cyber Panther starts with its supported layers visible by default.
- Timing text no longer hardcodes `Cyber Shark`; it prints `Graphic Figure · <selected GF>`.
- Next: build/install v0.19.25, select Cyber Panther and run the same 3-second / 90-frame export. Full Panther should show GPU frame + GPU creature + GPU wordmark when those layers are enabled.


## LATEST READY CANDIDATE — v0.19.23 / build 112 — generalized GF layers + Cyber Panther

- Status: CI PASS / phone QA pending.
- Exact APK/source SHA: `7f2b3373ef96cdcde33f40117c1cd9c44807d97a`.
- Validate #857 PASS, run `37553402776`.
- Android #509 PASS, run `37553402797`.
- Artifact: `FARIC-Music-Visualizer-v0.19.23-Debug`, id `11454285483`, digest `sha256:2dbdb8318cd07f2dbd3038cdbcb0e687187101e37be3de518267d3bc019fe417`.
- Exporter no longer requires the exact Cyber Shark child set for the direct-projectM GPU compositor: glow, frame, creature and wordmark are independently optional.
- The generic direct GPU order preserves the GF slots: projectM -> optional glow -> lower GF overlay -> optional frame -> FX overlay -> optional creature -> optional wordmark -> top effects/HUD overlay.
- Cyber Shark remains unchanged as the phone-known-good reference GF.
- Added Cyber Panther as the first alternate GF. It deliberately uses a different child set: Background/Glow + FX + Panther creature; Frame and Wordmark are absent.
- Cyber Panther artwork is generated once as a static procedural ARGB bitmap and therefore can use the same static GPU texture path as the Shark creature during export.
- Layer 3 now has a GF selector and persists the selected GF. Layer menu and Board editor expose only layers supported by the selected GF.
- Separate transform/reaction storage is retained per GF theme id.
- Correctness fallback paths remain for compositions that do not qualify for the advanced direct-projectM path.
- Next phone action: Termux 3 -> 10 -> 8. First run a Cyber Shark regression preview, then select Cyber Panther from Layer 3 and run the same warm-cache 3-second / 90-frame export. Verify Panther has no frame/wordmark, remains reactive, and timing shows GPU creature > 0 with GPU frame/wordmark = 0.

## LATEST PHONE-ACCEPTED RESULT — v0.19.22 / build 111 — GPU creature + wordmark PASS

- Phone QA: PASS on 1080x1920 / 90 frames.
- Visual QA: accepted by user.
- Exact APK source SHA: `1bf20367c15f109b934980bbccc64a2d74c59bea`.
- Timing: total 5451 ms; projectM 672 ms; composition 2306 ms; encoder 684 ms.
- Direct projectM path active: `projectM BGRA: GPU direct`.
- GPU timings: projectM 10 ms; glow 5 ms; frame 11 ms; creature 10 ms; wordmark 6 ms; overlay 503 ms.
- CPU Cyber Shark creature = 0 ms; wordmark = 0 ms. Cyber Shark total = 356 ms.
- Remaining largest CPU layer: HUD draw 1081 ms; effects 433 ms; FX 235 ms.
- Versus v0.19.21 total 6483 -> 5451 ms (-15.9%).
- Versus v0.19.11 baseline total 14925 -> 5451 ms (~63.5% lower; ~2.74x faster).
- v0.19.22 is accepted as the current phone-known-good export candidate.
- Next engineering step: before a risky HUD rewrite, profile/generalize export behavior for alternate layer sets and then add HUD subprofiling.

## LATEST READY CANDIDATE — v0.19.22 / build 111 — GPU Cyber Shark creature + wordmark

- Exact APK source SHA: `1bf20367c15f109b934980bbccc64a2d74c59bea`.
- Validate #841 PASS, run `37544681053`.
- Android #500 PASS, run `37544681027`.
- Artifact: `FARIC-Music-Visualizer-v0.19.22-Debug`, id `11450845435`, digest `sha256:c48e0a57ebbb45011f3a07f47aa3049fbd0aa400a5df53d47deea6dc3aa4bd0a`.
- v0.19.21 phone performance PASS: total 6483 ms, projectM 808 ms, composition 3255 ms, direct projectM FBO active and no glReadPixels block.
- v0.19.22 moves measured CPU creature 450 ms + wordmark 430 ms to dedicated static EGL textures while preserving their exact existing transforms.
- Z-order remains projectM -> glow -> background -> frame -> FX -> creature -> wordmark -> effects -> HUD.
- Direct projectM FBO, GPU glow and GPU frame are unchanged; advanced path has a v0.19.21 frame-only fallback.
- New metrics: `GPU creature` and `GPU wordmark`.
- BUILD_CHECKPOINT recorded.
- Next phone action: Termux 3 -> 10 -> 8; same warm-cache 3-second / 90-frame preview; paste full timing and report any creature/wordmark visual difference.


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
