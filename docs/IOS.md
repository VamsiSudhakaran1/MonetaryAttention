# Attention Mirror on iOS — spec, limitations & plan

Goal: an iPhone version for friends on iOS. This document is the honest
engineering picture **before** we build, so expectations are set correctly.

## TL;DR

iOS **can** show "where your time went" and a value-framed receipt for the
user's *own* device, using Apple's Screen Time APIs. iOS **cannot** do
package-level free access to usage data, and **cannot** detect ads inside other
apps at all. So iOS is a **reduced, on-device** version of the Android app, and
it requires a special entitlement from Apple.

## What carries over

- **Backend** (`backend/`) — reusable for shared config (CPM/ad defaults).
- **Estimate math** — ported to `ios/Shared/EstimateEngine.swift`, identical to
  the Kotlin/Python engines (`docs/ESTIMATE_SPEC.md`).
- **Brand, copy, dynamic messaging logic** — portable as designs/strings.

## What does NOT carry over (platform limits)

| Android capability | iOS reality |
|---|---|
| `UsageStatsManager`: read per-app time freely, store it, compute value, send anywhere | **No public equivalent.** Usage comes only via **Screen Time / DeviceActivity**, and is **sandboxed** (below). |
| Per-app identity (package name, icon) | App identities are **opaque `ApplicationToken`s**. You can render `Label(token)` but **can't read the bundle id/name** or map to your own per-app rows freely. |
| Accessibility ad-scanner ("count Sponsored labels") | **Impossible.** iOS doesn't allow reading other apps' on-screen content. No ad detection of any kind. |
| Background usage collection into your DB + WorkManager notifications with the numbers | A `DeviceActivityMonitor` extension fires on **thresholds/intervals**, but raw per-app numbers stay inside the extensions. |

### The core constraint: the Screen Time sandbox

- You request authorization with **FamilyControls** (`AuthorizationCenter`).
  For self-monitoring use **`.individual`** mode (iOS 16+) — an adult monitoring
  their *own* device, no parent/child pairing needed.
- Usage is surfaced through a **`DeviceActivityReport`** SwiftUI view backed by a
  **DeviceActivityReportExtension**. The extension receives
  `DeviceActivityResults` and produces a view; **the host app never sees the raw
  totals** — that's Apple's privacy boundary.
- **Implication:** our "₹X created today, ₹0 returned" receipt must be **computed
  and rendered inside the report extension** (the extension *does* have the
  durations). The host app shows branding, settings, onboarding, and hosts the
  report view — but can't store/export the per-app numbers.

## Required entitlement

- **Family Controls (Distribution)** capability — must be **requested from Apple**
  (developer.apple.com → Account → request the capability; describe the
  digital-wellbeing/self-monitoring use). Development works under the
  development entitlement; App Store distribution needs the granted one.
  Approval is **not guaranteed** and gates the whole app.

## Reduced iOS MVP (what we'll actually build)

1. **Onboarding + FamilyControls authorization** (`.individual`).
2. **Receipt screen** = a `DeviceActivityReport` whose extension computes, from
   each app/category's duration, the same estimate (time × ad rate × CPM range)
   and renders the Attention Mirror receipt UI (value range, ₹0 returned,
   conclusion). Uses `EstimateEngine.swift`.
3. **Daily summary** via `DeviceActivityMonitor` interval + a **local
   notification** ("Your attention receipt is ready"). The notification can
   carry aggregate/threshold-based text, not arbitrary per-app numbers.
4. **Premium / ad-free toggles** stored in an **App Group**, read by the
   extension to zero out value for those apps (same idea as Android).
5. **Settings + brand + dynamic copy.**

Explicitly **out of scope on iOS**: the "I saw an ad" calibration tile, the
Accessibility ad-scanner, per-app icons/rupee rows exported to the host app,
and the shareable receipt image built from exact per-app numbers (a generic
share card is still possible).

## Proposed structure

```
ios/
  AttentionMirror/                 # SwiftUI app (host): onboarding, settings, report host
  AttentionMirrorReport/           # DeviceActivityReportExtension (renders the receipt)
  AttentionMirrorMonitor/          # DeviceActivityMonitor extension (schedules + notifies)
  Shared/
    EstimateEngine.swift           # ✅ already added — portable math
    PlatformConfig+Defaults.swift  # default ad/CPM table (port of seed.py)
    Copy.swift                     # tone-aware copy (port of Copy.kt)
```

- **Min iOS:** 16.0 (for FamilyControls `.individual` + DeviceActivity report).
- **Stack:** SwiftUI, FamilyControls, DeviceActivity, ManagedSettings (only if we
  later add limits), App Groups, UserNotifications.
- **No cross-platform framework** (Flutter/RN/KMP) — the blocker is OS data
  access, not UI; native is the right call.

## Build order (after Android is live)

1. Apply for the Family Controls entitlement (long lead time — start early).
2. Xcode project + targets (app, report extension, monitor extension, App Group).
3. Port defaults + copy; wire `EstimateEngine.swift`.
4. Authorization flow + the report-extension receipt.
5. Daily monitor + local notification.
6. Premium toggles via App Group.
7. TestFlight → App Store review.

## Honest expectation to set with friends

iPhone users will get the **receipt and time breakdown for their own phone**,
but **not** ad-accurate counts (no detection) and a **less granular** per-app
experience than Android — because Apple keeps Screen Time data sandboxed. The
emotional core ("your time made money, you got ₹0") still lands.
