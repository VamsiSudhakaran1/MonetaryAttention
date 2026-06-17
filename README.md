# Attention Mirror (MonetaryAttention)

**See who profited from your scrolling.**

A digital well-being app that doesn't just show how much time you spent — it
shows the monetizable attention value others likely earned from it, and how much
came back to you (₹0).

> Existing digital well-being apps say *"you spent 3 hours on your phone."*
> Attention Mirror says *"you spent 3 hours on platforms that monetize you, you
> likely generated ₹X in ad value, and you received ₹0."*

Every value here is a **transparent estimate**, never a claim. We do not detect
real ads or exact rupee amounts — we estimate from app-usage time and tunable,
documented per-platform assumptions. See
[`docs/ESTIMATE_SPEC.md`](docs/ESTIMATE_SPEC.md).

## What's in this repo

| Path        | What it is                                                         |
|-------------|--------------------------------------------------------------------|
| `android/`  | Native Kotlin + Jetpack Compose MVP app (tracking, receipts, notif)|
| `backend/`  | FastAPI service: tunable platform config + receipt/summary API     |
| `docs/`     | `ESTIMATE_SPEC.md` — the canonical math, shared by both            |

The estimate math is implemented **twice** (Python in the backend, Kotlin in the
app) and kept in lockstep with the spec. Both implementations are tested and
verified to produce identical output for the same inputs.

## The MVP promise

Every night, an **Attention Receipt**:

```
ATTENTION RECEIPT — 17 June 2026

Time spent:    YouTube 1h 12m · Facebook 48m · Instagram 34m
Estimated ads seen:        46
Estimated value created:   ₹10–₹31
Amount returned to you:    ₹0

Your time created monetizable attention. You were paid with distraction.
```

## Quick start

**Backend** (runs anywhere, fully tested):

```bash
cd backend
python3 -m venv .venv && . .venv/bin/activate
pip install -r requirements.txt
pytest -q                 # 12 tests
uvicorn app.main:app --reload
```

**Android** (requires Android SDK):

```bash
cd android
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

See [`backend/README.md`](backend/README.md) and
[`android/README.md`](android/README.md) for details.

## Privacy & policy stance

The MVP tracks only **aggregate per-app foreground time** via Android's
`UsageStatsManager`. It does **not** read screen content, messages, or use
screen capture / Accessibility APIs — those are sensitive under Google Play
policy. Ad counts are honest estimates, shown as ranges.

## Status

This is the MVP scaffold: usage tracking, the estimate engine, daily receipt
notification, the three core screens, and a tunable backend. Not yet included
(deliberately): wallets, sponsors, surveys, marketplace, on-device ad detection,
shareable image cards. Those come later.
