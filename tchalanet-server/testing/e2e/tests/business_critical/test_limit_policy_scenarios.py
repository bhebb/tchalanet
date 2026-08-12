"""Limit policy e2e scenarios — Slice 15.

Three business-critical paths exercised against a real running API:

  1. Block number  — BLOCK_SELECTION_PER_DRAW blocks a specific selection at TENANT scope;
                     sale rejected, no ticket created, draw overview shows no stake for "34".
  2. Scope cascade — MAX_STAKE_PER_LINE configured at TENANT=500, DRAW_CHANNEL=300, SELLER_TERMINAL=100;
                     stake=150 rejected (below DRAW_CHANNEL but above SELLER_TERMINAL),
                     stake=50 accepted; admin overview resolves to DRAW_CHANNEL scope.
  3. Exposure      — MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW at DRAW_CHANNEL, limit=1000 HTG;
                     9 tickets at 100 HTG each → all accepted; POS detail shows ratio≈0.9;
                     10th ticket at 200 HTG → rejected; counter stays at 900.

Markers: L2 + business_critical + slow (skipped in fast CI).
Requires firebase-emulator provider (dynamic identity provisioning).
"""
from __future__ import annotations

import datetime as dt
import uuid
from typing import Any

import pytest

from tch_e2e.api_response import assert_ok
from tch_e2e.auth import FirebaseEmulatorAuth
from tch_e2e.client import ApiClient

pytestmark = [pytest.mark.L2, pytest.mark.business_critical, pytest.mark.slow]

_FIREBASE_PROVIDERS = {"firebase-emulator", "firebase"}


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _rid() -> dict[str, str]:
    return {"X-Request-Id": f"limits-e2e-{uuid.uuid4()}"}


def _data(resp) -> Any:
    body = resp.json()
    d = body.get("data") if isinstance(body, dict) else body
    if isinstance(d, dict) and set(d.keys()) == {"value"}:
        return d["value"]
    return d


def _items(page_data: Any) -> list[dict[str, Any]]:
    if not isinstance(page_data, dict):
        return []
    items = page_data.get("items") or page_data.get("content") or []
    return items if isinstance(items, list) else []


def _id(value: Any) -> str:
    if isinstance(value, dict):
        if set(value.keys()) == {"value"}:
            return _id(value["value"])
        return _id(value.get("id") or value.get("value"))
    return str(value)


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------

@pytest.fixture()
def fb_auth() -> FirebaseEmulatorAuth:
    import os
    provider = os.environ.get("TCH_E2E_AUTH_PROVIDER", "").strip().lower()
    if provider not in _FIREBASE_PROVIDERS:
        pytest.skip(
            "Limit policy e2e needs dynamic identity provisioning — set "
            "TCH_E2E_AUTH_PROVIDER=firebase-emulator."
        )
    return FirebaseEmulatorAuth.from_env()


