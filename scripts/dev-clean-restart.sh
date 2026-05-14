#!/usr/bin/env bash
# scripts/dev-clean-restart.sh
# Nettoie le cache Vite et redémarre le dev server proprement

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo "=========================================="
echo "  Clean & Restart Dev Server"
echo "=========================================="
echo

# 1. Arrêter les processus Vite existants
echo "→ Step 1: Stopping existing Vite processes..."
pkill -f "vite.*tchalanet-portal" || echo "  No Vite processes found"
echo

# 2. Nettoyer le cache Vite
echo "→ Step 2: Cleaning Vite cache..."
rm -rf "$ROOT/tchalanet-web/node_modules/.vite"
echo "✔ Cache cleaned"
echo

# 3. Nettoyer le dist si présent
echo "→ Step 3: Cleaning dist..."
rm -rf "$ROOT/tchalanet-web/dist/apps/tchalanet-portal"
echo "✔ Dist cleaned"
echo

# 4. Afficher la config allowedHosts
echo "→ Step 4: Verifying vite.config.mts..."
if grep -q "allowedHosts" "$ROOT/tchalanet-web/apps/tchalanet-portal/vite.config.mts"; then
  echo "✔ allowedHosts is configured:"
  grep -A 5 "allowedHosts" "$ROOT/tchalanet-web/apps/tchalanet-portal/vite.config.mts" | head -6
else
  echo "⚠ WARNING: allowedHosts not found in vite.config.mts!"
fi
echo

echo "=========================================="
echo "  ✔ Cleanup complete!"
echo "=========================================="
echo
echo "Now restart the dev server with:"
echo "  cd tchalanet-web && pnpm start:web"
echo
echo "Then access via:"
echo "  - http://localhost:4200"
echo "  - https://app.localtest.me (via Traefik)"
echo

