# Contributing to Roxy

Roxy is currently a single-owner project. Changes still follow a lightweight review process because the data is sensitive and Android background behavior is easy to break.

## Workflow

1. Select the first unblocked task from `tasks/NOW.md`.
2. Copy `tasks/templates/TASK.md` into `tasks/active/<task-id>.md`.
3. Fill scope, read-first files, acceptance checks, battery estimate, and privacy impact.
4. Make the smallest implementation that satisfies the task.
5. Run relevant checks and record results in the task file.
6. Move the task file to `tasks/done/` only after all exit checks are true.
7. Update `tasks/NOW.md` and any behavior documentation.

## Commit convention

Use short conventional subjects:

```text
docs: define battery measurement protocol
feat(android): persist test events locally
fix(api): make event ingestion idempotent
test(database): cover timezone boundary aggregation
```

Never include personal payloads, secrets, generated databases, exports, or backups.

