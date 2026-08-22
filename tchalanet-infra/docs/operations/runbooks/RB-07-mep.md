# RB-07 — Mise en production (MEP)

Runbook maître pour le premier go-live production de Tchalanet.
Chaque étape pointe vers le runbook détaillé existant.

**Durée estimée :** 2-3 heures (hors DNS propagation).

**URL healthcheck canonique :** `https://api.tchalanet.com/api/v1/actuator/health`
— c'est l'URL unique utilisée partout : smoke, monitoring, Grafana Synthetic,
runbooks. En interne (Traefik, conteneur) : `http://127.0.0.1:8080/api/v1/actuator/health`.

---

## GO / NO-GO

Promotion vers prod **uniquement si** :

- [ ] CI du SHA cible verte (tous workflows : `server-pr`, `web-pr`, `edge-pr`)
- [ ] SHA déployé et smoke-testé en staging (gate `verify-production-promotion`)
- [ ] Secrets prod complets dans Doppler `prd` + GitHub Secrets (Phase 1)
- [x] Postgres et Redis non exposés sur le host (`compose/docker-compose-prod-overrides.yml` créé, inclus dans le deploy script pour `ENV=prod`)
- [x] Dashboard Traefik inaccessible publiquement (router `traefik-dash` supprimé de `traefik/env/prod.yaml`)
- [ ] Backup prod opérationnel et testé (Phase 8)
- [ ] Observabilité prod visible dans Grafana (traces + logs, Phase 7)
- [ ] Canal mobile prod décidé et prêt (Phase 10) :
  - Google Play Console configuré pour `com.tchalanet.mobile`.
  - Track `internal` ou `closed` prêt pour les machines clients pilotes.
  - Versioning mobile automatique prêt ou `versionCode` prod validé à la main.
  - Build mobile pointe vers `https://api.tchalanet.com/api/v1` et
    `terminal.tchalanet.com`.
- [ ] Healthcheck canonique vert sur staging : `curl -sf https://api.stg.tchalanet.com/api/v1/actuator/health`
- [ ] SHA de rollback identifié (tag connu-bon précédent)
- [ ] Responsable MEP + responsable rollback identifiés

**Un seul item non coché = NO-GO.**

---

## Roadmap produit/technique

La roadmap fonctionnelle et technique pour obtenir une version testable sur
machines clients fin août est maintenue hors runbook :

→ [`openspec/roadmap-2026-08-client-pilot.md`](../../../../openspec/roadmap-2026-08-client-pilot.md)

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

Comparaison `stg` vs `prd` — ces secrets existent en staging mais pas en prod.

**Secrets DB — mapping des consommateurs :**

Les 4 noms servent 3 consommateurs différents. Un seul mot de passe à générer,
mais les 4 entrées doivent exister dans Doppler `prd` avec la même valeur.

| Nom Doppler | Consommateur | Rôle |
|---|---|---|
| `POSTGRES_PASSWORD` | Conteneur `postgres` | Mot de passe du superuser `postgres` (bootstrap) |
| `APP_DB_PASSWORD` | Script `postgres-init.sh` | Crée/met à jour le user `app_user` dans la DB `tchalanet_db` |
| `SPRING_DATASOURCE_PASSWORD` | Conteneur `api` (Spring Boot) | Connexion JDBC de l'API au user `app_user` |
| `DATABASE_PASSWORD` | Legacy / alias | À vérifier — supprimer si non consommé |

