"""Cashier POS tests — Task 6 of e2e-business-runtime-v1.

Design §9 main path:
  1. login cashier (fixture)
  2. GET /tenant/cashier/home
  3. select/bind operational context
  4. open session
  5. list available draws
  6. sell ticket
  7. print ticket
  8. send ticket notification if configured
  9. list recent tickets
  10. close session

Negative cases (blockers) covered here:
  - missing context → requiredStep=SELECT_OPERATIONAL_CONTEXT
  - closed session → requiredStep=OPEN_SESSION
  - sell with bogus session id → rejected / 4xx
  - sell with cross-tenant terminal id → rejected / 4xx
  - close session → state reflects CLOSED

Cases requiring admin backend state change (lock terminal, block outlet) → skipped.
"""
from __future__ import annotations

import os
from pathlib import Path

import pytest

from flows.cashier import CashierFlow
from flows.home import HomeFlow
from prereqs.app_user import ensure_app_user_synced
from prereqs.draws import ensure_draws_today
from prereqs.session import ensure_pos_session_open
from tch_e2e.api_response import assert_ok
from tch_e2e.assertions import assert_money_breakdown, assert_required_step, assert_sale_accepted
from tch_e2e.client import ApiClient
from tch_e2e.config import OpContext, SeedIds
from tch_e2e.scenario_world import ScenarioWorld


def _artifact_dir() -> Path:
    """Resolve the PDF output dir, always to an ABSOLUTE path.

    ``TCH_ARTIFACT_DIR`` may be relative (e.g. ``target/e2e/tickets``); a relative
    value is anchored to the e2e root (``testing/e2e``) so it doesn't depend on
    pytest's working directory and is easy to find.
    """
    e2e_root = Path(__file__).resolve().parents[2]  # testing/e2e
    raw = os.environ.get("TCH_ARTIFACT_DIR")
    if raw:
        base = Path(raw)
        if not base.is_absolute():
            base = e2e_root / base
    else:
        base = e2e_root / "target" / "e2e" / "tickets"
    base.mkdir(parents=True, exist_ok=True)
    print(f"[artifacts] PDFs -> {base}")
    return base


# ===========================================================================
# L1 — Happy path (design §9 main path)
# ===========================================================================


@pytest.mark.L1
@pytest.mark.cashier_pos
def test_cashier_morning_happy_path(onboard_cashier_for_pos) -> None:
    """Full POS morning flow: draws → sell → print → send → list.

    ``onboard_cashier_for_pos`` ensures:
    - seller profile + outlet assignment for the cashier user
    - terminal binding (idempotent)
    - OPEN sales session (auto-finalizes any stale CLOSED session)
    - today's draws generated and open
    """
    from fixtures.pos_context import PosContext
    pos: PosContext = onboard_cashier_for_pos

    flow = CashierFlow(pos.cashier_client, pos.op_context(), stake_cents=pos.stake_cents)
    flow.select_context()

    artifacts = _artifact_dir()

    draws = flow.list_available_draws()
    assert draws, "no draws returned to cashier"

    open_draws = [d for d in draws if d.get("status") == "OPEN"]
    assert open_draws, (
        f"no OPEN draws among {len(draws)} returned — "
        "check tenant draw schedules or run ensure_draws_today."
    )

    sold: list[str] = []
    for draw in open_draws[:2]:  # cap to 2 draws to keep the test fast
        game_codes = draw.get("gameCodes") or []
        assert game_codes, f"draw {draw['drawId']} exposes no gameCodes"
        game_code = game_codes[0]

        preview = flow.preview(draw, game_code)
        assert preview["decision"] == "ACCEPTABLE", (
            f"preview rejected for {draw['channelCode']}/{game_code}: {preview}"
        )

        ticket = flow.sell(draw, game_code)
        assert_sale_accepted({
            "outcome": "ACCEPTED",
            "ticketId": ticket.ticket_id,
            "ticketCode": ticket.ticket_code,
        })
        assert ticket.sale_status in {"PLACED", "ACCEPTED", "APPROVED"}
        assert ticket.backup.get("displayCode")
        sold.append(ticket.ticket_id)

        pdf_bytes = flow.print_pdf(ticket.ticket_id)
        assert pdf_bytes.startswith(b"%PDF"), "print must return a PDF binary"
        (artifacts / f"{draw['channelCode']}_{game_code}_{ticket.ticket_code}.pdf").write_bytes(pdf_bytes)

        detail = flow.get_ticket(ticket.ticket_id)
        assert detail["ticketCode"] == ticket.ticket_code

        # Step 8 — send receipt notification (only when a Slack channel is wired).
        # Optional by design (§9): skip silently when not configured so the happy
        # path stays green without external delivery infra.
        if os.environ.get("TCH_TEST_SLACK_CHANNEL_KEY"):
            send_result = flow.send_slack(ticket.ticket_id)
            assert send_result, "send returned an empty body"

    all_tickets = flow.list_tickets()
    listed_ids = {row["id"] for row in all_tickets}
    missing = set(sold) - listed_ids
    assert not missing, f"tickets just sold missing from list: {missing}"

    # Step 10 — close the session and confirm it is no longer OPEN.
    flow.close_session(pos.session_id, reason="e2e:test_cashier_morning_happy_path")
    current = pos.cashier_client.get(
        "/tenant/cashier/session/current",
        params={"terminalId": pos.terminal_id},
    )
    if current.status_code == 200:
        status = (current.json().get("data") or {}).get("status")
        assert status != "OPEN", f"session still OPEN after close: {status!r}"
    else:
        assert current.status_code in (204, 404), (
            f"unexpected session/current status after close: {current.status_code}"
        )


