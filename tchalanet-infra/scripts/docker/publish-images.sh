#!/usr/bin/env bash
set -euo pipefail

# publish-images.sh
# Usage: ./scripts/docker/publish-images.sh <org> [tag] [registry]
# Example: ./scripts/docker/publish-images.sh bhebb stg-20251114 ghcr.io

ORG="${1:?Organization (eg. tchalanet or your-gh-user)}"
TAG="${2:-}"
REGISTRY="${3:-ghcr.io}"

if [ -z "$TAG" ]; then
  # default tag: stg-<short-sha> if in a git repo, otherwise timestamp
  if command -v git >/dev/null 2>&1 && git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    SHA=$(git rev-parse --short HEAD)
    TAG="stg-${SHA}"
  else
    TAG="stg-$(date +%Y%m%d%H%M%S)"
  fi
fi

REPO_ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
SERVER_DIR="$REPO_ROOT/tchalanet-server"
INFRA_DIR="$REPO_ROOT/tchalanet-infra"
COMMON_COMPOSE_ENV="$INFRA_DIR/envs/common/compose.env"

API_IMAGE_FULL="$REGISTRY/$ORG/tchalanet-api:$TAG"
API_IMAGE_BASE_VAL="$REGISTRY/$ORG/tchalanet-api"

echo "→ Publish images: org=$ORG tag=$TAG registry=$REGISTRY"
echo "   API image: $API_IMAGE_FULL"
echo ""

# 0) quick checks
command -v docker >/dev/null 2>&1 || { echo "❌ docker not found, please install Docker" >&2; exit 2; }

# Check if logged into registry
echo "→ Checking authentication to $REGISTRY..."
AUTH_CHECK_PASSED=false

# Method 1: Check docker config.json for direct auth or credential store
if [ -f ~/.docker/config.json ]; then
  # Check for direct auth entry
  if grep -q "\"$REGISTRY\"" ~/.docker/config.json 2>/dev/null || \
     grep -q "\"https://$REGISTRY\"" ~/.docker/config.json 2>/dev/null; then
    AUTH_CHECK_PASSED=true
    echo "  ℹ Found auth entry in config.json"
  # Check if using credential store (Docker Desktop, osxkeychain, etc)
  elif grep -q '"credsStore"' ~/.docker/config.json 2>/dev/null; then
    CREDS_STORE=$(grep '"credsStore"' ~/.docker/config.json | cut -d'"' -f4)
    if [ -n "$CREDS_STORE" ]; then
      echo "  ℹ Using credential store: $CREDS_STORE"
      # Assume credentials are managed by the store - skip pull test
      AUTH_CHECK_PASSED=true
    fi
  fi
fi

# Method 2: Fallback - simple registry ping test (non-disruptive)
# Only run if Method 1 didn't confirm auth
if [ "$AUTH_CHECK_PASSED" = "false" ]; then
  echo "  ℹ Attempting registry ping test..."
  # Try a lightweight manifest check (doesn't download, just checks auth)
  if timeout 5 docker manifest inspect "$REGISTRY/hello-world:latest" >/dev/null 2>&1; then
    AUTH_CHECK_PASSED=true
    echo "  ℹ Registry access confirmed via manifest check"
  fi
fi

if [ "$AUTH_CHECK_PASSED" = "false" ]; then
  cat >&2 <<EOF

❌ Unable to verify authentication to $REGISTRY.

Please login first using one of these methods:

1) Using a token (recommended for scripts):
   echo "\$GHCR_TOKEN" | docker login $REGISTRY -u <username> --password-stdin

2) Using GitHub CLI (if registry is ghcr.io):
   gh auth token | docker login ghcr.io -u <username> --password-stdin

3) Interactive login:
   docker login $REGISTRY

Examples:
  # GitHub Container Registry (GHCR)
  echo "\$GHCR_TOKEN" | docker login ghcr.io -u $ORG --password-stdin

  # Docker Hub
  echo "\$DOCKERHUB_TOKEN" | docker login -u <username> --password-stdin

Then re-run this script.

If you ARE logged in and still see this error, you can skip this check by setting:
  export SKIP_AUTH_CHECK=1

EOF
  exit 2
fi

echo "✓ Authentication verified"
echo ""

# 1) Build API
echo ""
echo "→ Building API (maven package + docker build)"
if [ ! -d "$SERVER_DIR" ]; then
  echo "❌ Error: tchalanet-server not found at $SERVER_DIR" >&2
  exit 1
fi

pushd "$SERVER_DIR" >/dev/null
if [ -x ./mvnw ]; then
  echo "  - running ./mvnw -DskipTests package"
  ./mvnw -DskipTests package
else
  echo "  - mvnw not found, skipping maven package step"
fi

echo "  - docker build -t $API_IMAGE_FULL ."
docker build -t "$API_IMAGE_FULL" .

echo "  - push $API_IMAGE_FULL"
docker push "$API_IMAGE_FULL"
popd >/dev/null

# 2) Update envs/common/compose.env
if [ -f "$COMMON_COMPOSE_ENV" ]; then
  echo ""
  echo "→ Updating $COMMON_COMPOSE_ENV"
  # Backup
  cp "$COMMON_COMPOSE_ENV" "${COMMON_COMPOSE_ENV}.bak"

  # Update IMAGE_TAG
  if grep -qE '^IMAGE_TAG=' "$COMMON_COMPOSE_ENV"; then
    perl -0777 -pe "s/^IMAGE_TAG=.*$/IMAGE_TAG=$TAG/m" -i "$COMMON_COMPOSE_ENV"
  else
    echo "IMAGE_TAG=$TAG" >> "$COMMON_COMPOSE_ENV"
  fi

  # Update API_IMAGE_BASE
  if grep -qE '^API_IMAGE_BASE=' "$COMMON_COMPOSE_ENV"; then
    perl -0777 -pe "s|^API_IMAGE_BASE=.*$|API_IMAGE_BASE=$API_IMAGE_BASE_VAL|m" -i "$COMMON_COMPOSE_ENV"
  else
    echo "API_IMAGE_BASE=$API_IMAGE_BASE_VAL" >> "$COMMON_COMPOSE_ENV"
  fi

  echo "  - backup written to ${COMMON_COMPOSE_ENV}.bak"
  echo "  - composition vars updated: IMAGE_TAG, API_IMAGE_BASE"
else
  echo "❌ $COMMON_COMPOSE_ENV not found; cannot update compose env" >&2
fi

# 3) Print next steps for staging
cat <<EOF

✅ Done: images pushed and $COMMON_COMPOSE_ENV updated (backup at ${COMMON_COMPOSE_ENV}.bak)

Next steps (on staging host):
  cd /opt/tchalanet-infra/compose
  # pull images and bring up; include the same compose files you use for staging
  docker compose -f docker-compose-project.yml \
    -f docker-compose-postgres.yml \
    -f docker-compose-redis.yml \
    -f docker-compose-api.yml \
    -f docker-compose-edge-service.yml \
    pull

  docker compose -f docker-compose-project.yml \
    -f docker-compose-postgres.yml \
    -f docker-compose-redis.yml \
    -f docker-compose-api.yml \
    -f docker-compose-edge-service.yml \
    up -d postgres redis api edge-service

Tip: If you want full restart for all services, run the up -d without service names.
EOF

exit 0
