"""End-to-end business flow, one role at a time (firebase-emulator only).

Mirrors the product-owner flow:

  SUPER_ADMIN provisions a DEFAULT_HAITI_LOTTERY tenant + initial admin, then generates and
  opens today's draws. The admin logs in (minted emulator token), completes first login,
  configures maryaj gratis + a stake limit, and creates a seller-terminal. A POS sale is then
  placed on that seller-terminal (admin acts-as-terminal), an over-limit sale is rejected, and
  role separation is asserted (a TENANT_ADMIN cannot reach a SUPER_ADMIN-only endpoint).

Requires the API + Firebase Auth emulator on the firebase-emulator provider — the flow provisions
login identities, which only Firebase can do. Under any other provider the test skips cleanly.

A fresh tenant/seller-terminal is created per run, so the test never mutates shared seed data.
"""
from __future__ import annotations

import datetime as dt
import os
import uuid

import pytest

from tch_e2e.api_response import assert_ok
from tch_e2e.auth import FirebaseEmulatorAuth
from tch_e2e.client import ApiClient

pytestmark = [pytest.mark.L2, pytest.mark.full_flow]

_FIREBASE_PROVIDERS = {"firebase-emulator", "firebase"}


def _rid() -> dict:
    return {"X-Request-Id": f"flow-{uuid.uuid4()}"}


def _data(resp):
    body = resp.json()
    d = body.get("data") if isinstance(body, dict) else body
    if isinstance(d, dict) and set(d.keys()) == {"value"}:
        return d["value"]
    return d


@pytest.fixture()
def fb_auth() -> FirebaseEmulatorAuth:
    provider = os.environ.get("TCH_E2E_AUTH_PROVIDER", "").strip().lower()
    if provider not in _FIREBASE_PROVIDERS:
        pytest.skip(
            "Full provision→sell flow needs identity provisioning — set "
            "TCH_E2E_AUTH_PROVIDER=firebase-emulator (API + emulator on firebase-emulator)."
        )
    return FirebaseEmulatorAuth.from_env()


