def test_health(client):
    assert client.get("/health").json() == {"status": "ok"}


def test_platforms_seeded(client):
    rows = client.get("/platforms").json()
    packages = {r["package_name"] for r in rows}
    assert "com.google.android.youtube" in packages
    assert "com.whatsapp" in packages


def test_tune_platform_config(client):
    body = {
        "package_name": "com.google.android.youtube",
        "platform": "YouTube",
        "ads_per_minute": 1.0,
        "low_cpm_inr": 300,
        "high_cpm_inr": 900,
        "monetized": True,
    }
    r = client.put("/platforms/com.google.android.youtube", json=body)
    assert r.status_code == 200
    assert r.json()["ads_per_minute"] == 1.0


def test_bad_cpm_rejected(client):
    body = {
        "package_name": "com.x",
        "platform": "X",
        "ads_per_minute": 0.1,
        "low_cpm_inr": 500,
        "high_cpm_inr": 100,
        "monetized": True,
    }
    assert client.put("/platforms/com.x", json=body).status_code == 422


def test_usage_to_receipt_flow(client):
    batch = {
        "user_id": "u123",
        "events": [
            {
                "package_name": "com.google.android.youtube",
                "app_name": "YouTube",
                "local_date": "2026-06-17",
                "duration_seconds": 72 * 60,
            },
            {
                "package_name": "com.whatsapp",
                "app_name": "WhatsApp",
                "local_date": "2026-06-17",
                "duration_seconds": 30 * 60,
            },
        ],
    }
    assert client.post("/usage", json=batch).status_code == 202

    receipt = client.get("/users/u123/receipt/2026-06-17").json()
    assert receipt["estimated_ads_seen"] == 14  # YouTube only; WhatsApp not monetized
    assert receipt["user_received_inr"] == 0
    # WhatsApp still appears (time tracked) but with zero value
    whatsapp = next(p for p in receipt["per_platform"] if p["platform"] == "WhatsApp")
    assert whatsapp["estimated_ads_seen"] == 0
    assert whatsapp["minutes"] == 30.0


def test_usage_upsert_is_idempotent(client):
    def post(seconds):
        client.post(
            "/usage",
            json={
                "user_id": "u9",
                "events": [
                    {
                        "package_name": "com.instagram.android",
                        "app_name": "Instagram",
                        "local_date": "2026-06-17",
                        "duration_seconds": seconds,
                    }
                ],
            },
        )

    post(10 * 60)
    post(34 * 60)  # latest value wins, not summed
    receipt = client.get("/users/u9/receipt/2026-06-17").json()
    insta = next(p for p in receipt["per_platform"] if p["platform"] == "Instagram")
    assert insta["minutes"] == 34.0


def test_weekly_summary(client):
    for day in ["2026-06-15", "2026-06-16", "2026-06-17"]:
        client.post(
            "/usage",
            json={
                "user_id": "uw",
                "events": [
                    {
                        "package_name": "com.google.android.youtube",
                        "app_name": "YouTube",
                        "local_date": day,
                        "duration_seconds": 60 * 60,
                    }
                ],
            },
        )
    summary = client.get("/users/uw/weekly/2026-06-17").json()
    assert summary["start_date"] == "2026-06-11"
    assert summary["end_date"] == "2026-06-17"
    # 3 days * 60 min * 0.20 ads/min = 36 ads
    assert summary["estimated_ads_seen"] == 36
