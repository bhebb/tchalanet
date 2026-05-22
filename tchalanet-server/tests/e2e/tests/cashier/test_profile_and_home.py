"""Profile + Home access flows.

Covers:
    * GET  /tenant/me/profile           — current user landing payload
    * PATCH /tenant/me/profile          — mutate firstName, round-trip

    Cashier home (`/tenant/cashier/home`) branches in `CashierHomeService.mobileHome`:
        a) no operational context        → requiredStep=SELECT_OPERATIONAL_CONTEXT
        b) context OK but session closed → requiredStep=OPEN_SESSION
        c) context OK + session open     → full home (primaryAction=SELL_TICKET)
        d) surface mismatch              → 403 surface.not_allowed
    Plus the web variant `/tenant/cashier/web-home` (widgets, no requiredStep).

Test order matters for the home block: alphabetical `a/b/c/d/e` is intentional so
the session-closed test leaves a re-opened session for downstream tests.
"""
from __future__ import annotations

import random
import string

import pytest

from flows.home import HomeFlow
from flows.profile import ProfileFlow
from lib.api import ApiClient, assert_ok
from lib.context import CashierContext
from prereqs.app_user import ensure_app_user_synced
from prereqs.session import ensure_pos_session_open


def _random_letters(n: int) -> str:
    return "".join(random.choices(string.ascii_lowercase, k=n))


def _skip_if_not_provisioned(client: ApiClient) -> dict:
    """Probe `GET /tenant/me/profile`. If the cashier isn't yet linked to an
    `app_user` row (UserBootstrapFilter → 403 "User not provisioned"), skip the
    test instead of erroring the whole suite — this is the documented bypass
    when the SUPER_ADMIN sync endpoint is unreachable on the local env.
    """
    response = client.get("/tenant/me/profile")
    if response.status_code == 403 and "not provisioned" in response.text.lower():
        pytest.skip(
            "cashier user has no app_user row and /platform/ops/sync is inaccessible "
            "— grant SUPER_ADMIN to the test admin (or run the sync manually) and retry"
        )
    assert_ok(response)
    return response.json()["data"]


# --- profile -----------------------------------------------------------------


@pytest.mark.profile
def test_user_can_read_profile(super_admin_client: ApiClient, cashier_client: ApiClient) -> None:
    ensure_app_user_synced(super_admin_client)

    me = ProfileFlow(cashier_client).me()

    assert me["id"], "MeResponse must expose a user id"
    assert me["username"], "MeResponse must expose a username"
    assert me["roles"], "cashier user must expose at least one role"
    landing = me["landing"]
    assert landing["preferredSurface"], "preferredSurface must be set"
    assert landing["availableSurfaces"], "availableSurfaces must not be empty"


@pytest.mark.profile
def test_user_can_modify_profile(super_admin_client: ApiClient, cashier_client: ApiClient) -> None:
    ensure_app_user_synced(super_admin_client)
    profile = ProfileFlow(cashier_client)

    before = profile.me()
    original = before.get("firstName") or "cashier"
    new_first_name = (original[:-2] if len(original) > 2 else original) + _random_letters(2)
    assert new_first_name != original, "mutation must change firstName"

    updated = profile.update(first_name=new_first_name)
    assert updated["firstName"] == new_first_name, f"PATCH didn't return new firstName: {updated}"

    after = profile.me()
    assert after["firstName"] == new_first_name, (
        f"GET didn't return the patched firstName (expected={new_first_name}, "
        f"got={after.get('firstName')})"
    )


# --- home --------------------------------------------------------------------


def _assert_required_step(payload: dict, expected_type: str) -> None:
    required_step = payload.get("requiredStep")
    assert required_step, f"expected requiredStep={expected_type}, got payload={payload}"
    assert required_step["type"] == expected_type, (
        f"unexpected requiredStep.type: {required_step.get('type')} (expected {expected_type})"
    )


@pytest.mark.profile
def test_a_mobile_home_requires_operational_context(
    super_admin_client: ApiClient, cashier_client: ApiClient
) -> None:
    """No X-Tch-Outlet-Id / X-Tch-Terminal-Id headers → home demands setup."""
    ensure_app_user_synced(super_admin_client)

    # Empty context → no X-Tch-* headers are emitted.
    home = HomeFlow(cashier_client, context=CashierContext())
    payload = home.mobile_home(surface="MOBILE_POS")

    assert payload["surface"] == "MOBILE_POS"
    _assert_required_step(payload, "SELECT_OPERATIONAL_CONTEXT")

    primary = payload["primaryAction"]
    assert primary["kind"] == "SELECT_OPERATIONAL_CONTEXT", primary
    assert primary["href"] == "/operational-context/select", primary

    # In this branch quickActions are empty and only profile remains in nav.
    assert payload["quickActions"] == [], payload["quickActions"]
    nav_ids = [n["id"] for n in payload["navigation"]]
    assert nav_ids == ["profile"], nav_ids
    # primaryDraw / session must be absent.
    assert payload["session"] is None
    assert payload["primaryDraw"] is None


