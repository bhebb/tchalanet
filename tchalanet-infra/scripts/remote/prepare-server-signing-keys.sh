#!/usr/bin/env bash
# Ensure staging/prod API has a persistent Ed25519 server-signing keypair.
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

secrets_file="envs/$DEPLOY_ENV/.secrets"
[ -f "$secrets_file" ] || fail "Missing $secrets_file"

load_runtime_env() {
  set -a
  # shellcheck disable=SC1091
  . "envs/common/compose.env"
  # shellcheck disable=SC1090
  . "envs/$DEPLOY_ENV/compose.env"
  # shellcheck disable=SC1090
  . "$secrets_file"
  set +a
}

has_signing_env() {
  [ -n "${TCH_SERVER_SIGNING_ED25519_PRIVATE_KEY_PKCS8_BASE64:-}" ] &&
    [ -n "${TCH_SERVER_SIGNING_ED25519_PUBLIC_KEY_SPKI_BASE64:-}" ]
}

append_signing_env() {
  private_b64="$1"
  public_b64="$2"

  tmp_file="$(mktemp)"
  grep -v -E '^(TCH_SERVER_SIGNING_ED25519_PRIVATE_KEY_PKCS8_BASE64|TCH_SERVER_SIGNING_ED25519_PUBLIC_KEY_SPKI_BASE64)=' "$secrets_file" > "$tmp_file" || true
  {
    cat "$tmp_file"
    printf 'TCH_SERVER_SIGNING_ED25519_PRIVATE_KEY_PKCS8_BASE64=%s\n' "$private_b64"
    printf 'TCH_SERVER_SIGNING_ED25519_PUBLIC_KEY_SPKI_BASE64=%s\n' "$public_b64"
  } > "$secrets_file"
  rm -f "$tmp_file"
  chmod 600 "$secrets_file"
}

load_runtime_env
if has_signing_env; then
  log "Server signing keys already present in $secrets_file"
  exit 0
fi

if [ "$DEPLOY_ENV" != "staging" ]; then
  fail "Missing server signing keys in $secrets_file. Set TCH_SERVER_SIGNING_ED25519_PRIVATE_KEY_PKCS8_BASE64 and TCH_SERVER_SIGNING_ED25519_PUBLIC_KEY_SPKI_BASE64 in Doppler before deploying production."
fi

secret_dir="server/secrets"
private_b64_file="$secret_dir/server-signing-ed25519-private.pkcs8.b64"
public_b64_file="$secret_dir/server-signing-ed25519-public.spki.b64"
private_pem="$secret_dir/server-signing-ed25519-private.pem"
public_pem="$secret_dir/server-signing-ed25519-public.pem"

mkdir -p "$secret_dir"
chmod 700 "$secret_dir" || true
umask 077

if [ -s "$private_b64_file" ] && [ -s "$public_b64_file" ]; then
  log "Reusing persisted staging server signing keypair from $secret_dir"
else
  command -v openssl >/dev/null 2>&1 || fail "openssl is required to generate staging server signing keys"
  log "Generating persistent staging server signing keypair in $secret_dir"
  openssl genpkey -algorithm Ed25519 -out "$private_pem" >/dev/null 2>&1
  openssl pkey -in "$private_pem" -pubout -out "$public_pem" >/dev/null 2>&1
  awk 'NF && $0 !~ /^-----/ {printf "%s", $0}' "$private_pem" > "$private_b64_file"
  awk 'NF && $0 !~ /^-----/ {printf "%s", $0}' "$public_pem" > "$public_b64_file"
  rm -f "$private_pem" "$public_pem"
  chmod 600 "$private_b64_file" "$public_b64_file"
fi

private_b64="$(tr -d '\r\n' < "$private_b64_file")"
public_b64="$(tr -d '\r\n' < "$public_b64_file")"
[ -n "$private_b64" ] || fail "Generated private signing key is empty"
[ -n "$public_b64" ] || fail "Generated public signing key is empty"

printf '%s' "$private_b64" | base64 -d >/dev/null 2>&1 || fail "Private signing key is not valid base64"
printf '%s' "$public_b64" | base64 -d >/dev/null 2>&1 || fail "Public signing key is not valid base64"

append_signing_env "$private_b64" "$public_b64"
log "Server signing keys appended to $secrets_file for this deploy"
