"""Section 4 — Post-login dashboard PageModels (smoke contract).

Scope (e2e-business-runtime-v1 §4): smoke contract only. Assert the endpoint is
reachable, the ApiResponse shape is valid, the server resolves the *correct*
PageModel for the caller's role (logicalId + provider source + expected widget
ids), and no cross-surface provider leaks. KPI *values* are never asserted as
business truth.

Role → dashboard (server-side resolution, client passes no logicalId):

    CASHIER       → /tenant/page-models   → private.dashboard.cashier.web   (cashier_dashboard)
    TENANT_ADMIN  → /tenant/page-models   → private.dashboard.tenant_admin  (tenant_admin_dashboard)
    SUPER_ADMIN   → /platform/page-models → private.dashboard.superadmin     (platform_admin_dashboard)

The cashier surface here is the *web* dashboard (seller on a computer/tablet,
page engine driven) — distinct from the POS/Android app covered by
``tests/cashier_pos``.
"""
from __future__ import annotations

import json

import pytest

from tch_e2e.api_response import get_data
from tch_e2e.client import ApiClient


# ---------------------------------------------------------------------------
# expected contract (pinned from tchalanet-app/.../pagemodel/*.json + PageModelType)
# ---------------------------------------------------------------------------

TENANT_ADMIN = {
    "logical_id": "private.dashboard.tenant_admin",
    "scope": "private",
    "context": "private_dashboard_tenant_admin",
    "source": "tenant_admin_dashboard",
    "foreign_source": "platform_admin_dashboard",
    "widgets": [
        "dashboard.tenant_admin.header",
        "dashboard.tenant_admin.kpis",
        "dashboard.tenant_admin.readiness",
        "dashboard.tenant_admin.alerts",
        "dashboard.tenant_admin.operations",
    ],
}

CASHIER_WEB = {
    "logical_id": "private.dashboard.cashier.web",
    "scope": "private",
    "context": "private_dashboard_cashier",
    "source": "cashier_dashboard",
    "foreign_source": "tenant_admin_dashboard",
    "widgets": [
        "dashboard.cashier.identity",
        "dashboard.cashier.readiness",
        "dashboard.cashier.alerts",
        "dashboard.cashier.overview",
        "dashboard.cashier.session",
    ],
}

SUPERADMIN = {
    "logical_id": "private.dashboard.superadmin",
    "scope": "private",
    "context": "private_dashboard_superadmin",
    "source": "platform_admin_dashboard",
    "foreign_source": "tenant_admin_dashboard",
    "widgets": [
        "dashboard.superadmin.health",
        "dashboard.superadmin.tenants",
        "dashboard.superadmin.subscriptions",
        "dashboard.superadmin.onboarding",
        "dashboard.superadmin.alerts",
    ],
}


# ---------------------------------------------------------------------------
# helpers
# ---------------------------------------------------------------------------


def _skip_if_unrouted(resp) -> None:
    if resp.status_code in (404, 405):
        pytest.skip(f"page-models endpoint not routed: {resp.request.url} -> {resp.status_code}")


def _assert_dashboard_contract(data: dict, expected: dict) -> None:
    """Smoke-validate a DashboardPageModelResponse against its expected contract."""
    # ApiResponse.data shape
    assert "pageModel" in data, f"response missing pageModel: {list(data)}"
    assert "currentLang" in data, f"response missing currentLang: {list(data)}"
    assert isinstance(data.get("langs"), list) and data["langs"], (
        f"response missing non-empty langs list: {data.get('langs')!r}"
    )

    page_model = data["pageModel"]
    meta = page_model.get("meta", {})

    # correct surface resolved server-side
    assert meta.get("id") == expected["logical_id"], (
        f"wrong logicalId: expected {expected['logical_id']!r}, got {meta.get('id')!r}"
    )
    assert meta.get("scope") == expected["scope"], (
        f"wrong scope: expected {expected['scope']!r}, got {meta.get('scope')!r}"
    )
    assert meta.get("context") == expected["context"], (
        f"wrong context: expected {expected['context']!r}, got {meta.get('context')!r}"
    )

    # expected widget ids present
    widgets = page_model.get("content", {}).get("widgets", {})
    missing = [w for w in expected["widgets"] if w not in widgets]
    assert not missing, f"pageModel missing expected widgets {missing}; present={list(widgets)}"

    # provider source correct + no foreign-surface provider leak
    serialized = json.dumps(page_model)
    assert expected["source"] in serialized, (
        f"expected provider source {expected['source']!r} not found in pageModel"
    )
    assert expected["foreign_source"] not in serialized, (
        f"pageModel leaks foreign provider source {expected['foreign_source']!r}"
    )


