"""Reusable archive ops scenario for pytest and Locust.

The scenario keeps business/ops flow outside Locust so the load harness only chooses concurrency
and timing. Locust should call these methods; it should not duplicate endpoint sequencing.
"""
from __future__ import annotations

import datetime as dt
from dataclasses import dataclass
from typing import Any, Literal, Protocol

from tch_e2e.api_response import assert_ok


class ArchiveOpsClient(Protocol):
    def get(self, path: str, **kwargs: Any): ...

    def post(self, path: str, **kwargs: Any): ...


@dataclass(frozen=True)
class ArchiveOpsScenarioConfig:
    period_start: dt.date
    period_end: dt.date
    trigger_enabled: bool = False
    delete_enabled: bool = False
    partition_retention_cutoff: dt.date | None = None
    reason: str = "Locust archive ops smoke run"

    def __post_init__(self) -> None:
        if self.period_start >= self.period_end:
            raise ValueError("archive period_start must be before period_end")

    @classmethod
    def default(cls) -> "ArchiveOpsScenarioConfig":
        today = dt.date.today()
        first_of_month = today.replace(day=1)
        previous_month = (first_of_month - dt.timedelta(days=1)).replace(day=1)
        return cls(
            period_start=previous_month,
            period_end=first_of_month,
            trigger_enabled=False,
            delete_enabled=False,
            partition_retention_cutoff=first_of_month,
        )


@dataclass(frozen=True)
class ArchiveOpsReadResult:
    runs: int
    failed_runs: int
    invalid_objects: int
    active_legal_holds: int
    partition_plans: int
    summary: dict[str, Any]


def api_data(response) -> Any:
    body = response.json()
    data = body.get("data") if isinstance(body, dict) else body
    if isinstance(data, dict) and set(data.keys()) == {"value"}:
        return data["value"]
    return data


def count_items(value: Any) -> int:
    if isinstance(value, list):
        return len(value)
    if isinstance(value, dict):
        items = value.get("items") or value.get("content")
        if isinstance(items, list):
            return len(items)
    return 0


