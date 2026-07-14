"""Locust virtual users driving the real POS flows through tch_e2e."""
from __future__ import annotations

import random

from locust import User, between, tag, task

from flows.cashier import CashierFlow
from loadtest.basket import random_basket
from loadtest.bootstrap import new_cashier_api, run_id
from loadtest.client import LocustApiClient


class CashierUser(User):
    """A cashier: authenticates once (on_start), then sells 5–10 line baskets and reads POS draws."""

    weight = 3
    wait_time = between(0.5, 2.0)

    def on_start(self) -> None:
        api, ctx, stake = new_cashier_api()
        self._client = LocustApiClient(
            api, self.environment.events.request, run_context={"run_id": run_id()}
        )
        opts = self.environment.parsed_options
        self._min = int(getattr(opts, "basket_min", 5) or 5)
        self._max = int(getattr(opts, "basket_max", 10) or 10)
        self._stake = stake
        self.flow = CashierFlow(self._client, ctx, stake)
        self._draws = self._safe_draws()

    def on_stop(self) -> None:
        try:
            self._client.close()
        except Exception:
            pass

    def _safe_draws(self) -> list:
        try:
            return self.flow.list_available_draws() or []
        except Exception:
            return []

    def _pick_draw(self):
        if not self._draws:
            self._draws = self._safe_draws()
        return random.choice(self._draws) if self._draws else None

    @tag("sales")
    @task(3)
    def sell_basket(self) -> None:
        draw = self._pick_draw()
        if not draw:
            return
        lines = random_basket(self._min, self._max, self._stake)
        try:
            self.flow.preview_lines(draw, lines)  # timed as POST /tenant/sales/preparations
            self.flow.sell_lines(draw, lines)     # timed as POST /tenant/sales/preparations/{id}/confirm
        except Exception:
            # Per-request outcomes are already reported by LocustApiClient; keep the user alive.
            pass

    @tag("read")
    @task(2)
    def read_pos(self) -> None:
        # Exercises the cached read path (GET /tenant/cashier/draws/available).
        try:
            self.flow.list_available_draws()
        except Exception:
            pass
