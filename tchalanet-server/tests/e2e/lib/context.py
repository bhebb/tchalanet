"""Cashier operational context (outlet / terminal / session) used for X-Tch-* headers."""
from __future__ import annotations

from dataclasses import dataclass


@dataclass
class CashierContext:
    outlet_id: str | None = None
    terminal_id: str | None = None
    session_id: str | None = None

    def with_session(self, session_id: str) -> "CashierContext":
        return CashierContext(
            outlet_id=self.outlet_id,
            terminal_id=self.terminal_id,
            session_id=session_id,
        )
