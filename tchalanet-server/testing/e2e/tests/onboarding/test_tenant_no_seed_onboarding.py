"""Tenant onboarding from scratch — NO seed.

End-to-end of the spec flow (dashboard-overview-runtime-v1 §tenant-provisioning):

    1. SUPER_ADMIN provisions a brand-new tenant + initial TENANT_ADMIN
       (POST /platform/tenant-onboarding/provision).
    2. The freshly provisioned tenant is structurally INCOMPLETE — readiness shows
       users/outlets/terminals MISSING (identity READY).
    3. The config is completed *without any seed*, using only REST endpoints. The
       SUPER_ADMIN acts on behalf of the new tenant via the X-Tenant-Id override
       (`with_tenant`) — the optional "super admin finishes the config" path; the
       tenant admin could do the same once they have Keycloak credentials.
    4. Readiness then rolls up to READY (the 4 V1-checked sections — identity,
       users, outlets, terminals — are all READY; the rest are UNKNOWN and do not
       block).

Also documents the known provisioning gap: a DEFAULT_HAITI_LOTTERY provision
*reports* games/pricing/draw_channels as seeded in `domainStatuses`, but nothing
is actually created for the new tenant (the orchestrator only creates tenant +
admin). See ``test_provisioned_tenant_has_no_operational_catalog``.
"""
from __future__ import annotations

import uuid
from typing import Any

import pytest

from flows.onboarding import OnboardingFlow
from tch_e2e.api_response import get_data
from tch_e2e.client import ApiClient

_PROFILE = "DEFAULT_HAITI_LOTTERY"
_TIMEZONE = "America/Port-au-Prince"


# ---------------------------------------------------------------------------
# helpers
# ---------------------------------------------------------------------------


def _run() -> str:
    return uuid.uuid4().hex[:8]


def _extract_id(obj: Any) -> str | None:
    """Pull an id out of a variety of response shapes (str / {value} / {id})."""
    if isinstance(obj, str):
        return obj
    if isinstance(obj, dict):
        for key in ("id", "outletId", "terminalId", "sellerId", "userId", "value"):
            raw = obj.get(key)
            if raw:
                return raw.get("value") if isinstance(raw, dict) else raw
    return None


def _provision_fresh_tenant(client: ApiClient, run: str) -> dict[str, Any]:
    """Provision a brand-new tenant with an initial admin; return the result data."""
    resp = client.post(
        "/platform/tenant-onboarding/provision",
        json={
            "code": f"e2e-noseed-{run}",
            "name": f"E2E NoSeed {run}",
            "type": "BORLETTE",
            "profile": _PROFILE,
            "timezone": _TIMEZONE,
            "currency": "HTG",
            "initialAdminEmail": f"admin-{run}@e2e.local",
        },
    )
    if resp.status_code in (404, 405):
        pytest.skip("tenant-onboarding/provision endpoint not routed")
    data = get_data(resp)
    assert data.get("tenantId"), f"provision returned no tenantId: {data}"
    return data


def _section_status(readiness: dict[str, Any]) -> dict[str, str]:
    """Map section id -> status from a TenantReadinessView dict."""
    return {s["id"]: s["status"] for s in readiness.get("sections", [])}


# ---------------------------------------------------------------------------
# fixtures
# ---------------------------------------------------------------------------


@pytest.fixture(scope="module")
def provisioned_tenant(super_admin_client: ApiClient) -> dict[str, Any]:
    """A freshly provisioned tenant shared by the read-only assertions."""
    return _provision_fresh_tenant(super_admin_client, _run())


# ===========================================================================
# L2 — a freshly provisioned tenant is structurally incomplete
# ===========================================================================


@pytest.mark.L2
@pytest.mark.onboarding
def test_fresh_tenant_identity_ready_but_structure_missing(
    provisioned_tenant: dict[str, Any],
) -> None:
    """Provision result readiness: identity READY, users/outlets/terminals MISSING."""
    readiness = provisioned_tenant.get("readiness")
    assert readiness, f"provision result has no readiness view: {provisioned_tenant}"

    statuses = _section_status(readiness)
    assert statuses.get("identity") == "READY", f"identity should be READY: {statuses}"
    for section in ("users", "outlets", "terminals"):
        assert statuses.get(section) == "MISSING", (
            f"fresh tenant section {section!r} should be MISSING, got {statuses.get(section)!r}"
        )
    assert readiness.get("status") == "MISSING", (
        f"fresh tenant overall readiness should be MISSING: {readiness.get('status')!r}"
    )


@pytest.mark.L2
@pytest.mark.onboarding
def test_fresh_tenant_admin_was_created(provisioned_tenant: dict[str, Any]) -> None:
    """initialAdminEmail was provided → an initial TENANT_ADMIN user exists."""
    assert provisioned_tenant.get("initialAdminUserId"), (
        f"no initialAdminUserId — admin not created during provision: {provisioned_tenant}"
    )
    assert "CREATE_INITIAL_ADMIN" not in provisioned_tenant.get("nextSteps", [])


# ===========================================================================
# L2 — completing the config (no seed) drives readiness to READY
# ===========================================================================


