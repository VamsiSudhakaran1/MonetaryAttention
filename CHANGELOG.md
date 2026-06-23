# Changelog

All notable changes to this project are documented here. Dates are ISO (YYYY-MM-DD).

## [Unreleased]
### Added
- Configurable daily-notification time and a Quirky message tone (Gentle / Hard
  truth / Quirky), in addition to dynamic, day-specific messages.
- Ad-free (Premium) per-app toggles — time shown, value set to ₹0.
- Android `play` / `full` build flavors; `full` adds an opt-in Accessibility
  ad-scanner (sideload-only, never on Play).
- Backend API-key guard (deny-by-default) on write/ingest endpoints.
- Docs: PRIVACY, ASSUMPTIONS, PLAY_STORE_NOTES, DISTRIBUTION, IOS; ROADMAP;
  this changelog.
- iOS plan + portable Swift estimate engine.
- Release workflow (signed Play `.aab` + `full` APK on tag).

### Changed
- `compileSdk`/`targetSdk` → 35 (AGP 8.6.0) for Play readiness.
- Modern Manrope type system + flatter, de-boxed UI; premium dark theme.
- App icons, hour-by-hour timeline, weekly reports, paper-style receipt.

### Security
- `PUT`/`DELETE /platforms` and `POST /usage` now require `X-API-Key`
  (`ADMIN_API_KEY`); read-only `GET /platforms` stays public.

## [0.0.1] - 2026-06-17
- Initial scaffold: FastAPI estimate backend + Kotlin/Compose Android MVP.
