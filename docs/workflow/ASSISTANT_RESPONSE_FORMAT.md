# Assistant response handoff format

This is a standing communication rule for the FARIC Music Visualizer / PulseDeck project.

At the end of every meaningful implementation/build/testing handoff, always include a short, visually obvious action block for the user.

Preferred format:

```text
TERMUX:
3 → 10 → PASS → 8 → встановити APK

ТЕСТ:
1. Коротка перевірка №1.
2. Коротка перевірка №2.
3. Коротка перевірка №3.
```

If a manual Android build must be triggered first:

```text
TERMUX:
3 → 9 → 10 → PASS → 8 → встановити APK
```

If build status is FAIL:

```text
STOP:
APK не ставити.
Скинь результат пункту 10.
```

Current Termux menu meanings:
- 3 — Оновити репозиторій з GitHub
- 8 — Завантажити APK і відкрити папку
- 9 — Запустити Android build
- 10 — Статус Android build

Rules:
- Keep this block short and easy to scan.
- Do not bury the user's next action inside prose.
- Explain implementation details first if useful, but always finish with the concrete TERMUX + TEST block when the user is expected to do something.
- TEST should contain only the checks relevant to the current change/release.
- If the user does not need to do anything yet, say that explicitly instead of inventing steps.
