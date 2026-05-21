"""HTTP client wrapper around httpx with Tchalanet conventions."""
from __future__ import annotations

import os
import uuid
from dataclasses import dataclass, field
from typing import Any, Iterable, Mapping

import httpx

from lib.context import CashierContext


def _resolve_verify() -> bool | str:
    """Decide httpx `verify` from env. Defaults to False for local dev (mkcert / self-signed).

    - TCH_E2E_VERIFY_SSL=true  → verify with system store
    - TCH_E2E_VERIFY_SSL=/path/to/ca.crt → verify using custom CA bundle
    - TCH_E2E_VERIFY_SSL=false (default) → disable verification
    """
    raw = os.environ.get("TCH_E2E_VERIFY_SSL", "false").strip()
    if raw.lower() in {"true", "1", "yes"}:
        return True
    if raw.lower() in {"false", "0", "no", ""}:
        return False
    return raw  # treated as a path to a CA bundle


@dataclass
class ApiClient:
    """Thin wrapper around httpx.Client preserving JWT + X-Tch-* headers across requests."""

    base_url: str
    token: str
    timeout: float = 30.0
    extra_headers: dict[str, str] = field(default_factory=dict)
    _client: httpx.Client = field(init=False, repr=False)

    def __post_init__(self) -> None:
        self._client = httpx.Client(
            base_url=self.base_url,
            timeout=self.timeout,
            verify=_resolve_verify(),
            headers={"Authorization": f"Bearer {self.token}", **self.extra_headers},
        )

    # --- core verbs --------------------------------------------------------

    def get(self, path: str, *, params: Mapping[str, Any] | None = None,
            context: CashierContext | None = None) -> httpx.Response:
        return self._client.get(path, params=params, headers=_ctx_headers(context))

    def post(self, path: str, *, json: Any = None, context: CashierContext | None = None,
             idempotency_key: str | bool | None = None,
             headers: Mapping[str, str] | None = None) -> httpx.Response:
        merged = dict(_ctx_headers(context))
        if headers:
            merged.update(headers)
        if idempotency_key is True:
            merged["Idempotency-Key"] = str(uuid.uuid4())
        elif isinstance(idempotency_key, str):
            merged["Idempotency-Key"] = idempotency_key
        return self._client.post(path, json=json, headers=merged)

    def delete(self, path: str, *, context: CashierContext | None = None) -> httpx.Response:
        return self._client.delete(path, headers=_ctx_headers(context))

    # --- convenience -------------------------------------------------------

    def close(self) -> None:
        self._client.close()

    def __enter__(self) -> "ApiClient":
        return self

    def __exit__(self, *_: Any) -> None:
        self.close()


def _ctx_headers(context: CashierContext | None) -> dict[str, str]:
    if context is None:
        return {}
    headers: dict[str, str] = {}
    if context.outlet_id:
        headers["X-Tch-Outlet-Id"] = context.outlet_id
    if context.terminal_id:
        headers["X-Tch-Terminal-Id"] = context.terminal_id
    if context.session_id:
        headers["X-Tch-Sales-Session-Id"] = context.session_id
    return headers


def assert_ok(response: httpx.Response, *, expected: Iterable[int] = (200, 201, 202, 204)) -> None:
    if response.status_code not in expected:
        raise AssertionError(
            f"Expected {tuple(expected)} from {response.request.method} {response.request.url}, "
            f"got {response.status_code}: {response.text}"
        )
