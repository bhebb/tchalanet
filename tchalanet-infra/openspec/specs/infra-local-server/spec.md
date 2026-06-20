# infra-local-server Spec

## Requirement: P0 Local

P0 local SHALL include Traefik and PostgreSQL.

## Requirement: P0 Plus

P0 Plus SHALL add Redis.

## Requirement: Server V0

Server V0 SHALL include Traefik, PostgreSQL, Redis, API, edge-service, and web
where relevant.

## Requirement: Local Auth Emulator

Local IDE SHALL support Firebase Auth Emulator through explicit configuration:

```bash
TCH_IDENTITY_PROVIDER=firebase-emulator
FIREBASE_AUTH_EMULATOR_HOST=localhost:9099
```
