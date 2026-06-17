"""Attention-value estimate engine.

This is the canonical Python implementation of the math described in
``docs/ESTIMATE_SPEC.md``. The Android app mirrors these formulas in Kotlin.

Everything here is a *transparent estimate* — never an exact claim.
"""

from __future__ import annotations

from dataclasses import dataclass
from decimal import ROUND_HALF_UP, Decimal


def _round_half_up(value: float) -> int:
    """Round half up to the nearest integer (e.g. 14.5 -> 15)."""
    return int(Decimal(str(value)).quantize(Decimal("1"), rounding=ROUND_HALF_UP))


# User-assisted calibration: only trust a personal ads/minute rate once the
# user has marked ads over a meaningful sample of time. Below this, fall back to
# the seeded platform default. See "Calibration" in docs/ESTIMATE_SPEC.md.
MIN_CALIBRATION_MINUTES = 15.0


def effective_ads_per_minute(
    config: "PlatformConfig",
    observed_ads: int,
    observed_minutes: float,
) -> float:
    """Personal ads/minute from the user's own ad marks, else the default.

    ``observed_ads`` is how many ads the user marked while spending
    ``observed_minutes`` on this platform. With enough observed time we honour
    their real frequency (including 0, if they genuinely saw none); otherwise we
    keep the conservative seeded default.
    """
    if observed_minutes >= MIN_CALIBRATION_MINUTES and observed_ads >= 0:
        return observed_ads / observed_minutes
    return config.ads_per_minute


@dataclass(frozen=True)
class PlatformConfig:
    """Tunable per-platform ad assumptions."""

    platform: str
    package_name: str
    ads_per_minute: float
    low_cpm_inr: float
    high_cpm_inr: float
    monetized: bool = True


@dataclass(frozen=True)
class PlatformEstimate:
    """Estimate for a single platform over some usage window."""

    platform: str
    package_name: str
    minutes: float
    estimated_ads_seen: int
    value_low_inr: float
    value_high_inr: float


@dataclass(frozen=True)
class AttentionReceipt:
    """Aggregated estimate across all monetized platforms."""

    total_minutes: float
    estimated_ads_seen: int
    estimated_value_low_inr: int
    estimated_value_high_inr: int
    user_received_inr: int
    per_platform: list[PlatformEstimate]


def estimate_platform(
    config: PlatformConfig,
    duration_seconds: float,
    personal_ads_per_minute: float | None = None,
) -> PlatformEstimate:
    """Estimate ads + value for one platform given seconds of usage.

    ``personal_ads_per_minute``, when provided, overrides the platform default
    (used for user-assisted calibration; see :func:`effective_ads_per_minute`).
    """
    seconds = max(0.0, float(duration_seconds))
    minutes = seconds / 60.0

    if not config.monetized:
        return PlatformEstimate(
            platform=config.platform,
            package_name=config.package_name,
            minutes=minutes,
            estimated_ads_seen=0,
            value_low_inr=0.0,
            value_high_inr=0.0,
        )

    rate = config.ads_per_minute if personal_ads_per_minute is None else personal_ads_per_minute
    ads = _round_half_up(minutes * rate)
    value_low = ads * config.low_cpm_inr / 1000.0
    value_high = ads * config.high_cpm_inr / 1000.0
    return PlatformEstimate(
        platform=config.platform,
        package_name=config.package_name,
        minutes=minutes,
        estimated_ads_seen=ads,
        value_low_inr=value_low,
        value_high_inr=value_high,
    )


def build_receipt(
    configs_by_package: dict[str, PlatformConfig],
    usage_seconds_by_package: dict[str, float],
    personal_rates_by_package: dict[str, float] | None = None,
) -> AttentionReceipt:
    """Combine per-platform usage into a daily/weekly attention receipt.

    ``usage_seconds_by_package`` maps an Android package id to seconds spent.
    Packages without a known config are ignored (they don't contribute value),
    but their time is *not* counted in totals either — only configured platforms
    are part of the "monetized attention" story.

    ``personal_rates_by_package`` optionally supplies a calibrated ads/minute
    rate per package (from user-assisted ad counting), overriding the default.
    """
    rates = personal_rates_by_package or {}
    per_platform: list[PlatformEstimate] = []
    total_minutes = 0.0
    total_ads = 0
    total_low = 0.0
    total_high = 0.0

    for package, seconds in usage_seconds_by_package.items():
        config = configs_by_package.get(package)
        if config is None:
            continue
        est = estimate_platform(config, seconds, rates.get(package))
        per_platform.append(est)
        total_minutes += est.minutes
        total_ads += est.estimated_ads_seen
        total_low += est.value_low_inr
        total_high += est.value_high_inr

    per_platform.sort(key=lambda e: e.minutes, reverse=True)

    return AttentionReceipt(
        total_minutes=total_minutes,
        estimated_ads_seen=total_ads,
        estimated_value_low_inr=_round_half_up(total_low),
        estimated_value_high_inr=_round_half_up(total_high),
        user_received_inr=0,
        per_platform=per_platform,
    )
