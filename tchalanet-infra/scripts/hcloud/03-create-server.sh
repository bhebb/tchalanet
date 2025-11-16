#!/usr/bin/env bash
set -euo pipefail

# Couleurs pour les messages
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Fonction d'aide
usage() {
  cat <<EOF
Usage: $(basename "$0") [options]

Create a Hetzner Cloud server for Tchalanet infrastructure.

Options:
  -n, --name NAME        Server name (default: stg-app)
  -t, --type TYPE        Server type (default: cpx21)
  -i, --image IMAGE      OS image (default: ubuntu-24.04)
  -f, --firewall NAME    Firewall name (default: tch-fw)
  -k, --ssh-key NAME     SSH key name (default: tchalanet_stg)
  -l, --location LOC     Location (default: nbg1)
      --network NET     Private network name (optional)
      --public-ip IP    Use reserved public IP (optional)
      --dry-run         Show what would be done without creating server
  -h, --help            Show this help

Environment variables:
  HCLOUD_TOKEN          Hetzner Cloud API token (required)

Example:
  $(basename "$0") --name prod-app --type cpx31 --ssh-key tchalanet_prod --network tch-net --public-ip tch-pub-ip
EOF
  exit "${1:-0}"
}

# Fonction pour les messages
log() { echo -e "${BLUE}[INFO]${NC} $*" >&2; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*" >&2; }
success() { echo -e "${GREEN}[OK]${NC} $*" >&2; }
error() { echo -e "${RED}[ERROR]${NC} $*" >&2; }

# Validation de la présence de hcloud
if ! command -v hcloud >/dev/null; then
  error "hcloud CLI not found. Please install it first:"
  error "brew install hcloud-cli # macOS"
  error "apt install hcloud-cli # Ubuntu"
  exit 1
fi

# Validation du token Hetzner
: "${HCLOUD_TOKEN:?HCLOUD_TOKEN not set. Please export HCLOUD_TOKEN=your-token}"

# Valeurs par défaut
NAME="stg-app"
TYPE="cx23"
IMAGE="ubuntu-24.04"
FIREWALL="tch-fw"
SSH_KEY="tchalanet_stg"
LOCATION="nbg1"
NETWORK="" # Optionnel
PUBLIC_IP="" # Optionnel
DRY_RUN=0

# Traitement des arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    -n|--name) NAME="$2"; shift 2 ;;
    -t|--type) TYPE="$2"; shift 2 ;;
    -i|--image) IMAGE="$2"; shift 2 ;;
    -f|--firewall) FIREWALL="$2"; shift 2 ;;
    -k|--ssh-key) SSH_KEY="$2"; shift 2 ;;
    -l|--location) LOCATION="$2"; shift 2 ;;
    --network) NETWORK="$2"; shift 2 ;;
    --public-ip) PUBLIC_IP="$2"; shift 2 ;;
    --dry-run) DRY_RUN=1; shift ;;
    -h|--help) usage 0 ;;
    *) error "Unknown option: $1"; usage 1 ;;
  esac
done

# Fonction pour valider les paramètres hcloud
validate_hcloud_param() {
  local param=$1
  local value=$2
  local cmd=$3

  if ! hcloud "$cmd" describe "$value" &>/dev/null; then
    if [[ $DRY_RUN -eq 1 ]]; then
      warn "Would fail: Invalid $param: $value"
      return 0
    else
      error "Invalid $param: $value"
      echo "Available ${param}s:"
      hcloud "$cmd" list
      exit 1
    fi
  fi
}

# Validation des paramètres avec listes disponibles
validate_hcloud_param "server type" "$TYPE" "server-type"
validate_hcloud_param "image" "$IMAGE" "image"
validate_hcloud_param "firewall" "$FIREWALL" "firewall"
validate_hcloud_param "SSH key" "$SSH_KEY" "ssh-key"

# Vérifier si le nom de serveur est déjà utilisé
if hcloud server describe "$NAME" &>/dev/null; then
  error "Server name '$NAME' already exists"
  exit 1
fi

# Vérifier la localisation
if ! hcloud location describe "$LOCATION" &>/dev/null; then
  error "Invalid location: $LOCATION"
  echo "Available locations:"
  hcloud location list
  exit 1
fi

# Valider le réseau si spécifié
if [[ -n "$NETWORK" ]] && ! hcloud network describe "$NETWORK" &>/dev/null; then
  error "Network '$NETWORK' not found"
  echo "Available networks:"
  hcloud network list
  exit 1
fi

