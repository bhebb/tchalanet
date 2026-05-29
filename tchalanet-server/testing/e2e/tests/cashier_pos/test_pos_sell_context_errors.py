"""POS sell — operational context error cases.

These tests verify that the cashier sell endpoint rejects requests with
broken operational context. Each test mutates a fully onboarded PosContext
(produced by ``onboard_cashier_for_pos``) to introduce one specific fault.

Already covered in test_cashier_pos.py (not duplicated here):
  - test_missing_context_returns_required_step      (no X-Tch-* headers → home)
  - test_closed_session_returns_required_step       (GET home after close)
  - test_sell_with_wrong_session_id_blocked         (bogus session UUID)
  - test_sell_with_cross_tenant_terminal_blocked    (terminal from tenant B)
"""
from __future__ import annotations

import pytest

from fixtures.pos_context import PosContext
from flows.cashier import CashierFlow
from tch_e2e.api_response import assert_ok
from tch_e2e.client import ApiClient
from tch_e2e.config import SeedIds


# ===========================================================================
# L2 — Missing / wrong session header
# ===========================================================================


@pytest.mark.L2
@pytest.mark.cashier_pos
def test_sell_without_session_header_blocked(
    onboard_cashier_for_pos: PosContext,
) -> None:
    """Outlet + terminal headers present, no X-Tch-Sales-Session-Id → sell blocked."""
    ctx = onboard_cashier_for_pos
    draw = ctx.first_open_draw()
    if draw is None:
        pytest.skip("No OPEN draw available — run ensure_draws_today first.")

    broken = ctx.without_session()
    flow = CashierFlow(broken.cashier_client, broken.op_context(), broken.stake_cents)
    game_code = (draw.get("gameCodes") or ["HT_BOLET"])[0]
    result = flow.sell_expecting_rejection(draw, game_code)

    outcome = result.get("outcome")
    raw_status = result.get("_raw_status")
    assert outcome != "ACCEPTED" or (raw_status is not None and raw_status >= 400), (
        f"Expected sell to be blocked without session header, "
        f"got outcome={outcome!r}, raw_status={raw_status}"
    )


@pytest.mark.L2
@pytest.mark.cashier_pos
def test_sell_with_bogus_terminal_blocked(
    onboard_cashier_for_pos: PosContext,
) -> None:
    """Non-existent terminal ID → sell must be rejected / 4xx."""
    ctx = onboard_cashier_for_pos
    draw = ctx.first_open_draw()
    if draw is None:
        pytest.skip("No OPEN draw available.")

    broken = ctx.with_bogus_terminal()
    flow = CashierFlow(broken.cashier_client, broken.op_context(), broken.stake_cents)
    game_code = (draw.get("gameCodes") or ["HT_BOLET"])[0]
    result = flow.sell_expecting_rejection(draw, game_code)

    outcome = result.get("outcome")
    raw_status = result.get("_raw_status")
    assert outcome != "ACCEPTED" or (raw_status is not None and raw_status >= 400), (
        f"Expected sell to be blocked with bogus terminal, "
        f"got outcome={outcome!r}, raw_status={raw_status}"
    )


# ===========================================================================
# L2 — Business rule violations
# ===========================================================================


@pytest.mark.L2
@pytest.mark.cashier_pos
def test_sell_unknown_game_code_blocked(
    onboard_cashier_for_pos: PosContext,
) -> None:
    """Unknown game code → sell must be rejected (pricing / game not found)."""
    ctx = onboard_cashier_for_pos
    draw = ctx.first_open_draw()
    if draw is None:
        pytest.skip("No OPEN draw available.")

    # Force an unknown game code — CashierFlow falls back to MATCH_1_2D bet type
    # but the backend should reject because X_FAKE_GAME is not a valid game.
    result = ctx.cashier_flow().sell_expecting_rejection(draw, "X_FAKE_GAME")

    outcome = result.get("outcome")
    raw_status = result.get("_raw_status")
    assert outcome != "ACCEPTED" or (raw_status is not None and raw_status >= 400), (
        f"Expected sell to be blocked with unknown game code, "
        f"got outcome={outcome!r}, raw_status={raw_status}"
    )


@pytest.mark.L2
@pytest.mark.cashier_pos
def test_sell_bogus_draw_id_blocked(
    onboard_cashier_for_pos: PosContext,
) -> None:
    """Non-existent draw ID → sell must return 4xx or rejected outcome."""
    ctx = onboard_cashier_for_pos
    draw = ctx.first_open_draw()
    if draw is None:
        pytest.skip("No OPEN draw available (needed to build a structurally valid payload).")

    # Swap the real draw IDs with a non-existent UUID
    bogus_draw = {
        **draw,
        "drawId": "00000000-0000-0000-0000-000000000000",
        "drawChannelId": "00000000-0000-0000-0000-000000000000",
    }
    game_code = (draw.get("gameCodes") or ["HT_BOLET"])[0]
    result = ctx.cashier_flow().sell_expecting_rejection(bogus_draw, game_code)

    outcome = result.get("outcome")
    raw_status = result.get("_raw_status")
    assert outcome != "ACCEPTED" or (raw_status is not None and raw_status >= 400), (
        f"Expected sell to be blocked with bogus draw ID, "
        f"got outcome={outcome!r}, raw_status={raw_status}"
    )


# ===========================================================================
# L2 — Stubs for cases requiring additional admin setup
# ===========================================================================


@pytest.mark.L2
@pytest.mark.cashier_pos
@pytest.mark.skip(reason="requires two distinct outlets on the same tenant — TBD when dynamic onboarding lands")
def test_sell_with_outlet_terminal_mismatch_blocked() -> None:
    """Terminal belongs to outlet A, but X-Tch-Outlet-Id = outlet B → 403."""


@pytest.mark.L2
@pytest.mark.cashier_pos
@pytest.mark.skip(reason="requires two tenants with distinct cashiers — configure TCH_TENANT_2_* env vars")
def test_sell_with_other_seller_session_blocked() -> None:
    """Seller A token + session opened by seller B → 403."""


@pytest.mark.L2
@pytest.mark.cashier_pos
@pytest.mark.skip(reason="requires a draw with sales_close_at < now — TBD")
def test_sell_on_closed_draw_blocked() -> None:
    """Draw past its sales close time → sell must return business error."""


@pytest.mark.L2
@pytest.mark.cashier_pos
@pytest.mark.skip(reason="requires pricing odds to be absent for a game — TBD")
def test_sell_with_missing_pricing_odds_blocked() -> None:
    """No pricing_odds for the selected game/bet → sell must return pricing error."""


@pytest.mark.L2
@pytest.mark.cashier_pos
@pytest.mark.skip(reason="requires limit configuration with a very low stake cap — TBD")
def test_sell_exceeding_stake_limit_blocked() -> None:
    """Stake above configured limit → sell must return limit-exceeded error."""
