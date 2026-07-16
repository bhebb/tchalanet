"""Business-day happy-path scenario for stats/report validation.

This is deliberately a scenario harness, not a one-off smoke test:

1. provision mixed tenants (Maryaj gratis implicit/explicit/disabled, limits, multiple sellers,
   one seller with commission + bolet odds override);
2. select one draw for every active DEFAULT_HAITI_LOTTERY draw channel and force it sellable;
3. sell a documented deterministic basket on every draw, distributed across seller terminals;
4. enter manual results with known winning selections;
5. let scheduled apply/settle process the manual results, or force apply in fast-run mode, then
   validate the report/KPI data that feeds exports.

The reusable plan/payload definitions live in ``tch_e2e.business_day`` so a future Locust user can
drive the same sale mix without importing pytest.
"""
from __future__ import annotations

import datetime as dt
import os
import time
import uuid
from dataclasses import dataclass
from decimal import Decimal
from typing import Any

import pytest

from tch_e2e.api_response import assert_ok
from tch_e2e.auth import FirebaseEmulatorAuth
from tch_e2e.business_day import (
    ManualResultPlan,
    SellerTerminalPlan,
    TenantScenarioPlan,
    default_tenant_plans,
    draw_channel_id,
    draw_id,
    exposure_limit_probe_ticket,
    sale_payload,
    short_ticket,
    ticket_basket,
    ticket_scenario,
)
from tch_e2e.client import ApiClient

pytestmark = [pytest.mark.L2, pytest.mark.full_flow, pytest.mark.slow]

_FIREBASE_PROVIDERS = {"firebase-emulator", "firebase"}
_DEFAULT_START = dt.date(2026, 7, 9)


@dataclass(frozen=True)
class SellerRuntime:
    plan: SellerTerminalPlan
    seller_terminal_id: str
    client: ApiClient


@dataclass(frozen=True)
class TenantRuntime:
    plan: TenantScenarioPlan
    code: str
    tenant_id: str
    admin: ApiClient
    sellers: tuple[SellerRuntime, ...]


@dataclass
class ExpectedTenantTotals:
    tickets: int = 0
    gross_sales: Decimal = Decimal("0.00")
    promotion_lines: int = 0
    winning_floor: Decimal = Decimal("0.00")
    auto_promo_winning_ceiling: Decimal = Decimal("0.00")
    by_seller: dict[str, "ExpectedTenantTotals"] | None = None

    @property
    def winning_ceiling(self) -> Decimal:
        return self.winning_floor + self.auto_promo_winning_ceiling

    def seller(self, seller_terminal_id: str) -> "ExpectedTenantTotals":
        if self.by_seller is None:
            self.by_seller = {}
        return self.by_seller.setdefault(seller_terminal_id, ExpectedTenantTotals())


@dataclass(frozen=True)
class ResultFlowTiming:
    manual_record_seconds: float
    apply_mode: str
    apply_launch_seconds: float | None
    report_settle_seconds: float


@dataclass(frozen=True)
class TenantRunResult:
    runtime: TenantRuntime
    draws: tuple[dict[str, Any], ...]
    expected: ExpectedTenantTotals


@pytest.fixture()
def fb_auth() -> FirebaseEmulatorAuth:
    provider = os.environ.get("TCH_E2E_AUTH_PROVIDER", "").strip().lower()
    if provider not in _FIREBASE_PROVIDERS:
        pytest.skip(
            "Business-day scenario needs dynamic identity provisioning; set "
            "TCH_E2E_AUTH_PROVIDER=firebase-emulator."
        )
    return FirebaseEmulatorAuth.from_env()


def _rid() -> dict[str, str]:
    return {"X-Request-Id": f"business-day-{uuid.uuid4()}"}


def _data(resp):
    body = resp.json()
    data = body.get("data") if isinstance(body, dict) else body
    if isinstance(data, dict) and set(data.keys()) == {"value"}:
        return data["value"]
    return data


def _items(page_data: Any) -> list[dict[str, Any]]:
    if not isinstance(page_data, dict):
        return []
    items = page_data.get("items") or page_data.get("content") or []
    return items if isinstance(items, list) else []


def _page_total(page_data: Any) -> int:
    if not isinstance(page_data, dict):
        return 0
    if "totalElements" in page_data:
        return int(page_data["totalElements"])
    return len(_items(page_data))


def _id(value: Any) -> str:
    if isinstance(value, dict):
        if set(value.keys()) == {"value"}:
            return _id(value["value"])
        return _id(value.get("id") or value.get("value"))
    return str(value)


def _env_int(name: str, default: int) -> int:
    raw = os.environ.get(name, "").strip()
    return int(raw) if raw else default


def _result_apply_mode() -> str:
    raw = os.environ.get("TCH_E2E_RESULT_APPLY_MODE", "force").strip().lower()
    allowed = {"force", "scheduler", "scheduler_then_force"}
    if raw not in allowed:
        raise AssertionError(f"TCH_E2E_RESULT_APPLY_MODE must be one of {sorted(allowed)}, got {raw!r}")
    return raw


def _catalog_mutation_allowed() -> bool:
    raw = os.environ.get("TCH_E2E_ALLOW_CATALOG_MUTATION", "").strip().lower()
    return raw in {"1", "true", "yes"}


def _start_date() -> dt.date:
    raw = os.environ.get("TCH_E2E_BUSINESS_DAY_START", "").strip()
    return dt.date.fromisoformat(raw) if raw else _DEFAULT_START


def _decimal(value: Any) -> Decimal:
    if isinstance(value, dict):
        value = value.get("amount") or value.get("value")
    return Decimal(str(value or "0")).quantize(Decimal("0.01"))


