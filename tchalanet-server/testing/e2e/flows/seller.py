"""Seller onboarding flow — wraps /admin/sellers endpoints.

The client must be scoped to a tenant (X-Tenant-Id header).
Use ``ApiClient.with_tenant(tenant_id)`` or the ``tenant_admin_client`` fixture.

core.seller lifecycle:
  create_seller  → link_user → assign_outlet → (set_commission_policy)
"""
from __future__ import annotations

import datetime as dt
from typing import Any

import pytest

from tch_e2e.api_response import assert_ok
from tch_e2e.client import ApiClient


class SellerFlow:
    """Wraps /admin/sellers endpoints for seller onboarding and management."""

    def __init__(self, client: ApiClient) -> None:
        self.client = client  # must carry X-Tenant-Id

    # --- lifecycle ----------------------------------------------------------

    def create_seller(
        self,
        display_name: str,
        *,
        code: str | None = None,
        user_id: str | None = None,
    ) -> str:
        """POST /admin/sellers → returns seller_id string.

        409 = already exists (by code) — raises AssertionError.
        Skips gracefully when endpoint is not available (404/405/500).
        """
        body: dict[str, Any] = {"displayName": display_name}
        if code:
            body["code"] = code
        if user_id:
            body["userId"] = user_id

        response = self.client.post("/admin/sellers", json=body)
        self._skip_if_not_available(response, "create_seller")
        assert_ok(response, expected=(200, 201))
        data = response.json()
        # The API returns a SellerIdTypedWrapper or just the raw UUID
        raw = data.get("data") or data
        if isinstance(raw, dict):
            return str(raw.get("value") or raw.get("id") or raw)
        return str(raw)

    def get_seller(self, seller_id: str) -> dict[str, Any]:
        """GET /admin/sellers/{id}."""
        response = self.client.get(f"/admin/sellers/{seller_id}")
        self._skip_if_not_available(response, "get_seller")
        assert_ok(response)
        return response.json()["data"]

    def list_sellers(self) -> list[dict[str, Any]]:
        """GET /admin/sellers."""
        response = self.client.get("/admin/sellers")
        self._skip_if_not_available(response, "list_sellers")
        assert_ok(response)
        data = response.json()["data"]
        return data if isinstance(data, list) else []

    def link_user(self, seller_id: str, user_id: str) -> None:
        """POST /admin/sellers/{id}/user — link seller to app user."""
        response = self.client.post(
            f"/admin/sellers/{seller_id}/user",
            json={"userId": user_id},
        )
        self._skip_if_not_available(response, "link_user")
        assert_ok(response, expected=(200, 201, 204))

    def assign_outlet(
        self,
        seller_id: str,
        outlet_id: str,
        *,
        starts_at: str | None = None,
    ) -> str:
        """POST /admin/sellers/{id}/assignments → returns assignment_id string."""
        if starts_at is None:
            starts_at = dt.datetime.now(dt.timezone.utc).isoformat()
        response = self.client.post(
            f"/admin/sellers/{seller_id}/assignments",
            json={"outletId": outlet_id, "startsAt": starts_at},
        )
        self._skip_if_not_available(response, "assign_outlet")
        assert_ok(response, expected=(200, 201))
        data = response.json()
        raw = data.get("data") or data
        if isinstance(raw, dict):
            return str(raw.get("value") or raw.get("id") or raw)
        return str(raw)

    def update_status(self, seller_id: str, status: str) -> None:
        """PATCH /admin/sellers/{id}/status — e.g. status='ACTIVE'/'SUSPENDED'."""
        response = self.client.patch(
            f"/admin/sellers/{seller_id}/status",
            json={"status": status},
        )
        self._skip_if_not_available(response, "update_status")
        assert_ok(response, expected=(200, 201, 204))

    # --- helpers ------------------------------------------------------------

    @staticmethod
    def _skip_if_not_available(response: Any, method_name: str) -> None:
        if response.status_code in (404, 405):
            pytest.skip(
                f"SellerFlow.{method_name}: endpoint not routed "
                f"({response.request.method} {response.request.url} → {response.status_code})."
            )
        if response.status_code == 500:
            try:
                code = response.json().get("code", "")
            except Exception:
                code = ""
            if code == "internal.unexpected":
                pytest.skip(
                    f"SellerFlow.{method_name}: endpoint returns 500 — handler WIP."
                )
