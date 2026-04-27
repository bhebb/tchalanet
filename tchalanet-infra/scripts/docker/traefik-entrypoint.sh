#!/bin/sh
set -e

# Traefik entrypoint script
# Ensures acme.json has correct permissions before starting Traefik

ACME_FILE="/acme.json"
ENV="${ENV:-dev}"

if [ -f "$ACME_FILE" ]; then
  echo "→ Setting acme.json permissions to 600"
  chmod 600 "$ACME_FILE"
else
  echo "→ Creating acme.json with permissions 600"
  touch "$ACME_FILE"
  chmod 600 "$ACME_FILE"
fi

echo "→ Environment: $ENV"
echo "→ Starting Traefik..."
# If no args, start Traefik with explicit config file to ensure static config is loaded
if [ $# -eq 0 ]; then
  exec traefik --configFile=/etc/traefik/traefik.yml
else
  exec /entrypoint.sh "$@"
fi
