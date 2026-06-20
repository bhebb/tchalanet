# Operations

## Topologie supportée

La topologie opérationnelle standard est :

- Traefik
- PostgreSQL
- Redis
- API
- Edge service

Firebase est le serveur d'authentification externe. L'infra ne démarre pas de
serveur d'auth local en staging ou production.

## Local IDE

```bash
make local-ide-up ENV=dev
```

Lance Traefik, PostgreSQL et Firebase Auth Emulator.

```bash
make local-ide-up-redis ENV=dev
```

Ajoute Redis.

## API en container

```bash
make local-api-up ENV=dev
make local-api-smoke ENV=dev
```

## Produit local

```bash
make local-product-up ENV=dev
```

## Staging / production

```bash
make up-staging
make up-prod
```

## Smoke

```bash
make smoke-staging
```

Le smoke staging vérifie API, Edge et Web.