def _provision_tenant(
    sa: ApiClient,
    base_url: str,
    fb_auth: FirebaseEmulatorAuth,
    plan: TenantScenarioPlan,
) -> TenantRuntime:
    suffix = uuid.uuid4().hex[:8]
    tenant_code = f"bd{plan.key}{suffix}".lower()
    admin_email = f"admin-{tenant_code}@business-day.test"
    phone_seed = uuid.uuid4().int % 1_000_000

    prov = sa.post(
        "/platform/tenant-onboarding/provision",
        json={
            "code": tenant_code,
            "name": f"Business Day {plan.key} {suffix}",
            "type": "BORLETTE",
            "timezone": "America/Port-au-Prince",
            "currency": "HTG",
            "defaultCommissionRate": "10.00",
            "profile": "DEFAULT_HAITI_LOTTERY",
            "maryajGratisEnabled": False,
            "initialAdminEmail": admin_email,
        },
        headers=_rid(),
    )
    assert_ok(prov, expected=(200, 201))
    result = _data(prov)
    tenant_id = result["tenantId"]
    admin_email = result["initialAdminEmail"]

    admin = ApiClient(
        base_url=base_url,
        token=fb_auth.mint(subject=fb_auth.uid_for_email(admin_email), email=admin_email),
    )
    first = admin.post(
        "/identity/me/complete-first-login",
        json={
            "firstName": "Business",
            "lastName": plan.key.upper(),
            "phoneNumber": f"+50940{phone_seed:06d}",
            "passwordChanged": True,
        },
        headers=_rid(),
    )
    assert_ok(first)

    _configure_maryaj_variant(admin, plan)
    if plan.maryaj_variant == "disabled":
        campaigns = admin.get("/admin/promotions/campaigns", headers=_rid())
        assert_ok(campaigns)
        assert not (_data(campaigns) or []), "disabled maryaj tenant should start without campaigns"

    sellers = tuple(
        _create_seller_terminal(admin, base_url, plan.key, seller_plan, suffix, phone_seed, index)
        for index, seller_plan in enumerate(plan.seller_terminals)
    )
    assert len(sellers) >= 2, "business-day tenants must exercise multiple seller terminals"
    return TenantRuntime(plan, tenant_code, tenant_id, admin, sellers)


def _configure_maryaj_variant(admin: ApiClient, plan: TenantScenarioPlan) -> None:
    if plan.maryaj_variant == "disabled":
        return

    if plan.maryaj_variant in {"fixed_auto", "fixed_seller_selects"}:
        _configure_maryaj_gratis_fixed_pricing(admin, plan)
        body = {
            "payoutBaseAmount": "1.00",
            "quantityMode": "FIXED",
            "quantity": 1,
            "choiceMode": "SELLER_SELECTS" if plan.maryaj_seller_selects_expected else "AUTO_GENERATE",
            "regenerableBeforeConfirm": not plan.maryaj_seller_selects_expected,
            "maxRegenerationsBeforeConfirm": 3,
        }
        if not plan.maryaj_seller_selects_expected:
            body["generationStrategy"] = "RANDOM"
        maryaj = admin.post(
            "/admin/promotions/campaigns/templates/default-maryaj-gratis/instantiate",
            json=body,
            headers=_rid(),
        )
        assert_ok(maryaj)
        data = _data(maryaj)
        assert data["status"] == "ACTIVE"
        rule = data["rules"][0]
        effect = rule["effects"][0]
        assert effect["type"] == "FREE_GAME_LINE"
        assert effect["params"]["choiceMode"] == body["choiceMode"]
        return

    raise AssertionError(f"unsupported maryaj variant: {plan.maryaj_variant}")


def _configure_maryaj_gratis_fixed_pricing(admin: ApiClient, plan: TenantScenarioPlan) -> None:
    for variant, bet_option in (
        ("MARRIAGE_EXACT_ORDER", 1),
        ("MARRIAGE_REVERSE_ALLOWED", 2),
    ):
        pricing = admin.put(
            "/admin/pricing/rules",
            json={
                "gameCode": "HT_MARYAJ_GRATIS",
                "pricingVariantCode": variant,
                "betType": "MARRIAGE_2D2D",
                "betOption": bet_option,
                "odds": "1.0000",
                "payoutRuleType": "FIXED_AMOUNT",
                "fixedAmount": plan.maryaj_gratis_fixed_amount,
            },
            headers=_rid(),
        )
        assert_ok(pricing)
        data = _data(pricing)
        assert data["gameCode"] == "HT_MARYAJ_GRATIS"
        assert data["payoutRuleType"] == "FIXED_AMOUNT"
        assert _decimal(data["fixedAmount"]) == _decimal(plan.maryaj_gratis_fixed_amount)


def _create_seller_terminal(
    admin: ApiClient,
    base_url: str,
    tenant_key: str,
    plan: SellerTerminalPlan,
    suffix: str,
    phone_seed: int,
    index: int,
) -> SellerRuntime:
    create = admin.post(
        "/admin/seller-terminals",
        json={
            "terminalCode": f"BD-{tenant_key.upper()}-{plan.key.upper()}-{suffix}",
            "displayName": f"BD {tenant_key.upper()} {plan.key}",
            "firstName": "Business",
            "lastName": f"Seller {plan.key}",
            "email": f"seller-{tenant_key}-{plan.key}-{suffix}@business-day.test",
            "phoneNumber": f"+5094{index + 1}{phone_seed:06d}",
            "commissionRate": plan.commission_rate,
            "initialPin": "123456",
        },
        headers=_rid(),
    )
    assert_ok(create, expected=(200, 201))
    seller_terminal_id = _id(_data(create))

    if plan.bolet_override_odds:
        override = admin.put(
            f"/admin/controls/pricing-rules/seller-terminals/{seller_terminal_id}",
            json={
                "gameCode": "HT_BOLET",
                "pricingVariantCode": "MATCH_1_2D",
                "betType": "MATCH_1_2D",
                "betOption": None,
                "odds": plan.bolet_override_odds,
                "payoutRuleType": "STAKE_MULTIPLIER",
                "fixedAmount": None,
                "reason": "business-day e2e bolet odds override",
            },
            headers=_rid(),
        )
        assert_ok(override)
        listed = admin.get(
            f"/admin/controls/pricing-rules/seller-terminals/{seller_terminal_id}",
            headers=_rid(),
        )
        assert_ok(listed)
        assert any(
            str(item.get("gameCode")) == "HT_BOLET" and str(item.get("odds")) == plan.bolet_override_odds
            for item in (_data(listed) or [])
        ), "seller-terminal bolet odds override should be visible"

    client = ApiClient(
        base_url=base_url,
        token=admin.token,
        extra_headers={"X-Tch-Act-As-Terminal": seller_terminal_id},
    )
    return SellerRuntime(plan, seller_terminal_id, client)


