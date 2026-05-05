#!/usr/bin/env bash
# Restaure le dernier backup PostgreSQL sur le staging.
# Usage: ./staging-restore-latest.sh
set -euo pipefail

SERVER="${HCLOUD_SERVER_NAME:-stg-app}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
BACKUP_DIR="${BACKUP_DIR:-$ROOT/backups/staging}"

command -v hcloud >/dev/null 2>&1 || { echo "❌ hcloud CLI non trouvé" >&2; exit 1; }

LATEST=$(ls -t "$BACKUP_DIR"/staging-pg-*.sql.gz 2>/dev/null | head -1 || true)
if [ -z "$LATEST" ]; then
  echo "❌ Aucun backup trouvé dans $BACKUP_DIR" >&2
  exit 1
fi

IP=$(hcloud server describe "$SERVER" -o json | jq -r '.public_net.ipv4.ip')
if [ -z "$IP" ] || [ "$IP" = "null" ]; then
  echo "❌ Impossible de récupérer l'IP du serveur '$SERVER'" >&2
  exit 1
fi

echo "→ Restauration $LATEST sur staging ($IP)..."
echo "⚠️  Ceci écrase la base de données existante."
read -r -p "Confirmer? [y/N] " CONFIRM
[ "$CONFIRM" = "y" ] || { echo "Annulé."; exit 1; }

gunzip -c "$LATEST" | ssh -i ~/.ssh/tchalanet_stg -o StrictHostKeyChecking=no "tch@$IP" \
  'docker exec -i $(docker ps --filter "name=.*postgres.*" --format "{{.Names}}" | head -1) psql -U postgres'

echo "✅ Restauration OK depuis $LATEST"
