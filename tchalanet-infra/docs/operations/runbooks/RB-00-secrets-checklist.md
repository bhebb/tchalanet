# RB-00 — Checklist secrets et variables

Inventaire complet de tous les secrets et variables d'environnement requis pour staging et prod.
À consulter avant de lancer RB-01 ou RB-02.

---

## GitHub Actions Secrets

Configurer dans : GitHub → Settings → Secrets and variables → Actions → Secrets

| Secret | Staging | Prod | Description |
|---|---|---|---|
| `SSH_PRIVATE_KEY` | ✅ requis | — | Clé privée SSH vers le serveur staging (`~/.ssh/tchalanet_stg`) |
| `SSH_PRIVATE_KEY_PROD` | — | ✅ requis | Clé privée SSH vers le serveur prod (`~/.ssh/tchalanet_prod`) |
| `SERVER_HOST` | ✅ requis | — | IP du serveur Hetzner staging |
| `PROD_SERVER_HOST` | — | ✅ requis | IP du serveur Hetzner prod |
| `DOPPLER_TOKEN_STG` | ✅ requis | — | Service Token Doppler config `stg` (projet `tchalanet`) |
| `DOPPLER_TOKEN_PROD` | — | ✅ requis | Service Token Doppler config `prd` (projet `tchalanet`) |

**À obtenir :**
- `SSH_PRIVATE_KEY` : `cat ~/.ssh/tchalanet_stg`
- `DOPPLER_TOKEN_*` : Doppler dashboard → projet `tchalanet` → config `stg`/`prd` → Access → Generate Service Token

**Secrets obsolètes à supprimer (après migration CF Pages) :**
- `VERCEL_TOKEN`
- `VERCEL_ORG_ID`
- `VERCEL_PROJECT_ID`

---

## GitHub Actions Variables (non sensibles)

Configurer dans : GitHub → Settings → Secrets and variables → Actions → Variables

| Variable | Valeur | Description |
|---|---|---|
| `TCH_API_BASE_URL_STG` | `https://api.stg.tchalanet.com` | Utilisé par le workflow mobile (RB-03) |
| `TCH_TERMINAL_EMAIL_DOMAIN_STG` | `terminal.stg.tchalanet.com` | Domaine email terminaux staging |

---

## Cloudflare Pages — Variables par projet

Configurer dans : CF Pages → projet → Settings → Environment variables

### Projet `tchalanet-public-portal`

| Variable | Production | Preview (staging) |
|---|---|---|
| `TCH_API_BASE_URL` | `https://api.tchalanet.com` | `https://api.stg.tchalanet.com` |
| `TCH_EDGE_BASE_URL` | `https://edge.tchalanet.com` | `https://edge.stg.tchalanet.com` |
| `TCH_ADMIN_PORTAL_URL` | `https://admin.tchalanet.com` | `https://admin.stg.tchalanet.com` |
| `TCH_PLATFORM_PORTAL_URL` | `https://portal.tchalanet.com` | `https://portal.stg.tchalanet.com` |

### Projet `tchalanet-admin-portal`

| Variable | Production | Preview (staging) |
|---|---|---|
| `TCH_API_BASE_URL` | `https://api.tchalanet.com` | `https://api.stg.tchalanet.com` |
| `TCH_EDGE_BASE_URL` | `https://edge.tchalanet.com` | `https://edge.stg.tchalanet.com` |

### Projet `tchalanet-platform-portal`

| Variable | Production | Preview (staging) |
|---|---|---|
| `TCH_API_BASE_URL` | `https://api.tchalanet.com` | `https://api.stg.tchalanet.com` |
| `TCH_EDGE_BASE_URL` | `https://edge.tchalanet.com` | `https://edge.stg.tchalanet.com` |

> **Firebase** : la config Firebase (apiKey, projectId, etc.) est déjà dans le code (`environment.prod.ts`) — c'est un identifiant client public, pas un secret. Ne pas la dupliquer ici.

