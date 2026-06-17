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


def estimate_platform(config: PlatformConfig, duration_seconds: float) -> PlatformEstimate:
    """Estimate ads + value for one platform given seconds of usage."""
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

    ads = _round_half_up(minutes * config.ads_per_minute)
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
) -> AttentionReceipt:
    """Combine per-platform usage into a daily/weekly attention receipt.

    ``usage_seconds_by_package`` maps an Android package id to seconds spent.
    Packages without a known config are ignored (they don't contribute value),
    but their time is *not* counted in totals either — only configured platforms
    are part of the "monetized attention" story.
    """
    per_platform: list[PlatformEstimate] = []
    total_minutes = 0.0
    total_ads = 0
    total_low = 0.0
    total_high = 0.0

    for package, seconds in usage_seconds_by_package.items():
        config = configs_by_package.get(package)
        if config is None:
            continue
        est = estimate_platform(config, seconds)
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
