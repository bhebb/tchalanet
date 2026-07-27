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

expected_project="${FIREBASE_PROJECT_ID:-tchalanet}"
validate_credentials() {
  [ -f "$target" ] || return 1
  grep -q '"private_key"' "$target" || return 1
  grep -q '"client_email"' "$target" || return 1
  command -v jq >/dev/null 2>&1 || fail "jq is required to validate Firebase credentials project"
  credential_project="$(jq -r '.project_id // empty' "$target" 2>/dev/null || true)"
  [ -n "$credential_project" ] || return 1
  [ "$credential_project" = "$expected_project" ]
}

if [ -f "$target" ]; then
  if validate_credentials; then
    chmod 600 "$target" || true
    log "Firebase Admin credentials already exist at $target (project=$expected_project)"
    exit 0
  fi
  log "Discarding Firebase credentials that do not belong to project=$expected_project"
  rm -f "$target"
fi

if [ -d "$target" ]; then
  rmdir "$target" 2>/dev/null || \
    fail "FIREBASE_CREDENTIALS_HOST_PATH points to a non-empty directory: $target"
fi

target_dir="$(dirname "$target")"
if ! mkdir -p "$target_dir" 2>/dev/null; then
  if command -v sudo >/dev/null 2>&1; then
    sudo mkdir -p "$target_dir"
    sudo chown "$(id -u):$(id -g)" "$target_dir"
  else
    fail "Cannot create Firebase credentials directory: $target_dir"
  fi
fi
umask 077

json="${FIREBASE_ADMIN_JSON:-${FIREBASE_CREDENTIALS_JSON:-${FIREBASE_SERVICE_ACCOUNT_JSON:-${FIREBASE_SERVICE_ACCOUNT:-}}}}"
json_base64="${FIREBASE_ADMIN_JSON_BASE64:-${FIREBASE_SERVICE_ACCOUNT_BASE64:-}}"

if [ -n "$json" ]; then
  printf '%s' "$json" > "$target"
elif [ -n "$json_base64" ]; then
  printf '%s' "$json_base64" | base64 -d > "$target"
else
  fail "Missing Firebase Admin credentials file at $target. Set one of FIREBASE_ADMIN_JSON, FIREBASE_CREDENTIALS_JSON, FIREBASE_SERVICE_ACCOUNT_JSON, FIREBASE_SERVICE_ACCOUNT, FIREBASE_ADMIN_JSON_BASE64, or FIREBASE_SERVICE_ACCOUNT_BASE64 in Doppler config for $DEPLOY_ENV."
fi

validate_credentials || {
  rm -f "$target"
  fail "Firebase Admin credentials do not belong to project=$expected_project or are invalid"
}
chmod 600 "$target"
log "Firebase Admin credentials written to $target"
