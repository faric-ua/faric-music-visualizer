# FARIC Music Visualizer — Assistant Workflow

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

## A+B — режим коротких етапів і постійного handoff (експеримент, 2026-10-09)

Користувач обрав комбінацію **A (обмежені за обсягом операції) + Б (контекст у GitHub)** як негайний робочий режим. Повний алгоритм, критерії перевірки та готова інструкція для наступного чату: [docs/workflow/ASSISTANT_AB_RESILIENCE_DRAFT.md](docs/workflow/ASSISTANT_AB_RESILIENCE_DRAFT.md). Доки користувач не підтвердив практичний результат, документ лишається DRAFT; не обіцяти технічної гарантії від зависань.

- Перед роботою читати тільки актуальний handoff + необхідні для поточного кроку файли; не перебирати всю історію GitHub і чату.
- Ділити великі задачі на невеликі незалежні етапи, кожен з окремим перевірюваним результатом; по завершенні етапу одразу повідомляти висновок.
- На тривалих кроках показувати корисний проміжний результат або змістовний статус; уникати понад приблизно 20 секунд мовчання, коли доступне оновлення.
- Не запускати безкінечні цикли опитування CI / мережі; обмежити перевірки і зафіксувати RUNNING, FAILED чи невизначений стан, якщо результату ще немає.
- Не підміняти GitHub CI PASS підтвердженням PHONE QA PASS і не починати нову APK, поки користувач перевіряє чинну, якщо немає окремого доручення.
- Записувати source SHA, build, PASS/FAIL/NOT TESTED, змінені файли, незавершені задачі і наступну точну дію в релевантні handoff-документи після фактичного завершення етапу.
- Якщо відповідь або інструмент завис: відновлюватися з останнього підтвердженого GitHub-стану, не повторювати мутації наосліп. Новий чат або перезапуск користувача може знадобитися; асистент не керує UI чи системними тайм-аутами.
- Дотримуватися наявного ASSISTANT_RESPONSE_CONTRACT.md; це доповнення не замінює формат Termux → APK-тест.

