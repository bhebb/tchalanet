#!/usr/bin/env bash
# Crée l'environnement staging Hetzner complet (réseau, firewall, serveur).
# Usage: ./staging-create.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

command -v hcloud >/dev/null 2>&1 || { echo "❌ hcloud CLI non trouvé — installe-le via: brew install hcloud" >&2; exit 1; }

create_location="${HCLOUD_LOCATION:-fsn1}"
case "$create_location" in
  ash)
    create_network_zone="${HCLOUD_NETWORK_ZONE:-us-east}"
    create_subnet_cidr="${HCLOUD_NETWORK_SUBNET_CIDR:-10.0.2.0/24}"
    ;;
  hil)
    create_network_zone="${HCLOUD_NETWORK_ZONE:-us-west}"
    create_subnet_cidr="${HCLOUD_NETWORK_SUBNET_CIDR:-10.0.3.0/24}"
    ;;
  sin)
    create_network_zone="${HCLOUD_NETWORK_ZONE:-ap-southeast}"
    create_subnet_cidr="${HCLOUD_NETWORK_SUBNET_CIDR:-10.0.4.0/24}"
    ;;
  *)
    create_network_zone="${HCLOUD_NETWORK_ZONE:-eu-central}"
    create_subnet_cidr="${HCLOUD_NETWORK_SUBNET_CIDR:-10.0.1.0/24}"
    ;;
esac

echo "→ Création réseau Hetzner..."
bash "$ROOT/scripts/hcloud/01-create-network.sh" \
  "${HCLOUD_NETWORK_NAME:-tch-net}" \
  "$create_subnet_cidr" \
  "$create_network_zone"

echo "→ Création firewall..."
bash "$ROOT/scripts/hcloud/02-create-firewall.sh"

if hcloud server describe "${HCLOUD_SERVER_NAME:-stg-app}" >/dev/null 2>&1; then
  echo "✔ Serveur '${HCLOUD_SERVER_NAME:-stg-app}' existe déjà; aucune création nécessaire."
  echo ""
  echo "✅ Staging déjà présent."
  exit 0
fi

echo "→ Création serveur..."
create_log="$(mktemp /tmp/tchalanet-staging-create.XXXXXX)"
cleanup_create_log() { rm -f "$create_log"; }
trap cleanup_create_log EXIT
create_type="${HCLOUD_SERVER_TYPE:-cx23}"
if ! bash "$ROOT/scripts/hcloud/03-create-server.sh" \
  --location "$create_location" \
  --type "$create_type" \
  --network "${HCLOUD_NETWORK_NAME:-tch-net}" 2>&1 | tee "$create_log"; then
  if [ -z "${HCLOUD_LOCATION:-}" ] && [ "$create_location" = "fsn1" ] && \
    grep -Eq 'resource_unavailable|error during placement' "$create_log"; then
    echo "⚠️  fsn1 indisponible pour ce type; nouvelle tentative en nbg1..."
    bash "$ROOT/scripts/hcloud/03-create-server.sh" \
      --location nbg1 \
      --type "$create_type" \
      --network "${HCLOUD_NETWORK_NAME:-tch-net}"
  else
    exit 1
  fi
fi

echo ""
echo "✅ Staging créé."
echo ""
echo "Étapes suivantes :"
echo "  1. Configurer le DNS : faire pointer *.stg.tchalanet.com → IP du serveur"
echo "  2. make up-staging"
echo "  3. make smoke-staging"
