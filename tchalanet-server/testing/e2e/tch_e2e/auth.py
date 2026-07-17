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


def env_or_default(name: str, default: str) -> str:
    value = os.environ.get(name)
    if value is None or not value.strip():
        return default
    return value.strip()


class E2EAuth(Protocol):
    def password_grant(self, *, username: str, password: str) -> str: ...


@dataclass(frozen=True)
class FirebaseEmulatorAuth:
    """Mints unsigned (alg=none) Firebase-shaped tokens for the firebase-emulator provider.

    FirebaseEmulatorJwtDecoderConfig on the server accepts ONLY unsigned tokens whose issuer is
    ``https://securetoken.google.com/<projectId>`` and whose audience contains ``<projectId>``.
    The ``sub`` must equal the user's FIREBASE external subject; the server's Firebase bootstrap
    provisions each user with firebase uid = app_user id, so we reuse the same deterministic
    subjects as the local provider. Run the API with the emulator + bootstrap first
    (TCH_IDENTITY_PROVIDER=firebase-emulator, users=super_admin,admin,cashier).
    """

    project_id: str
    emulator_host: str

    @classmethod
    def from_env(cls) -> "FirebaseEmulatorAuth":
        return cls(
            project_id=env_or_default(
                "TCH_FIREBASE_PROJECT_ID",
                env_or_default("FIREBASE_PROJECT_ID", "demo-tchalanet-local"),
            ),
            emulator_host=env_or_default("TCH_FIREBASE_EMULATOR_HOST", "127.0.0.1:9099"),
        )

    def password_grant(self, *, username: str, password: str) -> str:
        del password  # emulator tokens are minted locally, no credential exchange
        identity = _local_identity(username)
        return self.mint(subject=identity.subject, email=identity.email)

    def mint(self, *, subject: str, email: str | None = None) -> str:
        """Mint a token for an arbitrary FIREBASE subject (uid).

        Use for dynamically-provisioned actors: a seller-terminal's subject is its
        sellerTerminalId (returned by create); a provisioned tenant admin's subject is the
        emulator-generated uid — resolve it with :meth:`uid_for_email` first.
        """
        now = int(time.time())
        claims = {
            "iss": f"https://securetoken.google.com/{self.project_id}",
            "aud": self.project_id,
            "sub": subject,
            "user_id": subject,
            "iat": now,
            "auth_time": now,
            "exp": now + 3600,
            "email_verified": True,
            "firebase": {"sign_in_provider": "password"},
        }
        if email:
            claims["email"] = email
            claims["firebase"] = {
                "identities": {"email": [email]},
                "sign_in_provider": "password",
            }
        return _unsigned_jwt(claims)

    def uid_for_email(self, email: str) -> str:
        """Look up the emulator-generated uid for *email* (Identity Toolkit query REST)."""
        url = (
            f"http://{self.emulator_host}/identitytoolkit.googleapis.com/v1/projects/"
            f"{self.project_id}/accounts:query"
        )
        resp = httpx.post(
            url, headers={"Authorization": "Bearer owner"}, json={}, timeout=15.0
        )
        if resp.status_code != 200:
            raise RuntimeError(
                f"Firebase emulator account lookup failed ({resp.status_code}): {resp.text}"
            )
        wanted = email.strip().lower()
        for user in resp.json().get("userInfo", []) or []:
            if (user.get("email") or "").strip().lower() == wanted:
                return user["localId"]
        raise RuntimeError(f"No Firebase emulator user with email {email!r}")


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
        return self.mint(
            subject=identity.subject,
            email=identity.email,
            username=identity.username,
            tenant_code=identity.tenant_code,
            role=identity.role,
        )

    def mint(
        self,
        *,
        subject: str,
        email: str | None = None,
        username: str | None = None,
        tenant_code: str | None = None,
        role: str = "SELLER_TERMINAL",
    ) -> str:
        now = int(time.time())
        claims = {
            "iss": self.issuer,
            "sub": subject,
            "iat": now,
            "exp": now + 3600,
            "email_verified": True,
            # Hint only. TchRequestContext replaces this with DB-owned authorization.
            "roles": [role],
        }
        if email:
            claims["email"] = email
        if username:
            claims["preferred_username"] = username
        if tenant_code:
            claims["tenant_code"] = tenant_code
        return _hs256(claims, self.secret)


@dataclass(frozen=True)
class _LocalIdentity:
    username: str
    subject: str
    email: str
    tenant_code: str
    role: str


def auth_from_env() -> E2EAuth:
    provider = os.environ.get("TCH_E2E_AUTH_PROVIDER", "firebase-emulator").strip().lower()
    if provider in {"local-jwt", "local-perf"}:
        return LocalJwtAuth.from_env()
    if provider in {"firebase-emulator", "firebase"}:
        return FirebaseEmulatorAuth.from_env()
    raise RuntimeError(
        "TCH_E2E_AUTH_PROVIDER must be one of: "
        "local-jwt, local-perf, firebase-emulator"
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


def _unsigned_jwt(claims: dict[str, object]) -> str:
    """Encode an alg=none JWT with an empty signature segment (trailing dot)."""
    header = {"alg": "none", "typ": "JWT"}
    return f"{_b64_json(header)}.{_b64_json(claims)}."


def _b64_json(value: dict[str, object]) -> str:
    return _b64(json.dumps(value, separators=(",", ":"), sort_keys=True).encode("utf-8"))


def _b64(value: bytes) -> str:
    return base64.urlsafe_b64encode(value).rstrip(b"=").decode("ascii")
