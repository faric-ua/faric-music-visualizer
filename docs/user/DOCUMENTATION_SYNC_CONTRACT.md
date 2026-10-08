# FARIC · Documentation Sync & Recovery Contract

**State (2026-10-08): PARTIALLY IMPLEMENTED.** This is an enduring handoff and an actionable implementation plan, **not** a claim that all document formats already synchronize automatically.

## User requirement

The user's desired workflow is **one Termux update (main-menu item 3) to receive ALL current project documentation**. Changes in the Android app should be reflected in reliable, version-labelled help. The docs should remain recoverable if a ChatGPT conversation ends, a new chat begins, or a device is replaced. User-facing guides and their images should stay accessible independently of ephemeral chat attachments.

## Source of truth / already implemented

- Repository `faric-ua/faric-music-visualizer`, branch `main`, commits persisted in GitHub. Main entry files `START_HERE_ASSISTANT.md`, `ACTIVE_PLAN.md`, `CURRENT_HANDOFF.md` all contain the guide and Termux documentation references. They must not be removed during closeout.
- Canonical **editable text:** `docs/user/FARIC_USER_GUIDE_UK.md`; documentation index and update rules: `docs/user/README.md`; Termux usage: `docs/user/TERMUX_DOCUMENTATION_MENU.md`.
- Main Termux option **3** uses git fetch + fast-forward merge. This synchronizes **committed tracked files** from GitHub; it does not update uncommitted assistant work, fetch files from prior chat attachments, install a new APK, or rebuild PDF/DOCX.
- Main Termux option **20 — Documentation** includes online canonical Markdown, project-doc links, import from Downloads, and offline formats. `20 → 1` can render a fallback HTML from checked-out Markdown when no imported HTML is found. **Imported pre-rendered HTML may take priority, so 20 → 1 is not yet guaranteed to show the newest Markdown revision.**
- The first designed **PDF, DOCX and screenshot-rich HTML were delivered as chat downloads, not committed to the repository**. `20 → 6` imports a downloaded guide archive from Android Downloads into `Documents/FARIC-Music-Visualizer/documentation/vX.Y.Z/`. It does not automatically download future guide releases.
- Current described app baseline: `v0.19.43 / build 132`. Android app code may advance without corresponding phone QA; the guide must clearly distinguish working, untested, planned, and deprecated behavior.

## Release documentation rule — mandatory on each user-facing change

1. Inspect the exact current source and UI, not merely previous assistant responses or a screenshot of another build.
2. Update canonical Markdown guide, screenshot/asset references, docs index and version/QA caveats **in the same release workflow** as any changed controls, pages, behavior, supported formats or export requirements.
3. Label factual assertions as **implemented/source-verified**, **CI PASS**, **phone PASS/FAIL**, or **planned** as appropriate. Never claim tests passed without evidence.
4. On build/release closeout, run validation that the guide's version is intentional and provide a short change summary to the user. If needed, regenerate PDF/DOCX/HTML and inspect layout, links and image aspect ratio.
5. Preserve older guide versions when publishing a newer one. No destructive renaming of images, irreversible cleanups, rewriting old snapshots or overwriting user edits without approval.
6. Keep the user-facing Termux menu compact: **one parent Documentation item 20** and child entries only.
7. Before switching chats, update `CURRENT_HANDOFF.md` and `ACTIVE_PLAN.md` with **the exact latest source revision, docs revision, build status, pending QA, and this contract**. A new assistant reads those files first.

## Remaining work for true one-action synchronization

### DOCSYNC-001 — Automatically publish durable documentation artifacts (NOT IMPLEMENTED)

- Create a reliable, reproducible GitHub Actions job that builds `HTML`, `PDF` and `DOCX` from canonical Markdown on source/guide releases, including properly sized screenshots/assets and font rendering. Preserve original image aspect ratios (the Shark cover in v1.0 was visibly stretched).
- Give each set a version and manifest: app source SHA, guide revision SHA, semantic version, creation date, size and SHA256 checksums; attach to a persistent GitHub Release or a repository-controlled artifacts channel, not solely ephemeral Actions artifacts or chat links.
- Implement an authenticated read-only Termux documentation sync: compare published manifest, download only changed bundles, verify SHA256, safely install into versioned documentation folders, leave previous versions intact, show current local vs remote revision and clear failure handling.
- Integrate with **main-menu item 3** only after phone-verified opt-in/design so a single update can also refresh generated documents, without requiring repeated menu navigation. In failures: retain existing local files and show useful error, never display a false PASS.
- Define freshness priority: new Markdown/rendered bundle beats stale previously imported HTML, while preserving screenshot-rich imported copies under their original version.
- Add GitHub CI regression checks: generated docs content, PDF geometry, noncropped screenshots, actual working links and Termux smoke for no connection, corrupted archive, changed version, and safe rollback.
- Add phone QA proving on-device full update with only **3**, then open HTML/PDF/DOCX under menu **20**, no manual ZIP import. Only after this test passes may status be declared **FULLY AUTOMATIC**.

### DOCSYNC-002 — Make updates harder to forget (NOT IMPLEMENTED)

- Add CI/release guard ensuring changes to user-visible app features flag docs for review or change, and ensure release notes/changelog consistently cite the corresponding guide revision.
- Document explicit exceptions for refactors not changing behavior, avoid meaningless guide churn.
- Add a manual audit checklist in `docs/user/README.md` so a new chat can resume even without connector access.

## Recovery and guarantees

**Guaranteed by git commit + GitHub availability:** already committed text, scripts and asset references can be recovered without this chat. A clone/fetch provides them even when a ChatGPT conversation is lost.

**Not guaranteed:** that an assistant keeps working or updating GitHub after the chat stops; that every local unsaved file is uploaded; that generated PDF/DOCX/HTML from chat remain accessible forever; that GitHub/network/account access never fails; or that `3` updates PDFs and DOCXs today.

**Next engineering gate:** implement DOCSYNC-001 without touching Android release binaries or disrupting existing Termux menu numbers. Until then accurately say **'git-tracked docs sync with 3; offline PDF/DOCX are separately imported'**.

**Related:** `ASSISTANT_RESPONSE_CONTRACT.md`, `docs/user/FARIC_USER_GUIDE_UK.md`, `docs/user/TERMUX_DOCUMENTATION_MENU.md`, `docs/user/README.md`.
