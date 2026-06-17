"""Pydantic request/response schemas (the API contract)."""

from __future__ import annotations

from pydantic import BaseModel, Field


class PlatformConfigIn(BaseModel):
    package_name: str
    platform: str
    ads_per_minute: float = Field(ge=0)
    low_cpm_inr: float = Field(ge=0)
    high_cpm_inr: float = Field(ge=0)
    monetized: bool = True


class PlatformConfigOut(PlatformConfigIn):
    pass


class UsageEventIn(BaseModel):
    """A single platform's usage for one local day."""

    package_name: str
    app_name: str = ""
    local_date: str = Field(description="ISO date, device local, e.g. 2026-06-17")
    duration_seconds: int = Field(ge=0)


class UsageBatchIn(BaseModel):
    user_id: str
    events: list[UsageEventIn]


class PlatformEstimateOut(BaseModel):
    platform: str
    package_name: str
    minutes: float
    estimated_ads_seen: int
    value_low_inr: float
    value_high_inr: float


class ReceiptOut(BaseModel):
    user_id: str
    date: str
    total_minutes: float
    estimated_ads_seen: int
    estimated_value_low_inr: int
    estimated_value_high_inr: int
    user_received_inr: int
    per_platform: list[PlatformEstimateOut]


class WeeklySummaryOut(BaseModel):
    user_id: str
    start_date: str
    end_date: str
    total_minutes: float
    estimated_ads_seen: int
    estimated_value_low_inr: int
    estimated_value_high_inr: int
    user_received_inr: int
    per_platform: list[PlatformEstimateOut]
