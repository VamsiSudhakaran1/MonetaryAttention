# Roadmap

## v0.1-alpha (next release) — Android, Play-safe wedge
- `play` flavor only on Google Play (Internal testing).
- `UsageStatsManager` tracking, on-device only (no upload).
- Daily attention-receipt notification at a user-chosen time.
- Manual "I saw an ad" calibration.
- Shareable receipt card.
- "How was this calculated?" transparency view.
- Privacy policy + assumptions published.
- Signed APK / Play internal testing link + screenshots.

**Goal:** 100 people install it and share their first receipt.

**Not in v0.1:** wallet, sponsors, surveys, marketplace, accounts, cloud sync.

## Later
- Senior mode (large type, simpler words, gentle wording, optional voice readout).
- Regional language support.
- Tighter per-platform assumptions with confidence + sourcing.
- `full` (sideload) build: opt-in Accessibility ad scanner for real ad counts.
- iOS companion (Screen Time / FamilyControls) — see [docs/IOS.md](docs/IOS.md).

## Distribution
- Play **Internal → Closed → Production** for the `play` flavor.
- GitHub Releases for the signed `full` (sideload) build.