# Valider l'IP publique si spécifiée
if [[ -n "$PUBLIC_IP" ]] && ! hcloud primary-ip describe "$PUBLIC_IP" &>/dev/null; then
  error "Primary IP '$PUBLIC_IP' not found"
  echo "Available primary IPs:"
  hcloud primary-ip list
  exit 1
fi

# Construire la commande
CMD=(hcloud server create
  --name "$NAME"
  --type "$TYPE"
  --image "$IMAGE"
  --location "$LOCATION"
  --firewall "$FIREWALL"
  --ssh-key "$SSH_KEY"
  --label "env=${NAME%%"-app"}"
  --label "app=tchalanet")

# Ajouter le réseau si spécifié
if [[ -n "$NETWORK" ]]; then
  CMD+=(--network "$NETWORK")
fi

# Ajouter l'IP publique si spécifiée
if [[ -n "$PUBLIC_IP" ]]; then
  CMD+=(--primary-ipv4 "$PUBLIC_IP")
fi

# Vérifier si cloud-init.yml existe dans le répertoire du script
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CLOUD_INIT_FILE="$SCRIPT_DIR/cloud-init.yml"

if [[ -f "$CLOUD_INIT_FILE" ]]; then
  CMD+=(--user-data-from-file "$CLOUD_INIT_FILE")
else
  warn "cloud-init.yml not found at $CLOUD_INIT_FILE, will generate it"
fi

# Mode dry-run
if [[ $DRY_RUN -eq 1 ]]; then
  log "Would execute:"
  echo "${CMD[*]}"
  exit 0
fi

# Générer cloud-init.yml pour l'environnement si non présent
ENV_SHORT="${NAME%%"-app"}"
# Convertir stg → staging, prod → production
case "$ENV_SHORT" in
  stg) ENV_FULL="staging" ;;
  prod) ENV_FULL="production" ;;
  staging|production) ENV_FULL="$ENV_SHORT" ;;
  *) ENV_FULL="staging" ;; # default
esac

if [[ ! -f "$CLOUD_INIT_FILE" ]]; then
  log "Generating cloud-init.yml for $ENV_FULL..."
  if ! "$SCRIPT_DIR/04-generate-cloud-init.sh" "$ENV_FULL"; then
    error "Failed to generate cloud-init.yml"
    exit 1
  fi
  # Ajouter le fichier généré à la commande
  CMD+=(--user-data-from-file "$CLOUD_INIT_FILE")
fi

log "Creating server $NAME..."
log "Type: $TYPE, Image: $IMAGE, Location: $LOCATION"

# Création du serveur
if ! "${CMD[@]}"; then
  error "Failed to create server"
  exit 1
fi

# Récupérer les informations du serveur créé
SERVER_INFO=$(hcloud server describe "$NAME" --output json)

# Extraire l'IP et d'autres informations utiles
SERVER_IP=$(echo "$SERVER_INFO" | jq -r '.public_net.ipv4.ip')
SERVER_ID=$(echo "$SERVER_INFO" | jq -r '.id')
SERVER_STATUS=$(echo "$SERVER_INFO" | jq -r '.status')

success "Server created successfully!"
log "Server ID: $SERVER_ID"
log "Server IP: $SERVER_IP"
log "Initial status: $SERVER_STATUS"

# --- Nouvel ajout: préparation tests SSH et refresh host key ---
KEY_PATH="$HOME/.ssh/${SSH_KEY}"
HOST_REFRESH_SCRIPT="$(cd "${SCRIPT_DIR}/../utils" && pwd)/ssh-host-refresh.sh"
if [[ -x "$HOST_REFRESH_SCRIPT" ]]; then
  log "Refreshing SSH host key (known_hosts cleanup + keyscan) for $SERVER_IP"
  "$HOST_REFRESH_SCRIPT" "$SERVER_IP" "tchalanet_${ENV_SHORT}" || warn "Host key refresh script failed (non fatal)"
else
  warn "ssh-host-refresh.sh not found or not executable; skipping host key refresh"
fi

root_ok=0
user_ok=0
log "Probing early SSH connectivity (root vs tch)..."
if ssh -i "$KEY_PATH" -o StrictHostKeyChecking=accept-new -o BatchMode=yes -o ConnectTimeout=5 root@"$SERVER_IP" 'echo root-ok' &>/dev/null; then
  root_ok=1; log "Early SSH root@${SERVER_IP} OK"
fi
if ssh -i "$KEY_PATH" -o StrictHostKeyChecking=accept-new -o BatchMode=yes -o ConnectTimeout=5 tch@"$SERVER_IP" 'echo tch-ok' &>/dev/null; then
  user_ok=1; log "Early SSH tch@${SERVER_IP} OK"
