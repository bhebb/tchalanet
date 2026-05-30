# Scripts Inventory

Statuts :

- `KEEP_PUBLIC` — appelable depuis le Makefile directement
- `KEEP_INTERNAL` — utilisé par d'autres scripts ou le Makefile en interne
- `LEGACY` — ne plus utiliser, ne pas supprimer avant confirmation
- `DELETE_LATER` — à supprimer après migration confirmée

---

## scripts/utils/

| Script                      | Statut        | Rôle                                                             |
| --------------------------- | ------------- | ---------------------------------------------------------------- |
| `merge-env.sh`              | KEEP_INTERNAL | Génère `.env.merged` depuis `envs/<env>/`                        |
| `run-compose.sh`            | KEEP_INTERNAL | Wrapper docker compose avec résolution de fichiers               |
| `run-compose-wrapper.sh`    | KEEP_INTERNAL | Wrapper d'entrée pour service-up.sh                              |
| `service-up.sh`             | KEEP_INTERNAL | Monte/descend un service Docker Compose par nom                  |
| `up-seq.sh`                 | KEEP_INTERNAL | Démarre les services en séquence (ordre P0→server-v0)            |
| `pre-render-traefik.sh`     | KEEP_INTERNAL | Génère `traefik/dynamic/10-routers.yaml` depuis env/             |
| `render-traefik-dynamic.sh` | KEEP_PUBLIC   | Rend `traefik/dynamic/` depuis `dynamic-src/<env>/`              |
| `setup-networks.sh`         | KEEP_INTERNAL | Crée les réseaux Docker edge/back                                |
| `wait-keycloak.sh`          | KEEP_INTERNAL | Attend que Keycloak soit prêt                                    |
| `health-check.sh`           | KEEP_INTERNAL | Health check générique sur un endpoint HTTP                      |
| `ssh-host-refresh.sh`       | KEEP_INTERNAL | Rafraîchit known_hosts pour un hôte SSH                          |
| `generate-staging-certs.sh` | LEGACY        | Générateur de certs auto-signés staging (remplacé Let's Encrypt) |
| `generate-cookie-secret.sh` | KEEP_INTERNAL | Génère un secret aléatoire pour cookies                          |
| `smoke-staging.sh`          | KEEP_PUBLIC   | Vérifie Keycloak, API, Edge, Web en staging                      |

## scripts/docker/

| Script                  | Statut        | Rôle                                            |
| ----------------------- | ------------- | ----------------------------------------------- |
| `postgres-init.sh`      | KEEP_INTERNAL | Init DB PostgreSQL (KC, App)                    |
| `redis-entrypoint.sh`   | KEEP_INTERNAL | Entrypoint Redis avec support requirepass       |
| `traefik-entrypoint.sh` | KEEP_INTERNAL | Entrypoint Traefik custom                       |
| `publish-images.sh`     | KEEP_PUBLIC   | Publie les images Docker vers le registry       |

## scripts/keycloak/

| Script         | Statut      | Rôle                                                 |
| -------------- | ----------- | ---------------------------------------------------- |
| `get-realm.sh` | KEEP_PUBLIC | Génère realm JSON depuis base + overlay + validation |

## scripts/hcloud/

| Script                      | Statut        | Rôle                                            |
| --------------------------- | ------------- | ----------------------------------------------- |
| `01-create-network.sh`      | KEEP_INTERNAL | Crée le réseau privé Hetzner                    |
| `02-create-firewall.sh`     | KEEP_INTERNAL | Configure le firewall Hetzner                   |
| `03-create-server.sh`       | KEEP_INTERNAL | Crée le serveur Hetzner                         |
| `04-generate-cloud-init.sh` | KEEP_INTERNAL | Génère le cloud-init pour bootstrap             |
| `staging-create.sh`         | KEEP_PUBLIC   | Orchestre création complète staging (01+02+03)  |
| `staging-destroy.sh`        | KEEP_PUBLIC   | Détruit staging avec backup auto + confirmation |

## scripts/remote/

| Script                            | Statut        | Rôle                                              |
| --------------------------------- | ------------- | ------------------------------------------------- |
| `01-bootstrap.sh`                 | KEEP_INTERNAL | Bootstrap Docker + infra sur serveur remote       |
| `install-docker.sh`               | KEEP_INTERNAL | Installation Docker sur serveur remote            |
| `push-infra-bkup.sh`              | KEEP_INTERNAL | Pousse le dossier infra sur un serveur remote     |
| `staging-backup.sh`               | KEEP_PUBLIC   | Backup PostgreSQL staging vers ./backups/staging/ |
| `staging-restore-latest.sh`       | KEEP_PUBLIC   | Restaure le dernier backup sur staging            |

## scripts/doppler/

| Script                        | Statut        | Rôle                                              |
| ----------------------------- | ------------- | ------------------------------------------------- |
| `download-doppler-secrets.sh` | KEEP_PUBLIC   | Télécharge les secrets Doppler dans `.secrets`    |
| `setup-doppler.sh`            | KEEP_PUBLIC   | Configure le projet Doppler localement            |
| `create-secrets-from-env.sh`  | KEEP_INTERNAL | Initialise les secrets Doppler depuis un .env     |
| `generate-secrets.sh`         | KEEP_INTERNAL | Génère des secrets aléatoires pour initialisation |

## scripts/local/

| Script                         | Statut | Rôle                                                   |
| ------------------------------ | ------ | ------------------------------------------------------ |
| `local-setup-env.sh`           | LEGACY | Setup env local (remplacé par merge-env.sh + Makefile) |
| `setup-api-env.sh`             | LEGACY | Setup env API local (remplacé par merge-env.sh)        |
| `start-traefik.sh`             | LEGACY | Démarrage Traefik direct (remplacé par Makefile)       |
