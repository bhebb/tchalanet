# Quick Start Infra

## Local IDE

```bash
cd tchalanet-infra
make local-ide-up ENV=dev
```

Cela démarre :

- Traefik
- PostgreSQL

L'API reste lancée depuis l'IDE avec :

```bash
SPRING_PROFILES_ACTIVE=local-ide
TCH_IDENTITY_PROVIDER=firebase
FIREBASE_PROJECT_ID=<project-id>
FIREBASE_CREDENTIALS_PATH=<local firebase admin json>
```

Le profil `local-ide` utilise Firebase réel par défaut. N'utilisez
`firebase-emulator` dans l'IDE que pour une session de debug volontaire.

## Local API en container

```bash
make local-api-up ENV=dev
```

Cela démarre Traefik, PostgreSQL, Redis et API.

Ce mode Docker dev est celui utilisé par les E2E locaux. Il utilise Firebase
Auth Emulator via `envs/dev/.env` et `envs/dev/compose.env`.

## Local produit

```bash
make local-product-up ENV=dev
```

Cela démarre Traefik, PostgreSQL, Redis, API, edge-service et web.

## Staging / production

```bash
make up-staging
make up-prod
```

La topologie standard est :

```text
Traefik -> API -> PostgreSQL
             -> Redis
             -> Edge service
```

Firebase reste le serveur d'authentification externe. Aucun conteneur
d'authentification n'est démarré dans l'infra standard.
