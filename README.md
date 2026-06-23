# Attention Mirror (MonetaryAttention)

**Existing digital-wellbeing apps show how much time you lost. Attention Mirror
shows who gained from it.**

Every night it shows your **unpaid attention receipt**: the time you spent on
monetized apps, the ads you likely saw, the **estimated** value others made from
your attention — and the ₹0 that came back to you.

> No spying. No screen reading. No account. Your data stays on your phone.

Every value is a **transparent estimate** shown as a range (e.g. *₹18–₹42*),
never an exact revenue claim. See [docs/ASSUMPTIONS.md](docs/ASSUMPTIONS.md) and
[docs/ESTIMATE_SPEC.md](docs/ESTIMATE_SPEC.md).

## The privacy promise

- In the Play build we **do not** read messages, screenshots, screen content, or
  use Accessibility — only **aggregate per-app time** via `UsageStatsManager`.
- **On-device by default**: the app declares **no `INTERNET` permission** and has
  no networking code, so it cannot upload your usage.

Full details: [docs/PRIVACY.md](docs/PRIVACY.md).

## What it does (Android)

- Daily **Attention Receipt** computed on-device, with Home / Reports / Receipt
  screens: app icons, an hour-by-hour timeline, and a weekly report.
- **Daily receipt notification** at a time you choose, with dynamic,
  non-repeating copy (Gentle / Hard-truth / Quirky tones).
- **Shareable receipt card** (PNG) — the viral hook.
- Accuracy controls: **ad-free/Premium toggles** and a manual **"I saw an ad"**
  calibration tile.

## Build flavors

| Flavor | Ad detection | Where |
|---|---|---|
| `play` | estimates + Premium toggles + manual calibration | Google Play |
| `full` | adds an **opt-in** Accessibility ad-scanner (off by default) | sideload / Release only — **not Play** |

The privacy-pure `play` build is the hero product; `full` is for power users who
explicitly want real ad counts. See [docs/DISTRIBUTION.md](docs/DISTRIBUTION.md).

## Repo layout

```
android/   Kotlin + Jetpack Compose app (play & full flavors)
backend/   FastAPI: read-only config (GET /platforms) + API-key-guarded admin/ingest
ios/       Portable Swift estimate engine + plan (built after Android is live)
docs/      ESTIMATE_SPEC, PRIVACY, ASSUMPTIONS, DISTRIBUTION, PLAY_STORE_NOTES, IOS
```

## Quick start

**Backend** (runs anywhere, fully tested):

```bash
cd backend
python3 -m venv .venv && . .venv/bin/activate
pip install -r requirements.txt
pytest -q
uvicorn app.main:app --reload      # GET /platforms is public; writes need X-API-Key
```

**Android** (requires Android SDK):

```bash
cd android
./gradlew :app:assemblePlayDebug          # Play-safe build
./gradlew :app:testPlayDebugUnitTest
```

## The math

One formula, three implementations kept in sync (Python, Kotlin, Swift):

```
minutes  = duration_seconds / 60
ads_seen = round(minutes × ads_per_minute)
value    = ads_seen × CPM / 1000     # at low and high CPM → a range
```

## Status

Pre-release, heading to **v0.1-alpha** (Android, Play-safe). See
[ROADMAP.md](ROADMAP.md) and [CHANGELOG.md](CHANGELOG.md). CI builds both Android
flavors and runs backend + unit tests on every push.
