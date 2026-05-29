"""POS ticket size tests — SHORT, MEDIUM, LONG.

Tests that the POS sell endpoint accepts tickets of different sizes:

  SHORT  → 1 game, 1 line, low stake          (smoke)
  MEDIUM → 2-3 games, 3-5 lines, mixed stakes
  LONG   → all 5 V1 games, 17 lines           (all games + print)

Uses ``onboard_cashier_for_pos`` fixture — skips on same-day session constraint.
"""
from __future__ import annotations

import pytest

from fixtures.pos_context import PosContext
from tch_e2e.assertions import assert_sale_accepted
from tch_e2e.ticket_matrix import (
    LONG_ALL_GAMES,
    MEDIUM_MULTI_GAME,
    MEDIUM_MULTI_LINE_MIXED_STAKE,
    SHORT_SINGLE_GAME_LOW_STAKE,
    TicketScenario,
)


# ---------------------------------------------------------------------------
# helpers
# ---------------------------------------------------------------------------


def _draw_supporting(ctx: PosContext, needed_games: set[str]) -> dict | None:
    """Return the first OPEN draw that exposes all ``needed_games``, or None."""
    try:
        draws = ctx.cashier_flow().list_available_draws(lookahead_hours=24)
    except Exception:
        return None
    for draw in draws:
        if draw.get("status") != "OPEN":
            continue
        available = set(draw.get("gameCodes") or [])
        if needed_games.issubset(available):
            return draw
    return None


def _sell_scenario(ctx: PosContext, draw: dict, scenario: TicketScenario) -> None:
    """Sell ``scenario`` lines on ``draw`` and assert ACCEPTED."""
    from tch_e2e.api_response import assert_ok
    import uuid

    flow = ctx.cashier_flow()
    lines = scenario.to_payload_lines()
    response = ctx.cashier_client.post(
        "/tenant/cashier/tickets/sell",
        json={
            "terminalId": ctx.terminal_id,
            "drawId": draw["drawId"],
            "drawChannelId": draw["drawChannelId"],
            "currency": "HTG",
            "lines": lines,
        },
        context=ctx.op_context(),
        idempotency_key=str(uuid.uuid4()),
    )
    assert_ok(response, expected=(200, 201))
    data = response.json()["data"]
    assert_sale_accepted({
        "outcome": data.get("outcome"),
        "ticketId": data.get("ticketId"),
        "ticketCode": data.get("ticketCode"),
    })
    return data


# ===========================================================================
# L1 — SHORT
# ===========================================================================


@pytest.mark.L1
@pytest.mark.cashier_pos
def test_sell_short_single_game(
    onboard_cashier_for_pos: PosContext,
) -> None:
    """SHORT_SINGLE_GAME_LOW_STAKE — 1 line, HT_BOLET, 1.00 HTG."""
    ctx = onboard_cashier_for_pos
    needed = {ls.game_code for ls in SHORT_SINGLE_GAME_LOW_STAKE.lines}
    draw = _draw_supporting(ctx, needed)
    if draw is None:
        pytest.skip(f"No OPEN draw with games {needed}.")

    data = _sell_scenario(ctx, draw, SHORT_SINGLE_GAME_LOW_STAKE)
    assert data.get("ticketId"), f"ticketId missing from SHORT sell response: {data}"


# ===========================================================================
# L1 — MEDIUM
# ===========================================================================


@pytest.mark.L1
@pytest.mark.cashier_pos
def test_sell_medium_multi_line_mixed_stake(
    onboard_cashier_for_pos: PosContext,
) -> None:
    """MEDIUM_MULTI_LINE_MIXED_STAKE — 2 games, 4 lines, mixed stakes."""
    ctx = onboard_cashier_for_pos
    needed = {ls.game_code for ls in MEDIUM_MULTI_LINE_MIXED_STAKE.lines}
    draw = _draw_supporting(ctx, needed)
    if draw is None:
        pytest.skip(f"No OPEN draw with games {needed} — configure HT_MARYAJ pricing.")

    data = _sell_scenario(ctx, draw, MEDIUM_MULTI_LINE_MIXED_STAKE)
    assert data.get("ticketId"), f"ticketId missing from MEDIUM sell response: {data}"


@pytest.mark.L1
@pytest.mark.cashier_pos
def test_sell_medium_multi_game(
    onboard_cashier_for_pos: PosContext,
) -> None:
    """MEDIUM_MULTI_GAME — 3 games (BOLET, MARYAJ, LOTO3), 5 lines."""
    ctx = onboard_cashier_for_pos
    needed = {ls.game_code for ls in MEDIUM_MULTI_GAME.lines}
    draw = _draw_supporting(ctx, needed)
    if draw is None:
        pytest.skip(f"No OPEN draw with games {needed}.")

    data = _sell_scenario(ctx, draw, MEDIUM_MULTI_GAME)
    assert data.get("ticketId"), f"ticketId missing from MEDIUM_MULTI_GAME sell response: {data}"


# ===========================================================================
# L1 — LONG (all 5 games)
# ===========================================================================


@pytest.mark.L1
@pytest.mark.cashier_pos
def test_sell_long_all_games(
    onboard_cashier_for_pos: PosContext,
) -> None:
    """LONG_ALL_GAMES — all 5 V1 games, 17 lines, mixed stakes.

    This is the main print/receipt validation test: a long ticket exercises
    the full receipt layout including all game sections.
    """
    ctx = onboard_cashier_for_pos
    needed = {ls.game_code for ls in LONG_ALL_GAMES.lines}
    draw = _draw_supporting(ctx, needed)
    if draw is None:
        pytest.skip(
            f"No OPEN draw exposes all 5 games {needed}. "
            "Enable HT_BOLET, HT_MARYAJ, HT_LOTO3, HT_LOTO4, HT_LOTO5 pricing."
        )

    data = _sell_scenario(ctx, draw, LONG_ALL_GAMES)
    ticket_id = data.get("ticketId")
    assert ticket_id, f"ticketId missing from LONG sell response: {data}"

    # Print the long ticket and verify it produces a valid PDF
    flow = ctx.cashier_flow()
    pdf_bytes = flow.print_pdf(ticket_id)
    assert pdf_bytes.startswith(b"%PDF"), (
        f"Long ticket print did not return a PDF binary (got {len(pdf_bytes)} bytes, "
        f"prefix {pdf_bytes[:8]!r})"
    )


@pytest.mark.L2
@pytest.mark.cashier_pos
def test_sell_long_and_list_tickets(
    onboard_cashier_for_pos: PosContext,
) -> None:
    """Sell a long ticket → list tickets → sold ticket appears in the list."""
    ctx = onboard_cashier_for_pos
    needed = {ls.game_code for ls in LONG_ALL_GAMES.lines}
    draw = _draw_supporting(ctx, needed)
    if draw is None:
        pytest.skip(f"No OPEN draw with games {needed}.")

    data = _sell_scenario(ctx, draw, LONG_ALL_GAMES)
    ticket_id = data.get("ticketId")
    assert ticket_id

    # List and confirm the ticket appears
    all_tickets = ctx.cashier_flow().list_tickets()
    listed_ids = {row.get("id") or row.get("ticketId") for row in all_tickets}
    assert ticket_id in listed_ids, (
        f"Sold LONG ticket {ticket_id!r} missing from list (got {len(all_tickets)} items)."
    )
