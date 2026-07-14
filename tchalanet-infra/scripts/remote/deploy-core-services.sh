#!/usr/bin/env bash
# Deploy core infrastructure services only: Traefik + Redis.
set -euo pipefail

ENV="${ENV:-staging}"
DOPPLER_IMAGE="${DOPPLER_IMAGE:-dopplerhq/cli:3.75.1}"
DOCKER_BIN="${DOCKER_BIN:-docker}"

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

log() { printf -- '-> %s\n' "$*"; }
fail() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }
require_file() { [ -f "$1" ] || fail "Missing required file: $1"; }

print_core_diagnostics() {
  log "Core diagnostics"
  $DOCKER_BIN ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' >&2 || true
  $DOCKER_BIN logs --tail 120 "tchl-traefik-$ENV" >&2 || true
  $DOCKER_BIN logs --tail 120 "tchl-redis-$ENV" >&2 || true
  if command -v ss >/dev/null 2>&1; then
    ss -ltnp | grep -E ':(80|443|6379)\b' >&2 || true
  elif command -v netstat >/dev/null 2>&1; then
    netstat -ltnp 2>/dev/null | grep -E ':(80|443|6379)\b' >&2 || true
  fi
}

case "$ENV" in
  staging|stg)
    ENV="staging"
    DOPPLER_CONFIG="stg"
    TRAEFIK_ENV_FILE="staging.yaml"
    ;;
  prod|production)
    ENV="prod"
    DOPPLER_CONFIG="prd"
    TRAEFIK_ENV_FILE="prod.yaml"
    ;;
  *)
    fail "ENV must be staging or prod"
    ;;
esac

require_file "envs/common/compose.env"
require_file "envs/$ENV/compose.env"
require_file "compose/docker-compose-project.yml"
require_file "compose/docker-compose-traefik.yml"
require_file "compose/docker-compose-redis.yml"
require_file "scripts/utils/merge-env.sh"
require_file "scripts/utils/pre-render-traefik.sh"

log "Preparing Docker networks for $ENV"
$DOCKER_BIN network create "edge-$ENV" >/dev/null 2>&1 || true
$DOCKER_BIN network create "back-$ENV" >/dev/null 2>&1 || true

log "Merging core env files"
scripts/utils/merge-env.sh "$ENV"

log "Rendering Traefik routes for $ENV"
chmod +x scripts/utils/pre-render-traefik.sh || true
scripts/utils/pre-render-traefik.sh "$TRAEFIK_ENV_FILE"
mkdir -p traefik
touch traefik/acme.json
chmod 600 traefik/acme.json || true

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

compose_env="$(mktemp /tmp/tchalanet-core-compose-env.XXXXXX)"
cleanup() { rm -f "$compose_env"; }
trap cleanup EXIT
cat "envs/common/compose.env" "envs/$ENV/compose.env" "envs/$ENV/.secrets" > "$compose_env"

compose_cmd=(
  $DOCKER_BIN compose
  --project-name "tch-$ENV"
  --env-file "$compose_env"
  -f compose/docker-compose-project.yml
  -f compose/docker-compose-traefik.yml
  -f compose/docker-compose-redis.yml
)

log "Pulling core images"
"${compose_cmd[@]}" pull traefik redis

log "Starting core services"
"${compose_cmd[@]}" up -d traefik redis

log "Checking Traefik ping"
for attempt in $(seq 1 12); do
  if curl -fsS --connect-timeout 2 --max-time 5 http://127.0.0.1/ping >/dev/null; then
    printf 'OK: Traefik ping OK\n'
    break
  fi
  if [ "$attempt" = "12" ]; then
    print_core_diagnostics
    fail "Traefik did not expose local HTTP ping on port 80"
  fi
  sleep 5
done

log "Checking Redis health"
for attempt in $(seq 1 12); do
  health_status="$($DOCKER_BIN inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "tchl-redis-$ENV" 2>/dev/null || true)"
  if [ "$health_status" = "healthy" ]; then
    printf 'OK: Redis healthy\n'
    break
  fi
  if [ "$attempt" = "12" ]; then
    print_core_diagnostics
    fail "Redis did not become healthy, last status=$health_status"
  fi
  sleep 5
done

printf 'OK: Core infra ready - ENV=%s services=traefik,redis\n' "$ENV"
