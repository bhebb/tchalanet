#!/usr/bin/env bash
set -euo pipefail

TCH_LOCAL_JWT_ISSUER="${TCH_LOCAL_JWT_ISSUER:-tchalanet-local}"
TCH_LOCAL_JWT_SECRET="${TCH_LOCAL_JWT_SECRET:-dev-only-change-me-at-least-32-characters}"
LOCAL_JWT_USER="${LOCAL_JWT_USER:-admin}"
OUTPUT_EXPORT=false

usage() {
  cat <<'EOF'
Usage: scripts/local-jwt-token.sh [options]

Generate an HS256 token for the local-jwt or local-perf identity provider.

Options:
  --user USER  Seeded identity: super_admin, admin, or cashier (default: admin)
  --export     Print: export LOCAL_JWT_TOKEN='<token>'
  -h, --help   Show this help
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --user)
      LOCAL_JWT_USER="${2:?Missing value for --user}"
      shift 2
      ;;
    --export)
      OUTPUT_EXPORT=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

case "${LOCAL_JWT_USER}" in
  super_admin) subject="00000000-0000-0000-0000-000000010001" ;;
  admin) subject="00000000-0000-0000-0000-000000010002" ;;
  cashier) subject="00000000-0000-0000-0000-000000010003" ;;
  *)
    echo "Unsupported local user: ${LOCAL_JWT_USER}" >&2
    echo "Expected one of: super_admin, admin, cashier" >&2
    exit 2
    ;;
esac

token="$(
  python3 - "${TCH_LOCAL_JWT_SECRET}" "${TCH_LOCAL_JWT_ISSUER}" "${subject}" "${LOCAL_JWT_USER}" <<'PY'
import base64
import hashlib
import hmac
import json
import sys
import time

secret, issuer, subject, username = sys.argv[1:]
now = int(time.time())

def encode(value):
    raw = json.dumps(value, separators=(",", ":")).encode()
    return base64.urlsafe_b64encode(raw).rstrip(b"=").decode()

header = encode({"alg": "HS256", "typ": "JWT"})
payload = encode({
    "iss": issuer,
    "sub": subject,
    "email": f"{username}@localtest.me",
    "email_verified": True,
    "preferred_username": username,
    "iat": now,
    "exp": now + 3600,
})
signing_input = f"{header}.{payload}".encode()
signature = base64.urlsafe_b64encode(
    hmac.new(secret.encode(), signing_input, hashlib.sha256).digest()
).rstrip(b"=").decode()
print(f"{header}.{payload}.{signature}")
PY
)"

if [[ "${OUTPUT_EXPORT}" == "true" ]]; then
  printf "export LOCAL_JWT_TOKEN='%s'\n" "${token}"
else
  printf '%s\n' "${token}"
fi
