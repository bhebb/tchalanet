# RB-01 — Provisionnement du serveur staging

**Quand utiliser ce runbook :** première mise en service du serveur staging, ou recréation complète après destruction.

**Durée estimée :** 20-30 min (hors propagation DNS).

**Résultat attendu :** serveur Hetzner actif, stack Docker running, smoke test vert, CI/CD automatique câblé sur push `main`.

---

## Prérequis

| Outil | Vérification |
|---|---|
| `hcloud` CLI | `hcloud version` |
| `ssh-keygen` | présent par défaut macOS |
| Token Hetzner (Read+Write) | [console.hetzner.cloud](https://console.hetzner.cloud) → Security → API Tokens |
| Accès GitHub repo (admin) | pour configurer les Secrets Actions |
| Token Doppler `stg` (Service Token) | Doppler dashboard → projet `tchalanet` → config `stg` → Access |

```bash
# Configurer hcloud (une seule fois par machine)
export HCLOUD_TOKEN="<token-hetzner>"
hcloud server list   # doit répondre sans erreur
```

---

## Étape 1 — Clé SSH dédiée staging

```bash
# Générer (une seule fois — réutiliser si déjà existante)
ssh-keygen -t ed25519 -C "tchalanet-stg" -f ~/.ssh/tchalanet_stg

# Enregistrer chez Hetzner
hcloud ssh-key create --name tchalanet_stg --public-key-from-file ~/.ssh/tchalanet_stg.pub

# Vérifier
hcloud ssh-key list
```

Ajouter dans `~/.ssh/config` :
```
Host tchalanet_stg
  HostName <IP_STG>           # mettre à jour après l'étape 2
  User tch
  IdentityFile ~/.ssh/tchalanet_stg
  StrictHostKeyChecking accept-new
```

---

## Étape 2 — Créer le serveur Hetzner

```bash
cd tchalanet-infra

./scripts/hcloud/staging-create.sh
```

Ce script enchaîne : réseau privé → firewall → cloud-init → serveur `stg-app` (CX23 / Ubuntu 24.04 / nbg1).

Récupérer l'IP :
```bash
IP=$(hcloud server describe stg-app -o json | jq -r '.public_net.ipv4.ip')
echo "IP staging : $IP"
```

Mettre à jour `~/.ssh/config` avec l'IP ci-dessus.

**Checkpoint :** `hcloud server list` affiche `stg-app` en `running`.

---

## Étape 3 — DNS Cloudflare

Dans le dashboard Cloudflare (domaine `tchalanet.com`) → DNS → ajouter :

| Type | Nom | Valeur | Proxy |
|---|---|---|---|
| A | `api.stg` | `<IP_STG>` | DNS only (gris) |
| A | `edge.stg` | `<IP_STG>` | DNS only (gris) |
| A | `*.stg` | `<IP_STG>` | DNS only (gris) |

> Mettre **DNS only** (pas le proxy orange) — Traefik gère TLS via Let's Encrypt sur le port 443 directement.

Vérification propagation (~1 min en général avec Cloudflare) :
```bash
dig +short api.stg.tchalanet.com
# doit retourner <IP_STG>
```

---

## Étape 4 — Bootstrap Docker sur la VM

```bash
# Copier l'infra sur la VM
./scripts/remote/push-infra.sh "$IP"

# Bootstrap (installe Docker, crée les réseaux compose, prépare Traefik)
ssh tch@$IP 'cd /opt/tchalanet-infra && ./scripts/remote/01-bootstrap.sh staging'
```

**Checkpoint :** `ssh tch@$IP 'docker version'` répond sans erreur.

---

## Étape 5 — Secrets Doppler (premier déploiement)

Sur la VM :
```bash
ssh tch@$IP
cd /opt/tchalanet-infra

export DOPPLER_TOKEN="<service-token-stg>"

docker run --rm \
  -e DOPPLER_TOKEN="$DOPPLER_TOKEN" \
  -v "$PWD":/work -w /work \
  dopplerhq/cli:3.72 \
  sh -lc 'doppler secrets download --format env --project tchalanet --config staging \
    > envs/staging/.secrets && chmod 600 envs/staging/.secrets'

make env-merge ENV=staging
```

---

## Étape 6 — Premier déploiement manuel

```bash
# Toujours sur la VM
make up-staging
```

Vérifications :
```bash
docker ps --format "table {{.Names}}\t{{.Status}}"
# Doit lister : tchl-traefik-stg, tchl-postgres-stg, tchl-redis-stg, tchl-api-stg, tchl-edge-stg
```

---

## Étape 7 — Smoke test

```bash
# Depuis la VM ou votre machine locale (après propagation DNS)
make smoke-staging

# Ou manuellement :
curl -fsS https://api.stg.tchalanet.com/actuator/health | jq .status
curl -fsS https://edge.stg.tchalanet.com/health
```

**Checkpoint :** les deux endpoints retournent `"UP"` / `200 OK`.

---

## Étape 8 — Configurer GitHub Secrets

Dans GitHub → Settings → Secrets and variables → Actions → **New repository secret** :

| Secret | Valeur |
|---|---|
| `SSH_PRIVATE_KEY` | contenu de `~/.ssh/tchalanet_stg` (clé privée) |
| `SERVER_HOST` | `<IP_STG>` |
| `DOPPLER_TOKEN_STG` | Service Token Doppler config `stg` |

```bash
# Récupérer la clé privée
cat ~/.ssh/tchalanet_stg
```

> **Sécurité :** ne jamais coller la clé dans un fichier commité. GitHub Secrets la chiffre au repos.

---

## Étape 9 — Activer le CD automatique

Le workflow `deploy-staging.yml` est actuellement **manuel uniquement** (`workflow_dispatch`).

Pour activer le déclencheur automatique sur push `main`, ajouter le bloc `push` dans le workflow :

```yaml
on:
  push:
    branches: [main]
    paths:
      - 'tchalanet-server/**'
      - 'tchalanet-edge-service/**'
      - 'tchalanet-infra/**'
  workflow_dispatch:
    inputs:
      # ... (inchangé)
```

> **À faire :** ouvrir une PR pour modifier `.github/workflows/deploy-staging.yml` avec ce trigger. Voir tâche dans `openspec/changes/`.

En attendant, déclencher manuellement depuis GitHub Actions → Deploy Staging → Run workflow.

---

## Étape 10 — Vérifier le CD end-to-end

1. Faire un commit sur `main` dans `tchalanet-server/` ou `tchalanet-infra/`
2. Vérifier dans GitHub Actions que le workflow `Deploy Staging` se déclenche automatiquement
3. Attendre la fin du job `deploy`
4. Relancer `make smoke-staging`

**Checkpoint final :** pipeline vert + smoke test vert = staging opérationnel en CD.

---

## Rollback

Si le déploiement casse la stack :

```bash
ssh tch@$IP
cd /opt/tchalanet-infra

# Revenir à un IMAGE_TAG connu
IMAGE_TAG=sha-<sha-précédent> make deploy ENV=staging

# Ou redémarrer sans changer les images
make up-staging
```

Pour retrouver les tags disponibles : `ghcr.io` → packages → `tchalanet-api` → versions.

---

## Recréer le serveur (reset complet)

```bash
# Supprimer (irréversible — les volumes Docker sont perdus)
hcloud server delete stg-app

# Recréer depuis l'étape 2
```

> La base de données est dans le volume Docker `tchl-pg-data-stg`. Si Neon est utilisé (PostgreSQL managé), la DB survit à la destruction du serveur.

---

## Troubleshooting

| Symptôme | Cause probable | Action |
|---|---|---|
| `HCLOUD_TOKEN not set` | variable non exportée | `export HCLOUD_TOKEN=...` |
| SSH `Permission denied` | mauvaise clé ou mauvais user | vérifier `~/.ssh/config`, user=`tch` |
| Traefik 404 sur `api.stg.*` | DNS pas encore propagé | `dig +short api.stg.tchalanet.com` |
| `DOPPLER_TOKEN_STG manquant` | Secret GitHub non configuré | vérifier Settings → Secrets → Actions |
| API en `starting` après 3 min | Flyway migration lente ou erreur | `docker logs tchl-api-stg --tail 50` |
| `make smoke-staging` échoue | Edge ou API down | `docker ps` + `docker logs <container>` |
