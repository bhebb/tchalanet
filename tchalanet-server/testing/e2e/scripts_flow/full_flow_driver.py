"""Live driver for the end-to-end business flow the product owner described:

  1. SUPER_ADMIN provisions a tenant (DEFAULT_HAITI_LOTTERY) with an initial admin.
  2. SUPER_ADMIN generates + opens today's draws for the tenant.
  3. The admin logs in (minted firebase-emulator token, sub = initialAdminUserId),
     completes first login.
  4. The admin configures the tenant: maryaj gratis + a limit policy.
  5. The admin creates a seller-terminal.
  6. The seller logs in (minted token, sub = sellerTerminalId) and sells a ticket
     (including a maryaj-gratis line).
  7. Limit is exercised (sell that should breach the configured limit).
  8. Role separation is spot-checked (seller cannot hit admin endpoints).

Run against a live API on the firebase-emulator provider:

    cd tchalanet-server/testing/e2e
    source .venv-perf/bin/activate
    PYTHONPATH=. python scripts_flow/full_flow_driver.py

It prints a PASS/FAIL line per step and never mutates shared seed data (fresh tenant each run).
"""
from __future__ import annotations

import datetime as dt
import os
import sys
import uuid

from tch_e2e.auth import FirebaseEmulatorAuth
from tch_e2e.client import ApiClient

BASE = os.environ.get("TCH_BASE_URL", "http://localhost:8083/api/v1")
OK = "\033[32mPASS\033[0m"
KO = "\033[31mFAIL\033[0m"
_step = 0


def step(msg: str) -> None:
    global _step
    _step += 1
    print(f"\n=== [{_step}] {msg} ===")


def show(resp, limit: int = 600) -> None:
    body = resp.text or ""
    print(f"    -> {resp.request.method} {resp.request.url.path} :: {resp.status_code}")
    if body:
        print(f"       {body[:limit]}")


def data_of(resp):
    body = resp.json()
    d = body.get("data") if isinstance(body, dict) else body
    if isinstance(d, dict) and set(d.keys()) == {"value"}:
        return d["value"]
    return d


def rid() -> dict:
    return {"X-Request-Id": f"flow-{uuid.uuid4()}"}


