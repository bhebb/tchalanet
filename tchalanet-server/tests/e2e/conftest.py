"""Root pytest fixtures for tchalanet E2E."""
from __future__ import annotations

import os
from pathlib import Path

import pytest
from dotenv import load_dotenv

from lib.api import ApiClient
from lib.auth import KeycloakAuth
from lib.context import CashierContext
from lib.ids import SeedIds


@pytest.fixture(scope="session", autouse=True)
def _load_env() -> None:
    """Load .env.local from tchalanet-server/scripts/.env.local (the canonical location)."""
    here = Path(__file__).resolve()
    candidates = [
        here.parents[2] / "scripts" / ".env.local",  # tchalanet-server/scripts/.env.local
        here.parent / ".env.local",                  # tests/e2e/.env.local (optional override)
    ]
    for candidate in candidates:
        if candidate.exists():
            load_dotenv(candidate, override=False)
            return
    raise RuntimeError(
        f".env.local not found in any of: {[str(c) for c in candidates]}"
    )


@pytest.fixture(scope="session")
def base_url() -> str:
    return os.environ["TCH_BASE_URL"].rstrip("/")


@pytest.fixture(scope="session")
def keycloak() -> KeycloakAuth:
    return KeycloakAuth.from_env()


@pytest.fixture(scope="session")
def seed_ids() -> SeedIds:
    return SeedIds.from_env()


@pytest.fixture(scope="session")
def super_admin_token(keycloak: KeycloakAuth) -> str:
    return keycloak.password_grant(
        username=os.environ["TCH_SUPER_ADMIN_USERNAME"],
        password=os.environ["TCH_SUPER_ADMIN_PASSWORD"],
    )


@pytest.fixture(scope="session")
def cashier_token(keycloak: KeycloakAuth) -> str:
    return keycloak.password_grant(
        username=os.environ["TCH_SELLER_USERNAME"],
        password=os.environ["TCH_SELLER_PASSWORD"],
    )


@pytest.fixture(scope="session")
def super_admin_client(base_url: str, super_admin_token: str) -> ApiClient:
    return ApiClient(base_url=base_url, token=super_admin_token)


@pytest.fixture()
def cashier_client(base_url: str, cashier_token: str, seed_ids: SeedIds) -> ApiClient:
    """Cashier client without operational context — caller attaches it as needed."""
    return ApiClient(base_url=base_url, token=cashier_token)


@pytest.fixture()
def cashier_context(seed_ids: SeedIds) -> CashierContext:
    """Default cashier context built from seed IDs. session_id is filled by the session prereq."""
    return CashierContext(
        outlet_id=seed_ids.outlet_id,
        terminal_id=seed_ids.terminal_id,
        session_id=None,
    )
