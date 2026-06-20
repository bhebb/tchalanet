# Démarrage

## Local IDE

```bash
make local-ide-up ENV=dev
```

Démarre Traefik, PostgreSQL et Firebase Auth Emulator.

## Local IDE avec Redis

```bash
make local-ide-up-redis ENV=dev
```

## API en container

```bash
make local-api-up ENV=dev
```

## Produit local

```bash
make local-product-up ENV=dev
```

## Auth

Local :

```bash
TCH_IDENTITY_PROVIDER=firebase-emulator
FIREBASE_PROJECT_ID=demo-tchalanet-local
FIREBASE_AUTH_EMULATOR_HOST=localhost:9099
```

Staging/prod :

```bash
TCH_IDENTITY_PROVIDER=firebase
FIREBASE_PROJECT_ID=<project-id>
FIREBASE_CREDENTIALS_PATH=/run/secrets/firebase-admin.json
```
