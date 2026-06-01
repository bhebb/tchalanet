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


# ===========================================================================
# Helpers
# ===========================================================================


def _anon_json(base_url: str, path: str) -> tuple[int, dict]:
    r = _anon_get(base_url, path)
    try:
        return r.status_code, r.json()
    except Exception:
        return r.status_code, {}


def _assert_api_success(body: dict) -> dict:
    assert body.get("status") == "SUCCESS", f"Unexpected envelope: {body}"
    assert "data" in body, f"Missing data in envelope: {body}"
    return body["data"]


# ===========================================================================
# L0/L1 — Public settings & i18n (anonymous runtime config)
# ===========================================================================


@pytest.mark.L0
@pytest.mark.public
def test_public_settings_reachable(base_url: str) -> None:
    """GET /public/settings — no auth, ApiResponse with a (possibly empty) list."""
    status, body = _anon_json(base_url, "/public/settings")
    assert status == 200, body
    data = _assert_api_success(body)
    assert isinstance(data, list), f"public settings data must be a list, got {type(data)}"


@pytest.mark.L1
@pytest.mark.public
def test_public_settings_namespace_filter(base_url: str) -> None:
    """Namespace filter is accepted and still returns a controlled list."""
    status, body = _anon_json(base_url, "/public/settings?namespace=public")
    assert status == 200, body
    assert isinstance(_assert_api_success(body), list)


@pytest.mark.L1
@pytest.mark.public
def test_public_i18n_bundle_loads(base_url: str) -> None:
    """GET /public/i18n?locale=fr&surface=PUBLIC_HOME — returns a populated bundle."""
    status, body = _anon_json(base_url, "/public/i18n?locale=fr&surface=PUBLIC_HOME")
    assert status == 200, body
    data = _assert_api_success(body)
    assert data.get("locale") == "fr"
    surfaces = data.get("surfaces", {})
    assert "PUBLIC_HOME" in surfaces, f"PUBLIC_HOME surface missing: {surfaces.keys()}"
    assert surfaces["PUBLIC_HOME"], "PUBLIC_HOME bundle must expose at least one key"


@pytest.mark.L1
@pytest.mark.public
def test_public_i18n_multiple_surfaces(base_url: str) -> None:
    """Multiple surface params resolve independently in one bundle."""
    status, body = _anon_json(
        base_url, "/public/i18n?locale=fr&surface=PUBLIC_HOME&surface=PUBLIC_RESULTS"
    )
    assert status == 200, body
    surfaces = _assert_api_success(body).get("surfaces", {})
    assert {"PUBLIC_HOME", "PUBLIC_RESULTS"} <= set(surfaces.keys()), surfaces.keys()


@pytest.mark.L1
@pytest.mark.public
def test_public_i18n_requires_locale(base_url: str) -> None:
    """Missing required locale param → controlled 400, not a 500."""
    status, _ = _anon_json(base_url, "/public/i18n?surface=PUBLIC_HOME")
    assert status == 400, f"Expected 400 for missing locale, got {status}"


# ===========================================================================
# L1 — Public news
# ===========================================================================


@pytest.mark.L1
@pytest.mark.public
def test_public_news_reachable(base_url: str) -> None:
    """GET /public/news — anonymous list of public content items."""
    status, body = _anon_json(base_url, "/public/news?limit=5&surface=PUBLIC_HOME")
    assert status == 200, body
    data = body if isinstance(body, list) else body.get("data", body)
    assert isinstance(data, list), f"news payload must be a list, got {type(data)}"


# ===========================================================================
# L1 — Public draw results
# ===========================================================================


@pytest.mark.L1
@pytest.mark.public
def test_public_draw_result_slots(base_url: str) -> None:
    """GET /public/draw-results/slots — slot list with provider/timezone metadata."""
    status, body = _anon_json(base_url, "/public/draw-results/slots")
    assert status == 200, body
    items = _assert_api_success(body).get("items")
    assert isinstance(items, list), f"slots.items must be a list: {body}"
    if items:
        slot = items[0]
        assert slot.get("slotKey") and slot.get("provider"), f"slot missing keys: {slot}"


