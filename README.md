# Roxy

Roxy is a private, local-first personal context engine for one Android phone. It collects explicitly enabled signals, builds a trustworthy timeline, computes deterministic daily facts, and uses AI only to retrieve and explain evidence.

This repository is currently **scaffolded for implementation**. No collectors or application features have been implemented yet.

## Start here

1. Read [AGENTS.md](AGENTS.md) for repository rules.
2. Read [PLAN.md](PLAN.md) for the full product roadmap.
3. Complete the decisions in [tasks/NOW.md](tasks/NOW.md).
4. Follow [docs/setup/WINDOWS.md](docs/setup/WINDOWS.md).
5. Implement only the active task in `tasks/active/` when that folder is created.

## Documentation map

| Question | Document |
|---|---|
| What are we building? | [docs/PRODUCT.md](docs/PRODUCT.md) |
| How does it work? | [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) |
| What should the UI feel like? | [docs/DESIGN.md](docs/DESIGN.md) |
| How do we protect battery? | [docs/BATTERY.md](docs/BATTERY.md) |
| How do we protect private data? | [docs/PRIVACY-SECURITY.md](docs/PRIVACY-SECURITY.md) |
| What data do we store? | [docs/DATA-MODEL.md](docs/DATA-MODEL.md) |
| How do we develop and test? | [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md), [docs/TESTING.md](docs/TESTING.md) |
| What is being worked on now? | [tasks/NOW.md](tasks/NOW.md) |
| What are the phases? | [tasks/phases/README.md](tasks/phases/README.md) |

## Planned layout

```text
apps/android       Native Kotlin + Jetpack Compose app
apps/api           TypeScript/Fastify API and scheduled jobs
apps/dashboard     Next.js dashboard
packages/contracts Shared TypeScript validation contracts
packages/database  Drizzle schema, migrations, and queries
packages/ai        AI provider adapter, retrieval, and prompts
infra              Local services and backup/restore tooling
docs               Product and engineering source of truth
tasks              Small, verifiable work packets
```

## Working principle

Reliability before breadth, deterministic facts before AI, and user control before automation.

