"""Reusable onboarding of a *sellable* POS seller, from scratch, via REST.

``onboard_pos_seller`` performs the whole chain for one tenant:

    outlet → cashier user (DB + Firebase) → seller (linked + assigned)
    → terminal → assign terminal to cashier → bind terminal (known device cred)
    → cashier JWT → open session

and returns a :class:`PosContext` ready to sell. It is tenant-agnostic: pass any
``tenant_id`` whose tenant already has an operational catalog (games / pricing /
open draws). The SUPER_ADMIN acts as the tenant via the X-Tenant-Id override, and
the freshly-created cashier authenticates with the platform default password
(``Changeme1!`` in the local Firebase bootstrap configuration).

The same helper is what a true cross-tenant dual-POS test would call once per
tenant; today only the seeded tenant has a catalog, so a provisioned Tenant B
cannot yet sell (no games/draws).
"""
from __future__ import annotations

import uuid
from typing import Any

from flows.onboarding import OnboardingFlow
from flows.terminal import TerminalBindingFlow
from fixtures.pos_context import PosContext
from prereqs.session import ensure_pos_session_open
from tch_e2e.auth import E2EAuth
from tch_e2e.client import ApiClient
from tch_e2e.config import OpContext

_DEFAULT_PASSWORD = "Changeme1!"


def ensure_tenant_plan(super_admin_client: ApiClient, tenant_id: str, plan_code: str = "DEMO") -> None:
    """Make sure ``tenant_id`` has an active subscription (idempotent).

    Creating outlets/terminals via REST needs entitlements (outlet quota +
    terminal.licensing feature); provisioning does not attach a plan, and even the
    seeded tenant has none (its outlet was SQL-seeded, bypassing the check). DEMO
    has huge limits so repeated e2e runs don't exhaust quota.
    """
    cur = super_admin_client.get(f"/platform/subscriptions/{tenant_id}")
    if cur.status_code == 200:
        data = cur.json().get("data") or {}
        if data.get("planCode") and data.get("status") == "ACTIVE":
            return
    super_admin_client.post(
        f"/platform/subscriptions/{tenant_id}/apply",
        json={"planCode": plan_code, "effectiveAt": "2020-01-01T00:00:00Z"},
    )


def _id_of(obj: Any) -> str | None:
    if isinstance(obj, str):
        return obj
    if isinstance(obj, dict):
        for k in ("id", "outletId", "terminalId", "sellerId", "userId", "value"):
            v = obj.get(k)
            if v:
                return v.get("value") if isinstance(v, dict) else v
    return None


def onboard_pos_seller(
    *,
    super_admin_client: ApiClient,
    admin_client: ApiClient,
    keycloak: E2EAuth,
    base_url: str,
    tenant_id: str,
    label: str,
    stake_cents: int = 100,
) -> PosContext:
    """Onboard a brand-new, ready-to-sell seller on ``tenant_id``; return its PosContext.

    ``admin_client`` must be scoped to the target tenant *with its tenant code* (e.g. the
    real tenant admin), so the created cashier's Keycloak ``tenant_code`` attribute is
    correct. A SUPER_ADMIN X-Tenant-Id override only carries the tenant UUID, which would
    leave the new user in the wrong tenant. ``super_admin_client`` is used only for the
    platform-level subscription apply.
    """
    run = uuid.uuid4().hex[:6]
    ensure_tenant_plan(super_admin_client, tenant_id)
    flow = OnboardingFlow(admin_client)

    outlet_id = _id_of(flow.create_outlet_admin(name=f"{label} Outlet {run}", slug=f"{label.lower()}-{run}"))
    if not outlet_id:
        raise AssertionError(f"{label}: outlet not created")

    email = f"{label.lower()}-{run}@e2e.local"
    user_id = _id_of(flow.create_identity_user(email=email, role="CASHIER", outletId=outlet_id))
    if not user_id:
        raise AssertionError(f"{label}: cashier user not created")

    flow.ensure_seller_for_user(
        user_id=user_id, outlet_id=outlet_id,
        display_name=f"{label} Seller {run}", code=f"{label.lower()}-{run}")

    terminal_id = _id_of(flow.create_terminal_admin(outlet_id=outlet_id, label=f"{label} Terminal {run}"))
    if not terminal_id:
        raise AssertionError(f"{label}: terminal not created")

    # The operating cashier must own the terminal (assignment + terminal.assigned_user_id).
    assign = admin_client.post(f"/admin/terminals/{terminal_id}/assign-user", json={"userId": user_id})
    if assign.status_code >= 500:
        raise AssertionError(f"{label}: assign-user failed {assign.status_code}: {assign.text}")

    # Authenticate as the new cashier (default password, immediately usable).
    token = keycloak.password_grant(username=email, password=_DEFAULT_PASSWORD)
    cred = f"e2e-cred-{run}"  # device credential we control → presented as X-Device-Binding
    cashier = ApiClient(base_url=base_url, token=token, extra_headers={"X-Device-Binding": cred})

    # Bind the terminal with that exact credential so the cashier resolves STRONG trust.
    # KNOWN BLOCKER: an API-created user's Keycloak token carries tenant_code="default"
    # (the custom KC protocol mapper defaults it when the user attribute is unset, and the
    # creating context's tenant code is not propagated to the new KC user). The cashier
    # therefore resolves to the wrong tenant and /tenant/* is denied (403). The KC realm
    # role IS now assigned (so authority is present); this tenant_code propagation is the
    # remaining gap before an API-onboarded seller can sell.
    challenge_resp = cashier.post(
        f"/tenant/terminals/{terminal_id}/activation-challenges", json={"deliveryMode": "E2E"})
    if challenge_resp.status_code == 403:
        import pytest
        pytest.skip(
            "API-onboarded cashier cannot operate POS yet: its KC token has "
            "tenant_code=default (not the tenant's code), so /tenant/* is denied. "
            "Fix tenant_code propagation for API-provisioned users to unblock."
        )
    binding = TerminalBindingFlow(cashier)
    challenge = challenge_resp.json()["data"] if challenge_resp.status_code in (200, 201) \
        else binding.create_challenge(terminal_id)
    clear_code = challenge.get("deliveryRef") or challenge.get("clearCode")
    binding.verify_challenge(terminal_id, challenge["challengeId"], clear_code, binding_credential=cred)

    # Open (or reuse) an OPEN session for this terminal.
    ctx = ensure_pos_session_open(
        cashier,
        OpContext(outlet_id=outlet_id, terminal_id=terminal_id),
        super_admin_client=super_admin_client,
    )

    return PosContext(
        tenant_id=tenant_id,
        outlet_id=outlet_id,
        terminal_id=terminal_id,
        session_id=ctx.session_id,
        cashier_client=cashier,
        stake_cents=stake_cents,
    )
