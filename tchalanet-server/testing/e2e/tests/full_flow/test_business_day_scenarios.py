"""Business-day happy-path scenario for stats/report validation.

This is deliberately a scenario harness, not a one-off smoke test:

1. provision mixed tenants (Maryaj gratis implicit/explicit/disabled, limits, multiple sellers,
   one seller with commission + bolet odds override);
2. select one draw for every active DEFAULT_HAITI_LOTTERY draw channel and force it sellable;
3. sell a documented deterministic basket on every draw, distributed across seller terminals;
4. enter manual results with known winning selections;
5. let scheduled apply process the manual results, or force apply in fast-run mode, then
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
from decimal import Decimal, ROUND_HALF_UP
from typing import Any, Literal

import httpx
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
    winning_lot1_only_ticket,
)
from tch_e2e.client import ApiClient, _resolve_verify

pytestmark = [pytest.mark.L2, pytest.mark.full_flow, pytest.mark.slow]

_FIREBASE_PROVIDERS = {"firebase-emulator", "firebase"}
_DEFAULT_START = dt.date(2026, 7, 9)
_SELLER_SELECTED_MARYAJ_GRATIS = "13-21"
ScenarioMode = Literal["happy_path", "availability_gates"]


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
    seller_commission: Decimal = Decimal("0.00")
    by_seller: dict[str, "ExpectedTenantTotals"] | None = None
    by_seller_draw: dict[tuple[str, str], "ExpectedTenantTotals"] | None = None

    @property
    def winning_ceiling(self) -> Decimal:
        return self.winning_floor + self.auto_promo_winning_ceiling

    def seller(self, seller_terminal_id: str) -> "ExpectedTenantTotals":
        if self.by_seller is None:
            self.by_seller = {}
        return self.by_seller.setdefault(seller_terminal_id, ExpectedTenantTotals())

    def seller_draw(self, seller_terminal_id: str, draw_id_value: str) -> "ExpectedTenantTotals":
        if self.by_seller_draw is None:
            self.by_seller_draw = {}
        return self.by_seller_draw.setdefault((seller_terminal_id, draw_id_value), ExpectedTenantTotals())


@dataclass(frozen=True)
class WinningTicketProbe:
    ticket_id: str
    expected_winnings: Decimal
    scenario_key: str


@dataclass(frozen=True)
class ResultFlowTiming:
    manual_record_seconds: float
    apply_mode: str
    apply_launch_seconds: float | None
    report_projection_seconds: float


@dataclass(frozen=True)
class TenantRunResult:
    runtime: TenantRuntime
    draws: tuple[dict[str, Any], ...]
    expected: ExpectedTenantTotals
    winning_probe: WinningTicketProbe | None = None


@dataclass(frozen=True)
class BusinessDayRunConfig:
    scenario: ScenarioMode
    tenant_keys: tuple[str, ...]
    start: dt.date
    draw_count: int
    basket_repeats: int
    apply_mode: str
    allow_catalog_mutation: bool


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


def _env_int_any(names: tuple[str, ...], default: int) -> int:
    for name in names:
        raw = os.environ.get(name, "").strip()
        if raw:
            return int(raw)
    return default


def _env_csv(name: str) -> tuple[str, ...]:
    raw = os.environ.get(name, "").strip()
    if not raw:
        return ()
    return tuple(item.strip().lower() for item in raw.split(",") if item.strip())


def _result_apply_mode() -> str:
    raw = os.environ.get("TCH_E2E_RESULT_APPLY_MODE", "force").strip().lower()
    allowed = {"force", "scheduler", "scheduler_then_force"}
    if raw not in allowed:
        raise AssertionError(f"TCH_E2E_RESULT_APPLY_MODE must be one of {sorted(allowed)}, got {raw!r}")
    return raw


def _catalog_mutation_allowed() -> bool:
    raw = os.environ.get("TCH_E2E_ALLOW_CATALOG_MUTATION", "").strip().lower()
    return raw in {"1", "true", "yes"}


def _env_mode(name: str, default: str, allowed: set[str]) -> str:
    raw = os.environ.get(name, default).strip().lower()
    if raw not in allowed:
        raise AssertionError(f"{name} must be one of {sorted(allowed)}, got {raw!r}")
    return raw


def _start_date() -> dt.date:
    raw = os.environ.get("TCH_E2E_BUSINESS_DAY_START", "").strip()
    return dt.date.fromisoformat(raw) if raw else _DEFAULT_START


def _business_day_config(scenario: ScenarioMode) -> BusinessDayRunConfig:
    basket_repeats = _env_int("TCH_E2E_BUSINESS_DAY_BASKET_REPEATS", 1)
    assert basket_repeats >= 1, "TCH_E2E_BUSINESS_DAY_BASKET_REPEATS must be >= 1"
    draw_count = _env_int("TCH_E2E_BUSINESS_DAY_DRAW_COUNT", 0)
    assert draw_count >= 0, "TCH_E2E_BUSINESS_DAY_DRAW_COUNT must be >= 0"
    return BusinessDayRunConfig(
        scenario=scenario,
        tenant_keys=_env_csv("TCH_E2E_BUSINESS_DAY_TENANTS"),
        start=_start_date(),
        draw_count=draw_count,
        basket_repeats=basket_repeats,
        apply_mode=_result_apply_mode(),
        allow_catalog_mutation=_catalog_mutation_allowed(),
    )


def _selected_tenant_plans(config: BusinessDayRunConfig) -> tuple[TenantScenarioPlan, ...]:
    plans_by_key = {plan.key: plan for plan in default_tenant_plans()}
    if not config.tenant_keys:
        return tuple(plans_by_key.values())

    unknown = sorted(set(config.tenant_keys) - set(plans_by_key))
    assert not unknown, (
        f"TCH_E2E_BUSINESS_DAY_TENANTS contains unknown tenant key(s): {unknown}; "
        f"allowed={sorted(plans_by_key)}"
    )
    selected = tuple(plans_by_key[key] for key in config.tenant_keys)
    if len(selected) < 2:
        pytest.skip("business-day report isolation needs at least two tenants; select two or more tenant keys")
    return selected


def _decimal(value: Any) -> Decimal:
    if isinstance(value, dict):
        value = value.get("amount") or value.get("value")
    return Decimal(str(value or "0")).quantize(Decimal("0.01"))


def _currency(value: Any) -> str | None:
    if isinstance(value, dict):
        if "currency" in value:
            return _currency(value["currency"])
        if "value" in value:
            return _currency(value["value"])
    return str(value) if value is not None else None


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
            "maryajGratisEnabled": plan.maryaj_gratis_expected,
            "initialAdminUsername": f"admin-{tenant_code}",
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

    _configure_explicit_bet_options(admin)
    _configure_maryaj_variant(admin, plan)
    if plan.maryaj_variant == "disabled":
        campaigns = admin.get("/admin/promotions/campaigns", headers=_rid())
        assert_ok(campaigns)
        assert not _items(_data(campaigns)), "disabled maryaj tenant should start without campaigns"

    sellers = tuple(
        _create_seller_terminal(admin, base_url, plan.key, seller_plan, suffix, phone_seed, index)
        for index, seller_plan in enumerate(plan.seller_terminals)
    )
    assert len(sellers) >= 2, "business-day tenants must exercise multiple seller terminals"
    activated = sa.post(f"/platform/tenants/{tenant_id}/activate", headers=_rid())
    assert activated.status_code in (200, 204), activated.text
    return TenantRuntime(plan, tenant_code, tenant_id, admin, sellers)


def _configure_explicit_bet_options(admin: ApiClient) -> None:
    for game_code in ("HT_MARYAJ", "HT_LOTO3", "HT_LOTO4", "HT_LOTO5"):
        current = admin.get(f"/admin/games/{game_code}/bet-options", headers=_rid())
        assert_ok(current)
        data = _data(current) or {}
        bet_types = data.get("betTypes") or []
        assert bet_types, f"{game_code} should expose tenant bet-option config"
        updated = []
        for config in bet_types:
            row = dict(config)
            row["selectionPolicy"] = "EXPLICIT_ONLY"
            updated.append(row)
        saved = admin.patch(
            f"/admin/games/{game_code}/bet-options",
            json={"betTypes": updated},
            headers=_rid(),
        )
        assert_ok(saved)
        saved_rows = (_data(saved) or {}).get("betTypes") or []
        assert all(row.get("selectionPolicy") == "EXPLICIT_ONLY" for row in saved_rows), (
            f"{game_code} should allow explicit bet options in E2E"
        )


def _configure_maryaj_variant(admin: ApiClient, plan: TenantScenarioPlan) -> None:
    if plan.maryaj_variant == "disabled":
        return

    if plan.maryaj_variant in {"fixed_auto", "fixed_seller_selects"}:
        body = {}
        if plan.maryaj_seller_selects_expected:
            body["choiceMode"] = "SELLER_SELECTS"
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
        expected_choice_mode = "SELLER_SELECTS" if plan.maryaj_seller_selects_expected else "AUTO_GENERATE"
        assert effect["params"]["choiceMode"] == expected_choice_mode
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
        listed_data = _data(listed)
        override_rows = _items(listed_data) or (listed_data if isinstance(listed_data, list) else [listed_data])
        assert any(
            str(item.get("gameCode")) == "HT_BOLET"
            and Decimal(str(item.get("odds"))) == Decimal(plan.bolet_override_odds)
            for item in override_rows
            if isinstance(item, dict)
        ), f"seller-terminal bolet odds override should be visible: {listed_data}"

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

    draws = _eventually_generated_draws(runtime.admin, start, today, expected_channel_codes)

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
        reschedule = sa.with_tenant(runtime.tenant_id, "business-day draw reschedule").post(
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

    open_resp = runtime.admin.post(
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


def _eventually_generated_draws(
    admin: ApiClient,
    start: dt.date,
    end: dt.date,
    expected_channel_codes: set[str],
) -> list[dict[str, Any]]:
    deadline = time.monotonic() + _env_int("TCH_E2E_DRAW_GENERATE_TIMEOUT_SECONDS", 30)
    last_total = 0
    last_codes: set[str] = set()
    while True:
        draws = _list_draws_for_window(admin, start, end)
        last_total = len(draws)
        last_codes = {_draw_channel_code(draw) for draw in draws}
        if expected_channel_codes.issubset(last_codes):
            return sorted(
                draws,
                key=lambda d: (_draw_channel_code(d), d.get("drawDate") or "", d.get("scheduledAt") or ""),
            )
        if time.monotonic() >= deadline:
            missing = sorted(expected_channel_codes - last_codes)
            raise AssertionError(
                f"generated draws did not become visible before timeout: "
                f"total={last_total} missing={missing}"
            )
        time.sleep(0.5)


def _list_draws_for_window(admin: ApiClient, start: dt.date, end: dt.date) -> list[dict[str, Any]]:
    page = 0
    items: list[dict[str, Any]] = []
    while True:
        listed = admin.get(
            "/admin/draws",
            params={
                "from": start.isoformat(),
                "to": end.isoformat(),
                "page": page,
                "size": 500,
                "sort": "scheduledAt,desc",
            },
            headers=_rid(),
        )
        assert_ok(listed)
        data = _data(listed)
        items.extend(_items(data))
        if not isinstance(data, dict) or not data.get("hasNext"):
            return items
        page += 1


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
        assert confirm.status_code in (400, 403, 409, 422), (
            f"blocked selection should not confirm: {confirm.status_code} {confirm.text}"
        )
    else:
        assert response.status_code in (400, 403, 409, 422), (
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


def _ticket_details(client: ApiClient, ticket_id: str) -> dict[str, Any]:
    response = client.get(f"/tenant/cashier/tickets/{ticket_id}", headers=_rid())
    assert_ok(response)
    return _data(response) or {}


def _settlement_support(admin: ApiClient, ticket_id: str) -> dict[str, Any]:
    response = admin.get(f"/admin/tickets/{ticket_id}/settlement-variants", headers=_rid())
    assert_ok(response)
    return _data(response) or {}


def _only_customer_line(support: dict[str, Any], game_code: str, selection: str) -> dict[str, Any]:
    lines = [
        line
        for line in support.get("lines") or []
        if line.get("gameCode") == game_code
        and str(line.get("selection") or "") == selection.replace("-", "")
    ]
    if not lines:
        lines = [line for line in support.get("lines") or [] if line.get("gameCode") == game_code]
    assert len(lines) == 1, f"expected one support line for {game_code}/{selection}: {support}"
    return lines[0]


def _admin_ticket_details(client: ApiClient, ticket_id: str) -> dict[str, Any]:
    response = client.get(f"/tenant/tickets/{ticket_id}", headers=_rid())
    assert_ok(response)
    return _data(response) or {}


def _public_verify_ticket(client: ApiClient, public_code: str) -> dict[str, Any]:
    headers = _rid()
    host_header = os.environ.get("TCH_E2E_HOST_HEADER", "").strip()
    if host_header:
        headers["Host"] = host_header
    response = httpx.post(
        f"{client.base_url.rstrip('/')}/public/tickets/verify",
        json={"publicCode": public_code},
        headers=headers,
        timeout=client.timeout,
        verify=_resolve_verify(),
    )
    assert_ok(response)
    return _data(response) or {}


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
                    "rawSelection": _SELLER_SELECTED_MARYAJ_GRATIS,
                    "selectionSource": "CUSTOMER_SELECTED",
                }
            ]
    prep = seller.client.post(
        "/tenant/sales/preparations",
        json=payload,
        headers=_rid(),
    )
    try:
        assert_ok(prep)
    except AssertionError as exc:
        raise AssertionError(
            f"{runtime.code}/{seller.plan.key}/{scenario.key} preparation failed; "
            f"payload={payload}"
        ) from exc
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
            assert promo_line["selection"] == _SELLER_SELECTED_MARYAJ_GRATIS
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


def _print_ticket_pdf(seller: SellerRuntime, ticket_id: str) -> bytes:
    response = seller.client.post(
        f"/tenant/cashier/tickets/{ticket_id}/print",
        json={
            "terminalId": seller.seller_terminal_id,
            "printOptionsRequest": {
                "outputFormat": "PDF",
                "paperSize": "RECEIPT_80MM",
            },
            "recordPrint": True,
            "deliveryOptions": ["RETURN_FILE"],
        },
        headers=_rid(),
    )
    assert_ok(response)
    assert response.content, f"ticket print should return PDF bytes for {ticket_id}"
    return response.content


def _send_ticket_to_slack(seller: SellerRuntime, ticket_id: str) -> dict[str, Any]:
    channel_key = os.environ.get("TCH_TEST_SLACK_CHANNEL_KEY", "delivery").strip() or "delivery"
    response = seller.client.post(
        f"/tenant/cashier/tickets/{ticket_id}/send",
        json={
            "terminalId": seller.seller_terminal_id,
            "channel": "SLACK_INTERNAL",
            "channelKey": channel_key,
            "locale": "fr",
        },
        headers=_rid(),
    )
    assert_ok(response, expected=(200, 202))
    return _data(response) or {}


def _dispatch_due_communications(super_admin_client: ApiClient) -> int:
    response = super_admin_client.post("/platform/ops/communication/dispatch-due", headers=_rid())
    assert_ok(response)
    data = _data(response) or {}
    return int(data.get("dispatched") or 0)


def _assert_ticket_slack_delivery_sent(
    super_admin_client: ApiClient,
    runtime: TenantRuntime,
    ticket_id: str,
) -> None:
    channel_key = os.environ.get("TCH_TEST_SLACK_CHANNEL_KEY", "delivery").strip() or "delivery"
    correlation_key = f"{ticket_id}:slack_internal:{channel_key}".lower()
    deadline = time.monotonic() + _env_int("TCH_E2E_TICKET_SLACK_TIMEOUT_SECONDS", 45)
    last_match: dict[str, Any] | None = None
    last_summary: dict[str, Any] | None = None

    while time.monotonic() < deadline:
        _dispatch_due_communications(super_admin_client)
        response = super_admin_client.get(
            "/platform/ops/communication/messages",
            params={
                "tenantId": runtime.tenant_id,
                "channel": "SLACK_INTERNAL",
                "recipient": channel_key,
                "page": 0,
                "size": 50,
            },
            headers=_rid(),
        )
        assert_ok(response)
        data = _data(response) or {}
        if isinstance(data, dict):
            last_summary = data.get("summary")
            messages_page = data.get("messages") or {}
        else:
            messages_page = {}
        for message in _items(messages_page):
            if str(message.get("correlationKey") or "").lower() != correlation_key:
                continue
            last_match = message
            attempts = message.get("attempts") or []
            if message.get("status") == "SENT" and any(
                attempt.get("status") == "SENT" and attempt.get("provider") == "edge"
                for attempt in attempts
            ):
                return
            if message.get("status") in {"FAILED", "SKIPPED", "CANCELLED"}:
                raise AssertionError(
                    f"{runtime.code}: Slack delivery ended as {message.get('status')} "
                    f"for ticket={ticket_id}: {message}"
                )
        time.sleep(0.5)

    raise AssertionError(
        f"{runtime.code}: Slack delivery did not reach SENT for ticket={ticket_id}; "
        f"lastMatch={last_match} summary={last_summary}"
    )


def _maybe_print_and_deliver_ticket(
    super_admin_client: ApiClient,
    runtime: TenantRuntime,
    seller: SellerRuntime,
    ticket_id: str,
    *,
    printed_or_sent: set[tuple[str, str]],
) -> None:
    print_mode = _env_mode("TCH_E2E_TICKET_PRINT_MODE", "none", {"none", "sample", "all"})
    slack_mode = _env_mode("TCH_E2E_TICKET_SLACK_MODE", "none", {"none", "sample", "all"})
    sample_key = (runtime.code, seller.seller_terminal_id)
    should_sample = sample_key not in printed_or_sent

    if print_mode == "all" or (print_mode == "sample" and should_sample):
        pdf = _print_ticket_pdf(seller, ticket_id)
        assert pdf.startswith(b"%PDF"), f"ticket print should return a PDF document for {ticket_id}"

    if slack_mode == "all" or (slack_mode == "sample" and should_sample):
        sent = _send_ticket_to_slack(seller, ticket_id)
        assert sent is not None, f"ticket Slack delivery should return data for {ticket_id}"
        _assert_ticket_slack_delivery_sent(super_admin_client, runtime, ticket_id)

    if should_sample and (print_mode == "sample" or slack_mode == "sample"):
        printed_or_sent.add(sample_key)


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


def _close_draws(runtime: TenantRuntime, draws: list[dict[str, Any]], reason: str) -> float:
    started = time.monotonic()
    response = runtime.admin.post(
        "/admin/draws/lifecycle/close",
        json={"drawIds": [draw_id(draw) for draw in draws], "reason": reason},
        headers=_rid(),
    )
    assert_ok(response)
    return time.monotonic() - started


def _force_apply_results(
    sa: ApiClient,
    runtime: TenantRuntime,
    draws: list[dict[str, Any]],
    result: ManualResultPlan,
) -> float:
    started = time.monotonic()
    slots_by_date: dict[str, set[str]] = {}
    for draw in draws:
        draw_date = str(draw.get("drawDate") or "").strip()
        slot_key = _draw_slot_key(draw)
        if draw_date and slot_key:
            slots_by_date.setdefault(draw_date, set()).add(slot_key)

    assert slots_by_date, f"{runtime.code}: force apply requires dated result slots"
    for draw_date, slot_keys in sorted(slots_by_date.items()):
        applied = sa.post(
            "/platform/ops/draws/apply",
            json={
                "tenantCodes": [runtime.code],
                # The operations handler clamps broad replay windows. Apply the
                # generated draw date directly so an older business day is covered.
                "baseDate": draw_date,
                "daysBack": 0,
                "slotKeys": sorted(slot_keys),
                "force": True,
                "dryRun": False,
                "maxSlots": len(slot_keys),
                "reason": f"business-day e2e applies manual result {result.lot1}-{result.lot2}-{result.lot3}",
            },
            headers=_rid(),
        )
        assert_ok(applied)
        _wait_for_batch_completion(sa, _data(applied) or {}, runtime.code, draw_date)
    return time.monotonic() - started


def _wait_for_batch_completion(
    sa: ApiClient,
    launch: dict[str, Any],
    tenant_code: str,
    draw_date: str,
    timeout_seconds: int = 30,
) -> None:
    launches = launch.get("launches") or []
    assert launches, f"{tenant_code}: result-apply launch returned no executions: {launch}"
    execution_id = launches[0].get("executionId") or launches[0].get("execution_id")
    assert execution_id, f"{tenant_code}: result-apply launch has no execution id: {launch}"

    deadline = time.monotonic() + timeout_seconds
    last_execution: dict[str, Any] | None = None
    while time.monotonic() < deadline:
        response = sa.get(f"/platform/ops/batch/executions/{execution_id}", headers=_rid())
        assert_ok(response)
        last_execution = _data(response) or {}
        status = str(last_execution.get("status") or "").upper()
        if status == "COMPLETED":
            return
        if status in {"FAILED", "STOPPED", "ABANDONED"}:
            raise AssertionError(
                f"{tenant_code}: result-apply batch failed for {draw_date}: {last_execution}"
            )
        time.sleep(0.5)

    raise AssertionError(
        f"{tenant_code}: result-apply batch did not complete for {draw_date}: {last_execution}"
    )


def _wait_for_draws_resulted(runtime: TenantRuntime, draws: list[dict[str, Any]], timeout_seconds: int = 30) -> float:
    started = time.monotonic()
    deadline = time.monotonic() + timeout_seconds
    last_by_draw: dict[str, Any] = {}
    while time.monotonic() < deadline:
        all_resulted = True
        for draw in draws:
            response = runtime.admin.get(f"/admin/draws/{draw_id(draw)}", headers=_rid())
            assert_ok(response)
            data = _data(response) or {}
            last_by_draw[draw_id(draw)] = data
            if data.get("status") not in {"RESULTED", "SETTLED"} or not (data.get("result") or data.get("lastResult")):
                all_resulted = False
        if all_resulted:
            return time.monotonic() - started
        time.sleep(0.5)
    raise AssertionError(f"{runtime.code}: draws did not reach RESULTED after result apply: {last_by_draw}")


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


def _financials_breakdown(runtime: TenantRuntime, start: dt.date) -> dict[str, Any]:
    response = runtime.admin.get(
        "/admin/financials/breakdown",
        params={
            "from": start.isoformat(),
            "to": dt.date.today().isoformat(),
            "drawLimit": 500,
            "sellerTerminalLimit": 500,
        },
        headers=_rid(),
    )
    assert_ok(response)
    return _data(response)


def _draw_top_selections(runtime: TenantRuntime, draw: dict[str, Any]) -> dict[str, Any]:
    response = runtime.admin.get(
        f"/admin/financials/draws/{draw_id(draw)}/top-selections",
        params={"limit": 10},
        headers=_rid(),
    )
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
            and expected.winning_floor
            <= _decimal(summary.get("payoutsPaid"))
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
    overview, projection_seconds = _wait_for_report_totals(runtime, start, draw_ids, expected, timeout_seconds)
    summary = overview["summary"]
    assert summary["ticketsSold"] == expected.tickets
    assert _decimal(summary["grossSales"]) == expected.gross_sales
    assert summary["promotionLines"] == expected.promotion_lines
    winnings = _decimal(summary["winningsCalculated"])
    payouts_paid = _decimal(summary.get("payoutsPaid"))
    assert expected.winning_floor <= winnings <= expected.winning_ceiling, (
        f"{runtime.code} winnings must include deterministic paid/seller-selected wins and "
        f"only optional random auto-promo wins: floor={expected.winning_floor} "
        f"ceiling={expected.winning_ceiling} actual={winnings}"
    )
    assert expected.winning_floor <= payouts_paid <= expected.winning_ceiling, (
        f"{runtime.code} payoutsPaid must follow the same paid result event as winningsCalculated "
        f"until an audited paid adjustment is applied: floor={expected.winning_floor} "
        f"ceiling={expected.winning_ceiling} actual={payouts_paid}"
    )
    winning_top_selection_candidates = {"12", "12-21", "112", "2125", "11221", "11225", "22125"}
    assert any(
        row["displaySelection"] in winning_top_selection_candidates for row in overview["topSelections"]
    ), f"{runtime.code} top selections should include a documented winning basket selection"

    financials = _financials_breakdown(runtime, start)
    financials_summary = financials["summary"]
    assert financials_summary["ticketsSold"] == expected.tickets
    assert _decimal(financials_summary["grossSales"]) == expected.gross_sales
    assert financials_summary["promotionLines"] == expected.promotion_lines
    financial_winnings = _decimal(financials_summary["winningsCalculated"])
    financial_payouts = _decimal(financials_summary.get("payoutsPaid"))
    assert expected.winning_floor <= financial_winnings <= expected.winning_ceiling
    assert expected.winning_floor <= financial_payouts <= expected.winning_ceiling
    financial_draw_ids = {row["drawId"] for row in financials["drawRows"]}
    assert set(draw_ids).issubset(financial_draw_ids)
    for seller_id, seller_expected in (expected.by_seller or {}).items():
        seller_rows = [
            row for row in financials["sellerTerminalDailyRows"] if row["sellerTerminalId"] == seller_id
        ]
        assert seller_rows, f"{runtime.code} financial breakdown missing seller {seller_id}"
        assert sum(row["ticketsSold"] for row in seller_rows) == seller_expected.tickets
        assert sum((_decimal(row["grossSales"]) for row in seller_rows), Decimal("0.00")) == seller_expected.gross_sales
        assert (
            sum((_decimal(row["sellerCommission"]) for row in seller_rows), Decimal("0.00"))
            == seller_expected.seller_commission
        )
    rows_by_seller_draw = {
        (row["sellerTerminalId"], row["drawId"]): row for row in financials["sellerTerminalDrawRows"]
    }
    sellers_per_draw: dict[str, set[str]] = {draw_id_value: set() for draw_id_value in draw_ids}
    for (seller_id, draw_id_value), seller_draw_expected in (expected.by_seller_draw or {}).items():
        row = rows_by_seller_draw.get((seller_id, draw_id_value))
        assert row, (
            f"{runtime.code} financial breakdown missing seller×draw row: "
            f"seller={seller_id} draw={draw_id_value}"
        )
        assert row["ticketsSold"] == seller_draw_expected.tickets
        assert _decimal(row["grossSales"]) == seller_draw_expected.gross_sales
        assert _decimal(row["sellerCommission"]) == seller_draw_expected.seller_commission
        sellers_per_draw.setdefault(draw_id_value, set()).add(seller_id)
    for draw_id_value, seller_ids in sellers_per_draw.items():
        assert len(seller_ids) == len(runtime.sellers), (
            f"{runtime.code} draw {draw_id_value} should have participation from every configured "
            f"seller terminal; expected={len(runtime.sellers)} actual={len(seller_ids)} sellers={seller_ids}"
        )

    direct_top = _draw_top_selections(runtime, draws[0])
    assert direct_top["drawId"] == draw_id(draws[0])
    assert any(
        row["displaySelection"] in winning_top_selection_candidates for row in direct_top["topSelections"]
    ), f"{runtime.code} direct financial top selections should include a documented winning basket selection"

    draw_report = _report(runtime, "/admin/reports/draws", start, draw_ids)
    assert draw_report["summary"]["ticketsSold"] == expected.tickets
    assert _decimal(draw_report["summary"]["grossSales"]) == expected.gross_sales
    draw_winnings = _decimal(draw_report["summary"]["winningsCalculated"])
    draw_payouts = _decimal(draw_report["summary"].get("payoutsPaid"))
    assert expected.winning_floor <= draw_winnings <= expected.winning_ceiling
    assert expected.winning_floor <= draw_payouts <= expected.winning_ceiling
    reported_draw_ids = {row["drawId"] for row in draw_report["rows"]}
    assert set(draw_ids).issubset(reported_draw_ids)
    reported_channel_tokens = {row["drawChannelCode"] for row in draw_report["rows"]}
    expected_channel_codes = {_draw_channel_code(draw) for draw in draws}
    expected_channel_ids = {draw_channel_id(draw) for draw in draws}
    assert expected_channel_codes.issubset(reported_channel_tokens) or expected_channel_ids.issubset(
        reported_channel_tokens
    ), (
        f"{runtime.code} draw report should identify selected channels by code or id: "
        f"codes={expected_channel_codes} ids={expected_channel_ids} reported={reported_channel_tokens}"
    )

    seller_report = _report(runtime, "/admin/reports/seller-terminals", start)
    assert seller_report["summary"]["ticketsSold"] == expected.tickets
    assert _decimal(seller_report["summary"]["grossSales"]) == expected.gross_sales
    rows_by_seller = {row["sellerTerminalId"]: row for row in seller_report["rows"]}
    for seller_id, seller_expected in (expected.by_seller or {}).items():
        row = rows_by_seller[seller_id]
        assert row["ticketsSold"] == seller_expected.tickets
        assert _decimal(row["grossSales"]) == seller_expected.gross_sales
        assert _decimal(row["sellerCommission"]) == seller_expected.seller_commission
    return projection_seconds


def _assert_paid_amount_adjustment_only_changes_paid_reports(
    super_admin_client: ApiClient,
    runtime: TenantRuntime,
    start: dt.date,
    draws: list[dict[str, Any]],
    probes: list[WinningTicketProbe],
) -> None:
    if not probes:
        return
    draw_ids = [draw_id(draw) for draw in draws]
    before = _report(runtime, "/admin/reports/overview", start, draw_ids)["summary"]
    before_winnings = _decimal(before["winningsCalculated"])
    before_payouts = _decimal(before.get("payoutsPaid"))

    reason = f"business-day e2e paid amount adjustment {uuid.uuid4()}"
    adjusted = None
    probe = None
    rejected: dict[str, str] = {}
    for candidate in probes:
        previous_paid = candidate.expected_winnings
        adjusted_paid = (previous_paid - Decimal("1.00")).quantize(Decimal("0.01"))
        if adjusted_paid < Decimal("0.00"):
            continue
        response = runtime.admin.patch(
            f"/tenant/tickets/{candidate.ticket_id}/payout-paid-amount",
            json={
                "previousPaidAmount": f"{previous_paid:.2f}",
                "paidAmount": f"{adjusted_paid:.2f}",
                "reason": reason,
            },
            headers=_rid(),
        )
        if response.status_code < 300:
            adjusted = response
            probe = candidate
            break
        rejected[candidate.ticket_id] = f"{response.status_code}: {response.text}"
    assert adjusted is not None and probe is not None, (
        f"{runtime.code}: no deterministic winning probe accepted paid adjustment; rejected={rejected}"
    )
    adjustment = _data(adjusted) or {}
    assert adjustment["ticketId"]
    assert adjustment["deltaAmountCents"] == -100

    deadline = time.monotonic() + _env_int("TCH_E2E_BUSINESS_DAY_REPORT_TIMEOUT_SECONDS", 20)
    last: dict[str, Any] | None = None
    while time.monotonic() < deadline:
        last = _report(runtime, "/admin/reports/overview", start, draw_ids)["summary"]
        if (
            _decimal(last["winningsCalculated"]) == before_winnings
            and _decimal(last.get("payoutsPaid")) == before_payouts - Decimal("1.00")
        ):
            break
        time.sleep(0.5)
    else:
        raise AssertionError(
            f"{runtime.code}: paid adjustment did not project only to payoutsPaid; "
            f"beforeWinnings={before_winnings} beforePayouts={before_payouts} last={last}"
        )

    audit = super_admin_client.get(
        "/platform/audit",
        params={
            "tenantId": runtime.tenant_id,
            "entityType": "PAYOUT",
            "entityId": probe.ticket_id,
            "action": "UPDATE",
            "page": 0,
            "size": 10,
        },
        headers=_rid(),
    )
    assert_ok(audit)
    audit_items = _items(_data(audit))
    assert any(reason in str(item) for item in audit_items), (
        f"{runtime.code}: paid adjustment audit entry should include reason={reason}; "
        f"items={audit_items}"
    )


def _wait_for_overview_summary(
    runtime: TenantRuntime,
    business_date: dt.date,
    draws: list[dict[str, Any]],
    *,
    tickets_sold: int,
    gross_sales: Decimal,
    winnings_calculated: Decimal,
    payouts_paid: Decimal,
) -> dict[str, Any]:
    deadline = time.monotonic() + _env_int("TCH_E2E_BUSINESS_DAY_REPORT_TIMEOUT_SECONDS", 20)
    last: dict[str, Any] | None = None
    while time.monotonic() < deadline:
        last = _report(
            runtime,
            "/admin/reports/overview",
            business_date,
            [draw_id(draw) for draw in draws],
        )["summary"]
        if (
            last["ticketsSold"] == tickets_sold
            and _decimal(last["grossSales"]) == gross_sales
            and _decimal(last["winningsCalculated"]) == winnings_calculated
            and _decimal(last.get("payoutsPaid")) == payouts_paid
        ):
            return last
        time.sleep(0.5)

    raise AssertionError(
        f"{runtime.code}: overview did not reach expected reconciliation totals; "
        f"expected=(tickets={tickets_sold}, gross={gross_sales}, winnings={winnings_calculated}, "
        f"paid={payouts_paid}) last={last}"
    )


def _reconcile_analytics(
    super_admin_client: ApiClient,
    runtime: TenantRuntime,
    business_date: dt.date,
    *,
    mode: str,
    repair_reason: str | None = None,
    idempotency_key: str | None = None,
) -> dict[str, Any]:
    response = super_admin_client.post(
        "/platform/ops/analytics/reconciliation",
        json={
            "tenantId": runtime.tenant_id,
            "from": business_date.isoformat(),
            "to": business_date.isoformat(),
            "mode": mode,
            "repairReason": repair_reason,
        },
        idempotency_key=idempotency_key,
        headers=_rid(),
    )
    assert_ok(response)
    data = _data(response) or {}
    assert data.get("status") == "SUCCESS", f"{runtime.code}: reconciliation failed: {data}"
    assert data.get("mismatches") == [], f"{runtime.code}: reconciliation mismatches: {data}"
    return data


def _assert_winning_ticket_can_be_verified(runtime: TenantRuntime, probes: list[WinningTicketProbe]) -> None:
    if not probes:
        return
    probe = next((item for item in probes if not item.scenario_key.startswith("maryaj-")), probes[0])
    details = _ticket_details(runtime.sellers[0].client, probe.ticket_id)
    assert _id(details.get("id")) == probe.ticket_id
    assert details.get("publicCode"), f"{runtime.code}: winning ticket detail should expose publicCode"
    assert details.get("lines"), f"{runtime.code}: winning ticket detail should expose sold lines"

    public_data: dict[str, Any] = {}
    deadline = time.monotonic() + _env_int("TCH_E2E_WINNING_TICKET_VERIFY_TIMEOUT_SECONDS", 20)
    data: dict[str, Any] = {}
    while time.monotonic() < deadline:
        verified = runtime.sellers[0].client.post(
            "/tenant/cashier/tickets/verify",
            json={"scannedValue": details["publicCode"]},
            headers=_rid(),
        )
        assert_ok(verified)
        data = _data(verified) or {}
        public_data = _public_verify_ticket(runtime.admin, details["publicCode"])
        if data.get("status") in {"PAYABLE", "ALREADY_PAID"} and public_data.get("status") in {
            "WON_CLAIMABLE",
            "WON_PAID",
        }:
            break
        time.sleep(0.5)
    else:
        latest_details = _ticket_details(runtime.sellers[0].client, probe.ticket_id)
        raise AssertionError(
            f"{runtime.code}: winning ticket should verify as payable/paid after result apply; "
            f"ticket={probe.ticket_id} publicCode={details['publicCode']} "
            f"lastCashierVerify={data} lastPublicVerify={public_data} latestDetails={latest_details}"
        )
    params = data.get("params") or {}
    assert _decimal(params.get("amount")) == probe.expected_winnings, (
        f"{runtime.code}: winning ticket verify amount mismatch; "
        f"ticket={probe.ticket_id} expected={probe.expected_winnings} response={data}"
    )
    assert params.get("currency") == "HTG"
    assert _decimal(public_data.get("winningAmount")) == probe.expected_winnings, (
        f"{runtime.code}: public winning ticket verify amount mismatch; "
        f"ticket={probe.ticket_id} expected={probe.expected_winnings} response={public_data}"
    )
    assert _currency(public_data.get("winningAmount")) == "HTG"


def _assert_winning_ticket_before_apply(runtime: TenantRuntime, probes: list[WinningTicketProbe]) -> None:
    if not probes:
        return
    probe = next((item for item in probes if not item.scenario_key.startswith("maryaj-")), probes[0])

    cashier_details = _ticket_details(runtime.sellers[0].client, probe.ticket_id)
    admin_details = _admin_ticket_details(runtime.admin, probe.ticket_id)
    assert _id(cashier_details.get("id")) == probe.ticket_id
    assert _id(admin_details.get("id")) == probe.ticket_id
    assert cashier_details.get("status") == "APPROVED"
    if admin_details.get("resultStatus") is not None:
        assert admin_details["resultStatus"] == "NOT_RESULTED"
    if admin_details.get("settlementStatus") is not None:
        assert admin_details["settlementStatus"] == "NOT_SETTLED"
    assert cashier_details.get("publicCode"), f"{runtime.code}: pre-apply detail should expose publicCode"

    cashier_verified = runtime.sellers[0].client.post(
        "/tenant/cashier/tickets/verify",
        json={"scannedValue": cashier_details["publicCode"]},
        headers=_rid(),
    )
    assert_ok(cashier_verified)
    data = _data(cashier_verified) or {}
    assert data.get("status") in {"NOT_PAYABLE_PENDING_DRAW", "NOT_PAYABLE_RESULT_PENDING"}, (
        f"{runtime.code}: pre-apply winning candidate should not be payable yet; "
        f"ticket={probe.ticket_id} response={data} cashierDetails={cashier_details} "
        f"adminDetails={admin_details}"
    )
    params = data.get("params") or {}
    assert _decimal(params.get("amount")) == Decimal("0.00")
    assert params.get("ticketStatus") == "AWAITING_RESULT"

    public_data = _public_verify_ticket(runtime.admin, cashier_details["publicCode"])
    assert public_data.get("status") == "AWAITING_RESULT", (
        f"{runtime.code}: public pre-apply verification should await result; "
        f"ticket={probe.ticket_id} response={public_data}"
    )
    assert public_data.get("winningAmount") is None


def _apply_or_wait_for_scheduler(
    sa: ApiClient,
    runtime: TenantRuntime,
    start: dt.date,
    draws: list[dict[str, Any]],
    expected: ExpectedTenantTotals,
    result: ManualResultPlan,
) -> ResultFlowTiming:
    mode = _result_apply_mode()
    if mode in {"force", "scheduler_then_force"}:
        _close_draws(runtime, draws, "business-day e2e closes draw before forced result backfill")

    manual_seconds = _record_manual_results(sa, draws, result)

    # Manual result entry records the known result; applying it is the business trigger that
    # evaluates tickets, auto-settles winning ticket state, and emits analytics events.
    # The draw lifecycle settle step is intentionally not required by this happy path.
    # That scheduler only scans today/yesterday; week-long historical backfill scenarios must
    # use force apply or scheduler_then_force.
    if mode == "scheduler":
        assert _scheduler_can_cover(draws), (
            "TCH_E2E_RESULT_APPLY_MODE=scheduler only covers today/yesterday draws; "
            "use force for the 2026-07-09 business-day backfill scenario or set the start date "
            "to yesterday/today for a pure scheduler assertion."
        )
        projection_seconds = _assert_reports(
            runtime,
            start,
            draws,
            expected,
            timeout_seconds=_env_int("TCH_E2E_RESULT_SCHEDULER_TIMEOUT_SECONDS", 390),
        )
        return ResultFlowTiming(manual_seconds, mode, None, projection_seconds)

    if mode == "scheduler_then_force":
        if _scheduler_can_cover(draws):
            try:
                projection_seconds = _assert_reports(
                    runtime,
                    start,
                    draws,
                    expected,
                    timeout_seconds=_env_int("TCH_E2E_RESULT_SCHEDULER_GRACE_SECONDS", 390),
                )
                return ResultFlowTiming(manual_seconds, mode, None, projection_seconds)
            except AssertionError:
                pass
        apply_seconds = _force_apply_results(sa, runtime, draws, result)
        _wait_for_draws_resulted(runtime, draws)
        projection_seconds = _assert_reports(runtime, start, draws, expected)
        return ResultFlowTiming(manual_seconds, mode, apply_seconds, projection_seconds)

    apply_seconds = _force_apply_results(sa, runtime, draws, result)
    _wait_for_draws_resulted(runtime, draws)
    projection_seconds = _assert_reports(runtime, start, draws, expected)
    return ResultFlowTiming(manual_seconds, mode, apply_seconds, projection_seconds)


def _assert_cross_tenant_report_isolation(start: dt.date, runs: list[TenantRunResult]) -> None:
    for run in runs:
        other_draw_ids = [
            draw_id(draw)
            for other in runs
            if other.runtime.tenant_id != run.runtime.tenant_id
            for draw in other.draws
        ]
        assert other_draw_ids, "cross-tenant isolation needs at least two tenants with draws"
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


@pytest.mark.L2
@pytest.mark.full_flow
@pytest.mark.slow
def test_seller_pricing_override_sale_result_and_payout(
    super_admin_client: ApiClient,
    base_url: str,
    fb_auth: FirebaseEmulatorAuth,
) -> None:
    """A seller override is snapshotted at sale time and drives the settled payout."""

    plan = TenantScenarioPlan(
        key="pricing-override",
        maryaj_mode="disabled",
        maryaj_variant="disabled",
        seller_terminals=(
            SellerTerminalPlan("override", commission_rate="10.00", bolet_override_odds="65.0000"),
            SellerTerminalPlan("fallback"),
        ),
    )
    runtime = _provision_tenant(super_admin_client, base_url, fb_auth, plan)
    seller = runtime.sellers[0]
    draw = _generate_and_force_open_draws(super_admin_client, runtime, dt.date.today())[0]
    result = ManualResultPlan()
    scenario = winning_lot1_only_ticket(result, seller.plan)

    ticket_id, total, _, expected_payout = _sell_ticket(runtime, seller, draw, scenario)
    assert total == Decimal("1.00")
    assert expected_payout == Decimal("65.00")

    support = _settlement_support(runtime.admin, ticket_id)
    line = _only_customer_line(support, "HT_BOLET", result.bolet_win)
    terms = line["settlementTermsSnapshot"]["terms"]
    assert [term["ruleCode"] for term in terms] == [
        "MATCH_1_2D",
        "MATCH_2_2D",
        "MATCH_3_2D",
    ]
    lot1_term = terms[0]
    assert lot1_term["source"] == "SELLER_TERMINAL_OVERRIDE"
    assert _decimal(lot1_term["payoutRule"]["multiplier"]) == Decimal("65.0000")

    _assert_winning_ticket_before_apply(
        runtime,
        [WinningTicketProbe(ticket_id, expected_payout, scenario.key)],
    )
    _close_draws(runtime, [draw], "seller pricing override e2e closes draw before result apply")
    _record_manual_results(super_admin_client, [draw], result)
    _force_apply_results(super_admin_client, runtime, [draw], result)
    _wait_for_draws_resulted(runtime, [draw])

    settled_support = _settlement_support(runtime.admin, ticket_id)
    settled_line = _only_customer_line(settled_support, "HT_BOLET", result.bolet_win)
    applied = settled_line["appliedSettlementSnapshot"]
    assert applied["schemaVersion"] == 1
    assert [term["ruleCode"] for term in applied["terms"]] == ["MATCH_1_2D"]
    assert applied["terms"][0]["source"] == "SELLER_TERMINAL_OVERRIDE"
    assert _decimal(applied["terms"][0]["realizedAmount"]) == Decimal("65.00")

    cashier_details = _ticket_details(seller.client, ticket_id)
    cashier_line = cashier_details["lines"][0]
    assert cashier_line["appliedSettlement"]["terms"][0]["ruleCode"] == "MATCH_1_2D"
    assert _decimal(cashier_line["payoutAmountCents"]) == Decimal("6500")

    _assert_winning_ticket_can_be_verified(
        runtime,
        [WinningTicketProbe(ticket_id, expected_payout, scenario.key)],
    )


@pytest.mark.L2
@pytest.mark.full_flow
@pytest.mark.slow
def test_reconciliation_rebuild_preserves_ticket_paid_amount_correction(
    super_admin_client: ApiClient,
    base_url: str,
    fb_auth: FirebaseEmulatorAuth,
) -> None:
    """Exercise sale, result, paid correction and idempotent analytics rebuild over real APIs."""

    plan = TenantScenarioPlan(
        key="reconciliation-paid-correction",
        maryaj_mode="disabled",
        maryaj_variant="disabled",
        seller_terminals=(
            SellerTerminalPlan("override", commission_rate="10.00", bolet_override_odds="65.0000"),
            SellerTerminalPlan("fallback"),
        ),
    )
    runtime = _provision_tenant(super_admin_client, base_url, fb_auth, plan)
    seller = runtime.sellers[0]
    business_date = dt.date.today()
    draw = _generate_and_force_open_draws(super_admin_client, runtime, business_date)[0]
    result = ManualResultPlan()
    scenario = winning_lot1_only_ticket(result, seller.plan)

    ticket_id, total, _, expected_payout = _sell_ticket(runtime, seller, draw, scenario)
    assert total == Decimal("1.00")
    assert expected_payout == Decimal("65.00")

    _close_draws(runtime, [draw], "reconciliation E2E closes draw before applying the result")
    _record_manual_results(super_admin_client, [draw], result)
    _force_apply_results(super_admin_client, runtime, [draw], result)
    _wait_for_draws_resulted(runtime, [draw])
    _wait_for_overview_summary(
        runtime,
        business_date,
        [draw],
        tickets_sold=1,
        gross_sales=total,
        winnings_calculated=expected_payout,
        payouts_paid=expected_payout,
    )

    adjusted_paid = expected_payout - Decimal("1.00")
    adjustment_reason = f"reconciliation E2E paid correction {uuid.uuid4()}"
    adjusted = runtime.admin.patch(
        f"/tenant/tickets/{ticket_id}/payout-paid-amount",
        json={"paidAmount": f"{adjusted_paid:.2f}", "reason": adjustment_reason},
        headers=_rid(),
    )
    assert_ok(adjusted)
    adjustment = _data(adjusted) or {}
    assert adjustment["ticketId"] == ticket_id
    assert adjustment["deltaAmountCents"] == -100
    corrected_summary = _wait_for_overview_summary(
        runtime,
        business_date,
        [draw],
        tickets_sold=1,
        gross_sales=total,
        winnings_calculated=expected_payout,
        payouts_paid=adjusted_paid,
    )

    validated = _reconcile_analytics(
        super_admin_client,
        runtime,
        business_date,
        mode="VALIDATE",
    )
    assert validated["mode"] == "VALIDATE"

    repair_key = f"reconciliation-{uuid.uuid4()}"
    repair_reason = "reconciliation E2E rebuild after paid amount correction"
    rebuilt = _reconcile_analytics(
        super_admin_client,
        runtime,
        business_date,
        mode="REBUILD_AND_VALIDATE",
        repair_reason=repair_reason,
        idempotency_key=repair_key,
    )
    replayed = _reconcile_analytics(
        super_admin_client,
        runtime,
        business_date,
        mode="REBUILD_AND_VALIDATE",
        repair_reason=repair_reason,
        idempotency_key=repair_key,
    )
    assert rebuilt["mode"] == "REBUILD_AND_VALIDATE"
    assert replayed["runId"] == rebuilt["runId"], "same idempotency key must replay the repair result"

    rebuilt_summary = _wait_for_overview_summary(
        runtime,
        business_date,
        [draw],
        tickets_sold=1,
        gross_sales=total,
        winnings_calculated=expected_payout,
        payouts_paid=adjusted_paid,
    )
    assert rebuilt_summary == corrected_summary


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
@pytest.mark.parametrize("scenario_mode", [pytest.param("happy_path", id="happy_path")])
def test_business_day_happy_path_supports_reports_results_and_future_locust(
    super_admin_client: ApiClient,
    base_url: str,
    fb_auth: FirebaseEmulatorAuth,
    scenario_mode: ScenarioMode,
) -> None:
    config = _business_day_config(scenario_mode)
    start = config.start
    result = ManualResultPlan()

    runtimes = [
        _provision_tenant(super_admin_client, base_url, fb_auth, plan)
        for plan in _selected_tenant_plans(config)
    ]

    if not config.tenant_keys:
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
        winning_probes: list[WinningTicketProbe] = []
        printed_or_sent: set[tuple[str, str]] = set()
        for draw_index, draw in enumerate(draws):
            ticket_index = 0
            draw_id_value = draw_id(draw)
            for _ in range(config.basket_repeats):
                for seller_offset in range(len(runtime.sellers)):
                    seller = runtime.sellers[(draw_index + seller_offset) % len(runtime.sellers)]
                    for scenario in ticket_basket(result, seller.plan, runtime.plan):
                        ticket_id, total, promo_count, winnings = _sell_ticket(
                            runtime,
                            seller,
                            draw,
                            scenario=scenario,
                        )
                        if winnings > Decimal("0.00"):
                            winning_probes.append(
                                WinningTicketProbe(
                                    ticket_id=ticket_id,
                                    expected_winnings=winnings,
                                    scenario_key=scenario.key,
                                )
                            )
                        expected.tickets += 1
                        expected.gross_sales += total
                        expected.promotion_lines += promo_count
                        expected.winning_floor += winnings
                        expected.auto_promo_winning_ceiling += scenario.auto_generated_promo_winning_ceiling
                        seller_expected = expected.seller(seller.seller_terminal_id)
                        seller_expected.tickets += 1
                        seller_expected.gross_sales += total
                        seller_expected.seller_commission += (
                            total * Decimal(seller.plan.commission_rate) / Decimal("100")
                        ).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
                        seller_draw_expected = expected.seller_draw(seller.seller_terminal_id, draw_id_value)
                        seller_draw_expected.tickets += 1
                        seller_draw_expected.gross_sales += total
                        seller_draw_expected.seller_commission += (
                            total * Decimal(seller.plan.commission_rate) / Decimal("100")
                        ).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
                        _maybe_print_and_deliver_ticket(
                            super_admin_client,
                            runtime,
                            seller,
                            ticket_id,
                            printed_or_sent=printed_or_sent,
                        )
                        ticket_index += 1

            min_tickets = _env_int("TCH_E2E_BUSINESS_DAY_MIN_TICKETS_PER_DRAW", 10)
            assert ticket_index >= min_tickets, (
                f"{runtime.code} {draw_id(draw)} should sell at least {min_tickets} tickets "
                f"for reliable stats; sold {ticket_index}"
            )

        _assert_exposure_limit_blocks(runtime, draws[0])

        _assert_winning_ticket_before_apply(runtime, winning_probes)
        timing = _apply_or_wait_for_scheduler(super_admin_client, runtime, start, draws, expected, result)
        assert timing.manual_record_seconds <= _env_int("TCH_E2E_MANUAL_RESULT_MAX_SECONDS", 10)
        if timing.apply_launch_seconds is not None:
            assert timing.apply_launch_seconds <= _env_int("TCH_E2E_RESULT_APPLY_LAUNCH_MAX_SECONDS", 10)
        if timing.apply_mode == "scheduler":
            assert timing.report_projection_seconds <= _env_int("TCH_E2E_RESULT_SCHEDULER_TIMEOUT_SECONDS", 390)
        else:
            assert timing.report_projection_seconds <= _env_int_any(
                ("TCH_E2E_RESULT_REPORT_MAX_SECONDS", "TCH_E2E_RESULT_SETTLE_MAX_SECONDS"),
                20,
            )
        _assert_winning_ticket_can_be_verified(runtime, winning_probes)
        _assert_paid_amount_adjustment_only_changes_paid_reports(
            super_admin_client, runtime, start, draws, winning_probes
        )
        run_results.append(TenantRunResult(runtime, tuple(draws), expected))

    _assert_cross_tenant_report_isolation(start, run_results)
    assert _placeholder_route_inventory()["admin"], "placeholder routes are inventoried for human decision"


@pytest.mark.L2
@pytest.mark.full_flow
@pytest.mark.slow
@pytest.mark.serial_catalog_mutation
@pytest.mark.parametrize("scenario_mode", [pytest.param("availability_gates", id="availability_gates")])
def test_sale_availability_gates_block_unavailable_runtime_state(
    super_admin_client: ApiClient,
    base_url: str,
    fb_auth: FirebaseEmulatorAuth,
    scenario_mode: ScenarioMode,
) -> None:
    """A seller cannot complete a sale when one runtime availability gate is inactive.

    This is deliberately separate from the business-day happy path: it mutates availability
    switches and asserts each switch blocks a fresh sale attempt cleanly.
    """

    config = _business_day_config(scenario_mode)
    if not config.allow_catalog_mutation:
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
    draws = _generate_and_force_open_draws(super_admin_client, runtime, config.start)
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
                "sortOrder": slot_data.get("sortOrder"),
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
    control_draw = _generate_and_force_open_draws(super_admin_client, control_runtime, config.start)[0]
    control_seller = control_runtime.sellers[0]

    activated_before_suspend = super_admin_client.post(
        f"/platform/tenants/{runtime.tenant_id}/activate",
        headers=_rid(),
    )
    assert activated_before_suspend.status_code in (200, 204), activated_before_suspend.text

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

    channel = admin_as_sa.get(
        "/platform/draw-channels",
        params={"code": _draw_channel_code(draw), "page": 0, "size": 10},
        headers=_rid(),
    )
    assert_ok(channel)
    channel_data = next(
        (item for item in _items(_data(channel)) if _id(item["id"]) == draw_channel_id(draw)),
        None,
    )
    assert channel_data is not None, (
        f"draw channel snapshot not found for id={draw_channel_id(draw)} "
        f"code={_draw_channel_code(draw)} response={_data(channel)}"
    )
    disabled_channel = admin_as_sa.post(
        f"/platform/draw-channels/{draw_channel_id(draw)}/disable?tenantId={_id(channel_data['tenantId'])}",
        headers=_rid(),
    )
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