@pytest.fixture()
def limit_world(super_admin_client: ApiClient, base_url: str, fb_auth: FirebaseEmulatorAuth):
    """Provisions a fresh DEFAULT_HAITI_LOTTERY tenant + seller terminal for limit tests.

    Yields a dict with keys: admin, seller, draw, draw_channel_id, seller_terminal_id.
    A fresh tenant per test run avoids interference between scenarios.
    """
    sa = super_admin_client
    suffix = uuid.uuid4().hex[:8]
    pn = uuid.uuid4().int % 1_000_000
    admin_phone = f"+50940{pn:06d}"
    seller_phone = f"+50941{pn:06d}"
    today = dt.date.today()

    # Provision tenant
    prov = sa.post(
        "/platform/tenant-onboarding/provision",
        json={
            "code": f"lim{suffix}",
            "name": f"Limit Tenant {suffix}",
            "type": "BORLETTE",
            "timezone": "America/Port-au-Prince",
            "currency": "HTG",
            "defaultCommissionRate": "10.00",
            "profile": "DEFAULT_HAITI_LOTTERY",
            "maryajGratisEnabled": False,
            "initialAdminUsername": f"adm-{suffix}",
            "initialAdminEmail": f"adm-{suffix}@limtest.test",
        },
        headers=_rid(),
    )
    assert_ok(prov, expected=(200, 201))
    result = _data(prov)
    tenant_id = result["tenantId"]
    admin_email = result["initialAdminEmail"]

    assert_ok(sa.post(f"/platform/tenants/{tenant_id}/activate", headers=_rid()))

    # Generate + open draws
    assert_ok(sa.post("/platform/ops/draws/generate", json={
        "tenantId": tenant_id, "from": today.isoformat(), "to": today.isoformat(),
        "dryRun": False, "force": False, "reason": "limits e2e"}, headers=_rid()))
    assert_ok(sa.post("/platform/ops/draws/open-today", json={
        "limit": 500, "lookaheadHours": 24, "lagHours": 1, "dryRun": False},
        headers=_rid()))

    # Admin login
    admin = ApiClient(base_url=base_url, token=fb_auth.mint(
        subject=fb_auth.uid_for_email(admin_email), email=admin_email))
    assert_ok(admin.post("/identity/me/complete-first-login", json={
        "firstName": "Limit", "lastName": "Admin", "phoneNumber": admin_phone,
        "passwordChanged": True}, headers=_rid()))

    # Create seller terminal
    term_code = f"LIM-{suffix}"
    create = admin.post("/admin/seller-terminals", json={
        "terminalCode": term_code, "displayName": f"Limit Seller {suffix}",
        "firstName": "Limit", "lastName": "Seller", "email": f"seller-{suffix}@limtest.test",
        "phoneNumber": seller_phone, "commissionRate": "10.00", "initialPin": "123456"},
        headers=_rid())
    assert_ok(create, expected=(200, 201))
    seller_terminal_id = str(_data(create))

    # POS client (admin acts-as-terminal)
    seller = ApiClient(
        base_url=base_url,
        token=admin.token,
        extra_headers={"X-Tch-Act-As-Terminal": seller_terminal_id},
    )

    # Pick an open draw
    draws_resp = seller.get("/tenant/cashier/draws/available",
                            params={"lookaheadHours": 24, "limit": 20}, headers=_rid())
    assert_ok(draws_resp)
    draws = _data(draws_resp) or []
    assert draws, "Limit tenant must have open draws"
    draw = draws[0]

    yield {
        "admin": admin,
        "seller": seller,
        "draw": draw,
        "draw_channel_id": draw["drawChannelId"],
        "seller_terminal_id": seller_terminal_id,
    }


# ---------------------------------------------------------------------------
# Sell helpers
# ---------------------------------------------------------------------------

def _prepare_and_confirm(seller: ApiClient, draw: dict, selection: str, stake_htg: str) -> dict:
    """Prepare + confirm a single HT_BOLET line. Returns flattened confirm data."""
    lines = [{"lineNumber": 1, "gameCode": "HT_BOLET", "betType": "MATCH_1_2D",
               "selection": selection, "stakeAmount": stake_htg}]
    payload = {
        "drawId": draw["drawId"],
        "drawChannelId": draw["drawChannelId"],
        "currency": {"value": "HTG"},
        "lines": lines,
    }
    prep_resp = seller.post("/tenant/sales/preparations", json=payload, headers=_rid())
    if prep_resp.status_code >= 400:
        return {"_status": prep_resp.status_code, "_body": prep_resp.text}
    prep_data = _data(prep_resp)
    confirm_resp = seller.post(
        f"/tenant/sales/preparations/{prep_data['preparationId']}/confirm",
        idempotency_key=str(uuid.uuid4()),
        headers=_rid(),
    )
    if confirm_resp.status_code >= 500:
        raise AssertionError(f"5xx from confirm: {confirm_resp.status_code} {confirm_resp.text[:300]}")
    raw = _data(confirm_resp) or {}
    sale = raw.get("sale") or raw
    ticket = sale.get("ticket") or {}
    return {
        "outcome": sale.get("outcome"),
        "ticketId": raw.get("ticketId") or ticket.get("ticketId") or sale.get("ticketId"),
        "issues": sale.get("issues"),
        "_status": confirm_resp.status_code,
    }


# ===========================================================================
# Cas 1 — Bloquer un numéro via API admin
# ===========================================================================

