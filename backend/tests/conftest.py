import os

# Point the app's module-level engine at an ephemeral in-memory DB so importing
# the app (and its lifespan) never creates a stray file. Tests use their own
# isolated engine via the dependency override below.
os.environ.setdefault("DATABASE_URL", "sqlite://")
# Configure the write-endpoint API key for tests; the client sends it by default.
os.environ.setdefault("ADMIN_API_KEY", "test-key")

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.db import Base, get_db
from app.main import app
from app.seed import seed_platforms


@pytest.fixture()
def client():
    """A TestClient backed by an isolated in-memory SQLite database.

    Uses a dependency override (no module reloading) so each test gets a fresh,
    seeded database without polluting global state.
    """
    engine = create_engine(
        "sqlite://",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
        future=True,
    )
    TestingSession = sessionmaker(bind=engine, autoflush=False, autocommit=False, future=True)
    Base.metadata.create_all(bind=engine)

    seed_db = TestingSession()
    try:
        seed_platforms(seed_db)
    finally:
        seed_db.close()

    def override_get_db():
        db = TestingSession()
        try:
            yield db
        finally:
            db.close()

    app.dependency_overrides[get_db] = override_get_db
    # The app's lifespan also seeds the *real* DB; we don't enter it here, so
    # use the client without triggering startup against the file DB.
    with TestClient(app, headers={"X-API-Key": "test-key"}) as c:
        yield c
    app.dependency_overrides.clear()
    Base.metadata.drop_all(bind=engine)
