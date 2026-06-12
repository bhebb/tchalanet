"""Two independent sellers sell at the same time → tickets stay separated.

Builds two brand-new sellers from scratch via the reusable ``onboard_pos_seller``
helper (each: outlet → cashier → seller → bound terminal → session), has them
sell concurrently, then verifies ticket isolation: each cashier sees only its own
ticket and cannot read the other's.

NOTE on cross-tenant: the same helper takes a ``tenant_id`` and would run one
seller per distinct tenant — but a freshly-provisioned Tenant B has no catalog
(no games/pricing/draws) and cannot sell yet. So both sellers here run on the
seeded, catalog-bearing tenant (separation is proven at the seller/outlet level).
Cross-tenant dual-POS unlocks once tenant provisioning seeds a catalog.
"""
from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor

import pytest

from fixtures.seller_pos import onboard_pos_seller
from prereqs.draws import ensure_draws_today
from tch_e2e.auth import E2EAuth
from tch_e2e.client import ApiClient
from tch_e2e.config import SeedIds


def _ticket_ids(flow) -> set[str]:
    return {
        (row.get("id") or row.get("ticketId"))
        for row in flow.list_tickets()
        if (row.get("id") or row.get("ticketId"))
    }


@pytest.mark.L3
@pytest.mark.cashier_pos
def test_two_sellers_sell_concurrently_and_tickets_are_separated(
    super_admin_client: ApiClient,
    tenant_admin_client: ApiClient,
    keycloak: E2EAuth,
    base_url: str,
    seed_ids: SeedIds,
) -> None:
    # Shared catalog: make sure today's draws exist and are open.
    ensure_draws_today(super_admin_client, seed_ids)

    seller1 = onboard_pos_seller(
        super_admin_client=super_admin_client, admin_client=tenant_admin_client,
        keycloak=keycloak, base_url=base_url,
        tenant_id=seed_ids.tenant_id, label="ConcA", stake_cents=seed_ids.stake_cents)
    seller2 = onboard_pos_seller(
        super_admin_client=super_admin_client, admin_client=tenant_admin_client,
        keycloak=keycloak, base_url=base_url,
        tenant_id=seed_ids.tenant_id, label="ConcB", stake_cents=seed_ids.stake_cents)

    draw1 = seller1.first_open_draw()
    draw2 = seller2.first_open_draw()
    if not draw1 or not draw2:
        pytest.skip("No OPEN draw available for both sellers — catalog/draws not ready")

    # Both sellers sell HT_BOLET at the same time.
    with ThreadPoolExecutor(max_workers=2) as pool:
        f1 = pool.submit(seller1.cashier_flow().sell, draw1, "HT_BOLET")
        f2 = pool.submit(seller2.cashier_flow().sell, draw2, "HT_BOLET")
        ticket1 = f1.result()
        ticket2 = f2.result()

    assert ticket1.ticket_id and ticket2.ticket_id
    assert ticket1.ticket_id != ticket2.ticket_id, "concurrent sells produced the same ticket id"

    # Each cashier sees ONLY its own ticket.
    ids1 = _ticket_ids(seller1.cashier_flow())
    ids2 = _ticket_ids(seller2.cashier_flow())
    assert ticket1.ticket_id in ids1, "seller 1 cannot see its own ticket"
    assert ticket2.ticket_id in ids2, "seller 2 cannot see its own ticket"
    assert ticket2.ticket_id not in ids1, "seller 1 leaked seller 2's ticket in its list"
    assert ticket1.ticket_id not in ids2, "seller 2 leaked seller 1's ticket in its list"

    # And cannot fetch the other's ticket by id.
    cross = seller1.cashier_client.get(
        f"/tenant/cashier/tickets/{ticket2.ticket_id}", context=seller1.op_context())
    assert cross.status_code in (403, 404), (
        f"seller 1 fetched seller 2's ticket by id (status {cross.status_code}) — leak"
    )
