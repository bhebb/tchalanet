#!/usr/bin/env bash
# Materialize Firebase Admin service account JSON on the host for the API bind mount.
set -euo pipefail

DEPLOY_ENV="${1:-${ENV:-staging}}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

log() { printf -- '-> %s\n' "$*"; }
fail() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

case "$DEPLOY_ENV" in
  staging|stg) DEPLOY_ENV="staging" ;;
  prod|production) DEPLOY_ENV="prod" ;;
  *) fail "ENV must be staging or prod" ;;
esac

[ -f "envs/common/compose.env" ] || fail "Missing envs/common/compose.env"
[ -f "envs/$DEPLOY_ENV/compose.env" ] || fail "Missing envs/$DEPLOY_ENV/compose.env"
[ -f "envs/$DEPLOY_ENV/.secrets" ] || fail "Missing envs/$DEPLOY_ENV/.secrets"

set -a
# shellcheck disable=SC1091
. "envs/common/compose.env"
# shellcheck disable=SC1090
. "envs/$DEPLOY_ENV/compose.env"
# shellcheck disable=SC1090
. "envs/$DEPLOY_ENV/.secrets"
set +a
ENV="$DEPLOY_ENV"

provider="${TCH_IDENTITY_PROVIDER:-firebase}"
if [ "$provider" != "firebase" ]; then
  log "Skipping Firebase Admin credentials for TCH_IDENTITY_PROVIDER=$provider"
  exit 0
fi

target="${FIREBASE_CREDENTIALS_HOST_PATH:-}"
[ -n "$target" ] || fail "FIREBASE_CREDENTIALS_HOST_PATH is required for Firebase runtime deploy"

if [ -f "$target" ]; then
  chmod 600 "$target" || true
  log "Firebase Admin credentials already exist at $target"
  exit 0
fi

if [ -d "$target" ]; then
  rmdir "$target" 2>/dev/null || \
    fail "FIREBASE_CREDENTIALS_HOST_PATH points to a non-empty directory: $target"
fi

mkdir -p "$(dirname "$target")"
umask 077

json="${FIREBASE_ADMIN_JSON:-${FIREBASE_CREDENTIALS_JSON:-${FIREBASE_SERVICE_ACCOUNT_JSON:-${FIREBASE_SERVICE_ACCOUNT:-}}}}"
json_base64="${FIREBASE_ADMIN_JSON_BASE64:-${FIREBASE_SERVICE_ACCOUNT_BASE64:-}}"

if [ -n "$json" ]; then
  printf '%s' "$json" > "$target"
elif [ -n "$json_base64" ]; then
  printf '%s' "$json_base64" | base64 -d > "$target"
else
  fail "Missing Firebase Admin credentials file at $target. Ensure push-infra-bkup.sh copied tchalanet-server/tchalanet-39115-firebase-adminsdk-fbsvc-62e904a236.json, or set one of FIREBASE_ADMIN_JSON, FIREBASE_CREDENTIALS_JSON, FIREBASE_SERVICE_ACCOUNT_JSON, FIREBASE_SERVICE_ACCOUNT, FIREBASE_ADMIN_JSON_BASE64, or FIREBASE_SERVICE_ACCOUNT_BASE64 in Doppler config for $DEPLOY_ENV."
fi

if ! grep -q '"private_key"' "$target" || ! grep -q '"client_email"' "$target"; then
  rm -f "$target"
  fail "Firebase Admin credentials secret did not look like a service account JSON"
fi

chmod 600 "$target"
log "Firebase Admin credentials written to $target"
