# GLP-1 Tracker — notes for contributors and coding agents

Free, offline Android app to track GLP-1 treatments (Ozempic, Wegovy, Mounjaro…). No account, no ads, no network.
The owner is French-speaking (Québec): talk to them in French; UI ships in French and English.

## Build
- Built **only on GitHub Actions** (`.github/workflows/android.yml`): unit tests, release APK, and a GitHub
  Release `build-N` with `GLP-1-Tracker.apk` for every push to the default branch.
- Local: JDK 17 + Android SDK 35 → `./gradlew testDebugUnitTest assembleDebug`.
- Versions live in `gradle/libs.versions.toml`. `versionCode` = `GITHUB_RUN_NUMBER`.
- Never commit keystores or passwords; release signing comes from repo secrets (see README).

## Layout (`app/src/main/java/com/rickdnamps/glp1tracker`)
- `domain/` pure Kotlin (unit-tested): medication catalog + PK params, `Pharmacokinetics` (Bateman model),
  `Schedule` (next dose / reminder time), enums, `Format`.
- `data/` Room entities/DAOs (`AppDatabase`, bump `version` + add a migration when changing entities),
  `Settings.kt` (DataStore, `UserSettings`), `TrackerRepository`, `Backup` (JSON backup + CSV export).
- `notifications/` AlarmManager reminders, boot receiver, "Done" action that logs the dose.
- `scale/` Bluetooth LE scales: `ScaleProtocols.kt` is pure and unit-tested (standard 0x181D, QN/Renpho
  0xFFE0/0xFFF0, Renpho ES-CS20M "Lefu" 0x1A10); `BleScaleManager` does the GATT plumbing.
- `health/` Health Connect weight import (read-only).
- `ui/` Compose Material 3: one `AppViewModel` (`UiState`), screens in `ui/screens`, editors are bottom sheets
  (`Editor` sealed interface), shared widgets in `ui/components` (charts are hand-drawn on Canvas).

## Conventions
- Every user-visible string goes in **both** `res/values/strings.xml` (EN) and `res/values-fr/strings.xml` (FR).
  Escape apostrophes (`\'`).
- Weights are stored in kg; convert with `WeightUnit` only for display/input.
- Keep `domain/` and `scale/ScaleProtocols.kt` free of Android imports so they stay unit-testable.
- Medical wording: estimates only, never advice; keep the disclaimers.
- Roadmap and ideas: `docs/ROADMAP.md`.
