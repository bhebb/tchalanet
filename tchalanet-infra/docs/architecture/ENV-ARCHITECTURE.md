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
SPRING_PROFILES_ACTIVE=local-ide
TCH_IDENTITY_PROVIDER=firebase
FIREBASE_PROJECT_ID=<project-id>
FIREBASE_CREDENTIALS_PATH=<local firebase admin json>
```

E2E local / API Docker dev :

```bash
TCH_IDENTITY_PROVIDER=firebase-emulator
FIREBASE_PROJECT_ID=demo-tchalanet-local
FIREBASE_AUTH_EMULATOR_HOST=localhost:9099
```

Rule of thumb:

- `local-ide` uses real Firebase unless a developer deliberately opts into the emulator.
- server E2E uses Firebase Auth Emulator for deterministic provisioning.
- `dev` Docker API used by E2E uses emulator host `firebase-emulator:9099`.
- `staging` and `prod` use real Firebase and must not set `FIREBASE_AUTH_EMULATOR_HOST`.

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

### Webhooks transverses

Les webhooks sont des secrets optionnels, définis avec le même nom dans chaque
configuration Doppler qui les utilise :

```bash
SLACK_WEBHOOK_OPS_ALERTS
SLACK_WEBHOOK_BATCH_DRAWS
SLACK_WEBHOOK_DELIVERY
SLACK_WEBHOOK_SECURITY_AUDIT
```

Ne créer aucune valeur vide ou de démonstration. Tant qu'un canal Slack n'est
pas disponible, l'absence du secret est un état valide : le workflow écrit son
rapport GitHub sans tentative de notification. Quand l'URL réelle est prête,
l'enregistrer dans chaque configuration Doppler concernée, jamais dans
`envs/common/*.env` ni dans Git.
