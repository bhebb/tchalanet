#!/usr/bin/env bash
set -euo pipefail

# up-seq.sh — build/pull images puis start stack dans l'ordre
# Usage:
#   ./scripts/utils/up-seq.sh                # ENV=dev implicite (build local par défaut)
#   ./scripts/utils/up-seq.sh staging        # staging (pull par défaut)
#   ./scripts/utils/up-seq.sh dev --no-build # dev sans build local (pull uniquement)
#   ./scripts/utils/up-seq.sh prod --local-build # prod en build local (override du mode pull)
# Variables:
#   ENV=dev|staging|prod
#   LOCAL_BUILD=1 pour forcer build, 0 pour désactiver
# Priorité: option CLI > VARIABLE > heuristique (dev => build, sinon pull)

# 0) Parse args
ENV="${1:-${ENV:-dev}}"
shift || true
CLI_LOCAL_BUILD=""
for arg in "$@"; do
  case "$arg" in
    --local-build) CLI_LOCAL_BUILD="1" ;;
    --no-build)    CLI_LOCAL_BUILD="0" ;;
    *) echo "⚠️  Argument ignoré: $arg" >&2 ;;
  esac
done

# Déterminer LOCAL_BUILD effectif
if [[ -n "$CLI_LOCAL_BUILD" ]]; then
  LOCAL_BUILD="$CLI_LOCAL_BUILD"
else
  # Si variable fournie prendre sa valeur, sinon heuristique
  if [[ -n "${LOCAL_BUILD:-}" ]]; then
    LOCAL_BUILD="${LOCAL_BUILD}"
  else
    if [[ "$ENV" == "dev" ]]; then LOCAL_BUILD="1"; else LOCAL_BUILD="0"; fi
  fi
fi

export ENV LOCAL_BUILD

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

# 1) detect docker
DOCKER_BIN=""
if command -v docker >/dev/null 2>&1; then
  DOCKER_BIN="$(command -v docker)"
else
  echo "❌ docker introuvable" >&2; exit 2
fi

# 2) Compose files
FILES=(
  "compose/docker-compose-project.yml"
  "compose/docker-compose-traefik.yml"
  "compose/docker-compose-redis.yml"
)

# Postgres local uniquement en dev — staging/prod utilisent Neon
if [[ "$ENV" == "dev" ]]; then
  FILES+=("compose/docker-compose-postgres.yml")
fi

# Override générique (ports, extra_hosts)
if [[ -f "compose/docker-compose.override.yml" ]]; then
  FILES+=("compose/docker-compose.override.yml")
fi

# Inclure local-build si demandé
if [[ "$LOCAL_BUILD" == "1" ]]; then
  if [[ -f "compose/docker-compose.local-build.yml" ]]; then
    FILES+=("compose/docker-compose.local-build.yml")
  else
    echo "⚠️  local-build demandé mais compose/docker-compose.local-build.yml absent" >&2
  fi
fi

FILES+=(
  "compose/docker-compose-api.yml"
  "compose/docker-compose-edge-service.yml"
)

# 3) validate files
for f in "${FILES[@]}"; do
  [[ -f "$f" ]] || { echo "❌ Fichier compose manquant: $f" >&2; exit 3; }
done

# 4) env-file (interpolation sans secrets)
TMP_ENV_FILE="$(mktemp /tmp/tch-compose-env.XXXXXX)"
cleanup() { rm -f "$TMP_ENV_FILE" 2>/dev/null || true; }
trap cleanup EXIT
[[ -f "envs/common/compose.env" ]] && cat "envs/common/compose.env" > "$TMP_ENV_FILE" || true
[[ -f "envs/$ENV/compose.env" ]] && cat "envs/$ENV/compose.env" >> "$TMP_ENV_FILE" || true
if [[ "${INCLUDE_SECRETS_IN_INTERPOLATION:-0}" == "1" ]]; then
  [[ -f "envs/$ENV/.secrets" ]] && cat "envs/$ENV/.secrets" >> "$TMP_ENV_FILE" || true
  echo "ℹ️  Secrets inclus pour interpolation" >&2
fi

echo "→ ENV=$ENV LOCAL_BUILD=$LOCAL_BUILD"
echo "→ Fichiers compose: ${FILES[*]}"
echo "→ Fichier env interpolation: $TMP_ENV_FILE"
[[ -f "envs/$ENV/.env.merged" ]] || echo "⚠️  envs/$ENV/.env.merged absent (run make env-merge)" >&2

compose_files_args=()
for f in "${FILES[@]}"; do compose_files_args+=( -f "$f" ); done

# 5) Build ou Pull
if [[ "$LOCAL_BUILD" == "1" ]]; then
  echo "→ [BUILD] Construction des images locales"
  "$DOCKER_BIN" compose --env-file "$TMP_ENV_FILE" "${compose_files_args[@]}" build --parallel || true
else
  echo "→ [PULL] Récupération des images pré-construites"
  "$DOCKER_BIN" compose --env-file "$TMP_ENV_FILE" "${compose_files_args[@]}" pull || echo "⚠️  Pull partiel" >&2
fi

# 6) Up core
CORE_SVCS=(traefik redis)
if [[ "$ENV" == "dev" ]]; then
  CORE_SVCS=(traefik postgres redis)
fi
for svc in "${CORE_SVCS[@]}"; do
  echo "→ Up $svc"
  "$DOCKER_BIN" compose --project-name "tch-${ENV}" --env-file "$TMP_ENV_FILE" "${compose_files_args[@]}" up -d "$svc" || echo "⚠️  $svc up non-zero" >&2
done

# 7) Up API and Edge service
echo "→ Up api"
"$DOCKER_BIN" compose --project-name "tch-${ENV}" --env-file "$TMP_ENV_FILE" "${compose_files_args[@]}" up -d api || echo "⚠️  api up non-zero" >&2
echo "→ Up edge-service"
"$DOCKER_BIN" compose --project-name "tch-${ENV}" --env-file "$TMP_ENV_FILE" "${compose_files_args[@]}" up -d edge-service || echo "⚠️  edge-service up non-zero" >&2

echo "ℹ️  Stack initiale opérationnelle (LOCAL_BUILD=$LOCAL_BUILD)."
