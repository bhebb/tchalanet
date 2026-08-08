# RB-07 — Mise en production (MEP)

Runbook maître pour le premier go-live production de Tchalanet.
Chaque étape pointe vers le runbook détaillé existant.

**Durée estimée :** 2-3 heures (hors DNS propagation et CONATEL si applicable).

---

## Workflow de déploiement prod

Le déploiement production utilise la **promotion d'image** — on ne build jamais
en prod. Le workflow `Deploy Runtime Services` (`deploy-infra-runtime.yml`)
applique cette règle :

```
Code → CI verte → Build image stg (sha-xxx) → Deploy staging → Smoke OK
   → Promote: même sha-xxx → Deploy prod (build interdit, gate staging)
```

Le job `verify-production-promotion` vérifie que le tag d'image a un
déploiement staging réussi avant d'autoriser le déploiement prod. Le tag
`latest` est interdit.

**Procédure :**
1. `Deploy Runtime Services` → `target_environment: staging`, `build_api: true`,
   `build_edge: true` → builds les images `sha-<commit>` et les déploie en staging.
2. Smoke staging ([RB-04 §5](./RB-04-release-rollback.md)).
3. `Deploy Runtime Services` → `target_environment: production`,
   `api_image_tag: sha-<même commit>`, `edge_image_tag: sha-<même commit>`.
   Build est interdit — les mêmes images GHCR passent directement.

---

## Phase 1 — Secrets et variables

→ [RB-00 — Checklist secrets](./RB-00-secrets-checklist.md)

### 1.1 GitHub Actions Secrets (manquants pour prod)

| Secret | Action |
|---|---|
| `SSH_PRIVATE_KEY_PROD` | `cat ~/.ssh/tchalanet_prod` → GitHub Secrets |
| `PROD_SERVER_HOST` | IP du serveur Hetzner prod |
| `DOPPLER_TOKEN_PROD` | Doppler → `tchalanet` → config `prd` → Generate Service Token |

### 1.2 Doppler `prd` — Secrets manquants

Comparaison `stg` vs `prd` — ces secrets existent en staging mais pas en prod :

| Catégorie | Secrets à créer |
|---|---|
| **DB** | `APP_DB_PASSWORD`, `DATABASE_PASSWORD`, `POSTGRES_PASSWORD`, `SPRING_DATASOURCE_PASSWORD` |
| **Redis** | `REDIS_PASSWORD` |
| **Firebase** | `FIREBASE_ADMIN_JSON_BASE64`, `FIREBASE_CREDENTIALS_PATH`, `FIREBASE_PROJECT_ID`, `TCH_IDENTITY_PROVIDER` |
| **Edge** | `EDGE_HMAC_SECRET` |
| **Email** | `BREVO_API_KEY`, `EMAIL_ENABLED`, `EMAIL_FROM_ADDRESS`, `EMAIL_FROM_NAME`, `EMAIL_PROVIDER` |
| **Slack** | `SLACK_ENABLED`, `SLACK_WEBHOOK_*` (5 canaux) |
| **Loterie** | `TCH_US_LOTTERY_PROXY_SECRET`, `TCH_US_LOTTERY_PROXY_URL` |

> Ne PAS copier les valeurs staging. Générer des mots de passe frais pour
> prod (DB, Redis, EDGE_HMAC). Firebase : même projet si partagé, sinon
> créer un projet Firebase prod séparé.

### 1.3 Nettoyage

| Secret GitHub | Raison |
|---|---|
| `LOTTERY_PROXY_SHARED_SECRET` | Proxy loterie supprimé (commit `14515f2`) |

### 1.4 Variables GitHub manquantes

| Variable | Valeur |
|---|---|
| `TCH_TERMINAL_EMAIL_DOMAIN_PROD` | `terminal.tchalanet.com` |

---

## Phase 2 — Hardening prod

### 2.1 Traefik

L'existant est solide : HTTPS redirect, Let's Encrypt, HSTS, XSS filter,
nosniff, frameDeny, gzip. Ce qui manque pour prod :

**Rate limiting** — ajouter un middleware dans
`traefik/dynamic-src/common/02-middlewares.yaml` :

```yaml
rate-limit:
  rateLimit:
    average: 100
    burst: 200
    period: 1s
```

Et l'appliquer au router `api` dans `traefik/env/prod.yaml` :

```yaml
middlewares: [secure-headers@file, gzip@file, rate-limit@file]
```

**Dashboard prod** — le router `traefik-dash` dans `traefik/env/prod.yaml`
expose le dashboard sur `traefik.tchalanet.com` sans auth. Options :
- Supprimer le router en prod (recommandé V0).
- Ou ajouter un middleware `basicAuth` avec credentials hashés.

