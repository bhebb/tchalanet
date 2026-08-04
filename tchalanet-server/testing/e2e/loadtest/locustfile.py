"""Locust entrypoint for Tchalanet load/perf tests.

Run from ``testing/e2e``:

    locust -f loadtest/locustfile.py --class-picker --host "$TCH_BASE_URL"

The Web UI (default http://localhost:8089) is the operation page: pick the scenario (class-picker),
edit the inputs below, launch, and watch live RPS / p50-p95-p99 / failures.
"""
from __future__ import annotations

import os
import sys

# Make testing/e2e importable (tch_e2e, flows, loadtest) regardless of the cwd Locust runs from.
_E2E_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if _E2E_ROOT not in sys.path:
    sys.path.insert(0, _E2E_ROOT)

from locust import events  # noqa: E402

from loadtest.users import ArchiveOpsUser, CashierUser  # noqa: E402,F401  (registers User classes)


@events.init_command_line_parser.add_listener
def _tch_load_args(parser) -> None:
    """Domain inputs, editable as form fields in the Locust Web UI start screen."""
    parser.add_argument(
        "--basket-min", type=int, default=5, env_var="TCH_BASKET_MIN",
        include_in_web_ui=True, help="Min ticket lines per sale (5–10 target).",
    )
    parser.add_argument(
        "--basket-max", type=int, default=10, env_var="TCH_BASKET_MAX",
        include_in_web_ui=True, help="Max ticket lines per sale (5–10 target).",
    )
    parser.add_argument(
        "--archive-period-start", default="", env_var="TCH_ARCHIVE_PERIOD_START",
        include_in_web_ui=True, help="Archive ops period start, YYYY-MM-DD.",
    )
    parser.add_argument(
        "--archive-period-end", default="", env_var="TCH_ARCHIVE_PERIOD_END",
        include_in_web_ui=True, help="Archive ops period end, YYYY-MM-DD.",
    )
    parser.add_argument(
        "--archive-retention-cutoff", default="", env_var="TCH_ARCHIVE_RETENTION_CUTOFF",
        include_in_web_ui=True, help="Partition cleanup retention cutoff, YYYY-MM-DD.",
    )
    parser.add_argument(
        "--archive-trigger", action="store_true", default=False, env_var="TCH_ARCHIVE_TRIGGER",
        include_in_web_ui=True, help="Opt in to one POST /platform/archive/runs per user.",
    )
    parser.add_argument(
        "--archive-delete", action="store_true", default=False, env_var="TCH_ARCHIVE_DELETE",
        include_in_web_ui=True, help="Opt in to DELETE-mode archive purges after dry-run checks.",
    )
    parser.add_argument(
        "--archive-seed", action="store_true", default=False, env_var="TCH_ARCHIVE_SEED",
        include_in_web_ui=True, help="Run archive_seed.sql before archive lifecycle calls.",
    )
    parser.add_argument(
        "--archive-seed-command", default="", env_var="TCH_ARCHIVE_SEED_COMMAND",
        include_in_web_ui=True, help="Command that receives archive_seed.sql on standard input.",
    )
    parser.add_argument(
        "--archive-reason", default="Locust archive ops smoke run", env_var="TCH_ARCHIVE_REASON",
        include_in_web_ui=True, help="Reason sent to archive write/dry-run endpoints.",
    )
