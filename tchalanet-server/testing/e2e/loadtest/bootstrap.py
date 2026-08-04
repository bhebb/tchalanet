"""Bootstrap an authenticated cashier ApiClient, reusing tch_e2e auth/config."""
from __future__ import annotations

import os
import uuid
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from dotenv import load_dotenv

from tch_e2e.auth import LocalJwtAuth, auth_from_env
from tch_e2e.client import ApiClient
from tch_e2e.config import OpContext, SeedIds, load_env

from loadtest.safety import assert_non_prod


def _load_env_best_effort() -> None:
    # In a deployed environment, config comes from real env vars; .env.local is optional.
    try:
        load_env()
    except Exception:
        pass
    locust_env = Path(__file__).resolve().parents[1] / "target" / "locust" / "local_perf.env"
    if locust_env.exists():
        load_dotenv(locust_env, override=False)


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


@dataclass(frozen=True)
class SellerTerminalRuntime:
    """Seller-terminal identity selected for a Locust user."""

    tenant_id: str
    tenant_code: str
    seller_terminal_id: str
    terminal_code: str
    display_name: str


def _data(response) -> Any:
    body = response.json().get("data")
    if isinstance(body, dict) and set(body.keys()) == {"value"}:
        return body["value"]
    return body


def _items(page_or_list: Any) -> list[dict[str, Any]]:
    if isinstance(page_or_list, list):
        return [item for item in page_or_list if isinstance(item, dict)]
    if not isinstance(page_or_list, dict):
        return []
    for key in ("items", "content"):
        value = page_or_list.get(key)
        if isinstance(value, list):
            return [item for item in value if isinstance(item, dict)]
    return []


def _admin_token() -> str:
    auth = auth_from_env()
    username = os.environ.get("TCH_SUPER_ADMIN_USERNAME", "super_admin")
    password = os.environ.get("TCH_SUPER_ADMIN_PASSWORD", "")
    return auth.password_grant(username=username, password=password)


def _seller_terminal_from_env(seed: SeedIds) -> SellerTerminalRuntime | None:
    terminal_id = (
        os.environ.get("TCH_LOAD_SELLER_TERMINAL_ID")
        or os.environ.get("TCH_SELLER_TERMINAL_ID")
        or ""
    ).strip()
    if not terminal_id:
        return None
    return SellerTerminalRuntime(
        tenant_id=os.environ.get("TCH_LOAD_TENANT_ID", seed.tenant_id),
        tenant_code=os.environ.get("TCH_LOAD_TENANT_CODE", seed.tenant_code),
        seller_terminal_id=terminal_id,
        terminal_code=os.environ.get("TCH_LOAD_SELLER_TERMINAL_CODE", terminal_id),
        display_name=os.environ.get("TCH_LOAD_SELLER_TERMINAL_NAME", terminal_id),
    )


def _pick_seller_terminal(seed: SeedIds, base: str) -> SellerTerminalRuntime:
    configured = _seller_terminal_from_env(seed)
    if configured is not None:
        return configured

    admin = ApiClient(base_url=base, token=_admin_token()).with_tenant(
        os.environ.get("TCH_LOAD_TENANT_ID", seed.tenant_id),
        override_reason="locust-seller-terminal-discovery",
    )
    try:
        response = admin.get(
            "/admin/seller-terminals",
            params={"page": 0, "size": 50, "status": "ACTIVE"},
        )
        if response.status_code >= 400:
            raise RuntimeError(
                "Could not discover an active seller-terminal for Locust "
                f"({response.status_code}): {response.text[:240]}"
            )
        rows = _items(_data(response))
    finally:
        admin.close()

    if not rows:
        raise RuntimeError(
            "No active seller-terminal found for Locust. Provision one first or set "
            "TCH_LOAD_SELLER_TERMINAL_ID."
        )

    selected = rows[0]
    terminal_id = str(selected.get("id") or selected.get("sellerTerminalId") or "")
    if not terminal_id:
        raise RuntimeError(f"Seller-terminal row has no id: {selected}")
    return SellerTerminalRuntime(
        tenant_id=os.environ.get("TCH_LOAD_TENANT_ID", seed.tenant_id),
        tenant_code=os.environ.get("TCH_LOAD_TENANT_CODE", seed.tenant_code),
        seller_terminal_id=terminal_id,
        terminal_code=str(selected.get("terminalCode") or terminal_id),
        display_name=str(selected.get("displayName") or terminal_id),
    )


def _seller_terminal_token(runtime: SellerTerminalRuntime) -> str:
    auth = auth_from_env()
    if not isinstance(auth, LocalJwtAuth):
        raise RuntimeError(
            "Locust seller-terminal mode requires TCH_E2E_AUTH_PROVIDER=local-perf "
            "or local-jwt. Use Firebase emulator only for canonical destructive E2E."
        )
    return auth.mint(
        subject=runtime.seller_terminal_id,
        email=f"{runtime.terminal_code.lower()}@local-perf.test",
        username=runtime.terminal_code,
        tenant_code=runtime.tenant_code,
        role="SELLER_TERMINAL",
    )


def new_seller_terminal_api() -> tuple[ApiClient, OpContext, int]:
    """Authenticated seller-terminal client + POS context + default stake (minor units)."""
    _load_env_best_effort()
    url = base_url()
    assert_non_prod(url)
    seed = SeedIds.from_env()
    runtime = _pick_seller_terminal(seed, url)
    mode = os.environ.get("TCH_LOAD_ACTOR_MODE", "seller-terminal").strip().lower()

    if mode == "admin-act-as":
        token = _admin_token()
        extra_headers = {
            "X-Tch-Client-Type": "POS",
            "X-Tch-Tenant-Override": runtime.tenant_id,
            "X-Tch-Override-Reason": "locust-admin-act-as-seller-terminal",
            "X-Tch-Act-As-Terminal": runtime.seller_terminal_id,
        }
    elif mode == "seller-terminal":
        token = _seller_terminal_token(runtime)
        extra_headers = {
            "X-Tch-Client-Type": "POS",
            "X-Tch-Act-As-Terminal": runtime.seller_terminal_id,
        }
    else:
        raise RuntimeError("TCH_LOAD_ACTOR_MODE must be seller-terminal or admin-act-as")

    client = ApiClient(base_url=url, token=token, extra_headers=extra_headers)
    ctx = OpContext(terminal_id=runtime.seller_terminal_id)
    return client, ctx, seed.stake_cents


def new_cashier_api() -> tuple[ApiClient, OpContext, int]:
    """Backward-compatible alias for the current seller-terminal load runtime."""
    if os.environ.get("TCH_LOAD_USE_LEGACY_CASHIER", "").lower() not in {"1", "true", "yes"}:
        return new_seller_terminal_api()

    # Legacy path kept only for forensic comparison with old POS seeds.
    _load_env_best_effort()
    url = base_url()
    assert_non_prod(url)
    seed = SeedIds.from_env()
    client = ApiClient(base_url=url, token=_cashier_token())
    ctx = OpContext(outlet_id=seed.outlet_id, terminal_id=seed.terminal_id)
    return client, ctx, seed.stake_cents


def new_platform_admin_api() -> ApiClient:
    """Authenticated SUPER_ADMIN client for platform ops load scenarios."""
    _load_env_best_effort()
    url = base_url()
    assert_non_prod(url)
    return ApiClient(base_url=url, token=_admin_token())
