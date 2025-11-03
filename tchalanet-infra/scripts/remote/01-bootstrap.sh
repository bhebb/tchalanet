
#!/usr/bin/env bash
set -euo pipefail

# bootstrap remote server for an environment
# Usage: ./scripts/remote/bootstrap.sh <env>
ENV="${1:-staging}"
ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

# source envs/<env>/.env if present
[ -f "envs/${ENV}/.env" ] && set -a && . "envs/${ENV}/.env" && set +a || true

log(){ echo "[bootstrap:$ENV] $*"; }

# Ensure docker installed
DOCKER_JUST_INSTALLED=0
if ! command -v docker >/dev/null 2>&1; then
  log "Installing Docker..."
  "$ROOT_DIR/scripts/remote/install-docker.sh"
  DOCKER_JUST_INSTALLED=1
fi

# Check if user can run docker without sudo
# If not, we'll use sudo for docker commands in this session
DOCKER_CMD="docker"
if ! docker ps >/dev/null 2>&1; then
  if [ "$DOCKER_JUST_INSTALLED" -eq 1 ]; then
    log "Docker just installed - using sudo for this session"
    log "(New SSH sessions will have docker access without sudo)"
    DOCKER_CMD="sudo docker"
  else
    log "Warning: Cannot access Docker - trying with sudo"
    DOCKER_CMD="sudo docker"
  fi
fi

# Export DOCKER_CMD for setup-networks.sh to use
export DOCKER_CMD

# Networks (edge/back) using compose.env (creates edge-<env>, back-<env>)
log "Ensuring docker networks for $ENV..."
"$ROOT_DIR/scripts/utils/setup-networks.sh" "$ENV"

# Prepare volumes & perms
log "Preparing volumes..."
mkdir -p server/traefik
: > server/traefik/acme.json
chmod 600 server/traefik/acme.json || true

log "Done. You can now push infra and run compose on the remote host."

