"""Admin config endpoints — view and tune per-platform ad assumptions."""

from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from ..auth import require_api_key
from ..db import get_db
from ..orm import PlatformConfigRow
from ..schemas import PlatformConfigIn, PlatformConfigOut

router = APIRouter(prefix="/platforms", tags=["platforms"])


@router.get("", response_model=list[PlatformConfigOut])
def list_platforms(db: Session = Depends(get_db)):
    return db.query(PlatformConfigRow).order_by(PlatformConfigRow.platform).all()


@router.put(
    "/{package_name}",
    response_model=PlatformConfigOut,
    dependencies=[Depends(require_api_key)],
)
def upsert_platform(
    package_name: str, body: PlatformConfigIn, db: Session = Depends(get_db)
):
    if body.high_cpm_inr < body.low_cpm_inr:
        raise HTTPException(422, "high_cpm_inr must be >= low_cpm_inr")
    if body.package_name != package_name:
        raise HTTPException(422, "package_name in path and body must match")

    row = db.get(PlatformConfigRow, package_name)
    if row is None:
        row = PlatformConfigRow(package_name=package_name)
        db.add(row)
    row.platform = body.platform
    row.ads_per_minute = body.ads_per_minute
    row.low_cpm_inr = body.low_cpm_inr
    row.high_cpm_inr = body.high_cpm_inr
    row.monetized = body.monetized
    db.commit()
    db.refresh(row)
    return row


@router.delete(
    "/{package_name}",
    status_code=204,
    dependencies=[Depends(require_api_key)],
)
def delete_platform(package_name: str, db: Session = Depends(get_db)):
    row = db.get(PlatformConfigRow, package_name)
    if row is None:
        raise HTTPException(404, "platform not found")
    db.delete(row)
    db.commit()
