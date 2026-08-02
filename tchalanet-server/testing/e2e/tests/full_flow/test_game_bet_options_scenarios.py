"""E2E matrix for tenant game bet-options.

The scenario is intentionally data-driven: each case configures tenant-visible
commercial bet options, sells a deterministic line, and asserts the persisted
ticket snapshots used later by settlement and reprint flows. This keeps the
matrix reusable for additional pytest cases and future Locust validation.
"""
from __future__ import annotations

import datetime as dt
import os
import shutil
import subprocess
import tempfile
import time
import uuid
from dataclasses import dataclass
from decimal import Decimal
from typing import Any

import pytest

from tch_e2e.api_response import assert_ok
from tch_e2e.auth import FirebaseEmulatorAuth
from tch_e2e.business_day import draw_channel_id, draw_id
from tch_e2e.client import ApiClient

pytestmark = [pytest.mark.L2, pytest.mark.full_flow, pytest.mark.slow]

_FIREBASE_PROVIDERS = {"firebase-emulator", "firebase"}
_ALLOWED_OPTION_CODES = {
    "MARRIAGE_2D2D": [1, 2],
    "LOTTO3_3D": [1, 2, 3],
    "LOTTO4_PATTERN": [1, 2, 3, 4, 5],
    "LOTTO5_PATTERN": [1, 2, 3],
}


@dataclass(frozen=True)
class SellerRuntime:
    seller_terminal_id: str
    client: ApiClient


@dataclass(frozen=True)
class TenantRuntime:
    code: str
    tenant_id: str
    admin: ApiClient
    seller: SellerRuntime
    draw: dict[str, Any]


@dataclass(frozen=True)
class SaleCase:
    name: str
    game_code: str
    bet_type: str
    selection: str
    bet_option: int | None
    expected_policy: str
    expected_label: str | None
    expected_rules: tuple[str, ...]
    expected_print_label: str | None = None
    forbidden_print_tokens: tuple[str, ...] = ()


@pytest.fixture()
def fb_auth() -> FirebaseEmulatorAuth:
    provider = os.environ.get("TCH_E2E_AUTH_PROVIDER", "").strip().lower()
    if provider not in _FIREBASE_PROVIDERS:
        pytest.skip("Bet-option E2E needs TCH_E2E_AUTH_PROVIDER=firebase-emulator.")
    return FirebaseEmulatorAuth.from_env()


