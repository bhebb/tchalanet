# RB-00 — Checklist secrets et variables

Inventaire complet de tous les secrets et variables d'environnement requis pour staging et prod.
À consulter avant de lancer RB-01 ou RB-02.

---

## Quel coffre ? GitHub ou Doppler

Le critère est **qui lit le secret**, pas ce qu'il protège.

| | GitHub Actions Secrets | Doppler |
|---|---|---|
| **Lecteur** | Le workflow CI/CD | Les conteneurs qui tournent sur la VM |
| **Chemin** | `${{ secrets.X }}` dans un `.yml` | `doppler secrets download` → `envs/<env>/.secrets` → `env_file` |
| **Question à se poser** | « GitHub en a-t-il besoin pour *atteindre* ou *déployer* l'infra ? » | « L'application en a-t-elle besoin pour *fonctionner* ? » |
| **Exemples** | `SSH_PRIVATE_KEY`, `SERVER_HOST`, `HCLOUD_TOKEN`, `DOPPLER_TOKEN_*` | `APP_DB_PASSWORD`, `REDIS_PASSWORD`, `EDGE_HMAC_SECRET` |

Un secret que le CI transmet à un script par SSH reste un secret **GitHub** : il
n'est jamais lu par un conteneur applicatif. C'est le cas des credentials de
backup.

### Cas d'école : backup et archive, deux coffres différents

Les deux écrivent dans Cloudflare R2, et pourtant leurs secrets ne vivent pas au
même endroit. Le critère n'est pas la destination, c'est le lecteur.

| | Backup base de données | Archive de données |
| --- | --- | --- |
| **Qui exécute** | Workflow `db-backup.yml` (GitHub Actions, par SSH) | L'API elle-même, dans son conteneur |
| **Coffre** | **GitHub Secrets** | **Doppler** |
| **Secrets** | `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`, `R2_BUCKET`, `BACKUP_AGE_*` | `TCH_ARCHIVE_STORAGE_ENDPOINT`, `TCH_ARCHIVE_STORAGE_ACCESS_KEY_ID`, `TCH_ARCHIVE_STORAGE_SECRET_ACCESS_KEY` |
| **Bucket** | bucket de backup | `tch-archive` |

Le backup est orchestré depuis GitHub : le workflow ouvre une session SSH et
passe les credentials en variables d'environnement au script. Aucun conteneur
applicatif ne les lit jamais — donc GitHub.

L'archive est déclenchée par l'API, qui lit sa configuration Spring au
démarrage. Ces valeurs doivent donc arriver dans l'environnement du conteneur,
c'est-à-dire par Doppler.

Les deux jeux de clés R2 sont **distincts et restreints à leur propre bucket**.
Ce n'est pas de la bureaucratie : une archive purgée est la seule copie
restante des données qu'elle contient, et un token de backup compromis ne doit
pas permettre de l'effacer. Réutiliser un seul token annulerait l'isolation que
justifie le bucket séparé.

Pour l'archive, `TYPE` et `BUCKET` restent committés dans `envs/<env>/api.env` :
ce sont des identifiants, relisibles en revue. Seuls l'endpoint — qui porte
l'identifiant de compte Cloudflare — et les clés vont dans Doppler.

Le cas particulier : `DOPPLER_TOKEN_*` vit dans GitHub et sert précisément à
récupérer le contenu de Doppler. GitHub est donc la racine de confiance ; il ne
contient jamais de secret applicatif en double.

> Un secret ne doit **jamais** exister dans les deux coffres. Deux copies
> divergent tôt ou tard, et c'est la copie que personne ne regarde qui gagne —
> exactement le mécanisme qui a mis staging à terre le 03/08/2026.

---

## GitHub Actions Secrets

Configurer dans : GitHub → Settings → Secrets and variables → Actions → Secrets

| Secret | Staging | Prod | Description |
|---|---|---|---|
| `SSH_PRIVATE_KEY` | ✅ requis | — | Clé privée SSH vers le serveur staging (`~/.ssh/tchalanet_stg`) |
| `SSH_PRIVATE_KEY_PROD` | — | ✅ requis | Clé privée SSH vers le serveur prod (`~/.ssh/tchalanet_prod`) |
| `SERVER_HOST` | optionnel/legacy | — | Ancienne IP statique staging; les workflows courants résolvent `stg-app` via Hetzner |
| `PROD_SERVER_HOST` | — | ✅ requis | IP du serveur Hetzner prod |
| `HCLOUD_TOKEN` | ✅ requis | — | Token API Hetzner Cloud Read+Write pour créer/détruire le serveur staging |
| `DOPPLER_TOKEN_STG` | ✅ requis | — | Service Token Doppler config `stg` (projet `tchalanet`) |
| `DOPPLER_TOKEN_PROD` | — | ✅ requis | Service Token Doppler config `prd` (projet `tchalanet`) |
| `CLOUDFLARE_ACCOUNT_ID` | ✅ requis | ✅ requis | Compte Cloudflare — utilisé par les backups R2 et les déploiements Cloudflare |
| `R2_ACCESS_KEY_ID` | ✅ requis | ✅ requis | Clé d'accès R2 pour les backups DB (voir RB — BACKUP-RESTORE) |
| `R2_SECRET_ACCESS_KEY` | ✅ requis | ✅ requis | Secret R2 correspondant |
| `R2_BUCKET` | ✅ requis | ✅ requis | Bucket de destination des backups |
| `BACKUP_AGE_PUBLIC_KEY` | ✅ requis | ✅ requis | Clé publique `age` — chiffre les backups |
| `BACKUP_AGE_PRIVATE_KEY` | ✅ requis | ✅ requis | Clé privée `age` — répétition de restauration |

