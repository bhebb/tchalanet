#!/usr/bin/env bash
# Deploy API + edge-service from published GHCR images on staging or production.
set -euo pipefail

ENV="${ENV:-staging}"
API_IMAGE_TAG="${API_IMAGE_TAG:-${IMAGE_TAG:-}}"
EDGE_IMAGE_TAG="${EDGE_IMAGE_TAG:-}"
DEPLOY_API="${DEPLOY_API:-1}"
DEPLOY_EDGE="${DEPLOY_EDGE:-1}"
FORCE_RECREATE="${FORCE_RECREATE:-1}"
RESET_DATABASE="${RESET_DATABASE:-0}"
RESET_DATABASE_CONFIRM="${RESET_DATABASE_CONFIRM:-}"
API_BASE_URL="${API_BASE_URL:-}"
WEB_ORIGINS="${WEB_ORIGINS:-}"
DOPPLER_IMAGE="${DOPPLER_IMAGE:-dopplerhq/cli:3.75.1}"
POSTGRES_IMAGE="${POSTGRES_IMAGE:-postgres:18.4}"
DOCKER_BIN="${DOCKER_BIN:-docker}"

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

log() { printf -- '-> %s\n' "$*"; }
fail() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }
require_file() { [ -f "$1" ] || fail "Missing required file: $1"; }
print_runtime_diagnostics() {
  log "Runtime diagnostics"
  $DOCKER_BIN ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' >&2 || true
  $DOCKER_BIN logs --tail 120 "tchl-traefik-$ENV" >&2 || true
  $DOCKER_BIN logs --tail 120 "tchl-api-$ENV" >&2 || true
  if command -v ss >/dev/null 2>&1; then
    ss -ltnp | grep -E ':(80|443)\b' >&2 || true
  elif command -v netstat >/dev/null 2>&1; then
    netstat -ltnp 2>/dev/null | grep -E ':(80|443)\b' >&2 || true
  fi
}
inspect_health() {
  $DOCKER_BIN inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$1" 2>/dev/null || true
}
require_core_services_ready() {
  log "Checking core infra services"
  traefik_status="$(inspect_health "tchl-traefik-$ENV")"
  redis_status="$(inspect_health "tchl-redis-$ENV")"
  if [ "$traefik_status" != "healthy" ] && [ "$traefik_status" != "running" ]; then
    print_runtime_diagnostics
    fail "Traefik core service is not ready, status=$traefik_status. Run the Manage Staging Infra workflow first."
  fi
  if ! curl -fsS --connect-timeout 2 --max-time 5 http://127.0.0.1/ping >/dev/null; then
    print_runtime_diagnostics
    fail "Traefik core service is not listening on local port 80. Run the Manage Staging Infra workflow first."
  fi
  if [ "$redis_status" != "healthy" ]; then
    print_runtime_diagnostics
    fail "Redis core service is not healthy, status=$redis_status. Run the Manage Staging Infra workflow first."
  fi
  printf 'OK: Core infra ready (traefik=%s redis=%s)\n' "$traefik_status" "$redis_status"
}

case "$ENV" in
  staging|stg)
    ENV="staging"
    DOPPLER_CONFIG="stg"
    API_BASE_URL="${API_BASE_URL:-https://api.stg.tchalanet.com/api/v1}"
    WEB_ORIGINS="${WEB_ORIGINS:-https://tchalanet-web-stg.pages.dev}"
    ;;
  prod|production)
    ENV="prod"
    DOPPLER_CONFIG="prd"
    API_BASE_URL="${API_BASE_URL:-https://api.tchalanet.com/api/v1}"
    WEB_ORIGINS="${WEB_ORIGINS:-https://tchalanet.com https://www.tchalanet.com https://app.tchalanet.com https://admin.tchalanet.com https://portal.tchalanet.com}"
    ;;
  *)
    fail "ENV must be staging or prod"
    ;;
esac

case "$DEPLOY_API" in
  1|true|yes) DEPLOY_API="1" ;;
  0|false|no) DEPLOY_API="0" ;;
  *) fail "DEPLOY_API must be 1/true or 0/false" ;;
esac
case "$DEPLOY_EDGE" in
  1|true|yes) DEPLOY_EDGE="1" ;;
  0|false|no) DEPLOY_EDGE="0" ;;
  *) fail "DEPLOY_EDGE must be 1/true or 0/false" ;;
