"""Ensure a POS session is open for the cashier on the given terminal."""
from __future__ import annotations

from tch_e2e.api_response import assert_ok
from tch_e2e.client import ApiClient
from tch_e2e.config import OpContext


def ensure_pos_session_open(
    cashier_client: ApiClient,
    context: OpContext,
    *,
    opening_float: str = "100.00",
    super_admin_client: ApiClient | None = None,
) -> OpContext:
    """Return `context` with `session_id` set. Opens a session if none is current.

    Handles the server constraint: one non-terminal session per user per outlet
    per business day.

    Flow:
    1. If there is an OPEN session for the terminal → reuse it.
    2. Try to open a new session.
    3. If 409 (duplicate-user-business-day) and `super_admin_client` is provided:
       a. Finalize the existing CLOSED session via
          PATCH /admin/sales-sessions/{id}/operational-controls/finalize
       b. Retry opening a new session.
    4. If still blocked and no admin client → pytest.skip.
    """
    import pytest

    # --- 1. check for an existing OPEN session for this terminal ---------------
    current = cashier_client.get(
        "/tenant/cashier/session/current",
        params={"terminalId": context.terminal_id},
    )
    if current.status_code == 200:
        body = current.json()
        data = body.get("data") if isinstance(body, dict) else None
        if isinstance(data, dict) and data.get("status") == "OPEN":
            return context.with_session(data["sessionId"])

    # --- 2. try to open a new session ------------------------------------------
    open_response = cashier_client.post(
        "/tenant/cashier/session/open",
        json={
            "outletId": context.outlet_id,
            "terminalId": context.terminal_id,
            "openingFloat": opening_float,
        },
    )

    if open_response.status_code not in (409,):
        assert_ok(open_response, expected=(200, 201))
        session = open_response.json()["data"]
        return context.with_session(session["sessionId"])

    body = open_response.json()
    code = body.get("code", "")
    if code != "sales.session.duplicate-user-business-day":
        # Unexpected conflict — re-raise
        assert_ok(open_response, expected=(200, 201))

    # --- 3. same-day constraint: find the CLOSED session and finalize ----------
    if super_admin_client is None:
        pytest.skip(
            "A closed session exists for this user/outlet/business-day. "
            "Pass super_admin_client to ensure_pos_session_open to auto-finalize it. "
            f"Server detail: {body.get('detail')}"
        )

    # Find the existing closed session for this terminal
    closed = _find_closed_session_for_terminal(cashier_client, context)
    if closed is None:
        pytest.skip(
            "same-day session conflict but couldn't locate the closed session to finalize. "
            f"Server detail: {body.get('detail')}"
        )

    session_id = closed["sessionId"]
    finalize = super_admin_client.patch(
        f"/admin/sales-sessions/{session_id}/operational-controls/finalize",
        json={"reason": "e2e:auto-finalize-for-session-reopen"},
    )
    if finalize.status_code not in (200, 201, 204):
        pytest.skip(
            f"Admin finalize of session {session_id} failed with {finalize.status_code}. "
            "Cannot reopen session today."
        )

    # --- 4. retry open after finalization --------------------------------------
    retry = cashier_client.post(
        "/tenant/cashier/session/open",
        json={
            "outletId": context.outlet_id,
            "terminalId": context.terminal_id,
            "openingFloat": opening_float,
        },
    )
    assert_ok(retry, expected=(200, 201))
    session = retry.json()["data"]
    return context.with_session(session["sessionId"])


def _find_closed_session_for_terminal(
    cashier_client: ApiClient,
    context: OpContext,
) -> dict | None:
    """Return the most recent CLOSED session for the terminal, if any."""
    # Try /tenant/cashier/session/current first (may return CLOSED too)
    current = cashier_client.get(
        "/tenant/cashier/session/current",
        params={"terminalId": context.terminal_id},
    )
    if current.status_code == 200:
        data = current.json().get("data") or {}
        if isinstance(data, dict) and data.get("status") == "CLOSED":
            return data

    # Fallback: try the generic sessions endpoint
    sessions = cashier_client.get(
        "/tenant/sessions/current",
        params={"terminalId": context.terminal_id},
    )
    if sessions.status_code == 200:
        data = sessions.json().get("data") or {}
        if isinstance(data, dict) and data.get("status") == "CLOSED":
            return data

    return None


def close_current_session_if_open(
    cashier_client: ApiClient,
    context: OpContext,
    *,
    closing_amount: str = "100.00",
    reason: str = "e2e:close_current_session_if_open",
) -> None:
    """Close current session when it exists and is OPEN; no-op otherwise."""
    current = cashier_client.get(
        "/tenant/cashier/session/current",
        params={"terminalId": context.terminal_id},
    )
    if current.status_code != 200:
        return

    body = current.json()
    data = body.get("data") if isinstance(body, dict) else None
    if not isinstance(data, dict) or data.get("status") != "OPEN":
        return

    close = cashier_client.post(
        "/tenant/cashier/session/close",
        json={
            "sessionId": data["sessionId"],
            "closingAmount": closing_amount,
            "reason": reason,
        },
        context=context.with_session(data["sessionId"]),
    )
    assert_ok(close)
