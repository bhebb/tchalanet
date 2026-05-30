"""Profile flow: GET / PATCH /tenant/me/profile."""
from __future__ import annotations

from typing import Any

from tch_e2e.api_response import assert_ok
from tch_e2e.client import ApiClient


class ProfileFlow:
    """Wraps the current-user profile endpoints."""

    def __init__(self, client: ApiClient) -> None:
        self.client = client

    def me(self) -> dict[str, Any]:
        response = self.client.get("/tenant/me/profile")
        assert_ok(response)
        return response.json()["data"]

    def update(
        self,
        *,
        first_name: str | None = None,
        last_name: str | None = None,
        phone: str | None = None,
        locale: str | None = None,
    ) -> dict[str, Any]:
        body: dict[str, Any] = {}
        if first_name is not None:
            body["firstName"] = first_name
        if last_name is not None:
            body["lastName"] = last_name
        if phone is not None:
            body["phone"] = phone
        if locale is not None:
            body["locale"] = locale
        response = self.client.patch("/tenant/me/profile", json=body)
        assert_ok(response)
        return response.json()["data"]
