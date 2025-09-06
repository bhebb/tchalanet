#!/usr/bin/env bash
set -euo pipefail
ENV_NAME="${1:-dev}"
BASE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="$BASE_DIR/envs/$ENV_NAME/.env"
REALM_FILE="$BASE_DIR/keycloak/realms/tchalanet-$ENV_NAME.json"

KC_CTN="keycloak"
. "$ENV_FILE"

# login admin (realm master)
docker compose --env-file "$ENV_FILE" exec -T "$KC_CTN" /opt/keycloak/bin/kcadm.sh \
  config credentials --server "http://${KC_HOST}" --realm master \
  --user "${KC_ADMIN:-admin}" --password "${KC_ADMIN_PASS:-admin}"

# importer via Admin REST (ex: re-créer clients depuis JSON) – à détailler si tu veux du “merge”.
echo "Pour un merge fin, il faut parser $REALM_FILE et appliquer les changements via kcadm (clients/roles/mappers)."
