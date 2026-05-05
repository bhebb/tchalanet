#!/usr/bin/env bash
# run-compose.sh — robust helper to run docker compose with merged envs and optional profiles
# Usage:
#   run-compose.sh "<profile args>" <action> <service_file> <service_name> "<extra_files>"
# Examples:
#   ./run-compose.sh "--profile core" up compose/docker-compose-traefik.yml traefik ""
#   ./run-compose.sh "--profile core --profile api" up compose/docker-compose-api.yml api "-f compose/docker-compose-postgres.yml -f compose/docker-compose-redis.yml"
# Notes:
#   - ENV controls which env set is used (dev|staging|prod). Default: dev
#   - If DOPPLER_TOKEN is present, the command is executed via `doppler run -- ...`
#   - Set MAKE_DEBUG=1 to see the final command before exec.

set -euo pipefail

# --- resolve docker binary (supports macOS Homebrew on Intel/ARM and Linux)
DOCKER_BIN="${DOCKER_BIN:-}"
if [[ -z "$DOCKER_BIN" ]]; then
  if command -v docker >/dev/null 2>&1; then
    DOCKER_BIN="$(command -v docker)"
  elif [[ -x /opt/homebrew/bin/docker ]]; then
    DOCKER_BIN=/opt/homebrew/bin/docker
  elif [[ -x /usr/local/bin/docker ]]; then
    DOCKER_BIN=/usr/local/bin/docker
  else
    echo "Error: docker not found in PATH. Install Docker Desktop or set DOCKER_BIN=/full/path/to/docker." >&2
    exit 127
  fi
fi

PROFILE_ARGS="${1:-}"
ACTION="${2:-}"
SERVICE_FILE="${3:-}"
SERVICE_NAME="${4:-}"
EXTRA_FILES_STR="${5:-}"

ENV="${ENV:-dev}"

# project root = two levels up from this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$ROOT_DIR"

# --- build merged env file (cleanup on exit)
# Only compose.env (interpolation vars) + .secrets (Doppler)
# .env files are merged separately by merge-env.sh → .env.merged (used via env_file: in compose YAML)
MERGED="$(mktemp /tmp/tchalanet-envs.XXXXXX)"
cleanup() { rm -f "$MERGED"; }
trap cleanup EXIT

# Merge order: common/compose.env (defaults) → env-specific compose.env → .secrets (sensitive)
[[ -f "envs/common/compose.env" ]] && cat "envs/common/compose.env" >>"$MERGED" || true
[[ -f "envs/$ENV/compose.env" ]]   && cat "envs/$ENV/compose.env"   >>"$MERGED" || true
[[ -f "envs/$ENV/.secrets" ]]      && cat "envs/$ENV/.secrets"      >>"$MERGED" || true

# --- compose base command (unique project per ENV)
cmd=( "$DOCKER_BIN" compose --project-name "tch-${ENV}" --env-file "$MERGED" )

# add profiles (split by words)
if [[ -n "$PROFILE_ARGS" ]]; then
  # shellcheck disable=SC2206
  read -r -a _pargs <<< "$PROFILE_ARGS"
  for p in "${_pargs[@]}"; do cmd+=("$p"); done
fi

# include project-level compose file if present
PROJECT_FILE="compose/docker-compose-project.yml"
[[ -f "$PROJECT_FILE" ]] && cmd+=(-f "$PROJECT_FILE")

# add extra -f files (support both formats: "file1 file2" OR "-f file1 -f file2")
if [[ -n "$EXTRA_FILES_STR" ]]; then
  # shellcheck disable=SC2206
  read -r -a _extras <<< "$EXTRA_FILES_STR"
  i=0
  while (( i < ${#_extras[@]} )); do
    token="${_extras[$i]}"
    if [[ "$token" == "-f" ]]; then
      ((i++))
      [[ $i -lt ${#_extras[@]} ]] || { echo "Missing file after -f" >&2; exit 3; }
      ef="${_extras[$i]}"
      [[ -f "$ef" ]] || { echo "Missing compose file: $ef" >&2; exit 3; }
      cmd+=(-f "$ef")
    else
      # token is a filename (no leading -f)
      ef="$token"
      [[ -f "$ef" ]] || { echo "Missing compose file: $ef" >&2; exit 3; }
      cmd+=(-f "$ef")
    fi
    ((i++))
  done
fi

# add the service-specific compose file (required)
if [[ -n "$SERVICE_FILE" ]]; then
  [[ -f "$SERVICE_FILE" ]] || { echo "Missing compose file: $SERVICE_FILE" >&2; exit 3; }
  cmd+=(-f "$SERVICE_FILE")
else
  echo "Missing required SERVICE_FILE argument." >&2
  exit 2
fi


# add action
case "$ACTION" in
  up)
    # If a single service is specified, avoid --remove-orphans to prevent removing other containers
    # when the intent is just to recreate/restart one service (safer default).
    if [[ -n "${SERVICE_NAME:-}" ]]; then
      cmd+=( up -d "$SERVICE_NAME" )
    else
      cmd+=( up -d --remove-orphans )
    fi
    ;;
  run)
    # run a one-off service (use --rm to remove container after exit)
    if [[ -n "${SERVICE_NAME:-}" ]]; then
      cmd+=( run --rm "$SERVICE_NAME" )
    else
      echo "run action requires a SERVICE_NAME" >&2; exit 2
    fi
    ;;
  down)    cmd+=( down --remove-orphans ) ;;
  logs)    cmd+=( logs -f "${SERVICE_NAME:-}" ) ;;
  config)  cmd+=( config ) ;;
  ps)      cmd+=( ps ) ;;
  *)
    echo "Unknown action: $ACTION (expected: up|down|logs|config|ps|run)" >&2
    exit 2
    ;;
esac

# debug print
if [[ -n "${MAKE_DEBUG:-}" ]]; then
  echo "ENV=$ENV" >&2
  echo "Using docker: $DOCKER_BIN" >&2
  echo "Using env file: $MERGED" >&2
  echo "Final cmd: ${cmd[*]}" >&2
fi

# --- optional Doppler wrapper
if [[ -n "${DOPPLER_TOKEN:-}" ]]; then
  echo "→ Using Doppler for secrets injection" >&2
  exec "$DOCKER_BIN" run --rm \
    -e DOPPLER_TOKEN="$DOPPLER_TOKEN" \
    -v "$PWD":/work -w /work \
    dopplerhq/cli:latest \
    doppler run -- "${cmd[@]}"
else
  exec "${cmd[@]}"
fi
