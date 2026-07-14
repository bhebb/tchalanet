#!/usr/bin/env bash
set -euo pipefail

SERVER_HOST="${1:?Usage: $0 <server_host_or_ip> [ENV] [--no-bootstrap]}"
ENV="${2:-staging}"
NO_BOOTSTRAP=0
if [ "${3:-}" = "--no-bootstrap" ]; then
  NO_BOOTSTRAP=1
fi
REMOTE_DIR="/opt/tchalanet-infra"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
REPO_ROOT="$(cd "$INFRA_DIR/.." && pwd)"

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
  --exclude 'traefik/dynamic/10-routers.yaml'
  --exclude '*.pem'
)

echo "→ Connecting to tch@$SERVER_HOST with key $SSH_KEY (ENV=$ENV)"

ssh $SSH_OPTS tch@"$SERVER_HOST" "sudo mkdir -p $REMOTE_DIR && sudo chown -R tch:tch $REMOTE_DIR"

FIREBASE_ADMIN_SRC="$REPO_ROOT/tchalanet-server/tchalanet-39115-firebase-adminsdk-fbsvc-62e904a236.json"
FIREBASE_ADMIN_DST="$INFRA_DIR/server/secrets/firebase-admin.json"
if [ -f "$FIREBASE_ADMIN_SRC" ]; then
  mkdir -p "$(dirname "$FIREBASE_ADMIN_DST")"
  cp "$FIREBASE_ADMIN_SRC" "$FIREBASE_ADMIN_DST"
  chmod 600 "$FIREBASE_ADMIN_DST"
  echo "→ Firebase Admin credentials staged for server sync: server/secrets/firebase-admin.json"
elif [ -n "${FIREBASE_ADMIN_JSON_BASE64:-}" ]; then
  mkdir -p "$(dirname "$FIREBASE_ADMIN_DST")"
  printf '%s' "$FIREBASE_ADMIN_JSON_BASE64" | base64 -d > "$FIREBASE_ADMIN_DST"
  chmod 600 "$FIREBASE_ADMIN_DST"
  echo "→ Firebase Admin credentials staged from FIREBASE_ADMIN_JSON_BASE64: server/secrets/firebase-admin.json"
else
  echo "⚠️  Firebase Admin credentials source not found: $FIREBASE_ADMIN_SRC" >&2
  echo "   Set FIREBASE_ADMIN_JSON_BASE64 env var or add credentials to Doppler config." >&2
fi

rsync -az --delete "${RSYNC_EXCLUDES[@]}" \
  -e "ssh $SSH_OPTS" \
  "$INFRA_DIR/" tch@"$SERVER_HOST":"$REMOTE_DIR/"

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
