# Roxy Agent Instructions

These instructions apply to the entire repository. A closer `AGENTS.md` may add area-specific rules but cannot weaken privacy, battery, or safety requirements here.

## Required reading order

Read only what the task needs:

1. This file.
2. `tasks/NOW.md` and the single active task file.
3. The documents listed in that task file under `Read first`.
4. Relevant source files and tests.

Do not load `PLAN.md` unless planning or changing scope. This keeps context and token use small.

## Scope discipline

- Work on one task ID at a time.
- Do not implement future phases while touching current code.
- Do not add dependencies, permissions, collectors, cloud services, or AI calls unless the active task explicitly requires them.
- Do not perform unrelated refactors.
- If the task is too large, split it in `tasks/BACKLOG.md` before coding.
- Prefer the smallest reversible change that passes acceptance checks.

## Hard product rules

- Do not add continuous microphone, camera, call recording, keystroke capture, accessibility scraping, screen capture, or VPN interception.
- Never infer sensitive facts as truth. Store hypotheses with evidence and confidence.
- Deterministic code owns numbers, time ranges, durations, rankings, and totals. AI may explain them.
- Every AI factual claim must be traceable to stored facts or events.
- Owner-requested app labels and aggregate usage timings may appear in Roxy's local, authenticated UI. They must not appear in logs, fixtures, screenshots, commits, or prompts used for development. Credentials, tokens, connection strings, and other secrets must never be exposed outside their approved secure storage.

## Battery contract

- Balanced mode design target: no more than 8% additional battery use over 24 hours on the target phone after tuning.
- Warning threshold: more than 12% additional use.
- Hard rejection ceiling: 20–25% additional use; a change causing this must not ship as the default.
- Collectors never make network calls directly. They write locally; WorkManager batches sync.
- Prefer OS callbacks, passive signals, geofences, and aggregation over polling.
- No wake lock or permanent foreground service without an explicit task, measurement plan, and visible user benefit.
- Every collector task must specify expected wake frequency, bytes/day, rows/day, and a phone battery test.

See `docs/BATTERY.md` for the measurement protocol.

## Code standards

- Kotlin/Compose/Room/WorkManager for Android.
- TypeScript strict mode for JavaScript projects; validate boundaries with Zod.
- UTC instants plus the observed IANA timezone for time data.
- Device-generated UUIDv7 event IDs and idempotent server writes.
- Database changes require migrations and rollback notes.
- Secrets belong in Android Keystore or ignored environment files, never source code.
- Prefer standard-library or existing dependency solutions. Explain any new dependency in the task notes.

## Verification before declaring done

Run the smallest relevant checks, then broader checks if shared behavior changed. A task is done only when:

- its automated acceptance checks pass;
- its manual check is documented or performed;
- failure states are visible rather than silent;
- privacy and battery checkboxes are completed;
- changed docs match changed behavior;
- `tasks/NOW.md` is updated.

Never claim a physical-phone check passed unless it was actually performed.

## Task handoff format

Report:

1. Outcome.
2. Files changed.
3. Checks run and results.
4. Manual phone check still needed.
5. Battery/privacy impact.
6. The next task ID, without starting it.
