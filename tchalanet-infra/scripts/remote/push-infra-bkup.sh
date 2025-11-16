#!/usr/bin/env bash
set -euo pipefail

SERVER_HOST="${1:?Usage: $0 <server_host_or_ip> [ENV] [--no-bootstrap]}"
ENV="${2:-staging}"
NO_BOOTSTRAP=0
if [ "${3:-}" = "--no-bootstrap" ]; then
  NO_BOOTSTRAP=1
fi
REMOTE_DIR="/opt/tchalanet-infra"

# Déterminer la clé SSH à utiliser selon l'environnement
case "$ENV" in
  staging|stg)
    SSH_KEY="$HOME/.ssh/tchalanet_stg"
    ;;
  production|prod)
    SSH_KEY="$HOME/.ssh/tchalanet_prod"
    ;;
  *)
    SSH_KEY="$HOME/.ssh/tchalanet_stg"
    ;;
esac

# Vérifier que la clé existe
if [ ! -f "$SSH_KEY" ]; then
  echo "❌ SSH key not found: $SSH_KEY" >&2
  echo "   Generate it with: ssh-keygen -t ed25519 -C \"tchalanet-$ENV\" -f $SSH_KEY" >&2
  exit 1
fi

SSH_OPTS="-i $SSH_KEY -o StrictHostKeyChecking=accept-new"

# Exclude sensitive files from rsync to avoid accidental leakage
RSYNC_EXCLUDES=(
  --exclude '.git'
  --exclude '.github'
  --exclude '.DS_Store'
  --exclude 'envs/*/.secrets'
  --exclude 'envs/*/.secrets.*'
  --exclude 'traefik/acme.json'
  --exclude 'traefik/certs/*.pem'
  --exclude '*.pem'
)

echo "→ Connecting to tch@$SERVER_HOST with key $SSH_KEY (ENV=$ENV)"

ssh $SSH_OPTS tch@"$SERVER_HOST" "sudo mkdir -p $REMOTE_DIR && sudo chown -R tch:tch $REMOTE_DIR"

rsync -az --delete "${RSYNC_EXCLUDES[@]}" \
  -e "ssh $SSH_OPTS" \
  ./ tch@"$SERVER_HOST":"$REMOTE_DIR/"

# Remote preparation: prefer explicit bootstrap unless disabled
if [ "$NO_BOOTSTRAP" -eq 0 ]; then
  echo "→ Running remote bootstrap for ENV=$ENV..."
  ssh $SSH_OPTS tch@"$SERVER_HOST" "bash -lc '
    set -euo pipefail
    cd \"$REMOTE_DIR\"
    if [ -f scripts/remote/01-bootstrap.sh ]; then
      chmod +x scripts/remote/01-bootstrap.sh
      ./scripts/remote/01-bootstrap.sh \"$ENV\"
    else
      echo \"⚠️  Warning: scripts/remote/01-bootstrap.sh not found\"
      echo \"   Docker and networks will not be configured automatically\"
      echo \"   Run manually: ssh tch@$SERVER_HOST cd /opt/tchalanet-infra && ./scripts/remote/01-bootstrap.sh $ENV\"
    fi
  '"
else
  echo "→ Skipping remote bootstrap (--no-bootstrap)"
  echo "   ⚠️  Remember to run bootstrap manually if Docker is not installed:"
  echo "   ssh tch@$SERVER_HOST 'cd $REMOTE_DIR && ./scripts/remote/01-bootstrap.sh $ENV'"
fi

echo "✔ Infra pushed and prepared on $SERVER_HOST:$REMOTE_DIR (ENV=$ENV)"
