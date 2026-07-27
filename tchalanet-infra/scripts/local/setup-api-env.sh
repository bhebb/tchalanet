#!/usr/bin/env bash
set -euo pipefail

# setup-api-env.sh
# Copie et configure le fichier .env pour l'API avec les bonnes valeurs depuis l'infra

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
API_ROOT="$(cd "$INFRA_ROOT/../tchalanet-server" && pwd)"

ENV="${1:-dev}"

# Couleurs
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() { echo -e "${BLUE}→${NC} $*"; }
success() { echo -e "${GREEN}✔${NC} $*"; }
warn() { echo -e "${YELLOW}⚠${NC} $*"; }

log "Configuration de l'API Spring Boot pour l'environnement: $ENV"

# Vérifier que le dossier API existe
if [ ! -d "$API_ROOT" ]; then
  warn "Dossier API introuvable: $API_ROOT"
  echo "Assure-toi que tchalanet-server est au même niveau que tchalanet-infra"
  exit 1
fi

# Lire les secrets de l'infra
SECRETS_FILE="$INFRA_ROOT/envs/$ENV/.secrets"
if [ ! -f "$SECRETS_FILE" ]; then
  warn "Fichier secrets introuvable: $SECRETS_FILE"
  echo "Exécute d'abord: make env-merge ENV=$ENV"
  exit 1
fi

# Charger les secrets
set -a
source "$SECRETS_FILE"
set +a

# local-ide is the real-Firebase IDE runtime. Docker dev/E2E keeps its own
# emulator configuration in envs/dev/compose.env and does not use this file.
IDENTITY_PROVIDER="${TCH_IDENTITY_PROVIDER:-firebase}"
FIREBASE_PROJECT="${FIREBASE_PROJECT_ID:-tchalanet}"
LOCAL_FIREBASE_DIR="$API_ROOT/.firebase"
LOCAL_FIREBASE_CREDENTIALS="$LOCAL_FIREBASE_DIR/firebase-admin.json"

if [ "$IDENTITY_PROVIDER" = "firebase" ]; then
  mkdir -p "$LOCAL_FIREBASE_DIR"
  umask 077

  if [ -n "${FIREBASE_ADMIN_JSON:-}" ]; then
    printf '%s' "$FIREBASE_ADMIN_JSON" > "$LOCAL_FIREBASE_CREDENTIALS"
  elif [ -n "${FIREBASE_ADMIN_JSON_BASE64:-}" ]; then
    printf '%s' "$FIREBASE_ADMIN_JSON_BASE64" | base64 -d > "$LOCAL_FIREBASE_CREDENTIALS"
  elif [ -n "${FIREBASE_CREDENTIALS_PATH:-}" ] && [ -f "$FIREBASE_CREDENTIALS_PATH" ]; then
    cp "$FIREBASE_CREDENTIALS_PATH" "$LOCAL_FIREBASE_CREDENTIALS"
  fi

  if [ -f "$LOCAL_FIREBASE_CREDENTIALS" ]; then
    if ! grep -q '"private_key"' "$LOCAL_FIREBASE_CREDENTIALS" \
      || ! grep -q '"client_email"' "$LOCAL_FIREBASE_CREDENTIALS"; then
      rm -f "$LOCAL_FIREBASE_CREDENTIALS"
      echo "Credential Firebase invalide dans $LOCAL_FIREBASE_CREDENTIALS" >&2
      exit 1
    fi
    credential_project="$(jq -r '.project_id // empty' "$LOCAL_FIREBASE_CREDENTIALS" 2>/dev/null || true)"
    if [ "$credential_project" != "$FIREBASE_PROJECT" ]; then
      rm -f "$LOCAL_FIREBASE_CREDENTIALS"
      echo "Credential Firebase project=$credential_project, attendu=$FIREBASE_PROJECT" >&2
      exit 1
    fi
    FIREBASE_CREDENTIALS="${LOCAL_FIREBASE_CREDENTIALS}"
  else
    FIREBASE_CREDENTIALS="${FIREBASE_CREDENTIALS_PATH:-}"
    warn "Credential Firebase local absent; configure FIREBASE_ADMIN_JSON_BASE64 dans Doppler dev"
  fi
else
  FIREBASE_CREDENTIALS=""
fi

# Créer le fichier .env pour l'API
API_ENV_FILE="$API_ROOT/.env"

log "Création de $API_ENV_FILE"

cat > "$API_ENV_FILE" <<EOF
# ========================================
# Tchalanet API Spring Boot - Configuration $ENV
# ========================================
# Généré automatiquement le $(date)
# IMPORTANT : Ce fichier ne doit PAS être versionné

# ========================================
# 🗄️ Postgres (Database)
# ========================================
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tchalanet_db
SPRING_DATASOURCE_USERNAME=app_user
SPRING_DATASOURCE_PASSWORD=${APP_DB_PASSWORD}

# ========================================
# 🔴 Redis (Cache)
# ========================================
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=${REDIS_PASSWORD:-devredis}

# ========================================
# 🔐 Authentication
# ========================================
TCH_IDENTITY_PROVIDER=${IDENTITY_PROVIDER}
FIREBASE_PROJECT_ID=${FIREBASE_PROJECT}
FIREBASE_CREDENTIALS_PATH=${FIREBASE_CREDENTIALS}
EOF

if [ "$IDENTITY_PROVIDER" = "firebase-emulator" ]; then
  cat >> "$API_ENV_FILE" <<EOF
FIREBASE_AUTH_EMULATOR_HOST=${FIREBASE_AUTH_EMULATOR_HOST:-localhost:9099}
EOF
fi

cat >> "$API_ENV_FILE" <<EOF

# ========================================
# 📡 Edge Service
# ========================================
TCH_EDGE_BASE_URL=http://localhost:3000
TCH_EDGE_HMAC_SECRET=${EDGE_HMAC_SECRET:-tch-local-ide-edge-hmac-2026}

# ========================================
# 🔧 Application Config
# ========================================
SERVER_PORT=8083
SPRING_PROFILES_ACTIVE=local-ide
LOGGING_LEVEL_ROOT=INFO
SPRING_JPA_SHOW_SQL=true
SPRING_JPA_HIBERNATE_DDL_AUTO=validate

# ========================================
# 🌐 CORS (Frontend)
# ========================================
CORS_ALLOWED_ORIGINS=http://localhost:4200,http://localhost:4201
EOF

chmod 600 "$API_ENV_FILE"

success "Fichier .env créé pour l'API Spring Boot: $API_ENV_FILE"
echo ""
echo "Pour lancer l'API:"
echo "  cd $API_ROOT"
echo "  set -a; source .env; set +a"
echo "  ./mvnw spring-boot:run"
