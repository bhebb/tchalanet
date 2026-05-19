#!/usr/bin/env zsh
# Script: setup-mkcert-truststore.sh
# Objectif:
#   - Générer un certificat local (mkcert) pour des domaines dev (SAN)
#   - Créer un truststore Java "complet" = cacerts (CA publiques) + CA mkcert (dev)
#
# Résultat:
#   - Les appels HTTPS vers Internet (NY/FL/news) continuent de marcher
#   - Les services locaux HTTPS signés par mkcert sont aussi acceptés
#
# Usage:
#   ./scripts/setup-mkcert-truststore.sh
#
# Prérequis:
#   brew install mkcert nss
#   mkcert -install   (au moins une fois)

set -euo pipefail

DOMAINS=(auth.localtest.me app.localtest.me api.localtest.me)
CERT_NAME="dev-local"

TRUSTSTORE="local-truststore.jks"
STORE_PASS="changeit" # password standard de cacerts
ENV_FILE=".env.local-truststore"

say() { print -r -- "$*"; }
die() { print -r -- "ERROR: $*" >&2; exit 1; }

say "[0/8] Démarrage..."

say "[1/8] Vérification mkcert..."
command -v mkcert >/dev/null 2>&1 || die "mkcert n'est pas installé. Installe: brew install mkcert nss"

# S'assure que mkcert a été installé dans le trust store OS (utile mais pas obligatoire pour Java)
# mkcert -install >/dev/null 2>&1 || true

CAROOT="$(mkcert -CAROOT)"
say "CA mkcert (CAROOT): ${CAROOT}"
[[ -f "${CAROOT}/rootCA.pem" ]] || die "rootCA.pem introuvable dans ${CAROOT}. Lance: mkcert -install"

say "[2/8] Vérification Java / cacerts..."
if [[ -z "${JAVA_HOME:-}" ]]; then
  if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    export JAVA_HOME="$(/usr/libexec/java_home)"
  fi
fi
[[ -n "${JAVA_HOME:-}" ]] || die "JAVA_HOME est vide. Fixe JAVA_HOME ou installe une JDK."

CACERTS_SRC="${JAVA_HOME}/lib/security/cacerts"
[[ -f "${CACERTS_SRC}" ]] || die "cacerts introuvable: ${CACERTS_SRC}"

say "JAVA_HOME: ${JAVA_HOME}"
say "cacerts:   ${CACERTS_SRC}"

say "[3/8] Génération certificat SAN mkcert pour: ${DOMAINS[*]}"
# Génère un cert leaf (serveur) + clé privée pour tes domaines dev
mkcert -cert-file "${CERT_NAME}.crt" -slotKey-file "${CERT_NAME}.key" "${DOMAINS[@]}"

say "[4/8] Conversion en PKCS#12 (Keycloak / proxies) => ${CERT_NAME}.p12"
openssl pkcs12 -export \
  -in "${CERT_NAME}.crt" \
  -inkey "${CERT_NAME}.key" \
  -out "${CERT_NAME}.p12" \
  -name "${CERT_NAME}" \
  -password "pass:${STORE_PASS}"

say "[5/8] Création truststore Java complet: ${TRUSTSTORE}"
# IMPORTANT:
# - On ne crée PAS un truststore vide, sinon les CA publiques disparaissent et les appels externes cassent.
# - On part de cacerts, puis on ajoute mkcert-root.
cp "${CACERTS_SRC}" "${TRUSTSTORE}"

# Importer la CA mkcert (root) dans le truststore
# -> ainsi Java fera confiance à tous les certs signés par mkcert (tes services dev)
keytool -importcert -noprompt \
  -alias "mkcert-root" \
  -file "${CAROOT}/rootCA.pem" \
  -keystore "${TRUSTSTORE}" \
  -storepass "${STORE_PASS}"

say "[6/8] Vérification du truststore (alias mkcert-root + sample CA publiques)"
keytool -list -keystore "${TRUSTSTORE}" -storepass "${STORE_PASS}" | grep -q "mkcert-root" \
  || die "Alias mkcert-root non trouvé dans ${TRUSTSTORE} (import échoué)."

# Affiche quelques entrées (utile pour sanity check)
say "OK: mkcert-root présent."
say "Aperçu (5 premières lignes):"
keytool -list -keystore "${TRUSTSTORE}" -storepass "${STORE_PASS}" | head -n 5 || true

say "[7/8] Génération fichier env: ${ENV_FILE}"
cat <<EOF > "${ENV_FILE}"
# Variables à sourcer pour exécuter l'appli avec ce truststore (CA publiques + mkcert)
export JAVA_TOOL_OPTIONS="-Djavax.net.ssl.trustStore=$PWD/${TRUSTSTORE} -Djavax.net.ssl.trustStorePassword=${STORE_PASS}"
# Profils recommandés sans 'insecure' pour activer vraie validation TLS
export SPRING_PROFILES_ACTIVE=local-ide
# Secret HMAC partagé entre tchalanet-server et tchalanet-edge-service en local
export EDGE_HMAC_SECRET="tch-local-ide-edge-hmac-2026"
EOF

say "[8/8] Terminé ✅"
say "Fichiers générés:"
say "  - ${CERT_NAME}.crt / ${CERT_NAME}.key (cert local serveur)"
say "  - ${CERT_NAME}.p12 (Keycloak / Traefik / proxies)"
say "  - ${TRUSTSTORE} (cacerts + mkcert-root)"
say ""
say "Utilisation:"
say "  source ./${ENV_FILE}"
say "  ./mvnw spring-boot:run"
