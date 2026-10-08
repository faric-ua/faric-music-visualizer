# PulseDeck Persistent Scene Host — architectural contract (2026-10-08)

Status: **IMPLEMENTED CANDIDATE ON MAIN / Validate #1081 PASS / Android #607 PASS / PHONE QA PENDING**.

## Problem evidence
- Repeated navigation Player ↔ projectM / Board remained FAIL on v0.19.43 and projectM both Back routes remained FAIL on v0.19.44.
- v0.19.45 isolated a risky GL thread lifetime but it does not solve the fundamental recreate-every-screen architecture.
- User videos `606999.mp4` (screen-recorded Playback Themes ↔ Player transition, visible scene recreation) and `607000.mp4` (valid-looking short MP4 export) motivate a different ownership model. Files supplied in the conversation, not embedded in Git.
- Most recent user export timing: 1080×1920, 90 frames, 5699 ms; projectM 790 ms, composition 2005 ms, HUD draw 1097 ms, Cyber Shark 385 ms, GPU overlay 676 ms. Overlapping stage metrics are not additive and **do not measure UI navigation latency**.

## Non-negotiable ownership rules
1. A **single Scene Host** owns one `PulseDeckLayerStack` and (when installed) exactly one live `ProjectMView` while the player Activity is foreground and an audio track/session is active.
2. **Menus are overlays** in the same Activity: Player HUD, Board editor, projectM authoring and Theme Picker. Opening/closing a menu must NOT call `clearScreenRefs()`, `ProjectMBridge.destroy()`, construct another `ProjectMView`, or call `setContentView()` with a new scene.
3. **Board mode:** retain underlying projectM, reactive, GF, effects and transport audio state. Hide the standard HUD temporarily, draw Board editing controls above the identical live scene, and apply edits to the same GF view. Closing restores exact visibility without re-creation.
4. **projectM authoring mode:** retain native projectM in the existing scene. Temporarily hide unrelated render layers; show projectM authoring UI above it (preset mode, NEXT, foreground tuning). On exit restore the previous layer visibility and continue the same playhead / GL context. Do not launch separate `ProjectMActivity` for ordinary Player → projectM authoring.
5. **Themes:** browsing the list is only a menu overlay; cancelling returns to the same scene. Changing a theme may replace necessary visual components, but must not destroy projectM merely to close a menu.
6. The visibility mask is temporary **and never changes persisted layer/object choices by itself**. Real user edits still persist via existing preferences/stores; themes, Composition Sets and opaque object IDs are backwards compatible.
7. Only lifecycle events that actually end the scene owner (track unload, Activity destruction, memory pressure, genuine renderer error) may release native resources; normal background/lock should pause/resume GL according to Android lifecycle without stopping audio. Single process-wide ProjectMBridge native owner is enforced.
8. There must be a recovery fallback if the scene or preset is absent, not a silent black screen.

## Acceptance evidence (must test on physical phone)
- Navigate Player → Board → Player ×3, and Player → projectM → Player via both app and system Back ×3; **zero extra native create/destroy** for navigation. Log counters (creation ID / epoch) + first-frame times under `FARIC-nav` / `FARIC-projectM`.
- Edit Frame rotation, group GF transform, projectM Center/Edge tuning and a preset; edits visible instantly; old settings and Composition Sets still work.
- Theme list open/cancel ×3: neither audio nor scene restarts; content change may intentionally refresh just the changed visual layer.
- Audio playhead continues; if normal layer groups were hidden in authoring, they resume exactly as before.
- 3-second GPU-direct MP4 export remains valid. Do not infer full-song export fix; BUG-EXPORT-001 is independently OPEN.
- Lock/unlock, rotation and process recreation must remain safe; no duplicate native bridge or automatically started extra audio.

## Delivery / rollback
- Work on `feat/pulsedeck-persistent-scene-host`, not experimental changes directly on verified main.
- Implement in coherent steps: (A) persistent shared host and return protocol, (B) projectM authoring overlay, (C) Board live overlay, (D) Theme browser overlay, (E) regression / CI / real phone QA. Do not mark a step PASS without evidence.
- Do not delete legacy `ProjectMActivity` or alter saved object IDs during migration; route it out of normal player flow only when replacement parity is good.
- Rollback by staying on previous verified main APK; do not silently reset data.