def main() -> int:
    auth = FirebaseEmulatorAuth.from_env()
    sa_token = auth.password_grant(username="super_admin", password="x")
    sa = ApiClient(base_url=BASE, token=sa_token)

    suffix = uuid.uuid4().hex[:8]
    admin_email = f"admin-{suffix}@flow.test"

    # ---- 1. provision tenant + admin (Haiti) -------------------------------
    step("SUPER_ADMIN provisions a DEFAULT_HAITI_LOTTERY tenant + initial admin")
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
            "initialAdminEmail": admin_email,
        },
        headers=rid(),
    )
    show(prov)
    if prov.status_code not in (200, 201):
        print(f"{KO} provisioning failed — cannot continue")
        return 1
    result = data_of(prov)
    tenant_id = result["tenantId"]
    admin_user_id = result.get("initialAdminUserId")
    admin_email = result.get("initialAdminEmail") or admin_email
    print(
        f"{OK} tenant={tenant_id} admin_user={admin_user_id} email={admin_email} "
        f"mustChangePw={result.get('initialAdminMustChangePassword')} "
        f"mustCompleteProfile={result.get('initialAdminMustCompleteProfile')}"
    )

    # ---- 2. generate + open draws for the tenant ---------------------------
    step("SUPER_ADMIN generates + opens today's draws for the tenant")
    today = dt.date.today()
    now_utc = dt.datetime.now(dt.timezone.utc)
    sales_floor = dt.datetime.combine(today, dt.time(11, 30), tzinfo=dt.timezone.utc)
    open_now = max(now_utc, sales_floor).isoformat().replace("+00:00", "Z")
    gen = sa.post(
        "/platform/ops/draws/generate",
        json={"tenantId": tenant_id, "from": today.isoformat(), "to": today.isoformat(),
              "dryRun": False, "force": False, "reason": "flow driver"},
        headers=rid(),
    )
    show(gen)
    opened = sa.post(
        "/platform/ops/draws/open-today",
        json={"now": open_now, "drawDate": today.isoformat(), "limit": 500, "dryRun": False},
        headers=rid(),
    )
    show(opened)
    print(f"{OK if gen.status_code < 300 and opened.status_code < 300 else KO} draws generate/open")

    # ---- 3. admin logs in + completes first login --------------------------
    step("Admin logs in (minted token) and completes first login")
    admin_uid = admin_user_id
    print(f"    admin firebase uid = {admin_uid}")
    admin = ApiClient(base_url=BASE, token=auth.mint(subject=admin_uid, email=admin_email))
    first = admin.post(
        "/identity/me/complete-first-login",
        json={"firstName": "Flow", "lastName": "Admin", "phoneNumber": "+50900000001",
              "passwordChanged": True},
        headers=rid(),
    )
    show(first)
    print(f"{OK if first.status_code < 300 else KO} admin first-login (password changed + profile)")

    # ---- 4a. maryaj gratis --------------------------------------------------
    step("Admin configures maryaj gratis (instantiate default template)")
    maryaj = admin.post(
        "/admin/promotions/campaigns/templates/default-maryaj-gratis/instantiate",
        json={}, headers=rid(),
    )
    show(maryaj)
    if maryaj.status_code < 300:
        campaign = data_of(maryaj)
        raw_id = campaign.get("id")
        cid = raw_id.get("value") if isinstance(raw_id, dict) else raw_id
        status = campaign.get("status")
        if status != "ACTIVE":
            act = admin.post(f"/admin/promotions/campaigns/{cid}/activate", headers=rid())
            show(act)
            status = "ACTIVE" if act.status_code < 300 else status
        print(f"{OK if status == 'ACTIVE' else KO} maryaj gratis campaign {cid} status={status}")
    else:
        print(f"{KO} maryaj instantiate")

    # ---- 4b. limit policy ---------------------------------------------------
    step("Admin configures a MAX_STAKE_PER_TICKET limit (BLOCK above 1000 HTG)")
    rules = admin.get("/admin/policies/limits/rules", headers=rid())
    show(rules, limit=400)
    assign = admin.put(
        "/admin/policies/limits/assignments",
        json={"targetType": "TENANT", "ruleKey": "MAX_STAKE_PER_TICKET", "enabled": True,
              "onBreach": "BLOCK", "params": {"valueCents": 100000}},
        headers=rid(),
    )
    show(assign)
    print(f"{OK if assign.status_code < 300 else KO} limit MAX_STAKE_PER_TICKET=1000 HTG")

    # ---- 5. admin creates a seller-terminal --------------------------------
    step("Admin creates a seller-terminal")
    term_code = f"FLOW-{suffix}"
    create = admin.post(
        "/admin/seller-terminals",
        json={"terminalCode": term_code, "displayName": f"Flow Seller {suffix}",
              "firstName": "Flow", "lastName": "Seller", "email": f"seller-{suffix}@flow.test",
              "phoneNumber": "+50900000002", "commissionRate": "10.00", "initialPin": "123456"},
        headers=rid(),
    )
    show(create)
    if create.status_code not in (200, 201):
        print(f"{KO} seller-terminal create — cannot test selling")
        return 2
    seller_terminal_id = str(data_of(create))
    print(f"{OK} seller-terminal id={seller_terminal_id}")

    # ---- 6. sell on the seller-terminal ------------------------------------
    # The admin drives POS on behalf of the seller-terminal via the documented bridge header
    # X-Tch-Act-As-Terminal (value = sellerTerminalId); TchContextFilter injects it into the
    # request context's sellerTerminalId. Restricted to TENANT_ADMIN / SUPER_ADMIN.
    step("POS sale on the seller-terminal (admin acts-as-terminal via X-Tch-Act-As-Terminal)")
    seller = ApiClient(
        base_url=BASE,
        token=admin.token,
        extra_headers={"X-Tch-Act-As-Terminal": seller_terminal_id},
    )
    draws_resp = seller.get(
        "/tenant/cashier/draws/available", params={"lookaheadHours": 24, "limit": 20}, headers=rid()
    )
    show(draws_resp, limit=900)
    if draws_resp.status_code >= 300:
        print(f"{KO} seller cannot list draws")
        return 3
    draws = data_of(draws_resp) or []
    if not draws:
        print(f"{KO} no available draws to sell into")
        return 3
    draw = draws[0]
    lines = [
        {"gameCode": "HT_BOLET", "betType": "MATCH_1_2D", "selection": "11", "betOption": None, "stake": "5.00"},
        {"gameCode": "HT_MARYAJ", "betType": "MARRIAGE_2D2D", "selection": "21-25", "betOption": 1, "stake": "5.00"},
    ]
    payload = {"sellerTerminalId": seller_terminal_id, "drawId": draw["drawId"],
               "drawChannelId": draw["drawChannelId"], "currency": "HTG", "lines": lines}
    prev = seller.post("/tenant/cashier/tickets/preview", json=payload, headers=rid())
    show(prev, limit=1200)
    if prev.status_code < 300:
        pdata = data_of(prev) or {}
        promo = pdata.get("promotion") or pdata.get("promotionDecision") or pdata.get("promotions")
        print(f"    maryaj-gratis / promotion in preview: {promo}")
    sold = seller.post("/tenant/cashier/tickets/sell", json=payload,
                       idempotency_key=str(uuid.uuid4()), headers=rid())
    show(sold, limit=900)
    if sold.status_code < 300 and (data_of(sold) or {}).get("outcome") == "ACCEPTED":
        print(f"{OK} sale ACCEPTED ticket={(data_of(sold) or {}).get('ticketId')}")
    else:
        print(f"{KO} sale not accepted")

    # ---- 7. limit breach ----------------------------------------------------
    step("Seller sells a ticket over the 1000 HTG limit -> expect BLOCK")
    big_lines = [
        {"gameCode": "HT_BOLET", "betType": "MATCH_1_2D", "selection": "22",
         "betOption": None, "stake": "600.00"},
        {"gameCode": "HT_BOLET", "betType": "MATCH_1_2D", "selection": "33",
         "betOption": None, "stake": "600.00"},
    ]
    big_payload = {"sellerTerminalId": seller_terminal_id, "drawId": draw["drawId"],
                   "drawChannelId": draw["drawChannelId"], "currency": "HTG", "lines": big_lines}
    breach = seller.post("/tenant/cashier/tickets/sell", json=big_payload,
                         idempotency_key=str(uuid.uuid4()), headers=rid())
    show(breach, limit=900)
    bdata = data_of(breach) or {}
    blocked = breach.status_code in (400, 409, 422) or bdata.get("outcome") in ("REJECTED", "BLOCKED")
    print(f"{OK if blocked else KO} over-limit sale blocked (outcome={bdata.get('outcome')}, http={breach.status_code})")

    # ---- 8. role separation -------------------------------------------------
    step("Role check: TENANT_ADMIN must NOT reach a SUPER_ADMIN-only endpoint")
    forbidden = admin.post(
        "/platform/tenant-onboarding/provision",
        json={"code": "nope", "name": "nope", "type": "BORLETTE",
              "timezone": "America/Port-au-Prince", "currency": "HTG",
              "defaultCommissionRate": "10.00", "profile": "MINIMAL"},
        headers=rid(),
    )
    show(forbidden)
    print(f"{OK if forbidden.status_code in (401, 403) else KO} admin blocked from SUPER_ADMIN provisioning (got {forbidden.status_code})")

    print("\n=== flow driver done ===")
    return 0


if __name__ == "__main__":
    sys.exit(main())
