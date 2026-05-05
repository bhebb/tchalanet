# Tasks: infra-refactor-local-server-v0

## 1. Documentation / charte

- [ ] Créer ou mettre à jour `README.md` infra.
- [ ] Créer `docs/00-infra-charter.md`.
- [ ] Documenter P0/P0+/server-v0/post-v0.
- [ ] Documenter les modes `local-ide`, `local-api-docker`, `local-product`, `server-v0`.

## 2. Makefile

- [ ] Ajouter `local-env`.
- [ ] Ajouter `p0-up`, `p0-smoke`, `p0-down`.
- [ ] Ajouter `p0-plus-up`, `p0-plus-down`.
- [ ] Ajouter `local-ide-up`, `local-ide-up-redis`, `local-ide-down`.
- [ ] Ajouter `local-api-up`, `local-api-down`, `local-api-logs`.
- [ ] Ajouter `local-product-up`, `local-product-down`.
- [ ] Ajouter `deploy-staging`, `up-staging`, `smoke-staging`.
- [ ] Ajouter commandes staging disposable : `staging-create`, `staging-backup`, `staging-destroy`, `staging-restore-latest`.

## 3. Traefik

- [ ] Séparer `traefik.yml` statique.
- [ ] Créer `dynamic-src/common`.
- [ ] Créer `dynamic-src/local`.
- [ ] Créer `dynamic-src/staging`.
- [ ] Créer `dynamic-src/prod`.
- [ ] Ajouter script render dynamic env.
- [ ] Monter seulement `traefik/dynamic` rendu.
- [ ] Local : mkcert + localtest.me.
- [ ] Staging/prod : Let's Encrypt.
- [ ] Pas de dashboard 8080 public hors local.

## 4. Keycloak

- [ ] Déplacer `get-realm.sh` sous `scripts/keycloak/` ou normaliser chemin.
- [ ] Séparer `realm.base.json` sans users locaux.
- [ ] Créer `overlays/dev.json` avec users locaux.
- [ ] Créer `overlays/staging.json` sans users.
- [ ] Créer `overlays/prod.json` sans users.
- [ ] Ajouter validation `jq empty`.
- [ ] Refuser `.users` hors dev/local.
- [ ] Corriger client API sans dépendance `bearerOnly` si applicable.

## 5. PostgreSQL

- [ ] Postgres `back-only`.
- [ ] Pas de port 5432 dans staging/prod.
- [ ] Port local via override uniquement.
- [ ] Ajouter `hba_file=/etc/postgresql/pg_hba.conf` dans command.
- [ ] Simplifier `pg_hba.conf` P0.
- [ ] Simplifier `postgresql.conf` P0.
- [ ] Corriger `postgres-init.sh` idempotent.
- [ ] Rendre Unleash DB optionnelle.
- [ ] Ajouter `make postgres-smoke` ou inclure dans `p0-smoke`.

## 6. Redis

- [ ] Redis `back-only`.
- [ ] Retirer `edge` de Redis.
- [ ] Corriger healthcheck avec `REDIS_PASSWORD`.
- [ ] Password obligatoire staging/prod via `.secrets`.
- [ ] Garder password optionnel dev.
- [ ] Ajouter `redis-smoke` dans P0+.

## 7. API Docker startup

- [ ] Vérifier datasource Docker.
- [ ] Vérifier issuer Keycloak local.
- [ ] Ajouter `extra_hosts auth.localtest.me:host-gateway` en local si nécessaire.
- [ ] Vérifier Flyway/app DB user.
- [ ] Vérifier Redis config.
- [ ] Désactiver Meili/Unleash/Umami en v0 si absents.
- [ ] Ajouter smoke `/actuator/health`.

## 8. Edge/Web compose

- [ ] Edge service sur `edge + back`.
- [ ] Web service sur `edge`.
- [ ] Edge health endpoint dans smoke.
- [ ] Web health/static smoke dans smoke.

## 9. Env/secrets

- [ ] Confirmer `compose.env`, `.env.merged`, `.secrets`.
- [ ] Vérifier que `.secrets` est ignoré git.
- [ ] Vérifier que `merge-env.sh` exclut secrets et compose.env.
- [ ] Documenter Doppler flow.

## 10. Scripts

- [ ] Créer `docs/reference/scripts-inventory.md`.
- [ ] Classifier chaque script.
- [ ] Déplacer legacy sans suppression brutale.
- [ ] Corriger chemins Makefile.

## 11. Hetzner staging disposable

- [ ] Vérifier scripts `hcloud/*`.
- [ ] Vérifier scripts `remote/*`.
- [ ] Créer `staging-create`.
- [ ] Créer `staging-destroy` avec confirmation.
- [ ] Créer `staging-backup`.
- [ ] Créer `staging-restore-latest`.
- [ ] Documenter que prod/client n'est pas disposable sans SLA explicite.

## 12. Validation finale

- [ ] `make p0-up ENV=dev` vert.
- [ ] `make p0-smoke ENV=dev` vert.
- [ ] `make p0-plus-up ENV=dev` vert.
- [ ] `make local-api-up ENV=dev` vert.
- [ ] `make staging-create` testé.
- [ ] `make deploy-staging` testé.
- [ ] `make staging-destroy` testé après backup.
