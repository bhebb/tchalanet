#!/usr/bin/env bash
# Backup PostgreSQL du staging vers ./backups/staging/.
# Usage: ./staging-backup.sh
set -euo pipefail

SERVER="${HCLOUD_SERVER_NAME:-stg-app}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
BACKUP_DIR="${BACKUP_DIR:-$ROOT/backups/staging}"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)

command -v hcloud >/dev/null 2>&1 || { echo "❌ hcloud CLI non trouvé" >&2; exit 1; }
command -v jq >/dev/null 2>&1 || { echo "❌ jq non trouvé" >&2; exit 1; }

mkdir -p "$BACKUP_DIR"

IP=$(hcloud server describe "$SERVER" -o json | jq -r '.public_net.ipv4.ip')
if [ -z "$IP" ] || [ "$IP" = "null" ]; then
  echo "❌ Impossible de récupérer l'IP du serveur '$SERVER'" >&2
  exit 1
fi

BACKUP_FILE="$BACKUP_DIR/staging-pg-$TIMESTAMP.sql.gz"
echo "→ Backup PostgreSQL staging ($IP) → $BACKUP_FILE"

ssh -i ~/.ssh/tchalanet_stg -o StrictHostKeyChecking=no "tch@$IP" \
  'docker exec $(docker ps --filter "name=.*postgres.*" --format "{{.Names}}" | head -1) pg_dumpall -U postgres | gzip' \
  > "$BACKUP_FILE"

SIZE=$(du -sh "$BACKUP_FILE" | cut -f1)
echo "✅ Backup: $BACKUP_FILE ($SIZE)"
