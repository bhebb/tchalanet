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
    seller_terminal_id: str | None = None,
) -> PosContext:
    """Build a POS context for the current SellerTerminal model."""
    from prereqs.draws import ensure_draws_today
    from tch_e2e.config import OpContext

    del tenant_admin_client
    terminal_id = seller_terminal_id or seed_ids.terminal_id
    ctx = OpContext(outlet_id=seed_ids.outlet_id, terminal_id=terminal_id)

    ensure_draws_today(super_admin_client, seed_ids)

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
        terminal_id=terminal_id,
        session_id=ctx.session_id,
        cashier_client=cashier_client,
        draw_id=draw_id,
        stake_cents=seed_ids.stake_cents,
    )