fi
if [[ $root_ok -eq 1 && $user_ok -eq 0 ]]; then
  warn "User 'tch' not yet available; cloud-init probablement en cours. Fallback temporaire: utilisez root@${SERVER_IP} pour inspection."
fi

# Attendre que le serveur soit prêt (SSH accessible)
log "Waiting for SSH to be available..."
TIMEOUT=300  # 5 minutes
ELAPSED=0
while ! timeout 2 nc -z "$SERVER_IP" 22 &>/dev/null; do
  echo -n "."
  sleep 2
  ELAPSED=$((ELAPSED + 2))
  if [ $ELAPSED -ge $TIMEOUT ]; then
    error "Timeout waiting for SSH to be ready"
    error "Check server status:"
    error "  hcloud server describe $NAME"
    exit 1
  fi
done
echo ""
success "SSH is available!"

# --- Attente cloud-init & re-test utilisateur tch ---
# Attendre que cloud-init soit terminé (avec timeout réduit)
log "Waiting for cloud-init to complete (this may take 2-3 minutes)..."
CLOUD_INIT_TIMEOUT=180  # 3 minutes
CLOUD_INIT_ELAPSED=0
CLOUD_INIT_OK=false

while [ $CLOUD_INIT_ELAPSED -lt $CLOUD_INIT_TIMEOUT ]; do
  # Vérifier le statut de cloud-init via SSH (BatchMode pour éviter les prompts)
  if ssh -i ~/.ssh/${SSH_KEY} -o StrictHostKeyChecking=accept-new -o ConnectTimeout=5 -o BatchMode=yes tch@${SERVER_IP} "cloud-init status --wait" &>/dev/null; then
    CLOUD_INIT_OK=true
    break
  fi

  echo -n "."
  sleep 5
  CLOUD_INIT_ELAPSED=$((CLOUD_INIT_ELAPSED + 5))
done
echo ""

if [ "$CLOUD_INIT_OK" = true ]; then
  success "Cloud-init completed successfully!"
else
  warn "Could not verify cloud-init completion (timeout after ${CLOUD_INIT_TIMEOUT}s)"
  warn "The server is accessible but cloud-init may still be running."
  warn "Check status manually with:"
  warn "  ssh -i ~/.ssh/${SSH_KEY} tch@${SERVER_IP} 'cloud-init status --wait'"
  warn ""
  warn "Continuing anyway - the server should be usable in a few minutes..."
fi

# Re-test après cloud-init
if [[ $user_ok -eq 0 ]]; then
  log "Re-testing SSH for user 'tch' after cloud-init cycle..."
  if ssh -i "$KEY_PATH" -o StrictHostKeyChecking=accept-new -o BatchMode=yes -o ConnectTimeout=6 tch@"$SERVER_IP" 'echo tch-ready' &>/dev/null; then
    user_ok=1; success "User 'tch' maintenant accessible via SSH"
  else
    warn "User 'tch' toujours indisponible via SSH. Étapes de diagnostic proposées:";
    echo "  1) Connexion root: ssh -i $KEY_PATH root@$SERVER_IP";
    echo "  2) Vérifier cloud-init: sudo tail -200 /var/log/cloud-init.log";
    echo "  3) Vérifier présence /home/tch: ls -ld /home/tch";
    echo "  4) Créer manuellement clé si absente: sudo mkdir -p /home/tch/.ssh; sudo cp /root/.ssh/authorized_keys /home/tch/.ssh/; sudo chown -R tch:tch /home/tch/.ssh";
    echo "  5) Relancer cloud-init status: cloud-init status --wait";
  fi
fi

success "Server is ready and accessible via SSH!"
cat <<EOF
You can now connect to your server:
  ssh -i ~/.ssh/${SSH_KEY} tch@${SERVER_IP}
  # or simply (if ~/.ssh/config is configured): ssh tchalanet_${ENV_SHORT}

To check cloud-init status:
  ssh -i ~/.ssh/${SSH_KEY} tch@${SERVER_IP} cloud-init status

DNS Configuration (Cloudflare):
  Add A record for ${ENV_SHORT}.tchalanet.com pointing to ${SERVER_IP}

Next steps:
  1. Push infra: cd tchalanet-infra && ./scripts/remote/push-infra-bkup.sh ${SERVER_IP} ${ENV_FULL}
  2. Bootstrap: ssh tch@${SERVER_IP} 'cd /opt/tchalanet-infra && ./scripts/remote/01-bootstrap.sh ${ENV_FULL}'
  3. Deploy: make deploy-${ENV_SHORT}

To delete this server:
  hcloud server delete $NAME

Server details:
  hcloud server describe $NAME
EOF
