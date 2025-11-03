#!/usr/bin/env bash
set -euo pipefail

# download-doppler-secrets.sh - Télécharge les secrets Doppler pour un environnement
# Usage: ./scripts/doppler/download-doppler-secrets.sh <env> <doppler_token>

ENV="${1:?Usage: $0 <env> <doppler_token>}"
DOPPLER_TOKEN="${2:?Doppler token required}"

# Mapping env vers config Doppler
case "$ENV" in
  staging|stg) DOPPLER_CONFIG="stg" ;;
  production|prod|prd) DOPPLER_CONFIG="prd" ;;
  dev) DOPPLER_CONFIG="dev" ;;
  *) echo "Unknown env: $ENV. Use dev, staging, or production" >&2; exit 1 ;;
esac

SECRETS_FILE="envs/${ENV}/.secrets"

echo "→ Téléchargement des secrets depuis Doppler..."
echo "  Project: tchalanet"
echo "  Config: $DOPPLER_CONFIG"
echo "  Output: $SECRETS_FILE"
echo ""

# Vérifier que doppler CLI est disponible (via Docker ou local)
if command -v doppler >/dev/null 2>&1; then
  # Doppler CLI installé localement
  echo "→ Utilisation de Doppler CLI local..."
  DOPPLER_TOKEN="$DOPPLER_TOKEN" doppler secrets download \
    --project tchalanet \
    --config "$DOPPLER_CONFIG" \
    --format env \
    --no-file \
    > "$SECRETS_FILE"
else
  # Utilisation via Docker
  echo "→ Utilisation de Doppler CLI via Docker..."
  # Use --entrypoint doppler to avoid the Docker entrypoint parsing flags differently
  docker run --rm \
    -e DOPPLER_TOKEN="$DOPPLER_TOKEN" \
    -v "$PWD":/work \
    -w /work \
    --entrypoint doppler \
    dopplerhq/cli:latest \
    secrets download \
      --project tchalanet \
      --config "${DOPPLER_CONFIG}" \
      --format env \
      --no-file \
    > "$SECRETS_FILE"
fi

# Vérifier que le fichier a été créé
if [ ! -f "$SECRETS_FILE" ]; then
  echo "❌ Erreur: Fichier $SECRETS_FILE non créé" >&2
  exit 1
fi

# Définir les permissions
chmod 600 "$SECRETS_FILE"

# Compter les secrets
SECRET_COUNT=$(grep -c "^[A-Z_]*=" "$SECRETS_FILE" || echo 0)

echo ""
echo "✅ Secrets téléchargés avec succès !"
echo "   Fichier: $SECRETS_FILE"
echo "   Permissions: 600"
echo "   Nombre de secrets: $SECRET_COUNT"
echo ""
echo "Prochaine étape:"
echo "  make env-merge ENV=$ENV"
