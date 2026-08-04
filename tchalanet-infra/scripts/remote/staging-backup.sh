#!/usr/bin/env bash
# DÉPRÉCIÉ — remplacé par scripts/remote/pg-backup.sh.
#
# Ce script appelait `pg_dumpall -U postgres` alors que le superuser est `admin`,
# sans mot de passe : il échouait. Il déposait par ailleurs le résultat sur le
# poste du développeur, donc jamais planifié et jamais hors-site.
#
# Voir docs/operations/BACKUP-RESTORE.md.
set -euo pipefail

cat >&2 <<'MSG'
❌ staging-backup.sh est déprécié et ne produisait pas de backup exploitable.

   Backups planifiés : workflow GitHub "Database Backup" (quotidien, vérifié).
   Backup manuel     : ENV=staging ./scripts/remote/pg-backup.sh   (sur la VM)
   Restauration      : ENV=staging ./scripts/remote/pg-restore.sh

   Documentation : docs/operations/BACKUP-RESTORE.md
MSG
exit 1
