# Roxy handoff — 2026-08-22

Repository: `D:\Roxy`  
Branch: `main`  
Working tree at handoff: clean  
Latest commit: `31d9933 docs: plan notification intelligence phase`

## Purpose of this handoff

Phase 4.2 is complete and qualified. Phase 4.3 has been planned, recorded in the backlog, and made current, but no Phase 4.3 implementation has begun. Use this file to resume the Phase 4.3 work in a new chat without reopening Phase 5.

Read `AGENTS.md` first, then `tasks/NOW.md`, `tasks/active/T-0418.md`, the documents named there, and only the relevant source/tests. Work one task ID at a time. Do not start a new phase without explicit owner approval.

## What is working now

### Automatic app-usage pipeline

- After Android Usage Access and pairing, unique 30-minute WorkManager work queries Android UsageStats foreground/background transitions.
- The phone stores observations locally, produces settled 15-minute per-app duration buckets, queues only those aggregate buckets, and syncs them through constrained WorkManager batches.
- The owner app can show daily total usage and local app labels for aggregate records.

### Automatic notification pipeline

- After Android Notification Access, Android binds `RoxyNotificationListener` and delivers posted/removed callbacks even when the main Roxy activity is not visibly running.
- Metadata-only collection is on by default after access is granted. An owner can pause collection or block individual app packages locally.
- The normal path stores only package identifier, posted/removed event kind, timestamps, timezone, and an opaque SHA-256 identity digest. It does not read or upload title, body, sender, contact, image, action, intent, remote view, or raw notification extras.
- Accepted metadata is written locally first, queued in the outbox, and uploaded by WorkManager with connectivity constraints.

### Hosted sync and read model

- The Android sync worker submits at most 250 pending records per request (maximum API body 256 KiB), receives acknowledgements, marks local records acknowledged, retries transient failures exponentially, and schedules further batches when needed.
- The hosted API is authenticated with the device pairing credential. The credential and server connection details must never be exposed in source, docs, terminal output, logs, fixtures, or prompts.
- PostgreSQL stores durable events in its `events` table. The API exposes authenticated usage summaries, notification summaries/timeline, and notification analytics.
- Dashboard reads are authenticated. Phone UI resolves package identifiers to app labels only while rendering locally.

### Existing owner UI

- Home: daily usage total, leading apps, collector health.
- Alerts: notification activity timeline, total event count, arrival/clear counts, busiest hour, and top source count/label.
- Health: collection state, pending queue count, and safe failure code.
- Settings: pairing, Android usage/notification settings links, and package block control.

## What was verified on the paired phone

Do not claim broader phone verification than this record.

- Current phone had USB debugging enabled and was paired by the owner during the work.
- Safe debug checks verified app usage as `collecting_automatically`, notifications as `collecting_metadata`, and both queues empty at the time of the check.
- Default notification metadata path was verified with a synthetic callback and then the owner confirmed real WhatsApp metadata was arriving. No personal notification content was inspected.
- The hosted notification summary returned success through the paired phone.
- The hosted notification analytics endpoint returned success through the paired phone with aggregate-only counts. No package labels or notification contents were inspected during verification.
- The current debug APK was installed after the latest Android validation. Rebuild/reinstall before claiming any later code is on the phone.

Use only aggregate-safe debug provider methods when device verification is needed. Do not dump the UI hierarchy, take screenshots containing owner labels/activity, inspect notification content, retrieve pairing credentials, or read protected app data.

## Important current limitation and Phase 4.3 motivation

The existing Alerts UI proves the pipeline but is not yet a polished analytics experience.

1. It displays a basic callback/timeline list and small summary cards rather than useful grouped charts.
2. It counts notification objects, not actual chat messages. A WhatsApp bundle can represent multiple messages as one Android notification, so Roxy must never call this a message count.
3. Existing lifecycle deduplication uses a unique identity digest. Repeated updates to a single Android notification collapse, and a later removal can collide with the posted identity. T-0418 must repair the lifecycle model before richer analytics relies on it.
4. Current analytics return hourly distribution and top-app ranking, but the Android card only shows a busiest hour and top source rather than full visual analysis.

Do not solve the message-count issue by reading notification text or app-specific content. A safe Phase 4.3 improvement may distinguish notification objects, updates, arrivals, and removals, but must label them honestly as notification activity/updates rather than messages.

## Current task plan

`tasks/NOW.md` and `tasks/active/T-0418.md` currently identify T-0418 as planned. It has not been started.

### Phase 4.3 — Notification intelligence and dashboard experience

