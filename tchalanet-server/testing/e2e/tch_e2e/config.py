"""Environment configuration, seed IDs, and tenant profiles."""
from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path

from dotenv import load_dotenv


_DEFAULT_TENANT_ID = "00000000-0000-0000-0000-000000000003"
_DEFAULT_OUTLET_ID = "00000000-0000-0000-0000-000000003001"
_DEFAULT_TERMINAL_ID = "00000000-0000-0000-0000-000000003101"
# Seeded cashier user id (V202 keycloak_sub for 'cashier' user).
_DEFAULT_CASHIER_USER_ID = "00000000-0000-0000-0000-000000010003"


def load_env() -> None:
    """Load .env.local from tchalanet-server/scripts/ (canonical) or testing/e2e/.env.local."""
    here = Path(__file__).resolve()
    candidates = [
        here.parents[3] / "scripts" / ".env.local",  # tchalanet-server/scripts/.env.local
        here.parents[1] / ".env.local",               # testing/e2e/.env.local (override)
    ]
    for candidate in candidates:
        if candidate.exists():
            load_dotenv(candidate, override=False)
            return
    raise RuntimeError(
        f".env.local not found in any of: {[str(c) for c in candidates]}"
    )


@dataclass(frozen=True)
class OpContext:
    """Operational context carried via X-Tch-* headers."""
    outlet_id: str | None = None
    terminal_id: str | None = None
    session_id: str | None = None

    def with_session(self, session_id: str) -> "OpContext":
        return OpContext(
            outlet_id=self.outlet_id,
            terminal_id=self.terminal_id,
            session_id=session_id,
        )


@dataclass(frozen=True)
class SeedIds:
    """Seed IDs loaded from env (fallbacks mirror migration V205)."""
    tenant_id: str
    tenant_code: str
    outlet_id: str
    terminal_id: str
    stake_cents: int
    generate_days: int
    # User ID of the cashier (app_user.id) — used for seller onboarding prereq.
    cashier_user_id: str | None = None

    @classmethod
    def from_env(cls) -> "SeedIds":
        return cls(
            tenant_id=os.environ.get("TCH_TENANT_ID", _DEFAULT_TENANT_ID),
            tenant_code=os.environ.get("TCH_TENANT_CODE", "tchalanet"),
            outlet_id=os.environ.get("TCH_OUTLET_ID", _DEFAULT_OUTLET_ID),
            terminal_id=os.environ.get("TCH_TERMINAL_ID", _DEFAULT_TERMINAL_ID),
            stake_cents=int(os.environ.get("TCH_STAKE_CENTS", "100")),
            generate_days=int(os.environ.get("TCH_GENERATE_DAYS", "7")),
            cashier_user_id=os.environ.get("TCH_CASHIER_USER_ID", _DEFAULT_CASHIER_USER_ID),
        )


@dataclass(frozen=True)
class TenantProfile:
    """One tenant's worth of test credentials — used for multi-tenant flows."""
    index: int
    tenant_id: str
    tenant_code: str
    outlet_id: str
    terminal_id: str
    seller_username: str
    seller_password: str
    stake_cents: int
    generate_days: int

    def to_seed_ids(self) -> SeedIds:
        return SeedIds(
            tenant_id=self.tenant_id,
            tenant_code=self.tenant_code,
            outlet_id=self.outlet_id,
            terminal_id=self.terminal_id,
            stake_cents=self.stake_cents,
            generate_days=self.generate_days,
        )

    @classmethod
    def from_env(cls, index: int) -> "TenantProfile | None":
        def pick(field: str, legacy: str | None, default: str | None) -> str | None:
            value = os.environ.get(f"TCH_TENANT_{index}_{field}")
            if value:
                return value
            if legacy is not None:
                value = os.environ.get(legacy)
                if value:
                    return value
            return default

        legacy_ok = index == 1
        tenant_code = pick("CODE", "TCH_TENANT_CODE" if legacy_ok else None, None)
        if not tenant_code:
            return None

        seller_username = pick("SELLER_USERNAME", "TCH_SELLER_USERNAME" if legacy_ok else None, None)
        seller_password = pick("SELLER_PASSWORD", "TCH_SELLER_PASSWORD" if legacy_ok else None, None)
        if not seller_username or not seller_password:
            raise RuntimeError(
                f"Tenant profile {index} ({tenant_code}) missing seller credentials."
            )
        return cls(
            index=index,
            tenant_id=pick("ID", "TCH_TENANT_ID" if legacy_ok else None, _DEFAULT_TENANT_ID),
            tenant_code=tenant_code,
            outlet_id=pick("OUTLET_ID", "TCH_OUTLET_ID" if legacy_ok else None, _DEFAULT_OUTLET_ID),
            terminal_id=pick("TERMINAL_ID", "TCH_TERMINAL_ID" if legacy_ok else None, _DEFAULT_TERMINAL_ID),
            seller_username=seller_username,
            seller_password=seller_password,
            stake_cents=int(pick("STAKE_CENTS", "TCH_STAKE_CENTS" if legacy_ok else None, "100")),
            generate_days=int(pick("GENERATE_DAYS", "TCH_GENERATE_DAYS" if legacy_ok else None, "7")),
        )
