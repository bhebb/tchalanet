"""PosContext — fully onboarded POS state and its builder.

A valid POS sell requires:
  tenant (active) → outlet (active) → terminal (active, bound)
  → cashier user (role + permissions) → OPEN sales session
  → open draw + compatible pricing odds + limits

``build_pos_context`` assembles all of this from the seed env, using
idempotent onboarding calls where available and falling back gracefully
when endpoints are still WIP (returns 404/500 on mobile_init branch).
"""
from __future__ import annotations

import dataclasses
from dataclasses import dataclass
from typing import TYPE_CHECKING, Any

if TYPE_CHECKING:
    from flows.cashier import CashierFlow
    from tch_e2e.client import ApiClient
    from tch_e2e.config import OpContext, SeedIds

_BOGUS_UUID = "00000000-0000-0000-0000-000000000000"


@dataclass
class PosContext:
    """All state needed to execute cashier POS operations.

    Designed to be mutated for negative tests via the ``with_*`` / ``without_*``
    helpers — each returns a new instance without modifying the original.
    """

    tenant_id: str
    outlet_id: str
    terminal_id: str
    session_id: str | None          # None = no X-Tch-Sales-Session-Id header
    cashier_client: "ApiClient"
    draw_id: str | None = None
    stake_cents: int = 100

    # --- context bridging ---------------------------------------------------

    def op_context(self) -> "OpContext":
        from tch_e2e.config import OpContext
        return OpContext(
            outlet_id=self.outlet_id,
            terminal_id=self.terminal_id,
            session_id=self.session_id,
        )

    def cashier_flow(self) -> "CashierFlow":
        from flows.cashier import CashierFlow
        return CashierFlow(self.cashier_client, self.op_context(), self.stake_cents)

    # --- negative-test mutation helpers -------------------------------------

    def without_session(self) -> "PosContext":
        """Outlet + terminal headers only — no X-Tch-Sales-Session-Id."""
        return dataclasses.replace(self, session_id=None)

    def without_context(self) -> "PosContext":
        """No X-Tch-* headers at all (anonymous POS call)."""
        return dataclasses.replace(self, outlet_id=None, terminal_id=None, session_id=None)

    def with_bogus_session(self) -> "PosContext":
        """Valid terminal/outlet, but session ID that does not exist."""
        return dataclasses.replace(self, session_id=_BOGUS_UUID)

    def with_bogus_terminal(self) -> "PosContext":
        """Valid outlet/session, but terminal ID that does not exist."""
        return dataclasses.replace(self, terminal_id=_BOGUS_UUID)

    def with_terminal(self, terminal_id: str) -> "PosContext":
        return dataclasses.replace(self, terminal_id=terminal_id)

    def with_outlet(self, outlet_id: str) -> "PosContext":
        return dataclasses.replace(self, outlet_id=outlet_id)

    def with_session(self, session_id: str) -> "PosContext":
        return dataclasses.replace(self, session_id=session_id)

    # --- convenience --------------------------------------------------------

    def first_open_draw(self, lookahead_hours: int = 24) -> dict[str, Any] | None:
        """Return the first OPEN draw visible to this cashier, or None."""
        try:
            draws = self.cashier_flow().list_available_draws(
                lookahead_hours=lookahead_hours
            )
            return next((d for d in draws if d.get("status") == "OPEN"), None)
        except Exception:
            return None


