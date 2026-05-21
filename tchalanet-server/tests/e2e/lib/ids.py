"""Seed IDs loaded from .env.local (with seed UUID fallbacks mirroring scripts/.env.local)."""
from __future__ import annotations

import os
from dataclasses import dataclass


# Seeds from migration V205__seed_outlet_terminal_pos.sql — same fallbacks as scripts/e2e-phase1.
_DEFAULT_TENANT_ID = "00000000-0000-0000-0000-000000000003"
_DEFAULT_OUTLET_ID = "00000000-0000-0000-0000-000000003001"
_DEFAULT_TERMINAL_ID = "00000000-0000-0000-0000-000000003101"


@dataclass(frozen=True)
class SeedIds:
    tenant_id: str
    tenant_code: str
    outlet_id: str
    terminal_id: str
    stake_cents: int
    generate_days: int

    @classmethod
    def from_env(cls) -> "SeedIds":
        return cls(
            tenant_id=os.environ.get("TCH_TENANT_ID", _DEFAULT_TENANT_ID),
            tenant_code=os.environ.get("TCH_TENANT_CODE", "tchalanet"),
            outlet_id=os.environ.get("TCH_OUTLET_ID", _DEFAULT_OUTLET_ID),
            terminal_id=os.environ.get("TCH_TERMINAL_ID", _DEFAULT_TERMINAL_ID),
            stake_cents=int(os.environ.get("TCH_STAKE_CENTS", "100")),
            generate_days=int(os.environ.get("TCH_GENERATE_DAYS", "7")),
        )