@pytest.mark.L2
@pytest.mark.full_flow
def test_provision_configure_and_sell(
    super_admin_client: ApiClient, base_url: str, fb_auth: FirebaseEmulatorAuth
) -> None:
    sa = super_admin_client
    suffix = uuid.uuid4().hex[:8]
    # Phones are globally unique in app_user — derive fresh ones per run to avoid collisions.
    pn = uuid.uuid4().int % 1_000_000
    admin_phone = f"+50930{pn:06d}"
    seller_phone = f"+50931{pn:06d}"

    # 1. SUPER_ADMIN provisions a DEFAULT_HAITI_LOTTERY tenant + initial admin ------------
    prov = sa.post(
        "/platform/tenant-onboarding/provision",
        json={
            "code": f"flow{suffix}",
            "name": f"Flow Tenant {suffix}",
            "type": "BORLETTE",
            "timezone": "America/Port-au-Prince",
            "currency": "HTG",
            "defaultCommissionRate": "10.00",
            "profile": "DEFAULT_HAITI_LOTTERY",
            "maryajGratisEnabled": True,
            "initialAdminUsername": f"admin-{suffix}",
            "initialAdminEmail": f"admin-{suffix}@flow.test",
        },
        headers=_rid(),
    )
    assert_ok(prov, expected=(200, 201))
    result = _data(prov)
    tenant_id = result["tenantId"]
    admin_email = result["initialAdminEmail"]
    assert result["profile"] == "DEFAULT_HAITI_LOTTERY"
    assert result["initialAdminUserId"]

    # Provisioning deliberately creates a DRAFT tenant. Sales require ACTIVE.
    activated = sa.post(f"/platform/tenants/{tenant_id}/activate", headers=_rid())
    assert_ok(activated, expected=(200, 204))

    # 2. SUPER_ADMIN generates + opens today's draws --------------------------------------
    today = dt.date.today()
    assert_ok(sa.post("/platform/ops/draws/generate", json={
        "tenantId": tenant_id, "from": today.isoformat(), "to": today.isoformat(),
        "dryRun": False, "force": False, "reason": "full_flow e2e"}, headers=_rid()))
    assert_ok(sa.post("/platform/ops/draws/open-today", json={
        "limit": 500, "lookaheadHours": 24, "lagHours": 1, "dryRun": False},
        headers=_rid()))

    # 3. Admin logs in (minted token) and completes first login ---------------------------
    admin = ApiClient(base_url=base_url, token=fb_auth.mint(
        subject=fb_auth.uid_for_email(admin_email), email=admin_email))
    first = admin.post("/identity/me/complete-first-login", json={
        "firstName": "Flow", "lastName": "Admin", "phoneNumber": admin_phone,
        "passwordChanged": True}, headers=_rid())
    assert_ok(first)
    assert _data(first)["mustChangePassword"] is False

    # 4. Admin configures maryaj gratis ---------------------------------------------------
    maryaj = admin.post(
        "/admin/promotions/campaigns/templates/default-maryaj-gratis/instantiate",
        json={}, headers=_rid())
    assert_ok(maryaj)
    assert _data(maryaj)["status"] == "ACTIVE"

    # 5. Admin configures a stake limit (BLOCK above 1000 HTG per ticket) -----------------
    assign = admin.put("/admin/policies/limits/assignments", json={
        "targetType": "TENANT", "ruleKey": "MAX_STAKE_PER_TICKET", "enabled": True,
        "onBreach": "BLOCK", "params": {"valueCents": 100000}}, headers=_rid())
    assert_ok(assign)

    # 6. Admin creates a seller-terminal --------------------------------------------------
    term_code = f"FLOW-{suffix}"
    create = admin.post("/admin/seller-terminals", json={
        "terminalCode": term_code, "displayName": f"Flow Seller {suffix}",
        "firstName": "Flow", "lastName": "Seller", "email": f"seller-{suffix}@flow.test",
        "phoneNumber": seller_phone, "commissionRate": "10.00", "initialPin": "123456"},
        headers=_rid())
    assert_ok(create, expected=(200, 201))
    seller_terminal_id = str(_data(create))

    # 7. POS sale on the seller-terminal (admin acts-as-terminal via bridge header) -------
    seller = ApiClient(base_url=base_url, token=admin.token,
                       extra_headers={"X-Tch-Act-As-Terminal": seller_terminal_id})
    draws_resp = seller.get("/tenant/cashier/draws/available",
                            params={"lookaheadHours": 24, "limit": 20}, headers=_rid())
    assert_ok(draws_resp)
    draws = _data(draws_resp) or []
    assert draws, "DEFAULT_HAITI_LOTTERY tenant must have open draws to sell into"
    draw = draws[0]

    def payload(lines):
        prepared_lines = []
        for index, line in enumerate(lines, start=1):
            prepared = {
                "lineNumber": index,
                "gameCode": line["gameCode"],
                "betType": line["betType"],
                "selection": line["selection"],
                "stakeAmount": line.get("stakeAmount", line.get("stake")),
            }
            if line.get("betOption") is not None:
                prepared["betOption"] = line["betOption"]
            prepared_lines.append(prepared)
        return {"drawId": draw["drawId"], "drawChannelId": draw["drawChannelId"],
                "currency": {"value": "HTG"}, "lines": prepared_lines}

    def confirm(preparation_id: str):
        resp = seller.post(
            f"/tenant/sales/preparations/{preparation_id}/confirm",
            idempotency_key=str(uuid.uuid4()),
            headers=_rid(),
        )
        if resp.status_code >= 300:
            return resp, _data(resp) or {}
        data = _data(resp) or {}
        sale = data.get("sale") or data
        ticket = sale.get("ticket") or {}
        return resp, {
            "outcome": sale.get("outcome"),
            "ticketId": data.get("ticketId") or ticket.get("ticketId") or sale.get("ticketId"),
            "ticketCode": ticket.get("ticketCode") or sale.get("ticketCode"),
            "issues": sale.get("issues"),
        }

    ok_lines = [
        {"gameCode": "HT_BOLET", "betType": "MATCH_1_2D", "selection": "11",
         "betOption": None, "stake": "5.00"},
    ]
    prev = seller.post("/tenant/sales/preparations", json=payload(ok_lines), headers=_rid())
    assert_ok(prev)
    assert _data(prev)["status"] == "DRAFT"

    sold, sold_data = confirm(_data(prev)["preparationId"])
    assert_ok(sold, expected=(200, 201))
    assert sold_data["outcome"] == "ACCEPTED"
    assert sold_data["ticketId"]

    # 8. Over-limit sale is rejected ------------------------------------------------------
    big_lines = [
        {"gameCode": "HT_BOLET", "betType": "MATCH_1_2D", "selection": "22",
         "betOption": None, "stake": "600.00"},
        {"gameCode": "HT_BOLET", "betType": "MATCH_1_2D", "selection": "33",
         "betOption": None, "stake": "600.00"},
    ]
    breach_prepare = seller.post("/tenant/sales/preparations", json=payload(big_lines), headers=_rid())
    if breach_prepare.status_code < 300:
        breach, bdata = confirm(_data(breach_prepare)["preparationId"])
    else:
        breach, bdata = breach_prepare, _data(breach_prepare) or {}
    assert breach.status_code in (400, 403, 409, 422) or bdata.get("outcome") in ("REJECTED", "BLOCKED"), (
        f"over-limit sale should be rejected, got http={breach.status_code} data={bdata}")

    # 9. Role separation: TENANT_ADMIN cannot reach a SUPER_ADMIN-only endpoint -----------
    forbidden = admin.post("/platform/tenant-onboarding/provision", json={
        "code": f"forbidden{suffix}", "name": f"Forbidden {suffix}", "type": "BORLETTE",
        "timezone": "America/Port-au-Prince", "currency": "HTG",
        "defaultCommissionRate": "10.00", "profile": "MINIMAL",
        "initialAdminUsername": f"forbidden-{suffix}",
        "initialAdminEmail": f"forbidden-{suffix}@flow.test"}, headers=_rid())
    assert forbidden.status_code in (401, 403), (
        f"TENANT_ADMIN must be denied SUPER_ADMIN provisioning, got {forbidden.status_code}")
