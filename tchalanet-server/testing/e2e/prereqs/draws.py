"""Ensure today's draws exist and are open (idempotent)."""
from __future__ import annotations

import datetime as dt

from tch_e2e.api_response import assert_ok
from tch_e2e.client import ApiClient
from tch_e2e.config import SeedIds


def ensure_draws_today(super_admin: ApiClient, seed_ids: SeedIds) -> dict[str, int]:
    """Generate today's draws and open the ones that are sellable right now.

    ``/open-today`` (unlike the removed ``/open-due``) respects each channel's
    ``sales_open_time``. The seed channels open sales at 05:30 local (≈09:30–11:30
    UTC), so a run before that window would open nothing. We therefore pass an
    explicit ``now`` = ``max(real now, today 11:30 UTC)``: past every sales-open,
    but the opened draws keep their real (future) cutoff, so they stay sellable at
    the actual current time.
    """
    today = dt.date.today()
    to_date = today + dt.timedelta(days=max(seed_ids.generate_days - 1, 0))

    now_utc = dt.datetime.now(dt.timezone.utc)
    sales_floor = dt.datetime.combine(today, dt.time(11, 30), tzinfo=dt.timezone.utc)
    open_now = max(now_utc, sales_floor).isoformat().replace("+00:00", "Z")

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
        # Legacy /open-due was replaced by /open-today (OpenTodayDrawsCommand):
        # opens today's SCHEDULED draws and cancels provider-unavailable slots.
        "/platform/ops/draws/open-today",
        json={
            "now": open_now,
            "drawDate": today.isoformat(),
            "limit": 500,
            "dryRun": False,
        },
    )
    assert_ok(open_response)

    body = open_response.json()
    data = body.get("data") if isinstance(body, dict) else {}
    if not isinstance(data, dict):
        data = {}
    return {
        "opened": int(data.get("opened", 0)),
        "skippedLocked": int(data.get("skippedLocked", 0)),
        "skippedTooLateOrCutoffPassed": int(data.get("skippedTooLateOrCutoffPassed", 0)),
        "canceledProviderClosed": int(data.get("canceledProviderClosed", 0)),
    }
