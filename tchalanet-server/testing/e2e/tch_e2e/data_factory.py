"""Test data builders — TBD when business_critical suite is built."""
from __future__ import annotations

from typing import Any


def build_ticket_lines(
    game_code: str,
    bet_type: str,
    selections: list[str],
    stake_cents: int,
    bet_option: int | None = None,
) -> list[dict[str, Any]]:
    """Build raw line specs for /tickets/preview or /tickets/sell."""
    return [
        {
            "gameCode": game_code,
            "betType": bet_type,
            "selection": sel,
            "betOption": bet_option,
            "stake": f"{stake_cents / 100:.2f}",
        }
        for sel in selections
    ]


def build_user_payload(username: str, password: str, role: str, **extra: Any) -> dict[str, Any]:
    """Build a user creation payload for onboarding endpoints."""
    return {"username": username, "password": password, "role": role, **extra}


def build_outlet_payload(name: str, **extra: Any) -> dict[str, Any]:
    return {"name": name, "status": "ACTIVE", **extra}


def build_terminal_payload(outlet_id: str, label: str, **extra: Any) -> dict[str, Any]:
    return {"outletId": outlet_id, "label": label, **extra}
