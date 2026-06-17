# Attention Mirror — Estimate Engine Spec

This is the **single source of truth** for the attention-value math. The Python
backend (`backend/app/estimate.py`) and the Android app
(`android/.../domain/EstimateEngine.kt`) implement these exact formulas. If you
change the math, change it in both places and update this doc.

## Principles

- **Estimates, never claims.** We do not assert exact ad counts or exact rupee
  amounts. Everything is a transparent estimate built from app usage time and
  configurable per-platform assumptions.
- **Honest ranges.** Value is shown as a low–high range (from a low/high CPM),
  because real ad rates vary by region, account, and content.
- **Only monetized platforms count.** Messaging, productivity, etc. contribute
  time but never attention value.

## Inputs

Per monetized platform, a `PlatformConfig`:

| Field                   | Meaning                                  |
|-------------------------|------------------------------------------|
| `platform`              | Display name (e.g. "YouTube")            |
| `package_name`          | Android package id                       |
| `ads_per_minute`        | Estimated ad impressions per minute      |
| `low_cpm_inr`           | Low end: cost per 1000 impressions (₹)   |
| `high_cpm_inr`          | High end: cost per 1000 impressions (₹)  |

A usage record per platform: `duration_seconds` (clamped to >= 0).

## Formulas

For each platform:

```
minutes        = duration_seconds / 60
ads_seen       = round(minutes * ads_per_minute)
value_low_inr  = ads_seen * low_cpm_inr  / 1000
value_high_inr = ads_seen * high_cpm_inr / 1000
```

Daily / weekly totals are the **sum across monetized platforms**:

```
total_ads_seen        = Σ ads_seen
total_value_low_inr   = round(Σ value_low_inr)
total_value_high_inr  = round(Σ value_high_inr)
user_received_inr     = 0   # always, by design
```

`round()` is round-half-up to the nearest integer.

## Default platform assumptions

These are the seeded defaults (editable via the admin config endpoint). They are
deliberately conservative; tune in the backend, not in the app.

| Platform   | Package                          | ads/min | low CPM | high CPM |
|------------|----------------------------------|---------|---------|----------|
| YouTube    | com.google.android.youtube       | 0.20    | 250     | 800      |
| Facebook   | com.facebook.katana              | 0.35    | 200     | 600      |
| Instagram  | com.instagram.android            | 0.45    | 220     | 650      |
| X          | com.twitter.android              | 0.30    | 150     | 450      |
| Reddit     | com.reddit.frontpage             | 0.25    | 120     | 400      |
| Snapchat   | com.snapchat.android             | 0.30    | 150     | 450      |
| ShareChat  | in.mohalla.sharechat             | 0.40    | 80      | 300      |
| Moj        | in.mohalla.video                 | 0.50    | 80      | 300      |
| Josh       | com.eterno.shortvideos           | 0.50    | 80      | 300      |
| Chrome     | com.android.chrome               | 0.10    | 100     | 350      |

WhatsApp (`com.whatsapp`) is tracked for **time only** and never contributes
attention value.

## Worked example

YouTube 72m, Facebook 48m, Instagram 34m:

```
YouTube:   72 * 0.20 = 14.4 -> 14 ads  -> 14*250/1000=3.50 .. 14*800/1000=11.20
Facebook:  48 * 0.35 = 16.8 -> 17 ads  -> 17*200/1000=3.40 .. 17*600/1000=10.20
Instagram: 34 * 0.45 = 15.3 -> 15 ads  -> 15*220/1000=3.30 .. 15*650/1000=9.75

total_ads = 46
value_low  = round(10.20) = 10
value_high = round(31.15) = 31
```

The numbers in the original mockups ("₹18–₹42") were illustrative; the engine
output is whatever the seeded config produces. Tune CPMs to taste.
