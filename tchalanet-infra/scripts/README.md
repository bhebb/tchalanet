# Infra Scripts

Scripts actifs :

- `utils/merge-env.sh` : fusionne les variables d'environnement.
- `utils/up-seq.sh` : démarre Traefik, PostgreSQL, Redis, API et edge-service.
- `utils/service-up.sh` : lance un service compose ciblé.
- `utils/smoke-local-infra.sh` : valide la configuration compose locale.
- `utils/smoke-staging.sh` : vérifie API, Edge et Web en staging.
- `docker/publish-images.sh` : publie l'image API.
- `doppler/` : génère et récupère les secrets.
- `local/setup-api-env.sh` : génère un env local IDE orienté Firebase.
- `local/backup-dev.sh` : backup local Docker PostgreSQL + `archive-data`.
- `local/restore-dev.sh` : restore rapide du dernier backup local Docker.

Firebase Auth Emulator est démarré via :

```bash
make up-firebase-emulator ENV=dev
```

Backup local dev :

```bash
bash tchalanet-infra/scripts/local/backup-dev.sh
BACKUP_PASSPHRASE='change-me' bash tchalanet-infra/scripts/local/backup-dev.sh
```

Restore local dev :

```bash
bash tchalanet-infra/scripts/local/restore-dev.sh
BACKUP_FILE=tchalanet-infra/backups/dev/tch-dev-db-YYYYmmdd-HHMMSS.dump bash tchalanet-infra/scripts/local/restore-dev.sh
```
