# RB-01 — Provisionnement du serveur staging

**Quand utiliser ce runbook :** première mise en service du serveur staging, ou recréation complète après destruction.

**Durée estimée :** 20-30 min pour une première mise en place, 5-10 min pour recréer un serveur
déjà câblé (secrets GitHub/Doppler prêts, DNS Cloudflare accessible).

**Résultat attendu :** serveur Hetzner actif, stack Docker running, smoke test vert, workflow manuel prêt à rejouer.

---

## Fast path — serveur staging jetable

Objectif: pouvoir éteindre/détruire `stg-app`, recréer une VM propre, puis redéployer l'API sans
intervention manuelle lourde.

### Via GitHub Actions

Workflow: GitHub Actions → **Deploy Staging** → **Run workflow**

| Besoin | `infra_action` | `infra_destroy_confirm` | `deploy_infra` | Notes |
|---|---|---|---|---|
| Déployer sur le serveur existant | `deploy-core` puis runtime | vide | — | résout `stg-app` via Hetzner |
| Détruire le serveur staging | `destroy` | `destroy staging` | `false` | garde-fou obligatoire |
| Créer un serveur propre | `create` | vide | `true` | déploie ensuite sur la nouvelle IP |
| Détruire puis recréer | `recreate` | `destroy staging` | `true` | chemin standard pour reset complet |
| Construire seulement les images | `none` | vide | `false` | pas d'action infra |

Après `create` ou `recreate`, le workflow affiche `Staging server IP: <IP>`.
Mettre à jour Cloudflare DNS (`api.stg`, `edge.stg`, `*.stg`) si l'IP a changé.
Les workflows de provisioning et de runtime résolvent l'IP courante de `stg-app` via Hetzner;
aucun secret `SERVER_HOST` staging n'est requis pour le déploiement.

État courant validé le 2026-08-07: `stg-app` est en région Hetzner Ashburn (`ash`) avec l'IP publique `178.156.181.67`, type `cpx21`, réseau privé `tch-net-us-east`.

### En local

1. Créer le serveur:
   ```bash
   cd tchalanet-infra
   ./scripts/hcloud/staging-create.sh
   IP=$(hcloud server describe stg-app -o json | jq -r '.public_net.ipv4.ip')
   ```
2. Mettre à jour Cloudflare DNS: `api.stg`, `edge.stg`, `*.stg` vers `$IP`, en **DNS only**.
3. Lancer GitHub Actions → **Deploy Runtime Services** → **Run workflow**.
4. Valider:
   ```bash
   curl -fsS https://api.stg.tchalanet.com/api/v1/actuator/health
   ```

Le workflow `Deploy API Staging` synchronise `tchalanet-infra/`, télécharge les secrets Doppler,
recrée le container API si demandé, puis fait les smokes health + CORS. Le serveur reste donc
remplaçable: aucune donnée critique de staging ne doit dépendre uniquement du disque local de la VM.

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

Ce script enchaîne : réseau privé US East → firewall → cloud-init → serveur `stg-app` (`cpx21` / Ubuntu 24.04 / `ash`).

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
| A | `api.stg` | `<IP_STG>` (`178.156.181.67` au 2026-08-07) | DNS only (gris) |
| A | `edge.stg` | `<IP_STG>` (`178.156.181.67` au 2026-08-07) | DNS only (gris) |
| A | `*.stg` | `<IP_STG>` (`178.156.181.67` au 2026-08-07) | DNS only (gris) |

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
./scripts/remote/push-infra-bkup.sh "$IP" staging

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
# Doit lister : tchl-traefik-staging, tchl-postgres-staging, tchl-redis-staging,
# tchl-api-staging, tchalanet-edge-service-staging.
# Postgres est un service géré par Compose : aucun avertissement "orphan container"
# ne doit apparaître. S'il en apparaît un, la stack a été montée sans
# docker-compose-postgres.yml et un --remove-orphans détruirait la base.
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
| `SERVER_HOST` | Legacy/manuel seulement; les workflows staging courants résolvent `stg-app` via Hetzner |
| `HCLOUD_TOKEN` | Token Hetzner Cloud Read+Write |
| `DOPPLER_TOKEN_STG` | Service Token Doppler config `stg` |

```bash
# Récupérer la clé privée
cat ~/.ssh/tchalanet_stg
```

> **Sécurité :** ne jamais coller la clé dans un fichier commité. GitHub Secrets la chiffre au repos.

---

## Étape 9 — Workflow manuel de staging

Le workflow `deploy-staging.yml` est volontairement manuel (`workflow_dispatch`) pour staging.
Il sait gérer le cycle de vie infra via l'input `infra_action` :

- `deploy` : déploie sur le serveur existant (`SERVER_HOST`)
- `destroy` : détruit `stg-app` après confirmation `destroy staging`
- `create` : crée `stg-app`, récupère la nouvelle IP, puis peut déployer dessus
- `recreate` : détruit puis recrée `stg-app`
- `none` : laisse l'infra intacte

Déclencher depuis GitHub Actions → **Deploy Staging** → **Run workflow**.

---

## Étape 10 — Vérifier le déploiement end-to-end

1. Lancer `Deploy Staging` avec `infra_action=deploy`
2. Attendre la fin du job `deploy`
3. Relancer `make smoke-staging`

**Checkpoint final :** pipeline vert + smoke test vert = staging opérationnel.

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

Option recommandée :

1. GitHub Actions → **Deploy Staging** → **Run workflow**
2. `infra_action=recreate`
3. `infra_destroy_confirm=destroy staging`
4. `skip_destroy_backup=true` — les données staging sont jetables
5. `deploy_infra=true`
6. Après le job `infra-create`, reporter la nouvelle IP dans Cloudflare DNS et `SERVER_HOST`.

Option locale :

```bash
# Supprimer avec confirmation obligatoire
cd tchalanet-infra
SKIP_BACKUP=1 make staging-destroy

# Recréer
make staging-create
```

> ⚠️ La base staging vit dans le volume Docker `pgdata-staging` sur la VM : détruire le
> serveur détruit la base. Il n'y a pas de base managée externe qui y survivrait.
> Le backup pré-destruction de `staging-destroy.sh` appelle `pg_dumpall -U postgres`
> alors que le superuser est `admin` — il échoue en silence. Ne pas compter dessus.

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