def _active_draw_channel_codes(runtime: TenantRuntime) -> tuple[str, ...]:
    listed = runtime.admin.get("/tenant/draw-channels", params={"activeOnly": True}, headers=_rid())
    assert_ok(listed)
    channels = _data(listed) or []
    codes = tuple(
        sorted(
            str(channel.get("channelCode") or channel.get("code"))
            for channel in channels
            if channel.get("active", True) and (channel.get("channelCode") or channel.get("code"))
        )
    )
    assert codes, f"{runtime.code} should have active draw channels from DEFAULT_HAITI_LOTTERY"
    return codes


def _draw_channel_code(draw: dict[str, Any]) -> str:
    channel = draw.get("channel") or {}
    return str(draw.get("drawChannelCode") or draw.get("channelCode") or channel.get("code") or "")


def _draw_slot_key(draw: dict[str, Any]) -> str:
    slot = draw.get("slot") or {}
    return str(draw.get("resultSlotKey") or slot.get("key") or slot.get("slotKey") or "")


def _generate_and_force_open_draws(sa: ApiClient, runtime: TenantRuntime, start: dt.date) -> list[dict[str, Any]]:
    today = dt.date.today()
    expected_channel_codes = set(runtime.plan.draw_channel_codes or _active_draw_channel_codes(runtime))
    generated = sa.post(
        "/platform/ops/draws/generate",
        json={
            "tenantCodes": [runtime.code],
            "from": start.isoformat(),
            "to": today.isoformat(),
            "dryRun": False,
            "force": False,
            "reason": "business-day e2e backdated window",
        },
        headers=_rid(),
    )
    assert_ok(generated)

    admin_as_sa = sa.with_tenant(runtime.tenant_id, "business-day draw lifecycle")
    listed = admin_as_sa.get(
        "/admin/draws",
        params={"from": start.isoformat(), "to": today.isoformat(), "page": 0, "size": 100},
        headers=_rid(),
    )
    assert_ok(listed)
    draws = sorted(
        _items(_data(listed)),
        key=lambda d: (_draw_channel_code(d), d.get("drawDate") or "", d.get("scheduledAt") or ""),
    )
    selected_by_channel: dict[str, dict[str, Any]] = {}
    for draw in draws:
        code = _draw_channel_code(draw)
        if code in expected_channel_codes and code not in selected_by_channel:
            selected_by_channel[code] = draw
    missing = expected_channel_codes - set(selected_by_channel)
    assert not missing, f"missing one generated draw for active channels: {sorted(missing)}"
    selected = [selected_by_channel[code] for code in sorted(selected_by_channel)]
    max_draws = _env_int("TCH_E2E_BUSINESS_DAY_DRAW_COUNT", 0)
    if max_draws > 0:
        selected = selected[:max_draws]
    assert selected, "business-day scenario needs at least one draw"

    now = dt.datetime.now(dt.timezone.utc)
    scheduled_at = (now + dt.timedelta(days=1)).replace(microsecond=0)
    cutoff_at = (scheduled_at - dt.timedelta(hours=1)).replace(microsecond=0)
    rescheduled: list[dict[str, Any]] = []
    for draw in selected:
        reschedule = admin_as_sa.post(
            f"/admin/draws/{draw_id(draw)}/reschedule",
            json={
                "scheduledAt": scheduled_at.isoformat().replace("+00:00", "Z"),
                "cutoffAt": cutoff_at.isoformat().replace("+00:00", "Z"),
                "reason": "business-day e2e makes a past official draw temporarily sellable",
                "force": True,
            },
            headers=_rid(),
        )
        assert_ok(reschedule)
        rescheduled.append(_data(reschedule))

    open_resp = admin_as_sa.post(
        "/admin/draws/lifecycle/open",
        json={
            "drawIds": [draw_id(draw) for draw in rescheduled],
            "reason": "business-day e2e forced open",
        },
        headers=_rid(),
    )
    assert_ok(open_resp)
    opened = _data(open_resp)
    assert all(draw["status"] == "OPEN" for draw in opened)
    return opened


def _configure_limits(runtime: TenantRuntime, draw: dict[str, Any]) -> None:
    if runtime.plan.blocked_selection:
        block = runtime.admin.put(
            "/admin/policies/limits/assignments",
            json={
                "targetType": "TENANT",
                "ruleKey": "BLOCK_SELECTION_PER_DRAW",
                "enabled": True,
                "onBreach": "BLOCK",
                "params": {
                    "betType": "MATCH_1_2D",
                    "selections": [runtime.plan.blocked_selection],
                },
            },
            headers=_rid(),
        )
        assert_ok(block)

    if runtime.plan.draw_channel_exposure_limit_cents:
        _upsert_draw_channel_exposure_limit(runtime, draw, runtime.plan.draw_channel_exposure_limit_cents)


def _upsert_draw_channel_exposure_limit(
    runtime: TenantRuntime,
    draw: dict[str, Any],
    value_minor_units: int,
) -> str:
    exposure = runtime.admin.put(
        "/admin/policies/limits/assignments",
        json={
            "targetType": "DRAW_CHANNEL",
            "targetId": draw_channel_id(draw),
            "ruleKey": "MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW",
            "enabled": True,
            "onBreach": "BLOCK",
            "params": {"valueCents": value_minor_units},
        },
        headers=_rid(),
    )
    assert_ok(exposure)
    data = _data(exposure) or {}
    if isinstance(data, dict):
        return _id(data.get("id") or data)
    return _id(data)


def _assert_limit_blocks(runtime: TenantRuntime, draw: dict[str, Any]) -> None:
    if not runtime.plan.blocked_selection:
        return
    blocked = {
        **sale_payload(draw, ticket_scenario(0, ManualResultPlan())),
        "lines": [
            {
                "lineNumber": 1,
                "gameCode": "HT_BOLET",
                "betType": "MATCH_1_2D",
                "selection": runtime.plan.blocked_selection,
                "stakeAmount": "1.00",
            }
        ],
    }
    response = runtime.sellers[0].client.post("/tenant/sales/preparations", json=blocked, headers=_rid())
    if response.status_code < 300:
        confirm = runtime.sellers[0].client.post(
            f"/tenant/sales/preparations/{_data(response)['preparationId']}/confirm",
            idempotency_key=str(uuid.uuid4()),
            headers=_rid(),
        )
        assert confirm.status_code in (400, 409, 422), (
            f"blocked selection should not confirm: {confirm.status_code} {confirm.text}"
        )
    else:
        assert response.status_code in (400, 409, 422), (
            f"blocked selection should fail cleanly: {response.status_code} {response.text}"
        )


