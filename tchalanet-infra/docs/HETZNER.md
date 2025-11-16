# Hetzner — Création/Recréation d'une instance (staging/prod)

But: documenter un processus simple et reproductible pour créer, effacer et recréer un serveur Hetzner, puis (ré)installer l'infra Docker Tchalanet.

Le workflow ci-dessous s'appuie exclusivement sur les scripts déjà présents dans `tchalanet-infra/scripts`.

---

## 🚀 Quick Start (copier/coller)

```bash
# 1. Configurer le token Hetzner (une seule fois)
export HCLOUD_TOKEN="ton_token_api_hetzner"  # récupéré depuis console.hetzner.cloud

# 2. Créer la clé SSH et l'enregistrer
ssh-keygen -t ed25519 -C "tchalanet-stg" -f ~/.ssh/tchalanet_stg
hcloud ssh-key create --name tchalanet_stg --public-key-from-file ~/.ssh/tchalanet_stg.pub

# 3. Créer l'infrastructure Hetzner
cd tchalanet-infra
./scripts/hcloud/02-create-firewall.sh
./scripts/hcloud/04-generate-cloud-init.sh staging
./scripts/hcloud/03-create-server.sh --name stg-app --type cx23 --image ubuntu-24.04 --firewall tch-fw --ssh-key tchalanet_stg --location nbg1

# 4. Récupérer l'IP et pousser l'infra
IP=$(hcloud server describe stg-app -o json | jq -r '.public_net.ipv4.ip')
./scripts/remote/push-infra-bkup.sh $IP staging

# 5. Bootstrap et déploiement (sur la VM)
ssh tch@$IP 'cd /opt/tchalanet-infra && ./scripts/remote/01-bootstrap.sh staging && make env-merge ENV=staging && make up-staging'
```

Pour les détails et explications, voir les sections ci-dessous.

---

## 0) Prérequis

### Installer et configurer Hetzner Cloud CLI

1. **Installer la CLI:**

```bash
brew install hcloud
```