- **T-0418 — Notification lifecycle accounting.** Repair deterministic metadata-only modelling of objects, updates, arrivals, and removals. Requires Room migration/rollback notes, contract compatibility, fixtures, and paired-phone aggregate-only validation.
- **T-0419 — Notification analytics read model.** Add authenticated daily/weekly/monthly aggregates, source ranking, hourly distribution, update/clear balance, burst windows, and honest empty/completeness states.
- **T-0420 — Android notification intelligence dashboard.** Replace the raw callback list with summary cards, hourly bars/heatmap, source ranking bars, arrival/clear balance, date ranges, and app drill-down. Resolve labels locally only while rendering.
- **T-0421 — Deterministic patterns and explanation copy.** Compute transparent baseline/change observations from stored aggregates. Every statement needs its exact supporting counts/time range and must not claim message meaning, mood, productivity, or intent.
- **T-0422 — Dashboard information architecture and settings cleanup.** Keep everyday analytics separate from pairing, permissions, diagnostics, and developer tools. Remove obsolete manual operational controls from the owner flow.
- **T-0423 — Phase 4.3 end-to-end qualification.** Verify lifecycle accuracy, visual states, offline backlog recovery, hosted sync, and a battery/bytes measurement plan on the paired phone.

Only T-0418 should be active at the start. Move to the next task only after all acceptance checks, documentation, and task tracking are complete.

## Guardrails that must remain true

- Roxy is a consenting-owner product, not hidden monitoring software.
- Never add continuous microphone, camera, call recording, screen capture, VPN interception, Accessibility scraping, global tap tracking, or keystroke capture.
- Global taps/keystrokes are not safe “non-sensitive analytics”: Android does not expose them generally without invasive monitoring, and they can reveal passwords, banking flows, messages, and forms.
- Do not add notification text, sender/contact information, notification extras, app-specific message parsing, actions, images, or raw payloads to Phase 4.3.
- Owner-visible labels and aggregate app timings may appear only in the local authenticated UI. They must not appear in logs, fixtures, screenshots, commits, terminal output, or development prompts.
- Deterministic code owns counts, rankings, time ranges, durations, and totals. Any future AI explanation must cite stored evidence.
- Collectors write locally; WorkManager handles network batching. No direct collector networking, wake locks, permanent foreground services, or polling just for cosmetic freshness.
- Preserve UTC instants plus observed IANA timezone, UUIDv7 event IDs, idempotent server writes, and migration/rollback notes for database changes.

## Key files for Phase 4.3

- `AGENTS.md`
- `tasks/NOW.md`
- `tasks/active/T-0418.md`
- `tasks/BACKLOG.md`
- `docs/PRIVACY-SECURITY.md`
- `docs/BATTERY.md`
- `apps/android/app/src/main/java/com/roxy/app/notifications/RoxyNotificationListener.kt`
- `apps/android/app/src/main/java/com/roxy/app/notifications/NotificationMetadata.kt`
- `apps/android/app/src/main/java/com/roxy/app/notifications/NotificationMetadataExporter.kt`
- `apps/android/app/src/main/java/com/roxy/app/data/NotificationMetadataEntity.kt` and related Room DAO/database migration files
- `apps/android/app/src/main/java/com/roxy/app/MainActivity.kt`
- `apps/android/app/src/main/java/com/roxy/app/notifications/NotificationAnalyticsReader.kt`
- `apps/android/app/src/debug/java/com/roxy/app/debug/Phase4DebugProvider.kt`
- `apps/api/src/app.ts` and `apps/api/src/app.test.ts`
- `packages/database/src/event-store.ts`
- `packages/database/migrations/0001_events.up.sql`
- Existing notification, API, database, and Android unit tests.

## Validation commands

Run the smallest relevant tests first, then these shared checks when server or Android behavior changes:

```powershell
# From D:\Roxy
pnpm check

# From D:\Roxy\apps\android
.\gradlew.bat testDebugUnitTest assembleDebug --console=plain

# From D:\Roxy
git diff --check
```

For phone work, first confirm the device is connected with `adb devices`. The repository’s local SDK was previously available at `D:\Roxy\.android-sdk\platform-tools\adb.exe`. Rebuild and install the debug APK before testing changed Android behavior. Treat connectivity and server deployment as external state: verify aggregate-safe outcomes rather than assuming a push means a deployment has completed.

## Commit/deployment practice

- Keep commits scoped to one task and update `tasks/NOW.md`, the active/done task file, and behavior documentation before declaring it complete.
- Push verified server/API changes so the hosted deployment can update before phone-to-cloud validation.
- The recent verified commits are:
  - `7cafb08 feat: enable default notification metadata`
  - `558ed21 feat: add notification metadata analytics`
  - `dd29bb2 test: qualify notification analytics end to end`
  - `31d9933 docs: plan notification intelligence phase`
- Do not disclose credentials, URLs, pairing values, personal app labels, notification content, or actual activity data in commit messages or handoff notes.

## Suggested first action in the new chat

Confirm the owner wants to begin T-0418, then read its required files and inspect the notification event entity/DAO/migrations and existing lifecycle tests. Design and test the corrected lifecycle schema before changing the dashboard or starting T-0419.
