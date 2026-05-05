# Design: infra-refactor-local-server-v0

## Charte infra

```text
Le Makefile est l'interface publique.
Docker Compose est le moteur.
Les scripts sont internes.
Les docs expliquent les parcours.
```

## Services

### P0 strict

```text
Traefik
PostgreSQL
Keycloak
```

### P0+

```text
Traefik
PostgreSQL
Keycloak
Redis
```

### server-v0

```text
Traefik
PostgreSQL
Keycloak
Redis
API Spring Boot
Edge Fastify/TypeScript
Web Angular
```

### post-v0

```text
Meilisearch
Unleash
Umami
Mailpit
Grafana/Loki/Prometheus
```

## Réseaux Docker

```text
Traefik      -> edge
Web          -> edge
API          -> edge + back
Edge service -> edge + back
Keycloak     -> edge + back
PostgreSQL   -> back
Redis        -> back
```

Redis reste `back-only`. PostgreSQL reste `back-only`.

## Modes

### local-ide

```text
API dans IntelliJ/IDE
Docker : Traefik + PostgreSQL + Keycloak
Redis optionnel
Web/Edge hors Docker si besoin
```

### local-api-docker

```text
API dans Docker
Docker : Traefik + PostgreSQL + Keycloak + Redis + API
Web/mobile hors Docker
Edge hors Docker ou Docker selon test
```

### local-product

```text
Tout server-v0 en Docker local
```

### server-v0

```text
Tout server-v0 en Docker sur Hetzner staging/prod
```

## Makefile public

Commandes cibles :

```bash
make local-env ENV=dev

make p0-up ENV=dev
make p0-smoke ENV=dev
make p0-down ENV=dev

make p0-plus-up ENV=dev
make p0-plus-down ENV=dev

make local-ide-up ENV=dev
make local-ide-up-redis ENV=dev
make local-ide-down ENV=dev

make local-api-up ENV=dev
make local-api-down ENV=dev
make local-api-logs ENV=dev

make local-product-up ENV=dev
make local-product-down ENV=dev

make staging-create
make staging-up
make staging-smoke
make staging-backup
make staging-destroy
make staging-restore-latest

make deploy-staging
make up-staging ENV=staging
```

## Traefik

### Structure

```text
traefik/
├── traefik.yml
├── dynamic-src/
│   ├── common/
│   ├── local/
│   ├── staging/
│   └── prod/
├── dynamic/
│   ├── common/
│   └── env/
├── certs/
└── acme/
```

Règle : un seul environnement dynamique rendu/monté à la fois.

### Hostnames local

```text
traefik.localtest.me
auth.localtest.me
api.localtest.me
app.localtest.me
edge.localtest.me
```

### Hostnames staging

```text
traefik.stg.tchalanet.com
auth.stg.tchalanet.com
api.stg.tchalanet.com
app.stg.tchalanet.com
edge.stg.tchalanet.com
```

### TLS

```text
local = mkcert
staging/prod = Let's Encrypt
```

Dashboard :

```text
local = port 8080 autorisé
staging/prod = pas de port 8080 public
```

## Keycloak

### Realm

```text
realm.base.json = commun, sans users locaux
overlays/dev.json = redirects locaux + users locaux
overlays/staging.json = redirects staging, sans users
overlays/prod.json = redirects prod, sans users
```

`get-realm.sh` doit :

- générer le JSON final ;
- appliquer overlay ;
- valider JSON avec `jq empty` ;
- forcer `.enabled=true` sauf override ;
- refuser `.users` hors `dev/local`.

### API client

Éviter de dépendre du vieux pattern `bearerOnly`. Préférer :

```json
{
  "clientId": "tchalanet-api",
  "protocol": "openid-connect",
  "publicClient": false,
  "serviceAccountsEnabled": false,
  "standardFlowEnabled": false,
  "directAccessGrantsEnabled": false
}
```

## PostgreSQL

### Décisions P0

```text
PostgreSQL back-only
pas de port 5432 exposé staging/prod
port 5432 exposable seulement via override local
scram-sha-256
TLS interne off en P0
logs vers stderr Docker
config conservatrice
Unleash DB optionnelle/post-v0
```

### `pg_hba.conf` P0

Utiliser Docker private ranges pour éviter CIDR fragile :

```conf
local   all             postgres                                peer
local   all             all                                     scram-sha-256
host    all             all             127.0.0.1/32            scram-sha-256
host    all             all             ::1/128                 scram-sha-256
host    all             all             172.16.0.0/12           scram-sha-256
host    all             all             10.0.0.0/8              scram-sha-256
host    all             all             192.168.0.0/16          scram-sha-256
```

