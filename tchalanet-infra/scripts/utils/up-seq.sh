#!/usr/bin/env bash
set -euo pipefail

# up-seq.sh — build images (compose.env) then start stack in order with robust Keycloak readiness
# Usage:
#   ENV=dev ./scripts/utils/up-seq.sh
#   ./scripts/utils/up-seq.sh dev
# Note: post-install/bootstrap behavior removed — tokens and one-shot bootstrap steps must be run manually.

# 0) ENV
ENV="${1:-${ENV:-dev}}"

# Important : rendre ENV visible pour run-compose.sh et docker compose
export ENV

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

# 1) detect docker binary (portable)
DOCKER_BIN=""
if command -v docker >/dev/null 2>&1; then
  DOCKER_BIN="$(command -v docker)"
elif [[ -x /opt/homebrew/bin/docker ]]; then
  DOCKER_BIN=/opt/homebrew/bin/docker
elif [[ -x /usr/local/bin/docker ]]; then
  DOCKER_BIN=/usr/local/bin/docker
else
  echo "❌ docker introuvable dans PATH" >&2
  exit 2
fi

# 2) Compose files (ordre important)
FILES=(
  "compose/docker-compose-project.yml"
  "compose/docker-compose-traefik.yml"
  "compose/docker-compose-postgres.yml"
  "compose/docker-compose-redis.yml"
  "compose/docker-compose.local-build.yml"
  "compose/docker-compose-keycloak.yml"
  "compose/docker-compose-unleash.yml"
  "compose/docker-compose-api.yml"
  "compose/docker-compose-meilisearch.yml"
)

# 3) profiles: disabled (nous n'utilisons plus les profiles)
PROFILES=""

# validate files exist
for f in "${FILES[@]}"; do
  [[ -f "$f" ]] || { echo "❌ Missing compose file: $f" >&2; exit 3; }
done

# run-compose wrapper
RUN="$ROOT/scripts/utils/run-compose.sh"
[[ -x "$RUN" ]] || { echo "❌ Missing $RUN (chmod +x ?)" >&2; exit 2; }

# 4) prepare tmp env-file (common then env-specific)
TMP_ENV_FILE="$(mktemp /tmp/tch-compose-env.XXXXXX)"
cleanup() { rm -f "$TMP_ENV_FILE" 2>/dev/null || true; }
trap cleanup EXIT

[[ -f "envs/common/compose.env" ]] && cat "envs/common/compose.env" > "$TMP_ENV_FILE" || true
[[ -f "envs/$ENV/compose.env" ]] && cat "envs/$ENV/compose.env" >> "$TMP_ENV_FILE" || true
[[ -f "envs/$ENV/.secrets" ]] && cat "envs/$ENV/.secrets" >> "$TMP_ENV_FILE" || true
echo "→ Build: using env-file $TMP_ENV_FILE"

# Create a persistent .env.merged file in envs/$ENV used at runtime by compose env_file declarations
MERGED_FILE="envs/$ENV/.env.merged"
echo "→ Generating persistent merged env file: $MERGED_FILE"
mkdir -p "envs/$ENV"
>"$MERGED_FILE"
[[ -f "envs/common/compose.env" ]] && cat "envs/common/compose.env" >> "$MERGED_FILE" || true
[[ -f "envs/$ENV/compose.env" ]] && cat "envs/$ENV/compose.env" >> "$MERGED_FILE" || true
[[ -f "envs/$ENV/.secrets" ]] && cat "envs/$ENV/.secrets" >> "$MERGED_FILE" || true
chmod 600 "$MERGED_FILE" 2>/dev/null || true
echo "→ Wrote merged env to $MERGED_FILE (last keys win)"

# 5) build: create array of -f args and call docker compose once for performance
compose_files_args=()
for f in "${FILES[@]}"; do
  compose_files_args+=( -f "$f" )
done

echo "→ Building Keycloak image first (attempt)"
if ! "$DOCKER_BIN" compose --env-file "$TMP_ENV_FILE" "${compose_files_args[@]}" build --parallel keycloak; then
  echo "⚠️ Keycloak build failed — continuing; other images will be built. Check build logs to debug." >&2
  "$DOCKER_BIN" compose --env-file "$TMP_ENV_FILE" "${compose_files_args[@]}" build --parallel || true
else
  echo "→ Keycloak build succeeded"
fi

echo "→ Building remaining images in parallel"
"$DOCKER_BIN" compose --env-file "$TMP_ENV_FILE" "${compose_files_args[@]}" build --parallel || true

echo "→ Pulling images referenced by local-build compose if needed"
"$DOCKER_BIN" compose --env-file "$TMP_ENV_FILE" \
  -f compose/docker-compose-project.yml \
  -f compose/docker-compose.local-build.yml \
  pull || true

# 6) helper string for run-compose wrapper (it expects a single string with -f tokens)
extra_files_str=""
for f in "${FILES[@]}"; do
  if [ -z "$extra_files_str" ]; then
    extra_files_str="-f $f"
  else
    extra_files_str="$extra_files_str -f $f"
  fi
done

# 7) bring up core services
echo "→ [1/3] Bring up core: traefik, postgres, redis"
if ! "$RUN" "$PROFILES" up "compose/docker-compose-traefik.yml" traefik "$extra_files_str"; then
  echo "⚠️ traefik up returned non-zero; continuing" >&2
fi
if ! "$RUN" "$PROFILES" up "compose/docker-compose-postgres.yml" postgres "$extra_files_str"; then
  echo "⚠️ postgres up returned non-zero; continuing" >&2
fi
if ! "$RUN" "$PROFILES" up "compose/docker-compose-redis.yml" redis "$extra_files_str"; then
  echo "⚠️ redis up returned non-zero; continuing" >&2
fi

# 8) bring up Keycloak
echo "→ [2/3] Bring up Keycloak"
if ! "$RUN" "$PROFILES" up "compose/docker-compose-keycloak.yml" keycloak "$extra_files_str"; then
  echo "⚠️ Keycloak 'up' returned non-zero; continuing" >&2
fi

# 9) wait for Keycloak realm (plus robuste)
REALM="${KC_REALM:-tchalanet}"
KC_INTERNAL_URL="${KC_INTERNAL_URL:-http://keycloak:8080}"
chmod +x scripts/utils/wait-keycloak.sh || true
if ! scripts/utils/wait-keycloak.sh "${REALM}" "${KC_INTERNAL_URL}" 240; then
  echo "❌ Keycloak realm '${REALM}' not ready; check logs" >&2
  exit 1
fi

# 10) bring up base app services
echo "→ [3/3] Bring up: api, unleash, meilisearch"
"$RUN" "$PROFILES" up "compose/docker-compose-api.yml" api "$extra_files_str" || echo "⚠️ api up non-zero" >&2
"$RUN" "$PROFILES" up "compose/docker-compose-unleash.yml" unleash "$extra_files_str" || echo "⚠️ unleash up non-zero" >&2
"$RUN" "$PROFILES" up "compose/docker-compose-meilisearch.yml" meilisearch "$extra_files_str" || echo "⚠️ meilisearch up non-zero" >&2


echo "ℹ️  Stack base is up."
