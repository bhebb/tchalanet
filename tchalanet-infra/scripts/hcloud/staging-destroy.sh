#!/usr/bin/env bash
# Détruit le serveur staging Hetzner avec confirmation obligatoire.
# Effectue un backup PostgreSQL automatique avant destruction (sauf SKIP_BACKUP=1).
# Usage: ./staging-destroy.sh
set -euo pipefail

SERVER="${HCLOUD_SERVER_NAME:-stg-app}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

command -v hcloud >/dev/null 2>&1 || { echo "❌ hcloud CLI non trouvé" >&2; exit 1; }

echo "⚠️  ATTENTION: Cette commande va détruire le serveur staging '$SERVER'"
echo "   Tous les volumes et données seront perdus de manière irréversible."
echo "   La prod n'est JAMAIS détruite par ce script."
echo ""
read -r -p "Tape 'destroy staging' pour confirmer: " CONFIRM
if [ "$CONFIRM" != "destroy staging" ]; then
  echo "Annulé."
  exit 1
fi

# Backup PostgreSQL avant destruction
if [ "${SKIP_BACKUP:-0}" != "1" ]; then
  echo "→ Backup PostgreSQL avant destruction..."
  IP=$(hcloud server describe "$SERVER" -o json 2>/dev/null | jq -r '.public_net.ipv4.ip' || echo "")
  if [ -n "$IP" ] && [ "$IP" != "null" ]; then
    BACKUP_DIR="$ROOT/backups/staging"
    mkdir -p "$BACKUP_DIR"
    TIMESTAMP=$(date +%Y%m%d-%H%M%S)
    BACKUP_FILE="$BACKUP_DIR/staging-pg-pre-destroy-$TIMESTAMP.sql.gz"
    # Le backup pré-destruction délègue à pg-backup.sh, qui lit les credentials
    # depuis le conteneur et vérifie le dump par restauration réelle. L'ancienne
    # version appelait `pg_dumpall -U postgres` alors que le superuser est
    # `admin`, et ne testait que `[ -s ]` — un gzip de message d'erreur passait
    # le contrôle, laissant croire la destruction protégée par un backup vide.
    if ssh -i ~/.ssh/tchalanet_stg -o StrictHostKeyChecking=no "tch@$IP" \
      'cd /opt/tchalanet-infra && ENV=staging ./scripts/remote/pg-backup.sh'; then
      echo "✅ Backup vérifié et poussé vers R2"
      rm -f "$BACKUP_FILE"
    else
      rm -f "$BACKUP_FILE"
      echo "❌ Le backup pré-destruction a échoué." >&2
      echo "   Relancer avec SKIP_BACKUP=1 pour détruire malgré tout." >&2
      exit 1
    fi
  else
    echo "⚠️  Serveur inaccessible — backup ignoré"
  fi
else
  echo "⚠️  SKIP_BACKUP=1 — backup ignoré"
fi

echo "→ Suppression du serveur '$SERVER'..."
hcloud server delete "$SERVER" 2>/dev/null && echo "✅ Serveur supprimé" || echo "⚠️  Serveur déjà absent ou erreur de suppression"

echo ""
echo "✅ Staging détruit."
echo "   Pour recréer: make staging-create"
