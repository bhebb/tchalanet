"""SellerTerminal admin lifecycle e2e — the current seller model (no outlet/seller/terminal).

Runs against a seeded non-prod stack with the local auth provider:
    TCH_E2E_AUTH_PROVIDER=local-jwt

Exercises /admin/seller-terminals: create, get, list, summary, block, unblock.
"""
from __future__ import annotations

import uuid

import pytest

from tch_e2e.api_response import assert_ok
from tch_e2e.client import ApiClient
from tch_e2e.config import SeedIds

_BASE = "/admin/seller-terminals"


def _admin(super_admin_client: ApiClient, seed_ids: SeedIds) -> ApiClient:
    """Admin client scoped to the seeded tenant (controller uses ctx.tenantIdRequired())."""
    return super_admin_client.with_tenant(seed_ids.tenant_id, override_reason="e2e-seller-terminal")


def _data(response) -> object:
    body = response.json().get("data")
    # typed ids serialize as scalar strings; some envelopes wrap {"value": ...}
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


def _create(admin: ApiClient) -> tuple[str, dict]:
    payload = _create_payload()
    resp = admin.post(_BASE, json=payload)
    assert_ok(resp, expected=(200, 201))
    terminal_id = _data(resp)
    assert terminal_id, "create must return a seller-terminal id"
    return str(terminal_id), payload


@pytest.mark.L1
@pytest.mark.seller_terminal
def test_create_and_get(super_admin_client: ApiClient, seed_ids: SeedIds) -> None:
    admin = _admin(super_admin_client, seed_ids)
    terminal_id, payload = _create(admin)

    got = admin.get(f"{_BASE}/{terminal_id}")
    assert_ok(got)
    view = got.json()["data"]
    assert view["terminalCode"] == payload["terminalCode"]
    assert view["displayName"] == payload["displayName"]
    assert view["status"] != "BLOCKED"


@pytest.mark.L1
@pytest.mark.seller_terminal
def test_list_includes_created_and_summary(super_admin_client: ApiClient, seed_ids: SeedIds) -> None:
    admin = _admin(super_admin_client, seed_ids)
    terminal_id, _ = _create(admin)

    listed = admin.get(_BASE, params={"page": 0, "size": 100})
    assert_ok(listed)
    page = listed.json()["data"]
    items = page.get("items") or page.get("content") or []
    ids = {str(row.get("id")) for row in items}
    assert terminal_id in ids, "created seller-terminal should appear in the list"

    summary = admin.get(f"{_BASE}/summary")
    assert_ok(summary)


@pytest.mark.L1
@pytest.mark.seller_terminal
def test_block_then_unblock(super_admin_client: ApiClient, seed_ids: SeedIds) -> None:
    admin = _admin(super_admin_client, seed_ids)
    terminal_id, _ = _create(admin)

    blocked = admin.patch(f"{_BASE}/{terminal_id}/block", json={"reason": "e2e lifecycle test"})
    assert_ok(blocked, expected=(200, 204))
    assert admin.get(f"{_BASE}/{terminal_id}").json()["data"]["status"] == "BLOCKED"

    unblocked = admin.patch(f"{_BASE}/{terminal_id}/unblock")
    assert_ok(unblocked, expected=(200, 204))
    assert admin.get(f"{_BASE}/{terminal_id}").json()["data"]["status"] != "BLOCKED"
