"""LocustApiClient — wraps tch_e2e.client.ApiClient and reports every call to Locust.

CashierFlow (and the rest of tch_e2e/flows) call ``client.get/post/patch/delete``. This wrapper
keeps the exact same signatures and return type (httpx.Response), so the existing flows run
unchanged, while each HTTP call is timed and pushed to Locust's stats via ``events.request``.

Metric names are stable (the request path, which is ID-free on the sell hot path) — never the raw
URL with IDs — so stats aggregate per endpoint.
"""
from __future__ import annotations

import time
from typing import Any

import httpx

from tch_e2e.client import ApiClient


class LocustApiClient:
    """Duck-typed drop-in for ApiClient that emits Locust request events."""

    def __init__(self, api: ApiClient, request_event: Any, *, run_context: dict | None = None):
        self._api = api
        self._request = request_event
        self._context = run_context or {}

    # --- ApiClient surface (same signatures) -------------------------------

    def get(self, path: str, **kwargs: Any) -> httpx.Response:
        return self._timed("GET", path, self._api.get, path, **kwargs)

    def post(self, path: str, **kwargs: Any) -> httpx.Response:
        return self._timed("POST", path, self._api.post, path, **kwargs)

    def patch(self, path: str, **kwargs: Any) -> httpx.Response:
        return self._timed("PATCH", path, self._api.patch, path, **kwargs)

    def delete(self, path: str, **kwargs: Any) -> httpx.Response:
        return self._timed("DELETE", path, self._api.delete, path, **kwargs)

    def with_tenant(self, tenant_id: str, override_reason: str = "load-test") -> "LocustApiClient":
        return LocustApiClient(
            self._api.with_tenant(tenant_id, override_reason),
            self._request,
            run_context=self._context,
        )

    def close(self) -> None:
        self._api.close()

    # --- timing / reporting ------------------------------------------------

    def _timed(self, method: str, name: str, fn, *args: Any, **kwargs: Any) -> httpx.Response:
        start = time.perf_counter()
        exception: Exception | None = None
        response: httpx.Response | None = None
        length = 0
        try:
            response = fn(*args, **kwargs)
            length = len(response.content or b"")
            # Infra failure: any 5xx, or an unexpected 4xx. Expected business blocks
            # (limit / cutoff) belong to dedicated failure-path tasks, not this default path.
            if response.status_code >= 500:
                exception = RuntimeError(f"{method} {name} -> {response.status_code}")
            elif response.status_code >= 400:
                exception = RuntimeError(
                    f"{method} {name} -> {response.status_code} {response.text[:120]}"
                )
        except Exception as exc:  # network / timeout
            exception = exc
            raise
        finally:
            elapsed_ms = (time.perf_counter() - start) * 1000.0
            self._request.fire(
                request_type=method,
                name=name,
                response_time=elapsed_ms,
                response_length=length,
                response=response,
                context=self._context,
                exception=exception,
            )
        return response
