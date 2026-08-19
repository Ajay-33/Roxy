# Now: Phase 0 — Foundation

No feature implementation should begin before the owner decisions in T-0001 are recorded.

Phase 0 is complete. The next work begins with Phase 1, T-0101.

## Ready queue

| Order | ID | Task | Depends on | Estimate |
|---:|---|---|---|---:|
| 1 | T-0001 | Record target phone, deployment, retention, and budget decisions | none | Done |
| 2 | T-0002 | Generate minimal Android project and install debug APK | T-0001 complete, `adb` available | Done |
| 3 | T-0003 | Start local pgvector database and prove migration up/down | Docker working | Done |
| 4 | T-0004 | Initialize TypeScript contracts/database/API packages with checks | T-0003 | Done |
| 5 | T-0005 | Add synthetic fixtures and repository-wide check command | T-0002, T-0004 | Done |

## T-0001 required answers

Update `docs/setup/TARGET-PHONE.md` and the decisions section of `PLAN.md`:

- phone manufacturer/model and Android/API version;
- whether Google Play services is available;
- first deployment choice: local laptop, private home host, or managed cloud later;
- notification default (recommended: metadata only);
- location default (recommended: Balanced);
- proposed raw retention periods accepted or changed;
- monthly spending ceiling;
- normal charging pattern and acceptable Trip-mode duration.

## Phase 0 exit gate

- [x] The five tasks above are done.
- [x] A fresh checkout has documented startup steps.
- [x] Minimal debug APK runs on the real target phone.
- [x] Local database health and migration rollback are verified.
- [x] No real secret or personal fixture is tracked.
