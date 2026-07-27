# Now: Phase 0 — Foundation

No feature implementation should begin before the owner decisions in T-0001 are recorded.

## Ready queue

| Order | ID | Task | Depends on | Estimate |
|---:|---|---|---|---:|
| 1 | T-0001 | Record target phone, deployment, retention, and budget decisions | none | 30–45 min |
| 2 | T-0002 | Generate minimal Android project and install debug APK | T-0001, `adb` available | 2–4 h |
| 3 | T-0003 | Start local pgvector database and prove migration up/down | Docker working | 1–2 h |
| 4 | T-0004 | Initialize TypeScript contracts/database/API packages with checks | T-0003 | 2–4 h |
| 5 | T-0005 | Add synthetic fixtures and repository-wide check command | T-0002, T-0004 | 2–3 h |

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

- The five tasks above are done.
- A fresh checkout has documented startup steps.
- Minimal debug APK runs on the real target phone.
- Local database health and migration rollback are verified.
- No real secret or personal fixture is tracked.

