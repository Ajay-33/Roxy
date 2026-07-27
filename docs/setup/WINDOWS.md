# Windows Setup

## Detected on 2026-07-27

| Tool | Detected |
|---|---|
| Git | 2.52.0 |
| Node.js | 22.19.0 |
| pnpm | 11.9.0 |
| Java | Amazon Corretto 17.0.17 |
| Docker CLI | 28.0.4 |
| Android `adb` | Not found on `PATH` |

Docker reported that its user config file could not be read in the current restricted environment. Verify Docker Desktop normally before the database task.

## Before T-0002

- Install/open Android Studio with Android SDK Platform Tools.
- Confirm `adb version` works. If installed but missing from `PATH`, use Android Studio’s SDK path rather than reinstalling.
- Enable Developer options and USB debugging on the target phone.
- Run `adb devices` and accept the phone’s authorization prompt.
- Record phone details in `docs/setup/TARGET-PHONE.md`.

## Before T-0003

- Start Docker Desktop.
- Copy `.env.example` to `.env`.
- Start PostgreSQL with the Compose file under `infra`.
- Verify database health before applying any migration.

## Target phone record

Copy this into `docs/setup/TARGET-PHONE.md` when the device is available:

```text
Manufacturer/model:
Android version/API:
Security patch:
Google Play services available:
Battery capacity/health if known:
OEM battery-management settings:
Baseline test dates:
Notes:
```

