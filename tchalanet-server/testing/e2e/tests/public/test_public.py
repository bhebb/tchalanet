"""Public runtime tests — no auth required.

Verifies that public endpoints are reachable without a token and return
the expected structure. These are smoke contracts, not business truth.
"""
from __future__ import annotations

import httpx
import pytest

from tch_e2e.client import _resolve_verify


def _anon_get(base_url: str, path: str) -> httpx.Response:
    """Unauthenticated GET — public endpoints only."""
    url = base_url.rstrip("/") + path
    return httpx.get(url, timeout=10.0, verify=_resolve_verify(), follow_redirects=True)


# ===========================================================================
# L0 — Backend public signing key
# ===========================================================================


@pytest.mark.L0
@pytest.mark.public
def test_backend_signing_keys_reachable(base_url: str) -> None:
    """GET /public/security/backend-signing-keys — no auth, returns 200."""
    response = _anon_get(base_url, "/public/security/backend-signing-keys")
    assert response.status_code == 200, (
        f"Expected 200, got {response.status_code}: {response.text[:300]}"
    )


@pytest.mark.L0
@pytest.mark.public
def test_backend_signing_keys_shape(base_url: str) -> None:
    """Response has status=SUCCESS, activeKeyId, and at least one ACTIVE ED25519 key."""
    response = _anon_get(base_url, "/public/security/backend-signing-keys")
    assert response.status_code == 200
    body = response.json()

    assert body.get("status") == "SUCCESS", f"Unexpected status: {body.get('status')}"

    data = body.get("data", {})
    assert data.get("activeKeyId"), "activeKeyId must be non-empty"

    keys = data.get("keys", [])
    assert len(keys) >= 1, "At least one key must be present"

    active_key = next((k for k in keys if k.get("status") == "ACTIVE"), None)
    assert active_key is not None, f"No ACTIVE key found in: {keys}"
    assert active_key.get("algorithm") == "ED25519", (
        f"Expected ED25519, got {active_key.get('algorithm')}"
    )
    assert active_key.get("publicKeyFormat") == "SPKI_BASE64", (
        f"Expected SPKI_BASE64 format, got {active_key.get('publicKeyFormat')}"
    )
    assert active_key.get("publicKey"), "publicKey must be non-empty"
    assert active_key.get("keyId") == data.get("activeKeyId"), (
        "ACTIVE key id must match activeKeyId"
    )


@pytest.mark.L0
@pytest.mark.public
def test_backend_signing_keys_not_ephemeral(base_url: str) -> None:
    """The active key must be stable across two consecutive calls (not regenerated per-request)."""
    r1 = _anon_get(base_url, "/public/security/backend-signing-keys")
    r2 = _anon_get(base_url, "/public/security/backend-signing-keys")
    assert r1.status_code == 200 and r2.status_code == 200

    key1 = r1.json()["data"]["keys"][0]["publicKey"]
    key2 = r2.json()["data"]["keys"][0]["publicKey"]
    assert key1 == key2, (
        "Public key changed between requests — ephemeral key detected. "
        "Set TCH_KEYMANAGEMENT_SERVER_SIGNING_PUBLIC_KEY_SPKI_BASE64 in .secrets."
    )