**Maintenance mode** — préparer un middleware qui renvoie 503 avec une page
statique, activable en renommant un fichier :

```yaml
# traefik/dynamic-src/common/03-maintenance.yaml.disabled
maintenance:
  headers:
    customResponseHeaders:
      Retry-After: "3600"
```

Pour activer : `mv 03-maintenance.yaml.disabled 03-maintenance.yaml` et
remplacer le middleware du router API. Traefik recharge automatiquement
(`watch: true`).

### 2.2 Postgres

`postgresql.conf` existant est bon (`data-checksums`, scram-sha-256, WAL, autovacuum, slow query 1s).

**Port exposé** — le compose expose `${POSTGRES_HOST_PORT:-5432}:5432` pour
le dev IDE. En prod, le supprimer ou forcer `POSTGRES_HOST_PORT` à vide dans
`envs/prod/compose.env`. Seul le réseau Docker `back` doit y accéder.

**SSL intra-Docker** — `ssl = off` est acceptable tant que Postgres n'est
joignable que via le réseau Docker interne (`back-prod`). Traefik gère le TLS
externe.

**Tuning prod** (si le serveur a plus de RAM que le staging cpx21) — ajuster
`shared_buffers`, `effective_cache_size`, `work_mem` dans un override
`envs/prod/postgresql-prod.conf` ou via variables compose.

### 2.3 Redis

Existant bon : password, AOF, maxmemory 256M, LRU, resource limits.

**Port exposé** — même chose que Postgres, supprimer `ports:
"${REDIS_HOST_PORT:-6379}:6379"` en prod.

**Persistence prod** — `appendfsync everysec` est le bon défaut. Redis est un
cache (`allkeys-lru`) — la perte complète est tolérable (l'API reconstruit le
cache).

### 2.4 Compose — ports à verrouiller en prod

Ajouter dans `envs/prod/compose.env` :

```bash
# Disable host-port exposure in prod (services only accessible via Docker network)
POSTGRES_HOST_PORT=
REDIS_HOST_PORT=
```

Et adapter les compose files pour ne pas bind si la variable est vide, ou
utiliser un override compose prod (`docker-compose-prod-overrides.yml`) qui
supprime les port mappings.

---

## Phase 3 — Serveur Hetzner prod

→ [RB-01 — Provisionnement serveur](./RB-01-staging-provision.md) (adapter pour prod)
→ [HETZNER.md](../HETZNER.md)

