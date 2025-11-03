#!/usr/bin/env bash
set -euo pipefail

ENV="${1:-staging}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# Use DOCKER_CMD from environment (defaults to 'docker')
# This allows bootstrap to pass 'sudo docker' if needed
DOCKER_CMD="${DOCKER_CMD:-docker}"

# Couleurs pour les messages
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

# Fonctions pour les messages
log() { echo -e "${BLUE}→${NC} $*" >&2; }
success() { echo -e "${GREEN}✔${NC} $*" >&2; }
error() { echo -e "${RED}✖${NC} $*" >&2; exit 1; }

# Prefer using envs/<env>/compose.env for compose-time variables
COMPOSE_ENV_FILE="$ROOT_DIR/envs/$ENV/compose.env"
if [ ! -f "$COMPOSE_ENV_FILE" ]; then
  echo "ERROR: compose.env not found for environment '$ENV'. Expected: $COMPOSE_ENV_FILE" >&2
  echo "Create envs/$ENV/compose.env (copy from envs/common/compose.env if needed) and retry." >&2
  exit 2
fi

# Source compose env for DOCKER_NETWORK_* variables
# shellcheck disable=SC1090
set -a; . "$COMPOSE_ENV_FILE"; set +a
log "Loaded compose env: $COMPOSE_ENV_FILE"

# Determine network names (allow overrides via env)
EDGE_NAME="${DOCKER_NETWORK_EDGE:-edge-${ENV}}"
BACK_NAME="${DOCKER_NETWORK_BACK:-back-${ENV}}"

# Créer les réseaux s'ils n'existent pas
create_network() {
  local name="$1"
  if ! $DOCKER_CMD network inspect "$name" >/dev/null 2>&1; then
    log "Création du réseau $name..."
    $DOCKER_CMD network create "$name"
    success "Réseau $name créé"
  else
    success "Réseau $name existe déjà"
  fi
}

create_network "$EDGE_NAME"
create_network "$BACK_NAME"

log "Liste des réseaux Docker (filtrés) :"
$DOCKER_CMD network ls | grep -E "(${EDGE_NAME}|${BACK_NAME})" || true
