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
Usage: $(basename "$0") [options]

Create and configure Hetzner Cloud firewall for Tchalanet infrastructure.

Options:
  -n, --name NAME      Firewall name (default: tch-fw)
  -s, --server NAME    Server to attach firewall to (optional)
  -a, --admin-ips IPS  Comma-separated admin source IPs (default: 0.0.0.0/0)
      --dry-run       Show what would be done without executing
  -h, --help           Show this help

Environment:
  HCLOUD_TOKEN        Hetzner Cloud API token (required)

Example:
  $(basename "$0") --name prod-fw --server prod-app --admin-ips "203.0.113.5,198.51.100.7"
EOF
  exit "${1:-0}"
}

# Validation du token Hetzner
: "${HCLOUD_TOKEN:?HCLOUD_TOKEN not set. Please export HCLOUD_TOKEN=your-token}"

# Valeurs par défaut
FW_NAME="tch-fw"
SERVER_NAME=""
DRY_RUN=0
ADMIN_IPS="0.0.0.0/0"

# Traitement des arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    -n|--name) FW_NAME="$2"; shift 2 ;;
    -s|--server) SERVER_NAME="$2"; shift 2 ;;
    -a|--admin-ips) ADMIN_IPS="$2"; shift 2 ;;
    --dry-run) DRY_RUN=1; shift ;;
    -h|--help) usage 0 ;;
    *) error "Unknown option: $1"; usage 1 ;;
  esac
done

if [[ "$ADMIN_IPS" == "0.0.0.0/0" ]]; then
  warn "ADMIN_IPS not provided; defaulting to 0.0.0.0/0 (open). Consider restricting to your IPs for SSH access."
fi

# Vérifier si le firewall existe déjà
if ! hcloud firewall describe "$FW_NAME" &>/dev/null; then
  log "Creating firewall '$FW_NAME'..."

  # Construire la commande de création
  CREATE_CMD=(hcloud firewall create --name "$FW_NAME")

  if [[ $DRY_RUN -eq 1 ]]; then
    log "Would execute:"
    echo "${CREATE_CMD[*]}"
  else
    if ! "${CREATE_CMD[@]}"; then
      error "Failed to create firewall"
      exit 1
    fi
    success "Firewall created successfully"
  fi
else
  success "Firewall '$FW_NAME' already exists"
fi

# Construire la liste des règles; ajouter SSH, HTTP, HTTPS avec les IPs administratives
IFS=',' read -r -a ADMIN_ARRAY <<< "$ADMIN_IPS"

RULES=()
for ip in "${ADMIN_ARRAY[@]}"; do
  RULES+=("--direction in --protocol tcp --port 22 --source-ips $ip")
done
RULES+=("--direction in --protocol tcp --port 80 --source-ips 0.0.0.0/0,::/0")
RULES+=("--direction in --protocol tcp --port 443 --source-ips 0.0.0.0/0,::/0")

# Fonction pour ajouter une règle si elle n'existe pas (meilleur effort)
add_firewall_rule() {
  local fw="$1" rule_args="$2"
  # Extraction du port en utilisant sed (compatible macOS)
  local port=$(echo "$rule_args" | sed -n 's/.*--port \([0-9]*\).*/\1/p')
  local proto=$(echo "$rule_args" | sed -n 's/.*--protocol \([a-z]*\).*/\1/p')

  # Vérifier si une règle similaire existe déjà
  if [[ -n "$port" ]] && hcloud firewall describe "$fw" -o json 2>/dev/null | grep -q "\"port\": \"$port\""; then
    log "Rule for port $port already exists on $fw; skipping"
    return 0
  fi

  if [[ $DRY_RUN -eq 1 ]]; then
    log "Would execute: hcloud firewall add-rule $fw $rule_args"
    return 0
  fi

  # Exécuter la commande sans eval pour éviter les problèmes de parsing
  if ! hcloud firewall add-rule $fw $rule_args 2>&1; then
    warn "Failed to add rule: $rule_args (may already exist)"
    return 0  # Continue même si échec (peut être un duplicate)
  fi
  success "Rule added: $rule_args"
}

# Ajouter les règles
for r in "${RULES[@]}"; do
  add_firewall_rule "$FW_NAME" "$r"
done

# Appliquer le firewall au serveur si spécifié
if [[ -n "$SERVER_NAME" ]]; then
  # Vérifier que le serveur existe
  if ! hcloud server describe "$SERVER_NAME" &>/dev/null; then
    error "Server '$SERVER_NAME' not found"
    exit 1
  fi

  log "Applying firewall to server '$SERVER_NAME'..."
  APPLY_CMD=(hcloud firewall apply-to-resource "$FW_NAME" --type server --server "$SERVER_NAME")

  if [[ $DRY_RUN -eq 1 ]]; then
    log "Would execute:"
    echo "${APPLY_CMD[*]}"
  else
    if ! "${APPLY_CMD[@]}"; then
      error "Failed to apply firewall to server"
      exit 1
    fi
    success "Firewall applied to server successfully"
  fi
fi

# Afficher la configuration finale
if [[ $DRY_RUN -eq 0 ]]; then
  log "Firewall configuration:"
  hcloud firewall describe "$FW_NAME"
fi
