# Privacy

Attention Mirror is built privacy-first. In plain words:

- **We do not read your messages.**
- **We do not record or screenshot your screen.**
- **We do not read the content inside any app.**
- **Your usage data stays on your device by default.**
- **No account. No login. No sign-up.**
- **All rupee values are estimates — never actual revenue claims.**

## How it works

The app reads only **aggregate per-app foreground time** via Android's
`UsageStatsManager` (the same data the system Digital Wellbeing screen uses). It
turns that time into an **estimated** "attention receipt" entirely **on your
phone**.

Provable, not just promised:
- The app declares **no `INTERNET` permission** and contains **no networking
  code** — it literally cannot upload your usage anywhere.
- Receipts are computed locally from a bundled assumptions table
  (see [ASSUMPTIONS.md](ASSUMPTIONS.md)).

## Permissions we request

| Permission | Why |
|---|---|
| `PACKAGE_USAGE_STATS` (Usage access) | Read aggregate per-app time. No content. |
| `POST_NOTIFICATIONS` | Show the daily receipt notification (Android 13+). |
| `RECEIVE_BOOT_COMPLETED` | Re-arm the daily notification after a reboot. |

## The optional `full` build (not on Google Play)

A separate **sideload-only** build (`full` flavor) offers an **opt-in**
Accessibility-based ad detector that counts on-screen "Sponsored" labels in
supported apps to improve estimate accuracy. It is:

- **off by default** and must be explicitly enabled in system Accessibility settings,
- restricted to the apps already tracked,
- reads only whether an ad marker is present, and stores **only**: the app, the
  matched marker keyword (e.g. "sponsored"), and each ad's on-screen start/end
  times (to report count, how long it showed, and frequency) — never any other
  screen content, on-device,
- **never shipped to Google Play** (Play distributes the privacy-pure build).

If you use the Play build, none of this applies — there is no Accessibility
service in it at all.

## Data we collect

**None off-device, by default.** No analytics, no crash SDK, no ad SDK, no
third-party trackers.
