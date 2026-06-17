"""Glue between persisted rows and the pure estimate engine."""

from __future__ import annotations

from datetime import date, timedelta

from sqlalchemy.orm import Session

from . import estimate as engine
from .orm import PlatformConfigRow, UsageEventRow


def load_configs(db: Session) -> dict[str, engine.PlatformConfig]:
    rows = db.query(PlatformConfigRow).all()
    return {
        r.package_name: engine.PlatformConfig(
            platform=r.platform,
            package_name=r.package_name,
            ads_per_minute=r.ads_per_minute,
            low_cpm_inr=r.low_cpm_inr,
            high_cpm_inr=r.high_cpm_inr,
            monetized=r.monetized,
        )
        for r in rows
    }


def _usage_for_dates(
    db: Session, user_id: str, dates: list[str]
) -> dict[str, float]:
    """Sum usage seconds per package across the given local dates."""
    rows = (
        db.query(UsageEventRow)
        .filter(UsageEventRow.user_id == user_id)
        .filter(UsageEventRow.local_date.in_(dates))
        .all()
    )
    totals: dict[str, float] = {}
    for r in rows:
        totals[r.package_name] = totals.get(r.package_name, 0.0) + r.duration_seconds
    return totals


def daily_receipt(db: Session, user_id: str, day: str) -> engine.AttentionReceipt:
    configs = load_configs(db)
    usage = _usage_for_dates(db, user_id, [day])
    return engine.build_receipt(configs, usage)


def weekly_summary(
    db: Session, user_id: str, end_day: str
) -> tuple[str, engine.AttentionReceipt]:
    end = date.fromisoformat(end_day)
    days = [(end - timedelta(days=i)).isoformat() for i in range(7)]
    configs = load_configs(db)
    usage = _usage_for_dates(db, user_id, days)
    start = (end - timedelta(days=6)).isoformat()
    return start, engine.build_receipt(configs, usage)
