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

Local IDE peut utiliser :

```bash
TCH_IDENTITY_PROVIDER=firebase-emulator
FIREBASE_AUTH_EMULATOR_HOST=localhost:9099
```
