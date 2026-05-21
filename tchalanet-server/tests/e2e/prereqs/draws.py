"""Ensure today's draws exist and are open (idempotent)."""
from __future__ import annotations

import datetime as dt

from lib.api import ApiClient, assert_ok
from lib.ids import SeedIds


def ensure_draws_today(super_admin: ApiClient, seed_ids: SeedIds) -> dict[str, int]:
    """Generate today's draws and force-open everything due in a wide horizon.

    Why `/open-due` instead of `/open-today` :
      * `/open-today` n'ouvre que les tirages dont le `defaultSalesOpenTime` est déjà passé
        (utile pour vérifier ce qui devrait ouvrir aujourd'hui selon le schedule).
      * `/open-due` opère sur une fenêtre `openHorizonHours` / `openLagHours` et force
        l'ouverture indépendamment de l'heure de vente — c'est ce qu'on veut pour rendre
        les tirages du jour sellables peu importe l'heure du test.
    """
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
        "/platform/ops/draws/open-due",
        json={
            "openHorizonHours": 24,
            "openLagHours": 24,
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
    }
