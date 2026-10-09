# FARIC Music Visualizer — Assistant Workflow

## USER FIRST — immediate Termux/APK decision (2026-10-09)

At the START of every PulseDeck release, build or phone-QA answer state one concrete instruction in Ukrainian: **download/install/test now — YES/NO; exact Termux menu numbers or “do nothing”; exact build/version**. This must precede all status updates, technical explanation and changelog. The LAST two blocks remain **Termux — що натиснути** and **Перевірка APK — що перевірити**. Do not offer tests for a build unavailable in the normal Termux release path. Read [ASSISTANT_RESPONSE_CONTRACT.md](ASSISTANT_RESPONSE_CONTRACT.md) every session.


## PERMANENT RESPONSE FORMAT — READ FIRST

Before replying about FARIC Music Visualizer releases, builds, Termux or phone QA, read [`ASSISTANT_RESPONSE_CONTRACT.md`](ASSISTANT_RESPONSE_CONTRACT.md). Finish relevant answers with **(1) exact Termux menu steps** and **(2) 3–6 short APK phone tests (action → expected result)**. This is a repository-wide user requirement and survives new chats; never substitute a long changelog for the phone-test checklist.

## Default loop

`idea/reference → behavior contract → ACTIVE_PLAN → release skeleton → implementation → static/unit tests → CI → APK → phone QA → evidence → closeout`

## Roles

ChatGPT:
- prepares code, docs, tests, diagrams, release notes and exact Termux blocks;
- inspects reference media supplied by the user;
- keeps the repository recoverable by a new session.

User:
- decides visual direction;
- installs/test APKs on a real phone;
- supplies screen recordings/screenshots when visual timing or performance needs proof.

## Git safety

Before commit:
- `git diff --check`;
- inspect `git status --short`;
- stage exact intended paths;
- inspect staged files/stat;
- stop on unexpected deletion or unrelated changes;
- require clean tree after push.

## Evidence boundaries

- Source review is not render PASS.
- Unit tests are not beat-sync PASS.
- CI build is not APK phone PASS.
- Emulator behavior is not physical-device performance PASS.
- One song PASS is not general beat-detection PASS.

## Documentation gate

For major behavior:
1. update product/behavior contract;
2. update `ACTIVE_PLAN.md`;
3. create/update release skeleton;
4. update diagram;
5. then implement.

## Active plan rule

`ACTIVE_PLAN.md` is the mutable execution checklist.
The first unchecked item is the default resume point.
Only mark `[x]` when current evidence proves it.

## Successful build checkpoint rule

Every successful Android build / Android GitHub Actions PASS must be recorded immediately in `BUILD_CHECKPOINTS.md` before starting the next implementation step.

Each checkpoint must include:
- app version + versionCode/build number;
- exact app/source commit SHA;
- Android workflow run number and run id;
- artifact name/id when available;
- the exact behavior/features successfully completed in that build;
- what remains unverified, especially physical-phone QA;
- one explicit next resume step.

If the resume point changes, update `CURRENT_HANDOFF.md` and `ACTIVE_PLAN.md` in the same work cycle.
Never rely on chat memory alone for a known-good build point.

## Android lifecycle

Rotation/recreation must restore:
- selected track;
- playback position when technically safe;
- active scene/preset;
- user-selected background;
- visualizer settings.

It must never start duplicate playback, duplicate analysis engines or duplicate render loops.

## Signing

This project gets its own signer.
Never copy YTM, Renault or Music Graph signing material.
