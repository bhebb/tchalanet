#!/usr/bin/env bash
set -euo pipefail

: "${FIREBASE_EMAIL:?Set FIREBASE_EMAIL}"
: "${FIREBASE_PASSWORD:?Set FIREBASE_PASSWORD}"

FIREBASE_AUTH_BASE_URL="${FIREBASE_AUTH_BASE_URL:-http://localhost:9099/identitytoolkit.googleapis.com/v1}"
FIREBASE_API_KEY="${FIREBASE_API_KEY:-demo-tchalanet-local}"
FIREBASE_CREATE_USER="${FIREBASE_CREATE_USER:-false}"

action="accounts:signInWithPassword"
if [[ "${FIREBASE_CREATE_USER}" == "true" ]]; then
  action="accounts:signUp"
fi

response="$(
  payload="$(
    python3 -c \
      'import json,sys; print(json.dumps({"email": sys.argv[1], "password": sys.argv[2], "returnSecureToken": True}))' \
      "${FIREBASE_EMAIL}" "${FIREBASE_PASSWORD}"
  )"
  curl --fail-with-body --silent --show-error \
    --request POST \
    "${FIREBASE_AUTH_BASE_URL}/${action}?key=${FIREBASE_API_KEY}" \
    --header 'Content-Type: application/json' \
    --data "${payload}"
)"

python3 -c 'import json,sys; print(json.load(sys.stdin)["idToken"])' <<<"${response}"
