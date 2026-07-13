#!/usr/bin/env bash
# Deploy only the API service on staging from the synced infra directory.
#
# Expected to run on the staging host from /opt/tchalanet-infra.
# Required env:
#   DOPPLER_TOKEN - staging Doppler service token, unless SKIP_DOPPLER=1 and .secrets already exists
# Optional env:
#   ENV=staging
#   IMAGE_TAG=latest
#   API_BASE_URL=https://api.stg.tchalanet.com/api/v1
#   WEB_ORIGINS="https://tchalanet-web-stg.pages.dev https://e25cee16.tchalanet-web-stg.pages.dev"
#   FORCE_RECREATE=1
#   RESET_DATABASE=1
#   RESET_DATABASE_CONFIRM="destroy staging database"
set -euo pipefail

ENV="${ENV:-staging}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
FORCE_RECREATE="${FORCE_RECREATE:-1}"
RESET_DATABASE="${RESET_DATABASE:-0}"
RESET_DATABASE_CONFIRM="${RESET_DATABASE_CONFIRM:-}"
API_BASE_URL="${API_BASE_URL:-https://api.stg.tchalanet.com/api/v1}"
WEB_ORIGINS="${WEB_ORIGINS:-https://tchalanet-web-stg.pages.dev}"
DOPPLER_IMAGE="${DOPPLER_IMAGE:-dopplerhq/cli:3.75.1}"
POSTGRES_IMAGE="${POSTGRES_IMAGE:-postgres:18.4}"

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

log() {
  printf -- '-> %s\n' "$*"
}

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

require_file() {
  [ -f "$1" ] || fail "Missing required file: $1"
}

require_file "envs/common/compose.env"
require_file "envs/$ENV/compose.env"
require_file "envs/$ENV/api.env"
require_file "compose/docker-compose-project.yml"
require_file "compose/docker-compose-redis.yml"
require_file "compose/docker-compose-api.yml"

log "Preparing Docker networks for $ENV"
docker network create "edge-$ENV" >/dev/null 2>&1 || true
docker network create "back-$ENV" >/dev/null 2>&1 || true

log "Merging runtime env files"
scripts/utils/merge-env.sh "$ENV"

if [ "${SKIP_DOPPLER:-0}" != "1" ]; then
  [ -n "${DOPPLER_TOKEN:-}" ] || fail "DOPPLER_TOKEN is required unless SKIP_DOPPLER=1"
  log "Downloading staging secrets from Doppler"
  docker run --rm \
    --entrypoint sh \
    -e DOPPLER_TOKEN="$DOPPLER_TOKEN" \
    -v "$PWD":/work -w /work \
    "$DOPPLER_IMAGE" \
    -lc "doppler secrets download --format env --project tchalanet --config stg > envs/$ENV/.secrets"
  chmod 600 "envs/$ENV/.secrets"
elif [ ! -f "envs/$ENV/.secrets" ]; then
  fail "SKIP_DOPPLER=1 was set but envs/$ENV/.secrets does not exist"
fi

compose_env="$(mktemp /tmp/tchalanet-compose-env.XXXXXX)"
cleanup() {
  rm -f "$compose_env"
}
trap cleanup EXIT

cat "envs/common/compose.env" "envs/$ENV/compose.env" "envs/$ENV/.secrets" > "$compose_env"

compose_cmd=(
  docker compose
  --project-name "tch-$ENV"
  --env-file "$compose_env"
  -f compose/docker-compose-project.yml
  -f compose/docker-compose-redis.yml
  -f compose/docker-compose-api.yml
)

