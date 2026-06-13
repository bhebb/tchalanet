"""Ensure deterministic Firebase users are linked to their app_user rows."""
from __future__ import annotations

from tch_e2e.api_response import assert_ok
from tch_e2e.client import ApiClient


def ensure_app_user_synced(super_admin: ApiClient, *, strict: bool = False) -> dict[str, int]:
    """Trigger the Firebase → app_user bootstrap sync (SUPER_ADMIN). Idempotent."""
    response = super_admin.post("/platform/ops/sync/identity/firebase-bootstrap-users")
    if response.status_code not in (200, 201, 202, 204):
        if strict:
            assert_ok(response)
        print(
            f"[warn] /platform/ops/sync/identity/firebase-bootstrap-users returned "
            f"{response.status_code} — skipping sync. Body: {response.text}"
        )
        return {"attempted": 0, "createdInFirebase": 0, "linked": 0}
    body = response.json()
    data = body.get("data") if isinstance(body, dict) else {}
    if not isinstance(data, dict):
        data = {}
    return {
        "attempted": int(data.get("attempted", 0)),
        "createdInFirebase": int(data.get("createdInFirebase", 0)),
        "linked": int(data.get("linked", 0)),
    }
