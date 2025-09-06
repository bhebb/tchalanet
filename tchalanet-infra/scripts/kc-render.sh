#!/usr/bin/env bash
set -euo pipefail
ENV_NAME="${1:-dev}"
BASE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="$BASE_DIR/envs/$ENV_NAME/.env"
TEMPLATE="$BASE_DIR/keycloak/realm-template.json"
OUT_DIR="$BASE_DIR/keycloak/realms"
OUT_FILE="$OUT_DIR/tchalanet-$ENV_NAME.json"

[ -f "$ENV_FILE" ] || { echo "Missing $ENV_FILE"; exit 1; }
[ -f "$TEMPLATE" ] || { echo "Missing $TEMPLATE"; exit 1; }

set -a; . "$ENV_FILE"; set +a
mkdir -p "$OUT_DIR"

# remplacements simples
export APP_WEB_ORIGIN="http://${APP_WEB_HOST}"
export APP_API_HOST="${APP_WEB_HOST}" # adapte si API host diff

envsubst < "$TEMPLATE" | jq '.' > "$OUT_FILE"
echo "Rendered: $OUT_FILE"
