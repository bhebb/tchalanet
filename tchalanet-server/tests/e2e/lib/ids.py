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


@dataclass(frozen=True)
class TenantProfile:
    """One tenant's worth of test config — used for multi-tenant concurrent flows."""

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
        """Build a profile from `TCH_TENANT_<index>_*` envs, falling back to legacy
        `TCH_<FIELD>` envs (used for index=1). Returns None if neither a numbered nor a
        legacy tenant code is present.
        """
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

        seller_username = pick(
            "SELLER_USERNAME",
            "TCH_SELLER_USERNAME" if legacy_ok else None,
            None,
        )
        seller_password = pick(
            "SELLER_PASSWORD",
            "TCH_SELLER_PASSWORD" if legacy_ok else None,
            None,
        )
        if not seller_username or not seller_password:
            raise RuntimeError(
                f"Tenant profile {index} ({tenant_code}) is missing seller credentials "
                f"(TCH_TENANT_{index}_SELLER_USERNAME / TCH_TENANT_{index}_SELLER_PASSWORD)."
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

    @classmethod
    def discover(cls) -> list["TenantProfile"]:
        """Walk TCH_TENANT_1_*, TCH_TENANT_2_*, ... and assemble all declared profiles.

        Stops on the first gap. At most 16 profiles to avoid pathological env scans.
        """
        profiles: list[TenantProfile] = []
        for index in range(1, 17):
            profile = cls.from_env(index)
            if profile is None:
                break
            profiles.append(profile)
        return profiles
