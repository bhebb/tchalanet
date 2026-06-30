# Docker Compose Index

## Runtime standard

| Service | Compose file | Networks | Notes |
| --- | --- | --- | --- |
| Traefik | `docker-compose-traefik.yml` | `edge` | Routes HTTP/TLS |
| PostgreSQL | `docker-compose-postgres.yml` | `back` | Base applicative |
| Redis | `docker-compose-redis.yml` | `back` | Cache / jobs |
| API | `docker-compose-api.yml` | `edge`, `back` | Spring Boot |
| Edge service | `docker-compose-edge-service.yml` | `edge`, `back` | Messages / edge APIs |
| Web (portails) | `docker-compose-web.yml` | `edge` | Nginx — public, admin, platform. **Voir Plan B ci-dessous.** |
| Firebase emulator | `docker-compose-firebase-emulator.yml` | `edge`, `back` | Local IDE only |

> `web` n'est pas inclus dans `up-seq.sh` par défaut — le plan Docker (Plan B) l'ajoute.
> Le plan Vercel (Plan A) remplace ce conteneur par des projets Vercel par portail.

## Auth

Firebase est le provider standard via `TCH_IDENTITY_PROVIDER=firebase`.
Le local IDE utilise `firebase-emulator`.

## Local build

`docker-compose.local-build.yml` construit uniquement les images applicatives
locales comme l'API.
