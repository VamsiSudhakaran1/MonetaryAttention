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
| YouTube    | com.google.android.youtube       | 0.20    | 310     | 980      |
| Facebook   | com.facebook.katana              | 0.35    | 250     | 730      |
| Instagram  | com.instagram.android            | 0.45    | 270     | 800      |
| X          | com.twitter.android              | 0.30    | 185     | 550      |
| Reddit     | com.reddit.frontpage             | 0.25    | 150     | 490      |
| Snapchat   | com.snapchat.android             | 0.30    | 185     | 550      |
| ShareChat  | in.mohalla.sharechat             | 0.40    | 100     | 370      |
| Moj        | in.mohalla.video                 | 0.50    | 100     | 370      |
| Josh       | com.eterno.shortvideos           | 0.50    | 100     | 370      |
| Chrome     | com.android.chrome               | 0.10    | 125     | 430      |

WhatsApp (`com.whatsapp`) is tracked for **time only** and never contributes
attention value.

## Worked example

YouTube 72m, Facebook 48m, Instagram 34m:

```
YouTube:   72 * 0.20 = 14.4 -> 14 ads  -> 14*310/1000=4.34 .. 14*980/1000=13.72
Facebook:  48 * 0.35 = 16.8 -> 17 ads  -> 17*250/1000=4.25 .. 17*730/1000=12.41
Instagram: 34 * 0.45 = 15.3 -> 15 ads  -> 15*270/1000=4.05 .. 15*800/1000=12.00

total_ads = 46
value_low  = round(12.64) = 13
value_high = round(38.13) = 38
```

The numbers in the original mockups ("₹18–₹42") were illustrative; the engine
output is whatever the seeded config produces. Tune CPMs to taste.

## Calibration (user-assisted ad counting)

Ad load varies wildly by account and content, so the user can mark ads they
actually see ("I saw an ad"). We turn their marks into a **personal ads/minute**
rate that overrides the platform default — without ever reading screen content.

```
if observed_minutes >= MIN_CALIBRATION_MINUTES (15):
    effective_ads_per_minute = observed_ads / observed_minutes
else:
    effective_ads_per_minute = config.ads_per_minute   # not enough sample yet
```

A sufficiently-sampled `0` is honoured (if you genuinely saw no ads, your rate is
0). Worked example from the brief: 7 ads marked over 20 minutes → `7 / 20 = 0.35`
ads/min. Everything downstream (`ads_seen`, value range) uses this rate.

- Python: `effective_ads_per_minute(...)`, plus the `personal_ads_per_minute` /
  `personal_rates_by_package` overrides on `estimate_platform` / `build_receipt`.
- Kotlin: `Calibration` + the `personalRatesByPackage` override on
  `EstimateEngine.buildReceipt`. Marks are stored locally in Room (`ad_marks`)
  and never leave the device.

## Tone (hard-truth mode)

The math is identical regardless of tone; only the *copy* changes. The receipt's
closing line and the daily notification have a respectful default and an opt-in
"hard truth" variant (see `domain/Copy.kt`). Default is respectful — the stronger
wording is shown only after the user enables hard-truth mode in Settings.
