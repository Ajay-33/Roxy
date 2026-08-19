# Windows Setup

## Detected on 2026-07-27

| Tool | Detected |
|---|---|
| Git | 2.52.0 |
| Node.js | 22.19.0 |
| pnpm | 11.9.0 |
| Java | Amazon Corretto 17.0.17 |
| Docker CLI | 28.0.4 |
| Android SDK command-line tools | 15859902 (project-local, ignored) |
| Android Platform Tools / `adb` | 37.0.1 at `.android-sdk/platform-tools/adb.exe` |
| Android SDK Platform / Build Tools | API 35 / 35.0.0 |

Docker reported that its user config file could not be read in the current restricted environment. Verify Docker Desktop normally before the database task.

## T-0002 phone connection

- The project-local Android SDK is intentionally ignored. Use `.android-sdk/platform-tools/adb.exe`, or set it on `PATH` in your own shell.
- Confirm `adb version` works.
- Enable Developer options and USB debugging on the target phone.
- Run `adb devices` and accept the phone’s authorization prompt. This was verified for iQOO I2301 on 2026-08-19.
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
