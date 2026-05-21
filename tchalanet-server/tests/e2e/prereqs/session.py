"""Ensure a POS session is open for the cashier on the given terminal."""
from __future__ import annotations

from lib.api import ApiClient, assert_ok
from lib.context import CashierContext


def ensure_pos_session_open(
    cashier_client: ApiClient,
    context: CashierContext,
    *,
    opening_float: str = "100.00",
) -> CashierContext:
    """Return `context` with `session_id` set. Opens a session if none is current."""
    current = cashier_client.get(
        "/tenant/cashier/session/current",
        params={"terminalId": context.terminal_id},
    )
    if current.status_code == 200:
        body = current.json()
        data = body.get("data") if isinstance(body, dict) else None
        if isinstance(data, dict) and data.get("status") == "OPEN":
            return context.with_session(data["sessionId"])

    # 204 No Content or non-OPEN → open a new session.
    open_response = cashier_client.post(
        "/tenant/cashier/session/open",
        json={
            "outletId": context.outlet_id,
            "terminalId": context.terminal_id,
            "openingFloat": opening_float,
        },
    )
    assert_ok(open_response, expected=(200, 201))
    session = open_response.json()["data"]
    return context.with_session(session["sessionId"])
