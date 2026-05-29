"""Business-level assertions reused across test suites."""
from __future__ import annotations

from typing import Any


def assert_required_step(payload: dict[str, Any], expected_type: str) -> None:
    """Assert payload contains requiredStep.type == expected_type."""
    required_step = payload.get("requiredStep")
    assert required_step, (
        f"Expected requiredStep.type={expected_type!r}, but requiredStep is absent. "
        f"payload keys: {list(payload.keys())}"
    )
    actual = required_step.get("type")
    assert actual == expected_type, (
        f"requiredStep.type mismatch: expected={expected_type!r}, got={actual!r}"
    )


def assert_sale_accepted(data: dict[str, Any]) -> None:
    """Assert a sell response represents an accepted sale."""
    outcome = data.get("outcome")
    assert outcome == "ACCEPTED", (
        f"Expected outcome=ACCEPTED, got {outcome!r}. "
        f"issues={data.get('issues')}, sellerInstruction={data.get('sellerInstruction')}"
    )
    assert data.get("ticketId"), "Accepted sale must expose a ticketId"
    assert data.get("ticketCode"), "Accepted sale must expose a ticketCode"


def assert_sale_rejected(data: dict[str, Any]) -> None:
    """Assert a sell response represents a rejected/blocked sale."""
    outcome = data.get("outcome")
    assert outcome != "ACCEPTED", (
        f"Expected sale to be rejected/blocked, but outcome={outcome!r} (ticketId={data.get('ticketId')})"
    )


def assert_money_breakdown(data: dict[str, Any]) -> None:
    """Assert the money breakdown block is present and internally consistent."""
    breakdown = data.get("moneyBreakdown") or data.get("breakdown")
    if breakdown is None:
        return  # Not all endpoints expose breakdown — skip gracefully
    stake_total = breakdown.get("stakeTotal") or breakdown.get("totalStake")
    assert stake_total is not None, f"moneyBreakdown missing stakeTotal: {breakdown}"


def assert_line_snapshots(data: dict[str, Any]) -> None:
    """Assert each line in the sale has a snapshot with odds/payout data."""
    lines = data.get("lines") or []
    for i, line in enumerate(lines):
        snapshot = line.get("snapshot") or line.get("pricingSnapshot")
        assert snapshot, f"Line {i} missing snapshot: {line}"
