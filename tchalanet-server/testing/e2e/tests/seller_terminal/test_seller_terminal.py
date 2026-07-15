"""SellerTerminal admin lifecycle e2e — the current seller model (no outlet/seller/terminal).

Auth via the local provider (no Keycloak; Firebase in prod):
    TCH_E2E_AUTH_PROVIDER=local-jwt

Exercises /admin/seller-terminals. NOTE: *creating* a seller-terminal provisions a login identity,
which only a provisioning-capable provider (firebase) supports — under local-jwt the create/lifecycle
tests skip cleanly, while the read endpoints (list/summary) still run.
"""
from __future__ import annotations

import uuid

import pytest

from tch_e2e.api_response import assert_ok
from tch_e2e.client import ApiClient
from tch_e2e.config import SeedIds

_BASE = "/admin/seller-terminals"
_PROVISIONING_UNSUPPORTED = "not supported for configured provider"


def _hdrs() -> dict:
    # Mutating admin endpoints require a per-request X-Request-Id.
    return {"X-Request-Id": f"e2e-{uuid.uuid4()}"}


def _admin(super_admin_client: ApiClient, seed_ids: SeedIds) -> ApiClient:
    """Admin client scoped to the seeded tenant via the SUPER_ADMIN tenant override."""
    return super_admin_client.with_tenant(seed_ids.tenant_id, override_reason="e2e-seller-terminal")


def _data(response):
    body = response.json().get("data")
    if isinstance(body, dict) and set(body.keys()) == {"value"}:
        return body["value"]
    return body


def _create_payload() -> dict:
    s = uuid.uuid4().hex[:8]
    return {
        "terminalCode": f"E2E-{s}",
        "displayName": f"E2E Terminal {s}",
        "firstName": "E2E",
        "lastName": "Tester",
        "email": f"e2e-{s}@test.test",
        "phoneNumber": "+50900000000",
        "commissionRate": "10.00",
        "initialPin": "123456",
    }


def _create_or_skip(admin: ApiClient) -> tuple[str, dict]:
    payload = _create_payload()
    resp = admin.post(_BASE, json=payload, headers=_hdrs())
    if resp.status_code == 422 and _PROVISIONING_UNSUPPORTED in resp.text:
        pytest.skip(
            "SellerTerminal creation provisions a login identity — needs a provisioning provider "
            "(firebase); the local-jwt provider cannot provision identities."
        )
    if resp.status_code == 403 and "entitlement.limit_exceeded" in resp.text:
        pytest.skip("Local tenant already reached the seller-terminal entitlement limit.")
    assert_ok(resp, expected=(200, 201))
    terminal_id = _data(resp)
    assert terminal_id, "create must return a seller-terminal id"
    return str(terminal_id), payload


@pytest.mark.L1
@pytest.mark.seller_terminal
def test_list_and_summary(super_admin_client: ApiClient, seed_ids: SeedIds) -> None:
    """Read endpoints work under any provider (validates auth + tenant override + routing)."""
    admin = _admin(super_admin_client, seed_ids)

    listed = admin.get(_BASE, params={"page": 0, "size": 50}, headers=_hdrs())
    assert_ok(listed)
    page = listed.json()["data"]
    assert "items" in page or "content" in page, f"expected a page shape, got: {page}"

    summary = admin.get(f"{_BASE}/summary", headers=_hdrs())
    assert_ok(summary)


@pytest.mark.L1
@pytest.mark.seller_terminal
def test_create_and_get(super_admin_client: ApiClient, seed_ids: SeedIds) -> None:
    admin = _admin(super_admin_client, seed_ids)
    terminal_id, payload = _create_or_skip(admin)

    got = admin.get(f"{_BASE}/{terminal_id}", headers=_hdrs())
    assert_ok(got)
    view = got.json()["data"]
    assert view["terminalCode"] == payload["terminalCode"]
    assert view["displayName"] == payload["displayName"]
    assert view["status"] != "BLOCKED"


@pytest.mark.L1
@pytest.mark.seller_terminal
def test_block_then_unblock(super_admin_client: ApiClient, seed_ids: SeedIds) -> None:
    admin = _admin(super_admin_client, seed_ids)
    terminal_id, _ = _create_or_skip(admin)

    blocked = admin.patch(
        f"{_BASE}/{terminal_id}/block", json={"reason": "e2e lifecycle test"}, headers=_hdrs()
    )
    assert_ok(blocked, expected=(200, 204))
    assert admin.get(f"{_BASE}/{terminal_id}", headers=_hdrs()).json()["data"]["status"] == "BLOCKED"

    unblocked = admin.patch(f"{_BASE}/{terminal_id}/unblock", headers=_hdrs())
    assert_ok(unblocked, expected=(200, 204))
    assert admin.get(f"{_BASE}/{terminal_id}", headers=_hdrs()).json()["data"]["status"] != "BLOCKED"
