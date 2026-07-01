# Scripts Index

| Script | Usage | Notes |
| --- | --- | --- |
| `scripts/utils/merge-env.sh` | `make env-merge` | Produit `envs/<env>/.env.merged` |
| `scripts/utils/up-seq.sh` | `make up-staging`, `make up-prod` | Démarre Traefik, PostgreSQL, Redis, API, edge-service |
| `scripts/utils/service-up.sh` | `make up-api`, `make logs-api` | Helper par service |
| `scripts/utils/smoke-local-infra.sh` | manuel | Valide la config compose locale |
| `scripts/utils/smoke-staging.sh` | `make smoke-staging` | Vérifie API, edge-service et web |
| `scripts/docker/publish-images.sh` | manuel/CI | Publie l'image API |
| `scripts/doppler/generate-secrets.sh` | manuel | Génère les secrets runtime sans secrets d'auth locale |
| `scripts/local/setup-api-env.sh` | manuel | Génère un env local IDE Firebase |
| `scripts/hcloud/staging-create.sh` | `make staging-create` | Provisionnement complet staging (réseau + firewall + serveur) |
| `scripts/hcloud/staging-destroy.sh` | manuel | Supprime le serveur staging Hetzner |
| `scripts/hcloud/01-create-network.sh` | via staging-create.sh | Crée le réseau privé Hetzner |
| `scripts/hcloud/02-create-firewall.sh` | via staging-create.sh | Crée le firewall `tch-fw` (SSH/HTTP/HTTPS) |
| `scripts/hcloud/03-create-server.sh` | via staging-create.sh | Crée la VM avec cloud-init |
| `scripts/hcloud/04-generate-cloud-init.sh` | via staging-create.sh | Génère `cloud-init.yml` depuis le template |
| `scripts/remote/push-infra.sh` | CI/CD + manuel | Rsync de l'infra vers la VM distante |
| `scripts/remote/01-bootstrap.sh` | après création VM | Installe Docker, réseaux compose, Traefik sur la VM |