@pytest.mark.profile
def test_b_mobile_home_requires_open_session(
    super_admin_client: ApiClient,
    cashier_client: ApiClient,
    cashier_context: CashierContext,
) -> None:
    """Context OK but no open session → requiredStep=OPEN_SESSION.

    Closes the current session (re-opening it if needed), calls home, then
    re-opens a session so downstream tests are unaffected.
    """
    ensure_app_user_synced(super_admin_client)
    ctx = ensure_pos_session_open(cashier_client, cashier_context)

    # Close it so the home service sees no open session.
    close = cashier_client.post(
        "/tenant/cashier/session/close",
        json={
            "sessionId": ctx.session_id,
            "closingAmount": "100.00",
            "reason": "e2e:test_b_mobile_home_requires_open_session",
        },
        context=ctx,
    )
    assert_ok(close)

    try:
        # Re-issue context without the now-stale sessionId so the X-Tch-Sales-Session-Id
        # header doesn't point to a closed session.
        ctx_no_session = CashierContext(outlet_id=ctx.outlet_id, terminal_id=ctx.terminal_id)
        payload = HomeFlow(cashier_client, context=ctx_no_session).mobile_home(surface="MOBILE_POS")

        assert payload["surface"] == "MOBILE_POS"
        _assert_required_step(payload, "OPEN_SESSION")

        primary = payload["primaryAction"]
        assert primary["kind"] == "OPEN_SESSION", primary
        assert primary["href"] == "/session/open", primary

        quick_kinds = [a["kind"] for a in payload["quickActions"]]
        assert quick_kinds == ["PROFILE"], quick_kinds

        nav_ids = [n["id"] for n in payload["navigation"]]
        assert nav_ids == ["profile"], nav_ids

        # operational context is reported as ready/trusted in this branch.
        op = payload["operationalContext"]
        assert op["ready"] is True and op["trusted"] is True, op
        # session block is present but closed.
        session = payload["session"]
        assert session is not None and session["open"] is False, session
    finally:
        # Restore: re-open a session for downstream tests.
        ensure_pos_session_open(cashier_client, cashier_context)


@pytest.mark.profile
def test_c_mobile_home_happy_path(
    super_admin_client: ApiClient,
    cashier_client: ApiClient,
    cashier_context: CashierContext,
) -> None:
    """Full POS home: context + open session → primary action = SELL_TICKET."""
    ensure_app_user_synced(super_admin_client)
    ctx = ensure_pos_session_open(cashier_client, cashier_context)

    payload = HomeFlow(cashier_client, context=ctx).mobile_home(surface="MOBILE_POS")

    assert payload["surface"] == "MOBILE_POS"
    assert payload.get("requiredStep") is None, payload.get("requiredStep")

    primary = payload["primaryAction"]
    assert primary["kind"] == "SELL_TICKET", primary
    assert primary["href"] == "/sell", primary

    quick_kinds = [a["kind"] for a in payload["quickActions"]]
    assert quick_kinds == ["RECENT_TICKETS", "SESSION", "PROFILE"], quick_kinds

    nav_ids = [n["id"] for n in payload["navigation"]]
    assert nav_ids == ["sell", "tickets", "session", "profile"], nav_ids

    widget_ids = {w["id"] for w in payload["widgets"]}
    assert widget_ids == {"session_status", "primary_draw"}, widget_ids

    session = payload["session"]
    assert session is not None and session["open"] is True, session
    op = payload["operationalContext"]
    assert op["ready"] is True and op["trusted"] is True, op


@pytest.mark.profile
def test_d_web_home_returns_widgets(
    super_admin_client: ApiClient,
    cashier_client: ApiClient,
    cashier_context: CashierContext,
) -> None:
    """Web surface: widget-driven dashboard, no requiredStep regardless of state."""
    ensure_app_user_synced(super_admin_client)
    ctx = ensure_pos_session_open(cashier_client, cashier_context)

    payload = HomeFlow(cashier_client, context=ctx).web_home(surface="CASHIER_WEB")

    assert payload["surface"] == "CASHIER_WEB"
    assert payload.get("requiredStep") is None, payload.get("requiredStep")

    primary = payload["primaryAction"]
    assert primary["kind"] == "SELL_TICKET", primary
    assert primary["href"] == "/cashier/sell", primary

    widget_ids = {w["id"] for w in payload["widgets"]}
    assert widget_ids == {"session_summary", "next_draw", "recent_tickets"}, widget_ids


@pytest.mark.profile
def test_e_mobile_home_rejects_wrong_surface(
    super_admin_client: ApiClient, cashier_client: ApiClient
) -> None:
    """Asking the mobile endpoint for the web surface (or vice versa) must 403."""
    ensure_app_user_synced(super_admin_client)

    response = cashier_client.get(
        "/tenant/cashier/home",
        headers={"X-Tch-Surface": "CASHIER_WEB"},
    )
    assert response.status_code == 403, (
        f"expected 403 surface.not_allowed, got {response.status_code}: {response.text}"
    )

    response = cashier_client.get(
        "/tenant/cashier/web-home",
        headers={"X-Tch-Surface": "MOBILE_POS"},
    )
    assert response.status_code == 403, (
        f"expected 403 surface.not_allowed, got {response.status_code}: {response.text}"
    )