> `APP_DB_PASSWORD` et `SPRING_DATASOURCE_PASSWORD` doivent avoir la même
> valeur (c'est le même user `app_user`). `POSTGRES_PASSWORD` est le superuser,
> il peut (et devrait) être différent. Lors de la rotation, les 3 consommateurs
> doivent être mis à jour ensemble.

**Autres secrets manquants :**

| Catégorie | Secrets à créer |
|---|---|
| **Redis** | `REDIS_PASSWORD` |
| **Firebase** | `FIREBASE_ADMIN_JSON_BASE64`, `FIREBASE_CREDENTIALS_PATH`, `FIREBASE_PROJECT_ID`, `TCH_IDENTITY_PROVIDER` |
| **Edge** | `EDGE_HMAC_SECRET` |
| **Email** | `BREVO_API_KEY`, `EMAIL_ENABLED`, `EMAIL_FROM_ADDRESS`, `EMAIL_FROM_NAME`, `EMAIL_PROVIDER` |
| **Slack** | `SLACK_ENABLED`, `SLACK_WEBHOOK_*` (5 canaux) |
| **Loterie** | `TCH_US_LOTTERY_PROXY_SECRET`, `TCH_US_LOTTERY_PROXY_URL` |
| **Mobile** | `TCH_TERMINAL_EMAIL_DOMAIN_PROD`, `FIREBASE_ANDROID_APP_ID_PROD` si app Firebase prod séparée |

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
| `TCH_MOBILE_API_BASE_URL_PROD` | `https://api.tchalanet.com/api/v1` |
| `FIREBASE_ANDROID_APP_ID_PROD` | App ID Android prod si différent du staging |

---

## Phase 2 — Hardening prod

### 2.1 Traefik

L'existant est solide : HTTPS redirect, Let's Encrypt, HSTS, XSS filter,
nosniff, frameDeny, gzip. Ce qui manque pour prod :

**Rate limiting** — ajouter un middleware dans
`traefik/dynamic-src/common/02-middlewares.yaml` :

```yaml
rate-limit-api:
  rateLimit:
    average: 100
    burst: 200
    period: 1s

rate-limit-auth:
  rateLimit:
    average: 10
    burst: 20
    period: 1s
```

Valeurs initiales à ajuster après observation du trafic réel. Deux policies :
- `rate-limit-api` : endpoints généraux (résultats, catalogue). 100 req/s
  couvre largement le trafic attendu au lancement.
- `rate-limit-auth` : endpoints de login/token. Plus restrictif pour limiter
  le brute-force.

Appliquer au router `api` dans `traefik/env/prod.yaml` :

```yaml
middlewares: [secure-headers@file, gzip@file, rate-limit-api@file]
```

**Dashboard prod** — **supprimer** le router `traefik-dash` dans
`traefik/env/prod.yaml`. Le dashboard n'a pas besoin d'être accessible en prod.
Pour un diagnostic ponctuel, SSH tunnel (`ssh -L 8080:localhost:8080 prod`).

> **GO/NO-GO** : le dashboard Traefik ne doit pas être accessible publiquement.

**Maintenance mode** — Traefik ne supporte pas nativement le renvoi d'une
réponse 503 statique via un middleware seul. La bonne approche :

1. Préparer un fichier `traefik/maintenance/503.html` avec la page de maintenance.
2. Créer un router qui capture tout le trafic avec une priorité haute et le
   redirige vers un micro-service maintenance (ou utiliser le plugin `ContentType`
   via Traefik local plugins).

En V0, la procédure est plus simple : arrêter les services API/edge (Traefik
renvoie 502), et poser une page de maintenance côté CF Pages si nécessaire
(voir section « Switch on / Switch off »).

### 2.2 Postgres

`postgresql.conf` existant est bon (`data-checksums`, scram-sha-256, WAL,
autovacuum, slow query 1s).

**SSL intra-Docker** — `ssl = off` est acceptable tant que Postgres n'est
joignable que via le réseau Docker interne (`back-prod`). Traefik gère le TLS
externe.

**Tuning prod** (si le serveur a plus de RAM que le staging cpx21) — ajuster
`shared_buffers`, `effective_cache_size`, `work_mem` dans un override
`envs/prod/postgresql-prod.conf` ou via variables compose.

### 2.3 Redis

Existant bon : password, AOF, maxmemory 256M, LRU, resource limits.

**Persistence prod** — `appendfsync everysec` est le bon défaut. Redis est un
cache (`allkeys-lru`) — la perte complète est tolérable (l'API reconstruit le
cache).

### 2.4 Verrouiller les ports en prod — override compose obligatoire

Les compose files exposent les ports Postgres et Redis sur le host pour le dev
IDE. En prod, ces ports **doivent** être supprimés — pas désactivés par une
variable vide (Compose interpole une chaîne vide, il ne supprime pas le
mapping).

Créer `compose/docker-compose-prod-overrides.yml` :

```yaml
services:
  postgres:
    ports: !reset []
  redis:
    ports: !reset []
```

Et l'inclure dans la séquence compose prod (`up-prod`). Le `!reset` de
Compose v2.24+ remplace la liste héritée au lieu de la merger.

Si la version de Compose ne supporte pas `!reset`, utiliser un override
qui bind sur `127.0.0.1` uniquement (accessible seulement via SSH tunnel) :

```yaml
services:
  postgres:
    ports:
      - "127.0.0.1:5432:5432"
  redis:
    ports:
      - "127.0.0.1:6379:6379"
```

> **GO/NO-GO** : Postgres et Redis ne doivent pas être accessibles depuis
> l'extérieur du serveur. Vérifier avec `ss -tlnp | grep -E '5432|6379'` —
> seul `127.0.0.1` ou aucun binding.

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
- [ ] **Ne pas** créer `traefik.tchalanet.com` en prod.

---

## Phase 5 — Déploiement backend (promotion d'image)

Procédure dans la section « Workflow » en haut de ce document.

- [ ] `Deploy Runtime Services` staging → build + deploy → smoke OK
- [ ] Backup DB staging de référence (via `db-backup.yml`)
- [ ] `Deploy Runtime Services` production → promote même tag
- [ ] Vérifier Flyway migrations (logs API au démarrage)
- [ ] Smoke prod ([RB-04 §5](./RB-04-release-rollback.md)) :
  - [ ] Healthcheck : `curl -sf https://api.tchalanet.com/api/v1/actuator/health`
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

> **GO/NO-GO** : l'observabilité doit être opérationnelle et visible avant
> de déclarer la MEP terminée.

---

## Phase 8 — Backups

→ [BACKUP-RESTORE.md](../BACKUP-RESTORE.md)
→ [RB-06 — Disaster Recovery](./RB-06-disaster-recovery.md)

- [ ] Workflow `db-backup.yml` fonctionne pour prod
- [ ] Backup manuel → vert (chiffré dans R2)
- [ ] Planifier backup automatique (cron schedule dans le workflow)
- [ ] Tester une restauration (sur un env jetable, pas en prod)

> **GO/NO-GO** : au moins un backup prod réussi et une restauration testée
> avant d'accepter du trafic réel.

---

## Phase 9 — Notifications (Edge)

→ [EDGE-SERVICE.md](../services/EDGE-SERVICE.md)

- [ ] `SLACK_ENABLED=true` dans Doppler `prd`
- [ ] 5 webhooks Slack configurés
- [ ] Messages préfixés `[PROD]` (PR #576)
- [ ] Email : `EMAIL_ENABLED` selon besoin, `BREVO_API_KEY` prêt

---

## Phase 10 — Mobile Android

→ [RB-03 — Distribution mobile](./RB-03-mobile-distribution.md)
→ [`tchalanet-mobile/docs/RELEASE.md`](../../../../tchalanet-mobile/docs/RELEASE.md)
→ [RB-00 — Checklist secrets](./RB-00-secrets-checklist.md)

Cette phase ne détaille pas la distribution : utiliser RB-03 pour les commandes,
les canaux, le versioning et le rollback mobile. Ici, on garde seulement les
critères MEP.

- [ ] Décision canal actée dans RB-03 :
  - Google Play prêt pour `com.tchalanet.mobile`.
  - Track `internal` ou `closed` prêt pour les machines clients pilotes.
- [ ] Secrets/variables mobile présents dans RB-00 :
  - `TCH_ANDROID_KEYSTORE_*`
  - `FIREBASE_ADMIN_JSON_BASE64`
  - `TCH_MOBILE_API_BASE_URL_PROD`
  - `TCH_TERMINAL_EMAIL_DOMAIN_PROD`
  - `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_BASE64` si Google Play est utilisé.
- [ ] Versioning mobile validé selon RB-03 :
  - `versionName`
  - `versionCode`
  - tag mobile créé ou prêt à être créé après distribution.
- [ ] Build mobile prod/pilote distribué via le canal choisi.
- [ ] Smoke mobile prod effectué :
  - login vendeur ;
  - bootstrap terminal/profil ;
  - tirages vendables ;
  - préparation de vente ;
  - confirmation de vente ;
  - vérification/réimpression ticket ;
  - déconnexion/reconnexion.
- [ ] Rollback mobile compris : publier un nouveau build connu-bon avec
      `versionCode` supérieur, voir RB-03.

---

## Switch on / Switch off

### Mettre en maintenance (switch off)

1. **API** — sur le serveur prod :
   ```bash
   cd /opt/tchalanet-infra
   docker compose -f <api-compose> stop api edge-service
   ```
   Traefik renvoie 502 (backend absent). Postgres/Redis restent up.

2. **CF Pages (optionnel)** — les portails restent accessibles (statique), mais
   les appels API échouent. Pour une maintenance complète, ajouter un fichier
   `_redirects` dans le build CF Pages qui redirige tout vers
   `/maintenance.html` (page statique à préparer dans le repo web).

### Remettre en service (switch on)

1. `docker compose -f <api-compose> start api edge-service`
2. Attendre le healthcheck : `curl -sf https://api.tchalanet.com/api/v1/actuator/health`
3. Smoke rapide : login + vente test.

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
| Health API | Traefik healthcheck + Grafana | `curl -sf https://api.tchalanet.com/api/v1/actuator/health` |
| Traces | Grafana Cloud Tempo | https://silverkiwi1488.grafana.net |
| Logs | Grafana Cloud Loki | idem |
| Uptime externe | À mettre en place (UptimeRobot / Grafana Synthetic) | — |
| Slack alertes | Edge service → canal `ops-alerts` | Messages `[PROD] [ERROR] …` |
| Backups | Workflow `db-backup.yml` | Vérifier le dernier run dans GitHub Actions |
| Mobile | Google Play / Firebase App Distribution | Track/release active + version distribuée |

**Alertes à configurer dans Grafana Cloud :**
- API health DOWN (pas de trace depuis >5 min)
- Taux d'erreur 5xx > 5% sur 5 min
- Backup workflow en échec

---

## Post-MEP

- [ ] Renseigner contacts d'astreinte dans RB-04 §10
- [ ] Supprimer `LOTTERY_PROXY_SHARED_SECRET` de GitHub Secrets
- [ ] Vérifier `DATABASE_PASSWORD` dans Doppler — s'il n'est pas consommé, le supprimer
- [ ] Configurer monitoring uptime externe
- [ ] Documenter la version mobile distribuée (`versionName`, `versionCode`, canal, SHA)
- [ ] Planifier rotation mots de passe prod (DB, Redis, HMAC) — tous les 90 jours
- [ ] Vérifier les ports : `ss -tlnp | grep -E '5432|6379'` — aucun binding public
- [ ] Premier backup post-MEP comme baseline
