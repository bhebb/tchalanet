#!/usr/bin/env bash
set -euo pipefail
# wait-unleash.sh [base_url] [timeout_seconds] [admin_token_optional]
# Ex: ./wait-unleash.sh http://unleash:4242 120 "user:xxxx"

BASE="${1:-http://unleash:4242}"
TIMEOUT="${2:-120}"
ADMIN_TOKEN="${3:-}"

need(){ command -v "$1" >/dev/null 2>&1 || { echo "Missing: $1" >&2; exit 2; }; }
need curl

end=$(( $(date +%s) + TIMEOUT ))
echo "→ Waiting Unleash at ${BASE} (timeout=${TIMEOUT}s)..."

# 1) /health (public)
while (( $(date +%s) < end )); do
  if curl -fsS "${BASE}/health" >/dev/null 2>&1; then
    echo "  - /health OK"
    break
  fi
  sleep 2
done

# 2) /api/admin/health (si token fourni)
exit 0
