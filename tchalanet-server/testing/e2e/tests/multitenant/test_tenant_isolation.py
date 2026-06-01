"""Multi-tenant isolation & concurrency — Tenant A (seed) vs a fresh Tenant B.

Provisions a brand-new, fully-onboarded Tenant B (plan + outlet + seller +
terminal) next to the seeded Tenant A, then proves the two operate side by side
without leaking into each other:

  - RLS read isolation: each tenant's admin scope lists ONLY its own outlets /
    terminals / sellers (A never sees B's rows, B never sees A's).
  - cross-tenant fetch by id is blocked (403 / 404).
  - under CONCURRENCY: interleaved A/B reads on a shared connection pool never
    leak — `app.current_tenant` must be correct per request, not bled from a
    pooled connection or a neighbouring thread.

Both tenants are driven by the SUPER_ADMIN via the X-Tenant-Id override, so the
test needs no per-tenant Keycloak users.
"""
from __future__ import annotations

import uuid
from concurrent.futures import ThreadPoolExecutor
from typing import Any

import pytest

from flows.onboarding import OnboardingFlow
from tch_e2e.api_response import get_data
from tch_e2e.client import ApiClient
from tch_e2e.config import SeedIds

_PROFILE = "DEFAULT_HAITI_LOTTERY"
_TIMEZONE = "America/Port-au-Prince"


# ---------------------------------------------------------------------------
# helpers
# ---------------------------------------------------------------------------


def _run() -> str:
    return uuid.uuid4().hex[:8]


def _id_of(obj: Any) -> str | None:
    if isinstance(obj, str):
        return obj
    if isinstance(obj, dict):
        for k in ("id", "outletId", "terminalId", "sellerId", "value"):
            v = obj.get(k)
            if v:
                return v.get("value") if isinstance(v, dict) else v
    return None


def _list_ids(client: ApiClient, path: str) -> set[str]:
    """Collect row ids from a paginated ({items:[...]}) or plain-list admin endpoint."""
    data = get_data(client.get(path, params={"size": 200}))
    rows = data.get("items") if isinstance(data, dict) else data
    ids: set[str] = set()
    for row in rows or []:
        rid = _id_of(row)
        if rid:
            ids.add(str(rid))
    return ids


def _provision_operational_tenant_b(super_admin_client: ApiClient) -> dict[str, Any]:
    """Provision Tenant B and complete its structure (plan + outlet + seller + terminal)."""
    run = _run()
    prov = super_admin_client.post(
        "/platform/tenant-onboarding/provision",
        json={
            "code": f"e2e-mt-{run}",
            "name": f"E2E MT {run}",
            "type": "BORLETTE",
            "profile": _PROFILE,
            "timezone": _TIMEZONE,
            "currency": "HTG",
            "initialAdminEmail": f"admin-mt-{run}@e2e.local",
        },
    )
    if prov.status_code in (404, 405):
        pytest.skip("tenant-onboarding/provision endpoint not routed")
    tenant_id = get_data(prov)["tenantId"]

    # Plan → entitlements (needed before /admin/outlets).
    applied = super_admin_client.post(
        f"/platform/subscriptions/{tenant_id}/apply",
        json={"planCode": "STANDARD", "effectiveAt": "2020-01-01T00:00:00Z"},
    )
    assert applied.status_code in (200, 201), f"apply plan failed: {applied.text}"

    admin = super_admin_client.with_tenant(tenant_id, override_reason="e2e-multitenant")
    flow = OnboardingFlow(admin)

    outlet_id = _id_of(flow.create_outlet_admin(name=f"MT Outlet {run}", slug=f"mt-{run}"))
    assert outlet_id, "tenant B outlet not created"

    user_id = _id_of(flow.create_identity_user(
        email=f"cashier-mt-{run}@e2e.local", role="CASHIER", outletId=outlet_id))
    assert user_id, "tenant B cashier user not created"

    flow.ensure_seller_for_user(
        user_id=user_id, outlet_id=outlet_id,
        display_name=f"MT Seller {run}", code=f"mt-{run}")

    terminal_id = _id_of(flow.create_terminal_admin(
        outlet_id=outlet_id, label=f"MT Terminal {run}"))
    assert terminal_id, "tenant B terminal not created"

    return {
        "tenant_id": tenant_id,
        "admin": admin,
        "outlet_id": str(outlet_id),
        "terminal_id": str(terminal_id),
        "user_id": str(user_id),
    }


# ---------------------------------------------------------------------------
# fixtures
# ---------------------------------------------------------------------------


@pytest.fixture(scope="module")
def tenant_b(super_admin_client: ApiClient) -> dict[str, Any]:
    return _provision_operational_tenant_b(super_admin_client)


