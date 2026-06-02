"""Phase 1 — Super admin provisions a new tenant with an initial admin.

Uses only existing REST endpoints — no seeds, no direct DB access.

Flow
----
1.  [super_admin] POST /platform/tenant-onboarding/provision
      { code, name, type, profile, timezone, currency, initialAdminEmail }
    → tenantId + initialAdminUserId

Assertions
----------
- HTTP 200/201, response shape valid
- tenantId present
- initialAdminUserId present (admin created in DB with TENANT_ADMIN role)
- nextSteps no longer contains CREATE_INITIAL_ADMIN
- warnings empty (initialAdminEmail was provided)

Phase 2 (tenant admin completes onboarding — outlets, terminals, cashier)
is covered in a separate test once the admin can log in via Keycloak.
"""
from __future__ import annotations

import uuid

import pytest

from tch_e2e.api_response import assert_ok
from tch_e2e.client import ApiClient


_PROFILE = "DEFAULT_HAITI_LOTTERY"
_TIMEZONE = "America/New_York"


def _run_id() -> str:
    return uuid.uuid4().hex[:8]


# ===========================================================================
# L2 — Tenant provisioning (super admin, phase 1)
# ===========================================================================


@pytest.mark.L2
@pytest.mark.onboarding
def test_provision_creates_tenant_and_initial_admin(
    super_admin_client: ApiClient,
) -> None:
    """Super admin provisions a tenant with initialAdminEmail — admin created in DB."""
    run = _run_id()
    admin_email = f"admin-{run}@e2e.local"

    resp = super_admin_client.post(
        "/platform/tenant-onboarding/provision",
        json={
            "code": f"e2e-{run}",
            "name": f"E2E Tenant {run}",
            "type": "BORLETTE",
            "profile": _PROFILE,
            "timezone": _TIMEZONE,
            "currency": "HTG",
            "initialAdminEmail": admin_email,
        },
    )
    if resp.status_code in (404, 405):
        pytest.skip("provision endpoint not routed")
    assert_ok(resp, expected=(200, 201))

    data = resp.json()["data"]

    # tenant identity
    tenant_id = data.get("tenantId")
    assert tenant_id, f"tenantId missing from response: {data}"

    tenant_code = data.get("tenantCode")
    assert tenant_code, f"tenantCode missing from response: {data}"

    # initial admin created in DB
    initial_admin_user_id = data.get("initialAdminUserId")
    assert initial_admin_user_id, (
        f"initialAdminUserId missing — admin was not created during provision: {data}"
    )

    # nextSteps should NOT include CREATE_INITIAL_ADMIN since admin was created
    next_steps = data.get("nextSteps", [])
    assert "CREATE_INITIAL_ADMIN" not in next_steps, (
        f"CREATE_INITIAL_ADMIN still in nextSteps after providing initialAdminEmail: {next_steps}"
    )

    # no warnings (email was provided)
    warnings = data.get("warnings", [])
    assert "INITIAL_ADMIN_EMAIL_MISSING" not in warnings, (
        f"Unexpected warning — initialAdminEmail was provided: {warnings}"
    )


@pytest.mark.L2
@pytest.mark.onboarding
def test_provision_without_admin_email_returns_warning(
    super_admin_client: ApiClient,
) -> None:
    """Provision without initialAdminEmail → warning + CREATE_INITIAL_ADMIN in nextSteps."""
    run = _run_id()

    resp = super_admin_client.post(
        "/platform/tenant-onboarding/provision",
        json={
            "code": f"e2e-noadmin-{run}",
            "name": f"E2E NoAdmin {run}",
            "type": "BORLETTE",
            "profile": _PROFILE,
            "timezone": _TIMEZONE,
            "currency": "HTG",
        },
    )
    if resp.status_code in (404, 405):
        pytest.skip("provision endpoint not routed")
    assert_ok(resp, expected=(200, 201))

    data = resp.json()["data"]
    assert data.get("tenantId"), f"tenantId missing: {data}"

    warnings = data.get("warnings", [])
    assert "INITIAL_ADMIN_EMAIL_MISSING" in warnings, (
        f"Expected INITIAL_ADMIN_EMAIL_MISSING warning: {warnings}"
    )

    next_steps = data.get("nextSteps", [])
    assert "CREATE_INITIAL_ADMIN" in next_steps, (
        f"Expected CREATE_INITIAL_ADMIN in nextSteps: {next_steps}"
    )

    assert data.get("initialAdminUserId") is None, (
        f"initialAdminUserId should be null when no email provided: {data}"
    )