> **Thèmes** : `theme-presets.registry.ts` et `token-manifest.generated.ts` sont commitués dans le repo. Aucune variable de build nécessaire.

---

## Doppler — Secrets d'infrastructure (sur serveur Hetzner)

Configurés dans le projet Doppler `tchalanet`, configs `stg` / `prd`. Téléchargés au déploiement via le workflow CI.

Ces secrets ne sont PAS listés ici pour des raisons de sécurité. Voir Doppler dashboard → projet `tchalanet` → Secrets pour l'inventaire complet.

Catégories attendues :
- DB (Neon) : `SPRING_DATASOURCE_URL` (ex: `jdbc:postgresql://ep-xxx.neon.tech/tchalanet_db?sslmode=require&user=xxx&password=xxx`)
- Redis : `REDIS_PASSWORD`
- JWT/Auth : `JWT_SECRET`, `JWT_ISSUER_URI`
- Firebase Admin : `FIREBASE_PROJECT_ID`, chemin vers `firebase-admin.json`
- Edge : `EDGE_HMAC_SECRET`, `EDGE_API_KEY`
- Logs (Grafana Cloud) : `GRAFANA_LOKI_URL`, `GRAFANA_LOKI_USER`, `GRAFANA_LOKI_PASSWORD`

**Note Neon :** la connection string Neon inclut les credentials dans l'URL — un seul secret `SPRING_DATASOURCE_URL` suffit pour la DB en staging/prod. Pas de `APP_DB_PASSWORD` séparé.

**Obtenir les secrets Grafana Cloud :**
1. Grafana Cloud → Home → My Account → Stack → Loki → Details
2. Copier l'URL (`GRAFANA_LOKI_URL`) et le User (`GRAFANA_LOKI_USER`)
3. Generate an API Token → scope `logs:write` → c'est `GRAFANA_LOKI_PASSWORD`

---

## Mobile — Secrets GitHub Actions

Requis quand le workflow `mobile-distribute.yml` sera créé (RB-03) :

| Secret | Description |
|---|---|
| `FIREBASE_ANDROID_APP_ID` | `1:768000918177:android:5fc04b59928349269aa6e0` (non sensible, peut être une variable) |
| `FIREBASE_SERVICE_ACCOUNT` | JSON du compte de service Firebase (rôle App Distribution Admin) |

**À créer dans Firebase Console :** IAM → Service Accounts → Create → rôle `Firebase App Distribution Admin` → Download JSON key.

---

## Résumé — Ce qui est déjà dans le code (ne pas dupliquer)

| Donnée | Fichier | Raison |
|---|---|---|
| Firebase `apiKey` | `environment.prod.ts` | Identifiant public client, pas un secret |
| Firebase `projectId`, `appId`, etc. | `environment.prod.ts` | Idem — public |
| Theme presets CSS | `theme-presets.registry.ts` | Auto-généré commitué |
| Token manifest | `token-manifest.generated.ts` | Auto-généré commitué |
| GHCR registry URL | `deploy-staging.yml` | Résolu par `github.repository_owner` |

---

## Checklist de vérification avant premier déploiement

```bash
# Staging
[ ] SSH_PRIVATE_KEY    → GitHub Secrets
[ ] SERVER_HOST        → GitHub Secrets
[ ] DOPPLER_TOKEN_STG  → GitHub Secrets

# CF Pages (staging preview env)
[ ] TCH_API_BASE_URL   → https://api.stg.tchalanet.com
[ ] TCH_EDGE_BASE_URL  → https://edge.stg.tchalanet.com

# Optionnel prod (plus tard)
[ ] SSH_PRIVATE_KEY_PROD → GitHub Secrets
[ ] PROD_SERVER_HOST     → GitHub Secrets
[ ] DOPPLER_TOKEN_PROD   → GitHub Secrets
```
