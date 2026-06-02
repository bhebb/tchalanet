"""Section 4 — Tenant overview & readiness (smoke contract).

Scope (e2e-business-runtime-v1 §4): the tenant overview is a *structural
diagnosis / navigation* surface, NOT a dashboard. It returns sections with
per-section status + issues + route, plus a rolled-up status and missingCount.
It MUST NOT repeat dashboard KPI fields.

Endpoints:
    GET /admin/overview          → TenantAdminOverviewView (TENANT_ADMIN / SUPER_ADMIN)
    GET /admin/policies/overview → PoliciesOverviewView    (TENANT_ADMIN / SUPER_ADMIN)

Readiness has no standalone endpoint — it is the ``sections[]`` projection on
the overview, so readiness consistency is asserted here.
"""
from __future__ import annotations

import pytest

from tch_e2e.api_response import get_data
from tch_e2e.client import ApiClient


_READINESS_STATUSES = {"READY", "PARTIAL", "MISSING", "UNKNOWN"}

# Dashboard KPI fields that MUST NOT appear on the structural overview.
_FORBIDDEN_KPI_FIELDS = {
    "salesToday",
    "ticketCountToday",
    "activeSessions",
    "openDraws",
}


def _skip_if_unrouted(resp) -> None:
    if resp.status_code in (404, 405):
        pytest.skip(f"overview endpoint not routed: {resp.request.url} -> {resp.status_code}")


# ===========================================================================
# L1 — tenant overview shape + access control
# ===========================================================================


@pytest.mark.L1
@pytest.mark.overview
def test_tenant_overview_returns_structural_sections(tenant_admin_client: ApiClient) -> None:
    """TENANT_ADMIN → GET /admin/overview → header + status + missingCount + sections[]."""
    resp = tenant_admin_client.get("/admin/overview")
    _skip_if_unrouted(resp)
    data = get_data(resp)

    header = data.get("header")
    assert header and header.get("tenantId"), f"overview missing header.tenantId: {data}"
    assert header.get("tenantCode"), f"overview missing header.tenantCode: {header}"

    assert data.get("status") in _READINESS_STATUSES, f"bad top status: {data.get('status')!r}"
    assert isinstance(data.get("missingCount"), int), f"missingCount not int: {data.get('missingCount')!r}"

    sections = data.get("sections")
    assert isinstance(sections, list) and sections, f"overview sections empty: {data}"


@pytest.mark.L1
@pytest.mark.overview
def test_tenant_overview_omits_dashboard_kpis(tenant_admin_client: ApiClient) -> None:
    """Overview is structural — it must NOT carry dashboard KPI fields."""
    resp = tenant_admin_client.get("/admin/overview")
    _skip_if_unrouted(resp)
    data = get_data(resp)
    leaked = _FORBIDDEN_KPI_FIELDS & set(data.keys())
    assert not leaked, f"overview leaks dashboard KPI fields: {leaked}"


@pytest.mark.L1
@pytest.mark.overview
def test_cashier_cannot_access_tenant_overview(cashier_client: ApiClient) -> None:
    """CASHIER → GET /admin/overview → 403 (admin/super only)."""
    resp = cashier_client.get("/admin/overview")
    _skip_if_unrouted(resp)
    assert resp.status_code == 403, (
        f"cashier must not reach /admin/overview, got {resp.status_code}: {resp.text}"
    )


# ===========================================================================
# L2 — readiness consistency (the readiness contract, via overview sections)
# ===========================================================================


@pytest.mark.L2
@pytest.mark.overview
@pytest.mark.readiness
def test_readiness_sections_are_well_formed(tenant_admin_client: ApiClient) -> None:
    """Every section has a valid status + id; each issue carries a section + messageKey."""
    resp = tenant_admin_client.get("/admin/overview")
    _skip_if_unrouted(resp)
    sections = get_data(resp)["sections"]

    for section in sections:
        assert section.get("id"), f"section missing id: {section}"
        assert section.get("status") in _READINESS_STATUSES, (
            f"section {section.get('id')!r} bad status: {section.get('status')!r}"
        )
        for issue in section.get("issues") or []:
            assert issue.get("section"), f"issue missing section: {issue}"
            assert issue.get("messageKey"), f"issue missing messageKey: {issue}"


@pytest.mark.L2
@pytest.mark.overview
@pytest.mark.readiness
def test_readiness_missing_count_matches_sections(tenant_admin_client: ApiClient) -> None:
    """Rolled-up missingCount must equal #(MISSING) + #(PARTIAL) sections.

    Per TenantReadinessAssembler.countMissing, UNKNOWN sections are NOT counted as
    missing (an undiagnosed section does not block readiness).
    """
    resp = tenant_admin_client.get("/admin/overview")
    _skip_if_unrouted(resp)
    data = get_data(resp)
    sections = data["sections"]

    incomplete = [s for s in sections if s.get("status") in ("MISSING", "PARTIAL")]
    assert data["missingCount"] == len(incomplete), (
        f"missingCount={data['missingCount']} but {len(incomplete)} sections are MISSING/PARTIAL: "
        f"{[(s.get('id'), s.get('status')) for s in incomplete]}"
    )


# ===========================================================================
# L2 — policies overview
# ===========================================================================


@pytest.mark.L2
@pytest.mark.overview
def test_policies_overview_returns_assignments_and_autonomy(tenant_admin_client: ApiClient) -> None:
    """TENANT_ADMIN → GET /admin/policies/overview → tenantAssignmentsCount + autonomyConfigured."""
    resp = tenant_admin_client.get("/admin/policies/overview")
    _skip_if_unrouted(resp)
    data = get_data(resp)

    assert isinstance(data.get("tenantAssignmentsCount"), int), (
        f"tenantAssignmentsCount missing/not int: {data.get('tenantAssignmentsCount')!r}"
    )
    assert isinstance(data.get("autonomyConfigured"), bool), (
        f"autonomyConfigured missing/not bool: {data.get('autonomyConfigured')!r}"
    )


@pytest.mark.L2
@pytest.mark.overview
def test_cashier_cannot_access_policies_overview(cashier_client: ApiClient) -> None:
    """CASHIER → GET /admin/policies/overview → 403."""
    resp = cashier_client.get("/admin/policies/overview")
    _skip_if_unrouted(resp)
    assert resp.status_code == 403, (
        f"cashier must not reach /admin/policies/overview, got {resp.status_code}: {resp.text}"
    )
