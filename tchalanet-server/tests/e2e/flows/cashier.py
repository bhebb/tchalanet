"""Cashier business flow: preview, sell, print, send, get, list."""
from __future__ import annotations

import os
import uuid
from dataclasses import dataclass
from typing import Any

from lib.api import ApiClient, assert_ok
from lib.context import CashierContext


@dataclass
class SoldTicket:
    ticket_id: str
    ticket_code: str
    public_code: str
    sale_status: str
    backup: dict[str, Any]


class CashierFlow:
    """Wraps the cashier HTTP surface for a given (client, context)."""

    def __init__(self, client: ApiClient, context: CashierContext, stake_cents: int) -> None:
        self.client = client
        self.context = context
        self.stake_cents = stake_cents

    # --- operational context ----------------------------------------------

    def select_context(self, *, strict: bool = False) -> dict[str, Any] | None:
        """Pre-flight validation. Best-effort by default (non-strict): server-side issues are
        logged but don't break the happy path, because the X-Tch-* headers on subsequent calls
        are what actually carry the operational context per request.
        """
        response = self.client.post(
            "/tenant/cashier/operational-context/select",
            json={
                "outletId": self.context.outlet_id,
                "terminalId": self.context.terminal_id,
                "salesSessionId": self.context.session_id,
            },
        )
        if response.status_code in (200, 201):
            return response.json()["data"]
        if strict:
            assert_ok(response)
        print(
            f"[warn] /operational-context/select returned {response.status_code} — "
            f"continuing via X-Tch-* headers. Body: {response.text}"
        )
        return None

    # --- draws -------------------------------------------------------------

    def list_available_draws(self, lookahead_hours: int = 24, limit: int = 20) -> list[dict[str, Any]]:
        response = self.client.get(
            "/tenant/cashier/draws/available",
            params={"lookaheadHours": lookahead_hours, "limit": limit},
            context=self.context,
        )
        assert_ok(response)
        return response.json()["data"]

    # --- tickets -----------------------------------------------------------

    def preview(self, draw: dict[str, Any], game_code: str) -> dict[str, Any]:
        return self._do_preview(self._sale_payload(draw, [game_code]))

    def preview_multi_game(self, draw: dict[str, Any], game_codes: list[str]) -> dict[str, Any]:
        return self._do_preview(self._sale_payload(draw, game_codes))

    def _do_preview(self, payload: dict[str, Any]) -> dict[str, Any]:
        response = self.client.post(
            "/tenant/cashier/tickets/preview",
            json=payload,
            context=self.context,
        )
        assert_ok(response)
        return response.json()["data"]

    def sell(self, draw: dict[str, Any], game_code: str) -> SoldTicket:
        return self._do_sell(self._sale_payload(draw, [game_code]))

    def sell_multi_game(self, draw: dict[str, Any], game_codes: list[str]) -> SoldTicket:
        """Sells one ticket with one line per game — useful to exercise the per-game
        section layout in the receipt PDF/ESC_POS rendering."""
        return self._do_sell(self._sale_payload(draw, game_codes))

    def sell_lines(self, draw: dict[str, Any], lines: list[dict[str, Any]]) -> SoldTicket:
        """Sells one ticket with caller-provided raw line specs. Each line must contain
        `gameCode, betType, selection, betOption, stake` keys."""
        payload = {
            "terminalId": self.context.terminal_id,
            "drawId": draw["drawId"],
            "drawChannelId": draw["drawChannelId"],
            "currency": "HTG",
            "lines": lines,
        }
        return self._do_sell(payload)

    def _do_sell(self, payload: dict[str, Any]) -> SoldTicket:
        idem = str(uuid.uuid4())
        response = self.client.post(
            "/tenant/cashier/tickets/sell",
            json=payload,
            context=self.context,
            idempotency_key=idem,
        )
        assert_ok(response, expected=(200, 201))
        data = response.json()["data"]
        if data.get("outcome") != "ACCEPTED" or data.get("ticketId") is None:
            # 201 returned but the server rejected the sale (REJECTED / PENDING_APPROVAL etc.).
            # Surface the issues + seller instruction so the caller can debug.
            raise AssertionError(
                f"Sell did not result in ACCEPTED — outcome={data.get('outcome')}\n"
                f"  issues={data.get('issues')}\n"
                f"  sellerInstruction={data.get('sellerInstruction')}\n"
                f"  payload lines={payload.get('lines')}"
            )
        return SoldTicket(
            ticket_id=data["ticketId"],
            ticket_code=data["ticketCode"],
            public_code=data["publicCode"],
            sale_status=data["saleStatus"],
            backup=data.get("backup") or {},
        )

    def get_ticket(self, ticket_id: str) -> dict[str, Any]:
        response = self.client.get(
            f"/tenant/cashier/tickets/{ticket_id}",
            context=self.context,
        )
        assert_ok(response)
        return response.json()["data"]

    def list_tickets(self, size: int = 50) -> list[dict[str, Any]]:
        """Walks the full pagination until `hasNext=false`. Returns the flat list of items."""
        all_items: list[dict[str, Any]] = []
        page = 0
        while True:
            response = self.client.get(
                "/tenant/cashier/tickets",
                params={"page": page, "size": size, "sort": "createdAt,desc"},
                context=self.context,
            )
            assert_ok(response)
            data = response.json()["data"]
            all_items.extend(data.get("items", []))
            if not data.get("hasNext"):
                break
            page += 1
        return all_items

    def print_pdf(self, ticket_id: str) -> bytes:
        return self._print(ticket_id, "PDF")

    def print_escpos(self, ticket_id: str) -> bytes:
        return self._print(ticket_id, "ESC_POS")

    def _print(self, ticket_id: str, fmt: str) -> bytes:
        response = self.client.post(
            f"/tenant/cashier/tickets/{ticket_id}/print",
            json={
                "format": fmt,
                "recordPrint": True,
                "deliveryOptions": ["RETURN_FILE"],
            },
            context=self.context,
        )
        assert_ok(response)
        return response.content

    def send_slack(self, ticket_id: str) -> dict[str, Any]:
        # Defaults to the "delivery" channel configured in tchalanet-edge-service.
        # Override via TCH_TEST_SLACK_CHANNEL_KEY (e.g. "security-audit") if needed.
        channel_key = os.environ.get("TCH_TEST_SLACK_CHANNEL_KEY", "delivery")
        response = self.client.post(
            f"/tenant/cashier/tickets/{ticket_id}/send",
            json={
                "terminalId": self.context.terminal_id,
                "channel": "SLACK_INTERNAL",
                "channelKey": channel_key,
                "locale": "fr",
            },
            context=self.context,
        )
        assert_ok(response, expected=(200, 202))
        return response.json()["data"]

    # --- helpers -----------------------------------------------------------

    # Game → (betType, [selections...], betOption). Multiple selections per game so the
    # receipt renderer can be inspected for its group-by-game layout.
    _GAME_BET_PROFILE: dict[str, tuple[str, list[str], int | None]] = {
        "HT_BOLET":  ("MATCH_1_2D",      ["11", "22", "33"],          None),
        "HT_MARYAJ": ("MARRIAGE_2D2D",   ["21-25", "33-77"],          1),
        "HT_LOTO3":  ("LOTTO3_3D",       ["012", "345"],              1),
        "HT_LOTO4":  ("LOTTO4_PATTERN",  ["1234", "5678"],            1),
        "HT_LOTO5":  ("LOTTO5_PATTERN",  ["12345", "67890"],          1),
    }

    def _sale_payload(self, draw: dict[str, Any], game_codes: list[str]) -> dict[str, Any]:
        lines = []
        for code in game_codes:
            bet_type, selections, bet_option = self._GAME_BET_PROFILE.get(
                code,
                ("MATCH_1_2D", ["11"], None),  # safe default for unknown games
            )
            for selection in selections:
                lines.append({
                    "gameCode": code,
                    "betType": bet_type,
                    "selection": selection,
                    "betOption": bet_option,
                    "stake": f"{self.stake_cents / 100:.2f}",
                })
        return {
            "terminalId": self.context.terminal_id,
            "drawId": draw["drawId"],
            "drawChannelId": draw["drawChannelId"],
            "currency": "HTG",
            "lines": lines,
        }
