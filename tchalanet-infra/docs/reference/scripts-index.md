# Scripts Index — tchalanet-infra/scripts

All scripts are internal implementation details. Prefer `make <target>` over calling scripts directly.

## scripts/utils/ — Core orchestration

| Script                      | Makefile target                                    | Purpose                                                           |
| --------------------------- | -------------------------------------------------- | ----------------------------------------------------------------- |
| `run-compose.sh`            | (called by service-up.sh)                          | Merge envs and run docker compose with proper flags               |
| `run-compose-wrapper.sh`    | (called by Makefile)                               | Wrapper that resolves compose files for a service                 |
| `service-up.sh`             | `up-<service>`, `down-<service>`, `logs-<service>` | Bring a named service up/down/logs                                |
| `up-seq.sh`                 | `up-all`, `up-staging`, `up-prod`                  | Sequenced full-stack startup                                      |
| `merge-env.sh`              | `env-merge`                                        | Merge `common/.env` + `<env>/.env` → `.env.merged`                |
| `setup-networks.sh`         | `networks`                                         | Create `edge-<env>` and `back-<env>` Docker networks              |
| `pre-render-traefik.sh`     | `render-traefik`                                   | Copy `traefik/env/<env>.yaml` → `traefik/dynamic/10-routers.yaml` |
| `generate-staging-certs.sh` | `certs-staging`                                    | Generate self-signed certs for staging                            |
| `smoke-staging.sh`          | `smoke-staging`                                    | HTTP smoke tests against staging endpoints                        |
| `wait-keycloak.sh`          | (called by up-seq.sh)                              | Block until Keycloak OIDC discovery responds                      |

## scripts/local/ — Local dev helpers

| Script                         | Purpose                                                                            |
| ------------------------------ | ---------------------------------------------------------------------------------- |
| `local-setup-env.sh`           | Bootstrap a new env profile (creates compose.env, runs setup-networks + env-merge) |
| `setup-api-env.sh`             | Generate `../tchalanet-server/.env` from infra secrets for IDE development         |

## scripts/keycloak/

| Script         | Makefile target | Purpose                                                         |
| -------------- | --------------- | --------------------------------------------------------------- |
| `get-realm.sh` | `get-realm`     | Export realm JSON from running Keycloak for use in Docker image |

## scripts/remote/ — Server operations

| Script                      | Makefile target             | Purpose                                                 |
| --------------------------- | --------------------------- | ------------------------------------------------------- |
| `push-infra-bkup.sh`        | `push-staging`, `push-prod` | rsync infra files to remote server                      |
| `01-bootstrap.sh`           | —                           | One-time server bootstrap (Docker install, directories) |
| `install-docker.sh`         | —                           | Install Docker on a fresh Debian/Ubuntu server          |
| `staging-backup.sh`         | `staging-backup`            | Backup PostgreSQL data before server destroy            |
| `staging-restore-latest.sh` | `staging-restore-latest`    | Restore latest backup on a fresh staging server         |

## scripts/hcloud/ — Hetzner Cloud IaC

| Script                      | Makefile target   | Purpose                                             |
| --------------------------- | ----------------- | --------------------------------------------------- |
| `staging-create.sh`         | `staging-create`  | Create staging VM, network, firewall via hcloud CLI |
| `staging-destroy.sh`        | `staging-destroy` | Destroy staging VM                                  |
| `01-create-network.sh`      | —                 | Create Hetzner private network                      |
| `02-create-firewall.sh`     | —                 | Create Hetzner firewall rules                       |
| `03-create-server.sh`       | —                 | Create server (used by staging-create.sh)           |
| `04-generate-cloud-init.sh` | —                 | Render cloud-init.yml from template                 |

## scripts/doppler/ — Secrets management

| Script                        | Makefile target    | Purpose                                               |
| ----------------------------- | ------------------ | ----------------------------------------------------- |
| `download-doppler-secrets.sh` | `doppler-download` | Download secrets from Doppler → `envs/<env>/.secrets` |
| `setup-doppler.sh`            | —                  | Interactive Doppler CLI setup                         |
| `generate-secrets.sh`         | —                  | Generate random secret values locally                 |
| `create-secrets-from-env.sh`  | —                  | Import existing .env values into Doppler              |
