"""HTTP client wrapper around httpx with Tchalanet conventions."""
from __future__ import annotations

import os
import uuid
from dataclasses import dataclass, field
from typing import Any, Iterable, Mapping

import httpx

from tch_e2e.config import OpContext


def _resolve_verify() -> bool | str:
    raw = os.environ.get("TCH_E2E_VERIFY_SSL", "false").strip()
    if raw.lower() in {"true", "1", "yes"}:
        return True
    if raw.lower() in {"false", "0", "no", ""}:
        return False
    return raw


@dataclass
class ApiClient:
    """Thin wrapper around httpx.Client preserving JWT + X-Tch-* headers."""

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

    def get(self, path: str, *, params: Mapping[str, Any] | None = None,
            context: OpContext | None = None,
            headers: Mapping[str, str] | None = None) -> httpx.Response:
        merged = dict(_ctx_headers(context))
        if headers:
            merged.update(headers)
        return self._client.get(path, params=params, headers=merged)

    def post(self, path: str, *, json: Any = None, context: OpContext | None = None,
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

    def patch(self, path: str, *, json: Any = None, context: OpContext | None = None,
              headers: Mapping[str, str] | None = None) -> httpx.Response:
        merged = dict(_ctx_headers(context))
        if headers:
            merged.update(headers)
        return self._client.patch(path, json=json, headers=merged)

    def delete(self, path: str, *, context: OpContext | None = None) -> httpx.Response:
        return self._client.delete(path, headers=_ctx_headers(context))

    def with_tenant(self, tenant_id: str) -> "ApiClient":
        """Return a copy scoped to *tenant_id* via the X-Tenant-Id header.

        Required for /admin/* endpoints when using a SUPER_ADMIN token — the
        server reads X-Tenant-Id to populate TchRequestContext.tenantIdSafe().
        """
        return ApiClient(
            base_url=self.base_url,
            token=self.token,
            timeout=self.timeout,
            extra_headers={**self.extra_headers, "X-Tenant-Id": tenant_id},
        )

    def close(self) -> None:
        self._client.close()

    def __enter__(self) -> "ApiClient":
        return self

    def __exit__(self, *_: Any) -> None:
        self.close()


def _ctx_headers(context: OpContext | None) -> dict[str, str]:
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
