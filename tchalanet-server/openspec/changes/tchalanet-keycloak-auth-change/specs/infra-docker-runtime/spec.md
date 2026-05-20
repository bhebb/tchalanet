# Spec — infra-docker-runtime

## Intent

Provide a reproducible local/prod-ready Docker runtime baseline for Tchalanet identity/auth work.

## Services

```text
postgres
redis
traefik
keycloak
keycloak-provider maison
```

## Version policy

All runtime image tags must be centralized in infra env/config files and reflected in `VERSIONS.md` when they change.

## Target structure

```text
tchalanet-infra/
  envs/
    common/compose.env
    local/.env
    prod/.env.example
  compose/
    docker-compose.local.yml
    docker-compose.prod.yml
    postgres.yml
    redis.yml
    traefik.yml
    keycloak.yml
  keycloak/
    realm/
      base-realm.json
      overlays/local.json
      overlays/prod.json
    tchalanet-keycloak-provider/
```

## Local services

### Postgres

- Provides the main Tchalanet DB.
- Must expose a stable local port.
- Must have a healthcheck.
- Must use persistent local volume.

### Redis

- Supports cache/session/infra needs.
- Must be optional from a business correctness perspective.
- Must have a healthcheck.

### Traefik

- Routes local desktop hostnames:
  - `auth.tchalanet.lan`
  - `api.tchalanet.lan`
  - `app.tchalanet.lan`
- Uses HTTP locally.
- Uses HTTPS/TLS in prod.

### Keycloak

- Loads the Tchalanet provider.
- Imports the local realm.
- Uses external Postgres if selected for prod-like local.
- Must be configured through env vars, not hardcoded compose values.

## Acceptance criteria

- `docker compose up` starts Postgres, Redis, Traefik, and Keycloak.
- All services report healthy.
- Keycloak provider is loaded.
- Local realm is importable.
- Traefik routes `auth/api/app` hostnames correctly.
- Image tags are not duplicated across unrelated files.
