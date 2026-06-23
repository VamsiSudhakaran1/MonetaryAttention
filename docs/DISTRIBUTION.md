# Distribution & install guide

Attention Mirror ships in **two build flavors**:

| Flavor | App ID | Ad scanner | Where it goes |
|--------|--------|-----------|---------------|
| `play` | `com.attentionmirror` | ❌ estimates + Premium toggles + "I saw an ad" calibration | **Google Play** (testing track → production) |
| `full` | `com.attentionmirror.full` | ✅ opt-in Accessibility ad-scanner | **Signed APK / GitHub Release only** |

> ⚠️ The `full` flavor is **not** for Google Play. Using an `AccessibilityService`
> for analytics (counting "Sponsored" labels) violates Play's Accessibility
> policy and would get the app removed. Keep it to direct/Release distribution.

Build variants:

```bash
cd android
./gradlew :app:assemblePlayDebug    # Play-safe debug
./gradlew :app:assembleFullDebug    # sideload debug, with scanner
./gradlew :app:assemblePlayRelease  # Play upload (needs signing, below)
./gradlew :app:assembleFullRelease  # signed sideload build
```

CI builds both debug flavors on every push and uploads them as the
`attention-mirror-debug` artifact.

## 1. Why "unknown app won't install / access denied"

A debug APK is signed with a throwaway key, which Play Protect and OEM skins
(MIUI/Xiaomi, Samsung, Oppo/Vivo) treat as risky. Per-device, the user must:

- Enable **Install unknown apps** for the installer (Files/Chrome).
- MIUI: disable **Scan apps before installing**, enable **Install via USB**.
- Grant **Usage access** (Settings → Apps → Special access → Usage access).

**The real fix is publishing on Play** — a signed, Play-distributed build clears
Play Protect and removes almost all of this friction. Sideloading the `full`
build will always have some friction; that's the trade-off for the scanner.

## 2. Release signing

1. Create a keystore (once, keep it safe — losing it means you can't update the
   Play listing):

   ```bash
   keytool -genkey -v -keystore attention-mirror.jks \
     -keyalg RSA -keysize 2048 -validity 10000 -alias upload
   ```

2. Create `android/keystore.properties` (gitignored):

   ```properties
   storeFile=/absolute/path/to/attention-mirror.jks
   storePassword=********
   keyAlias=upload
   keyPassword=********
   ```

3. `./gradlew :app:assemblePlayRelease` (or `bundlePlayRelease` for an `.aab`).
   When `keystore.properties` is absent (e.g. CI), release builds stay unsigned.

For Play, prefer **Play App Signing**: upload an `.aab` signed with your upload
key; Google manages the app signing key.

## 3. Google Play testing track (fixes the install problem)

1. Create the app in the [Play Console](https://play.google.com/console).
2. Upload `app-play-release.aab` to **Testing → Internal testing**.
3. Add your parents'/testers' emails to the testers list, share the opt-in link.
4. They install from the Play Store link — no "unknown app", no Play Protect
   warning, no per-OEM toggles.
5. Promote Internal → Closed → Production when ready.

Play data-safety/permissions notes for the `play` flavor:
- `PACKAGE_USAGE_STATS` requires a Permissions Declaration — justify it as core
  digital-wellbeing functionality (aggregate time only, no content).
- The `play` flavor contains **no** AccessibilityService, so there's nothing to
  declare there.

## 4. The `full` flavor's ad scanner (external download)

The `full` build is distributed **outside Google Play** (signed APK / GitHub
Release) for users who opt into richer, more accurate ad reporting.

The opt-in `AdScannerService` watches the tracked apps for on-screen ad markers
("Sponsored", "Promoted", "Paid partnership") and records, **per ad**:

- **which** ad/app and the matched marker keyword,
- **how long** it was on screen (appearance → disappearance), and
- **how frequently** ads appear (derived: ads/min vs tracked time).

This shows up in an **"Ads detected"** section (count · on-screen seconds · avg
duration · ads/min) and also feeds the same calibration the manual "I saw an ad"
tile uses, so estimates become real per-user ad rates (e.g. Reels' high load).

- Off by default; the user enables it in **Settings → Accessibility**.
- Restricted to tracked packages (`res/xml/accessibility_config.xml`).
- Stores only the app, marker keyword, and start/end times — never other screen
  content. Everything on-device. See [PRIVACY.md](PRIVACY.md).
