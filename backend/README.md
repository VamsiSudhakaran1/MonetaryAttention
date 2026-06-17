# Attention Mirror — Backend

FastAPI service that stores tunable per-platform ad assumptions and turns
reported app-usage into honest, **estimated** "attention receipts".

> All values are estimates. The service never claims to detect real ads or exact
> rupee amounts. See [`../docs/ESTIMATE_SPEC.md`](../docs/ESTIMATE_SPEC.md).

## Run

```bash
cd backend
python3 -m venv .venv && . .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload
```

Open http://127.0.0.1:8000/docs for the interactive API.

By default it uses a local SQLite file (`attention_mirror.db`). For production
set `DATABASE_URL` to a PostgreSQL URL, e.g.:

```bash
export DATABASE_URL=postgresql+psycopg://user:pass@host:5432/attention
```

## Test

```bash
. .venv/bin/activate
pytest -q
```

## API

| Method | Path                                  | Purpose                                  |
|--------|---------------------------------------|------------------------------------------|
| GET    | `/health`                             | Liveness check                           |
| GET    | `/platforms`                          | List per-platform ad assumptions         |
| PUT    | `/platforms/{package_name}`           | Admin: create/tune a platform config     |
| DELETE | `/platforms/{package_name}`           | Admin: remove a platform config          |
| POST   | `/usage`                              | Upsert a batch of per-day usage events   |
| GET    | `/users/{user_id}/receipt/{day}`      | Daily attention receipt                  |
| GET    | `/users/{user_id}/weekly/{end_day}`   | 7-day rolling summary (ends on `end_day`)|

### Example

```bash
curl -X POST localhost:8000/usage -H 'Content-Type: application/json' -d '{
  "user_id":"u1",
  "events":[
    {"package_name":"com.google.android.youtube","app_name":"YouTube","local_date":"2026-06-17","duration_seconds":4320}
  ]
}'

curl localhost:8000/users/u1/receipt/2026-06-17
```

## Layout

```
app/
  main.py         FastAPI app + lifespan (table create + seed)
  db.py           Engine/session (SQLite default, PostgreSQL via DATABASE_URL)
  orm.py          SQLAlchemy tables: platform_configs, usage_events
  schemas.py      Pydantic request/response contracts
  estimate.py     Pure estimate engine (canonical math)
  service.py      Bridges persisted rows <-> estimate engine
  seed.py         Default platform assumptions
  routers/        platforms.py (admin config), usage.py (ingest + receipts)
tests/            pytest: engine math + API flows
```
