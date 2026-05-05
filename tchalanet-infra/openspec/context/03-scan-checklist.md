# Checklist scan infra avant proposal

## Ordre de scan

```text
1. Makefile
2. compose/
3. traefik/
4. envs/
5. scripts/docker/
6. scripts/utils/
7. scripts/keycloak/
8. scripts/remote/
9. scripts/hcloud/
10. .github/workflows/
11. docs/
```

## Makefile

- Lister les targets publiques existantes.
- Identifier les targets doublons/legacy.
- Vérifier que P0/P0+/local-api/local-product/staging sont couverts.
- Vérifier que le Makefile appelle les scripts, pas l'inverse.

## Compose

Vérifier les services v0 :

- Traefik
- PostgreSQL
- Keycloak
- Redis
- API
- Edge
- Web

Vérifier que post-v0 reste optionnel :

- Meilisearch
- Unleash
- Umami
- Mailpit

## Networks

- PostgreSQL uniquement `back`.
- Redis uniquement `back`.
- API `edge + back`.
- Edge `edge + back`.
- Keycloak `edge + back`.
- Web `edge`.
- Traefik `edge`.

## Traefik

- Statique commun.
- Dynamic source par env.
- Un seul env rendu à la fois.
- Localhost/localtest.me cohérent.
- Dashboard non public staging/prod.

## Env/secrets

- `compose.env` = compose-time.
- `.env.merged` = runtime non-secret.
- `.secrets` = runtime secret, non committé.
- Doppler utilisé pour staging/prod.
- `merge-env.sh` n'inclut pas `.secrets` dans `.env.merged`.

## Keycloak

- Base realm sans users locaux.
- Overlay dev avec users locaux.
- Overlay staging/prod sans users.
- Script de génération valide le JSON.
- Pas de workflow CI qui delete realm en v0.

## PostgreSQL

- `pg_hba.conf` simple et back-only.
- `postgresql.conf` conservateur.
- Init script idempotent.
- Unleash DB optionnelle.
- Pas d'exposition 5432 staging/prod.

## Redis

- Healthcheck compatible password.
- `REDIS_PASSWORD` obligatoire staging/prod.
- Pas de réseau `edge`.
- Pas d'exposition 6379 staging/prod.

## Workflows

Cible :

- `server-pr.yml`
- `web-pr.yml`
- `edge-pr.yml`
- `infra-check.yml`
- `docs.yml`
- `deploy-staging.yml`

Archiver :

- deploy prod ancien
- manage realm séparé
- build publish all sur push main
- security scans automatiques lourds
- docs doublons

## Hetzner staging disposable

Vérifier ou prévoir :

- `staging-create`
- `staging-destroy`
- `staging-backup`
- `staging-restore-latest`
- firewall 22/80/443
- pas de DB/Redis public
- GHCR images avec `IMAGE_TAG`
- smoke tests après deploy
