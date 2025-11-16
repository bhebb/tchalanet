#!/usr/bin/env bash
set -euo pipefail

# Couleurs pour les messages
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Fonctions pour les messages
log() { echo -e "${BLUE}→${NC} $*" >&2; }
success() { echo -e "${GREEN}✔${NC} $*" >&2; }
error() { echo -e "${RED}✖${NC} $*" >&2; }
warn() { echo -e "${YELLOW}!${NC} $*" >&2; }

usage() {
  cat <<EOF
Usage: $(basename "$0") [NET_NAME] [CIDR] [ZONE]

Create a private network in Hetzner Cloud.

Arguments:
  NET_NAME    Network name (default: tch-net)
  CIDR        Network CIDR (default: 10.10.0.0/16)
  ZONE        Network zone (default: eu-central)
              Valid zones: eu-central | eu-west

Environment:
  HCLOUD_TOKEN    Hetzner Cloud API token (required)

Example:
  $(basename "$0") prod-net 10.20.0.0/16 eu-west
EOF
  exit "${1:-0}"
}

# Validation du token Hetzner
: "${HCLOUD_TOKEN:?HCLOUD_TOKEN not set. Please export HCLOUD_TOKEN=your-token}"

# Arguments avec valeurs par défaut
NET_NAME="${1:-tch-net}"
CIDR="${2:-10.10.0.0/16}"
ZONE="${3:-eu-central}"

# Validation de la zone
case "$ZONE" in
  eu-central|eu-west) ;;
  *) error "Invalid zone: $ZONE. Must be eu-central or eu-west"; usage 1 ;;
esac

# Validation du CIDR
if ! [[ $CIDR =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+/[0-9]+$ ]]; then
  error "Invalid CIDR format: $CIDR"
  usage 1
fi

# Validation du nom (pas de caractères spéciaux sauf - et _)
if ! [[ $NET_NAME =~ ^[a-zA-Z0-9_-]+$ ]]; then
  error "Invalid network name: $NET_NAME (use only letters, numbers, - and _)"
  usage 1
fi

# Vérification si le réseau existe
if hcloud network describe "$NET_NAME" &>/dev/null; then
  success "Network '$NET_NAME' already exists"

  # Vérifier si le sous-réseau existe dans la zone
  if hcloud network describe "$NET_NAME" -o format='{{range .Subnets}}{{.NetworkZone}}{{"\n"}}{{end}}' | grep -q "^${ZONE}$"; then
    success "Subnet in zone $ZONE already exists"
  else
    log "Adding subnet (type: server) in zone $ZONE"
    if ! hcloud network add-subnet "$NET_NAME" --type server --network-zone "$ZONE" --ip-range "$CIDR"; then
      error "Failed to add subnet"
      exit 1
    fi
    success "Subnet added successfully"
  fi
else
  log "Creating network '$NET_NAME' ($CIDR)"
  if ! hcloud network create --name "$NET_NAME" --ip-range "$CIDR"; then
    error "Failed to create network"
    exit 1
  fi
  success "Network created successfully"

  log "Adding subnet (type: server) in zone $ZONE"
  if ! hcloud network add-subnet "$NET_NAME" --type server --network-zone "$ZONE" --ip-range "$CIDR"; then
    error "Failed to add subnet"
    error "Cleaning up - deleting network"
    hcloud network delete "$NET_NAME"
    exit 1
  fi
  success "Subnet added successfully"
fi

log "Network details:"
hcloud network describe "$NET_NAME"
