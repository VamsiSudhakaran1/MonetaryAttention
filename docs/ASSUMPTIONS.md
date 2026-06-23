# Estimate assumptions

The whole app lives or dies on whether people trust the numbers. So: **every
value below is a transparent, conservative estimate — not a measurement.** The
receipt always shows a **range**, never a single exact figure, and the in-app
"How was this calculated?" view shows the working.

Formula (see [ESTIMATE_SPEC.md](ESTIMATE_SPEC.md)):

```
minutes  = duration_seconds / 60
ads_seen = round(minutes × ads_per_minute)
value    = ads_seen × CPM / 1000      # computed at low and high CPM
```

## Default table

Defaults shipped with the app (tunable; the manual "I saw an ad" calibration and,
on the `full` build, the opt-in scanner refine `ads/min` per user).

| Platform | Ads/min | Low CPM (₹) | High CPM (₹) | Confidence | Reasoning |
|---|---|---|---|---|---|
| YouTube | 0.20 | 250 | 800 | Medium | Pre/mid-roll on longer sessions; Premium users should mark ad-free. |
| Facebook | 0.35 | 200 | 600 | Medium | Dense in-feed ads; high-value ad market. |
| Instagram | 0.45 | 220 | 650 | Low–Med | Reels ad load is high and bursty; calibration helps a lot. |
| X | 0.30 | 150 | 450 | Low | Promoted posts vary widely by account. |
| Reddit | 0.25 | 120 | 400 | Low | Promoted posts; lower CPM. |
| Snapchat | 0.30 | 150 | 450 | Low | Stories/Discover ads. |
| ShareChat | 0.40 | 80 | 300 | Low | Regional CPMs lower than global. |
| Moj | 0.50 | 80 | 300 | Low | Short-video, high ad frequency, low CPM. |
| Josh | 0.50 | 80 | 300 | Low | Short-video, high ad frequency, low CPM. |
| Chrome | 0.10 | 100 | 350 | Low | Web browsing is mixed; conservative. |
| WhatsApp | — | — | — | — | **Not monetized** — time tracked, value always ₹0. |

_Last reviewed: 2026-06._

## Why ranges, and why conservative

- Ad load depends on region, account, content type, and Premium status —
  none of which a usage-time-only app can know precisely.
- We deliberately keep CPMs conservative so the app under-claims rather than
  over-claims. Over-claiming would (rightly) destroy trust.

## How users improve accuracy

1. **Ad-free toggles** — mark Premium apps (YouTube/X) so their value is ₹0.
2. **"I saw an ad" tile** — manual calibration replaces the default `ads/min`
   with the user's observed rate.
3. **Opt-in scanner** (`full` build only) — automates the above from on-screen
   ad markers.

## Updating these numbers

Keep this table, `docs/ESTIMATE_SPEC.md`, `backend/app/seed.py`,
`android/.../domain/PlatformConfig.kt`, and `ios/Shared/EstimateEngine.swift`
in sync. Bump _Last reviewed_ when changing values.
