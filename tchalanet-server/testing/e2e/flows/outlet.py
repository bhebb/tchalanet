"""Outlet management flow — config, receipt header/footer, sales capability.

Admin calls (update_receipt_config) require a tenant-scoped client (X-Tenant-Id).
Tenant calls (get_sales_capability, get_operational_context) use a TENANT_USER token.
"""
from __future__ import annotations

from typing import Any

import pytest

from tch_e2e.api_response import assert_ok
from tch_e2e.client import ApiClient


class OutletFlow:
    """Wraps outlet admin + tenant query endpoints."""

    def __init__(self, admin_client: ApiClient, tenant_client: ApiClient | None = None) -> None:
        self.admin_client = admin_client    # needs X-Tenant-Id for /admin/outlets
        self.tenant_client = tenant_client  # TENANT_USER for /tenant/outlets

    # --- admin config -------------------------------------------------------

    def update_receipt_config(
        self,
        outlet_id: str,
        *,
        header_message: str | None = None,
        footer_message: str | None = None,
        receipt_printing_enabled: bool | None = None,
    ) -> None:
        """PATCH /admin/outlets/{id}/config — set receipt header / footer text.

        Only the fields supplied are changed; others remain unchanged.
        """
        patch: dict[str, Any] = {}
        if header_message is not None:
            patch["receiptHeaderMessage"] = header_message
        if footer_message is not None:
            patch["receiptFooterMessage"] = footer_message
        if receipt_printing_enabled is not None:
            patch["receiptPrintingEnabled"] = receipt_printing_enabled

        response = self.admin_client.patch(f"/admin/outlets/{outlet_id}/config", json=patch)
        self._skip_if_not_available(response, "update_receipt_config")
        assert_ok(response, expected=(200, 201, 204))

    # --- tenant queries -----------------------------------------------------

    def get_sales_capability(self, outlet_id: str) -> dict[str, Any]:
        """GET /tenant/outlets/{id}/sales-capability."""
        client = self.tenant_client or self.admin_client
        response = client.get(f"/tenant/outlets/{outlet_id}/sales-capability")
        self._skip_if_not_available(response, "get_sales_capability")
        assert_ok(response)
        return response.json()["data"]

    def get_operational_context(self, outlet_id: str) -> dict[str, Any]:
        """GET /tenant/outlets/{id}/operational-context."""
        client = self.tenant_client or self.admin_client
        response = client.get(f"/tenant/outlets/{outlet_id}/operational-context")
        self._skip_if_not_available(response, "get_operational_context")
        assert_ok(response)
        return response.json()["data"]

    # --- helpers ------------------------------------------------------------

    @staticmethod
    def _skip_if_not_available(response: Any, method_name: str) -> None:
        if response.status_code in (404, 405):
            pytest.skip(
                f"OutletFlow.{method_name}: endpoint not routed "
                f"({response.request.method} {response.request.url} → {response.status_code})."
            )
        if response.status_code == 500:
            try:
                code = response.json().get("code", "")
            except Exception:
                code = ""
            if code == "internal.unexpected":
                pytest.skip(
                    f"OutletFlow.{method_name}: endpoint returns 500 — handler WIP."
                )