def test_tenant_game_bet_options_are_configured_sold_snapshotted_and_reprinted(
    super_admin_client: ApiClient,
    base_url: str,
    fb_auth: FirebaseEmulatorAuth,
) -> None:
    runtime = _provision_runtime(super_admin_client, base_url, fb_auth)

    _assert_default_catalog_is_filtered_by_bet_type(runtime.admin)

    _configure_explicit(
        runtime.admin,
        "HT_MARYAJ",
        "MARRIAGE_2D2D",
        [1, 2],
    )
    _assert_pos_options(runtime.seller.client, "HT_MARYAJ", "MARRIAGE_2D2D", "EXPLICIT_ONLY", [1, 2])
    maryaj_exact = _sell_and_assert_snapshot(
        runtime,
        SaleCase(
            "maryaj-exact",
            "HT_MARYAJ",
            "MARRIAGE_2D2D",
            "12-21",
            1,
            "EXPLICIT_ONLY",
            "Ordre exact",
            ("MARRIAGE_EXACT_ORDER",),
            expected_print_label="Ordre exact",
        ),
    )
    _sell_and_assert_snapshot(
        runtime,
        SaleCase(
            "maryaj-reverse",
            "HT_MARYAJ",
            "MARRIAGE_2D2D",
            "21-12",
            2,
            "EXPLICIT_ONLY",
            "Revers / Double",
            ("MARRIAGE_REVERSE_ALLOWED",),
            expected_print_label="Revers / Double",
        ),
    )

    before_reprint = _settlement_snapshot(runtime.admin, maryaj_exact)
    _configure_explicit(runtime.admin, "HT_MARYAJ", "MARRIAGE_2D2D", [2])
    _assert_pos_options(runtime.seller.client, "HT_MARYAJ", "MARRIAGE_2D2D", "EXPLICIT_ONLY", [2])
    after_reprint_pdf = _print_ticket_pdf(runtime.seller, maryaj_exact)
    assert after_reprint_pdf.startswith(b"%PDF")
    after_reprint = _settlement_snapshot(runtime.admin, maryaj_exact)
    assert after_reprint == before_reprint, "reprint must keep the original persisted snapshots"

    _configure_implicit(runtime.admin, "HT_MARYAJ", "MARRIAGE_2D2D", [1, 2])
    _assert_pos_options(runtime.seller.client, "HT_MARYAJ", "MARRIAGE_2D2D", "IMPLICIT_BEST_MATCH", [])
    _sell_and_assert_snapshot(
        runtime,
        SaleCase(
            "maryaj-implicit-best-match",
            "HT_MARYAJ",
            "MARRIAGE_2D2D",
            "12-21",
            None,
            "IMPLICIT_BEST_MATCH",
            None,
            ("MARRIAGE_EXACT_ORDER", "MARRIAGE_REVERSE_ALLOWED"),
            forbidden_print_tokens=("Ordre exact", "Revers / Double"),
        ),
    )

    _configure_explicit(runtime.admin, "HT_LOTO3", "LOTTO3_3D", [1, 2, 3])
    _assert_pos_options(runtime.seller.client, "HT_LOTO3", "LOTTO3_3D", "EXPLICIT_ONLY", [1, 2, 3])
    for case in (
        SaleCase("loto3-straight", "HT_LOTO3", "LOTTO3_3D", "112", 1, "EXPLICIT_ONLY", "Exact", ("LOTTO3_STRAIGHT",)),
        SaleCase("loto3-box-3-way", "HT_LOTO3", "LOTTO3_3D", "121", 2, "EXPLICIT_ONLY", "Désordre / Box", ("LOTTO3_BOX_3_WAY",)),
        SaleCase("loto3-box-6-way", "HT_LOTO3", "LOTTO3_3D", "123", 2, "EXPLICIT_ONLY", "Désordre / Box", ("LOTTO3_BOX_6_WAY",)),
        SaleCase(
            "loto3-exact-plus-box",
            "HT_LOTO3",
            "LOTTO3_3D",
            "123",
            3,
            "EXPLICIT_ONLY",
            "Exact + Permuté",
            ("LOTTO3_STRAIGHT", "LOTTO3_BOX_6_WAY"),
            expected_print_label="Exact + Permuté",
            forbidden_print_tokens=("LOTTO3_EXACT_PLUS_BOX",),
        ),
    ):
        _sell_and_assert_snapshot(runtime, case)

    _configure_implicit(runtime.admin, "HT_LOTO3", "LOTTO3_3D", [1, 2])
    _assert_pos_options(runtime.seller.client, "HT_LOTO3", "LOTTO3_3D", "IMPLICIT_BEST_MATCH", [])
    _sell_and_assert_snapshot(
        runtime,
        SaleCase(
            "loto3-implicit-exact-box",
            "HT_LOTO3",
            "LOTTO3_3D",
            "123",
            None,
            "IMPLICIT_BEST_MATCH",
            None,
            ("LOTTO3_STRAIGHT", "LOTTO3_BOX_6_WAY"),
            forbidden_print_tokens=("Exact", "Désordre / Box", "LOTTO3_STRAIGHT", "LOTTO3_BOX"),
        ),
    )

    _configure_explicit(runtime.admin, "HT_LOTO4", "LOTTO4_PATTERN", [1, 2, 3, 4, 5])
    _assert_pos_options(runtime.seller.client, "HT_LOTO4", "LOTTO4_PATTERN", "EXPLICIT_ONLY", [1, 2, 3, 4, 5])
    for case in (
        SaleCase("loto4-straight", "HT_LOTO4", "LOTTO4_PATTERN", "1234", 1, "EXPLICIT_ONLY", "Exact", ("LOTTO4_STRAIGHT",)),
        SaleCase("loto4-box-4-way", "HT_LOTO4", "LOTTO4_PATTERN", "1112", 2, "EXPLICIT_ONLY", "Désordre / Box", ("LOTTO4_BOX_4_WAY",)),
        SaleCase("loto4-box-6-way", "HT_LOTO4", "LOTTO4_PATTERN", "1122", 2, "EXPLICIT_ONLY", "Désordre / Box", ("LOTTO4_BOX_6_WAY",)),
        SaleCase("loto4-box-12-way", "HT_LOTO4", "LOTTO4_PATTERN", "1123", 2, "EXPLICIT_ONLY", "Désordre / Box", ("LOTTO4_BOX_12_WAY",)),
        SaleCase("loto4-box-24-way", "HT_LOTO4", "LOTTO4_PATTERN", "1234", 2, "EXPLICIT_ONLY", "Désordre / Box", ("LOTTO4_BOX_24_WAY",)),
        SaleCase("loto4-front-pair", "HT_LOTO4", "LOTTO4_PATTERN", "12", 3, "EXPLICIT_ONLY", "2 premiers chiffres", ("LOTTO4_FRONT_PAIR",)),
        SaleCase("loto4-back-pair", "HT_LOTO4", "LOTTO4_PATTERN", "34", 4, "EXPLICIT_ONLY", "2 derniers chiffres", ("LOTTO4_BACK_PAIR",)),
        SaleCase("loto4-exact-plus-box", "HT_LOTO4", "LOTTO4_PATTERN", "1234", 5, "EXPLICIT_ONLY", "Exact + Permuté", ("LOTTO4_STRAIGHT", "LOTTO4_BOX_24_WAY")),
    ):
        _sell_and_assert_snapshot(runtime, case)

    _configure_implicit(runtime.admin, "HT_LOTO4", "LOTTO4_PATTERN", [1, 2, 3, 4])
    _assert_pos_options(runtime.seller.client, "HT_LOTO4", "LOTTO4_PATTERN", "IMPLICIT_BEST_MATCH", [])
    _sell_and_assert_snapshot(
        runtime,
        SaleCase(
            "loto4-implicit",
            "HT_LOTO4",
            "LOTTO4_PATTERN",
            "1234",
            None,
            "IMPLICIT_BEST_MATCH",
            None,
            ("LOTTO4_STRAIGHT", "LOTTO4_BOX_24_WAY", "LOTTO4_FRONT_PAIR", "LOTTO4_BACK_PAIR"),
        ),
    )

    _configure_explicit(runtime.admin, "HT_LOTO5", "LOTTO5_PATTERN", [1, 2, 3])
    _assert_pos_options(runtime.seller.client, "HT_LOTO5", "LOTTO5_PATTERN", "EXPLICIT_ONLY", [1, 2, 3])
    for case in (
        SaleCase("loto5-lot1-lot2", "HT_LOTO5", "LOTTO5_PATTERN", "11221", 1, "EXPLICIT_ONLY", "1er lot + 2e lot", ("LOTTO5_LOT1_LOT2",)),
        SaleCase("loto5-lot1-lot3", "HT_LOTO5", "LOTTO5_PATTERN", "11225", 2, "EXPLICIT_ONLY", "1er lot + 3e lot", ("LOTTO5_LOT1_LOT3",)),
        SaleCase("loto5-mixed", "HT_LOTO5", "LOTTO5_PATTERN", "22125", 3, "EXPLICIT_ONLY", "Mixte 1er/2e/3e lot", ("LOTTO5_MIXED_1_2_3",)),
    ):
        _sell_and_assert_snapshot(runtime, case)

    _configure_explicit(runtime.admin, "HT_LOTO5", "LOTTO5_PATTERN", [1, 3], hidden=[3])
    _assert_pos_options(runtime.seller.client, "HT_LOTO5", "LOTTO5_PATTERN", "EXPLICIT_ONLY", [1])
    _assert_sale_rejected(runtime, "HT_LOTO5", "LOTTO5_PATTERN", "11225", 2)
    _assert_incompatible_option_rejected(runtime.admin, "HT_LOTO3", "LOTTO3_3D", 5)