@pytest.mark.L1
@pytest.mark.public
def test_public_draw_result_history(base_url: str) -> None:
    """GET /public/draw-results/history — reachable, ApiResponse shape."""
    status, body = _anon_json(base_url, "/public/draw-results/history")
    assert status == 200, body
    _assert_api_success(body)


# ===========================================================================
# L1/L2 — Public page model (home)
# ===========================================================================


@pytest.mark.L1
@pytest.mark.public
def test_public_home_page_model_loads(base_url: str) -> None:
    """GET /public/page-models/public.home — public scope, public_home context."""
    status, body = _anon_json(base_url, "/public/page-models/public.home")
    assert status == 200, body
    data = _assert_api_success(body)
    meta = (data.get("pageModel") or {}).get("meta", {})
    assert meta.get("id") == "public.home", f"unexpected page model id: {meta}"
    assert meta.get("scope") == "public", f"home page model must be public scope: {meta}"
    assert meta.get("context") == "public_home", f"expected public_home context: {meta}"


@pytest.mark.L2
@pytest.mark.public
def test_public_home_has_no_private_provider_source(base_url: str) -> None:
    """The anonymous home page model must not leak any private provider source.

    Private dashboards are served by providers like ``tenant_admin_dashboard`` /
    ``superadmin``. The public page must only use public/json_file sources.
    """
    import json

    status, body = _anon_json(base_url, "/public/page-models/public.home")
    assert status == 200, body
    serialized = json.dumps(_assert_api_success(body))
    for private_source in ("tenant_admin_dashboard", "superadmin", "private.dashboard"):
        assert private_source not in serialized, (
            f"Public home page leaks a private provider source: {private_source!r}"
        )


@pytest.mark.L1
@pytest.mark.public
def test_public_unknown_page_model_is_controlled(base_url: str) -> None:
    """Unknown logicalId → controlled 404, never a 500."""
    status, _ = _anon_json(base_url, "/public/page-models/does.not.exist")
    assert status == 404, f"Expected 404 for unknown page model, got {status}"


# ===========================================================================
# L1 — Public ticket check
# ===========================================================================


@pytest.mark.L1
@pytest.mark.public
def test_public_ticket_check_unknown_is_controlled(base_url: str) -> None:
    """Unknown ticket code → controlled 4xx (ticket.not_found), never a 500."""
    status, body = _anon_json(
        base_url, "/public/tickets/UNKNOWN123/verify?verificationCode=000000"
    )
    assert status in (404, 422, 400), f"Expected controlled 4xx, got {status}: {body}"
    assert status < 500
    detail = body.get("detail", "")
    assert "ticket" in str(detail).lower() or status == 404, f"unexpected detail: {body}"


# ===========================================================================
# L1 — Public tchala (dream→number lookup)
# ===========================================================================


@pytest.mark.L1
@pytest.mark.public
def test_public_tchala_search(base_url: str) -> None:
    """GET /public/tchala/search — paginated public lookup, ApiResponse shape."""
    status, body = _anon_json(base_url, "/public/tchala/search?q=lapli&lang=fr")
    assert status == 200, body
    data = _assert_api_success(body)
    assert "items" in data and "page" in data, f"unexpected page shape: {data}"


# ===========================================================================
# L3 — Public endpoints under parallel anonymous load
# ===========================================================================


@pytest.mark.L3
@pytest.mark.public
def test_public_endpoints_handle_parallel_reads(base_url: str) -> None:
    """Several public endpoints hit concurrently → all 200, never a 5xx."""
    from tch_e2e.concurrency import run_concurrent

    paths = [
        "/public/settings",
        "/public/i18n?locale=fr&surface=PUBLIC_HOME",
        "/public/news?limit=3",
        "/public/draw-results/slots",
        "/public/page-models/public.home",
        "/public/security/backend-signing-keys",
    ]

    def fire():
        return [(_anon_get(base_url, p).status_code, p) for p in paths]

    results = run_concurrent(fire, n=5)
    for res in results:
        assert not isinstance(res, Exception), f"parallel public read raised: {res!r}"
        for code, path in res:
            assert code < 500, f"public {path} returned {code} under concurrency"
            assert code == 200, f"public {path} expected 200, got {code}"
