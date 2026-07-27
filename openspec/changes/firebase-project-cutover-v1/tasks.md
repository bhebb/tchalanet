# Tasks

## Configuration

- [x] Make the server Firebase project fallback explicit as `tchalanet`.
- [x] Make `local-ide` use real Firebase and materialize local credentials from
      Doppler-provided values without committing them.
- [x] Keep Docker `dev`/E2E on the Firebase Auth Emulator and expose a separate
      explicit emulator command for local IDE work when needed.
- [x] Align Doppler `dev` and `stg` project/provider/credential values.
- [x] Make mobile staging distribution default to the new Firebase App ID.

## Validation and documentation

- [x] Add configuration validation for Firebase project/credential mismatch.
- [x] Update active mobile and operations documentation.
- [x] Run focused shell/configuration/mobile/server validations.

Validation notes:

- `bash -n` passes for the changed shell scripts.
- Docker Compose interpolation passes with a validation-only HMAC value.
- Maven `validate` passes for the server modules.
- Flutter analyze could not run because the installed Flutter SDK cache is not
  writable in this environment.