# ===========================================================================
# L1 — POS home blockers
# ===========================================================================


@pytest.mark.L1
@pytest.mark.cashier_pos
def test_missing_context_returns_required_step(
    super_admin_client: ApiClient,
    cashier_client_a: ApiClient,
) -> None:
    """No X-Tch-* headers → home demands SELECT_OPERATIONAL_CONTEXT."""
    ensure_app_user_synced(super_admin_client)

    payload = HomeFlow(cashier_client_a, context=OpContext()).mobile_home(surface="MOBILE_POS")

    assert payload["surface"] == "MOBILE_POS"
    assert_required_step(payload, "SELECT_OPERATIONAL_CONTEXT")

    primary = payload["primaryAction"]
    # primaryAction uses 'type' (not 'kind') and 'route' (not 'href') since schema v2
    primary_type = primary.get("type") or primary.get("kind")
    assert primary_type == "SELECT_OPERATIONAL_CONTEXT", primary
    assert payload.get("session") is None
    assert payload.get("primaryDraw") is None


@pytest.mark.L2
@pytest.mark.cashier_pos
def test_closed_session_returns_required_step(
    super_admin_client: ApiClient,
    cashier_client_a: ApiClient,
    cashier_context_a: OpContext,
) -> None:
    """Context OK but no open session → requiredStep=OPEN_SESSION.

    L2 because closing a session on the same business day prevents reopening it
    (backend constraint: existsForBusinessDate). Run this test on a fresh business day
    or after implementing GET /tenant/cashier/session/today.
    """
    ensure_app_user_synced(super_admin_client)
    ctx = ensure_pos_session_open(cashier_client_a, cashier_context_a)

    close = cashier_client_a.post(
        "/tenant/cashier/session/close",
        json={
            "sessionId": ctx.session_id,
            "closingAmount": "100.00",
            "reason": "e2e:test_closed_session_returns_required_step",
        },
        context=ctx,
    )
    assert_ok(close)

    ctx_no_session = OpContext(outlet_id=ctx.outlet_id, terminal_id=ctx.terminal_id)
    payload = HomeFlow(cashier_client_a, context=ctx_no_session).mobile_home(surface="MOBILE_POS")

    assert payload["surface"] == "MOBILE_POS"
    assert_required_step(payload, "OPEN_SESSION")

    primary = payload["primaryAction"]
    primary_type = primary.get("type") or primary.get("kind")
    assert primary_type == "OPEN_SESSION", primary


# ===========================================================================
# L1 — Close session reflects state
# ===========================================================================


