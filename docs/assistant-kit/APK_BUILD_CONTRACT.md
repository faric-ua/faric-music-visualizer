# APK Build Contract

## Identity

This project must use:
- its own Android package ID;
- its own development signer;
- its own release signer.

Never copy signer material from sibling projects.

## State tracking

Track separately:
- repository source SHA;
- CI run ID;
- signed APK source;
- phone-installed/tested version.

A successful CI build is not phone PASS.

## Planned CI

When Android scaffold exists:
1. unit/static tests;
2. exact ref checkout;
3. Android build;
4. signer verification;
5. zipalign/apksigner verification;
6. SHA-256;
7. short-lived artifact upload.

Because the repository is public, standard public GitHub-hosted Actions are preferred for CI.
