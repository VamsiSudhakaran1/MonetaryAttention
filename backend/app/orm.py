"""SQLAlchemy ORM models — the persisted data model."""

from __future__ import annotations

from datetime import datetime

from sqlalchemy import Boolean, DateTime, Float, Integer, String, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from .db import Base


class PlatformConfigRow(Base):
    """Tunable per-platform ad assumptions (admin-editable)."""

    __tablename__ = "platform_configs"

    package_name: Mapped[str] = mapped_column(String, primary_key=True)
    platform: Mapped[str] = mapped_column(String, nullable=False)
    ads_per_minute: Mapped[float] = mapped_column(Float, nullable=False, default=0.0)
    low_cpm_inr: Mapped[float] = mapped_column(Float, nullable=False, default=0.0)
    high_cpm_inr: Mapped[float] = mapped_column(Float, nullable=False, default=0.0)
    monetized: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)


class UsageEventRow(Base):
    """A reported chunk of app usage from a device."""

    __tablename__ = "usage_events"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    user_id: Mapped[str] = mapped_column(String, index=True, nullable=False)
    package_name: Mapped[str] = mapped_column(String, index=True, nullable=False)
    app_name: Mapped[str] = mapped_column(String, nullable=False, default="")
    local_date: Mapped[str] = mapped_column(String, index=True, nullable=False)
    duration_seconds: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    created_at: Mapped[datetime] = mapped_column(
        DateTime, default=datetime.utcnow, nullable=False
    )

    __table_args__ = (
        UniqueConstraint(
            "user_id", "package_name", "local_date", name="uq_usage_user_pkg_date"
        ),
    )