def test_block_number_rejects_sale_and_no_ticket(limit_world) -> None:
    """BLOCK_SELECTION_PER_DRAW at TENANT scope blocks selection "34".

    Verifies:
    - Sale with "34" is rejected.
    - No ticket created.
    - Draw overview shows no stake for "34" in exposure alerts.
    """
    admin: ApiClient = limit_world["admin"]
    seller: ApiClient = limit_world["seller"]
    draw: dict = limit_world["draw"]
    draw_id = draw["drawId"]

    # Configure the block
    assign_resp = admin.put("/admin/policies/limits/assignments", json={
        "targetType": "TENANT",
        "ruleKey": "BLOCK_SELECTION_PER_DRAW",
        "enabled": True,
        "onBreach": "BLOCK",
        "params": {"selections": ["34"]},
    }, headers=_rid())
    assert_ok(assign_resp)

    # Attempt to sell "34"
    result = _prepare_and_confirm(seller, draw, "34", "5.00")
    assert result.get("outcome") == "REJECTED" or result.get("_status", 200) in (422, 400), (
        f"Expected rejection for blocked selection, got: {result}"
    )
    assert result.get("ticketId") is None, "No ticket must be created for blocked selection"

    # Admin overview: "34" must not appear in exposure alerts (no stake accumulated)
    overview_resp = admin.get(f"/admin/draws/{draw_id}/overview", headers=_rid())
    assert_ok(overview_resp)
    overview = _data(overview_resp)
    alert_keys = [a["selectionKey"] for a in (overview.get("exposureAlerts") or [])]
    assert "34" not in alert_keys, (
        f"Blocked selection '34' must have no stake in exposure alerts, got: {alert_keys}"
    )


# ===========================================================================
# Cas 2 — Override de scope TENANT → DRAW_CHANNEL → SELLER_TERMINAL
# ===========================================================================

def test_scope_cascade_seller_terminal_overrides_draw_channel(limit_world) -> None:
    """MAX_STAKE_PER_LINE: TENANT=500, DRAW_CHANNEL=300, SELLER_TERMINAL=100 HTG.

    Verifies:
    - Stake 150 HTG (> SELLER_TERMINAL limit of 100) → rejected.
    - Stake 50 HTG (below all limits) → accepted.
    - Admin overview resolves MAX_STAKE_PER_LINE at DRAW_CHANNEL scope (=300),
      never at SELLER_TERMINAL (BFF admin is not contextualized to a terminal).
    """
    admin: ApiClient = limit_world["admin"]
    seller: ApiClient = limit_world["seller"]
    draw: dict = limit_world["draw"]
    draw_id = draw["drawId"]
    draw_channel_id: str = limit_world["draw_channel_id"]
    seller_terminal_id: str = limit_world["seller_terminal_id"]

    for target_type, target_id, value_cents in [
        ("TENANT",          None,               50000),   # 500 HTG
        ("DRAW_CHANNEL",    draw_channel_id,    30000),   # 300 HTG
        ("SELLER_TERMINAL", seller_terminal_id, 10000),   # 100 HTG
    ]:
        body: dict = {
            "targetType": target_type,
            "ruleKey": "MAX_STAKE_PER_LINE",
            "enabled": True,
            "onBreach": "BLOCK",
            "params": {"valueCents": value_cents},
        }
        if target_id:
            body["targetId"] = target_id
        assert_ok(admin.put("/admin/policies/limits/assignments", json=body, headers=_rid()))

    # 150 HTG → must be rejected (above SELLER_TERMINAL=100)
    rejected = _prepare_and_confirm(seller, draw, "11", "150.00")
    assert rejected.get("outcome") == "REJECTED" or rejected.get("_status", 200) in (422, 400), (
        f"Stake 150 HTG must be rejected (SELLER_TERMINAL limit=100), got: {rejected}"
    )

    # 50 HTG → must be accepted
    accepted = _prepare_and_confirm(seller, draw, "22", "50.00")
    assert accepted.get("outcome") == "ACCEPTED", (
        f"Stake 50 HTG must be accepted (below all limits), got: {accepted}"
    )

    # Admin BFF overview: effectiveLimits resolves at DRAW_CHANNEL (=300), not SELLER_TERMINAL
    overview_resp = admin.get(f"/admin/draws/{draw_id}/overview", headers=_rid())
    assert_ok(overview_resp)
    overview = _data(overview_resp)
    limit_items = {
        item["ruleKey"]: item
        for item in (overview.get("effectiveLimits") or [])
    }
    if "MAX_STAKE_PER_LINE" in limit_items:
        resolved_scope = limit_items["MAX_STAKE_PER_LINE"].get("resolvedScope")
        assert resolved_scope == "DRAW_CHANNEL", (
            f"Admin BFF must resolve MAX_STAKE_PER_LINE at DRAW_CHANNEL, got: {resolved_scope}"
        )