def _assert_exposure_limit_blocks(runtime: TenantRuntime, draw: dict[str, Any]) -> None:
    if not runtime.plan.draw_channel_exposure_limit_cents:
        return
    original_limit = runtime.plan.draw_channel_exposure_limit_cents
    # The probe selection is absent from the happy-path basket, so its current exposure is
    # expected to be zero. The temporary cap is therefore current exposure + 100 minor units.
    current_probe_exposure_minor_units = 0
    _upsert_draw_channel_exposure_limit(runtime, draw, current_probe_exposure_minor_units + 100)
    try:
        _assert_sale_blocked(
            runtime.sellers[0],
            draw,
            "draw-channel exposure limit",
            scenario=exposure_limit_probe_ticket(),
            expected_issue_codes={"limits.blocked", "selection_exposure_limit", "limit.max_stake_exposure"},
            ticket_count_client=runtime.admin,
        )
    finally:
        _upsert_draw_channel_exposure_limit(runtime, draw, original_limit)


def _ticket_count(client: ApiClient, draw: dict[str, Any]) -> int:
    listed = client.get(
        "/tenant/tickets",
        params={"drawId": draw_id(draw), "page": 0, "size": 1},
        headers=_rid(),
    )
    assert_ok(listed)
    return _page_total(_data(listed))


def _assert_sale_blocked(
    seller: SellerRuntime,
    draw: dict[str, Any],
    reason: str,
    *,
    scenario=None,
    expected_issue_codes: set[str] | None = None,
    ticket_count_client: ApiClient | None = None,
    assert_ticket_count: bool = True,
) -> None:
    inspector = ticket_count_client or seller.client
    before_ticket_count = _ticket_count(inspector, draw) if assert_ticket_count else None
    payload = sale_payload(draw, scenario or short_ticket())
    prep = seller.client.post("/tenant/sales/preparations", json=payload, headers=_rid())
    if prep.status_code >= 300:
        assert prep.status_code in (400, 403, 409, 422), (
            f"{reason}: expected clean prepare rejection, got {prep.status_code} {prep.text}; "
            f"draw={draw_id(draw)} channel={draw_channel_id(draw)} "
            f"ticketCountBefore={before_ticket_count}"
        )
        if expected_issue_codes:
            assert any(code in prep.text for code in expected_issue_codes), (
                f"{reason}: expected one of {sorted(expected_issue_codes)} in prepare error, got {prep.text}"
            )
        if assert_ticket_count:
            after_ticket_count = _ticket_count(inspector, draw)
            assert after_ticket_count == before_ticket_count, (
                f"{reason}: prepare rejection created ticket side effects; "
                f"before={before_ticket_count} after={after_ticket_count}"
            )
        return

    prepared = _data(prep)
    confirm = seller.client.post(
        f"/tenant/sales/preparations/{prepared['preparationId']}/confirm",
        idempotency_key=str(uuid.uuid4()),
        headers=_rid(),
    )
    if confirm.status_code >= 300:
        assert confirm.status_code in (400, 403, 409, 422), (
            f"{reason}: expected clean confirm rejection, got {confirm.status_code} {confirm.text}; "
            f"draw={draw_id(draw)} channel={draw_channel_id(draw)} "
            f"ticketCountBefore={before_ticket_count}"
        )
        if expected_issue_codes:
            assert any(code in confirm.text for code in expected_issue_codes), (
                f"{reason}: expected one of {sorted(expected_issue_codes)} in confirm error, got {confirm.text}"
            )
        if assert_ticket_count:
            after_ticket_count = _ticket_count(inspector, draw)
            assert after_ticket_count == before_ticket_count, (
                f"{reason}: confirm rejection created ticket side effects; "
                f"before={before_ticket_count} after={after_ticket_count}"
            )
        return

    data = _data(confirm) or {}
    outcome = (data.get("sale") or data).get("outcome")
    assert outcome != "ACCEPTED", (
        f"{reason}: sale should be blocked but confirm accepted: {data}; "
        f"draw={draw_id(draw)} channel={draw_channel_id(draw)} ticketCountBefore={before_ticket_count}"
    )
    if expected_issue_codes:
        assert any(code in confirm.text for code in expected_issue_codes), (
            f"{reason}: expected one of {sorted(expected_issue_codes)} in rejected sale body, got {confirm.text}"
        )
    if assert_ticket_count:
        after_ticket_count = _ticket_count(inspector, draw)
        assert after_ticket_count == before_ticket_count, (
            f"{reason}: rejected sale body created ticket side effects; "
            f"before={before_ticket_count} after={after_ticket_count}"
        )


