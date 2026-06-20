# Compose

Les fichiers compose sont organisés par service. La stack standard ne contient
pas de serveur d'authentification local : Firebase est externe, et Firebase Auth
Emulator est utilisé seulement en local IDE.

## Stack standard

```bash
docker compose \
  -f compose/docker-compose-project.yml \
  -f compose/docker-compose-traefik.yml \
  -f compose/docker-compose-postgres.yml \
  -f compose/docker-compose-redis.yml \
  -f compose/docker-compose-api.yml \
  -f compose/docker-compose-edge-service.yml \
  up -d
```

## Local IDE

```bash
make local-ide-up ENV=dev
```

## Production

Production et staging doivent définir :

```bash
TCH_IDENTITY_PROVIDER=firebase
FIREBASE_PROJECT_ID=<project-id>
```
