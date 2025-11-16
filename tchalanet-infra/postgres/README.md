# Configuration PostgreSQL

Configuration PostgreSQL pour Tchalanet (PostgreSQL 18).

## 📁 Contenu

```
postgres/
├── postgresql.conf    # Configuration principale PostgreSQL
└── pg_hba.conf       # Host-based authentication
```

## 📝 Fichiers

### `postgresql.conf`

Configuration principale du serveur PostgreSQL.

**Paramètres clés :**

- Performance tuning (shared_buffers, work_mem, etc.)
- Connexions (max_connections)
- Logging
- Réplication (si applicable)

**Montage :** `/etc/postgresql/postgresql.conf` dans le conteneur

### `pg_hba.conf`

Configuration d'authentification des connexions.

**Méthode par défaut :** `scram-sha-256` (sécurisé)

**Montage :** `/etc/postgresql/pg_hba.conf` dans le conteneur

## 🔧 Usage dans Docker Compose

```yaml
# compose/docker-compose-postgres.yml
volumes:
  - ../postgres/postgresql.conf:/etc/postgresql/postgresql.conf:ro
  - ../postgres/pg_hba.conf:/etc/postgresql/pg_hba.conf:ro
command:
  - 'postgres'
  - '-c'
  - 'config_file=/etc/postgresql/postgresql.conf'
```

## 🎛️ Personnalisation

### Modifier la configuration

1. Éditer `postgres/postgresql.conf` ou `postgres/pg_hba.conf`
2. Redémarrer PostgreSQL :
   ```bash
   make restart-postgres ENV=staging
   ```

### Vérifier la config appliquée

```bash
# Se connecter au conteneur
docker exec -it tchl-postgres-staging psql -U postgres

# Vérifier un paramètre
SHOW shared_buffers;
SHOW max_connections;

# Voir toute la config
SELECT * FROM pg_settings WHERE source != 'default';
```

## 📊 Tuning recommandé

### Environnement local (dev)

```conf
shared_buffers = 256MB
work_mem = 16MB
maintenance_work_mem = 128MB
effective_cache_size = 1GB
max_connections = 100
```

### Staging

```conf
shared_buffers = 512MB
work_mem = 32MB
maintenance_work_mem = 256MB
effective_cache_size = 2GB
max_connections = 200
```

### Production

```conf
shared_buffers = 2GB
work_mem = 64MB
maintenance_work_mem = 512MB
effective_cache_size = 6GB
max_connections = 300
```

Ces valeurs peuvent être surchargées via variables d'environnement dans `envs/<env>/compose.env` :

```bash
PG_SHARED_BUFFERS=512MB
PG_WORK_MEM=32MB
PG_MAINTENANCE_WORK_MEM=256MB
PG_EFFECTIVE_CACHE_SIZE=2GB
```

## 🔐 Authentification

### Méthodes disponibles

- `scram-sha-256` ✅ Recommandé (sécurisé)
- `md5` ⚠️ Moins sécurisé (legacy)
- `trust` ❌ Pas de password (dev uniquement)

**Configuration actuelle :** via `POSTGRES_HOST_AUTH_METHOD` dans `envs/common/postgres.env`

### pg_hba.conf format

```conf
# TYPE  DATABASE  USER      ADDRESS      METHOD
host    all       all       0.0.0.0/0    scram-sha-256
host    all       all       ::1/128      scram-sha-256
```

## 📚 Ressources

- [PostgreSQL 18 Configuration](https://www.postgresql.org/docs/18/runtime-config.html)
- [pg_hba.conf Documentation](https://www.postgresql.org/docs/18/auth-pg-hba-conf.html)
- [PostgreSQL Performance Tuning](https://wiki.postgresql.org/wiki/Tuning_Your_PostgreSQL_Server)
- [PGTune](https://pgtune.leopard.in.ua/) - Outil de génération de config

## 🔗 Voir aussi

- [compose/docker-compose-postgres.yml](../compose/docker-compose-postgres.yml)
- [scripts/docker/postgres-init.sh](../scripts/docker/postgres-init.sh)
- [envs/common/postgres.env](../envs/common/postgres.env)
