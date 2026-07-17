# Environments

## Files

- `common/compose.env` : interpolation compose commune.
- `common/api.env` : defaults API non secrets.
- `common/postgres.env` : defaults PostgreSQL non secrets.
- `<env>/compose.env` : overrides par environnement.
- `<env>/.secrets` : secrets locaux non versionnés.

## Auth

```bash
TCH_IDENTITY_PROVIDER=firebase
```

Environment rule:

- `local-ide` uses real Firebase by default.
- local server E2E uses Firebase Auth Emulator.
- Docker `dev` API is configured for the emulator because it is the E2E runtime.
- `staging` and `prod` use real Firebase and must not set `FIREBASE_AUTH_EMULATOR_HOST`.

E2E / Docker dev emulator values:

```bash
TCH_IDENTITY_PROVIDER=firebase-emulator
FIREBASE_AUTH_EMULATOR_HOST=localhost:9099
```
