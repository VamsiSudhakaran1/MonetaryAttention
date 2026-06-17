from app import estimate as engine


def cfg(**kw):
    base = dict(
        platform="YouTube",
        package_name="com.google.android.youtube",
        ads_per_minute=0.20,
        low_cpm_inr=250,
        high_cpm_inr=800,
        monetized=True,
    )
    base.update(kw)
    return engine.PlatformConfig(**base)


def test_single_platform_matches_spec_example():
    # 72 minutes * 0.20 = 14.4 -> 14 ads
    est = engine.estimate_platform(cfg(), 72 * 60)
    assert est.estimated_ads_seen == 14
    assert est.value_low_inr == 14 * 250 / 1000
    assert est.value_high_inr == 14 * 800 / 1000


def test_round_half_up():
    # 50 min * 0.45 = 22.5 -> 23 ads (half rounds up)
    est = engine.estimate_platform(
        cfg(platform="Instagram", ads_per_minute=0.45), 50 * 60
    )
    assert est.estimated_ads_seen == 23


def test_non_monetized_contributes_no_value():
    est = engine.estimate_platform(
        cfg(platform="WhatsApp", monetized=False, ads_per_minute=0.0), 60 * 60
    )
    assert est.estimated_ads_seen == 0
    assert est.value_low_inr == 0
    assert est.minutes == 60


def test_negative_duration_clamped():
    est = engine.estimate_platform(cfg(), -100)
    assert est.minutes == 0
    assert est.estimated_ads_seen == 0


def test_build_receipt_aggregates_and_sorts():
    configs = {
        "com.google.android.youtube": cfg(),
        "com.instagram.android": cfg(
            platform="Instagram",
            package_name="com.instagram.android",
            ads_per_minute=0.45,
            low_cpm_inr=220,
            high_cpm_inr=650,
        ),
        "com.facebook.katana": cfg(
            platform="Facebook",
            package_name="com.facebook.katana",
            ads_per_minute=0.35,
            low_cpm_inr=200,
            high_cpm_inr=600,
        ),
    }
    usage = {
        "com.google.android.youtube": 72 * 60,
        "com.facebook.katana": 48 * 60,
        "com.instagram.android": 34 * 60,
        "com.unknown.app": 99 * 60,  # no config -> ignored entirely
    }
    receipt = engine.build_receipt(configs, usage)

    # 14 + 17 + 15 ads
    assert receipt.estimated_ads_seen == 46
    assert receipt.estimated_value_low_inr == 10  # round(3.5+3.4+3.3)
    assert receipt.estimated_value_high_inr == 31  # round(11.2+10.2+9.75)
    assert receipt.user_received_inr == 0
    # sorted by minutes desc
    assert [p.platform for p in receipt.per_platform] == [
        "YouTube",
        "Facebook",
        "Instagram",
    ]
    # unknown app excluded
    assert all(p.package_name != "com.unknown.app" for p in receipt.per_platform)


def test_calibration_uses_personal_rate_with_enough_sample():
    # Spec example: 7 ads marked over 20 minutes -> 0.35 ads/min.
    rate = engine.effective_ads_per_minute(cfg(), observed_ads=7, observed_minutes=20)
    assert rate == 0.35


def test_calibration_falls_back_below_sample_threshold():
    # Only 5 minutes observed (< 15) -> keep the seeded default.
    rate = engine.effective_ads_per_minute(cfg(), observed_ads=4, observed_minutes=5)
    assert rate == 0.20


def test_calibration_honours_zero_ads_when_sampled_enough():
    # User spent 30 min and marked no ads -> trust their 0 rate.
    rate = engine.effective_ads_per_minute(cfg(), observed_ads=0, observed_minutes=30)
    assert rate == 0.0


def test_personal_rate_overrides_default_in_estimate():
    # 60 min at a calibrated 0.35 ads/min -> 21 ads (vs 12 at the 0.20 default).
    est = engine.estimate_platform(cfg(), 60 * 60, personal_ads_per_minute=0.35)
    assert est.estimated_ads_seen == 21


def test_build_receipt_applies_personal_rates():
    configs = {"com.google.android.youtube": cfg()}
    usage = {"com.google.android.youtube": 60 * 60}
    rates = {"com.google.android.youtube": 0.35}
    receipt = engine.build_receipt(configs, usage, personal_rates_by_package=rates)
    assert receipt.estimated_ads_seen == 21
