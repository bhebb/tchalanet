"""Fast iteration test: sells ONE ticket and exercises print PDF + ESC/POS + Slack send.

Use this to peaufiner the receipt layout and validate the Slack delivery pipeline without
re-running the full draws × games matrix from `test_happy_path.py`.

Run with:
    uv run pytest tests/cashier/test_single_ticket.py -v -s
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
    """Default to `tchalanet-app/target/pdf/single` — `target/` is already gitignored by Maven
    and the path makes sense when running on the server side. Override via TCH_ARTIFACT_DIR.
    """
    raw = os.environ.get("TCH_ARTIFACT_DIR")
    if raw:
        base = Path(raw)
    else:
        # tests/e2e/tests/cashier/test_single_ticket.py → tchalanet-server → tchalanet-app/target/pdf
        e2e_root = Path(__file__).resolve().parents[2]
        base = e2e_root.parent.parent / "tchalanet-app" / "target" / "pdf" / "single"
    base.mkdir(parents=True, exist_ok=True)
    return base


@pytest.mark.happy_path
def test_single_ticket_print_and_send(
    super_admin_client: ApiClient,
    cashier_client: ApiClient,
    cashier_context: CashierContext,
    seed_ids: SeedIds,
) -> None:
    # --- bootstrap (idempotent) -------------------------------------------
    ensure_app_user_synced(super_admin_client)
    ensure_draws_today(super_admin_client, seed_ids)
    cashier_context = ensure_pos_session_open(cashier_client, cashier_context)

    flow = CashierFlow(cashier_client, cashier_context, stake_cents=seed_ids.stake_cents)
    flow.select_context()

    # --- pick the first OPEN draw with all its configured games ----------
    draws = flow.list_available_draws()
    open_draws = [d for d in draws if d.get("status") == "OPEN" and d.get("gameCodes")]
    assert open_draws, "no OPEN draws with configured gameCodes returned"
    draw = open_draws[0]
    game_codes = list(draw["gameCodes"])
    print(f"\n>>> draw={draw['channelCode']} ({draw['drawId']}) games={game_codes}")

    # --- preview (multi-game) --------------------------------------------
    preview = flow.preview_multi_game(draw, game_codes)
    assert preview["decision"] == "ACCEPTABLE", f"preview not acceptable: {preview}"

    # --- sell (one ticket with one line per game) ------------------------
    ticket = flow.sell_multi_game(draw, game_codes)
    print(f">>> sold ticketCode={ticket.ticket_code} publicCode={ticket.public_code}")
    print(f">>> backup.shareableText:\n{ticket.backup.get('shareableText')}\n")
    assert ticket.backup.get("displayCode")

    # --- print PDF + ESC_POS ----------------------------------------------
    artifacts = _artifact_dir()
    print(f">>> artifacts: {artifacts}")

    name_stub = f"{draw['channelCode']}_multi_{ticket.ticket_code}"

    pdf_bytes = flow.print_pdf(ticket.ticket_id)
    assert pdf_bytes.startswith(b"%PDF"), "print PDF did not return a PDF binary"
    pdf_path = artifacts / f"{name_stub}.pdf"
    pdf_path.write_bytes(pdf_bytes)
    print(f">>> PDF: {pdf_path}  ({len(pdf_bytes)} bytes)")

    escpos_bytes = flow.print_escpos(ticket.ticket_id)
    escpos_path = artifacts / f"{name_stub}.escpos"
    escpos_path.write_bytes(escpos_bytes)
    print(f">>> ESC_POS: {escpos_path}  ({len(escpos_bytes)} bytes)")
    # Also dump the readable portion to stdout for quick visual review of layout.
    try:
        readable = escpos_bytes.decode("cp437", errors="replace")
    except Exception:
        readable = escpos_bytes.decode("latin-1", errors="replace")
    print(f">>> ESC_POS readable preview:\n{readable}\n")

    # --- send Slack -------------------------------------------------------
    channel_key = os.environ.get("TCH_TEST_SLACK_CHANNEL_KEY", "delivery")
    print(f">>> sending Slack via channelKey={channel_key}")
    send_result = flow.send_slack(ticket.ticket_id)
    print(f">>> Slack send response: {send_result}")
    assert send_result.get("queued") is True
    print(
        ">>> NOTE: 'queued: true' only confirms the message hit the outbound queue.\n"
        ">>>       If nothing arrives in Slack: check tchalanet-edge-service logs\n"
        ">>>       and the platform.communication queue for delivery errors."
    )
