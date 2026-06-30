# Operations

## Topologie supportée

La topologie opérationnelle standard est :

- Traefik
- PostgreSQL
- Redis
- API
- Edge service

Firebase est le serveur d'authentification externe. L'infra ne démarre pas de
serveur d'auth local en staging ou production.

## Local IDE

```bash
make local-ide-up ENV=dev
```

Lance Traefik, PostgreSQL et Firebase Auth Emulator.

```bash
make local-ide-up-redis ENV=dev
```

Ajoute Redis.

## API en container

```bash
make local-api-up ENV=dev
make local-api-smoke ENV=dev
```

## Produit local

```bash
make local-product-up ENV=dev
```

## Staging / production

```bash
make up-staging
make up-prod
```

## Smoke

```bash
make smoke-staging
```

Le smoke staging vérifie API, Edge et Web.

## Runbooks

Procédures pas-à-pas pour les opérations critiques :

| Runbook | Quand l'utiliser |
|---|---|
| [RB-00 — Secrets & variables checklist](runbooks/RB-00-secrets-checklist.md) | **Lire en premier** — inventaire complet de tous les secrets requis staging + prod |
| [RB-01 — Provisionnement staging](runbooks/RB-01-staging-provision.md) | Première mise en service ou recréation complète du serveur staging |
| [RB-02 — Déploiement web CF Pages](runbooks/RB-02-web-cf-pages.md) | Mise en place initiale des 3 portails Angular sur Cloudflare Pages |
| [RB-03 — Distribution mobile Android](runbooks/RB-03-mobile-distribution.md) | Build et distribution d'une version Android staging via Firebase App Distribution |

---

## Backup PostgreSQL production

Objectif : pouvoir reconstruire un serveur Docker supprimé/recréé rapidement, sans dépendre du
volume local.

Solution recommandée :

- `pg_dump -Fc` pour la base applicative `tchalanet_db`;
- archive séparée de `archive-data` tant que le storage archive est local;
- chiffrement avant sortie du serveur;
- upload vers un object storage externe avec immutabilité/Object Lock;
- test restore régulier sur staging ou serveur jetable.

Provider recommandé : Backblaze B2 avec Object Lock pour dev/staging/prod léger. AWS S3 Object
Lock est l'option plus enterprise/compliance. Le bucket doit avoir le versioning/immutability activé
dès sa création.

Commande de backup type sur serveur :

```bash
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_DIR="/var/backups/tchalanet"
mkdir -p "$BACKUP_DIR"

docker exec -e PGPASSWORD="$APP_DB_PASSWORD" tchl-postgres-prod \
  pg_dump -h 127.0.0.1 -U postgres -d tchalanet_db -Fc --no-owner --no-privileges \
  > "$BACKUP_DIR/tch-prod-db-$TIMESTAMP.dump"

tar -C /opt/tchalanet/tchalanet-server/archive-data \
  -czf "$BACKUP_DIR/tch-prod-archive-data-$TIMESTAMP.tar.gz" .

shasum -a 256 "$BACKUP_DIR/tch-prod-db-$TIMESTAMP.dump" \
  "$BACKUP_DIR/tch-prod-archive-data-$TIMESTAMP.tar.gz" \
  > "$BACKUP_DIR/tch-prod-$TIMESTAMP.sha256"
```

Chiffrement avant upload :

```bash
openssl enc -aes-256-cbc -pbkdf2 -salt \
  -pass env:BACKUP_PASSPHRASE \
  -in "$BACKUP_DIR/tch-prod-db-$TIMESTAMP.dump" \
  -out "$BACKUP_DIR/tch-prod-db-$TIMESTAMP.dump.enc"
```

Upload recommandé :

```bash
rclone copy "$BACKUP_DIR" b2:tchalanet-prod-backups/postgres \
  --include "tch-prod-*-$TIMESTAMP.*"
```

Restore rapide :

```bash
docker exec -e PGPASSWORD="$APP_DB_PASSWORD" tchl-postgres-prod \
  psql -h 127.0.0.1 -U postgres -d postgres -v ON_ERROR_STOP=1 <<SQL
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = 'tchalanet_db' AND pid <> pg_backend_pid();
DROP DATABASE IF EXISTS tchalanet_db;
CREATE DATABASE tchalanet_db OWNER app_user;
SQL

docker exec -i -e PGPASSWORD="$APP_DB_PASSWORD" tchl-postgres-prod \
  pg_restore -h 127.0.0.1 -U postgres -d tchalanet_db --no-owner --role=app_user \
  < tch-prod-db-YYYYmmdd-HHMMSS.dump
```

Minimum opérationnel :

- daily backup;
- rétention 30 jours pour staging, 90 jours minimum pour prod;
- copie hors serveur obligatoire;
- restore test au moins mensuel;
- accès bucket limité à un compte dédié backup, pas une clé admin partagée.
