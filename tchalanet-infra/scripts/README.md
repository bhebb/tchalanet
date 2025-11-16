# Scripts Tchalanet Infrastructure

Organisation des scripts pour la gestion de l'infrastructure Tchalanet.

## 📁 Structure

```
scripts/
├── docker/          # Scripts runtime pour conteneurs (entrypoints essentiels)
├── local/           # Scripts développement local (wrappers vers utils/*)
├── hcloud/          # Scripts Hetzner Cloud (provisioning)
├── keycloak/        # Scripts gestion Keycloak
├── remote/          # (ex-deployment) Scripts d'exécution côté serveur
├── doppler/         # Scripts gestion secrets Doppler
└── utils/           # Scripts utilitaires (source), appelés via local/*
```

## 🎯 Usage par cas d'usage

### 🏠 Développement Local

```bash
# Setup réseaux Docker pour un env
./scripts/local/setup-networks.sh staging

# Préparer envs (.env.merged)
make -C tchalanet-infra env-prepare ENV=staging

# Diagnostics
./scripts/local/diagnose-stack.sh staging
make -C tchalanet-infra check-health ENV=staging
```

### 📦 Build & up séquentiel (helper)

Le script `scripts/utils/up-seq.sh` prend en charge un build séquentiel des fichiers compose (en utilisant `envs/common/compose.env` + `envs/$ENV/compose.env` comme env-file temporaire) puis démarre la stack dans l'ordre configuré : traefik → postgres → redis → keycloak → api → unleash → …

- Usage :
```bash
# build + up (dev)
ENV=dev ./scripts/utils/up-seq.sh
```

- Pour sauter la phase build automatique (Makefile) :
```bash
DO_BUILD=0 make up-all ENV=dev
```

### ☁️ Provisioning Cloud (Hetzner)

```bash
# Créer infrastructure de base
./scripts/hcloud/create-network.sh
./scripts/hcloud/create-firewall.sh
./scripts/hcloud/create-server.sh staging

# Générer cloud-init
./scripts/hcloud/generate-cloud-init.sh staging > cloud-init.yml
```

### 🚀 Remote (ex-Déploiement)

```bash
# Initialisation côté serveur
./scripts/remote/bootstrap.sh staging

# Synchroniser l'infra sur le serveur
./scripts/remote/push-infra.sh <server-host-or-ip>
```

### 🔑 Keycloak

```bash
./scripts/keycloak/export-realm.sh staging
./scripts/keycloak/import-realm.sh staging
```

## 🔧 Scripts utilitaires (source)

- `utils/check-envars.sh` (wrapper: `local/check-envars.sh`)
- `utils/merge-envs.sh` (wrapper: `local/merge-envs.sh`)
- `utils/dedupe-envs.sh` (wrapper: `local/dedupe-envs.sh`)
- `utils/dedupe-intra-env.sh` (wrapper: `local/dedupe-intra-env.sh`)
- `utils/docker-pull-retry.sh` (wrapper: `local/docker-pull-retry.sh`)
- `utils/setup-networks.sh` (wrapper: `local/setup-networks.sh`)

## 📝 Conventions

- Scripts exécutables (`chmod +x`) et POSIX-friendly
- Logs simples, erreurs sur stderr, codes sortie cohérents

## 🚦 Workflow complet (0 → prod)

1) Dev local → up-core/up-flags-edge
2) Provisioning hcloud → create-network, create-server
3) Remote sync → push-infra + make up-all sur le serveur
4) Monitoring/health → make check-health

## Ordres d’exécution (préfixés)

### hcloud/
1) `01-create-network.sh`
2) `02-create-firewall.sh`
3) `03-create-server.sh`
4) `04-generate-cloud-init.sh`

### local/
1) `01-setup-networks.sh ENV=staging`
2) `02-env-prepare.sh ENV=staging`
3) `03-up-core.sh ENV=staging`
4) `04-up-flags-edge.sh ENV=staging`
5) `05-check-health.sh ENV=staging`

### remote/
1) `01-bootstrap.sh <env>`
2) `02-push-infra.sh <server>`
3) `03-prepare-remote.sh`

Les scripts préfixés sont des wrappers qui appellent les scripts existants et servent de « chemins dorés ». Nous ferons une passe pour archiver les scripts non essentiels (_legacy) une fois la stabilisation faite.
