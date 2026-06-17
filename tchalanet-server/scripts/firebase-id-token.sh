#!/usr/bin/env bash
set -euo pipefail

FIREBASE_AUTH_BASE_URL="${FIREBASE_AUTH_BASE_URL:-http://localhost:9099/identitytoolkit.googleapis.com/v1}"
FIREBASE_API_KEY="${FIREBASE_API_KEY:-demo-tchalanet-local}"
FIREBASE_EMAIL="${FIREBASE_EMAIL:-admin@local}"
FIREBASE_PASSWORD="${FIREBASE_PASSWORD:-Changeme1!}"
FIREBASE_CREATE_USER="${FIREBASE_CREATE_USER:-false}"
OUTPUT_EXPORT=false

usage() {
  cat <<'EOF'
Usage: scripts/firebase-id-token.sh [options]

Generate a Firebase ID token. Defaults target the local Auth Emulator.

Options:
  --email EMAIL       Account email (default: admin@local)
  --password PASSWORD Account password (default: Changeme1!)
  --create            Create the account before returning its token
  --export            Print: export FIREBASE_ID_TOKEN='<token>'
  -h, --help          Show this help

Environment variables remain supported:
  FIREBASE_AUTH_BASE_URL, FIREBASE_API_KEY, FIREBASE_EMAIL,
  FIREBASE_PASSWORD, FIREBASE_CREATE_USER
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --email)
      FIREBASE_EMAIL="${2:?Missing value for --email}"
      shift 2
      ;;
    --password)
      FIREBASE_PASSWORD="${2:?Missing value for --password}"
      shift 2
      ;;
    --create)
      FIREBASE_CREATE_USER=true
      shift
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

action="accounts:signInWithPassword"
if [[ "${FIREBASE_CREATE_USER}" == "true" ]]; then
  action="accounts:signUp"
fi

payload="$(
  python3 -c \
    'import json,sys; print(json.dumps({"email": sys.argv[1], "password": sys.argv[2], "returnSecureToken": True}))' \
    "${FIREBASE_EMAIL}" "${FIREBASE_PASSWORD}"
)"

response_file="$(mktemp "${TMPDIR:-/tmp}/tch-firebase-token.XXXXXX")"
trap 'rm -f "${response_file}"' EXIT

http_code="$(
  curl --silent --show-error \
    --output "${response_file}" \
    --write-out '%{http_code}' \
    --request POST \
    "${FIREBASE_AUTH_BASE_URL}/${action}?key=${FIREBASE_API_KEY}" \
    --header 'Content-Type: application/json' \
    --data "${payload}" \
    || true
)"

if [[ "${http_code}" != "200" ]]; then
  echo "Firebase token request failed (HTTP ${http_code:-network-error})." >&2
  cat "${response_file}" >&2
  echo >&2
  exit 1
fi

token="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["idToken"])' <"${response_file}")"
if [[ "${OUTPUT_EXPORT}" == "true" ]]; then
  printf "export FIREBASE_ID_TOKEN='%s'\n" "${token}"
else
  printf '%s\n' "${token}"
fi