if [ "$RESET_DATABASE" = "1" ]; then
  [ "$ENV" = "staging" ] || fail "RESET_DATABASE is only allowed for ENV=staging"
  [ "$RESET_DATABASE_CONFIRM" = "destroy staging database" ] || \
    fail "RESET_DATABASE_CONFIRM must be exactly: destroy staging database"

  log "Stopping API before staging database reset"
  "${compose_cmd[@]}" stop api >/dev/null 2>&1 || true

  log "Resetting staging database schema (DROP SCHEMA public CASCADE)"
  set -a
  # shellcheck disable=SC1090
  . "envs/$ENV/.secrets"
  set +a

  db_url="${SPRING_DATASOURCE_URL:-}"
  [ -n "$db_url" ] || fail "SPRING_DATASOURCE_URL is required to reset the database"
  db_url="${db_url#jdbc:}"

  postgres_env=()
  [ -n "${SPRING_DATASOURCE_USERNAME:-}" ] && postgres_env+=(-e "PGUSER=$SPRING_DATASOURCE_USERNAME")
  [ -n "${SPRING_DATASOURCE_PASSWORD:-}" ] && postgres_env+=(-e "PGPASSWORD=$SPRING_DATASOURCE_PASSWORD")

  docker run --rm \
    "${postgres_env[@]}" \
    "$POSTGRES_IMAGE" \
    psql "$db_url" -v ON_ERROR_STOP=1 \
      -c 'DROP SCHEMA IF EXISTS public CASCADE;' \
      -c 'CREATE SCHEMA public;' \
      -c 'GRANT ALL ON SCHEMA public TO public;'
fi

log "Pulling API image tag=$IMAGE_TAG"
IMAGE_TAG="$IMAGE_TAG" "${compose_cmd[@]}" pull api || true

up_args=(up -d)
if [ "$FORCE_RECREATE" = "1" ]; then
  up_args+=(--force-recreate)
fi
up_args+=(api)

log "Starting API service tag=$IMAGE_TAG"
IMAGE_TAG="$IMAGE_TAG" "${compose_cmd[@]}" "${up_args[@]}"

log "Waiting for API health"
health_url="$API_BASE_URL/actuator/health"
for attempt in $(seq 1 36); do
  if curl -fsS --connect-timeout 5 --max-time 15 "$health_url" >/dev/null; then
    printf 'OK: API health OK (%s)\n' "$health_url"
    break
  fi
  if [ "$attempt" = "36" ]; then
    docker logs --tail 120 "tchl-api-$ENV" >&2 || true
    fail "API did not become healthy: $health_url"
  fi
  sleep 5
done

log "Checking CORS preflight"
for origin in $WEB_ORIGINS; do
  headers="$(mktemp /tmp/tchalanet-cors-headers.XXXXXX)"
  status="$(
    curl -sS --connect-timeout 5 --max-time 15 -o /dev/null -D "$headers" -w '%{http_code}' \
      -X OPTIONS "$API_BASE_URL/runtime/private" \
      -H "Origin: $origin" \
      -H "Access-Control-Request-Method: GET" \
      -H "Access-Control-Request-Headers: authorization,content-type,x-request-id"
  )"
  if [ "$status" != "200" ]; then
    cat "$headers" >&2
    rm -f "$headers"
    fail "CORS preflight failed for $origin with HTTP $status"
  fi
  if ! grep -Fqi "access-control-allow-origin: $origin" "$headers"; then
    cat "$headers" >&2
    rm -f "$headers"
    fail "CORS allow-origin missing for $origin"
  fi
  rm -f "$headers"
  printf 'OK: CORS preflight OK (%s)\n' "$origin"
done

log "Checking private endpoint unauthenticated response carries CORS"
origin="${WEB_ORIGINS%% *}"
headers="$(mktemp /tmp/tchalanet-private-headers.XXXXXX)"
status="$(
  curl -sS --connect-timeout 5 --max-time 15 -o /dev/null -D "$headers" -w '%{http_code}' \
    "$API_BASE_URL/runtime/private" \
    -H "Origin: $origin" \
    -H "X-Request-Id: deploy_api_staging_smoke"
)"
if [ "$status" != "401" ]; then
  cat "$headers" >&2
  rm -f "$headers"
  fail "Expected unauthenticated /runtime/private to return 401, got HTTP $status"
fi
if ! grep -Fqi "access-control-allow-origin: $origin" "$headers"; then
  cat "$headers" >&2
  rm -f "$headers"
  fail "Unauthenticated /runtime/private did not include CORS allow-origin"
fi
rm -f "$headers"
printf 'OK: Private unauthenticated smoke OK (401 with CORS)\n'

printf 'OK: API staging deploy complete - IMAGE_TAG=%s\n' "$IMAGE_TAG"
