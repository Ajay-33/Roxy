# Now: Phase 1 — Durable Sync

Phase 1 is complete and verified end to end. The next phase is not started until this phase is committed.

## Phase 1 queue

| Order | ID | Task | Depends on | Estimate |
|---:|---|---|---|---:|
| 1 | T-0101 | Define event envelope v1 in Zod and synthetic fixtures | Phase 0 complete | Done |
| 2 | T-0102 | Add Android Room event/pending-sync entities | T-0101 | Done |
| 3 | T-0103 | Persist a manual test event locally | T-0102 | Done |
| 4 | T-0104 | Add authenticated batch-ingestion endpoint | T-0101, T-0003 | Done |
| 5 | T-0105 | Add idempotent event insert and per-ID acknowledgment | T-0104 | Done |
| 6 | T-0106 | Add WorkManager sync with network constraint and backoff | T-0103, T-0105 | Done |
| 7 | T-0107 | Add queue diagnostics and offline/retry acceptance test | T-0106 | Done |

## Phase 1 exit gate

- Versioned contract shared through fixtures.
- Device authentication and safe logging work.
- Queue diagnostics show oldest age, counts, last success, and safe error.
- Backoff creates no rapid wake/network loop.
