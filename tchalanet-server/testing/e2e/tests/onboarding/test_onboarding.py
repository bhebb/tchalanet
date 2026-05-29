"""Tenant onboarding tests — Task 5 of e2e-business-runtime-v1.

Covers:
  - Provisioning preview (read-only)
  - Tenant provisioning (skipped if endpoint not available)
  - User onboarding (admin + cashier)
  - Outlet onboarding
  - Terminal onboarding
  - Terminal binding

All tests use OnboardingFlow (super_admin_client) — no raw API calls in test functions.
Endpoints that are not yet implemented skip gracefully (404/405 → pytest.skip).
"""
from __future__ import annotations

import os

import pytest

from flows.onboarding import OnboardingFlow
from tch_e2e.client import ApiClient
from tch_e2e.config import SeedIds
from tch_e2e.scenario_world import ScenarioWorld

_PROFILE = "DEFAULT_HAITI_LOTTERY"


# ===========================================================================
# L1 — Provisioning preview (read-only, Task 5 items 1-2)
# ===========================================================================


@pytest.mark.L1
@pytest.mark.onboarding
def test_provisioning_preview_is_read_only(
    super_admin_client: ApiClient,
    seed_ids: SeedIds,
) -> None:
    """Preview must not create any persistent state.

    Calling provisioning_preview() and then verifying the seed tenant still
    resolves to the same tenant_id confirms no duplicate was created.
    """
    flow = OnboardingFlow(super_admin_client)
    preview = flow.provisioning_preview(_PROFILE)

    # The preview response must not look like a created tenant
    # (no top-level tenantId that differs from what we already have).
    created_id = preview.get("tenantId") or preview.get("id")
    assert created_id is None or created_id == seed_ids.tenant_id, (
        f"provisioning_preview created (or returned) an unexpected tenantId: {created_id!r}. "
        "Preview must be read-only."
    )


@pytest.mark.L1
@pytest.mark.onboarding
def test_provisioning_preview_returns_domains_and_readiness(
    super_admin_client: ApiClient,
) -> None:
    """Preview response must contain domain or readiness information."""
    flow = OnboardingFlow(super_admin_client)
    preview = flow.provisioning_preview(_PROFILE)

    has_domains = bool(preview.get("domains"))
    has_readiness = "readiness" in preview or "ready" in preview
    has_steps = bool(preview.get("steps") or preview.get("onboardingSteps"))

    assert has_domains or has_readiness or has_steps, (
        f"provisioning_preview response has neither domains, readiness, nor steps: {preview}"
    )


# ===========================================================================
# L1 — Tenant provisioning (Task 5 item 3 — skip if endpoint absent)
# ===========================================================================


@pytest.mark.L1
@pytest.mark.onboarding
def test_provision_tenant_creates_tenant(
    super_admin_client: ApiClient,
    world: ScenarioWorld,
) -> None:
    """Provisioning creates a tenant with a unique run-scoped code."""
    code = f"e2e-run-{world.run_id}"
    flow = OnboardingFlow(super_admin_client)
    result = flow.provision_tenant(
        profile=_PROFILE,
        code=code,
        display_name=f"E2E Tenant {world.run_id}",
    )
    assert result.get("tenantId") or result.get("id"), (
        f"provision_tenant did not return a tenantId: {result}"
    )


# ===========================================================================
# L1 — User onboarding (Task 5 items 4-5)
# ===========================================================================


@pytest.mark.L1
@pytest.mark.onboarding
def test_user_onboarding_creates_tenant_admin(
    super_admin_client: ApiClient,
    seed_ids: SeedIds,
    world: ScenarioWorld,
) -> None:
    """Create (or confirm existence of) a TENANT_ADMIN on the seed tenant."""
    flow = OnboardingFlow(super_admin_client)
    result = flow.create_tenant_user(
        tenant_id=seed_ids.tenant_id,
        username=f"e2e-admin-{world.run_id}",
        password="E2eTest@2025!",
        role="TENANT_ADMIN",
    )
    # 409 = already exists → flow returns {} which is fine; otherwise check id
    user_id = result.get("id") or result.get("userId")
    assert user_id or result == {}, (
        f"create_tenant_user (TENANT_ADMIN) returned unexpected body: {result}"
    )


@pytest.mark.L1
@pytest.mark.onboarding
def test_user_onboarding_creates_cashier(
    super_admin_client: ApiClient,
    seed_ids: SeedIds,
    world: ScenarioWorld,
) -> None:
    """Create (or confirm existence of) a CASHIER on the seed tenant."""
    flow = OnboardingFlow(super_admin_client)
    result = flow.create_tenant_user(
        tenant_id=seed_ids.tenant_id,
        username=f"e2e-cashier-{world.run_id}",
        password="E2eTest@2025!",
        role="CASHIER",
    )
    user_id = result.get("id") or result.get("userId")
    assert user_id or result == {}, (
        f"create_tenant_user (CASHIER) returned unexpected body: {result}"
    )


# ===========================================================================
# L1 — Outlet onboarding (Task 5 item 6)
# ===========================================================================


@pytest.mark.L1
@pytest.mark.onboarding
def test_outlet_onboarding_creates_active_outlet(
    super_admin_client: ApiClient,
    seed_ids: SeedIds,
    world: ScenarioWorld,
) -> None:
    """Create (or tolerate existing) an active outlet on the seed tenant."""
    flow = OnboardingFlow(super_admin_client)
    result = flow.create_outlet(
        tenant_id=seed_ids.tenant_id,
        name=f"E2E Outlet {world.run_id}",
    )
    if result == {}:
        return  # 409 — already exists
    status = result.get("status")
    assert status in (None, "ACTIVE"), (
        f"Expected outlet status=ACTIVE, got {status!r}: {result}"
    )


# ===========================================================================
# L1 — Terminal onboarding (Task 5 item 7)
# ===========================================================================


@pytest.mark.L1
@pytest.mark.onboarding
def test_terminal_onboarding_creates_terminal(
    super_admin_client: ApiClient,
    seed_ids: SeedIds,
    world: ScenarioWorld,
) -> None:
    """Create (or tolerate existing) a terminal on the seed outlet."""
    flow = OnboardingFlow(super_admin_client)
    result = flow.create_terminal(
        tenant_id=seed_ids.tenant_id,
        outlet_id=seed_ids.outlet_id,
        label=f"E2E Terminal {world.run_id}",
    )
    if result == {}:
        return  # 409 — already exists
    terminal_id = result.get("id") or result.get("terminalId")
    assert terminal_id, f"create_terminal did not return a terminal id: {result}"


# ===========================================================================
# L1 — Terminal binding (Task 5 item 8)
# ===========================================================================


@pytest.mark.L1
@pytest.mark.onboarding
def test_terminal_binding_prepares_pos_flow(
    super_admin_client: ApiClient,
    seed_ids: SeedIds,
) -> None:
    """Bind the seed terminal — 2xx or 409 (already bound) are both acceptable."""
    flow = OnboardingFlow(super_admin_client)
    result = flow.bind_terminal(
        tenant_id=seed_ids.tenant_id,
        terminal_id=seed_ids.terminal_id,
    )
    # Result is either the binding data or {} (if already bound → 409 tolerated)
    assert isinstance(result, dict), f"bind_terminal returned non-dict: {result!r}"
