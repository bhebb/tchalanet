#!/usr/bin/env bash
set -euo pipefail

# generate-staging-certs.sh - Génère des certificats auto-signés pour staging
# Usage: ./scripts/utils/generate-staging-certs.sh [IP]

IP="${1:-91.98.194.162}"
CERT_DIR="$(cd "$(dirname "$0")/../.." && pwd)/traefik/certs"

mkdir -p "$CERT_DIR"

echo "→ Génération de certificats auto-signés pour staging..."
echo "  IP: $IP"
echo "  Domaines: *.stg.tchalanet.com"

# Générer la clé privée
openssl genrsa -out "$CERT_DIR/staging-key.pem" 2048

# Générer le certificat (valide 365 jours)
openssl req -new -x509 \
  -key "$CERT_DIR/staging-key.pem" \
  -out "$CERT_DIR/staging-cert.pem" \
  -days 365 \
  -subj "/C=CA/ST=Quebec/L=Montreal/O=Tchalanet/OU=Staging/CN=*.stg.tchalanet.com" \
  -addext "subjectAltName=DNS:*.stg.tchalanet.com,DNS:stg.tchalanet.com,IP:$IP"

# Permissions
chmod 600 "$CERT_DIR/staging-key.pem"
chmod 644 "$CERT_DIR/staging-cert.pem"

echo ""
echo "✅ Certificats générés :"
echo "   Certificat: $CERT_DIR/staging-cert.pem"
echo "   Clé privée: $CERT_DIR/staging-key.pem"
echo ""
echo "⚠️  Ce sont des certificats auto-signés."
echo "   Les navigateurs afficheront un avertissement de sécurité."
echo "   Pour production, utiliser Let's Encrypt."