def _rid() -> dict[str, str]:
    return {"X-Request-Id": f"bet-options-e2e-{uuid.uuid4()}"}


def _data(response) -> Any:
    try:
        return response.json().get("data")
    except Exception:
        return None


def _id(value: Any) -> str:
    if isinstance(value, dict):
        return _id(value.get("value") or value.get("id") or value.get("ticketId") or value.get("sellerTerminalId"))
    return str(value)


def _provision_runtime(sa: ApiClient, base_url: str, fb_auth: FirebaseEmulatorAuth) -> TenantRuntime:
    del fb_auth  # this scenario uses platform-admin tenant override after provisioning
    suffix = uuid.uuid4().hex[:8]
    tenant_code = f"betopts{suffix}".lower()
    admin_email = f"admin-{tenant_code}@bet-options.test"
    phone_seed = uuid.uuid4().int % 1_000_000
    prov = sa.post(
        "/platform/tenant-onboarding/provision",
        json={
            "code": tenant_code,
            "name": f"Bet Options {suffix}",
            "type": "BORLETTE",
            "timezone": "America/Port-au-Prince",
            "currency": "HTG",
            "defaultCommissionRate": "10.00",
            "profile": "DEFAULT_HAITI_LOTTERY",
            "maryajGratisEnabled": False,
            "initialAdminUsername": f"admin-{tenant_code}",
            "initialAdminEmail": admin_email,
        },
        headers=_rid(),
    )
    if prov.status_code in (200, 201):
        tenant_id = (_data(prov) or {})["tenantId"]
    elif prov.status_code == 422 and "Firebase user lookup failed" in prov.text:
        # Local firebase-emulator lookup can fail after the tenant/profile data has been created.
        # The scenario only needs a tenant-scoped admin surface, so continue through platform
        # override instead of depending on a dynamically-minted tenant-admin token.
        tenant_id = _lookup_tenant_id(sa, tenant_code)
    else:
        assert_ok(prov, expected=(200, 201))
        raise AssertionError("unreachable")

    admin = sa.with_tenant(tenant_id, "bet-options e2e tenant override")

    seller = _create_seller(admin, base_url, suffix, phone_seed)
    activated = sa.post(f"/platform/tenants/{tenant_id}/activate", headers=_rid())
    assert activated.status_code in (200, 204), activated.text
    draw = _open_one_draw(sa, tenant_code, tenant_id, admin)
    return TenantRuntime(tenant_code, tenant_id, admin, seller, draw)


