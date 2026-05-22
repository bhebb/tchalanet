"""Profile flow: GET / PATCH /tenant/me/profile (platform.identity)."""
from __future__ import annotations

from typing import Any

from lib.api import ApiClient, assert_ok


class ProfileFlow:
    """Wraps the current-user profile endpoints exposed by platform.identity.

    All endpoints live under `/tenant/me/profile` and are role-agnostic
    (any authenticated user can read/patch their own profile).
    """

    def __init__(self, client: ApiClient) -> None:
        self.client = client

    def me(self) -> dict[str, Any]:
        """GET /tenant/me/profile → MeResponse (full landing payload)."""
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
        """PATCH /tenant/me/profile → UserResponse.

        Only the fields explicitly passed are sent. The server treats nulls as
        "no change", so we strip them client-side too to keep the payload tight.
        """
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
