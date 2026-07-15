"""Root pytest fixtures for tchalanet E2E — business runtime V1."""
from __future__ import annotations

import os
import uuid

import pytest

from fixtures.pos_context import PosContext
from tch_e2e.api_response import assert_ok
from tch_e2e.auth import E2EAuth, auth_from_env
from tch_e2e.client import ApiClient
from tch_e2e.config import OpContext, SeedIds, load_env
from tch_e2e.scenario_world import ScenarioWorld


@pytest.fixture(scope="session", autouse=True)
def _load_env() -> None:
    load_env()


@pytest.fixture(scope="session")
def base_url() -> str:
    return os.environ["TCH_BASE_URL"].rstrip("/")


@pytest.fixture(scope="session")
def keycloak() -> E2EAuth:
    """Legacy fixture name; provider is selected by TCH_E2E_AUTH_PROVIDER."""
    return auth_from_env()


@pytest.fixture(scope="session")
def seed_ids() -> SeedIds:
    return SeedIds.from_env()


@pytest.fixture(scope="session")
def super_admin_token(keycloak: E2EAuth) -> str:
    return keycloak.password_grant(
        username=os.environ.get("TCH_SUPER_ADMIN_USERNAME", "super_admin"),
        password=os.environ.get("TCH_SUPER_ADMIN_PASSWORD", ""),
    )


@pytest.fixture(scope="session")
def super_admin_client(base_url: str, super_admin_token: str) -> ApiClient:
    return ApiClient(base_url=base_url, token=super_admin_token)


@pytest.fixture(scope="session")
def seller_terminal_id_a(tenant_admin_client: ApiClient) -> str:
    def normalize_id(data):
        if isinstance(data, dict):
            if set(data.keys()) == {"value"}:
                return normalize_id(data["value"])
            return normalize_id(data.get("sellerTerminalId") or data.get("id"))
        return data

    def reuse_existing() -> str | None:
        listed = tenant_admin_client.get("/admin/seller-terminals", params={"page": 0, "size": 50})
        if listed.status_code != 200:
            return None
        data = listed.json().get("data") or {}
        items = data.get("items") or data.get("content") or []
        candidates = [
            item for item in items
            if str(item.get("terminalCode") or "").startswith("E2E-POS-")
            and item.get("status") == "ACTIVE"
            and not item.get("mustChangePin", False)
        ]
        candidates = candidates or [
            item for item in items
            if str(item.get("terminalCode") or "").startswith("E2E-POS-")
            and item.get("status") == "ACTIVE"
        ]
        if not candidates:
            return None
        return str(normalize_id(candidates[0]))

    suffix = uuid.uuid4().hex[:8]
    phone_suffix = str(int(uuid.uuid4().int % 10_000_000)).zfill(7)
    response = tenant_admin_client.post(
        "/admin/seller-terminals",
        json={
            "terminalCode": f"E2E-POS-{suffix}",
            "displayName": f"E2E POS {suffix}",
            "firstName": "E2E",
            "lastName": "Seller",
            "email": f"e2e-pos-{suffix}@test.test",
            "phoneNumber": f"+509{phone_suffix}",
            "commissionRate": "10.00",
            "initialPin": "123456",
        },
    )
    if response.status_code == 403 and "entitlement.limit_exceeded" in response.text:
        existing = reuse_existing()
        if existing:
            return existing
    assert_ok(response, expected=(200, 201))
    data = normalize_id(response.json().get("data"))
    assert data, f"seller-terminal create returned no id: {response.text[:300]}"
    return str(data)


@pytest.fixture(scope="session")
def cashier_token(keycloak: E2EAuth, seller_terminal_id_a: str) -> str:
    if not hasattr(keycloak, "mint"):
        pytest.skip("Current POS e2e requires firebase-emulator token minting.")
    return keycloak.mint(  # type: ignore[attr-defined]
        subject=seller_terminal_id_a,
        email=f"{seller_terminal_id_a}@seller-terminal.localtest.me",
    )


