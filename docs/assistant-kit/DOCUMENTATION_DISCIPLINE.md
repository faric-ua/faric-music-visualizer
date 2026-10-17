# Documentation Discipline

> **18.10.2026 — замінено.** `ACTIVE_PLAN.md` і `CURRENT_HANDOFF.md` більше не ведуться (лежать у `docs/history/`).
> Стан — `STATUS.md`, задачі — `TODO.md`, журнал — `docs/PROJECT_LEDGER.md`, правила — `CLAUDE.md`. Нижче — історія.

Documentation is implementation state.

Before major code:
1. update behavior/product contract;
2. update ACTIVE_PLAN;
3. create/update release skeleton;
4. update diagram;
5. implement.

After verified progress:
- mark exact ACTIVE_PLAN item;
- update QA/evidence;
- update CURRENT_HANDOFF if resume point changed.

Never rewrite old PASS/FAIL evidence to make history cleaner.
