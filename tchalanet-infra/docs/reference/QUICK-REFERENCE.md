# Quick Reference

## Standard runtime

```text
Traefik
PostgreSQL
Redis
API
Edge service
```

Authentication is Firebase:

```bash
TCH_IDENTITY_PROVIDER=firebase
FIREBASE_PROJECT_ID=<project-id>
```

Local IDE uses Firebase Auth Emulator:

```bash
TCH_IDENTITY_PROVIDER=firebase-emulator
FIREBASE_PROJECT_ID=demo-tchalanet-local
FIREBASE_AUTH_EMULATOR_HOST=localhost:9099
```

## Commands

```bash
make env-merge ENV=dev
make local-ide-up ENV=dev
make local-ide-up-redis ENV=dev
make local-api-up ENV=dev
make local-product-up ENV=dev

make up-staging
make up-prod
make smoke-staging
```

## Compose config check

```bash
ENV=dev ./scripts/utils/smoke-local-infra.sh
```

## Logs

```bash
make logs-api ENV=dev
make logs-redis ENV=dev
make logs-postgres ENV=dev
```
