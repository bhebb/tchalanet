# Deployment

## Runtime standard

Production et staging utilisent :

- Traefik
- PostgreSQL
- Redis
- API
- Edge service
- Firebase externe pour l'authentification

## Variables minimales

```bash
TCH_IDENTITY_PROVIDER=firebase
FIREBASE_PROJECT_ID=<project-id>
FIREBASE_CREDENTIALS_PATH=/run/secrets/firebase-admin.json
APP_DB_PASSWORD=<secret>
REDIS_PASSWORD=<secret>
EDGE_HMAC_SECRET=<secret>
```

## Staging

```bash
make env-merge ENV=staging
make up-staging
make smoke-staging
```

## Production

```bash
make env-merge ENV=prod
make up-prod
```
