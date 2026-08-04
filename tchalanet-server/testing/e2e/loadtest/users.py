"""Locust virtual users driving the real seller-terminal POS flows through tch_e2e."""
from __future__ import annotations

import datetime as dt
import random
import time

from locust import User, between, tag, task

from flows.cashier import CashierFlow
from loadtest.archive_seed import ArchiveSeedConfig, seed_archive_fixture
from loadtest.basket import random_basket
from loadtest.bootstrap import new_platform_admin_api, new_seller_terminal_api, run_id
from loadtest.client import LocustApiClient
from tch_e2e.archive_ops import ArchiveOpsScenario, ArchiveOpsScenarioConfig


class CashierUser(User):
    """A seller-terminal: authenticates once, then sells 5–10 line baskets and reads POS draws."""

    weight = 3
    wait_time = between(0.5, 2.0)

    def on_start(self) -> None:
        api, ctx, stake = new_seller_terminal_api()
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
            self.flow.sell_lines(draw, lines)     # prepare + POST /tenant/sales/preparations/{id}/confirm
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


class ArchiveOpsUser(User):
    """A SUPER_ADMIN ops user exercising archive management endpoints via the shared scenario."""

    weight = 1
    wait_time = between(2.0, 6.0)

    def on_start(self) -> None:
        api = new_platform_admin_api()
        self._client = LocustApiClient(
            api, self.environment.events.request, run_context={"run_id": run_id()}
        )
        opts = self.environment.parsed_options
        period_start, period_end, retention_cutoff = _archive_dates(opts)
        self.scenario = ArchiveOpsScenario(
            self._client,
            ArchiveOpsScenarioConfig(
                period_start=period_start,
                period_end=period_end,
                trigger_enabled=bool(getattr(opts, "archive_trigger", False)),
                delete_enabled=bool(getattr(opts, "archive_delete", False)),
                partition_retention_cutoff=retention_cutoff,
                reason=str(
                    getattr(opts, "archive_reason", None) or "Locust archive ops smoke run"
                ),
            ),
        )
        self._archive_seed = ArchiveSeedConfig(
            enabled=bool(getattr(opts, "archive_seed", False)),
            command=str(getattr(opts, "archive_seed_command", None) or ""),
        )

    def on_stop(self) -> None:
        self._client.close()

    @tag("archive", "read")
    @task(5)
    def read_archive_ops(self) -> None:
        self.scenario.read_ops_surface()

    @tag("archive", "purge")
    @task(1)
    def dry_run_archive_purges(self) -> None:
        self.scenario.ticket_purge_dry_run()
        self.scenario.domain_purge_dry_run("DRAW")
        self.scenario.domain_purge_dry_run("DRAW_RESULT")
        self.scenario.domain_purge_dry_run("ENTITY_REVISION")

    @tag("archive", "trigger")
    @task(1)
    def trigger_archive_once(self) -> None:
        self.scenario.trigger_archive_run_once()

    @tag("archive", "lifecycle")
    @task(1)
    def archive_lifecycle_once(self) -> None:
        started = time.perf_counter()
        try:
            seed_archive_fixture(self._archive_seed)
            self.scenario.run_archive_lifecycle_once()
        except Exception as exc:
            self.environment.events.request.fire(
                request_type="SCENARIO",
                name="archive-lifecycle",
                response_time=(time.perf_counter() - started) * 1000.0,
                response_length=0,
                response=None,
                context={"run_id": run_id()},
                exception=exc,
            )
        finally:
            self.environment.runner.stop()


def _archive_dates(opts) -> tuple[dt.date, dt.date, dt.date]:
    today = dt.date.today()
    first_of_month = today.replace(day=1)
    period_end = _date_arg(getattr(opts, "archive_period_end", None), first_of_month)
    period_start = _date_arg(
        getattr(opts, "archive_period_start", None),
        (period_end - dt.timedelta(days=1)).replace(day=1),
    )
    retention_cutoff = _date_arg(
        getattr(opts, "archive_retention_cutoff", None),
        period_end,
    )
    return period_start, period_end, retention_cutoff


def _date_arg(value: str | None, default: dt.date) -> dt.date:
    if value:
        return dt.date.fromisoformat(value)
    return default
