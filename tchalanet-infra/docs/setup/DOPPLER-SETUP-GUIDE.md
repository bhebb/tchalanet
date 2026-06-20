# Doppler Setup Guide

## Secrets requis

```bash
POSTGRES_PASSWORD=<secret>
APP_DB_PASSWORD=<secret>
SPRING_DATASOURCE_PASSWORD=<same-as-app-db>
REDIS_PASSWORD=<secret>
EDGE_HMAC_SECRET=<secret>
FIREBASE_PROJECT_ID=<project-id>
FIREBASE_CREDENTIALS_PATH=/run/secrets/firebase-admin.json
```

Les credentials Firebase Admin doivent être injectés par secret manager ou
volume secret. Ne pas committer de JSON de service account.

## Génération locale

```bash
./scripts/doppler/generate-secrets.sh staging
```

## Download

```bash
DOPPLER_TOKEN=dp.st.xxx make doppler-download ENV=staging
```
