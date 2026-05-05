#!/usr/bin/env bash
# Smoke test staging : vérifie Traefik, Keycloak, API, Edge.
# Usage: ENV=staging ./smoke-staging.sh
set -euo pipefail

BASE="${STAGING_BASE:-stg.tchalanet.com}"
FAIL=0

check() {
  local label="$1" url="$2"
  if curl -sf --max-time 10 "$url" > /dev/null; then
    echo "✅ $label"
  else
    echo "❌ $label — $url"
    FAIL=1
  fi
}

echo "→ Smoke staging ($BASE)"
check "Keycloak OIDC"  "https://auth.$BASE/realms/tchalanet/.well-known/openid-configuration"
check "API health"     "https://api.$BASE/actuator/health"
check "Edge health"    "https://edge.$BASE/health"
check "Web app"        "https://app.$BASE"

[ "$FAIL" -eq 0 ] && echo "✅ Smoke staging OK" || { echo "❌ Smoke staging FAILED"; exit 1; }
