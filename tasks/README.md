# Task System

The task files are the operational source of truth. `PLAN.md` explains the whole journey; tasks describe only the next verifiable change.

## Folders

- `NOW.md`: ordered queue for the current phase.
- `BACKLOG.md`: future task IDs and dependencies.
- `active/`: at most one implementation task at a time.
- `done/`: completed task packets with evidence.
- `phases/`: phase goals and exit gates.
- `templates/`: reusable task and handoff formats.

## Status flow

```text
backlog -> ready -> active -> blocked or done
```

Only one task may be `active`. A task can be marked done only when all acceptance checks are true; physical phone checks may not be guessed.

## Task size

Target one focused session, usually 1–4 hours. Split tasks that:

- touch Android, API, and dashboard together;
- require more than one new permission;
- combine schema migration with unrelated UI;
- contain more than about seven acceptance checks;
- cannot be reverted independently.

## Context budget

Each task lists no more than five `Read first` files. Agents should inspect more only when a referenced symbol or failing test requires it. Handoffs capture outcomes, not a transcript of the work.

