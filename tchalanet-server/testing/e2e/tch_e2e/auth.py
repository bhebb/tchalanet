"""Interchangeable E2E authentication providers."""
from __future__ import annotations

import base64
import hashlib
import hmac
import json
import os
import time
from dataclasses import dataclass
from typing import Protocol

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


class E2EAuth(Protocol):
    def password_grant(self, *, username: str, password: str) -> str: ...


@dataclass(frozen=True)
class LocalJwtAuth:
    issuer: str
    secret: str

    @classmethod
    def from_env(cls) -> "LocalJwtAuth":
        secret = os.environ["TCH_LOCAL_JWT_SECRET"]
        if len(secret) < 32:
            raise RuntimeError("TCH_LOCAL_JWT_SECRET must contain at least 32 characters")
        return cls(
            issuer=os.environ.get("TCH_LOCAL_JWT_ISSUER", "tchalanet-local"),
            secret=secret,
        )

    def password_grant(self, *, username: str, password: str) -> str:
        del password
        identity = _local_identity(username)
        now = int(time.time())
        return _hs256(
            {
                "iss": self.issuer,
                "sub": identity.subject,
                "iat": now,
                "exp": now + 3600,
                "email": identity.email,
                "email_verified": True,
                "preferred_username": identity.username,
                "tenant_code": identity.tenant_code,
                # Hint only. TchRequestContext replaces this with DB-owned authorization.
                "roles": [identity.role],
            },
            self.secret,
        )


@dataclass(frozen=True)
class _LocalIdentity:
    username: str
    subject: str
    email: str
    tenant_code: str
    role: str


def auth_from_env() -> E2EAuth:
    provider = os.environ.get("TCH_E2E_AUTH_PROVIDER", "keycloak").strip().lower()
    if provider == "keycloak":
        return KeycloakAuth.from_env()
    if provider in {"local-jwt", "local-perf"}:
        return LocalJwtAuth.from_env()
    raise RuntimeError(
        "TCH_E2E_AUTH_PROVIDER must be one of: keycloak, local-jwt, local-perf"
    )


def _local_identity(username: str) -> _LocalIdentity:
    normalized = username.strip().lower()
    defaults = {
        os.environ.get("TCH_SUPER_ADMIN_USERNAME", "super_admin").lower(): (
            "SUPER_ADMIN",
            "00000000-0000-0000-0000-000000010001",
            "super_admin@localtest.me",
        ),
        os.environ.get("TCH_TENANT_ADMIN_USERNAME", "admin").lower(): (
            "TENANT_ADMIN",
            "00000000-0000-0000-0000-000000010002",
            "admin@localtest.me",
        ),
        os.environ.get("TCH_SELLER_USERNAME", "cashier").lower(): (
            "CASHIER",
            "00000000-0000-0000-0000-000000010003",
            "cashier@localtest.me",
        ),
    }
    selected = defaults.get(normalized)
    if selected is None:
        raise RuntimeError(
            f"Local E2E auth has no pre-provisioned external identity for username {username!r}"
        )
    role, subject, email = selected
    return _LocalIdentity(
        username=username,
        subject=subject,
        email=email,
        tenant_code=os.environ.get("TCH_TENANT_CODE", "tchalanet"),
        role=role,
    )


def _hs256(claims: dict[str, object], secret: str) -> str:
    header = {"alg": "HS256", "typ": "JWT"}
    signing_input = f"{_b64_json(header)}.{_b64_json(claims)}"
    signature = hmac.new(
        secret.encode("utf-8"), signing_input.encode("ascii"), hashlib.sha256
    ).digest()
    return f"{signing_input}.{_b64(signature)}"


def _b64_json(value: dict[str, object]) -> str:
    return _b64(json.dumps(value, separators=(",", ":"), sort_keys=True).encode("utf-8"))


def _b64(value: bytes) -> str:
    return base64.urlsafe_b64encode(value).rstrip(b"=").decode("ascii")
