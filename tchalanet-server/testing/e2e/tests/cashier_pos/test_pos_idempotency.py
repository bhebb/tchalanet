"""POS ticket sell — idempotency tests.

Same idempotency key + same payload → same ticket (not a duplicate).
Same idempotency key + different payload → conflict (409 or rejected).

These tests require an OPEN session and an available draw and will
be skipped via ``onboard_cashier_for_pos`` when the same-day session
constraint is active.
"""
from __future__ import annotations

import uuid

import pytest

from fixtures.pos_context import PosContext


# ===========================================================================
# L2 — Idempotent resend
# ===========================================================================


@pytest.mark.L2
@pytest.mark.cashier_pos
def test_sell_same_key_same_payload_is_idempotent(
    onboard_cashier_for_pos: PosContext,
    super_admin_client,
    seed_ids,
) -> None:
    """Sending the same idempotency key + payload twice must return the same ticket."""
    ctx = onboard_cashier_for_pos
    draw = ctx.first_open_draw()
    if draw is None:
        pytest.skip("No OPEN draw available.")

    game_codes = draw.get("gameCodes") or []
    if not game_codes:
        pytest.skip(f"Draw {draw.get('drawId')} exposes no gameCodes.")

    game_code = game_codes[0]
    flow = ctx.cashier_flow()
    payload = flow._sale_payload(draw, [game_code])
    idem_key = str(uuid.uuid4())

    preparation = flow.prepare(payload)

    # First confirm — must be accepted
    first_data = flow.confirm_prepared_sale(preparation["preparationId"], idem_key)
    first_ticket_id = first_data.get("ticketId")
    assert first_data.get("outcome") == "ACCEPTED", (
        f"First sell must be ACCEPTED: {first_data}"
    )

    # Second confirm — same key, same preparation → same ticket
    second = flow._post_confirm(preparation["preparationId"], idem_key)
    # Idempotent response: 200/201 with same ticket, or 409 with same ticket reference
    assert second.status_code in (200, 201, 409), (
        f"Idempotent resend must return 2xx or 409, got {second.status_code}: {second.text[:300]}"
    )

    if second.status_code in (200, 201):
        second_data = flow._flatten_confirm_response(second.json()["data"])
        second_ticket_id = second_data.get("ticketId")
        assert second_ticket_id == first_ticket_id, (
            f"Idempotent resend returned a different ticketId: "
            f"{first_ticket_id!r} vs {second_ticket_id!r}"
        )


@pytest.mark.L2
@pytest.mark.cashier_pos
def test_sell_same_key_different_payload_returns_conflict(
    onboard_cashier_for_pos: PosContext,
) -> None:
    """Same idempotency key with a different payload → 409 conflict."""
    ctx = onboard_cashier_for_pos
    draw = ctx.first_open_draw()
    if draw is None:
        pytest.skip("No OPEN draw available.")

    game_codes = draw.get("gameCodes") or []
    if len(game_codes) < 1:
        pytest.skip(f"Draw {draw.get('drawId')} exposes no gameCodes.")

    flow = ctx.cashier_flow()
    idem_key = str(uuid.uuid4())

    # First preparation with selection "11"
    payload_a = flow._sale_payload(draw, [game_codes[0]])
    # Force a different selection for payload_b
    payload_b = {
        **payload_a,
        "lines": [{**payload_a["lines"][0], "selection": "99"}],
    }

    preparation_a = flow.prepare(payload_a)
    first_data = flow.confirm_prepared_sale(preparation_a["preparationId"], idem_key)
    assert first_data.get("outcome") == "ACCEPTED", first_data

    # Second preparation + confirm with the same idempotency key must conflict.
    preparation_b = flow.prepare(payload_b)
    second = flow._post_confirm(preparation_b["preparationId"], idem_key)
    assert second.status_code in (409, 422, 400), (
        f"Different preparation on same idempotency key must return 4xx conflict, "
        f"got {second.status_code}: {second.text[:300]}"
    )