@pytest.mark.L2
@pytest.mark.onboarding
def test_super_admin_completes_fresh_tenant_to_ready(
    super_admin_client: ApiClient,
) -> None:
    """SUPER_ADMIN provisions then completes a fresh tenant via REST → readiness READY.

    Drives the full structural onboarding with no seed: outlet → cashier user →
    seller (linked + assigned) → terminal, all on the brand-new tenant via the
    X-Tenant-Id override, then asserts the 4 V1-checked readiness sections flip to
    READY and the tenant rolls up to READY.
    """
    run = _run()
    provisioned = _provision_fresh_tenant(super_admin_client, run)
    tenant_id = provisioned["tenantId"]

    # 0) Apply a plan so the tenant has entitlements (limits.outlets.max etc.).
    #    Provisioning does not attach a subscription, so without this step
    #    POST /admin/outlets fails ("Missing entitlement limit: limits.outlets.max").
    apply = super_admin_client.post(
        f"/platform/subscriptions/{tenant_id}/apply",
        json={"planCode": "STANDARD", "effectiveAt": "2020-01-01T00:00:00Z"},
    )
    if apply.status_code in (404, 405):
        pytest.skip("subscription apply endpoint not routed")
    assert apply.status_code in (200, 201), (
        f"could not apply plan to fresh tenant: {apply.status_code} {apply.text}"
    )

    # SUPER_ADMIN acts as the new tenant (sanctioned override).
    admin = super_admin_client.with_tenant(tenant_id, override_reason="e2e-no-seed-onboarding")
    flow = OnboardingFlow(admin)

    # 1) Outlet (slug is required by CreateOutletRequest -> outlet.slug NOT NULL)
    outlet = flow.create_outlet_admin(name=f"NoSeed Outlet {run}", slug=f"noseed-{run}")
    outlet_id = _extract_id(outlet)
    assert outlet_id, f"could not resolve created outlet id: {outlet}"

    # 2) Cashier identity user (DB + Keycloak) — role=CASHIER requires an outletId
    user = flow.create_identity_user(
        email=f"cashier-{run}@e2e.local", role="CASHIER", outletId=outlet_id)
    user_id = _extract_id(user)
    assert user_id, f"could not resolve created user id: {user}"

    # 3) Seller linked to the user + assigned to the outlet (flips `users` → READY)
    seller = flow.ensure_seller_for_user(user_id=user_id, outlet_id=outlet_id,
                                         display_name=f"NoSeed Seller {run}",
                                         code=f"noseed-{run}")
    assert seller, "seller creation/assignment did not complete"

    # 4) Terminal on the outlet
    terminal = flow.create_terminal_admin(outlet_id=outlet_id, label=f"NoSeed Terminal {run}")
    assert _extract_id(terminal), f"could not resolve created terminal id: {terminal}"

    # Readiness must now roll up to READY (identity/users/outlets/terminals READY).
    overview = get_data(admin.get("/admin/overview"))
    statuses = {s["id"]: s["status"] for s in overview["sections"]}
    for section in ("identity", "users", "outlets", "terminals"):
        assert statuses.get(section) == "READY", (
            f"after completion section {section!r} should be READY, got "
            f"{statuses.get(section)!r}; full={statuses}"
        )
    assert overview["status"] == "READY", (
        f"completed tenant overall readiness should be READY, got {overview['status']!r}; "
        f"sections={statuses}"
    )


# ===========================================================================
# L2 — documented gap: provision does NOT seed the operational catalog
# ===========================================================================


@pytest.mark.L2
@pytest.mark.onboarding
def test_provisioned_tenant_has_no_operational_catalog(
    provisioned_tenant: dict[str, Any],
) -> None:
    """GAP: DEFAULT_HAITI_LOTTERY provision *reports* games/pricing/draw_channels
    seeded in domainStatuses, but nothing is actually created for the new tenant.

    The orchestrator only creates tenant + initial admin; games/pricing/draw_channels
    are NOT copied (the "channels pas copiés" gap). This test pins the current
    behavior so the gap is visible and tracked: the readiness sections for
    games_pricing / draws stay UNKNOWN (never checked / never seeded), even though
    domainStatuses advertises DEFAULT_LOTTERY / DEFAULT_HAITI.
    """
    domain_statuses = provisioned_tenant.get("domainStatuses", {})
    # provision advertises a Haiti lottery catalog...
    assert domain_statuses.get("games") == "DEFAULT_LOTTERY", domain_statuses
    assert domain_statuses.get("draw_channels") == "DEFAULT_HAITI", domain_statuses

    # ...but readiness never reflects an actual operational catalog for the tenant.
    statuses = _section_status(provisioned_tenant["readiness"])
    assert statuses.get("games_pricing") == "UNKNOWN", (
        f"games_pricing unexpectedly {statuses.get('games_pricing')!r} — if the orchestrator "
        "now seeds games/pricing, update this gap test."
    )
    assert statuses.get("draws") == "UNKNOWN", (
        f"draws unexpectedly {statuses.get('draws')!r} — if the orchestrator now seeds "
        "draw channels/draws, update this gap test."
    )
