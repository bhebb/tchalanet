"""Section 4 — Platform overview (smoke contract).

Scope (e2e-business-runtime-v1 §4): the platform overview is the super-admin
structural surface — global catalog/core/platform counts + section status. Like
the tenant overview it is NOT a dashboard; counts are validated for internal
sanity (active <= total), never asserted as business truth.

Endpoint:
    GET /platform/overview → PlatformAdminOverviewView (SUPER_ADMIN only)
"""
from __future__ import annotations

import pytest

from tch_e2e.api_response import get_data
from tch_e2e.client import ApiClient


def _skip_if_unrouted(resp) -> None:
    if resp.status_code in (404, 405):
        pytest.skip(f"platform overview not routed: {resp.request.url} -> {resp.status_code}")


def _assert_count_item(item: dict, label: str) -> None:
    assert isinstance(item, dict), f"{label} not an object: {item!r}"
    total, active = item.get("total"), item.get("active")
    assert isinstance(total, int) and isinstance(active, int), f"{label} total/active not ints: {item}"
    assert 0 <= active <= total, f"{label} active({active}) must be within 0..total({total})"


# ===========================================================================
# L1 — shape + access control
# ===========================================================================


@pytest.mark.L1
@pytest.mark.platformadmin
def test_platform_overview_returns_global_stats(super_admin_client: ApiClient) -> None:
    """SUPER_ADMIN → GET /platform/overview → generatedAt + catalog + core + platform."""
    resp = super_admin_client.get("/platform/overview")
    _skip_if_unrouted(resp)
    data = get_data(resp)

    assert data.get("generatedAt"), f"missing generatedAt: {data}"
    for block in ("catalog", "core", "platform"):
        assert isinstance(data.get(block), dict), f"missing/invalid {block} block: {data.get(block)!r}"


@pytest.mark.L1
@pytest.mark.platformadmin
def test_tenant_admin_cannot_access_platform_overview(tenant_admin_client: ApiClient) -> None:
    """TENANT_ADMIN → GET /platform/overview → 403."""
    resp = tenant_admin_client.get("/platform/overview")
    _skip_if_unrouted(resp)
    assert resp.status_code == 403, (
        f"tenant admin must not reach /platform/overview, got {resp.status_code}: {resp.text}"
    )


# ===========================================================================
# L2 — count sanity + sections
# ===========================================================================


@pytest.mark.L2
@pytest.mark.platformadmin
def test_platform_overview_counts_are_sane(super_admin_client: ApiClient) -> None:
    """CountItems satisfy 0 <= active <= total; at least one tenant exists (seeded)."""
    resp = super_admin_client.get("/platform/overview")
    _skip_if_unrouted(resp)
    data = get_data(resp)

    catalog = data["catalog"]
    for key in ("games", "resultSlots", "i18nGlobalKeys", "pageModelTemplates", "themePresets"):
        if catalog.get(key) is not None:
            _assert_count_item(catalog[key], f"catalog.{key}")

    platform = data["platform"]
    for key in ("plans", "pricingRules", "globalSettings"):
        if platform.get(key) is not None:
            _assert_count_item(platform[key], f"platform.{key}")

    tenants = data["core"].get("tenants", {})
    assert tenants.get("total", 0) >= 1, f"expected >=1 seeded tenant: {tenants}"
    assert tenants.get("active", 0) <= tenants.get("total", 0), f"tenants active>total: {tenants}"


@pytest.mark.L2
@pytest.mark.platformadmin
def test_platform_overview_sections_are_navigable(super_admin_client: ApiClient) -> None:
    """sections[] items expose key + boolean enabled + href (navigation surface)."""
    resp = super_admin_client.get("/platform/overview")
    _skip_if_unrouted(resp)
    sections = get_data(resp).get("sections")
    assert isinstance(sections, list) and sections, "platform overview has no sections"

    for item in sections:
        assert item.get("key"), f"section item missing key: {item}"
        assert isinstance(item.get("enabled"), bool), f"section {item.get('key')!r} enabled not bool: {item}"
        assert "href" in item, f"section {item.get('key')!r} missing href: {item}"
