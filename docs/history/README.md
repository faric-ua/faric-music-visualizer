# Історія — старі документи (до 18.10.2026)

Тут лежать документи, якими вели проєкт до 18.10.2026 (переважно англійською, з часів ChatGPT).
Перенесено з кореня репо `git mv` **без змін вмісту** — це історія, з неї буде урок з програмування.
Нічого тут не переписувати; нове — у `STATUS.md`, `TODO.md`, `docs/PROJECT_LEDGER.md`.

Увага: посилання всередині цих файлів написані відносно кореня репо, як було до перенесення
(напр. `docs/checkpoints/…` тепер — `../checkpoints/…`).

| Файл | Що це було | Чим замінено |
|---|---|---|
| `START_HERE_ASSISTANT.md` | Вхідна точка для асистента: порядок читання, «RESUME NOW» по версіях, постійні правила (якість графіки, стиль) | `CLAUDE.md` |
| `CURRENT_HANDOFF.md` | Передача між чатами: стан кожної версії від v0.2 до v0.19.71, що на телефоні | `STATUS.md` + `docs/PROJECT_LEDGER.md` |
| `ACTIVE_PLAN.md` | Робочий план з галочками; ~490 незакритих пунктів (більшість застарілі) — зведено в `TODO.md` 18.10 | `TODO.md` |
| `BUILD_CHECKPOINTS.md` | Запис кожної успішної збірки: SHA, номер запуску CI, артефакт | `docs/PROJECT_LEDGER.md`; записи по версіях — `docs/checkpoints/` |
| `OPEN_FINDINGS.md` | Відкриті помилки (`BUG-EXPORT-001`, `LIFE-ROT-001`, `PERF-NAV-004` …) | `TODO.md` (коди помилок збережено в дужках) |
| `PROJECT_STATUS.md` | Стан фази v0.1.0 («ще нема застосунку») | `STATUS.md` |
| `BACKLOG.md` | Ідеї (аудіо, візуал, фони, експорт, відео пачкою для YouTube) | `TODO.md` → «Ідеї на потім» |
| `VISUALIZER_TODO.md` (з `docs/visualizer/`) | Дорожня карта v0.5–v0.10 | `TODO.md` |
| `ASSISTANT_RESPONSE_CONTRACT.md` | Формат відповіді: дія першим реченням, 3–6 перевірок | `docs/RESPONSE_ALGORITHM.md` (з 16.10) |
| `ASSISTANT_RESPONSE_FORMAT.md` (з `docs/workflow/`) | Те саме, коротко | `docs/RESPONSE_ALGORITHM.md` |
| `VISUALIZER_ASSISTANT_WORKFLOW.md` | Цикл роботи ChatGPT, правила git і доказів | `CLAUDE.md` |
| `README_OLD.md` | Старий README (стан «foundation / planning») | `README.md` |
