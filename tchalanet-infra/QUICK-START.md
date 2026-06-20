# Quick Start Infra

## Local IDE

```bash
cd tchalanet-infra
make local-ide-up ENV=dev
```

Cela démarre :

- Traefik
- PostgreSQL
- Firebase Auth Emulator

L'API reste lancée depuis l'IDE avec :

```bash
SPRING_PROFILES_ACTIVE=local-ide
TCH_IDENTITY_PROVIDER=firebase-emulator
FIREBASE_PROJECT_ID=demo-tchalanet-local
FIREBASE_AUTH_EMULATOR_HOST=localhost:9099
```

## Local API en container

```bash
make local-api-up ENV=dev
```

Cela démarre Traefik, PostgreSQL, Redis et API.

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
