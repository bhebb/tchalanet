"""Bootstrap an authenticated cashier ApiClient, reusing tch_e2e auth/config."""
from __future__ import annotations

import os
import uuid

from tch_e2e.auth import auth_from_env
from tch_e2e.client import ApiClient
from tch_e2e.config import OpContext, SeedIds, load_env

from loadtest.safety import assert_non_prod


def _load_env_best_effort() -> None:
    # In a deployed environment, config comes from real env vars; .env.local is optional.
    try:
        load_env()
    except Exception:
        pass


def base_url() -> str:
    url = os.environ.get("TCH_BASE_URL")
    if not url:
        raise SystemExit("TCH_BASE_URL is required for load testing.")
    return url


def run_id() -> str:
    return os.environ.get("TCH_LOAD_RUN_ID") or f"load-{uuid.uuid4().hex[:8]}"


def _cashier_token() -> str:
    auth = auth_from_env()
    username = os.environ.get("TCH_SELLER_USERNAME", "cashier")
    password = os.environ.get("TCH_SELLER_PASSWORD", "")
    return auth.password_grant(username=username, password=password)


def new_cashier_api() -> tuple[ApiClient, OpContext, int]:
    """Authenticated cashier client + operational context + default stake (cents)."""
    _load_env_best_effort()
    url = base_url()
    assert_non_prod(url)
    seed = SeedIds.from_env()
    client = ApiClient(base_url=url, token=_cashier_token())
    ctx = OpContext(outlet_id=seed.outlet_id, terminal_id=seed.terminal_id)
    return client, ctx, seed.stake_cents
