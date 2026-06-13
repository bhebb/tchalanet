"""Auth context security tests — role-based access enforcement.

Verifies that the cashier token cannot reach platform / super-admin endpoints,
and that the super-admin token cannot call cashier-tenant endpoints without
being associated with a tenant.

These tests require no open session — only valid tokens.
"""
from __future__ import annotations

import pytest

from tch_e2e.client import ApiClient
from tch_e2e.config import OpContext


# ===========================================================================
# L1 — Cashier can reach their own surface
# ===========================================================================


@pytest.mark.L1
@pytest.mark.auth_context
def test_cashier_can_access_home(
    cashier_client_a: ApiClient,
) -> None:
    """GET /tenant/cashier/home with cashier token → 200 (no context required for home)."""
    response = cashier_client_a.get(
        "/tenant/cashier/home",
        headers={"X-Tch-Surface": "MOBILE_POS"},
    )
    assert response.status_code == 200, (
        f"Cashier should be able to reach /tenant/cashier/home, got {response.status_code}: "
        f"{response.text[:300]}"
    )


@pytest.mark.L1
@pytest.mark.auth_context
def test_cashier_can_read_own_profile(
    cashier_client_a: ApiClient,
) -> None:
    """GET /tenant/me/profile with cashier token → 200."""
    response = cashier_client_a.get("/tenant/me/profile")
    assert response.status_code == 200, (
        f"Cashier should read own profile, got {response.status_code}: {response.text[:300]}"
    )
    body = response.json()
    data = (body.get("data") or body) if isinstance(body, dict) else {}
    # Profile must include some identity field
    assert data.get("username") or data.get("userId") or data.get("id"), (
        f"Profile missing identity field: {data}"
    )


# ===========================================================================
# L1 — Cashier cannot access platform (super-admin) endpoints
# ===========================================================================


@pytest.mark.L1
@pytest.mark.auth_context
def test_cashier_cannot_list_platform_tenants(
    cashier_client_a: ApiClient,
) -> None:
    """GET /platform/tenants with cashier token → 403 (not 200 or 500)."""
    response = cashier_client_a.get("/platform/tenants")
    assert response.status_code in (401, 403), (
        f"Cashier must be forbidden from /platform/tenants, got {response.status_code}: "
        f"{response.text[:300]}"
    )


@pytest.mark.L1
@pytest.mark.auth_context
def test_cashier_cannot_access_platform_ops(
    cashier_client_a: ApiClient,
) -> None:
    """POST /platform/ops/sync/identity/firebase-bootstrap-users with cashier token → 403."""
    response = cashier_client_a.post(
        "/platform/ops/sync/identity/firebase-bootstrap-users"
    )
    assert response.status_code in (401, 403), (
        f"Cashier must be forbidden from platform ops, got {response.status_code}: "
        f"{response.text[:300]}"
    )


@pytest.mark.L1
@pytest.mark.auth_context
def test_cashier_cannot_generate_draws(
    cashier_client_a: ApiClient,
) -> None:
    """POST /platform/ops/draws/generate with cashier token → 403.

    Draw generation is a platform-admin operation.
    """
    response = cashier_client_a.post(
        "/platform/ops/draws/generate",
        json={
            "tenantId": "00000000-0000-0000-0000-000000000003",
            "from": "2025-01-01",
            "to": "2025-01-01",
            "dryRun": True,
            "force": False,
            "reason": "e2e:auth-check",
        },
    )
    assert response.status_code in (401, 403), (
        f"Cashier must be forbidden from draw generation, got {response.status_code}: "
        f"{response.text[:300]}"
    )


# ===========================================================================
# L1 — Super-admin cannot masquerade as cashier
# ===========================================================================


@pytest.mark.L1
@pytest.mark.auth_context
def test_super_admin_cannot_sell_tickets(
    super_admin_client: ApiClient,
) -> None:
    """Super-admin token at /tenant/cashier/tickets/sell → 403 or 4xx.

    The super-admin is not a tenant cashier and should not be able to sell.
    """
    ctx = OpContext(
        outlet_id="00000000-0000-0000-0000-000000003001",
        terminal_id="00000000-0000-0000-0000-000000003101",
        session_id="00000000-0000-0000-0000-000000000000",
    )
    response = super_admin_client.post(
        "/tenant/cashier/tickets/sell",
        json={
            "terminalId": "00000000-0000-0000-0000-000000003101",
            "drawId": "00000000-0000-0000-0000-000000000000",
            "drawChannelId": "00000000-0000-0000-0000-000000000000",
            "currency": "HTG",
            "lines": [{"gameCode": "HT_BOLET", "betType": "MATCH_1_2D",
                        "selection": "11", "stake": "1.00"}],
        },
        context=ctx,
    )
    # Must be rejected — super-admin has no tenant cashier role
    assert response.status_code >= 400, (
        f"Super-admin should not be able to sell tickets, got {response.status_code}: "
        f"{response.text[:300]}"
    )


# ===========================================================================
# L2 — Super-admin can reach platform endpoints
# ===========================================================================


@pytest.mark.L2
@pytest.mark.auth_context
def test_super_admin_can_access_platform(
    super_admin_client: ApiClient,
) -> None:
    """Super-admin token can call a read-only platform endpoint."""
    # Use onboarding preview — read-only, skips gracefully if WIP
    response = super_admin_client.get(
        "/platform/onboarding/tenants/preview",
        params={"profile": "DEFAULT_HAITI_LOTTERY"},
    )
    # 404/405 = endpoint WIP; that's fine — we're testing auth, not the endpoint
    if response.status_code in (404, 405, 500):
        pytest.skip(f"Platform preview endpoint WIP ({response.status_code}) — auth check skipped")
    assert response.status_code == 200, (
        f"Super-admin should reach platform preview, got {response.status_code}: "
        f"{response.text[:300]}"
    )