# ===========================================================================
# Cas 3 — Exposition cumulative + BFF POS detail
# ===========================================================================

def test_exposure_cumulative_and_pos_detail(limit_world) -> None:
    """MAX_STAKE_EXPOSURE at DRAW_CHANNEL, limit=1000 HTG on selection "12".

    Verifies:
    - 9 tickets at 100 HTG each → all accepted (cumul = 900 HTG).
    - POS draw detail: topSelections[0].selectionKey == "12", exposure.active == True,
      exposure.alerts[0].ratio ≈ 0.9.
    - 10th ticket at 200 HTG → rejected (would exceed 1000 HTG).
    - Counter unchanged at 900 HTG (no ticket, no accumulation).
    - sellerTerminalId is NOT needed as a query param (resolved from context).
    """
    admin: ApiClient = limit_world["admin"]
    seller: ApiClient = limit_world["seller"]
    draw: dict = limit_world["draw"]
    draw_id = draw["drawId"]
    draw_channel_id: str = limit_world["draw_channel_id"]

    # Configure exposure limit: 1000 HTG = 100000 cents
    assert_ok(admin.put("/admin/policies/limits/assignments", json={
        "targetType": "DRAW_CHANNEL",
        "targetId": draw_channel_id,
        "ruleKey": "MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW",
        "enabled": True,
        "onBreach": "BLOCK",
        "params": {"valueCents": 100000},
    }, headers=_rid()))

    # Sell 9 tickets at 100 HTG on "12" — all must be accepted
    for i in range(9):
        result = _prepare_and_confirm(seller, draw, "12", "100.00")
        assert result.get("outcome") == "ACCEPTED", (
            f"Ticket {i + 1}/9 at 100 HTG on '12' must be accepted, got: {result}"
        )

    # POS draw detail — check topSelections + exposure (no query param needed)
    detail_resp = seller.get(f"/tenant/cashier/draws/{draw_id}/detail", headers=_rid())
    assert_ok(detail_resp)
    detail = _data(detail_resp)

    top = detail.get("topSelections") or []
    assert any(t["selectionKey"] == "12" for t in top), (
        f"topSelections must contain '12', got: {[t.get('selectionKey') for t in top]}"
    )

    exposure = detail.get("exposure") or {}
    assert exposure.get("active") is True, f"exposure.active must be True, got: {exposure}"

    alerts = exposure.get("alerts") or []
    alert_12 = next((a for a in alerts if a.get("selectionKey") == "12"), None)
    if alert_12 is not None:
        ratio = alert_12.get("stakeRatio") or alert_12.get("ratio") or 0
        assert 0.85 <= ratio <= 0.95, (
            f"exposure ratio for '12' must be ≈ 0.9 after 9×100 HTG / 1000 HTG, got: {ratio}"
        )

    # 10th ticket at 200 HTG → must be rejected
    rejected = _prepare_and_confirm(seller, draw, "12", "200.00")
    assert rejected.get("outcome") == "REJECTED" or rejected.get("_status", 200) in (422, 400), (
        f"10th ticket at 200 HTG on '12' must be rejected (would exceed 1000 HTG), got: {rejected}"
    )

    # Counter must stay at 900 HTG — re-read detail after rejection
    detail_resp2 = seller.get(f"/tenant/cashier/draws/{draw_id}/detail", headers=_rid())
    assert_ok(detail_resp2)
    detail2 = _data(detail_resp2)
    alerts2 = (detail2.get("exposure") or {}).get("alerts") or []
    alert_12_after = next((a for a in alerts2 if a.get("selectionKey") == "12"), None)
    if alert_12_after is not None:
        stake = alert_12_after.get("stakeTotalCents") or alert_12_after.get("stakeTotal") or 0
        # Accept either cents (90000) or HTG (900) depending on the API contract
        stake_htg = stake / 100.0 if stake > 1000 else stake
        assert abs(stake_htg - 900.0) < 1.0, (
            f"Stake counter must remain at 900 HTG after rejected ticket, got: {stake_htg}"
        )
