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

Local IDE utilise Firebase réel par défaut. Les tests E2E locaux utilisent
Firebase Auth Emulator pour obtenir des identités déterministes et jetables.

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

- `local-ide-up` : Traefik + PostgreSQL. L'API tourne dans l'IDE avec Firebase
  réel par défaut.
- `local-ide-up-redis` : ajoute Redis.
- `local-api-up` : Traefik + PostgreSQL + Redis + API en container; ce mode
  Docker dev est configuré pour Firebase Auth Emulator afin de servir les E2E.
- `local-product-up` : ajoute edge-service et web.
- `up-staging` / `up-prod` : Traefik + PostgreSQL + Redis + API + edge-service.

## Auth

Variables principales :

```bash
TCH_IDENTITY_PROVIDER=firebase
FIREBASE_PROJECT_ID=<project-id>
FIREBASE_CREDENTIALS_PATH=/run/secrets/firebase-admin.json
```

E2E local / API Docker dev :

```bash
TCH_IDENTITY_PROVIDER=firebase-emulator
FIREBASE_PROJECT_ID=demo-tchalanet-local
FIREBASE_AUTH_EMULATOR_HOST=localhost:9099
```

Règle :

- `local-ide` : Firebase réel, sauf override volontaire du développeur.
- E2E serveur local : Firebase Auth Emulator.
- `staging` / `prod` : Firebase réel, jamais `FIREBASE_AUTH_EMULATOR_HOST`.

## Structure

- `compose/` : Docker Compose par service.
- `envs/` : variables par environnement.
- `scripts/` : helpers de déploiement, smoke tests, Doppler et Docker.
- `traefik/` : configuration dynamique et certificats.
- `firebase-emulator/` : setup local de Firebase Auth Emulator.