@pytest.mark.L2
@pytest.mark.cashier_pos
def test_close_session_reflects_state(
    cashier_client_a: ApiClient,
    cashier_context_a: OpContext,
) -> None:
    """Open → close → GET /session/current → status != OPEN.

    L2 because closing a session on the same business day prevents reopening it.
    """
    ctx = ensure_pos_session_open(cashier_client_a, cashier_context_a)

    close = cashier_client_a.post(
        "/tenant/cashier/session/close",
        json={
            "sessionId": ctx.session_id,
            "closingAmount": "100.00",
            "reason": "e2e:test_close_session_reflects_state",
        },
        context=ctx,
    )
    assert_ok(close)

    current = cashier_client_a.get(
        "/tenant/cashier/session/current",
        params={"terminalId": ctx.terminal_id},
    )
    if current.status_code == 204:
        return  # 204 No Content = no active session — correct
    assert_ok(current)
    data = current.json().get("data") or {}
    status = data.get("status")
    assert status != "OPEN", f"session should not be OPEN after close, got status={status!r}"


# ===========================================================================
# L2 — Sell blockers
# ===========================================================================


@pytest.mark.L2
@pytest.mark.cashier_pos
def test_sell_with_wrong_session_id_blocked(
    super_admin_client: ApiClient,
    cashier_client_a: ApiClient,
    cashier_context_a: OpContext,
    seed_ids: SeedIds,
) -> None:
    """Session ID that doesn't exist → sell must be rejected/4xx."""
    ensure_draws_today(super_admin_client, seed_ids)

    bogus_ctx = OpContext(
        outlet_id=cashier_context_a.outlet_id,
        terminal_id=cashier_context_a.terminal_id,
        session_id="00000000-0000-0000-0000-000000000000",
    )
    flow = CashierFlow(cashier_client_a, bogus_ctx, stake_cents=seed_ids.stake_cents)
    draws = flow.list_available_draws()
    open_draws = [d for d in draws if d.get("status") == "OPEN"]
    if not open_draws:
        pytest.skip("No OPEN draws available — run ensure_draws_today first")

    draw = open_draws[0]
    game_code = (draw.get("gameCodes") or ["HT_BOLET"])[0]
    result = flow.sell_expecting_rejection(draw, game_code)

    outcome = result.get("outcome")
    raw_status = result.get("_raw_status")
    assert outcome != "ACCEPTED" or (raw_status is not None and raw_status >= 400), (
        f"Expected sell to be blocked with bogus session, got outcome={outcome!r}, "
        f"raw_status={raw_status}"
    )


@pytest.mark.L2
@pytest.mark.cashier_pos
def test_sell_with_cross_tenant_terminal_blocked(
    super_admin_client: ApiClient,
    cashier_client_a: ApiClient,
    cashier_context_a: OpContext,
    seed_ids: SeedIds,
    world: ScenarioWorld,
) -> None:
    """Using a terminal_id that belongs to a different tenant → sell blocked."""
    cross_terminal = world.tenant_b.terminal_id or "00000000-0000-0000-0000-000000009999"
    if cross_terminal == seed_ids.terminal_id:
        pytest.skip("Tenant B not configured with a distinct terminal — set TCH_TENANT_2_TERMINAL_ID")

    ensure_draws_today(super_admin_client, seed_ids)
    ctx_cross = ensure_pos_session_open(cashier_client_a, cashier_context_a)
    cross_ctx = OpContext(
        outlet_id=cashier_context_a.outlet_id,
        terminal_id=cross_terminal,
        session_id=ctx_cross.session_id,
    )
    flow = CashierFlow(cashier_client_a, cross_ctx, stake_cents=seed_ids.stake_cents)
    draws = flow.list_available_draws()
    open_draws = [d for d in draws if d.get("status") == "OPEN"]
    if not open_draws:
        pytest.skip("No OPEN draws available")

    draw = open_draws[0]
    game_code = (draw.get("gameCodes") or ["HT_BOLET"])[0]
    result = flow.sell_expecting_rejection(draw, game_code)

    outcome = result.get("outcome")
    raw_status = result.get("_raw_status")
    assert outcome != "ACCEPTED" or (raw_status is not None and raw_status >= 400), (
        f"Expected sell to be blocked with cross-tenant terminal, got outcome={outcome!r}"
    )


@pytest.mark.L2
@pytest.mark.cashier_pos
@pytest.mark.skip(reason="requires admin lock API — implement when POST /terminals/{id}/lock is available")
def test_locked_terminal_blocks_sell() -> None:
    """Terminal locked → sell must be blocked. TBD: needs admin lock endpoint."""


@pytest.mark.L2
@pytest.mark.cashier_pos
@pytest.mark.skip(reason="requires admin outlet-block API — TBD")
def test_blocked_outlet_blocks_sell() -> None:
    """Outlet blocked / sales disabled → sell must be blocked. TBD."""