@pytest.fixture(scope="session")
def world(keycloak: E2EAuth, base_url: str, seed_ids: SeedIds, super_admin_token: str) -> ScenarioWorld:
    return ScenarioWorld.build(
        keycloak, base_url, seed_ids, super_admin_token=super_admin_token
    )


@pytest.fixture(scope="session")
def cashier_client_a(base_url: str, cashier_token: str, seller_terminal_id_a: str) -> ApiClient:
    """Session-scoped POS client for Tenant A's current SellerTerminal actor."""
    client = ApiClient(
        base_url=base_url,
        token=cashier_token,
        extra_headers={
            "X-Tch-Client-Type": "POS",
            "X-Tch-Act-As-Terminal": seller_terminal_id_a,
        },
    )
    change_pin = client.post(
        "/tenant/seller-terminal/me/change-pin",
        json={"newPin": "654321"},
    )
    assert change_pin.status_code in (200, 201, 204, 400, 409), (
        f"seller-terminal change-pin failed: {change_pin.status_code} {change_pin.text[:300]}"
    )
    return client


@pytest.fixture()
def cashier_context_a(seed_ids: SeedIds, seller_terminal_id_a: str) -> OpContext:
    """Function-scoped POS context for Tenant A's SellerTerminal."""
    return OpContext(
        outlet_id=seed_ids.outlet_id,
        terminal_id=seller_terminal_id_a,
    )


@pytest.fixture(scope="session")
def tenant_admin_token(keycloak: E2EAuth) -> str:
    """Obtain a TENANT_ADMIN token.

    Uses TCH_TENANT_ADMIN_USERNAME / TCH_TENANT_ADMIN_PASSWORD (env) which maps to
    the seeded 'admin' Keycloak user (TENANT_ADMIN role, tenant_code=tchalanet).
    Falls back gracefully with a clear message if the credentials are missing.
    """
    username = os.environ.get("TCH_TENANT_ADMIN_USERNAME")
    password = os.environ.get("TCH_TENANT_ADMIN_PASSWORD")
    local_auth = os.environ.get("TCH_E2E_AUTH_PROVIDER", "keycloak").strip().lower() != "keycloak"
    if local_auth:
        username = username or "admin"
        password = password or ""
    if not username or (not password and not local_auth):
        pytest.skip(
            "TCH_TENANT_ADMIN_USERNAME / TCH_TENANT_ADMIN_PASSWORD not set. "
            "Add them to .env.local (seeded user: admin / Changeme1!)."
        )
    return keycloak.password_grant(username=username, password=password)


@pytest.fixture(scope="session")
def tenant_admin_client(base_url: str, tenant_admin_token: str) -> ApiClient:
    """TENANT_ADMIN client for /admin/* endpoints (sellers, outlets, terminals).

    Uses the seeded 'admin' user whose JWT already carries tenant_code=tchalanet —
    no X-Tenant-Id override header needed.
    """
    return ApiClient(base_url=base_url, token=tenant_admin_token)


@pytest.fixture()
def onboard_cashier_for_pos(
    super_admin_client: ApiClient,
    cashier_client_a: ApiClient,
    seed_ids: SeedIds,
    tenant_admin_client: ApiClient,
    seller_terminal_id_a: str,
) -> PosContext:
    """Function-scoped fixture that returns a fully onboarded POS context.

    Idempotent: reuses the current OPEN session when one already exists.
    If a CLOSED session blocks opening today, it is auto-finalized via the
    admin endpoint so tests can always run without manual intervention.
    """
    from fixtures.pos_context import build_pos_context
    return build_pos_context(
        super_admin_client,
        cashier_client_a,
        seed_ids,
        tenant_admin_client=tenant_admin_client,
        seller_terminal_id=seller_terminal_id_a,
    )


# Legacy aliases used by migrated test files
@pytest.fixture()
def cashier_client(cashier_client_a: ApiClient) -> ApiClient:
    return cashier_client_a


@pytest.fixture()
def cashier_context(cashier_context_a: OpContext) -> OpContext:
    return cashier_context_a
