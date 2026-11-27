# Grindstone

A small, local-only Android app for logging workout sets. Pick an exercise, enter reps and
weight, tap **Add set**. Everything is stored on the device; there are no accounts and the app
has no network access.

Built with Kotlin, Jetpack Compose, Room and Hilt.

## Build

Requires Android Studio with the Android SDK (platform 37) and a device or emulator on
Android 8.0 (API 26) or newer.

```bash
./gradlew installDebug                 # build and install on a connected device
./gradlew testDebugUnitTest lintDebug  # tests (JVM + Robolectric, no emulator) and lint
```

On Windows, use `gradlew.bat`.
