# Development Guide

## Implementation order

Follow `tasks/NOW.md` and phase exit gates. Do not scaffold every framework on day one; initialize each app when its first task begins so generated versions are current and reviewable.

## Local services

Planned development topology:

```text
Android phone -> local API :4100 -> PostgreSQL :5432
Dashboard :3000 -> API :4100
```

`infra/docker-compose.yml` provides only PostgreSQL/pgvector initially. The API and dashboard run from source for quick feedback.

## Environment

Copy `.env.example` to `.env` and use development-only values. Do not add an AI key until the AI phase. Do not place a server key in Android resources or `BuildConfig`.

## Version policy

- Use supported stable releases available when a component is initialized.
- Pin direct dependency versions and commit lockfiles.
- Avoid broad automated upgrades during feature work.
- Record major framework/model decisions in `docs/decisions`.

## Quality commands

Root commands will become active as packages are initialized:

```text
pnpm check
pnpm lint
pnpm typecheck
pnpm test
```

Android checks will be defined in `apps/android/AGENTS.md` when the Gradle project is generated.

## Branch and commit guidance

- Keep changes task-sized.
- Commit generated scaffold separately from functional changes.
- Avoid commits containing both schema changes and unrelated UI work.
- Never rewrite or discard unrelated user changes.

## Dependency checklist

Before adding a library, record:

- the problem it solves;
- why platform/existing code is insufficient;
- maintenance and license status;
- Android size/battery impact or server runtime impact;
- removal/migration cost.

## Definition of done

Done means behavior, tests, manual check instructions/results, diagnostics, documentation, privacy check, and battery check are all updated. Compilation alone is not completion.

## Token-efficient agent sessions

- Give the agent a task ID, not a whole phase.
- Let it inspect the listed files rather than pasting them into chat.
- Keep task files below roughly 120 lines.
- Point to shared policies instead of repeating them in every prompt.
- Ask for a plan only when a task spans multiple modules or has an irreversible decision.
- Run focused checks first; do not repeatedly install or rebuild untouched projects.
- End each session with a compact handoff saved in the task file.