> Les secrets d'**archive** ne figurent pas dans ce tableau : ils vivent dans
> Doppler, pas ici. Voir la section « Cas d'école » ci-dessus.

**À obtenir :**
- `SSH_PRIVATE_KEY` : `cat ~/.ssh/tchalanet_stg`
- `HCLOUD_TOKEN` : Hetzner Cloud Console → projet staging → Security → API Tokens → Generate API token (`Read & Write`)
- `DOPPLER_TOKEN_*` : Doppler dashboard → projet `tchalanet` → config `stg`/`prd` → Access → Generate Service Token
- `R2_ACCESS_KEY_ID` / `R2_SECRET_ACCESS_KEY` : Cloudflare dashboard → R2 → **Manage API tokens** → Create token (Object Read & Write, restreint au bucket de backup). Ce n'est **pas** `CLOUDFLARE_API_TOKEN` : l'accès objet S3 exige un token R2 dédié.
- `BACKUP_AGE_*` : `age-keygen -o backup-age.key` → la ligne `# public key:` va dans `BACKUP_AGE_PUBLIC_KEY`, le fichier entier dans `BACKUP_AGE_PRIVATE_KEY`. Conserver la clé privée aussi dans le gestionnaire de mots de passe : la perdre rend tous les backups illisibles.

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
- DB : `SPRING_DATASOURCE_PASSWORD` et `APP_DB_PASSWORD` uniquement
- Redis : `REDIS_PASSWORD`
- JWT/Auth : `JWT_SECRET`, `JWT_ISSUER_URI`
- Firebase Admin : `FIREBASE_PROJECT_ID`, chemin vers `firebase-admin.json`
- Edge : `EDGE_HMAC_SECRET`, `EDGE_API_KEY`
- Logs (Grafana Cloud) : `GRAFANA_LOKI_URL`, `GRAFANA_LOKI_USER`, `GRAFANA_LOKI_PASSWORD`

**Règle DB — mots de passe seulement :** `SPRING_DATASOURCE_URL` et
`SPRING_DATASOURCE_USERNAME` sont committés dans `envs/<env>/.env`, jamais dans Doppler.
`docker-compose-api.yml` charge `env_file` dans l'ordre `.env.merged` puis `.secrets` :
une cible de connexion stockée dans Doppler écrase donc silencieusement la configuration
committée, sans diff ni revue. C'est ce qui a mis staging à terre le 03/08/2026.
Seuls `SPRING_DATASOURCE_PASSWORD` et `APP_DB_PASSWORD` appartiennent à Doppler.

**Obtenir les secrets Grafana Cloud :**
1. Grafana Cloud → Home → My Account → Stack → Loki → Details
2. Copier l'URL (`GRAFANA_LOKI_URL`) et le User (`GRAFANA_LOKI_USER`)
3. Generate an API Token → scope `logs:write` → c'est `GRAFANA_LOKI_PASSWORD`

---

## Mobile — Secrets GitHub Actions

Requis pour le workflow manuel `.github/workflows/mobile-distribute-staging.yml`
(RB-03) :

| Secret | Description |
|---|---|
| `FIREBASE_ADMIN_JSON_BASE64` | JSON base64 du compte de service Firebase avec rôle App Distribution Admin |
| `TCH_ANDROID_KEYSTORE_BASE64` | Keystore Android release encodé base64 |
| `TCH_ANDROID_KEYSTORE_PASSWORD` | Mot de passe du keystore |
| `TCH_ANDROID_KEY_ALIAS` | Alias de la clé release |
| `TCH_ANDROID_KEY_PASSWORD` | Mot de passe de la clé release |

| Variable | Description |
|---|---|
| `FIREBASE_ANDROID_APP_ID` | App ID Android Firebase (`1:1050094456835:android:afb4836a45c441769a3e36` si différent du défaut workflow) |

**À créer dans Firebase Console :** service account avec rôle `Firebase App Distribution Admin` → Download JSON key → encoder en base64 pour `FIREBASE_ADMIN_JSON_BASE64`.

**Futur Google Play :** prévoir un secret séparé `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_BASE64` quand le workflow Play Console sera créé.

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
[ ] HCLOUD_TOKEN       → GitHub Secrets
[ ] DOPPLER_TOKEN_STG  → GitHub Secrets

# CF Pages (staging preview env)
[ ] TCH_API_BASE_URL   → https://api.stg.tchalanet.com
[ ] TCH_EDGE_BASE_URL  → https://edge.stg.tchalanet.com

# Optionnel prod (plus tard)
[ ] SSH_PRIVATE_KEY_PROD → GitHub Secrets
[ ] PROD_SERVER_HOST     → GitHub Secrets
[ ] DOPPLER_TOKEN_PROD   → GitHub Secrets
```
