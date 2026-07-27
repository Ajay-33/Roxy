# ADR-0001: Native Android application

- Status: accepted
- Date: 2026-07-27

## Context

Roxy’s hardest features are Android permissions, background execution, Room, WorkManager, notification listening, usage statistics, and location. A cross-platform UI would still require substantial native code and a bridge that can fail silently.

## Decision

Build the phone app in Kotlin with Jetpack Compose. Use platform-supported Android components directly.

## Consequences

- One Android codebase and no iOS promise.
- Less bridge/debugging overhead and clearer lifecycle behavior.
- UI and collector code share Kotlin tooling but remain separate modules/packages where useful.