esac
[ "$DEPLOY_API" = "1" ] || [ "$DEPLOY_EDGE" = "1" ] || fail "At least one of DEPLOY_API or DEPLOY_EDGE must be enabled"

if [ "$DEPLOY_EDGE" = "1" ] && [ -z "$EDGE_IMAGE_TAG" ] && [ -n "$API_IMAGE_TAG" ]; then
  EDGE_IMAGE_TAG="$API_IMAGE_TAG"
fi

if [ "$DEPLOY_API" = "1" ]; then
  [ -n "$API_IMAGE_TAG" ] || fail "API_IMAGE_TAG is required when DEPLOY_API=1"
  [ "$API_IMAGE_TAG" != "latest" ] || fail "API_IMAGE_TAG must be immutable, not latest"
fi
if [ "$DEPLOY_EDGE" = "1" ]; then
  [ -n "$EDGE_IMAGE_TAG" ] || fail "EDGE_IMAGE_TAG is required when DEPLOY_EDGE=1"
  [ "$EDGE_IMAGE_TAG" != "latest" ] || fail "EDGE_IMAGE_TAG must be immutable, not latest"
fi
COMPOSE_API_TAG="${API_IMAGE_TAG:-unused}"
COMPOSE_EDGE_TAG="${EDGE_IMAGE_TAG:-unused}"

require_file "envs/common/compose.env"
require_file "envs/$ENV/compose.env"
require_file "compose/docker-compose-project.yml"
require_file "compose/docker-compose-redis.yml"
require_file "compose/docker-compose-api.yml"
require_file "compose/docker-compose-edge-service.yml"
require_file "scripts/remote/prepare-firebase-admin-credentials.sh"

log "Preparing Docker networks for $ENV"
$DOCKER_BIN network create "edge-$ENV" >/dev/null 2>&1 || true
$DOCKER_BIN network create "back-$ENV" >/dev/null 2>&1 || true

log "Merging runtime env files"
scripts/utils/merge-env.sh "$ENV"

if [ "${SKIP_DOPPLER:-0}" != "1" ]; then
  [ -n "${DOPPLER_TOKEN:-}" ] || fail "DOPPLER_TOKEN is required unless SKIP_DOPPLER=1"
  log "Downloading $ENV secrets from Doppler config=$DOPPLER_CONFIG"
  $DOCKER_BIN run --rm \
    --entrypoint sh \
    -e DOPPLER_TOKEN="$DOPPLER_TOKEN" \
    -v "$PWD":/work -w /work \
    "$DOPPLER_IMAGE" \
    -lc "doppler secrets download --no-file --format env --project tchalanet --config $DOPPLER_CONFIG > envs/$ENV/.secrets"
  if command -v sudo >/dev/null 2>&1; then
    sudo chown "$(id -u):$(id -g)" "envs/$ENV/.secrets"
  else
    chown "$(id -u):$(id -g)" "envs/$ENV/.secrets" 2>/dev/null || true
  fi
  chmod 600 "envs/$ENV/.secrets"
elif [ ! -f "envs/$ENV/.secrets" ]; then
  fail "SKIP_DOPPLER=1 was set but envs/$ENV/.secrets does not exist"
fi

if [ "$DEPLOY_API" = "1" ]; then
  log "Preparing Firebase Admin credentials"
  scripts/remote/prepare-firebase-admin-credentials.sh "$ENV"
fi

compose_env="$(mktemp /tmp/tchalanet-compose-env.XXXXXX)"
cleanup() { rm -f "$compose_env"; }
trap cleanup EXIT
cat "envs/common/compose.env" "envs/$ENV/compose.env" "envs/$ENV/.secrets" > "$compose_env"

compose_cmd=(
  $DOCKER_BIN compose
  --project-name "tch-$ENV"
  --env-file "$compose_env"
  -f compose/docker-compose-project.yml
  -f compose/docker-compose-redis.yml
  -f compose/docker-compose-api.yml
  -f compose/docker-compose-edge-service.yml
)

