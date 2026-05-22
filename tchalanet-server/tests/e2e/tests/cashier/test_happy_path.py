"""Cashier morning happy path.

Steps (in order):
    1. Login as cashier (handled by fixture).
    2. Ensure the cashier's app_user row is synced (idempotent ping).
    3. Ensure today's draws exist and are OPEN (super_admin, idempotent).
    4. Select the operational context (outlet + terminal + session).
    5. Ensure a POS session is open (idempotent — opens one if needed).
    6. List available draws.
    7. For each (draw × configured game): preview → sell → print → send_slack → get_by_id.
    8. List tickets and assert count matches what we just sold.

Artifacts (PDFs) are written to `TCH_ARTIFACT_DIR` (default `.tmp/e2e-cashier-pdfs`
relative to the e2e folder).
"""
from __future__ import annotations

import os
from pathlib import Path

import pytest

from flows.cashier import CashierFlow
from lib.api import ApiClient
from lib.context import CashierContext
from lib.ids import SeedIds
from prereqs.app_user import ensure_app_user_synced
from prereqs.draws import ensure_draws_today
from prereqs.session import ensure_pos_session_open


def _artifact_dir() -> Path:
    """Default to `tchalanet-app/target/pdf/cashier-happy-path`. `target/` is gitignored."""
    raw = os.environ.get("TCH_ARTIFACT_DIR")
    if raw:
        base = Path(raw)
    else:
        e2e_root = Path(__file__).resolve().parents[2]
        base = e2e_root.parent.parent / "tchalanet-app" / "target" / "pdf" / "cashier-happy-path"
    base.mkdir(parents=True, exist_ok=True)
    return base


@pytest.mark.happy_path
def test_cashier_morning_happy_path(
    super_admin_client: ApiClient,
    cashier_client: ApiClient,
    cashier_context: CashierContext,
    seed_ids: SeedIds,
) -> None:
    # --- prereqs (idempotent) ---------------------------------------------
    sync_summary = ensure_app_user_synced(super_admin_client)
    print(f"keycloak→app_user sync: {sync_summary}")
    open_summary = ensure_draws_today(super_admin_client, seed_ids)
    print(f"open-today summary: {open_summary}")

    cashier_context = ensure_pos_session_open(cashier_client, cashier_context)
    assert cashier_context.session_id is not None

    flow = CashierFlow(cashier_client, cashier_context, stake_cents=seed_ids.stake_cents)
    flow.select_context()

    artifacts = _artifact_dir()
    print(f"PDF artifacts will be written to {artifacts}")

    # --- draws -------------------------------------------------------------
    draws = flow.list_available_draws()
    assert draws, "no draws returned to cashier"
    print(f"draws returned: {[(d.get('channelCode'), d.get('status')) for d in draws]}")

    open_draws = [d for d in draws if d.get("status") == "OPEN"]
    assert open_draws, (
        f"no OPEN draws among the {len(draws)} returned — open-today did nothing. "
        "Check tenant draw schedules / cutoffs."
    )

    # --- vente pour chaque (draw × game configuré) -------------------------
    sold: list[str] = []
    for draw in open_draws:
        game_codes = draw.get("gameCodes") or []
        assert game_codes, f"draw {draw['drawId']} exposes no gameCodes"

        for game_code in game_codes:
            # preview
            preview = flow.preview(draw, game_code)
            assert preview["decision"] == "ACCEPTABLE", (
                f"preview rejected for {draw['channelCode']}/{game_code}: {preview}"
            )

            # sell
            ticket = flow.sell(draw, game_code)
            assert ticket.sale_status in {"PLACED", "ACCEPTED", "APPROVED"}, (
                f"unexpected sale_status: {ticket.sale_status}"
            )
            assert ticket.backup, "accepted sale must expose a populated backup block"
            assert ticket.backup.get("displayCode")
            assert ticket.backup.get("verificationShortUrl")
            assert ticket.backup.get("shareableText")
            sold.append(ticket.ticket_id)

            # print PDF (binaire) — persist to disk for visual inspection
            pdf_bytes = flow.print_pdf(ticket.ticket_id)
            assert pdf_bytes.startswith(b"%PDF"), "print did not return a PDF binary"
            (artifacts / f"{draw['channelCode']}_{game_code}_{ticket.ticket_code}.pdf").write_bytes(pdf_bytes)

            # send Slack (text-only)
            send_result = flow.send_slack(ticket.ticket_id)
            assert send_result.get("queued") is True

            # get by id
            detail = flow.get_ticket(ticket.ticket_id)
            assert detail["ticketCode"] == ticket.ticket_code

    # --- list tickets ------------------------------------------------------
    all_tickets = flow.list_tickets()
    listed_ids = {row["id"] for row in all_tickets}
    missing = set(sold) - listed_ids
    assert not missing, (
        f"tickets just sold are missing from list "
        f"(listed {len(listed_ids)} total, missing {len(missing)}): {missing}"
    )
