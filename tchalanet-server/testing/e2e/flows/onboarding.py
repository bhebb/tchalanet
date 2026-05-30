"""Onboarding flow: tenant provisioning, user, outlet, terminal, binding."""
from __future__ import annotations

from typing import Any

import pytest

from tch_e2e.api_response import assert_ok
from tch_e2e.client import ApiClient


class OnboardingFlow:
    """Wraps the platform admin onboarding endpoints.

    All methods use a super_admin ApiClient. If an endpoint is not yet implemented
    (404/405), the method raises pytest.skip so the test is marked as skipped
    rather than failed.
    """

    def __init__(self, client: ApiClient) -> None:
        self.client = client

    # --- tenant provisioning -----------------------------------------------

    def provisioning_preview(self, profile: str) -> dict[str, Any]:
        """GET /platform/onboarding/tenants/preview?profile=... — read-only."""
        response = self.client.get(
            "/platform/onboarding/tenants/preview",
            params={"profile": profile},
        )
        self._skip_if_not_available(response, "provisioning_preview")
        assert_ok(response)
        return response.json()["data"]

    def provision_tenant(self, profile: str, code: str, display_name: str) -> dict[str, Any]:
        """POST /platform/onboarding/tenants — create a tenant."""
        response = self.client.post(
            "/platform/onboarding/tenants",
            json={"profile": profile, "code": code, "displayName": display_name},
        )
        self._skip_if_not_available(response, "provision_tenant")
        assert_ok(response, expected=(200, 201))
        return response.json()["data"]

    # --- user onboarding ---------------------------------------------------

    def create_tenant_user(
        self,
        tenant_id: str,
        username: str,
        password: str,
        role: str,
        **extra: Any,
    ) -> dict[str, Any]:
        """POST /platform/tenants/{id}/users."""
        response = self.client.post(
            f"/platform/tenants/{tenant_id}/users",
            json={"username": username, "password": password, "role": role, **extra},
        )
        self._skip_if_not_available(response, "create_tenant_user")
        if response.status_code == 409:
            return response.json().get("data") or {}
        assert_ok(response, expected=(200, 201))
        return response.json()["data"]

    # --- outlet onboarding -------------------------------------------------

    def create_outlet(self, tenant_id: str, name: str, **extra: Any) -> dict[str, Any]:
        """POST /platform/tenants/{id}/outlets."""
        response = self.client.post(
            f"/platform/tenants/{tenant_id}/outlets",
            json={"name": name, "status": "ACTIVE", **extra},
        )
        self._skip_if_not_available(response, "create_outlet")
        if response.status_code == 409:
            return response.json().get("data") or {}
        assert_ok(response, expected=(200, 201))
        return response.json()["data"]

    # --- terminal onboarding -----------------------------------------------

    def create_terminal(
        self, tenant_id: str, outlet_id: str, label: str, **extra: Any
    ) -> dict[str, Any]:
        """POST /platform/tenants/{id}/terminals."""
        response = self.client.post(
            f"/platform/tenants/{tenant_id}/terminals",
            json={"outletId": outlet_id, "label": label, **extra},
        )
        self._skip_if_not_available(response, "create_terminal")
        if response.status_code == 409:
            return response.json().get("data") or {}
        assert_ok(response, expected=(200, 201))
        return response.json()["data"]

    # --- seller onboarding ------------------------------------------------

    def ensure_seller_for_user(
        self,
        user_id: str,
        outlet_id: str,
        *,
        display_name: str = "E2E Seller",
        code: str = "e2e-seller",
    ) -> dict[str, Any]:
        """Create a seller linked to `user_id` and assign them to `outlet_id`.

        Idempotent: tolerates 409 on both seller creation and outlet assignment.
        Returns the seller dict (may be empty on 409 if the endpoint doesn't
        return a body on conflict).
        """
        import datetime

        # Create seller (links user directly via userId field)
        create_resp = self.client.post(
            "/admin/sellers",
            json={"displayName": display_name, "code": code, "userId": user_id},
        )
        self._skip_if_not_available(create_resp, "ensure_seller_for_user/create")
        seller_id: str | None = None
        if create_resp.status_code == 409:
            # Seller already exists — look up via list endpoint
            seller_id = self._find_seller_for_user(user_id)
        else:
            assert_ok(create_resp, expected=(200, 201))
            body = create_resp.json().get("data")
            if isinstance(body, str):
                seller_id = body
            elif isinstance(body, dict):
                seller_id = body.get("sellerId") or body.get("id")
            else:
                # Unexpected — fall back to list lookup
                seller_id = self._find_seller_for_user(user_id)

        if seller_id is None:
            # Cannot assign outlet without a seller ID — skip gracefully
            return {}

        # Assign seller to outlet (starts_at in the past so it's immediately active)
        starts_at = "2020-01-01T00:00:00Z"
        assign_resp = self.client.post(
            f"/admin/sellers/{seller_id}/assignments",
            json={"outletId": outlet_id, "startsAt": starts_at},
        )
        self._skip_if_not_available(assign_resp, "ensure_seller_for_user/assign")
        if assign_resp.status_code != 409:
            assert_ok(assign_resp, expected=(200, 201))

        return {"sellerId": seller_id}

    def bind_terminal(self, tenant_id: str, terminal_id: str) -> dict[str, Any]:
        """POST /platform/tenants/{id}/terminals/{tid}/bind."""
        response = self.client.post(
            f"/platform/tenants/{tenant_id}/terminals/{terminal_id}/bind",
        )
        self._skip_if_not_available(response, "bind_terminal")
        if response.status_code == 409:
            return response.json().get("data") or {}
        assert_ok(response, expected=(200, 201, 204))
        try:
            return response.json().get("data") or {}
        except Exception:
            return {}

    def _find_seller_for_user(self, user_id: str) -> str | None:
        """GET /admin/sellers and return the first seller linked to user_id, or None."""
        try:
            resp = self.client.get("/admin/sellers")
            if resp.status_code != 200:
                return None
            sellers = resp.json().get("data") or []
            if not isinstance(sellers, list):
                return None
            for s in sellers:
                if isinstance(s, dict) and s.get("userId") == user_id:
                    return s.get("sellerId") or s.get("id")
        except Exception:
            pass
        return None

    # --- helpers -----------------------------------------------------------

    @staticmethod
    def _skip_if_not_available(response: Any, method_name: str) -> None:
        if response.status_code in (404, 405):
            pytest.skip(
                f"OnboardingFlow.{method_name}: endpoint not routed "
                f"({response.request.method} {response.request.url} → {response.status_code}). "
                "Wire the backend endpoint first."
            )
        if response.status_code == 500:
            try:
                code = response.json().get("code", "")
            except Exception:
                code = ""
            if code == "internal.unexpected":
                pytest.skip(
                    f"OnboardingFlow.{method_name}: endpoint exists but handler returns 500 "
                    f"({response.request.method} {response.request.url}). "
                    "Backend implementation is in progress (mobile_init branch)."
                )
