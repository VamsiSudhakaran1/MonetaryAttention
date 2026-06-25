"""Default platform assumptions — seeded on startup if the table is empty.

These mirror the table in ``docs/ESTIMATE_SPEC.md``. Conservative defaults;
tune via the admin config endpoint rather than editing the app.
"""

from __future__ import annotations

from sqlalchemy.orm import Session

from .orm import PlatformConfigRow

DEFAULT_PLATFORMS: list[dict] = [
    {"platform": "YouTube", "package_name": "com.google.android.youtube", "ads_per_minute": 0.20, "low_cpm_inr": 310, "high_cpm_inr": 980, "monetized": True},
    {"platform": "Facebook", "package_name": "com.facebook.katana", "ads_per_minute": 0.35, "low_cpm_inr": 250, "high_cpm_inr": 730, "monetized": True},
    {"platform": "Instagram", "package_name": "com.instagram.android", "ads_per_minute": 0.45, "low_cpm_inr": 270, "high_cpm_inr": 800, "monetized": True},
    {"platform": "X", "package_name": "com.twitter.android", "ads_per_minute": 0.30, "low_cpm_inr": 185, "high_cpm_inr": 550, "monetized": True},
    {"platform": "Reddit", "package_name": "com.reddit.frontpage", "ads_per_minute": 0.25, "low_cpm_inr": 150, "high_cpm_inr": 490, "monetized": True},
    {"platform": "Snapchat", "package_name": "com.snapchat.android", "ads_per_minute": 0.30, "low_cpm_inr": 185, "high_cpm_inr": 550, "monetized": True},
    {"platform": "ShareChat", "package_name": "in.mohalla.sharechat", "ads_per_minute": 0.40, "low_cpm_inr": 100, "high_cpm_inr": 370, "monetized": True},
    {"platform": "Moj", "package_name": "in.mohalla.video", "ads_per_minute": 0.50, "low_cpm_inr": 100, "high_cpm_inr": 370, "monetized": True},
    {"platform": "Josh", "package_name": "com.eterno.shortvideos", "ads_per_minute": 0.50, "low_cpm_inr": 100, "high_cpm_inr": 370, "monetized": True},
    {"platform": "Chrome", "package_name": "com.android.chrome", "ads_per_minute": 0.10, "low_cpm_inr": 125, "high_cpm_inr": 430, "monetized": True},
    # Tracked for time only — never contributes attention value.
    {"platform": "WhatsApp", "package_name": "com.whatsapp", "ads_per_minute": 0.0, "low_cpm_inr": 0, "high_cpm_inr": 0, "monetized": False},
]


def seed_platforms(db: Session) -> None:
    if db.query(PlatformConfigRow).count() > 0:
        return
    for row in DEFAULT_PLATFORMS:
        db.add(PlatformConfigRow(**row))
    db.commit()