@pytest.fixture(scope="module")
def tenant_a_admin(super_admin_client: ApiClient, seed_ids: SeedIds) -> ApiClient:
    """SUPER_ADMIN scoped to the seeded Tenant A (symmetric with Tenant B's admin)."""
    return super_admin_client.with_tenant(seed_ids.tenant_id, override_reason="e2e-multitenant")


# ===========================================================================
# L2 — RLS read isolation across tenants
# ===========================================================================


@pytest.mark.L2
@pytest.mark.multitenant
@pytest.mark.parametrize("resource", ["/admin/outlets", "/admin/terminals", "/admin/sellers"])
def test_admin_lists_are_tenant_isolated(
    tenant_a_admin: ApiClient,
    tenant_b: dict[str, Any],
    seed_ids: SeedIds,
    resource: str,
) -> None:
    """Each tenant's admin scope sees only its own rows for outlets/terminals/sellers."""
    a_ids = _list_ids(tenant_a_admin, resource)
    b_ids = _list_ids(tenant_b["admin"], resource)

    # Disjoint row sets — the core RLS guarantee.
    overlap = a_ids & b_ids
    assert not overlap, f"{resource}: tenants A and B share rows {overlap} — RLS leak"

    # Positive anchors: A sees its seeded outlet/terminal; B sees its created ones.
    if resource == "/admin/outlets":
        assert seed_ids.outlet_id in a_ids, "A admin cannot see its own seeded outlet"
        assert tenant_b["outlet_id"] in b_ids, "B admin cannot see its own outlet"
        assert tenant_b["outlet_id"] not in a_ids, "A admin leaked B's outlet"
        assert seed_ids.outlet_id not in b_ids, "B admin leaked A's seeded outlet"
    elif resource == "/admin/terminals":
        assert seed_ids.terminal_id in a_ids, "A admin cannot see its own seeded terminal"
        assert tenant_b["terminal_id"] in b_ids, "B admin cannot see its own terminal"
        assert tenant_b["terminal_id"] not in a_ids, "A admin leaked B's terminal"


@pytest.mark.L2
@pytest.mark.multitenant
def test_cross_tenant_outlet_fetch_is_blocked(
    tenant_a_admin: ApiClient,
    tenant_b: dict[str, Any],
    seed_ids: SeedIds,
) -> None:
    """Fetching the other tenant's outlet by id must not return it (403/404)."""
    # B tries to read A's seeded outlet.
    b_reads_a = tenant_b["admin"].get(f"/admin/outlets/{seed_ids.outlet_id}")
    assert b_reads_a.status_code in (403, 404), (
        f"Tenant B fetched Tenant A's outlet (status {b_reads_a.status_code}) — RLS leak: "
        f"{b_reads_a.text[:200]}"
    )
    # A tries to read B's outlet.
    a_reads_b = tenant_a_admin.get(f"/admin/outlets/{tenant_b['outlet_id']}")
    assert a_reads_b.status_code in (403, 404), (
        f"Tenant A fetched Tenant B's outlet (status {a_reads_b.status_code}) — RLS leak: "
        f"{a_reads_b.text[:200]}"
    )


# ===========================================================================
# L3 — RLS holds under concurrency (interleaved A/B reads on a shared pool)
# ===========================================================================


@pytest.mark.L3
@pytest.mark.multitenant
def test_concurrent_mixed_tenant_reads_never_leak(
    tenant_a_admin: ApiClient,
    tenant_b: dict[str, Any],
    seed_ids: SeedIds,
) -> None:
    """Fire many interleaved A/B outlet-list reads in parallel; each must see only
    its own tenant's outlet. Catches connection-pool / ThreadLocal tenant bleed."""
    a_outlet = seed_ids.outlet_id
    b_outlet = tenant_b["outlet_id"]
    b_admin = tenant_b["admin"]

    def probe(which: str) -> tuple[str, bool, bool]:
        client = tenant_a_admin if which == "A" else b_admin
        ids = _list_ids(client, "/admin/outlets")
        if which == "A":
            return ("A", a_outlet in ids, b_outlet in ids)  # (tenant, sees_own, sees_other)
        return ("B", b_outlet in ids, a_outlet in ids)

    # 40 interleaved probes (20 A, 20 B) on the shared API connection pool.
    order = ["A", "B"] * 20
    with ThreadPoolExecutor(max_workers=10) as pool:
        results = list(pool.map(probe, order))

    leaks = [r for r in results if r[2]]            # saw the OTHER tenant's outlet
    blind = [r for r in results if not r[1]]         # failed to see its OWN outlet
    assert not leaks, f"RLS leak under concurrency — {len(leaks)} probes saw the other tenant: {leaks[:5]}"
    assert not blind, f"RLS over-restriction under concurrency — {len(blind)} probes missed own outlet: {blind[:5]}"
