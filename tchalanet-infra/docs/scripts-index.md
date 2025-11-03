# Index des scripts (tchalanet-infra/scripts)

Vue condensée des scripts essentiels. Les anciens scripts ont été déplacés ou marqués « legacy ». Voir `scripts/local/_legacy/README.md`.

## Chemins dorés (ordre préfixé)

### Local (dev)
1. `scripts/local/01-setup-networks.sh <env>` → crée `edge-<env>` / `back-<env>`
2. `scripts/local/02-env-prepare.sh <env>` → dedupe + merge des envs (`make env-prepare`)
3. `scripts/local/03-up-core.sh <env>` → Postgres + Keycloak
4. `scripts/local/04-up-flags-edge.sh <env>` → Unleash + Unleash Edge
5. `scripts/local/05-check-health.sh <env>` → vérifications rapides

### Remote (serveur)
1. `scripts/remote/01-bootstrap.sh <env>` → prérequis serveur (réseaux, docker, acme)
2. `scripts/remote/02-push-infra.sh <host>` → rsync de l’infra
3. `scripts/remote/03-prepare-remote.sh` → préparation minimale (réseaux legacy, dossiers)

### Hetzner Cloud (IaC minimal)
1. `scripts/hcloud/01-create-network.sh`
2. `scripts/hcloud/02-create-firewall.sh`
3. `scripts/hcloud/03-create-server.sh`
4. `scripts/hcloud/04-generate-cloud-init.sh`

## Keycloak
- Générer un realm ENV-ready (suffixe `-realm.json`):
```bash
export KC_LOGIN_THEME=tchalanet
export DEFAULT_LOCALE=fr
export SUPPORTED_LOCALES="en,fr,ht"
export TEST_USER_PASSWORD=changeme
./scripts/keycloak/get-realm.sh staging
```
- Import auto: seuls les fichiers `*-realm.json` sont copiés dans l’image (voir `keycloak/Dockerfile`).
- Provider: `tch-json-claim-mapper` buildé au moment du `docker build` (aucun JAR commité; voir `keycloak/providers/.gitignore`).

## Utilitaires conservés
- `scripts/utils/check-envars.sh` (wrapper: `scripts/local/check-envars.sh`)
- `scripts/utils/merge-envs.sh` (wrapper: `scripts/local/merge-envs.sh`)
- `scripts/utils/dedupe-envs.sh` (wrapper)
- `scripts/utils/dedupe-intra-env.sh` (wrapper)
- `scripts/utils/docker-pull-retry.sh` (wrapper)

## Legacy
- Les anciens scripts (fix-*, validate-*, setup-*, up-*) sont dans `scripts/local/_legacy/`.
- Exemple de wrapper encore disponible: `scripts/local/local-up-staging.sh` (affiche un avertissement et appelle 01→05).

Notes
- Préférez les cibles Make: `make up-all ENV=<env>`, `make env-prepare ENV=<env>`, `make check-health ENV=<env>`.
- Les profils Compose permettent d’activer/désactiver des services non essentiels (Meili, Unleash) selon les besoins.
