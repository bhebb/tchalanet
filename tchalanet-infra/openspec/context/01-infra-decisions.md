# Décisions infra actées

## Service scopes

- P0 strict = Traefik + PostgreSQL + Keycloak.
- P0+ = P0 strict + Redis.
- server-v0 = Traefik + PostgreSQL + Keycloak + Redis + API + Edge + Web.
- Meilisearch, Unleash, Umami et Mailpit sont post-v0.

## Docker networks

- PostgreSQL = `back` uniquement.
- Redis = `back` uniquement.
- Edge service = `edge + back`.
- API = `edge + back`.
- Keycloak = `edge + back`.
- Web = `edge`.
- Traefik = `edge`.

## Cost control / Hetzner

- Sans client payant, staging Hetzner est disposable.
- Ne pas compter sur stop/start pour réduire fortement les coûts.
- Créer staging quand nécessaire, déployer, smoke tester, sauvegarder PostgreSQL si utile, puis détruire.
- Ajouter/maintenir les commandes :
  - `staging-create`
  - `staging-destroy`
  - `staging-backup`
  - `staging-restore-latest`
- Production/client workloads restent persistants seulement avec client payant ou SLA explicite.

## Vercel

- Vercel Free autorisé uniquement pour previews/dev non-commerciales.
- Staging/client/prod ne doivent pas dépendre de Vercel Free.
- server-v0 officiel inclut Web Docker derrière Traefik.
- Vercel Pro reste une option future.

## Workflows GitHub

Workflow cible v0 :

```text
server-pr.yml
web-pr.yml
edge-pr.yml
infra-check.yml
docs.yml
deploy-staging.yml
```

Règles :

- Déploiement manuel seulement.
- Pas de build Docker automatique sur chaque push main.
- Pas de `:latest`.
- Edge a son propre PR workflow léger.
- Edge/API/Web utilisent le même `IMAGE_TAG` en staging.
- Security scan, mobile release et prod deploy sont plus tard.

## Traefik

- Statique commun : `traefik/traefik.yml`.
- Dynamique source par environnement : `traefik/dynamic-src/{common,local,staging,prod}`.
- Un seul environnement rendu dans `traefik/dynamic/`.
- Local = `localtest.me` + mkcert.
- Staging/prod = Let's Encrypt.
- Dashboard 8080 local uniquement.

## Keycloak

- `realm.base.json` sans users locaux.
- `overlays/dev.json` avec users locaux.
- `overlays/staging.json` sans users.
- `overlays/prod.json` sans users.
- `get-realm.sh` valide le JSON final.
- `get-realm.sh` refuse les users hors dev/local.

## PostgreSQL

- Back-only.
- Pas de port 5432 exposé staging/prod.
- Port exposé seulement via override local.
- `scram-sha-256`.
- TLS interne off en P0.
- Logs vers stderr Docker.
- Config conservatrice.
- DB Unleash optionnelle/post-v0.

## Redis

- Back-only.
- Jamais exposé via Traefik.
- Password obligatoire staging/prod.
- Password optionnel dev.
- Healthcheck doit fonctionner avec `REDIS_PASSWORD`.
