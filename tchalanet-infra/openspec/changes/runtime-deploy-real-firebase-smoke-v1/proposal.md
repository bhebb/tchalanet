# Runtime deploy must pass real-Firebase smoke

## Why

The staging runtime deploy recreated the API successfully and passed the health
check, then exited with status 1 before running the CORS smoke. The optional
Firebase emulator validator returned a non-zero status when the runtime used
real Firebase, and `set -e` treated that intentional skip as a deployment
failure.

## What

- Make the emulator validator return success when it is intentionally skipped.
- Add regression coverage for the real-Firebase path under `set -e`.

## Impact

Staging and production runtime deployments can continue from the health check to
the existing CORS and private-endpoint smoke checks when using real Firebase.
Emulator validation behavior remains unchanged when the emulator is enabled.

## Non-goals

- No changes to Firebase credentials or Doppler secrets.
- No database reset or runtime image changes.
- No changes to API or edge application code.
