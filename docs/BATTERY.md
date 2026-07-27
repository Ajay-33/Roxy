# Battery Budget and Measurement

Battery is a release constraint, not a later optimization.

## Budget

The user-provided maximum is 20–25% additional battery consumption per day. Roxy uses stricter operating bands:

| Additional 24-hour drain | Meaning | Action |
|---:|---|---|
| 0–5% | Excellent | Keep monitoring |
| >5–8% | Target range | Acceptable for Balanced mode |
| >8–12% | Warning | Investigate and tune before expansion |
| >12–20% | Unacceptable default | Block release; reduce work/rate |
| >20–25% | Hard ceiling breach | Disable/revert offending behavior |

Trip mode may temporarily exceed the normal target because it is explicitly started, visible, and time-bounded. It must stop automatically at the configured limit.

## Energy design rules

- Prefer OS callbacks over polling.
- Aggregate on-device before upload.
- Use WorkManager constraints and batching; never maintain a heartbeat.
- Do not wake the device just to meet a cosmetic freshness target.
- Use passive/batched location and geofences in Balanced mode.
- Query usage stats in bounded overlapping windows, not every few minutes.
- Avoid high-frequency database writes; write transactions in batches where safe.
- No perpetual `dataSync` foreground service.
- Retry with exponential backoff and stop retrying permanent errors.
- AI processing runs on the server, not continuously on the phone.

## Initial operating targets

| Activity | Starting target |
|---|---|
| Normal sync | every 30–60 minutes when network is available |
| Usage query | every 30–60 minutes; reconcile a bounded overlap |
| Balanced location | passive/batched/significant movement; no fixed 10-minute GPS promise |
| Trip mode | user-started, persistent notification, auto-stop |
| Collector writes | normalized transitions/buckets, not repeated identical snapshots |
| Upload | <=256 KiB or 250 events per batch |

These are hypotheses until measured on the target phone.

## Measurement protocol

Battery percentage alone is noisy. Use paired real-use days where possible.

### Establish baseline

1. Use the target phone normally for three comparable 24-hour periods with Roxy disabled.
2. Record start/end battery, charging periods, screen-on time, travel, and battery-saver state.
3. Capture Android battery-usage screens and compute the median daily drain.

### Measure Roxy

1. Repeat for three comparable days with one new collector enabled.
2. Keep sync mode and travel pattern documented.
3. Record Roxy’s reported app usage plus total drain.
4. Compare medians and report a range, not false precision.

### Diagnose

Use Android Studio Energy Profiler and `dumpsys batterystats` during dedicated test windows. Look for excessive wakeups, location duration, jobs, network transfers, and foreground-service time.

## Required battery note per collector task

```text
Expected callbacks/queries per day:
Expected local rows per day:
Expected uploaded bytes per day:
Uses location / foreground service / wake lock:
Baseline test dates:
Enabled test dates:
Observed additional drain:
Decision and tuning:
```

Do not claim battery success from an emulator or a few minutes of profiling.