- [ ] Créer le serveur Hetzner prod
- [ ] Installer Docker + Docker Compose
- [ ] Cloner `tchalanet-infra`
- [ ] Configurer les réseaux Docker (`back-prod`, `edge-prod`)
- [ ] Déployer les certificats TLS (Traefik + Let's Encrypt auto)
- [ ] `FIREBASE_CREDENTIALS_HOST_PATH` dans `envs/prod/compose.env`

---

## Phase 4 — DNS Cloudflare

- [ ] Enregistrements A vers le serveur prod :
  - `api.tchalanet.com` → `<IP prod>` (proxy orange)
  - `edge.tchalanet.com` → `<IP prod>` (proxy orange)
- [ ] Les portails web sont sur CF Pages — voir Phase 6.
- [ ] Supprimer ou ne pas créer `traefik.tchalanet.com` en prod (§2.1).

---

## Phase 5 — Déploiement backend (promotion d'image)

Procédure dans la section « Workflow » en haut de ce document.

- [ ] `Deploy Runtime Services` staging → build + deploy → smoke OK
- [ ] Backup DB staging de référence (via `db-backup.yml`)
- [ ] `Deploy Runtime Services` production → promote même tag
- [ ] Vérifier Flyway migrations (logs API au démarrage)
- [ ] Smoke prod ([RB-04 §5](./RB-04-release-rollback.md)) :
  - [ ] `GET /actuator/health` → `UP`
  - [ ] Login admin OK
  - [ ] Login SellerTerminal OK
  - [ ] Vente ticket test OK

---

## Phase 6 — Déploiement web (CF Pages)

→ [RB-02 — Web CF Pages](./RB-02-web-cf-pages.md)

- [ ] Variables CF Pages **production** :

| Variable | Valeur |
|---|---|
| `TCH_API_BASE_URL` | `https://api.tchalanet.com` |
| `TCH_EDGE_BASE_URL` | `https://edge.tchalanet.com` |

- [ ] Domaines personnalisés CF Pages :
  - `app.tchalanet.com` → public-portal
  - `admin.tchalanet.com` → admin-portal
  - `portal.tchalanet.com` → platform-portal
- [ ] Build production déclenché (push `main`)
- [ ] Vérifier chargement et connexion API prod

---

## Phase 7 — Observabilité

→ [OBSERVABILITY.md](../OBSERVABILITY.md)

- [ ] `SPRING_PROFILES_ACTIVE=prod,grafana-cloud` (déjà dans `envs/prod/compose.env`)
- [ ] `OTEL_SERVICE_NAME=tchalanet-api-prod` (déjà dans Doppler `prd`)
- [ ] Traces visibles dans Grafana Cloud Tempo
- [ ] Logs WARN/ERROR dans Grafana Cloud Loki
- [ ] Dashboard : https://silverkiwi1488.grafana.net

---

## Phase 8 — Backups

→ [BACKUP-RESTORE.md](../BACKUP-RESTORE.md)
→ [RB-06 — Disaster Recovery](./RB-06-disaster-recovery.md)

- [ ] Workflow `db-backup.yml` fonctionne pour prod
- [ ] Backup manuel → vert (chiffré dans R2)
- [ ] Planifier backup automatique (cron schedule dans le workflow)
- [ ] Tester une restauration (sur un env jetable, pas en prod)

---

## Phase 9 — Notifications (Edge)

→ [EDGE-SERVICE.md](../services/EDGE-SERVICE.md)

- [ ] `SLACK_ENABLED=true` dans Doppler `prd`
- [ ] 5 webhooks Slack configurés
- [ ] Messages préfixés `[PROD]` (PR #576)
- [ ] Email : `EMAIL_ENABLED` selon besoin, `BREVO_API_KEY` prêt

---

## Switch on / Switch off

### Mettre en maintenance (switch off)

1. **API** — sur le serveur prod :
   ```bash
   cd /opt/tchalanet-infra
   # Arrêter les services runtime (Postgres/Redis restent up)
   docker compose -f <api-compose> stop api edge-service
   ```
   Traefik renverra 502/503 automatiquement (backend absent).

2. **Maintenance page** — activer le middleware maintenance Traefik (§2.1) pour
   renvoyer une réponse propre au lieu de 502.

3. **CF Pages** — les portails restent accessibles (static), seuls les appels API
   échouent. Pour une maintenance complète, configurer une page CF Pages
   `_redirects` qui redirige tout vers `/maintenance.html`.

### Remettre en service (switch on)

1. Désactiver le middleware maintenance Traefik.
2. `docker compose -f <api-compose> start api edge-service`
3. Attendre le healthcheck (`/actuator/health` → UP).
4. Smoke rapide : vente test + login.

---

## Rollback

→ [RB-04 — Release smoke & rollback](./RB-04-release-rollback.md)

| Situation | Action |
|---|---|
| API ne démarre pas | **Rollback applicatif** : `Deploy Runtime Services` prod avec le tag connu-bon précédent |
| Régression fonctionnelle | **Rollback applicatif** (même procédure) |
| Migration destructive/ratée | **Rollback DB** (RB-04 §8) + rollback applicatif |
| Cache incohérent | **Clear cache** via `CacheOpsController` (RB-04 §9) |

Le rollback applicatif est une re-promotion : lancer `Deploy Runtime Services`
avec `target_environment: production` et le tag `sha-<commit précédent>`. Le
gate staging est satisfait car ce tag avait déjà été validé.

---

## Monitoring

| Quoi | Outil | URL / Commande |
|---|---|---|
| Health API | Traefik healthcheck + Grafana | `curl https://api.tchalanet.com/api/v1/actuator/health` |
| Traces | Grafana Cloud Tempo | https://silverkiwi1488.grafana.net |
| Logs | Grafana Cloud Loki | idem |
| Uptime externe | À mettre en place (UptimeRobot / Grafana Synthetic) | — |
| Slack alertes | Edge service → canal `ops-alerts` | Messages `[PROD] [ERROR] …` |
| Backups | Workflow `db-backup.yml` | Vérifier le dernier run dans GitHub Actions |

**Alertes à configurer dans Grafana Cloud :**
- API health DOWN (pas de trace depuis >5 min)
- Taux d'erreur 5xx > 5% sur 5 min
- Backup workflow en échec

---

## Post-MEP

- [ ] Renseigner contacts d'astreinte dans RB-04 §10
- [ ] Supprimer `LOTTERY_PROXY_SHARED_SECRET` de GitHub Secrets
- [ ] Configurer monitoring uptime externe
- [ ] Planifier rotation mots de passe prod (DB, Redis, HMAC) — tous les 90 jours
- [ ] Vérifier les ports exposés (Postgres, Redis) sont bien fermés en prod
- [ ] Premier backup post-MEP comme baseline