Sécurité réelle : Docker network `back` + aucun port public.

### `postgresql.conf` P0

Conservateur :

```conf
listen_addresses = '*'
port = 5432
password_encryption = scram-sha-256
ssl = off
max_connections = 100
shared_buffers = 256MB
work_mem = 16MB
maintenance_work_mem = 128MB
effective_cache_size = 1GB
random_page_cost = 1.1
wal_level = replica
max_wal_size = 1GB
min_wal_size = 80MB
checkpoint_completion_target = 0.9
autovacuum = on
track_counts = on
timezone = 'UTC'
log_timezone = 'UTC'
log_destination = 'stderr'
logging_collector = off
log_min_duration_statement = 1000
log_checkpoints = on
log_connections = off
log_disconnections = off
log_lock_waits = on
log_temp_files = 0
track_io_timing = on
track_activity_query_size = 2048
```

### Init DB

Required v0 :

```text
KC_DB_NAME KC_DB_USERNAME KC_DB_PASSWORD
APP_DB_NAME APP_DB_USER APP_DB_PASSWORD
```

Optional post-v0 :

```text
UNLEASH_DB_NAME UNLEASH_DB_USER UNLEASH_DB_PASSWORD
```

## Redis

Décisions :

```text
Redis back-only
jamais edge
password obligatoire staging/prod
password optionnel dev
healthcheck compatible requirepass
appendonly yes
everysec
```

Healthcheck :

```yaml
healthcheck:
  test:
    [
      'CMD-SHELL',
      'if [ -n "$$REDIS_PASSWORD" ]; then redis-cli -a "$$REDIS_PASSWORD" --no-auth-warning ping; else redis-cli ping; fi',
    ]
```

## API Docker startup

À stabiliser après P0/P0+ vert.

Checklist :

- API sur `edge + back`.
- Datasource Docker : `jdbc:postgresql://postgres:5432/...`.
- Redis : `redis://redis:6379` ou Spring properties équivalentes.
- Issuer local : `https://auth.localtest.me/realms/tchalanet`.
- En local Docker, ajouter si nécessaire :

```yaml
extra_hosts:
  - 'auth.localtest.me:host-gateway'
```

- Désactiver post-v0 :

```text
TCH_SEARCH_ENABLED=false
TCH_FLAGS_ENABLED=false
TCH_ANALYTICS_ENABLED=false
```

## Env/secrets

```text
compose.env = interpolation Docker Compose / image tags / noms réseau
.env.merged = runtime non-secret généré
.secrets    = runtime secret
```

Règles :

- `merge-env.sh` exclut `compose.env`.
- `merge-env.sh` exclut `.secrets`.
- Les services runtime chargent `.env.merged` + `.secrets`.
- Les secrets ne sont jamais commit.

## Scripts

Organisation cible :

```text
scripts/
├── docker/
├── utils/
├── hcloud/
├── remote/
├── keycloak/
└── doppler/
```

Créer inventaire avec statuts :

```text
KEEP_PUBLIC
KEEP_INTERNAL
LEGACY
DELETE_LATER
```

Ne pas supprimer brutalement. Avant déplacement :

```bash
grep -R "script-name.sh" -n Makefile scripts docs compose
```

## Hetzner staging disposable

Décision coût : sans client payant, ne pas garder staging persistant.

Important : stop/start ne doit pas être considéré comme stratégie de réduction des coûts. Le staging doit être recréable et supprimable.

Flow :

```bash
make staging-create
make staging-up
make staging-smoke
make staging-backup
make staging-destroy
```

Restauration :

```bash
make staging-create
make staging-restore-latest
make staging-up
make staging-smoke
```

### Ce que `staging-create` prépare

- réseau Hetzner si nécessaire ;
- firewall ;
- VM ;
- bootstrap Docker ;
- `/opt/tchalanet-infra` ;
- networks Docker ;
- envs ;
- Traefik ACME dir ;
- DNS ou instruction DNS.

### Ce que `staging-destroy` exige

- confirmation explicite ;
- backup Postgres avant destroy, sauf override ;
- ne détruit jamais prod ;
- documente volumes supprimés.

## Budget

Objectif sans client : coût minimal.

```text
Phase 1 : Vercel Free pour preview/dev non-commercial, Hetzner staging créé au besoin.
Phase 2 : staging + prod seulement si client/pilote.
Plafond long terme visé : 150 USD/mois, mais pas avant client.
```

Vercel Free : seulement previews/dev non-commerciales. Client/staging/prod : Hetzner Docker web ou Vercel Pro plus tard.
