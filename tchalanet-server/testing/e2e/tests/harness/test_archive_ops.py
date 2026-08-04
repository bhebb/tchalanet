import datetime as dt

import pytest

from tch_e2e.archive_ops import ArchiveOpsScenario, ArchiveOpsScenarioConfig


class _Response:
    status_code = 200

    def __init__(self, data):
        self._data = data

    def json(self):
        return {"data": self._data}


class _ArchiveClient:
    def __init__(self):
        self.posts: list[tuple[str, dict]] = []

    def get(self, path, **kwargs):
        del kwargs
        if path == "/platform/archive/ops-summary":
            return _Response({"completedRuns": 1})
        return _Response([])

    def post(self, path, **kwargs):
        self.posts.append((path, kwargs["json"]))
        if path == "/platform/archive/runs":
            return _Response({"id": "run-1", "status": "COMPLETED"})
        if path == "/platform/archive/ticket-purge":
            return _Response({"mode": kwargs["json"]["mode"], "plan": {"eligible": True}})
        return _Response({"mode": kwargs["json"]["mode"], "plan": {"eligible": True}})


def test_archive_lifecycle_calls_the_real_endpoint_sequence_once():
    client = _ArchiveClient()
    scenario = ArchiveOpsScenario(
        client,
        ArchiveOpsScenarioConfig(
            period_start=dt.date(2025, 1, 1),
            period_end=dt.date(2025, 2, 1),
            trigger_enabled=True,
            reason="archive lifecycle test",
        ),
    )

    scenario.run_archive_lifecycle_once()
    scenario.run_archive_lifecycle_once()

    assert [path for path, _ in client.posts] == [
        "/platform/archive/runs",
        "/platform/archive/ticket-purge",
        "/platform/archive/domain-purge",
        "/platform/archive/domain-purge",
        "/platform/archive/domain-purge",
    ]
    assert all(body["mode"] == "DRY_RUN" for _, body in client.posts[1:])


def test_archive_lifecycle_requires_an_archive_trigger():
    scenario = ArchiveOpsScenario(
        _ArchiveClient(),
        ArchiveOpsScenarioConfig(
            period_start=dt.date(2025, 1, 1),
            period_end=dt.date(2025, 2, 1),
            reason="archive lifecycle test",
        ),
    )

    with pytest.raises(RuntimeError, match="trigger_enabled"):
        scenario.run_archive_lifecycle_once()
