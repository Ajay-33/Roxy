# Roxy handoff — Phase 5 start

Repository: `D:\Roxy`
Branch: `main`
Working tree at handoff: clean after the Phase 4.3 completion commit

## Purpose

Phase 4.3 is complete. It repaired notification lifecycle accounting, added deterministic day/week/month analytics and owner-facing dashboard improvements, and qualified the paired phone using aggregate-only diagnostics. The multi-day notification battery/bytes measurement is explicitly deferred; it is not a passed result.

Start with `AGENTS.md`, then `tasks/NOW.md`, `tasks/active/T-0501.md`, and the documents named under that task's **Read first** section. Work one task ID at a time. Do not implement future Phase 5 tasks while T-0501 is active.

## What Phase 4.3 delivered

- Metadata-only notification lifecycle records distinguish arrivals, updates, and removals deterministically, including Room migration notes and duplicate protection.
- Authenticated notification analytics provide honest completeness, day/week/month aggregation, hourly distributions, source ranking, bursts, and limited historical comparisons.
- The Android Alerts view has period controls, interactive hourly source drill-down, bounded ranking lists, evidence-backed pattern copy, and a locally resolved label fallback for package identifiers.
- Home and Activity retain their useful metrics and now use readable hour/minute durations and robust responsive card layouts.
- The paired phone validated lifecycle transitions, aggregate analytics for all periods, and recovery of a metadata-only sync backlog after a brief network interruption. No notification content or owner labels were inspected in diagnostics.

## Important deferred work

The three-day baseline plus three-day enabled notification battery/bytes protocol in `docs/BATTERY.md` is deferred by the owner. Treat it as unmeasured, not successful. Restart from day 1 if a later release decision needs that evidence.

## Phase 5 task plan

- **T-0501 — Location permission sequence and Balanced/Trip controls** (active): explicit owner enablement and permission/revocation UI; no default collection.
- **T-0502 — Passive/batched samples with accuracy metadata.**
- **T-0503 — Time-bounded Trip foreground service.**
- **T-0504 — Synthetic visit detection and impossible-jump filtering.**
- **T-0505 — Candidate place review and user labels.**
- **T-0506 — Location compaction/retention job.**
- **T-0507 — Three-day battery and route validation.**

## Non-negotiable guardrails

- Roxy is a consenting-owner product; never add hidden monitoring.
- Do not add microphone, camera, call recording, screen capture, VPN interception, Accessibility scraping, global tap tracking, or keystroke capture.
- Phase 5 must use explicit owner enablement, passive/batched signals where possible, local-first writes, and WorkManager-batched network sync.
- No wake lock, permanent foreground service, polling loop, or direct collector networking without the specific task's accepted design and battery measurement plan.
- Store UTC instants with observed IANA timezone; use device UUIDv7 event IDs and idempotent server writes.
- Do not expose owner-visible labels, usage timing, location, pairing values, notification contents, or credentials in logs, fixtures, screenshots, commit messages, or handoff notes.
- Deterministic code owns coordinates, ranges, durations, rankings, visit candidates, and totals. Any future AI explanation must be evidence-backed and must not turn a hypothesis into fact.

## Key files for T-0501

- `AGENTS.md`
- `tasks/NOW.md`
- `tasks/active/T-0501.md`
- `docs/PRODUCT.md`
- `docs/PRIVACY-SECURITY.md`
- `docs/BATTERY.md`
- `docs/ARCHITECTURE.md`
- `apps/android/app/src/main/java/com/roxy/app/MainActivity.kt`
- Android manifest, settings/health UI, and existing WorkManager configuration

## Verification baseline

```powershell
# D:\Roxy
pnpm check

# D:\Roxy\apps\android
.\gradlew.bat testDebugUnitTest assembleDebug --console=plain

# D:\Roxy
git diff --check
```

For physical-phone work, use only aggregate-safe diagnostics. Never dump UI hierarchy or inspect protected app data, notification contents, owner labels, pairing credentials, or real location records.

## Suggested first action

Read T-0501's required documents and inspect the current manifest and owner settings/health UI. Decide the minimal permission-and-control model before adding any location collector code.
