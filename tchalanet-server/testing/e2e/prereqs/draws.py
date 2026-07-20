"""Ensure today's draws exist and are open (idempotent)."""
from __future__ import annotations

import datetime as dt

from tch_e2e.api_response import assert_ok
from tch_e2e.client import ApiClient
from tch_e2e.config import SeedIds


def ensure_draws_today(super_admin: ApiClient, seed_ids: SeedIds) -> dict[str, int]:
    """Generate today's draws and open the daily upcoming horizon idempotently."""
    today = dt.date.today()
    to_date = today + dt.timedelta(days=max(seed_ids.generate_days - 1, 0))

    gen_response = super_admin.post(
        "/platform/ops/draws/generate",
        json={
            "tenantId": seed_ids.tenant_id,
            "from": today.isoformat(),
            "to": to_date.isoformat(),
            "dryRun": False,
            "force": False,
            "reason": "e2e cashier happy path",
        },
    )
    assert_ok(gen_response)

    open_response = super_admin.post(
        # Opens scheduled draws in the configured daily horizon and cancels
        # provider-unavailable slots.
        "/platform/ops/draws/open-today",
        json={
            "limit": 500,
            "lookaheadHours": 24,
            "lagHours": 1,
            "dryRun": False,
        },
    )
    assert_ok(open_response)

    body = open_response.json()
    data = body.get("data") if isinstance(body, dict) else {}
    if not isinstance(data, dict):
        data = {}

    # open-today is now a per-tenant batch: data = {tenants: [{result: {...}}, ...]}.
    # Sum the per-tenant results; fall back to the legacy single-result shape.
    keys = ("opened", "skippedLocked", "skippedTooLateOrCutoffPassed", "canceledProviderClosed")
    if isinstance(data.get("tenants"), list):
        totals = {k: 0 for k in keys}
        for outcome in data["tenants"]:
            res = (outcome or {}).get("result") or {}
            for k in keys:
                totals[k] += int(res.get(k, 0) or 0)
        return totals
    return {k: int(data.get(k, 0) or 0) for k in keys}