class ArchiveOpsScenario:
    """Archive ops HTTP scenario, reusable by Locust and non-Locust harnesses."""

    def __init__(self, client: ArchiveOpsClient, config: ArchiveOpsScenarioConfig | None = None):
        self.client = client
        self.config = config or ArchiveOpsScenarioConfig.default()
        self._triggered = False
        self._lifecycle_ran = False

    def read_ops_surface(self) -> ArchiveOpsReadResult:
        summary = self._get("/platform/archive/ops-summary")
        runs = self._get("/platform/archive/runs", params={"limit": 20})
        failed = self._get("/platform/archive/runs/failed", params={"limit": 20})
        invalid = self._get("/platform/archive/objects/invalid", params={"limit": 20})
        holds = self._get("/platform/archive/legal-holds/active", params={"limit": 20})
        plans = self._get(
            "/platform/archive/partition-cleanup/plan",
            params={
                "tableName": "audit_log",
                "retentionCutoff": self._retention_cutoff().isoformat(),
            },
        )
        return ArchiveOpsReadResult(
            runs=count_items(runs),
            failed_runs=count_items(failed),
            invalid_objects=count_items(invalid),
            active_legal_holds=count_items(holds),
            partition_plans=count_items(plans),
            summary=summary if isinstance(summary, dict) else {},
        )

    def trigger_archive_run_once(self) -> dict[str, Any] | None:
        if not self.config.trigger_enabled or self._triggered:
            return None
        self._triggered = True
        data = self._post(
            "/platform/archive/runs",
            json={
                "strategy": "MONTHLY",
                "periodStart": self.config.period_start.isoformat(),
                "periodEnd": self.config.period_end.isoformat(),
                "reason": self.config.reason,
            },
        )
        return data if isinstance(data, dict) else {}

    def ticket_purge_dry_run(self) -> dict[str, Any]:
        return self.ticket_purge("DRY_RUN")

    def ticket_purge(self, mode: Literal["DRY_RUN", "DELETE"]) -> dict[str, Any]:
        data = self._post(
            "/platform/archive/ticket-purge",
            json={
                "periodStart": self.config.period_start.isoformat(),
                "periodEnd": self.config.period_end.isoformat(),
                "batchSize": 100,
                "mode": mode,
                "reason": self.config.reason,
            },
        )
        return data if isinstance(data, dict) else {}

    def domain_purge_dry_run(self, dataset: str) -> dict[str, Any]:
        return self.domain_purge(dataset, "DRY_RUN")

    def domain_purge(self, dataset: str, mode: Literal["DRY_RUN", "DELETE"]) -> dict[str, Any]:
        data = self._post(
            "/platform/archive/domain-purge",
            json={
                "dataset": dataset,
                "periodStart": self.config.period_start.isoformat(),
                "periodEnd": self.config.period_end.isoformat(),
                "batchSize": 100,
                "mode": mode,
                "reason": self.config.reason,
            },
        )
        return data if isinstance(data, dict) else {}

    def run_archive_lifecycle_once(self) -> dict[str, Any]:
        if self._lifecycle_ran:
            return {}
        if not self.config.trigger_enabled:
            raise RuntimeError("archive lifecycle requires trigger_enabled")
        self._lifecycle_ran = True

        result: dict[str, Any] = {"read_before": self.read_ops_surface()}
        result["archive_run"] = self.trigger_archive_run_once()
        _assert_completed_run(result["archive_run"])
        result["read_after_archive"] = self.read_ops_surface()
        result["ticket_purge_dry_run"] = self.ticket_purge_dry_run()
        result["draw_purge_dry_run"] = self.domain_purge_dry_run("DRAW")
        result["draw_result_purge_dry_run"] = self.domain_purge_dry_run("DRAW_RESULT")
        result["entity_revision_purge_dry_run"] = self.domain_purge_dry_run("ENTITY_REVISION")
        _assert_eligible_ticket_purge(result["ticket_purge_dry_run"], "DRY_RUN")
        _assert_domain_dry_run(result["draw_purge_dry_run"])
        _assert_domain_dry_run(result["draw_result_purge_dry_run"])
        _assert_domain_dry_run(result["entity_revision_purge_dry_run"])

        if self.config.delete_enabled:
            result["ticket_purge_delete"] = self.ticket_purge("DELETE")
            _assert_eligible_ticket_purge(result["ticket_purge_delete"], "DELETE")
            result["draw_purge_dry_run_after_ticket_delete"] = self.domain_purge_dry_run("DRAW")
            _assert_eligible_domain_purge(
                result["draw_purge_dry_run_after_ticket_delete"], "DRY_RUN"
            )
            result["draw_purge_delete"] = self.domain_purge("DRAW", "DELETE")
            _assert_eligible_domain_purge(result["draw_purge_delete"], "DELETE")

            result["draw_result_purge_dry_run_after_draw_delete"] = self.domain_purge_dry_run(
                "DRAW_RESULT"
            )
            _assert_eligible_domain_purge(
                result["draw_result_purge_dry_run_after_draw_delete"], "DRY_RUN"
            )
            result["draw_result_purge_delete"] = self.domain_purge("DRAW_RESULT", "DELETE")
            _assert_eligible_domain_purge(result["draw_result_purge_delete"], "DELETE")

            result["entity_revision_purge_dry_run_after_domain_delete"] = self.domain_purge_dry_run(
                "ENTITY_REVISION"
            )
            _assert_eligible_domain_purge(
                result["entity_revision_purge_dry_run_after_domain_delete"], "DRY_RUN"
            )
            result["entity_revision_purge_delete"] = self.domain_purge("ENTITY_REVISION", "DELETE")
            _assert_eligible_domain_purge(result["entity_revision_purge_delete"], "DELETE")
            result["read_after_delete"] = self.read_ops_surface()
        return result

    def _get(self, path: str, **kwargs: Any) -> Any:
        response = self.client.get(path, **kwargs)
        assert_ok(response)
        return api_data(response)

    def _post(self, path: str, **kwargs: Any) -> Any:
        response = self.client.post(path, **kwargs)
        assert_ok(response)
        return api_data(response)

    def _retention_cutoff(self) -> dt.date:
        return self.config.partition_retention_cutoff or self.config.period_end


def _assert_completed_run(result: dict[str, Any] | None) -> None:
    if not isinstance(result, dict) or result.get("status") != "COMPLETED":
        raise AssertionError(f"archive run did not complete: {result!r}")


def _assert_eligible_ticket_purge(result: dict[str, Any], mode: str) -> None:
    _assert_eligible_purge(result, mode, "ticket")


def _assert_eligible_domain_purge(result: dict[str, Any], mode: str) -> None:
    _assert_eligible_purge(result, mode, "domain")


def _assert_domain_dry_run(result: dict[str, Any]) -> None:
    if result.get("mode") != "DRY_RUN":
        raise AssertionError(f"domain purge did not return a dry-run: {result!r}")
    plan = result.get("plan")
    if not isinstance(plan, dict):
        raise AssertionError(f"domain dry-run did not return a plan: {result!r}")


def _assert_eligible_purge(result: dict[str, Any], mode: str, kind: str) -> None:
    plan = result.get("plan") if isinstance(result, dict) else None
    if result.get("mode") != mode or not isinstance(plan, dict) or plan.get("eligible") is not True:
        raise AssertionError(f"{kind} purge is not eligible in {mode}: {result!r}")
