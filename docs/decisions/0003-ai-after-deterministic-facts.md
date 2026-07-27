# ADR-0003: AI follows deterministic facts

- Status: accepted
- Date: 2026-07-27

## Context

Language models are poor sources of exact totals and can create confident stories from incomplete observations.

## Decision

Code and SQL compute times, counts, rankings, visits, and completeness. AI is added only after those facts work; it retrieves and explains evidence with uncertainty.

## Consequences

- The MVP is useful before AI integration.
- Chat needs an intent/query layer, not only vector search.
- Factual regression tests can use exact expected answers.

