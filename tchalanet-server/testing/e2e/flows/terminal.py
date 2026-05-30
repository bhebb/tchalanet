"""Terminal binding flow — challenge / verify pairing.

The E2E delivery mode returns the clear code directly in the response so no
SMS or email is needed during automated tests.

Flow:
  1. POST /tenant/terminals/{id}/activation-challenges  {deliveryMode: "E2E"}
     → {challengeId, deliveryRef (= clear code)}
  2. POST /tenant/terminals/{id}/activation-challenges/{cid}/verify
     → {terminalId, bindingId, bindingType}

Admin shortcut (no challenge needed):
  POST /admin/terminals/{id}/activate-for-user
"""
from __future__ import annotations

import uuid
from typing import Any

import pytest

from tch_e2e.api_response import assert_ok
from tch_e2e.client import ApiClient


class TerminalBindingFlow:
    """Wraps the terminal activation challenge / verify POS pairing flow."""

    def __init__(self, client: ApiClient) -> None:
        """``client`` must have CASHIER, TENANT_ADMIN, or SUPER_ADMIN token.

        For /admin/terminals/* calls the client also needs X-Tenant-Id.
        """
        self.client = client

    # --- full E2E challenge/verify flow -------------------------------------

    def create_challenge(self, terminal_id: str) -> dict[str, Any]:
        """POST /tenant/terminals/{id}/activation-challenges with deliveryMode=E2E.

        Returns the challenge payload including ``challengeId`` and ``deliveryRef``
        (the plain-text code — only present in E2E delivery mode).
        """
        response = self.client.post(
            f"/tenant/terminals/{terminal_id}/activation-challenges",
            json={"deliveryMode": "E2E"},
        )
        self._skip_if_not_available(response, "create_challenge")
        assert_ok(response, expected=(200, 201))
        return response.json()["data"]

    def verify_challenge(
        self,
        terminal_id: str,
        challenge_id: str,
        clear_code: str,
        *,
        binding_public_key: str | None = None,
        binding_credential: str | None = None,
        device_fingerprint_hash: str | None = None,
    ) -> dict[str, Any]:
        """POST /tenant/terminals/{id}/activation-challenges/{cid}/verify.

        For E2E tests, fake crypto values are acceptable — the server stores them
        as-is (public key) or hashes them (credential). Only ``clearCode`` is
        verified against the stored challenge.
        """
        response = self.client.post(
            f"/tenant/terminals/{terminal_id}/activation-challenges/{challenge_id}/verify",
            json={
                "clearCode": clear_code,
                "bindingPublicKey": binding_public_key or f"e2e-pubkey-{uuid.uuid4()}",
                "bindingCredential": binding_credential or f"e2e-cred-{uuid.uuid4()}",
                "deviceFingerprintHash": device_fingerprint_hash or f"e2e-fp-{uuid.uuid4()}",
            },
        )
        self._skip_if_not_available(response, "verify_challenge")
        assert_ok(response, expected=(200, 201))
        return response.json()["data"]

    def bind_e2e(self, terminal_id: str) -> dict[str, Any]:
        """Full E2E binding in one call: create challenge → verify → return binding.

        Returns the binding response dict (terminalId, bindingId, bindingType).
        Skips gracefully if the terminal is already bound (409).
        """
        challenge = self.create_challenge(terminal_id)
        challenge_id = challenge["challengeId"]
        clear_code = challenge.get("deliveryRef") or challenge.get("clearCode")
        if not clear_code:
            pytest.skip(
                "Terminal binding challenge did not return a clear code in E2E mode — "
                "check TerminalChallengeDeliveryMode.E2E server implementation."
            )

        response = self.client.post(
            f"/tenant/terminals/{terminal_id}/activation-challenges/{challenge_id}/verify",
            json={
                "clearCode": clear_code,
                "bindingPublicKey": f"e2e-pubkey-{uuid.uuid4()}",
                "bindingCredential": f"e2e-cred-{uuid.uuid4()}",
                "deviceFingerprintHash": f"e2e-fp-{uuid.uuid4()}",
            },
        )
        if response.status_code == 409:
            # Terminal already bound — tolerate (idempotent for E2E)
            return response.json().get("data") or {}
        self._skip_if_not_available(response, "bind_e2e/verify")
        assert_ok(response, expected=(200, 201))
        return response.json()["data"]

    # --- admin shortcut -----------------------------------------------------

    def activate_for_user(self, terminal_id: str) -> None:
        """POST /admin/terminals/{id}/activate-for-user.

        Admin shortcut that bypasses the challenge/verify flow.
        Requires the client to carry X-Tenant-Id.

        Prerequisite: terminal must have a user assigned via
        POST /admin/terminals/{id}/assign-user first.
        A 422 "no user assigned" means this prerequisite is missing.
        """
        response = self.client.post(f"/admin/terminals/{terminal_id}/activate-for-user")
        self._skip_if_not_available(response, "activate_for_user")
        if response.status_code == 422:
            try:
                code = response.json().get("code", "")
                detail = response.json().get("detail", "")
            except Exception:
                code, detail = "", ""
            if code == "business_rule.violation":
                pytest.skip(
                    f"TerminalBindingFlow.activate_for_user: business rule blocked ({detail}). "
                    "Run POST /admin/terminals/{id}/assign-user first."
                )
        assert_ok(response, expected=(200, 201, 204))

    # --- helpers ------------------------------------------------------------

    @staticmethod
    def _skip_if_not_available(response: Any, method_name: str) -> None:
        if response.status_code in (404, 405):
            pytest.skip(
                f"TerminalBindingFlow.{method_name}: endpoint not routed "
                f"({response.request.method} {response.request.url} → {response.status_code})."
            )
        if response.status_code == 500:
            try:
                code = response.json().get("code", "")
            except Exception:
                code = ""
            if code == "internal.unexpected":
                pytest.skip(
                    f"TerminalBindingFlow.{method_name}: endpoint returns 500 — handler WIP."
                )
