# Roxy Android

Native Kotlin + Jetpack Compose app. Phase 0 includes a deliberately static, zero-permission diagnostic shell; it does not collect, store, or upload data.

## Build

With the project-local SDK installed, run `gradlew.bat assembleDebug` from this folder. The resulting APK is `app/build/outputs/apk/debug/app-debug.apk`.

Planned responsibilities: permissions, collectors, Room queue, WorkManager sync, diagnostics, timeline, chat, and controls.
