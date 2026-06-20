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