def _sell_ticket(
    runtime: TenantRuntime,
    seller: SellerRuntime,
    draw: dict[str, Any],
    scenario,
) -> tuple[str, Decimal, int, Decimal]:
    payload = sale_payload(draw, scenario)
    if scenario.seller_selects_promo_selection:
        payload["promotionChoices"] = [
            {
                "decisionId": None,
                "gameCode": "HT_MARYAJ_GRATIS",
                "index": 0,
                "rawSelection": ManualResultPlan().maryaj_win,
                "selectionSource": "CUSTOMER_SELECTED",
            }
        ]
    prep = seller.client.post(
        "/tenant/sales/preparations",
        json=payload,
        headers=_rid(),
    )
    assert_ok(prep)
    prepared = _data(prep)
    assert _decimal(prepared["totalAmount"]) == scenario.expected_total_amount

    promo_count = len(prepared.get("promotionLines") or [])
    assert promo_count == scenario.expected_promotion_lines, (
        f"{runtime.code} {scenario.key} expected exactly {scenario.expected_promotion_lines} "
        f"promotion line(s), got {promo_count}: {prepared.get('promotionLines')}"
    )
    if scenario.requires_maryaj_gratis:
        promo_line = prepared["promotionLines"][0]
        if scenario.seller_selects_promo_selection:
            assert promo_line["choiceMode"] == "SELLER_SELECTS"
            assert promo_line["selectionSource"] == "CUSTOMER_SELECTED"
            assert promo_line["selection"] == ManualResultPlan().maryaj_win
        else:
            assert promo_line["choiceMode"] == "AUTO_GENERATE"
            assert promo_line["selectionSource"] == "PROMOTION_GENERATED"
            assert promo_line.get("selection"), "AUTO_GENERATE Maryaj gratis should materialize a selection"
    elif not runtime.plan.maryaj_gratis_expected:
        assert promo_count == 0, f"{runtime.code} has Maryaj gratis disabled"

    sold = seller.client.post(
        f"/tenant/sales/preparations/{prepared['preparationId']}/confirm",
        idempotency_key=str(uuid.uuid4()),
        headers=_rid(),
    )
    assert_ok(sold, expected=(200, 201))
    data = _data(sold) or {}
    sale = data.get("sale") or data
    ticket = sale.get("ticket") or {}
    assert sale.get("outcome") == "ACCEPTED"
    total_amount = _decimal(ticket.get("totalAmount") or sale.get("totalAmount") or prepared["totalAmount"])
    assert total_amount == scenario.expected_total_amount

    ticket_id = data.get("ticketId") or sale.get("ticketId") or ticket.get("ticketId")
    assert ticket_id

    return _id(ticket_id), total_amount, scenario.expected_promotion_lines, scenario.expected_winnings


def _record_manual_results(sa: ApiClient, draws: list[dict[str, Any]], result: ManualResultPlan) -> float:
    started = time.monotonic()
    seen: set[tuple[str, str]] = set()
    for draw in draws:
        key = (draw["drawDate"], _draw_slot_key(draw))
        if key in seen:
            continue
        seen.add(key)
        manual = sa.post(
            "/platform/ops/draw-results/manual",
            json={
                "drawDate": draw["drawDate"],
                "slotKey": _draw_slot_key(draw),
                "recordedBy": "business-day-e2e",
                "notes": (
                    "Known winning selections: bolet 12, maryaj 12-21, loto3 112, "
                    "loto4 2125, loto5 lot1-lot2 11221, loto5 lot1-lot3 11225"
                ),
                "lot1": result.lot1,
                "lot2": result.lot2,
                "lot3": result.lot3,
                "force": True,
                "reason": "business-day e2e deterministic manual result",
            },
            headers=_rid(),
        )
        assert_ok(manual)
    return time.monotonic() - started


def _force_apply_results(sa: ApiClient, runtime: TenantRuntime, start: dt.date, result: ManualResultPlan) -> float:
    started = time.monotonic()
    applied = sa.post(
        "/platform/ops/draws/apply",
        json={
            "tenantCodes": [runtime.code],
            "baseDate": dt.date.today().isoformat(),
            "daysBack": max(0, (dt.date.today() - start).days),
            "slotKeys": None,
            "force": True,
            "dryRun": False,
            "maxSlots": 500,
            "reason": f"business-day e2e applies manual result {result.lot1}-{result.lot2}-{result.lot3}",
        },
        headers=_rid(),
    )
    assert_ok(applied)
    return time.monotonic() - started


def _scheduler_can_cover(draws: list[dict[str, Any]]) -> bool:
    today = dt.date.today()
    scheduler_dates = {today, today - dt.timedelta(days=1)}
    return all(dt.date.fromisoformat(draw["drawDate"]) in scheduler_dates for draw in draws)


def _report(runtime: TenantRuntime, path: str, start: dt.date, draw_ids: list[str] | None = None) -> dict[str, Any]:
    params: dict[str, Any] = {
        "from": start.isoformat(),
        "to": dt.date.today().isoformat(),
        "limit": 500,
        "drawLimit": 500,
        "sellerTerminalLimit": 500,
    }
    if draw_ids:
        params["drawIds"] = ",".join(draw_ids)
    response = runtime.admin.get(path, params=params, headers=_rid())
    assert_ok(response)
    return _data(response)


def _wait_for_report_totals(
    runtime: TenantRuntime,
    start: dt.date,
    draw_ids: list[str],
    expected: ExpectedTenantTotals,
    timeout_seconds: int | None = None,
) -> tuple[dict[str, Any], float]:
    started = time.monotonic()
    deadline = time.monotonic() + (
        timeout_seconds if timeout_seconds is not None else _env_int("TCH_E2E_BUSINESS_DAY_REPORT_TIMEOUT_SECONDS", 20)
    )
    last: dict[str, Any] | None = None
    while time.monotonic() < deadline:
        last = _report(runtime, "/admin/reports/overview", start, draw_ids)
        summary = last["summary"]
        if (
            summary["ticketsSold"] == expected.tickets
            and _decimal(summary["grossSales"]) == expected.gross_sales
            and summary["promotionLines"] == expected.promotion_lines
            and expected.winning_floor
            <= _decimal(summary["winningsCalculated"])
            <= expected.winning_ceiling
        ):
            return last, time.monotonic() - started
        time.sleep(1)
    raise AssertionError(
        "report totals did not settle for "
        f"{runtime.code}: drawIds={draw_ids} expected={expected} last={last}"
    )


