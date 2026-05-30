"""Cashier home flow: GET /tenant/cashier/home (mobile/POS surface)."""
from __future__ import annotations

from typing import Any

from tch_e2e.api_response import assert_ok
from tch_e2e.client import ApiClient
from tch_e2e.config import OpContext


class HomeFlow:
    """Wraps the cashier home BFF endpoints.

    `mobile_home` is the compact action-first POS payload (MOBILE_POS surface).
    `web_home` is the widget-based dashboard (CASHIER_WEB surface).
    """

    SURFACE_HEADER = "X-Tch-Surface"

    def __init__(self, client: ApiClient, context: OpContext | None = None) -> None:
        self.client = client
        self.context = context

    def mobile_home(self, *, surface: str | None = "MOBILE_POS") -> dict[str, Any]:
        return self._get("/tenant/cashier/home", surface)

    def web_home(self, *, surface: str | None = "CASHIER_WEB") -> dict[str, Any]:
        return self._get("/tenant/cashier/web-home", surface)

    def _get(self, path: str, surface: str | None) -> dict[str, Any]:
        headers = {self.SURFACE_HEADER: surface} if surface else None
        response = self.client.get(path, context=self.context, headers=headers)
        assert_ok(response)
        return response.json()["data"]
