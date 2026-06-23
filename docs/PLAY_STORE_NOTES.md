# Google Play notes

Only the **`play`** flavor (`com.attentionmirror`) goes to Google Play. The
`full` flavor (Accessibility scanner) is sideload/Release-only and must never be
uploaded — see [DISTRIBUTION.md](DISTRIBUTION.md).

## Target API
- `compileSdk = 35`, `targetSdk = 35` (Play requires API 35+ for new apps and
  updates).

## Permissions declaration
- **`PACKAGE_USAGE_STATS`** (special access) needs a Permissions Declaration in
  the Play Console. Justify it as **core digital-wellbeing functionality**:
  the app reads only aggregate per-app foreground time to estimate attention
  value; it never reads screen content or messages.
- `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` — standard.
- The `play` flavor contains **no AccessibilityService**, so there is nothing
  accessibility-related to declare.

## Data safety form
- **No data collected or shared.** The app has no `INTERNET` permission and no
  networking code; all processing is on-device.
- No analytics/ads/crash SDKs.

## Store listing essentials (still TODO)
- Privacy policy URL (publish [PRIVACY.md](PRIVACY.md)).
- Screenshots (Home, Receipt, Reports, share card).
- Short + full description leading with: *"Existing wellbeing apps show how much
  time you lost. Attention Mirror shows who gained from it."*
- Content rating questionnaire, data safety form, target audience.

## Release
- `./gradlew :app:bundlePlayRelease` (signed via `keystore.properties`) → upload
  the `.aab` to **Internal testing**, then promote.
