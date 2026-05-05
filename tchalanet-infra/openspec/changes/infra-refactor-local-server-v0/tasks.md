# Tasks: infra-refactor-local-server-v0

## 1. Documentation / charte

- [x] Créer ou mettre à jour `README.md` infra.
- [x] Créer `docs/00-infra-charter.md`.
- [x] Documenter P0/P0+/server-v0/post-v0.
- [x] Documenter les modes `local-ide`, `local-api-docker`, `local-product`, `server-v0`.

## 2. Makefile

- [x] Ajouter `local-env`.
- [x] Ajouter `p0-up`, `p0-smoke`, `p0-down`.
- [x] Ajouter `p0-plus-up`, `p0-plus-down`.
- [x] Ajouter `local-ide-up`, `local-ide-up-redis`, `local-ide-down`.
- [x] Ajouter `local-api-up`, `local-api-down`, `local-api-logs`.
- [x] Ajouter `local-product-up`, `local-product-down`.
- [x] Ajouter `deploy-staging`, `up-staging`, `smoke-staging`.
- [x] Ajouter commandes staging disposable : `staging-create`, `staging-backup`, `staging-destroy`, `staging-restore-latest`.

## 3. Traefik

- [x] Séparer `traefik.yml` statique.
- [x] Créer `dynamic-src/common`.
- [x] Créer `dynamic-src/local`.
- [x] Créer `dynamic-src/staging`.
- [x] Créer `dynamic-src/prod`.
- [x] Ajouter script render dynamic env.
- [ ] Monter seulement `traefik/dynamic` rendu (à câbler dans compose).
- [x] Local : mkcert + localtest.me.
- [x] Staging/prod : Let's Encrypt.
- [x] Pas de dashboard 8080 public hors local.

## 4. Keycloak

- [x] Déplacer `get-realm.sh` sous `scripts/keycloak/` ou normaliser chemin.
- [x] Séparer `realm.base.json` sans users locaux.
- [x] Créer `overlays/dev.json` avec users locaux.
- [x] Créer `overlays/staging.json` sans users.
- [x] Créer `overlays/prod.json` sans users.
- [x] Ajouter validation `jq empty`.
- [x] Refuser `.users` hors dev/local.
- [ ] Corriger client API sans dépendance `bearerOnly` si applicable.

## 5. PostgreSQL

- [x] Postgres `back-only`.
- [ ] Pas de port 5432 dans staging/prod.
- [ ] Port local via override uniquement.
- [ ] Ajouter `hba_file=/etc/postgresql/pg_hba.conf` dans command.
- [x] Simplifier `pg_hba.conf` P0.
- [x] Simplifier `postgresql.conf` P0.
- [x] Corriger `postgres-init.sh` idempotent.
- [x] Rendre Unleash DB optionnelle.
- [ ] Ajouter `make postgres-smoke` ou inclure dans `p0-smoke`.

## 6. Redis

- [x] Redis `back-only`.
- [x] Retirer `edge` de Redis.
- [x] Corriger healthcheck avec `REDIS_PASSWORD`.
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

- [x] Créer `docs/reference/scripts-inventory.md`.
- [x] Classifier chaque script.
- [ ] Déplacer legacy sans suppression brutale.
- [x] Corriger chemins Makefile.

## 11. Hetzner staging disposable

- [x] Vérifier scripts `hcloud/*`.
- [x] Vérifier scripts `remote/*`.
- [x] Créer `staging-create`.
- [x] Créer `staging-destroy` avec confirmation.
- [x] Créer `staging-backup`.
- [x] Créer `staging-restore-latest`.
- [x] Documenter que prod/client n'est pas disposable sans SLA explicite.

## 12. Validation finale

- [ ] `make p0-up ENV=dev` vert.
- [ ] `make p0-smoke ENV=dev` vert.
- [ ] `make p0-plus-up ENV=dev` vert.
- [ ] `make local-api-up ENV=dev` vert.
- [ ] `make staging-create` testé.
- [ ] `make deploy-staging` testé.
- [ ] `make staging-destroy` testé après backup.
