#!/usr/bin/env bash
# Crée l'environnement staging Hetzner complet (réseau, firewall, serveur).
# Usage: ./staging-create.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

command -v hcloud >/dev/null 2>&1 || { echo "❌ hcloud CLI non trouvé — installe-le via: brew install hcloud" >&2; exit 1; }

echo "→ Création réseau Hetzner..."
bash "$ROOT/scripts/hcloud/01-create-network.sh"

echo "→ Création firewall..."
bash "$ROOT/scripts/hcloud/02-create-firewall.sh"

echo "→ Création serveur..."
bash "$ROOT/scripts/hcloud/03-create-server.sh"

echo ""
echo "✅ Staging créé."
echo ""
echo "Étapes suivantes :"
echo "  1. Configurer le DNS : faire pointer *.stg.tchalanet.com → IP du serveur"
echo "  2. make up-staging"
echo "  3. make smoke-staging"
