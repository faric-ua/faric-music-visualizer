# projectM 4.2 migration plan

FARIC currently pins **libprojectM 4.1.7**.

For offline full-song export FARIC carries one intentionally small source-level
backport: the 4.2-style external frame-time control used to render projectM
against an explicit video timeline instead of the wall clock.

Current patch:

`app/src/main/cpp/patches/projectm-4.1.7-frame-time.patch`

The patch only adds the time-control path:

- `TimeKeeper::SetFrameTime()`;
- `TimeKeeper::GetFrameTime()`;
- `ProjectM::SetFrameTime()`;
- `ProjectM::GetFrameTime()`;
- C API `projectm_set_frame_time()`;
- C API `projectm_get_last_frame_time()`.

It deliberately does **not** pull the rest of unreleased 4.2 into FARIC.

## When to migrate

Do not switch FARIC to a moving projectM branch.

Migrate only after projectM publishes an official stable tag at or above
`v4.2.0` and Android/arm64 builds successfully in FARIC CI.

At the time this plan was written, the latest stable projectM release is
`v4.1.7`.

## Migration steps

1. Create a dedicated branch from a known-good FARIC release.
2. Change the projectM FetchContent pin from `v4.1.7` to the stable 4.2 tag.
3. Remove `projectm-4.1.7-frame-time.patch` from the CMake
   `PATCH_COMMAND`.
4. Keep FARIC's JNI API unchanged where possible:
   - `ProjectMBridge.setFrameTime(seconds)`;
   - `ProjectMBridge.pushOfflinePcm(...)`;
   - `ProjectMBridge.pushOfflineSignal(...)`.
5. Replace the backported symbol with upstream
   `projectm_set_frame_time()`.
6. Confirm the upstream frame-time contract:
   - first exported frame uses 0.0 seconds;
   - each next frame uses `frameIndex / fps`;
   - negative time switches back to the system clock.
7. Verify PCM behavior with the offline timeline and check that projectM audio
   analysis receives the expected number of samples per rendered frame.
8. Re-run the complete Android workflow and signer verification.
9. Phone-test the same preset and song on both builds and compare exported
   frames at fixed timestamps.
10. Only then delete the compatibility/backport notes.

## Required regression matrix

The migration is not accepted until all of these pass:

- projectM library installation and preset discovery;
- direct preset loading;
- texture pack lookup;
- manual/auto preset lock behavior;
- foreground FARIC compositor;
- normal live Now Playing rendering;
- PNG projectM snapshot export;
- 3-second MP4 export;
- full-song offline MP4 export;
- 9:16, 16:9, 1:1 and 4:5;
- pause/resume and repeated Export Lab entry;
- no black frames after GL context recreation;
- no audio contamination from the live player during offline export;
- frame-time determinism: rendering the same song/preset twice produces the
  same timeline behavior, apart from known preset randomness.

## Rollback

The 4.1.7 implementation remains the rollback baseline until the 4.2 branch
passes phone acceptance.

Rollback means:

- restore `GIT_TAG v4.1.7`;
- restore the single frame-time patch;
- keep the FARIC Kotlin/JNI export interfaces unchanged.

This keeps the application architecture stable while projectM itself can be
upgraded independently.