def build_pos_context(
    super_admin_client: "ApiClient",
    cashier_client: "ApiClient",
    seed_ids: "SeedIds",
    *,
    tenant_admin_client: "ApiClient | None" = None,
) -> PosContext:
    """Build a fully onboarded POS context ready for cashier sell operations.

    Steps
    -----
    1. Idempotent terminal bind via ``OnboardingFlow`` (tolerates 409 already-bound
       and pytest.skip from WIP endpoints).
    2. Open or reuse the current OPEN session via ``ensure_pos_session_open``
       (calls pytest.skip if same-day close constraint blocks a new session).
    3. Ensure today's draws are generated and open.
    4. Probe the first available OPEN draw (non-critical — stored as a convenience
       shortcut; tests that sell must assert it themselves).
    """
    import _pytest.outcomes
    from flows.onboarding import OnboardingFlow
    from prereqs.draws import ensure_draws_today
    from prereqs.session import ensure_pos_session_open
    from tch_e2e.config import OpContext

    # Use tenant admin client when available (preferred for /admin/* endpoints).
    # Fall back to super_admin_client with X-Tenant-Id scoped to seed tenant.
    admin_client = tenant_admin_client or super_admin_client.with_tenant(seed_ids.tenant_id)
    onboarding = OnboardingFlow(admin_client)

    # Step 1a — ensure the cashier user has a seller profile assigned to the outlet.
    # This is required for the sell endpoint (SELLER_NO_SELLER_FOR_USER / SELLER_NOT_ASSIGNED_TO_OUTLET).
    if seed_ids.cashier_user_id:
        try:
            onboarding.ensure_seller_for_user(
                user_id=seed_ids.cashier_user_id,
                outlet_id=seed_ids.outlet_id,
            )
        except _pytest.outcomes.OutcomeException:
            pass  # WIP or 409 — seeded seller assumed already present

    # Step 1b — assign the terminal to the cashier user.
    # The seed assigns the terminal to the tenant admin, but the cashier is the one
    # operating it. ValidateTerminalForOperationQueryHandler requires BOTH
    # terminal.assigned_user_id == cashier AND an ACTIVE terminal_assignment row for
    # the cashier; the assign-user command maintains both. Idempotent: 200 on (re)assign,
    # 409 if already assigned — both fine. This keeps onboarding self-sufficient and
    # reproducible instead of depending on the seed's terminal→user assignment.
    if seed_ids.cashier_user_id:
        assign = admin_client.post(
            f"/admin/terminals/{seed_ids.terminal_id}/assign-user",
            json={"userId": seed_ids.cashier_user_id},
        )
        # 404/405 = endpoint not routed (older backend); 422 = business rule — tolerate,
        # the seed assignment is then assumed correct. Only a 5xx is a real surprise.
        if assign.status_code >= 500:
            raise AssertionError(
                f"assign-user returned {assign.status_code}: {assign.text}"
            )

    # Step 1c — idempotent terminal bind
    # 409 = already bound; pytest.skip = endpoint WIP; both are fine
    try:
        OnboardingFlow(super_admin_client).bind_terminal(
            tenant_id=seed_ids.tenant_id,
            terminal_id=seed_ids.terminal_id,
        )
    except _pytest.outcomes.OutcomeException:
        pass  # WIP endpoint — seed terminal assumed already bound

    # Step 2 — open or reuse OPEN session
    # If a CLOSED session blocks opening a new one, auto-finalize via admin client.
    ctx = ensure_pos_session_open(
        cashier_client,
        OpContext(outlet_id=seed_ids.outlet_id, terminal_id=seed_ids.terminal_id),
        super_admin_client=super_admin_client,
    )

    # Step 3 — ensure draws exist and are open
    ensure_draws_today(super_admin_client, seed_ids)

    # Step 4 — find the first OPEN draw (best-effort)
    from flows.cashier import CashierFlow
    draw_id: str | None = None
    try:
        draws = CashierFlow(cashier_client, ctx, seed_ids.stake_cents).list_available_draws()
        open_draw = next((d for d in draws if d.get("status") == "OPEN"), None)
        if open_draw:
            draw_id = open_draw.get("drawId")
    except Exception:
        pass

    return PosContext(
        tenant_id=seed_ids.tenant_id,
        outlet_id=seed_ids.outlet_id,
        terminal_id=seed_ids.terminal_id,
        session_id=ctx.session_id,
        cashier_client=cashier_client,
        draw_id=draw_id,
        stake_cents=seed_ids.stake_cents,
    )