def _assert_reports(
    runtime: TenantRuntime,
    start: dt.date,
    draws: list[dict[str, Any]],
    expected: ExpectedTenantTotals,
    timeout_seconds: int | None = None,
) -> float:
    draw_ids = [draw_id(draw) for draw in draws]
    overview, settle_seconds = _wait_for_report_totals(runtime, start, draw_ids, expected, timeout_seconds)
    summary = overview["summary"]
    assert summary["ticketsSold"] == expected.tickets
    assert _decimal(summary["grossSales"]) == expected.gross_sales
    assert summary["promotionLines"] == expected.promotion_lines
    winnings = _decimal(summary["winningsCalculated"])
    assert expected.winning_floor <= winnings <= expected.winning_ceiling, (
        f"{runtime.code} winnings must include deterministic paid/seller-selected wins and "
        f"only optional random auto-promo wins: floor={expected.winning_floor} "
        f"ceiling={expected.winning_ceiling} actual={winnings}"
    )
    assert any(row["displaySelection"] == "12" for row in overview["topSelections"])

    draw_report = _report(runtime, "/admin/reports/draws", start, draw_ids)
    assert draw_report["summary"]["ticketsSold"] == expected.tickets
    assert _decimal(draw_report["summary"]["grossSales"]) == expected.gross_sales
    draw_winnings = _decimal(draw_report["summary"]["winningsCalculated"])
    assert expected.winning_floor <= draw_winnings <= expected.winning_ceiling
    reported_draw_ids = {row["drawId"] for row in draw_report["rows"]}
    assert set(draw_ids).issubset(reported_draw_ids)
    reported_channel_codes = {row["drawChannelCode"] for row in draw_report["rows"]}
    assert {_draw_channel_code(draw) for draw in draws}.issubset(reported_channel_codes)

    seller_report = _report(runtime, "/admin/reports/seller-terminals", start)
    assert seller_report["summary"]["ticketsSold"] == expected.tickets
    assert _decimal(seller_report["summary"]["grossSales"]) == expected.gross_sales
    rows_by_seller = {row["sellerTerminalId"]: row for row in seller_report["rows"]}
    for seller_id, seller_expected in (expected.by_seller or {}).items():
        row = rows_by_seller[seller_id]
        assert row["ticketsSold"] == seller_expected.tickets
        assert _decimal(row["grossSales"]) == seller_expected.gross_sales
        planned_rate = next(s.plan.commission_rate for s in runtime.sellers if s.seller_terminal_id == seller_id)
        assert _decimal(row["sellerCommission"]) == (
            seller_expected.gross_sales * Decimal(planned_rate) / Decimal("100")
        ).quantize(Decimal("0.01"))
    return settle_seconds


def _apply_or_wait_for_scheduler(
    sa: ApiClient,
    runtime: TenantRuntime,
    start: dt.date,
    draws: list[dict[str, Any]],
    expected: ExpectedTenantTotals,
    result: ManualResultPlan,
) -> ResultFlowTiming:
    manual_seconds = _record_manual_results(sa, draws, result)
    mode = _result_apply_mode()

    # Manual result entry records the known result; the normal product path is the
    # draw.processing scheduler, which runs results:external:apply and settle on its cron.
    # That scheduler only scans today/yesterday; week-long historical backfill scenarios must
    # use force apply or scheduler_then_force.
    if mode == "scheduler":
        assert _scheduler_can_cover(draws), (
            "TCH_E2E_RESULT_APPLY_MODE=scheduler only covers today/yesterday draws; "
            "use force for the 2026-07-09 business-day backfill scenario or set the start date "
            "to yesterday/today for a pure scheduler assertion."
        )
        settle_seconds = _assert_reports(
            runtime,
            start,
            draws,
            expected,
            timeout_seconds=_env_int("TCH_E2E_RESULT_SCHEDULER_TIMEOUT_SECONDS", 390),
        )
        return ResultFlowTiming(manual_seconds, mode, None, settle_seconds)

    if mode == "scheduler_then_force":
        if _scheduler_can_cover(draws):
            try:
                settle_seconds = _assert_reports(
                    runtime,
                    start,
                    draws,
                    expected,
                    timeout_seconds=_env_int("TCH_E2E_RESULT_SCHEDULER_GRACE_SECONDS", 390),
                )
                return ResultFlowTiming(manual_seconds, mode, None, settle_seconds)
            except AssertionError:
                pass
        apply_seconds = _force_apply_results(sa, runtime, start, result)
        settle_seconds = _assert_reports(runtime, start, draws, expected)
        return ResultFlowTiming(manual_seconds, mode, apply_seconds, settle_seconds)

    apply_seconds = _force_apply_results(sa, runtime, start, result)
    settle_seconds = _assert_reports(runtime, start, draws, expected)
    return ResultFlowTiming(manual_seconds, mode, apply_seconds, settle_seconds)


def _assert_cross_tenant_report_isolation(start: dt.date, runs: list[TenantRunResult]) -> None:
    for run in runs:
        other_draw_ids = [
            draw_id(draw)
            for other in runs
            if other.runtime.tenant_id != run.runtime.tenant_id
            for draw in other.draws
        ]
        assert other_draw_ids, "cross-tenant isolation needs at least two tenants with draws"
        overview = _report(run.runtime, "/admin/reports/overview", start, other_draw_ids)
        assert overview["summary"]["ticketsSold"] == 0, (
            f"{run.runtime.code} report leaked tickets for other tenant draws"
        )
        assert _decimal(overview["summary"]["grossSales"]) == Decimal("0.00"), (
            f"{run.runtime.code} report leaked gross sales for other tenant draws"
        )
        draw_report = _report(run.runtime, "/admin/reports/draws", start, other_draw_ids)
        assert draw_report["summary"]["ticketsSold"] == 0, (
            f"{run.runtime.code} draw report leaked tickets for other tenant draws"
        )
        assert _decimal(draw_report["summary"]["grossSales"]) == Decimal("0.00"), (
            f"{run.runtime.code} draw report leaked gross sales for other tenant draws"
        )
        foreign_ids = set(other_draw_ids)
        leaked_rows = [row for row in draw_report.get("rows", []) if row.get("drawId") in foreign_ids]
        assert not leaked_rows, f"{run.runtime.code} leaked foreign draw metadata: {leaked_rows}"

        own_overview = _report(run.runtime, "/admin/reports/overview", start)
        assert own_overview["summary"]["ticketsSold"] == run.expected.tickets
        assert _decimal(own_overview["summary"]["grossSales"]) == run.expected.gross_sales


def _placeholder_route_inventory() -> dict[str, list[str]]:
    return {
        "admin": [
            "/app/admin/company/settings",
            "/app/admin/help",
            "/app/admin/i18n",
            "/app/admin/pagemodels",
        ],
        "platform": [
            "/app/platform/entitlements",
            "/app/platform/access/backend-keys",
            "/app/platform/tchala/suggestions",
            "/app/platform/tchala/import",
            "/app/platform/tchala/cleanup",
            "/app/platform/reports",
            "/app/platform/ops/providers",
            "/app/platform/ops/resources",
            "/app/platform/ops/identity-sync",
        ],
    }