def _lookup_tenant_id(sa: ApiClient, tenant_code: str) -> str:
    response = sa.get("/platform/tenants/by-code", params={"code": tenant_code}, headers=_rid())
    assert_ok(response)
    data = _data(response) or {}
    return _id(data.get("tenantId") or data.get("id"))


def _create_seller(admin: ApiClient, base_url: str, suffix: str, phone_seed: int) -> SellerRuntime:
    created = admin.post(
        "/admin/seller-terminals",
        json={
            "terminalCode": f"BETOPT-{suffix}",
            "displayName": f"Bet Options seller {suffix}",
            "firstName": "Bet",
            "lastName": "Seller",
            "email": f"seller-{suffix}@bet-options.test",
            "phoneNumber": f"+50948{phone_seed:06d}",
            "commissionRate": "10.00",
            "initialPin": "123456",
        },
        headers=_rid(),
    )
    assert_ok(created, expected=(200, 201))
    seller_terminal_id = _id(_data(created))
    return SellerRuntime(
        seller_terminal_id,
        ApiClient(
            base_url=base_url,
            token=admin.token,
            extra_headers={**admin.extra_headers, "X-Tch-Act-As-Terminal": seller_terminal_id},
        ),
    )


def _open_one_draw(sa: ApiClient, tenant_code: str, tenant_id: str, admin: ApiClient) -> dict[str, Any]:
    today = dt.date.today()
    generated = sa.post(
        "/platform/ops/draws/generate",
        json={
            "tenantCodes": [tenant_code],
            "from": today.isoformat(),
            "to": today.isoformat(),
            "dryRun": False,
            "force": False,
            "reason": "bet-options e2e draw",
        },
        headers=_rid(),
    )
    assert_ok(generated)
    draws = _eventually_draws(admin, today)
    draw = draws[0]
    now = dt.datetime.now(dt.timezone.utc).replace(microsecond=0)
    rescheduled = sa.with_tenant(tenant_id, "bet-options draw reschedule").post(
        f"/admin/draws/{_id(draw.get('drawId') or draw.get('id'))}/reschedule",
        json={
            "scheduledAt": (now + dt.timedelta(days=1)).isoformat().replace("+00:00", "Z"),
            "cutoffAt": (now + dt.timedelta(hours=23)).isoformat().replace("+00:00", "Z"),
            "reason": "bet-options e2e makes draw sellable",
            "force": True,
        },
        headers=_rid(),
    )
    assert_ok(rescheduled)
    open_resp = admin.post(
        "/admin/draws/lifecycle/open",
        json={"drawIds": [_id(_data(rescheduled).get("drawId") or _data(rescheduled).get("id"))], "reason": "bet-options e2e"},
        headers=_rid(),
    )
    assert_ok(open_resp)
    opened = _data(open_resp)
    return opened[0] if isinstance(opened, list) else opened


