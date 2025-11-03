#!/usr/bin/env bash
set -euo pipefail

# wait-keycloak.sh [realm] [base_url] [timeout_seconds]
# Ex:
#   ./wait-keycloak.sh
#   ./wait-keycloak.sh tchalanet
#   ./wait-keycloak.sh tchalanet https://auth.localtest.me 600

REALM="${1:-${KC_REALM:-tchalanet}}"
KC_BASE_URL="${KC_KC_BASE_URL_EXTERNAL_URL:-https://auth.localtest.me}"
TIMEOUT="${3:-300}"

KC_NAME="${KEYCLOAK_CONTAINER:-tchl-keycloak}-${ENV:-dev}"

need() { command -v "$1" >/dev/null 2>&1 || { echo "Missing: $1" >&2; exit 2; }; }
need docker
need curl

echo "→ Waiting for Keycloak container '${KC_NAME}' to be healthy (timeout=${TIMEOUT}s)..."

end=$(( $(date +%s) + TIMEOUT ))

while :; do
  status="$(docker inspect -f '{{.State.Health.Status}}' "$KC_NAME" 2>/dev/null || echo 'unknown')"
  echo "  - health = ${status}"

  if [ "$status" = "healthy" ]; then
    echo "✓ Keycloak container is healthy"
    break
  fi

  if [ "$(date +%s)" -ge "$end" ]; then
    echo "✖ Timeout waiting for Keycloak health (last status=${status})" >&2
    exit 1
  fi

  sleep 3
done

echo "→ Checking realm '${REALM}' at ${KC_BASE_URL}/realms/${REALM}/.well-known/openid-configuration ..."

realm_end=$(( $(date +%s) + TIMEOUT ))

# Options curl : on ignore le cert si c'est du https (cas mkcert/Traefik local)
CURL_OPTS="-fsS"
case "$KC_BASE_URL" in
  https://*)
    CURL_OPTS="-fsSk"
    ;;
esac

while [ "$(date +%s)" -lt "$realm_end" ]; do
  if curl ${CURL_OPTS} "${KC_BASE_URL}/realms/${REALM}/.well-known/openid-configuration" >/dev/null 2>&1; then
    echo "✓ Keycloak realm '${REALM}' is ready"
    exit 0
  fi
  sleep 3
done

echo "❌ Keycloak realm '${REALM}' not ready; check logs or URL" >&2
exit 1
