"""Usage ingestion + receipt/summary endpoints."""

from __future__ import annotations

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from .. import service
from ..db import get_db
from ..orm import UsageEventRow
from ..schemas import (
    PlatformEstimateOut,
    ReceiptOut,
    UsageBatchIn,
    WeeklySummaryOut,
)

router = APIRouter(tags=["usage"])


@router.post("/usage", status_code=202)
def ingest_usage(batch: UsageBatchIn, db: Session = Depends(get_db)):
    """Upsert one row per (user, package, day). Idempotent by latest value."""
    for event in batch.events:
        row = (
            db.query(UsageEventRow)
            .filter_by(
                user_id=batch.user_id,
                package_name=event.package_name,
                local_date=event.local_date,
            )
            .one_or_none()
        )
        if row is None:
            row = UsageEventRow(
                user_id=batch.user_id,
                package_name=event.package_name,
                local_date=event.local_date,
            )
            db.add(row)
        row.app_name = event.app_name
        row.duration_seconds = event.duration_seconds
    db.commit()
    return {"accepted": len(batch.events)}


def _platform_payload(per_platform) -> list[PlatformEstimateOut]:
    return [
        PlatformEstimateOut(
            platform=p.platform,
            package_name=p.package_name,
            minutes=round(p.minutes, 1),
            estimated_ads_seen=p.estimated_ads_seen,
            value_low_inr=round(p.value_low_inr, 2),
            value_high_inr=round(p.value_high_inr, 2),
        )
        for p in per_platform
    ]


@router.get("/users/{user_id}/receipt/{day}", response_model=ReceiptOut)
def get_receipt(user_id: str, day: str, db: Session = Depends(get_db)):
    receipt = service.daily_receipt(db, user_id, day)
    return ReceiptOut(
        user_id=user_id,
        date=day,
        total_minutes=round(receipt.total_minutes, 1),
        estimated_ads_seen=receipt.estimated_ads_seen,
        estimated_value_low_inr=receipt.estimated_value_low_inr,
        estimated_value_high_inr=receipt.estimated_value_high_inr,
        user_received_inr=receipt.user_received_inr,
        per_platform=_platform_payload(receipt.per_platform),
    )


@router.get("/users/{user_id}/weekly/{end_day}", response_model=WeeklySummaryOut)
def get_weekly(user_id: str, end_day: str, db: Session = Depends(get_db)):
    start, receipt = service.weekly_summary(db, user_id, end_day)
    return WeeklySummaryOut(
        user_id=user_id,
        start_date=start,
        end_date=end_day,
        total_minutes=round(receipt.total_minutes, 1),
        estimated_ads_seen=receipt.estimated_ads_seen,
        estimated_value_low_inr=receipt.estimated_value_low_inr,
        estimated_value_high_inr=receipt.estimated_value_high_inr,
        user_received_inr=receipt.user_received_inr,
        per_platform=_platform_payload(receipt.per_platform),
    )
