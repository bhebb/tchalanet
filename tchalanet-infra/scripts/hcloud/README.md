# Scripts Hetzner Cloud

Scripts pour provisionner et gérer l'infrastructure sur Hetzner Cloud.

## 📜 Scripts disponibles

### `create-server.sh`

**Objectif :** Créer un serveur sur Hetzner Cloud

**Usage :**

```bash
./scripts/hcloud/create-server.sh <env>
```

**Actions :**

- Création serveur avec SSH key
- Application firewall
- Attachement au réseau privé
- Configuration IP publique
- Ajout labels (env, app)

**Variables requises :**

- `HCLOUD_TOKEN`
- Configuration dans le script

---

### `create-firewall.sh`

**Objectif :** Créer et configurer le firewall

**Usage :**

```bash
./scripts/hcloud/create-firewall.sh
```

**Actions :**

- Création firewall `tch-fw`
- Règles : 22 (SSH), 80 (HTTP), 443 (HTTPS)
- Application aux serveurs

---

### `create-network.sh`

**Objectif :** Créer le réseau privé

**Usage :**

```bash
./scripts/hcloud/create-network.sh
```

**Actions :**

- Création réseau `tch-net`
- Subnet configuration
- Zone assignment

---

## 🔧 Prérequis

1. **Hetzner Cloud CLI installé :**

```bash
brew install hcloud
```

2. **Token configuré :**

```bash
export HCLOUD_TOKEN="your-token"
# ou
hcloud context create tchalanet
```

3. **SSH Key générée :**

```bash
ssh-keygen -t ed25519 -C "env@tchalanet" -f ~/.ssh/tchalanet_<env>
hcloud ssh-key create --name tchalanet_<env> --public-key-from-file ~/.ssh/tchalanet_<env>.pub
```

## 📝 Workflow complet

```bash
# 1. Créer réseau
./scripts/hcloud/create-network.sh

# 2. Créer firewall
./scripts/hcloud/create-firewall.sh

# 3. Créer serveur staging
./scripts/hcloud/create-server.sh staging

# 4. Créer serveur prod
./scripts/hcloud/create-server.sh prod
```

## 🔗 Références

- [Hetzner Cloud Docs](https://docs.hetzner.cloud/)
- [hcloud CLI](https://github.com/hetznercloud/cli)