# ===========================================================================
# L1 — role resolution (the core security contract)
# ===========================================================================


@pytest.mark.L1
@pytest.mark.dashboard
def test_tenant_admin_resolves_tenant_admin_dashboard(tenant_admin_client: ApiClient) -> None:
    """TENANT_ADMIN → GET /tenant/page-models → private.dashboard.tenant_admin."""
    resp = tenant_admin_client.get("/tenant/page-models")
    _skip_if_unrouted(resp)
    _assert_dashboard_contract(get_data(resp), TENANT_ADMIN)


@pytest.mark.L1
@pytest.mark.dashboard
def test_cashier_resolves_cashier_web_dashboard(cashier_client: ApiClient) -> None:
    """CASHIER → GET /tenant/page-models → private.dashboard.cashier.web (NOT tenant_admin).

    A cashier hitting the same endpoint must get its own web dashboard, never the
    tenant-admin surface — server-side role resolution, no client logicalId.
    """
    resp = cashier_client.get("/tenant/page-models")
    _skip_if_unrouted(resp)
    data = get_data(resp)
    _assert_dashboard_contract(data, CASHIER_WEB)
    # explicit cross-role guard: must not be the tenant_admin model
    assert data["pageModel"]["meta"]["id"] != TENANT_ADMIN["logical_id"], (
        "cashier resolved the tenant_admin dashboard — role resolution broken"
    )


@pytest.mark.L1
@pytest.mark.dashboard
def test_super_admin_resolves_platform_dashboard(super_admin_client: ApiClient) -> None:
    """SUPER_ADMIN → GET /platform/page-models → private.dashboard.superadmin."""
    resp = super_admin_client.get("/platform/page-models")
    _skip_if_unrouted(resp)
    _assert_dashboard_contract(get_data(resp), SUPERADMIN)


# ===========================================================================
# L1 — cross-surface access control
# ===========================================================================


@pytest.mark.L1
@pytest.mark.dashboard
def test_tenant_admin_cannot_access_platform_page_models(tenant_admin_client: ApiClient) -> None:
    """TENANT_ADMIN → GET /platform/page-models → 403 (super-admin only)."""
    resp = tenant_admin_client.get("/platform/page-models")
    _skip_if_unrouted(resp)
    assert resp.status_code == 403, (
        f"tenant admin must not reach platform page-models, got {resp.status_code}: {resp.text}"
    )


@pytest.mark.L1
@pytest.mark.dashboard
def test_cashier_cannot_access_platform_page_models(cashier_client: ApiClient) -> None:
    """CASHIER → GET /platform/page-models → 403."""
    resp = cashier_client.get("/platform/page-models")
    _skip_if_unrouted(resp)
    assert resp.status_code == 403, (
        f"cashier must not reach platform page-models, got {resp.status_code}: {resp.text}"
    )


# ===========================================================================
# L2 — locale resolution
# ===========================================================================


@pytest.mark.L2
@pytest.mark.dashboard
@pytest.mark.parametrize("lang", ["fr", "en", "ht"])
def test_tenant_admin_dashboard_respects_lang(tenant_admin_client: ApiClient, lang: str) -> None:
    """?lang=<x> echoes back in currentLang for a supported locale."""
    resp = tenant_admin_client.get("/tenant/page-models", params={"lang": lang})
    _skip_if_unrouted(resp)
    data = get_data(resp)
    assert data.get("currentLang") == lang, (
        f"requested lang={lang!r} but currentLang={data.get('currentLang')!r}"
    )
    assert lang in data.get("langs", []), f"{lang!r} not advertised in langs {data.get('langs')}"
