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

from loadtest.users import CashierUser  # noqa: E402,F401  (registers the User class)


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
