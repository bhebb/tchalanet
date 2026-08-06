#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DEPLOY_SCRIPT="$SCRIPT_DIR/remote/deploy-runtime-services.sh"

# Load the production function so this test exercises the same guard clauses
# without starting containers or contacting the staging host.
eval "$(sed -n '/^verify_firebase_emulator_runtime()/,/^}/p' "$DEPLOY_SCRIPT")"

DEPLOY_API=1
RUNTIME_IDENTITY_PROVIDER=firebase
verify_firebase_emulator_runtime

DEPLOY_API=0
RUNTIME_IDENTITY_PROVIDER=firebase-emulator
verify_firebase_emulator_runtime

printf '%s\n' 'OK: real-Firebase and disabled API validator skips return success'
