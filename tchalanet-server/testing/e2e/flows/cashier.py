"""Cashier business flow: prepare sale, confirm, print, send, get, list."""
from __future__ import annotations

import uuid
from dataclasses import dataclass
from typing import Any

from tch_e2e.api_response import assert_ok, get_data
from tch_e2e.client import ApiClient
from tch_e2e.config import OpContext


@dataclass
class SoldTicket:
    ticket_id: str
    ticket_code: str
    public_code: str
    sale_status: str
    backup: dict[str, Any]


class CashierFlow:
    """Wraps the cashier HTTP surface for a given (client, context)."""

    def __init__(self, client: ApiClient, context: OpContext, stake_cents: int) -> None:
        self.client = client
        self.context = context
        self.stake_cents = stake_cents

    # --- operational context -----------------------------------------------

    def select_context(self, *, strict: bool = False) -> dict[str, Any] | None:
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
        return self.prepare(self._sale_payload(draw, [game_code]))

    def preview_multi_game(self, draw: dict[str, Any], game_codes: list[str]) -> dict[str, Any]:
        return self.prepare(self._sale_payload(draw, game_codes))

    def preview_lines(self, draw: dict[str, Any], lines: list[dict[str, Any]]) -> dict[str, Any]:
        payload = {
            "drawId": draw["drawId"],
            "drawChannelId": draw["drawChannelId"],
            "currency": "HTG",
            "lines": lines,
        }
        return self.prepare(payload)

    def prepare(self, payload: dict[str, Any]) -> dict[str, Any]:
        response = self.client.post(
            "/tenant/sales/preparations",
            json=self._prepare_payload(payload),
            context=self.context,
        )
        assert_ok(response)
        return response.json()["data"]

    def sell(self, draw: dict[str, Any], game_code: str) -> SoldTicket:
        return self._do_sell(self._sale_payload(draw, [game_code]))

    def sell_multi_game(self, draw: dict[str, Any], game_codes: list[str]) -> SoldTicket:
        return self._do_sell(self._sale_payload(draw, game_codes))

    def sell_lines(self, draw: dict[str, Any], lines: list[dict[str, Any]]) -> SoldTicket:
        payload = {
            "drawId": draw["drawId"],
            "drawChannelId": draw["drawChannelId"],
            "currency": "HTG",
            "lines": lines,
        }
        return self._do_sell(payload)

    def sell_expecting_rejection(
        self, draw: dict[str, Any], game_code: str
    ) -> dict[str, Any]:
        """Call prepare/confirm expecting a rejected outcome or a 4xx response.

        Returns the raw response data dict (does NOT raise on rejection).
        Raises only if the server returns an unexpected 5xx.
        """
        response = self._post_prepare(self._sale_payload(draw, [game_code]))
        if 200 <= response.status_code < 300:
            preparation = response.json()["data"]
            response = self._post_confirm(preparation["preparationId"], str(uuid.uuid4()))
        if response.status_code >= 500:
            raise AssertionError(
                f"Unexpected 5xx from sell: {response.status_code} — {response.text}"
            )
        try:
            return response.json().get("data") or response.json()
        except Exception:
            return {"_raw_status": response.status_code, "_raw_body": response.text}

    def _do_sell(self, payload: dict[str, Any]) -> SoldTicket:
        idem = str(uuid.uuid4())
        preparation = self.prepare(payload)
        response = self._post_confirm(preparation["preparationId"], idem)
        assert_ok(response, expected=(200, 201))
        data = self._flatten_confirm_response(response.json()["data"])
        if data.get("outcome") != "ACCEPTED" or data.get("ticketId") is None:
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

    def _post_prepare(self, payload: dict[str, Any]):
        return self.client.post(
            "/tenant/sales/preparations",
            json=self._prepare_payload(payload),
            context=self.context,
        )

    def _post_confirm(self, preparation_id: str, idempotency_key: str):
        return self.client.post(
            f"/tenant/sales/preparations/{preparation_id}/confirm",
            context=self.context,
            idempotency_key=idempotency_key,
        )

    def confirm_prepared_sale(self, preparation_id: str, idempotency_key: str) -> dict[str, Any]:
        response = self._post_confirm(preparation_id, idempotency_key)
        assert_ok(response, expected=(200, 201))
        return self._flatten_confirm_response(response.json()["data"])

    def _prepare_payload(self, payload: dict[str, Any]) -> dict[str, Any]:
        lines: list[dict[str, Any]] = []
        for idx, line in enumerate(payload.get("lines") or [], start=1):
            stake = line.get("stakeAmount", line.get("stake"))
            prepared = {
                "lineNumber": line.get("lineNumber", idx),
                "gameCode": line["gameCode"],
                "betType": line["betType"],
                "selection": line["selection"],
                "stakeAmount": stake,
            }
            if line.get("betOption") is not None:
                prepared["betOption"] = line.get("betOption")
            lines.append(prepared)
        return {
            "drawId": payload["drawId"],
            "drawChannelId": payload["drawChannelId"],
            "currency": {"value": payload.get("currency", "HTG")},
            "lines": lines,
        }

    def _flatten_confirm_response(self, data: dict[str, Any]) -> dict[str, Any]:
        sale = data.get("sale") or data
        ticket = sale.get("ticket") or {}
        flattened = {
            "preparationId": data.get("preparationId"),
            "alreadyConfirmed": data.get("alreadyConfirmed"),
            "outcome": sale.get("outcome"),
            "ticketId": data.get("ticketId") or ticket.get("ticketId") or sale.get("ticketId"),
            "ticketCode": ticket.get("ticketCode") or sale.get("ticketCode"),
            "publicCode": ticket.get("publicCode") or sale.get("publicCode"),
            "saleStatus": ticket.get("saleStatus") or sale.get("saleStatus"),
            "issues": sale.get("issues"),
            "backup": sale.get("backup") or {},
            "actionAvailability": sale.get("actionAvailability"),
            "sellerInstruction": sale.get("sellerInstruction"),
        }
        return flattened

    def get_ticket(self, ticket_id: str) -> dict[str, Any]:
        response = self.client.get(
            f"/tenant/cashier/tickets/{ticket_id}",
            context=self.context,
        )
        assert_ok(response)
        return response.json()["data"]

    def list_tickets(self, size: int = 50) -> list[dict[str, Any]]:
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
        # PrintTicketRequest carries the format inside `printOptionsRequest`
        # (the backend field name; outputFormat = PDF | ESC_POS | PNG | HTML_PREVIEW,
        # paperSize = RECEIPT_58MM | RECEIPT_80MM | A4). POS receipts use 80mm.
        response = self.client.post(
            f"/tenant/cashier/tickets/{ticket_id}/print",
            json={
                "terminalId": self.context.terminal_id,
                "printOptionsRequest": {
                    "outputFormat": fmt,
                    "paperSize": "RECEIPT_80MM",
                },
                "recordPrint": True,
                "deliveryOptions": ["RETURN_FILE"],
            },
            context=self.context,
        )
        assert_ok(response)
        return response.content

    def send_slack(self, ticket_id: str, *, channel_key: str = "delivery") -> dict[str, Any]:
        import os
        channel_key = os.environ.get("TCH_TEST_SLACK_CHANNEL_KEY", channel_key)
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

    # --- session -----------------------------------------------------------

    def close_session(
        self,
        session_id: str,
        *,
        closing_amount: str = "100.00",
        reason: str = "e2e:happy-path-close",
    ) -> dict[str, Any]:
        """Close the POS session (design §9 step 10)."""
        response = self.client.post(
            "/tenant/cashier/session/close",
            json={
                "sessionId": session_id,
                "closingAmount": closing_amount,
                "reason": reason,
            },
            context=self.context,
        )
        assert_ok(response)
        return response.json().get("data") or {}

    # --- helpers -----------------------------------------------------------

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
                code, ("MATCH_1_2D", ["11"], None)
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
            "drawId": draw["drawId"],
            "drawChannelId": draw["drawChannelId"],
            "currency": "HTG",
            "lines": lines,
        }
