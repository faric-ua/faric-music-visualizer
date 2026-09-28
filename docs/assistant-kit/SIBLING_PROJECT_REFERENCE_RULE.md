# Sibling Project Reference Rule

Read-only engineering references:
- `faric-ua/YTM`
- `faric-ua/renault-docs-android`
- `faric-ua/faric-music-graph`

## Reuse conceptually

- ACTIVE_PLAN crash recovery;
- release skeletons;
- phone QA/evidence separation;
- exact APK/source/run identity;
- lifecycle restoration;
- stable dev signing concept;
- Termux menu/install patterns;
- Git safety guards;
- public-repo CI strategy;
- lessons learned documentation.

## Never copy blindly

- package IDs;
- signing keys/secrets;
- dependency versions;
- SDK/toolchain versions;
- storage permissions;
- project-specific business logic;
- fixed paths from sibling projects.

Current platform/dependency facts must be rechecked when implementation begins.
