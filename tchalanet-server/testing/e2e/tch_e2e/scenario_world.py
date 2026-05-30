"""ScenarioWorld — per-run test world with up to three tenant contexts."""
from __future__ import annotations

import uuid
import warnings
from dataclasses import dataclass
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from tch_e2e.auth import KeycloakAuth
    from tch_e2e.client import ApiClient
    from tch_e2e.config import SeedIds, OpContext


@dataclass
class TenantCtx:
    """All state needed to act as a single tenant in E2E tests."""
    tenant_id: str
    tenant_code: str
    admin_token: str | None
    cashier_token: str | None
    outlet_id: str | None
    terminal_id: str | None
    session_id: str | None = None

    def cashier_context(self) -> "OpContext":
        from tch_e2e.config import OpContext
        return OpContext(
            outlet_id=self.outlet_id,
            terminal_id=self.terminal_id,
            session_id=self.session_id,
        )

    def with_session(self, session_id: str) -> "TenantCtx":
        return TenantCtx(
            tenant_id=self.tenant_id,
            tenant_code=self.tenant_code,
            admin_token=self.admin_token,
            cashier_token=self.cashier_token,
            outlet_id=self.outlet_id,
            terminal_id=self.terminal_id,
            session_id=session_id,
        )


@dataclass
class ScenarioWorld:
    """Stateful test world created once per pytest session.

    Tenant A — normal, seeded by migration V205.
    Tenant B — different config; stubbed if TCH_TENANT_2_* not set.
    Tenant C — minimal/incomplete; admin token only (for readiness/onboarding tests).
    """
    run_id: str
    tenant_a: TenantCtx
    tenant_b: TenantCtx
    tenant_c: TenantCtx

    @classmethod
    def build(
        cls,
        keycloak: "KeycloakAuth",
        base_url: str,
        seed_ids: "SeedIds",
        *,
        super_admin_token: str,
    ) -> "ScenarioWorld":
        import os

        run_id = str(uuid.uuid4())[:8]

        # --- Tenant A (seed) ------------------------------------------------
        seller_username = os.environ.get("TCH_SELLER_USERNAME")
        seller_password = os.environ.get("TCH_SELLER_PASSWORD")
        cashier_token_a: str | None = None
        if seller_username and seller_password:
            try:
                cashier_token_a = keycloak.password_grant(
                    username=seller_username, password=seller_password
                )
            except Exception as exc:
                warnings.warn(f"[ScenarioWorld] Could not get cashier token for tenant A: {exc}")

        tenant_a = TenantCtx(
            tenant_id=seed_ids.tenant_id,
            tenant_code=seed_ids.tenant_code,
            admin_token=super_admin_token,
            cashier_token=cashier_token_a,
            outlet_id=seed_ids.outlet_id,
            terminal_id=seed_ids.terminal_id,
        )

        # --- Tenant B (optional env) ----------------------------------------
        b_code = os.environ.get("TCH_TENANT_2_CODE")
        if b_code:
            b_username = os.environ["TCH_TENANT_2_SELLER_USERNAME"]
            b_password = os.environ["TCH_TENANT_2_SELLER_PASSWORD"]
            b_token: str | None = None
            try:
                b_token = keycloak.password_grant(username=b_username, password=b_password)
            except Exception as exc:
                warnings.warn(f"[ScenarioWorld] Could not get cashier token for tenant B: {exc}")
            tenant_b = TenantCtx(
                tenant_id=os.environ.get("TCH_TENANT_2_ID", ""),
                tenant_code=b_code,
                admin_token=super_admin_token,
                cashier_token=b_token,
                outlet_id=os.environ.get("TCH_TENANT_2_OUTLET_ID"),
                terminal_id=os.environ.get("TCH_TENANT_2_TERMINAL_ID"),
            )
        else:
            warnings.warn(
                "[ScenarioWorld] Tenant B not configured (TCH_TENANT_2_CODE missing). "
                "Cross-tenant tests that require distinct tenants will be skipped."
            )
            tenant_b = TenantCtx(
                tenant_id="",
                tenant_code="",
                admin_token=super_admin_token,
                cashier_token=None,
                outlet_id=None,
                terminal_id=None,
            )

        # --- Tenant C (incomplete — no outlet/terminal) ----------------------
        tenant_c = TenantCtx(
            tenant_id="",
            tenant_code="",
            admin_token=super_admin_token,
            cashier_token=None,
            outlet_id=None,
            terminal_id=None,
        )

        return cls(run_id=run_id, tenant_a=tenant_a, tenant_b=tenant_b, tenant_c=tenant_c)
