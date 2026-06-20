# Environment Architecture

## Sources

- `envs/common/compose.env` : variables compose non secrètes communes.
- `envs/common/*.env` : valeurs runtime non secrètes par service.
- `envs/<env>/compose.env` : overrides compose par environnement.
- `envs/<env>/.secrets` : secrets locaux non versionnés.

## Auth

Production et staging :

```bash
TCH_IDENTITY_PROVIDER=firebase
FIREBASE_PROJECT_ID=<project-id>
FIREBASE_CREDENTIALS_PATH=/run/secrets/firebase-admin.json
```

Local IDE :

```bash
TCH_IDENTITY_PROVIDER=firebase-emulator
FIREBASE_PROJECT_ID=demo-tchalanet-local
FIREBASE_AUTH_EMULATOR_HOST=localhost:9099
```

## Secrets

```bash
POSTGRES_PASSWORD
APP_DB_PASSWORD
SPRING_DATASOURCE_PASSWORD
REDIS_PASSWORD
EDGE_HMAC_SECRET
```

Les credentials Firebase Admin sont injectés via secret manager ou montage
secret et ne doivent jamais être versionnés.