@pytest.mark.L2
@pytest.mark.full_flow
@pytest.mark.slow
def test_business_day_happy_path_supports_reports_results_and_future_locust(
    super_admin_client: ApiClient, base_url: str, fb_auth: FirebaseEmulatorAuth
) -> None:
    start = _start_date()
    result = ManualResultPlan()
    basket_repeats = _env_int("TCH_E2E_BUSINESS_DAY_BASKET_REPEATS", 1)
    assert basket_repeats >= 1, "scenario needs at least one basket per selected draw"

    runtimes = [
        _provision_tenant(super_admin_client, base_url, fb_auth, plan)
        for plan in default_tenant_plans()
    ]

    assert len(runtimes) >= 5
    assert any(runtime.plan.maryaj_gratis_expected for runtime in runtimes)
    assert any(runtime.plan.blocked_selection for runtime in runtimes)
    assert any(seller.plan.has_override for runtime in runtimes for seller in runtime.sellers)

    run_results: list[TenantRunResult] = []
    for runtime in runtimes:
        draws = _generate_and_force_open_draws(super_admin_client, runtime, start)
        _configure_limits(runtime, draws[0])
        _assert_limit_blocks(runtime, draws[0])

        expected = ExpectedTenantTotals()
        for draw_index, draw in enumerate(draws):
            ticket_index = 0
            for _ in range(basket_repeats):
                for seller_offset in range(len(runtime.sellers)):
                    seller = runtime.sellers[(draw_index + seller_offset) % len(runtime.sellers)]
                    for scenario in ticket_basket(result, seller.plan, runtime.plan):
                        _, total, promo_count, winnings = _sell_ticket(
                            runtime,
                            seller,
                            draw,
                            scenario=scenario,
                        )
                        expected.tickets += 1
                        expected.gross_sales += total
                        expected.promotion_lines += promo_count
                        expected.winning_floor += winnings
                        expected.auto_promo_winning_ceiling += scenario.auto_generated_promo_winning_ceiling
                        seller_expected = expected.seller(seller.seller_terminal_id)
                        seller_expected.tickets += 1
                        seller_expected.gross_sales += total
                        ticket_index += 1

            min_tickets = _env_int("TCH_E2E_BUSINESS_DAY_MIN_TICKETS_PER_DRAW", 10)
            assert ticket_index >= min_tickets, (
                f"{runtime.code} {draw_id(draw)} should sell at least {min_tickets} tickets "
                f"for reliable stats; sold {ticket_index}"
            )

        _assert_exposure_limit_blocks(runtime, draws[0])

        timing = _apply_or_wait_for_scheduler(super_admin_client, runtime, start, draws, expected, result)
        assert timing.manual_record_seconds <= _env_int("TCH_E2E_MANUAL_RESULT_MAX_SECONDS", 10)
        if timing.apply_launch_seconds is not None:
            assert timing.apply_launch_seconds <= _env_int("TCH_E2E_RESULT_APPLY_LAUNCH_MAX_SECONDS", 10)
        if timing.apply_mode == "scheduler":
            assert timing.report_settle_seconds <= _env_int("TCH_E2E_RESULT_SCHEDULER_TIMEOUT_SECONDS", 390)
        else:
            assert timing.report_settle_seconds <= _env_int("TCH_E2E_RESULT_SETTLE_MAX_SECONDS", 20)
        run_results.append(TenantRunResult(runtime, tuple(draws), expected))

    _assert_cross_tenant_report_isolation(start, run_results)
    assert _placeholder_route_inventory()["admin"], "placeholder routes are inventoried for human decision"


