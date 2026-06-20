#!/usr/bin/env bash
set -euo pipefail

ENV="${ENV:-dev}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

DOCKER_BIN="${DOCKER_BIN:-docker}"

"$DOCKER_BIN" compose \
  --env-file <(cat envs/common/compose.env envs/"$ENV"/compose.env 2>/dev/null) \
  -f compose/docker-compose-project.yml \
  -f compose/docker-compose-postgres.yml \
  -f compose/docker-compose-redis.yml \
  -f compose/docker-compose-traefik.yml \
  -f compose/docker-compose-api.yml \
  -f compose/docker-compose-edge-service.yml \
  ps

"$DOCKER_BIN" compose \
  --env-file <(cat envs/common/compose.env envs/"$ENV"/compose.env 2>/dev/null) \
  -f compose/docker-compose-project.yml \
  -f compose/docker-compose-postgres.yml \
  -f compose/docker-compose-redis.yml \
  -f compose/docker-compose-traefik.yml \
  -f compose/docker-compose-api.yml \
  -f compose/docker-compose-edge-service.yml \
  config >/dev/null
