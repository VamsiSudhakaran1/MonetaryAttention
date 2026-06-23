"""API-key guard for mutating/ingest endpoints.

Deny-by-default: if ``ADMIN_API_KEY`` is not configured, the guarded endpoints
are locked entirely, so a fresh public deployment cannot be tampered with.
Read-only endpoints (e.g. GET /platforms) are never guarded.
"""

from __future__ import annotations

import os

from fastapi import Header, HTTPException, status


def require_api_key(x_api_key: str | None = Header(default=None)) -> None:
    expected = os.environ.get("ADMIN_API_KEY")
    if not expected:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Write endpoints are disabled (ADMIN_API_KEY is not configured).",
        )
    if not x_api_key or x_api_key != expected:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing or invalid X-API-Key.",
        )