@pytest.mark.L2
@pytest.mark.full_flow
@pytest.mark.slow
@pytest.mark.serial_catalog_mutation
def test_sale_availability_gates_block_unavailable_runtime_state(
    super_admin_client: ApiClient, base_url: str, fb_auth: FirebaseEmulatorAuth
) -> None:
    """A seller cannot complete a sale when one runtime availability gate is inactive.

    This is deliberately separate from the business-day happy path: it mutates availability
    switches and asserts each switch blocks a fresh sale attempt cleanly.
    """

    if not _catalog_mutation_allowed():
        pytest.skip(
            "set TCH_E2E_ALLOW_CATALOG_MUTATION=true only in an isolated E2E environment; "
            "this test mutates result-slot/draw-channel kill switches."
        )

    plan = TenantScenarioPlan(
        key="gates",
        maryaj_mode="disabled",
        maryaj_variant="disabled",
        seller_terminals=(SellerTerminalPlan("main"), SellerTerminalPlan("backup")),
    )
    runtime = _provision_tenant(super_admin_client, base_url, fb_auth, plan)
    draws = _generate_and_force_open_draws(super_admin_client, runtime, _start_date())
    draw = draws[0]
    seller = runtime.sellers[0]
    admin_as_sa = super_admin_client.with_tenant(runtime.tenant_id, "business-day sale gate e2e")

    baseline_ticket_id, _, _, _ = _sell_ticket(runtime, seller, draw, short_ticket())
    assert baseline_ticket_id, "baseline sale must work before mutating availability gates"

    blocked = runtime.admin.patch(
        f"/admin/seller-terminals/{seller.seller_terminal_id}/block",
        json={"reason": "business-day e2e sale gate"},
        headers=_rid(),
    )
    assert blocked.status_code in (200, 204), blocked.text
    try:
        _assert_sale_blocked(
            seller,
            draw,
            "blocked seller terminal",
            expected_issue_codes={"seller_terminal.cannot_sell", "sales.terminal_blocked"},
            ticket_count_client=runtime.admin,
        )
    finally:
        unblocked = runtime.admin.patch(
            f"/admin/seller-terminals/{seller.seller_terminal_id}/unblock",
            headers=_rid(),
        )
        assert unblocked.status_code in (200, 204), unblocked.text

    disabled_game = runtime.admin.post("/admin/games/HT_BOLET/disable", headers=_rid())
    assert_ok(disabled_game)
    try:
        _assert_sale_blocked(
            seller,
            draw,
            "disabled tenant game",
            expected_issue_codes={"sales.tenant_game_disabled", "tenant_game_disabled"},
            ticket_count_client=runtime.admin,
        )
    finally:
        enabled_game = runtime.admin.post("/admin/games/HT_BOLET/enable", headers=_rid())
        assert_ok(enabled_game, expected=(200, 201))

    texas_draw = next((candidate for candidate in draws if _draw_slot_key(candidate).startswith("TX_")), None)
    assert texas_draw is not None, "DEFAULT_HAITI_LOTTERY seed should include active Texas result slots"
    slot_key = _draw_slot_key(texas_draw)
    slot = super_admin_client.get(f"/platform/result-slots/by-key/{slot_key}", headers=_rid())
    assert_ok(slot)
    slot_data = _data(slot)
    assert slot_data and slot_data["active"], f"{slot_key} should start active from seed"
    disabled_slot = super_admin_client.post(f"/platform/result-slots/{slot_key}/disable", headers=_rid())
    assert_ok(disabled_slot)
    try:
        _assert_sale_blocked(
            seller,
            texas_draw,
            "inactive result slot",
            expected_issue_codes={"Result slot is not active for sales", "result_slot"},
            ticket_count_client=runtime.admin,
        )
    finally:
        restore_slot = super_admin_client.put(
            f"/platform/result-slots/{_id(slot_data['id'])}",
            json={
                "slotKey": slot_data["slotKey"],
                "provider": slot_data["provider"],
                "timezone": slot_data["timezone"],
                "drawTime": slot_data["drawTime"],
                "daysOfWeek": slot_data["daysOfWeek"],
                "sortOrder": slot_data["sortOrder"],
                "sourceCfg": slot_data["sourceCfg"],
                "projectionCfg": slot_data["projectionCfg"],
                "notes": slot_data.get("notes"),
                "labelKey": slot_data.get("labelKey"),
                "active": slot_data["active"],
            },
            headers=_rid(),
        )
        assert restore_slot.status_code < 300, f"failed to restore result slot {slot_key}: {restore_slot.text}"
        assert_ok(restore_slot)
        restored_slot = super_admin_client.get(f"/platform/result-slots/by-key/{slot_key}", headers=_rid())
        assert_ok(restored_slot)
        assert _data(restored_slot)["active"], f"cleanup failed: result slot {slot_key} is not active"

    control_plan = TenantScenarioPlan(
        key="gatesctrl",
        maryaj_mode="disabled",
        maryaj_variant="disabled",
        seller_terminals=(SellerTerminalPlan("main"), SellerTerminalPlan("backup")),
    )
    control_runtime = _provision_tenant(super_admin_client, base_url, fb_auth, control_plan)
    control_draw = _generate_and_force_open_draws(super_admin_client, control_runtime, _start_date())[0]
    control_seller = control_runtime.sellers[0]

    before_tenant_suspend_count = _ticket_count(admin_as_sa, draw)
    suspended = super_admin_client.post(
        f"/platform/tenants/{runtime.tenant_id}/suspend",
        json={"reason": "business-day e2e sale gate tenant inactive"},
        headers=_rid(),
    )
    assert suspended.status_code in (200, 204), suspended.text
    try:
        control_ticket_id, _, _, _ = _sell_ticket(control_runtime, control_seller, control_draw, short_ticket())
        assert control_ticket_id, "tenant suspension must not block sales for other tenants"
        _assert_sale_blocked(
            seller,
            draw,
            "inactive tenant",
            expected_issue_codes={"tenant", "TENANT", "access.denied", "forbidden"},
            assert_ticket_count=False,
        )
    finally:
        activated = super_admin_client.post(
            f"/platform/tenants/{runtime.tenant_id}/activate",
            headers=_rid(),
        )
        assert activated.status_code in (200, 204), activated.text
        restored_tenant = super_admin_client.get(f"/platform/tenants/{runtime.tenant_id}", headers=_rid())
        assert_ok(restored_tenant)
        assert _data(restored_tenant)["status"] == "ACTIVE", (
            f"cleanup failed: tenant {runtime.tenant_id} was not restored to ACTIVE"
        )
        after_tenant_suspend_count = _ticket_count(admin_as_sa, draw)
        assert after_tenant_suspend_count == before_tenant_suspend_count, (
            "inactive tenant gate created ticket side effects; "
            f"before={before_tenant_suspend_count} after={after_tenant_suspend_count}"
        )

    channel = admin_as_sa.get(f"/platform/draw-channels/{draw_channel_id(draw)}", headers=_rid())
    assert_ok(channel)
    channel_data = _data(channel)
    disabled_channel = admin_as_sa.post(f"/platform/draw-channels/{draw_channel_id(draw)}/disable", headers=_rid())
    assert_ok(disabled_channel)
    try:
        _assert_sale_blocked(
            seller,
            draw,
            "inactive draw channel",
            expected_issue_codes={"Draw channel is not active for sales", "draw_channel"},
            ticket_count_client=runtime.admin,
        )
    finally:
        restore_channel = admin_as_sa.put(
            f"/platform/draw-channels/{draw_channel_id(draw)}",
            json={
                "tenantId": _id(channel_data["tenantId"]),
                "code": channel_data["code"],
                "name": channel_data["name"],
                "label": channel_data.get("label"),
                "timezone": channel_data["timezone"],
                "drawTime": channel_data["drawTime"],
                "cutoffSec": channel_data["cutoffSec"],
                "daysOfWeek": channel_data["daysOfWeek"],
                "active": channel_data["active"],
                "sortOrder": channel_data["sortOrder"],
                "period": channel_data.get("period"),
                "notes": channel_data.get("notes"),
                "resultSlotId": _id(channel_data["resultSlotId"]),
                "defaultSource": channel_data.get("defaultSource"),
            },
            headers=_rid(),
        )
        assert restore_channel.status_code < 300, (
            f"failed to restore draw channel {draw_channel_id(draw)}: {restore_channel.text}"
        )
        assert_ok(restore_channel)

    closed = admin_as_sa.post(
        "/admin/draws/lifecycle/close",
        json={"drawIds": [draw_id(draw)], "reason": "business-day e2e sale gate close"},
        headers=_rid(),
    )
    assert_ok(closed)
    _assert_sale_blocked(
        seller,
        draw,
        "draw not open",
        expected_issue_codes={"Draw is not open for sales", "draw_closed"},
        ticket_count_client=runtime.admin,
    )