def _eventually_draws(admin: ApiClient, day: dt.date) -> list[dict[str, Any]]:
    deadline = time.monotonic() + 30
    last: list[dict[str, Any]] = []
    while time.monotonic() < deadline:
        listed = admin.get(
            "/admin/draws",
            params={"from": day.isoformat(), "to": day.isoformat(), "page": 0, "size": 100},
            headers=_rid(),
        )
        assert_ok(listed)
        data = _data(listed) or {}
        last = data.get("items") or data.get("content") or (data if isinstance(data, list) else [])
        if last:
            return last
        time.sleep(0.5)
    raise AssertionError(f"no generated draw visible for {day}: {last}")


def _assert_default_catalog_is_filtered_by_bet_type(admin: ApiClient) -> None:
    expected_by_game = {
        "HT_MARYAJ": ("MARRIAGE_2D2D", {1, 2}),
        "HT_LOTO3": ("LOTTO3_3D", {1, 2}),
        "HT_LOTO4": ("LOTTO4_PATTERN", {1, 2, 3, 4}),
        "HT_LOTO5": ("LOTTO5_PATTERN", {1, 2, 3}),
    }
    for game_code, (bet_type, expected_options) in expected_by_game.items():
        config = _get_bet_config(admin, game_code)
        row = _bet_type_row(config, bet_type)
        actual = {int(option["code"]) for option in row["options"]}
        assert actual == expected_options, f"{game_code}/{bet_type} should not expose incompatible global options"
        assert [option["displayOrder"] for option in row["options"]] == sorted(
            option["displayOrder"] for option in row["options"]
        )


def _get_bet_config(admin: ApiClient, game_code: str) -> dict[str, Any]:
    response = admin.get(f"/admin/games/{game_code}/bet-options", headers=_rid())
    assert_ok(response)
    return _data(response) or {}


def _bet_type_row(config: dict[str, Any], bet_type: str) -> dict[str, Any]:
    for row in config.get("betTypes") or []:
        if row.get("betType") == bet_type:
            return row
    raise AssertionError(f"missing bet type {bet_type} in {config}")


def _configure_explicit(
    admin: ApiClient,
    game_code: str,
    bet_type: str,
    enabled: list[int],
    *,
    hidden: list[int] | None = None,
) -> dict[str, Any]:
    return _configure(admin, game_code, bet_type, "EXPLICIT_ONLY", enabled, hidden=hidden)


def _configure_implicit(admin: ApiClient, game_code: str, bet_type: str, enabled: list[int]) -> dict[str, Any]:
    return _configure(admin, game_code, bet_type, "IMPLICIT_BEST_MATCH", enabled)


def _configure(
    admin: ApiClient,
    game_code: str,
    bet_type: str,
    policy: str,
    enabled: list[int],
    *,
    hidden: list[int] | None = None,
) -> dict[str, Any]:
    hidden = hidden or []
    config = _get_bet_config(admin, game_code)
    updated = []
    for row in config.get("betTypes") or []:
        if row.get("betType") != bet_type:
            updated.append(row)
            continue
        options = []
        seen_codes = set()
        for option in row.get("options") or []:
            code = int(option["code"])
            seen_codes.add(code)
            options.append(
                {
                    "code": code,
                    "enabled": code in enabled,
                    "visibleInPos": code in enabled and code not in hidden,
                    "displayOrder": enabled.index(code) + 1 if code in enabled else 100 + code,
                }
            )
        for code in _ALLOWED_OPTION_CODES.get(bet_type, []):
            if code in seen_codes or code not in enabled:
                continue
            options.append(
                {
                    "code": code,
                    "enabled": True,
                    "visibleInPos": code not in hidden,
                    "displayOrder": enabled.index(code) + 1,
                }
            )
        updated.append(
            {
                "betType": bet_type,
                "selectionPolicy": policy,
                "defaultOption": enabled[0],
                "options": options,
            }
        )
    saved = admin.patch(f"/admin/games/{game_code}/bet-options", json={"betTypes": updated}, headers=_rid())
    assert_ok(saved)
    saved_row = _bet_type_row(_data(saved) or {}, bet_type)
    assert saved_row["selectionPolicy"] == policy
    enabled_visible = [
        int(option["code"])
        for option in saved_row["options"]
        if option["enabled"] and option["visibleInPos"]
    ]
    assert enabled_visible == [code for code in enabled if code not in hidden]
    return saved_row


