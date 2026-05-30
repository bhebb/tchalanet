"""Keycloak password-grant helper."""
from __future__ import annotations

import os
from dataclasses import dataclass

import httpx


def _resolve_verify() -> bool | str:
    raw = os.environ.get("TCH_E2E_VERIFY_SSL", "false").strip()
    if raw.lower() in {"true", "1", "yes"}:
        return True
    if raw.lower() in {"false", "0", "no", ""}:
        return False
    return raw


@dataclass(frozen=True)
class KeycloakAuth:
    token_url: str
    client_id: str
    client_secret: str | None

    @classmethod
    def from_env(cls) -> "KeycloakAuth":
        return cls(
            token_url=os.environ["TCH_KEYCLOAK_TOKEN_URL"],
            client_id=os.environ.get("TCH_KEYCLOAK_CLIENT_ID", "tchalanet-swagger"),
            client_secret=os.environ.get("TCH_KEYCLOAK_CLIENT_SECRET") or None,
        )

    def password_grant(self, *, username: str, password: str) -> str:
        data = {
            "grant_type": "password",
            "client_id": self.client_id,
            "username": username,
            "password": password,
            "scope": "openid",
        }
        if self.client_secret:
            data["client_secret"] = self.client_secret
        response = httpx.post(self.token_url, data=data, timeout=15.0, verify=_resolve_verify())
        if response.status_code != 200:
            raise RuntimeError(
                f"Keycloak token request failed ({response.status_code}): {response.text}"
            )
        return response.json()["access_token"]
