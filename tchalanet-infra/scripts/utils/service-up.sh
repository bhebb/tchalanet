#!/usr/bin/env bash
# service-up.sh - small helper to run docker-compose actions for a single service
# Usage: service-up.sh <action> <service> [env]
# action: up|down|logs|config
# service: api|keycloak|postgres|redis|meilisearch|unleash|traefik|...
# env: dev|staging|prod (default: dev)
set -euo pipefail

if [ "$#" -lt 2 ]; then
  cat >&2 <<EOF
Usage: $0 <action> <service> [env]
  action: up|down|logs|config
  service: api|keycloak|postgres|redis|meilisearch|unleash|traefik|...
  env: dev|staging|prod (default: dev)
Example:
  $0 config api dev   # print the composed config for API service in dev
  $0 up api dev       # start API + deps for dev
EOF
  exit 2
fi

ACTION="$1"
SERVICE="$2"
ENV="${3:-dev}"
ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
COMPOSE_DIR="$ROOT_DIR/compose"

# Find compose file for the service
SERVICE_FILE_CAND="$COMPOSE_DIR/docker-compose-${SERVICE}.yml"
if [ -f "$SERVICE_FILE_CAND" ]; then
  SERVICE_FILE="$SERVICE_FILE_CAND"
else
  # try plural or project variants
  if [ -f "$COMPOSE_DIR/docker-compose-${SERVICE}s.yml" ]; then
    SERVICE_FILE="$COMPOSE_DIR/docker-compose-${SERVICE}s.yml"
  else
    echo "Error: compose file not found for service '$SERVICE' (checked $SERVICE_FILE_CAND)" >&2
    exit 3
  fi
fi

# Default profile and extra files per service
PROFILES="--profile core"
EXTRA_FILES=""
case "$SERVICE" in
  api)
    PROFILES="--profile core --profile cache --profile api"
    EXTRA_FILES="$COMPOSE_DIR/docker-compose-postgres.yml $COMPOSE_DIR/docker-compose-redis.yml $COMPOSE_DIR/docker-compose-keycloak.yml"
    ;;
  keycloak)
    PROFILES="--profile core"   # postgres est dans core → OK
    EXTRA_FILES="$COMPOSE_DIR/docker-compose-postgres.yml"
    ;;
  unleash)
    # ⟵ AVANT: PROFILES="--profile flags"
    PROFILES="--profile core --profile cache --profile flags"
    EXTRA_FILES="$COMPOSE_DIR/docker-compose-postgres.yml $COMPOSE_DIR/docker-compose-redis.yml"
    ;;
  meilisearch|meili)
    PROFILES="--profile search"
    ;;
  redis)
    PROFILES="--profile cache"
    ;;
  postgres)
    PROFILES="--profile core"
    ;;
  traefik)
    PROFILES="--profile core"
    ;;
  *)
    PROFILES="--profile core"
    ;;
esac



echo "→ service-up: action=$ACTION service=$SERVICE env=$ENV profiles=$PROFILES" >&2
echo "→ files: $EXTRA_FILES + $SERVICE_FILE" >&2

WRAPPER="$ROOT_DIR/scripts/utils/run-compose-wrapper.sh"
[ -x "$WRAPPER" ] || { echo "Error: helper $WRAPPER not found" >&2; exit 4; }

/usr/bin/env -i PATH=/usr/bin:/bin ENV="$ENV" MAKE_DEBUG="${MAKE_DEBUG:-}" \
  "$WRAPPER" "$PROFILES" "$ACTION" "$SERVICE_FILE" "$SERVICE" "$EXTRA_FILES"
