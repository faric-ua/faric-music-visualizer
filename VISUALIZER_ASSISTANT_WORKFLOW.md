# FARIC Music Visualizer — Assistant Workflow

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
