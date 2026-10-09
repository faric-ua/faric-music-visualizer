# PulseDeck — A+B assistant resilience protocol (DRAFT)

**Prepared:** 2026-10-09  
**Status:** TRIAL — user approved the A+B working mode, but reduction in stalls is not yet verified.  
**Scope:** assistant workflow only; no app source, signing, rendering, calibration, or release mutation.

## Purpose

Improve recoverability and reduce *avoidable* latency during GitHub-assisted PulseDeck sessions. This is a working protocol, **not a technical guarantee** against ChatGPT service stalls, connector delays, device problems, or interrupted conversations.

## A — bounded execution

1. **Resume briefly.** Read `START_HERE_ASSISTANT.md`, `CURRENT_HANDOFF.md`, `ACTIVE_PLAN.md`, `ASSISTANT_RESPONSE_CONTRACT.md`, `VISUALIZER_ASSISTANT_WORKFLOW.md`; inspect only the files necessary for the immediate change. Do not reread an entire history or scan all project sources without a concrete reason.
2. **Establish the gate.** Identify the exact source commit, branch, build, CI result, installed-phone version *if user-confirmed*, OPEN findings, deferrals, and whether phone QA blocks release or code changes.
3. **One independently useful task.** Select one clear deliverable with a limited scope (e.g. source audit, one change set, one test pass, one documentation commit). Break complicated work into small visible stages, not one monolithic action.
4. **Bound external operations.** Batch independent read-only checks when safe. Never run an open-ended CI polling loop. After limited checks, report the confirmed current status (PASS/FAIL/RUNNING/UNKNOWN) instead of assuming completion.
5. **Report real progress.** Provide useful results as soon as supported. During longer operations provide substantive user-facing updates; aim to avoid >~20 seconds without a meaningful update whenever possible. Do not send empty “working…” messages, invent completion, or promise background execution.
6. **Verify before claiming.** Build success is not real-device QA; compile success is not performance success. Mark `PASS` only for the test actually performed; distinguish `FAIL`, `NOT TESTED`, `PENDING` and `DEFERRED`.
7. **Do not collide with phone QA.** During the user's APK test, use a separate doc-only or experimental branch to prepare next steps, without merging unverified changes into the tested release. Do not rebuild or republish APK without need.

## Б — persistent, verifiable GitHub handoff

1. **Source of truth:** `START_HERE_ASSISTANT.md` (entry), `CURRENT_HANDOFF.md` (precise status), `ACTIVE_PLAN.md` (next steps), `BUILD_CHECKPOINTS.md` (verified builds), `OPEN_FINDINGS.md` (unresolved bugs), and the existing response/workflow contracts.
2. **Every verified milestone:** store version/build, full app/source SHA, relevant workflow run number + ID, artifact reference (if known), exact observed PASS/FAIL/NOT TESTED, affected paths, and one concrete resume action. Update only after evidence exists.
3. **Separate evidence:** app code audit, CI, video, log and phone QA are different sources; never transform one into another.
4. **Recheck before mutation:** inspect branch head and file blob SHA. Avoid duplicate changes on retries. Re-run required tests/CI before release, and record the actual outcome.
5. **Finish the stage:** short summary of done/pending, links to relevant GitHub paths/commits, and the required Termux + 3–6 phone-test instructions per `ASSISTANT_RESPONSE_CONTRACT.md` when applicable.

## Recovery when ChatGPT appears stuck

- If no new content appears, the user may stop and restart the response or start a new conversation; the assistant cannot control service-side timeouts or guarantee automatic continuation.
- On resume, fetch GitHub's **current** state rather than trusting a partially generated answer.
- Compare the latest actual SHA and workflow results with the last confirmed checkpoint. Never blindly repeat writes, merges, workflow runs, or APK installations.
- If interrupted in the middle of a write, verify whether the commit exists before continuing.
- A failed phone test reopens the relevant finding; do not mark the build phone-accepted.

## Trial acceptance (human-observed, not a benchmark guarantee)

- Run A+B through several real work stages, including a longer GitHub read/write workflow.
- Record whether responses delivered usable intermediate results, whether there was any nonresponsive stall, and whether a new chat can restore the correct state without user retelling.
- If the user confirms improvement, promote this draft to the stable workflow on `main` after normal review. If stalls remain, keep the observations and adjust the process, not just the prose.

## Current PulseDeck handoff at drafting time

- `v0.19.52 / build 141`, app/source `ffc0591c0099e17410f15b7113cc572fc10d660d`.
- Validate #1110 PASS, Android #628 PASS (run `37851207873`).
- `PERF-NAV-003` **OPEN / PHONE QA PENDING** for Player ↔ Library/Tracks transitions.
- Hero cleanup **DEFERRED**. Three-second MP4 export **NOT TESTED**, intentionally deferred.
- User is installing/testing signed build 141. Next source feature after its acceptance: design/implementation of Albums, Artists, Folders and user Collections, without sacrificing live projectM scene retention.

## Copy/paste starter for a new chat (after approval)

> Працюємо з PulseDeck у репозиторії `faric-ua/faric-music-visualizer`. Увімкни робочий протокол A+B з `VISUALIZER_ASSISTANT_WORKFLOW.md` і `docs/workflow/ASSISTANT_AB_RESILIENCE_DRAFT.md` (або затвердженої версії). Почни з `START_HERE_ASSISTANT.md`, `CURRENT_HANDOFF.md`, `ACTIVE_PLAN.md`, `ASSISTANT_RESPONSE_CONTRACT.md`. Перевір актуальні SHA/CI/Phone QA, не перезапускай уже завершені операції і не змінюй тестовану APK без окремого доручення. Розбий задачу на невеликі перевірювані етапи, повідомляй підтверджені проміжні результати, оновлюй GitHub handoff. Продовжуй з останньої доведеної точки, а не з припущень.
