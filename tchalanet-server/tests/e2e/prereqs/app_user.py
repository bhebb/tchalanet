"""Ensure the cashier's app_user row is in sync with Keycloak."""
from __future__ import annotations

from lib.api import ApiClient, assert_ok


def ensure_app_user_synced(super_admin: ApiClient) -> dict[str, int]:
    """Trigger the Keycloak → app_user bootstrap sync (SUPER_ADMIN).

    Hits `POST /platform/ops/sync/identity/keycloak-bootstrap-users` which iterates over the
    configured users in `application.yml` and upserts their app_user rows + tenant bindings.
    Idempotent; safe to call on every test run.

    Returns the sync summary `{attempted, foundInKeycloak, updatedRows}`.
    """
    response = super_admin.post("/platform/ops/sync/identity/keycloak-bootstrap-users")
    assert_ok(response)
    body = response.json()
    data = body.get("data") if isinstance(body, dict) else {}
    if not isinstance(data, dict):
        data = {}
    return {
        "attempted": int(data.get("attempted", 0)),
        "foundInKeycloak": int(data.get("foundInKeycloak", 0)),
        "updatedRows": int(data.get("updatedRows", 0)),
    }
