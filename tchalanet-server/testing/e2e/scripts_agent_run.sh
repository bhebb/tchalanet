#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

MODE="${1:-agent}"

usage() {
  cat <<'USAGE'
Usage:
  bash scripts_agent_run.sh [agent|smoke|business-day|bet-options|availability-gates|full-business-day]

Modes:
  agent              Default. Runs the current canonical agent check:
                     L0 + reduced business-day + BetOptions.
  smoke              Runs L0 only.
  business-day       Runs the reduced canonical business-day scenario.
  bet-options        Runs the BetOptions companion check.
  availability-gates Runs the isolated availability-gates check. Requires
                     TCH_E2E_ALLOW_CATALOG_MUTATION=true.
  full-business-day  Runs business-day with all configured tenants/draws.

Local defaults:
  TCH_E2E_AUTH_PROVIDER=firebase-emulator
  TCH_FIREBASE_PROJECT_ID=demo-tchalanet-local
  TCH_BASE_URL=https://127.0.0.1/api/v1
  TCH_E2E_HOST_HEADER=api.localtest.me
  TCH_E2E_VERIFY_SSL=false

Canonical scenario truth:
  docs/business-day-scenarios.md
USAGE
}

if [[ "$MODE" == "-h" || "$MODE" == "--help" ]]; then
  usage
  exit 0
fi

PYTHON_BIN="${PYTHON_BIN:-$ROOT_DIR/.venv/bin/python}"
if [[ ! -x "$PYTHON_BIN" ]]; then
  echo "Missing E2E Python runtime: $PYTHON_BIN" >&2
  echo "Create it from $ROOT_DIR with: python3 -m venv .venv && .venv/bin/pip install -e ." >&2
  exit 2
fi

export TCH_E2E_AUTH_PROVIDER="${TCH_E2E_AUTH_PROVIDER:-firebase-emulator}"
export TCH_FIREBASE_PROJECT_ID="${TCH_FIREBASE_PROJECT_ID:-demo-tchalanet-local}"
export TCH_BASE_URL="${TCH_BASE_URL:-https://127.0.0.1/api/v1}"
export TCH_E2E_HOST_HEADER="${TCH_E2E_HOST_HEADER:-api.localtest.me}"
export TCH_E2E_VERIFY_SSL="${TCH_E2E_VERIFY_SSL:-false}"

run_pytest() {
  echo
  echo "==> $*"
  "$PYTHON_BIN" -m pytest "$@"
}

run_smoke() {
  run_pytest -m L0 -q
}

run_business_day_reduced() {
  TCH_E2E_BUSINESS_DAY_TENANTS="${TCH_E2E_BUSINESS_DAY_TENANTS:-alpha,delta}" \
  TCH_E2E_BUSINESS_DAY_DRAW_COUNT="${TCH_E2E_BUSINESS_DAY_DRAW_COUNT:-1}" \
  TCH_E2E_RESULT_APPLY_MODE="${TCH_E2E_RESULT_APPLY_MODE:-force}" \
  TCH_E2E_TICKET_PRINT_MODE="${TCH_E2E_TICKET_PRINT_MODE:-none}" \
  TCH_E2E_TICKET_SLACK_MODE="${TCH_E2E_TICKET_SLACK_MODE:-none}" \
  run_pytest -q \
    tests/full_flow/test_business_day_scenarios.py::test_business_day_happy_path_supports_reports_results_and_future_locust
}

run_business_day_full() {
  TCH_E2E_RESULT_APPLY_MODE="${TCH_E2E_RESULT_APPLY_MODE:-force}" \
  TCH_E2E_TICKET_PRINT_MODE="${TCH_E2E_TICKET_PRINT_MODE:-none}" \
  TCH_E2E_TICKET_SLACK_MODE="${TCH_E2E_TICKET_SLACK_MODE:-none}" \
  run_pytest -q \
    tests/full_flow/test_business_day_scenarios.py::test_business_day_happy_path_supports_reports_results_and_future_locust
}

run_bet_options() {
  run_pytest -q \
    tests/full_flow/test_game_bet_options_scenarios.py::test_tenant_game_bet_options_are_configured_sold_snapshotted_and_reprinted
}

run_availability_gates() {
  if [[ "${TCH_E2E_ALLOW_CATALOG_MUTATION:-}" != "true" ]]; then
    echo "Refusing to run availability gates without TCH_E2E_ALLOW_CATALOG_MUTATION=true." >&2
    echo "This test mutates shared catalog/runtime switches and must run isolated." >&2
    exit 3
  fi
  TCH_E2E_BUSINESS_DAY_DRAW_COUNT="${TCH_E2E_BUSINESS_DAY_DRAW_COUNT:-1}" \
  run_pytest -q \
    tests/full_flow/test_business_day_scenarios.py::test_sale_availability_gates_block_unavailable_runtime_state
}

case "$MODE" in
  agent)
    run_smoke
    run_business_day_reduced
    run_bet_options
    ;;
  smoke)
    run_smoke
    ;;
  business-day)
    run_business_day_reduced
    ;;
  bet-options)
    run_bet_options
    ;;
  availability-gates)
    run_availability_gates
    ;;
  full-business-day)
    run_business_day_full
    ;;
  *)
    usage >&2
    exit 2
    ;;
esac