def _assert_pos_options(
    seller: ApiClient,
    game_code: str,
    bet_type: str,
    expected_policy: str,
    expected_codes: list[int],
) -> None:
    listed = seller.get("/tenant/cashier/games/available", headers=_rid())
    assert_ok(listed)
    rows = _data(listed) or []
    row = next((item for item in rows if item["gameCode"] == game_code and item["betType"] == bet_type), None)
    assert row is not None, f"POS should expose {game_code}/{bet_type}: {rows}"
    assert row["selectionPolicy"] == expected_policy
    assert [int(option["code"]) for option in row["options"]] == expected_codes
    if expected_policy == "IMPLICIT_BEST_MATCH":
        assert row["options"] == []


def _sell_and_assert_snapshot(runtime: TenantRuntime, case: SaleCase) -> str:
    ticket_id = _sell(runtime, case.game_code, case.bet_type, case.selection, case.bet_option)
    support = _settlement_support(runtime.admin, ticket_id)
    line = _only_customer_line(support, case.game_code, case.selection)
    assert line["selectionPolicySnapshot"] == case.expected_policy
    assert line.get("betOptionLabelSnapshot") == case.expected_label
    snapshot = line["settlementTermsSnapshot"]
    assert snapshot["selectionPolicy"] == case.expected_policy
    assert [term["ruleCode"] for term in snapshot["terms"]] == list(case.expected_rules)
    assert all(term.get("payoutRule") for term in snapshot["terms"])

    pdf = _print_ticket_pdf(runtime.seller, ticket_id)
    assert pdf.startswith(b"%PDF")
    text = _pdf_text_if_available(pdf)
    if text:
        if case.expected_print_label:
            assert case.expected_print_label in text
        for token in case.forbidden_print_tokens:
            assert token not in text
    return ticket_id


def _sell(runtime: TenantRuntime, game_code: str, bet_type: str, selection: str, bet_option: int | None) -> str:
    line: dict[str, Any] = {
        "lineNumber": 1,
        "gameCode": game_code,
        "betType": bet_type,
        "selection": selection,
        "stakeAmount": "1.00",
    }
    if bet_option is not None:
        line["betOption"] = bet_option
    prepared = runtime.seller.client.post(
        "/tenant/sales/preparations",
        json={
            "drawId": _id(runtime.draw.get("drawId") or runtime.draw.get("id")),
            "drawChannelId": draw_channel_id(runtime.draw),
            "currency": {"value": "HTG"},
            "lines": [line],
        },
        headers=_rid(),
    )
    assert_ok(prepared)
    preparation_id = (_data(prepared) or {})["preparationId"]
    sold = runtime.seller.client.post(
        f"/tenant/sales/preparations/{preparation_id}/confirm",
        idempotency_key=str(uuid.uuid4()),
        headers=_rid(),
    )
    assert_ok(sold, expected=(200, 201))
    data = _data(sold) or {}
    sale = data.get("sale") or data
    ticket = sale.get("ticket") or {}
    assert sale.get("outcome") == "ACCEPTED", sale
    return _id(data.get("ticketId") or sale.get("ticketId") or ticket.get("ticketId"))


def _assert_sale_rejected(
    runtime: TenantRuntime,
    game_code: str,
    bet_type: str,
    selection: str,
    bet_option: int | None,
) -> None:
    before = _ticket_count(runtime.admin, runtime.draw)
    response = runtime.seller.client.post(
        "/tenant/sales/preparations",
        json={
            "drawId": _id(runtime.draw.get("drawId") or runtime.draw.get("id")),
            "drawChannelId": draw_channel_id(runtime.draw),
            "currency": {"value": "HTG"},
            "lines": [
                {
                    "lineNumber": 1,
                    "gameCode": game_code,
                    "betType": bet_type,
                    "selection": selection,
                    "betOption": bet_option,
                    "stakeAmount": "1.00",
                }
            ],
        },
        headers=_rid(),
    )
    if response.status_code < 300:
        confirm = runtime.seller.client.post(
            f"/tenant/sales/preparations/{(_data(response) or {})['preparationId']}/confirm",
            idempotency_key=str(uuid.uuid4()),
            headers=_rid(),
        )
        assert confirm.status_code in (400, 403, 409, 422), confirm.text
    else:
        assert response.status_code in (400, 403, 409, 422), response.text
    assert _ticket_count(runtime.admin, runtime.draw) == before


