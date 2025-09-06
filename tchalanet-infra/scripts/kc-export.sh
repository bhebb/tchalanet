#!/usr/bin/env bash
set -euo pipefail

ENV_NAME="${1:-dev}"
BASE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="$BASE_DIR/envs/$ENV_NAME/.env"
OUT_DIR="$BASE_DIR/keycloak/exports"
OUT_FILE="$OUT_DIR/tchalanet-$ENV_NAME-export.json"

mkdir -p "$OUT_DIR"

# Nom de conteneur Keycloak (adapter si différent)
KC_CTN="keycloak"

# Export sans utilisateurs (recommandé pour promuvers envs)
docker compose --env-file "$ENV_FILE" exec -T "$KC_CTN" \
  /opt/keycloak/bin/kc.sh export \
  --realm tchalanet \
  --users skip \
  --file /tmp/tchalanet-export.json

docker compose --env-file "$ENV_FILE" cp "$KC_CTN":/tmp/tchalanet-export.json "$OUT_FILE"

echo "Exported realm → $OUT_FILE"