if [ "$RESET_DATABASE" = "1" ]; then
  [ "$DEPLOY_API" = "1" ] || fail "RESET_DATABASE requires DEPLOY_API=1"
  [ "$ENV" = "staging" ] || fail "RESET_DATABASE is only allowed for ENV=staging"
  [ "$RESET_DATABASE_CONFIRM" = "destroy staging database" ] || \
    fail "RESET_DATABASE_CONFIRM must be exactly: destroy staging database"

  log "Stopping API before staging database reset"
  API_IMAGE_TAG="$COMPOSE_API_TAG" IMAGE_TAG="$COMPOSE_API_TAG" TCH_EDGE_TAG="$COMPOSE_EDGE_TAG" \
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

  $DOCKER_BIN run --rm \
    "${postgres_env[@]}" \
    "$POSTGRES_IMAGE" \
    psql "$db_url" -v ON_ERROR_STOP=1 \
      -c 'DROP SCHEMA IF EXISTS public CASCADE;' \
      -c 'CREATE SCHEMA public;' \
      -c 'GRANT ALL ON SCHEMA public TO public;'
fi

services=()
[ "$DEPLOY_API" = "1" ] && services+=(api)
[ "$DEPLOY_EDGE" = "1" ] && services+=(edge-service)

require_core_services_ready

log "Pulling runtime images deploy_api=$DEPLOY_API api=${API_IMAGE_TAG:-<unchanged>} deploy_edge=$DEPLOY_EDGE edge=${EDGE_IMAGE_TAG:-<unchanged>}"
IMAGE_TAG="$COMPOSE_API_TAG" TCH_EDGE_TAG="$COMPOSE_EDGE_TAG" "${compose_cmd[@]}" pull "${services[@]}" || true

up_args=(up -d --no-deps)
if [ "$FORCE_RECREATE" = "1" ]; then
  up_args+=(--force-recreate)
fi

log "Starting runtime services"
start_services=()
[ "$DEPLOY_EDGE" = "1" ] && start_services+=(edge-service)
[ "$DEPLOY_API" = "1" ] && start_services+=(api)
IMAGE_TAG="$COMPOSE_API_TAG" TCH_EDGE_TAG="$COMPOSE_EDGE_TAG" "${compose_cmd[@]}" "${up_args[@]}" "${start_services[@]}"

if [ "$DEPLOY_API" = "1" ]; then
  log "Checking API container health"
  for attempt in $(seq 1 36); do
    health_status="$(inspect_health "tchl-api-$ENV")"
    if [ "$health_status" = "healthy" ]; then
      printf 'OK: API container healthy\n'
      break
    fi
    if [ "$attempt" = "36" ]; then
      print_runtime_diagnostics
      fail "API container did not become healthy, last status=$health_status"
    fi
    sleep 5
  done

  log "Waiting for API health"
  health_url="$API_BASE_URL/actuator/health"
  for attempt in $(seq 1 36); do
    if curl -fsS --connect-timeout 5 --max-time 15 "$health_url" >/dev/null; then
      printf 'OK: API health OK (%s)\n' "$health_url"
      break
    fi
    if [ "$attempt" = "36" ]; then
      print_runtime_diagnostics
      fail "API did not become healthy: $health_url"
    fi
    sleep 5
  done
fi

if [ "$DEPLOY_EDGE" = "1" ]; then
  log "Checking edge-service readiness"
  for attempt in $(seq 1 24); do
    if $DOCKER_BIN exec "tchalanet-edge-service-$ENV" wget -qO- http://127.0.0.1:3000/ready >/dev/null 2>&1; then
      printf 'OK: edge-service ready\n'
      break
    fi
    if [ "$attempt" = "24" ]; then
      $DOCKER_BIN logs --tail 120 "tchalanet-edge-service-$ENV" >&2 || true
      fail "edge-service did not become ready"
    fi
    sleep 5
  done
fi

if [ "$DEPLOY_API" = "1" ]; then
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
      -H "X-Request-Id: deploy_runtime_services_smoke"
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
fi

printf 'OK: Runtime deploy complete - ENV=%s DEPLOY_API=%s API_IMAGE_TAG=%s DEPLOY_EDGE=%s EDGE_IMAGE_TAG=%s\n' "$ENV" "$DEPLOY_API" "${API_IMAGE_TAG:-}" "$DEPLOY_EDGE" "${EDGE_IMAGE_TAG:-}"
