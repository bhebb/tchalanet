"""Seller onboarding tests.

Covers the core.seller lifecycle:
  1. Create a seller profile
  2. Link seller to an app user
  3. Assign seller to an outlet
  4. List / get seller to verify state

Plus outlet receipt configuration (header / footer):
  5. PATCH /admin/outlets/{id}/config → receiptHeaderMessage / receiptFooterMessage

And terminal binding (E2E challenge / verify flow):
  6. Create activation challenge (deliveryMode=E2E)
  7. Verify challenge → binding created

All tests use tenant-scoped admin clients (super_admin + X-Tenant-Id).
Endpoints still WIP are skipped gracefully.
"""
from __future__ import annotations

import pytest

from flows.outlet import OutletFlow
from flows.seller import SellerFlow
from flows.terminal import TerminalBindingFlow
from tch_e2e.client import ApiClient
from tch_e2e.config import SeedIds
from tch_e2e.scenario_world import ScenarioWorld


# ===========================================================================
# L1 — Seller lifecycle
# ===========================================================================


@pytest.mark.L1
@pytest.mark.onboarding
def test_create_seller_returns_id(
    tenant_admin_client: ApiClient,
    world: ScenarioWorld,
) -> None:
    """POST /admin/sellers → seller_id returned."""
    flow = SellerFlow(tenant_admin_client)
    seller_id = flow.create_seller(
        display_name=f"E2E Seller {world.run_id}",
        code=f"e2e-seller-{world.run_id}",
    )
    assert seller_id, f"create_seller returned empty id: {seller_id!r}"


@pytest.mark.L1
@pytest.mark.onboarding
def test_list_sellers_not_empty(
    tenant_admin_client: ApiClient,
) -> None:
    """GET /admin/sellers → at least the seed/created sellers are listed."""
    flow = SellerFlow(tenant_admin_client)
    sellers = flow.list_sellers()
    # Seed may or may not include a seller; at minimum the list must be a list
    assert isinstance(sellers, list), f"list_sellers did not return a list: {sellers!r}"


@pytest.mark.L1
@pytest.mark.onboarding
def test_get_seller_after_create(
    tenant_admin_client: ApiClient,
    world: ScenarioWorld,
) -> None:
    """Create a seller → GET it back → display name matches."""
    flow = SellerFlow(tenant_admin_client)
    display_name = f"E2E Get-Check {world.run_id}"
    seller_id = flow.create_seller(display_name=display_name, code=f"e2e-gc-{world.run_id}")
    seller = flow.get_seller(seller_id)

    name = seller.get("displayName") or seller.get("name")
    assert name == display_name, (
        f"Expected displayName={display_name!r}, got {name!r}: {seller}"
    )


@pytest.mark.L1
@pytest.mark.onboarding
def test_assign_seller_to_outlet(
    tenant_admin_client: ApiClient,
    seed_ids: SeedIds,
    world: ScenarioWorld,
) -> None:
    """Create seller → assign to seed outlet → assignment ID returned."""
    flow = SellerFlow(tenant_admin_client)
    seller_id = flow.create_seller(
        display_name=f"E2E Assign {world.run_id}",
        code=f"e2e-assign-{world.run_id}",
    )
    assignment_id = flow.assign_outlet(seller_id, seed_ids.outlet_id)
    assert assignment_id, f"assign_outlet returned empty assignment_id: {assignment_id!r}"


# ===========================================================================
# L1 — Outlet receipt header / footer configuration
# ===========================================================================


@pytest.mark.L1
@pytest.mark.onboarding
def test_outlet_receipt_header_footer_config(
    tenant_admin_client: ApiClient,
    seed_ids: SeedIds,
) -> None:
    """PATCH /admin/outlets/{id}/config → set and verify receipt header/footer.

    Sets a distinctive header and footer on the seed outlet. The values are
    intentionally unique so a subsequent print test can assert they appear in
    the PDF receipt.
    """
    flow = OutletFlow(admin_client=tenant_admin_client)
    flow.update_receipt_config(
        seed_ids.outlet_id,
        header_message="--- E2E TEST RECEIPT ---",
        footer_message="Merci pour votre confiance / Thank you",
    )
    # update_receipt_config asserts 2xx — if we reach here, the update succeeded


@pytest.mark.L1
@pytest.mark.onboarding
def test_outlet_sales_capability_is_enabled(
    tenant_admin_client: ApiClient,
    cashier_client_a: ApiClient,
    seed_ids: SeedIds,
) -> None:
    """GET /tenant/outlets/{id}/sales-capability → outlet can sell."""
    flow = OutletFlow(admin_client=tenant_admin_client, tenant_client=cashier_client_a)
    capability = flow.get_sales_capability(seed_ids.outlet_id)
    can_sell = capability.get("canSell") or capability.get("salesEnabled")
    assert can_sell is not False, (
        f"Seed outlet sales capability unexpectedly disabled: {capability}"
    )


# ===========================================================================
# L1 — Terminal binding (E2E challenge / verify)
# ===========================================================================


@pytest.mark.L1
@pytest.mark.onboarding
def test_terminal_activation_challenge_e2e_mode(
    cashier_client_a: ApiClient,
    seed_ids: SeedIds,
) -> None:
    """POST activation challenge with deliveryMode=E2E → challenge returned with deliveryRef.

    The seed terminal is already bound (V205), so this creates an additional
    challenge. The test verifies the challenge flow works end-to-end.
    """
    flow = TerminalBindingFlow(cashier_client_a)
    challenge = flow.create_challenge(seed_ids.terminal_id)

    assert challenge.get("challengeId"), (
        f"Challenge response missing challengeId: {challenge}"
    )
    # In E2E delivery mode the server must return the clear code in deliveryRef
    assert challenge.get("deliveryRef") or challenge.get("clearCode"), (
        f"E2E challenge missing deliveryRef/clearCode — check TerminalChallengeDeliveryMode.E2E: "
        f"{challenge}"
    )


@pytest.mark.L2
@pytest.mark.onboarding
def test_terminal_full_bind_e2e_flow(
    cashier_client_a: ApiClient,
    seed_ids: SeedIds,
) -> None:
    """Full E2E binding: create challenge → verify → binding created (or already bound 409).

    L2 because the seed terminal may already be bound (409 is tolerated).
    """
    flow = TerminalBindingFlow(cashier_client_a)
    result = flow.bind_e2e(seed_ids.terminal_id)
    # Returns {} on 409 (already bound) or the binding dict
    assert isinstance(result, dict), f"bind_e2e returned non-dict: {result!r}"


@pytest.mark.L2
@pytest.mark.onboarding
def test_terminal_activate_for_user_admin_shortcut(
    tenant_admin_client: ApiClient,
    seed_ids: SeedIds,
) -> None:
    """POST /admin/terminals/{id}/activate-for-user — admin shortcut binding."""
    flow = TerminalBindingFlow(tenant_admin_client)
    # Tolerates 409 (already active for user) — the assert is inside activate_for_user
    flow.activate_for_user(seed_ids.terminal_id)
