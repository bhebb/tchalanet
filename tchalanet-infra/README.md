# Tchalanet Infra

Infrastructure Docker pour les runtimes locaux, staging et production.

## Runtime standard

Le runtime standard utilise Firebase comme serveur d'authentification externe.

Services standard :

- Traefik
- PostgreSQL
- Redis
- API
- Edge service

Local IDE ajoute Firebase Auth Emulator pour tester l'authentification sans
secret Firebase réel.

## Commandes principales

```bash
make local-ide-up ENV=dev
make local-ide-up-redis ENV=dev
make local-api-up ENV=dev
make local-product-up ENV=dev

make up-staging
make up-prod
```

## Modes

- `local-ide-up` : Traefik + PostgreSQL + Firebase Auth Emulator. L'API tourne
  dans l'IDE.
- `local-ide-up-redis` : ajoute Redis.
- `local-api-up` : Traefik + PostgreSQL + Redis + API en container.
- `local-product-up` : ajoute edge-service et web.
- `up-staging` / `up-prod` : Traefik + PostgreSQL + Redis + API + edge-service.

## Auth

Variables principales :

```bash
TCH_IDENTITY_PROVIDER=firebase
FIREBASE_PROJECT_ID=<project-id>
FIREBASE_CREDENTIALS_PATH=/run/secrets/firebase-admin.json
```

Local emulator :

```bash
TCH_IDENTITY_PROVIDER=firebase-emulator
FIREBASE_PROJECT_ID=demo-tchalanet-local
FIREBASE_AUTH_EMULATOR_HOST=localhost:9099
```

## Structure

- `compose/` : Docker Compose par service.
- `envs/` : variables par environnement.
- `scripts/` : helpers de déploiement, smoke tests, Doppler et Docker.
- `traefik/` : configuration dynamique et certificats.
- `firebase-emulator/` : setup local de Firebase Auth Emulator.