2. **Obtenir un token API:**

   - Connecte-toi à [Hetzner Cloud Console](https://console.hetzner.cloud/)
   - Sélectionne ton projet
   - Security → API Tokens → Generate API Token
   - Permissions: Read & Write
   - Copie le token (il ne sera affiché qu'une fois)

3. **Configurer le token (choix: méthode 1 OU 2):**

   **Méthode 1 - Variable d'environnement (temporaire):**

   ```bash
   export HCLOUD_TOKEN="ton_token_ici"
   # Vérifie que ça fonctionne:
   hcloud server list
   ```

   **Méthode 2 - Contexte hcloud (permanent - recommandé):**

   ```bash
   hcloud context create tchalanet
   # Colle ton token quand demandé
   hcloud context use tchalanet
   # Vérifie:
   hcloud server list
   ```

   > Pour persister le token dans ton shell, ajoute `export HCLOUD_TOKEN="..."` dans `~/.zshrc` ou `~/.bashrc`.

4. **Vérifier la configuration:**

```bash
hcloud server list    # doit retourner la liste (vide si aucun serveur)
hcloud ssh-key list   # doit fonctionner sans erreur
```

### Clé SSH

- Générer une clé SSH dédiée:

```bash
ssh-keygen -t ed25519 -C "tchalanet-stg" -f ~/.ssh/tchalanet_stg
```

- Enregistrer la clé publique chez Hetzner:

```bash
hcloud ssh-key create --name tchalanet_stg --public-key-from-file ~/.ssh/tchalanet_stg.pub
```

- Vérifier:

```bash
hcloud ssh-key list
```

### Optionnel

- DNS: entrée A/AAAA vers l'adresse publique (ex: Cloudflare)
- rsync: le script `scripts/remote/push-infra-bkup.sh` utilise `rsync` côté local et côté VM. Si la VM ne dispose pas de rsync par défaut, installe-le: `sudo apt-get update && sudo apt-get install -y rsync`.

### Configuration SSH simplifiée (alias)

Pour te connecter simplement avec `ssh tchalanet_stg` au lieu de `ssh -i ~/.ssh/tchalanet_stg tch@<IP>`, ajoute dans `~/.ssh/config`:

```ssh-config
Host tchalanet_stg
  HostName <IP_ou_DNS_STG>
  User tch
  IdentityFile ~/.ssh/tchalanet_stg
  StrictHostKeyChecking accept-new

Host tchalanet_prod
  HostName <IP_ou_DNS_PROD>
  User tch
  IdentityFile ~/.ssh/tchalanet_prod
  StrictHostKeyChecking accept-new
```

Ensuite, connexion simplifiée:

```bash
ssh tchalanet_stg
ssh tchalanet_prod
```

Pour automatiser la résolution de l'IP depuis hcloud, ajoute dans ton `.zshrc` ou `.bashrc`:

```bash
alias ssh-stg='ssh -i ~/.ssh/tchalanet_stg tch@$(hcloud server describe stg-app -o json | jq -r ".public_net.ipv4.ip")'
alias ssh-prod='ssh -i ~/.ssh/tchalanet_prod tch@$(hcloud server describe prod-app -o json | jq -r ".public_net.ipv4.ip")'
```

---

## 1) Provisioning Hetzner (réseau, firewall, serveur)

Tous les scripts suivants sont dans `tchalanet-infra/scripts/hcloud`.

1. Réseau privé (une seule fois par env)

```bash
cd tchalanet-infra
./scripts/hcloud/01-create-network.sh  # si disponible (sinon, ignorer)
```

2. Firewall (SSH/HTTP/HTTPS)

```bash
./scripts/hcloud/02-create-firewall.sh  # crée le firewall `tch-fw`
```

3. cloud-init pour l'environnement

```bash
# Génère ./scripts/hcloud/cloud-init.yml à partir du template
./scripts/hcloud/04-generate-cloud-init.sh staging    # ou production
```

4. Créer le serveur

```bash
# Exemple staging
./scripts/hcloud/03-create-server.sh \
  --name stg-app \
  --type cx23 \
  --image ubuntu-24.04 \
  --firewall tch-fw \
  --ssh-key tchalanet_stg \
  --location nbg1 \
  --network tch-net        # si créé précédemment (optionnel)

# À la fin, le script affiche l'IP publique et des instructions de connexion
```

5. Effacer et recréer (itératif)

```bash
# Supprimer la VM
hcloud server delete stg-app
# Recréer: relancer les étapes 3 et 4 (le réseau/firewall persistent)
```

---

## 2) Bootstrap de la machine distante

Objectif: préparer Docker, réseaux compose (`edge-<env>`, `back-<env>`), fichiers Traefik.

> **Note**: cloud-init configure uniquement le système de base (utilisateur `tch`, firewall UFW, timezone, packages essentiels). L'installation de Docker se fait via le script `01-bootstrap.sh` qui appelle `scripts/remote/install-docker.sh`.

Sur votre machine locale:

```bash
# Copier l'infra sur la VM (crée /opt/tchalanet-infra); exclut .secrets
./scripts/remote/push-infra-bkup.sh <IP_STG> staging
```

Puis, sur la VM (SSH):

```bash
ssh tch@<IP_STG>
cd /opt/tchalanet-infra
# Installe Docker si absent + crée réseaux + prépare Traefik
./scripts/remote/01-bootstrap.sh staging
```

---

## 3) Secrets via Doppler (recommandé)

Sur la VM (ou via workflow CI) — écrit `envs/staging/.secrets` utilisé par compose:

```bash
export DOPPLER_TOKEN="<token-service-staging>"
docker run --rm -e DOPPLER_TOKEN="$DOPPLER_TOKEN" -v "$PWD":/work -w /work dopplerhq/cli:latest \
  sh -lc 'doppler secrets download --format env --project tchalanet --config staging > envs/staging/.secrets && chmod 600 envs/staging/.secrets'
```

Générer ensuite le fichier d'env runtime:

```bash
# Toujours depuis /opt/tchalanet-infra
make env-merge ENV=staging
```

> Rappel: `.env.merged` agrège `envs/common/*.env` + `envs/staging/*.env` (hors `.secrets` et `compose.env`).

---

## 4) Déployer/Redéployer l'infra Docker

Sur la VM, vous pouvez soit utiliser les commandes Compose brutes, soit les cibles Make.

- Via Compose (explicite):

```bash
cd /opt/tchalanet-infra
# Pull des images (si nécessaires)
docker compose --env-file envs/staging/.env.merged \
  -f compose/docker-compose-project.yml \
  -f compose/docker-compose-postgres.yml \
  -f compose/docker-compose-redis.yml \
  -f compose/docker-compose-keycloak.yml \
  -f compose/docker-compose-unleash.yml \
  -f compose/docker-compose-api.yml \
  -f compose/docker-compose-traefik.yml pull

# Up (détaché)
docker compose --env-file envs/staging/.env.merged \
  -f compose/docker-compose-project.yml \
  -f compose/docker-compose-postgres.yml \
  -f compose/docker-compose-redis.yml \
  -f compose/docker-compose-keycloak.yml \
  -f compose/docker-compose-unleash.yml \
  -f compose/docker-compose-api.yml \
  -f compose/docker-compose-traefik.yml up -d
```

- Via Make (plus court):

```bash
cd /opt/tchalanet-infra
make up-staging  # équivaut à env-merge + rendu traefik + up séquentiel
```

Vérifications rapides:

```bash
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
curl -fsS http://unleash:4242/health
curl -fsS http://api:8080/actuator/health
```

---

## 5) Mises à jour applicatives (API)

Le plus simple: construire/publier l'image depuis CI, puis sur la VM:

```bash
# Mettre à jour uniquement l'API
cd /opt/tchalanet-infra
docker compose --env-file envs/staging/.env.merged \
  -f compose/docker-compose-project.yml \
  -f compose/docker-compose-api.yml pull

docker compose --env-file envs/staging/.env.merged \
  -f compose/docker-compose-project.yml \
  -f compose/docker-compose-api.yml up -d
```

---

## 6) Résumé des commandes à retenir

```bash
# Créer cloud-init et la VM
./scripts/hcloud/04-generate-cloud-init.sh staging
./scripts/hcloud/03-create-server.sh --name stg-app --ssh-key tchalanet_stg --firewall tch-fw --type cx23 --image ubuntu-24.04 --location nbg1

# Pousser l'infra et booter la VM
./scripts/remote/push-infra-bkup.sh <IP_STG> staging
ssh tch@<IP_STG> 'cd /opt/tchalanet-infra && ./scripts/remote/01-bootstrap.sh staging'

# Secrets et env runtime
ssh tch@<IP_STG> 'cd /opt/tchalanet-infra && export DOPPLER_TOKEN=... && docker run --rm -e DOPPLER_TOKEN="$DOPPLER_TOKEN" -v "$PWD":/work -w /work dopplerhq/cli:latest sh -lc "doppler secrets download --format env --project tchalanet --config staging > envs/staging/.secrets && chmod 600 envs/staging/.secrets" && make env-merge ENV=staging'

# Déployer
ssh tch@<IP_STG> 'cd /opt/tchalanet-infra && make up-staging'

# Supprimer
hcloud server delete stg-app
```

---

## FAQ

- **Erreur: `HCLOUD_TOKEN: HCLOUD_TOKEN not set`**

  - Le token API Hetzner n'est pas configuré. Solutions:
    - Export temporaire: `export HCLOUD_TOKEN="ton_token"`
    - Contexte permanent: `hcloud context create tchalanet` puis saisir le token
    - Persister dans shell: ajoute `export HCLOUD_TOKEN="..."` dans `~/.zshrc`
  - Vérifie ensuite: `hcloud server list` (doit fonctionner sans erreur)

- Comment me connecter simplement avec `ssh tchalanet_stg` ?

  - Configure `~/.ssh/config` (voir section 0 "Configuration SSH simplifiée").
  - Ou utilise un alias shell: `alias ssh-stg='ssh -i ~/.ssh/tchalanet_stg tch@$(hcloud server describe stg-app -o json | jq -r ".public_net.ipv4.ip")'`
  - Ou utilise la CLI hcloud directement: `hcloud server ssh --user tch stg-app`

- Dois-je pousser aussi le dossier `tchalanet-server` (source) via rsync ?

  - Non (recommandé): on ne pousse pas le code source. On build l’image Docker de l’API (CI/CD ou local), on la pousse au registre (ex: GHCR), puis la VM fait `docker compose pull && up -d` en fonction du `IMAGE_TAG`/`API_IMAGE_BASE` définis dans `envs/<env>/compose.env` + `.env.merged`.
  - Option alternative (moins recommandée): pousser le JAR ou le dossier `tchalanet-server/` pour builder l’image directement sur la VM. Cela complexifie et rallonge le déploiement; privilégier l’image pré‑buildée.

- Et si le registre est privé (GHCR privé) ?
  - Sur la VM, connecter Docker au registre (PAT): `echo $PAT | docker login ghcr.io -u USERNAME --password-stdin` avant `docker compose pull`.
