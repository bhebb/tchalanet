#!/usr/bin/env zsh
# Script: setup-mkcert-truststore.sh
# Objectif: Générer un certificat local (mkcert) pour les domaines dev et créer un truststore Java dédié.
# Usage: ./scripts/setup-mkcert-truststore.sh
# Prérequis: brew install mkcert nss

set -euo pipefail

DOMAINS=(auth.localtest.me app.localtest.me api.localtest.me)
CERT_NAME="dev-local"
TRUSTSTORE="local-truststore.jks"
STORE_PASS="changeit"

echo "[1/6] Vérification mkcert..."
if ! command -v mkcert >/dev/null 2>&1; then
  echo "mkcert n'est pas installé. Installez-le: brew install mkcert nss" >&2
  exit 1
fi

CAROOT=$(mkcert -CAROOT)
echo "CA mkcert: $CAROOT"

echo "[2/6] Génération certificat SAN pour: ${DOMAINS[*]}"
mkcert -cert-file ${CERT_NAME}.crt -key-file ${CERT_NAME}.key ${DOMAINS[@]}

echo "[3/6] Conversion en PKCS#12 (Keycloak / proxies)"
openssl pkcs12 -export -in ${CERT_NAME}.crt -inkey ${CERT_NAME}.key -out ${CERT_NAME}.p12 -name ${CERT_NAME} -password pass:${STORE_PASS}

echo "[4/6] Création / mise à jour truststore Java dédié: ${TRUSTSTORE}"
if [[ -f ${TRUSTSTORE} ]]; then
  echo "Truststore existe, tentative d'import (remplacement si alias déjà là)."
fi
# On importe la CA plutôt que le cert leaf pour éviter de regénérer le truststore à chaque changement mineur.
keytool -importcert -trustcacerts -file "$CAROOT/rootCA.pem" -alias mkcert-root -keystore ${TRUSTSTORE} -storepass ${STORE_PASS} -noprompt || true

echo "[5/6] Vérification contenu truststore"
keytool -list -keystore ${TRUSTSTORE} -storepass ${STORE_PASS} | grep mkcert-root || echo "Alias mkcert-root non trouvé (vérifiez l'import)."

cat <<EOF > .env.local-truststore
# Variables à sourcer pour exécuter l'appli avec ce truststore
export JAVA_TOOL_OPTIONS="-Djavax.net.ssl.trustStore=$PWD/${TRUSTSTORE} -Djavax.net.ssl.trustStorePassword=${STORE_PASS}"
# Profils recommandés sans 'insecure' pour activer vraie validation TLS
export SPRING_PROFILES_ACTIVE=local-ide
EOF

echo "[6/6] Terminé. Pour utiliser: source ./.env.local-truststore puis ./mvnw spring-boot:run"

