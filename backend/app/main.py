"""Attention Mirror backend — FastAPI app.

Serves tunable platform config and turns reported app-usage into honest,
estimated "attention receipts". See ``docs/ESTIMATE_SPEC.md`` for the math.
"""

from __future__ import annotations

from contextlib import asynccontextmanager

from fastapi import FastAPI

from .db import Base, SessionLocal, engine
from .routers import platforms, usage
from .seed import seed_platforms


@asynccontextmanager
async def lifespan(app: FastAPI):
    Base.metadata.create_all(bind=engine)
    db = SessionLocal()
    try:
        seed_platforms(db)
    finally:
        db.close()
    yield


app = FastAPI(
    title="Attention Mirror API",
    description="See who profited from your scrolling. All values are estimates.",
    version="0.1.0",
    lifespan=lifespan,
)

app.include_router(platforms.router)
app.include_router(usage.router)


@app.get("/health", tags=["meta"])
def health():
    return {"status": "ok"}