def _assert_incompatible_option_rejected(admin: ApiClient, game_code: str, bet_type: str, incompatible: int) -> None:
    config = _get_bet_config(admin, game_code)
    updated = []
    for row in config.get("betTypes") or []:
        if row.get("betType") != bet_type:
            updated.append(row)
            continue
        updated.append(
            {
                "betType": bet_type,
                "selectionPolicy": "EXPLICIT_ONLY",
                "defaultOption": incompatible,
                "options": [
                    {"code": incompatible, "enabled": True, "visibleInPos": True, "displayOrder": 1}
                ],
            }
        )
    response = admin.patch(f"/admin/games/{game_code}/bet-options", json={"betTypes": updated}, headers=_rid())
    assert response.status_code in (400, 409, 422), response.text


def _ticket_count(admin: ApiClient, draw: dict[str, Any]) -> int:
    response = admin.get(
        "/tenant/tickets",
        params={"drawId": draw_id(draw), "page": 0, "size": 1},
        headers=_rid(),
    )
    assert_ok(response)
    data = _data(response) or {}
    return int(data.get("totalItems") or data.get("totalElements") or data.get("total") or len(data.get("items") or []))


def _settlement_support(admin: ApiClient, ticket_id: str) -> dict[str, Any]:
    response = admin.get(f"/admin/tickets/{ticket_id}/settlement-variants", headers=_rid())
    assert_ok(response)
    return _data(response) or {}


def _settlement_snapshot(admin: ApiClient, ticket_id: str) -> list[dict[str, Any]]:
    return [
        {
            "gameCode": line["gameCode"],
            "betType": line["betType"],
            "betOption": line.get("betOption"),
            "selectionPolicySnapshot": line.get("selectionPolicySnapshot"),
            "betOptionLabelSnapshot": line.get("betOptionLabelSnapshot"),
            "settlementTermsSnapshot": line.get("settlementTermsSnapshot"),
        }
        for line in (_settlement_support(admin, ticket_id).get("lines") or [])
    ]


def _only_customer_line(support: dict[str, Any], game_code: str, selection: str) -> dict[str, Any]:
    lines = [
        line
        for line in support.get("lines") or []
        if line.get("gameCode") == game_code and str(line.get("selection") or "") == selection.replace("-", "")
    ]
    if not lines:
        lines = [line for line in support.get("lines") or [] if line.get("gameCode") == game_code]
    assert len(lines) == 1, f"expected one support line for {game_code}/{selection}: {support}"
    return lines[0]


def _print_ticket_pdf(seller: SellerRuntime, ticket_id: str) -> bytes:
    response = seller.client.post(
        f"/tenant/cashier/tickets/{ticket_id}/print",
        json={
            "sellerTerminalId": seller.seller_terminal_id,
            "terminalId": seller.seller_terminal_id,
            "printOptionsRequest": {"outputFormat": "PDF", "paperSize": "RECEIPT_80MM"},
            "recordPrint": True,
            "reprintReason": "bet-options e2e reprint snapshot validation",
            "deliveryOptions": ["RETURN_FILE"],
        },
        headers=_rid(),
    )
    assert_ok(response)
    return response.content


def _pdf_text_if_available(pdf: bytes) -> str | None:
    if not shutil.which("pdftotext"):
        return None
    with tempfile.TemporaryDirectory(prefix="tch-bet-options-") as tmp:
        pdf_path = os.path.join(tmp, "ticket.pdf")
        txt_path = os.path.join(tmp, "ticket.txt")
        with open(pdf_path, "wb") as handle:
            handle.write(pdf)
        subprocess.run(["pdftotext", pdf_path, txt_path], check=True)
        with open(txt_path, "r", encoding="utf-8") as handle:
            return handle.read()
