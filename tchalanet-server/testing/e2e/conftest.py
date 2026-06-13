"""Root pytest fixtures for tchalanet E2E — business runtime V1."""
from __future__ import annotations

import os

import pytest

from fixtures.pos_context import PosContext
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
def cashier_token(keycloak: E2EAuth, base_url: str, super_admin_token: str) -> str:
    seller_username = os.environ.get("TCH_SELLER_USERNAME", "cashier")
    seller_password = os.environ.get("TCH_SELLER_PASSWORD", "")

    token = keycloak.password_grant(username=seller_username, password=seller_password)
    probe = ApiClient(base_url=base_url, token=token).get("/tenant/me/profile")
    if probe.status_code == 200:
        return token

    if os.environ.get("TCH_E2E_AUTH_PROVIDER", "keycloak").strip().lower() != "keycloak":
        raise RuntimeError(
            f"Local cashier token obtained but /tenant/me/profile returns {probe.status_code}. "
            "Recreate the database so LOCAL_JWT/LOCAL_PERF seed identities are present."
        )

    sync = ApiClient(base_url=base_url, token=super_admin_token).post(
        "/platform/ops/sync/identity/firebase-bootstrap-users")
    if sync.status_code in (200, 201, 202, 204):
        token = keycloak.password_grant(username=seller_username, password=seller_password)
        retry = ApiClient(base_url=base_url, token=token).get("/tenant/me/profile")
        if retry.status_code == 200:
            return token

    raise RuntimeError(
        f"Cashier token obtained but /tenant/me/profile returns {probe.status_code}. "
        "Run the Firebase bootstrap sync manually or check seller credentials."
    )


@pytest.fixture(scope="session")
def world(keycloak: E2EAuth, base_url: str, seed_ids: SeedIds, super_admin_token: str) -> ScenarioWorld:
    return ScenarioWorld.build(
        keycloak, base_url, seed_ids, super_admin_token=super_admin_token
    )


@pytest.fixture(scope="session")
def cashier_client_a(base_url: str, cashier_token: str) -> ApiClient:
    """Session-scoped cashier client for Tenant A.

    Sends ``X-Device-Binding: e2e-cred-dev`` on every request so the server
    resolves STRONG operational-context trust for the seeded terminal binding
    (hash seeded by V205/V211/V212 = SHA256Hex(tenantId|terminalId|e2e-cred-dev)).
    """
    return ApiClient(
        base_url=base_url,
        token=cashier_token,
        extra_headers={"X-Device-Binding": "e2e-cred-dev"},
    )


@pytest.fixture()
def cashier_context_a(seed_ids: SeedIds) -> OpContext:
    """Function-scoped cashier context for Tenant A — session_id filled by prereq."""
    return OpContext(
        outlet_id=seed_ids.outlet_id,
        terminal_id=seed_ids.terminal_id,
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
) -> PosContext:
    """Function-scoped fixture that returns a fully onboarded POS context.

    Idempotent: reuses the current OPEN session when one already exists.
    If a CLOSED session blocks opening today, it is auto-finalized via the
    admin endpoint so tests can always run without manual intervention.
    """
    from fixtures.pos_context import build_pos_context
    return build_pos_context(
        super_admin_client, cashier_client_a, seed_ids, tenant_admin_client=tenant_admin_client
    )


# Legacy aliases used by migrated test files
@pytest.fixture()
def cashier_client(cashier_client_a: ApiClient) -> ApiClient:
    return cashier_client_a


@pytest.fixture()
def cashier_context(cashier_context_a: OpContext) -> OpContext:
    return cashier_context_a
