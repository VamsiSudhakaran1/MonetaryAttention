# Attention Mirror — Android app

Native Kotlin + Jetpack Compose app. Every night it shows your **unpaid
attention receipt**: time spent on monetized platforms, estimated ads seen,
estimated value created — and the ₹0 returned to you.

> **Privacy by design.** The app reads only aggregate per-app foreground *time*
> via Android's `UsageStatsManager`. It never reads your screen, messages, or
> content. WhatsApp is tracked for time only and never assigned value.

## Build

Requires the Android SDK (API 34) and JDK 17+. From `android/`:

```bash
./gradlew :app:assembleDebug      # build the APK
./gradlew :app:testDebugUnitTest  # run the estimate-engine unit tests
```

> This module is not buildable in environments without the Android SDK. The
> pure-Kotlin estimate engine (`domain/`) is plain JVM code and is verified to
> produce identical output to the Python backend (see `docs/ESTIMATE_SPEC.md`).

A Gradle wrapper properties file is included; generate the wrapper jar/scripts
with a local Gradle (`gradle wrapper`) if they aren't present.

## How it works

1. **Track** — `UsageStatsCollector` reads per-app foreground seconds for the
   day (after the user grants Usage Access in Settings).
2. **Store** — `AttentionRepository` persists one row per (app, day) in Room.
3. **Estimate** — `EstimateEngine` applies `duration × ads/min × CPM range`
   per platform and aggregates an honest `AttentionReceipt`.
4. **Notify** — `DailyReceiptWorker` (WorkManager, default 21:30) posts the
   end-of-day receipt notification.
5. **Show** — Compose screens: Home, Receipt, Week (with platform comparison
   and a clearly-labelled yearly projection).

## Source layout

```
app/src/main/java/com/attentionmirror/
  AttentionMirrorApp.kt      Application: channel + schedule daily worker
  MainActivity.kt            Compose host; re-checks permission on resume
  domain/                    Pure JVM: PlatformConfig, EstimateEngine, Formatting
  data/                      Room: UsageRecord, UsageDao, AppDatabase, Repository
  tracking/                  UsageStatsCollector (UsageStatsManager)
  notification/              DailyReceiptWorker, Scheduler, BootReceiver
  ui/                        Theme, ViewModel, Components, Screens, AttentionApp
app/src/test/java/...        EstimateEngineTest (mirrors backend tests)
```

## Permissions

- `PACKAGE_USAGE_STATS` — special access; the app sends users to
  *Settings → Usage access*. Cannot be granted at runtime.
- `POST_NOTIFICATIONS` — for the daily receipt (Android 13+).
- `RECEIVE_BOOT_COMPLETED` — to re-arm the daily notification after reboot.

This MVP deliberately does **not** use screen capture (`MediaProjection`) or
the Accessibility API. Those are sensitive under Google Play policy; ad counts
here are transparent *estimates*, not detections.
