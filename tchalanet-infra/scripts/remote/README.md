# Scripts Remote

Scripts pour préparer/synchroniser l'infra sur un serveur distant.

## Note importante sur Hetzner et cloud-init

Sur les images Ubuntu Hetzner, le **DataSourceHetzner** ignore souvent le bloc `users:` du cloud-init. La clé SSH spécifiée via `--ssh-key` est injectée **uniquement dans root**, pas dans les users custom définis dans cloud-init.

**Solution adoptée**: Notre `cloud-init.template.yml` crée l'utilisateur `tch` via `runcmd` (fallback **100% idempotent**) avec :

- Création compte + home dir (uniquement si n'existe pas)
- Injection clé SSH (ajout sans écraser les clés existantes)
- Configuration sudo NOPASSWD (uniquement si fichier absent)
- Permissions correctes

**Idempotence garantie**: Les commandes peuvent être relancées plusieurs fois sans risque de duplication ou écrasement. Si l'utilisateur `tch` existe déjà (via le bloc `users:` ou précédente exécution), les commandes sont no-op ou ajoutent uniquement ce qui manque.

**Workflow initial**:

1. Le serveur démarre, cloud-init s'exécute
2. `runcmd` crée l'utilisateur `tch` + copie la clé depuis root (ou utilise la variable `${SSH_KEY}`)
3. Connexion possible avec `ssh -i ~/.ssh/tchalanet_stg tch@<IP>`

Si `tch` n'est pas accessible immédiatement après création serveur, connecte-toi en `root` pour diagnostic :

```bash
ssh -i ~/.ssh/tchalanet_stg root@<IP>
tail -100 /var/log/cloud-init.log
id tch
ls -la /home/tch/.ssh/
```

---

## 01-bootstrap.sh

Objectif: configuration initiale (one-shot) d'un serveur:

- Installe Docker si absent
- Crée les réseaux Docker edge-<env> et back-<env>
- Prépare Traefik (server/traefik/acme.json 600)

Usage:

```bash
./scripts/remote/01-bootstrap.sh <env>
```

## 02-push-infra.sh (optionnel)

Objectif: pousser l'infra (rsync) vers le serveur et exécuter le bootstrap si présent.

Usage:

```bash
./scripts/remote/02-push-infra.sh <server-host-or-ip> [env]
```

Notes:

- Si vous utilisez les workflows GitHub (infra.yml, server\_\*), ce script est optionnel.
- En l'absence de bootstrap côté serveur, un fallback minimal crée acme.json et réseaux edge/back pour l'env.

## install-docker.sh

Installe Docker/compose-plugin sur la machine. Appelé par bootstrap si nécessaire.

## 03-rotate-meili-master-key.sh

À exécuter sur le serveur (dans `/opt/tchalanet-infra`).

Objectif:

- Régénérer `MEILI_MASTER_KEY` pour un env donné
- Sauvegarder `envs/<env>/.secrets`
- Redémarrer Meilisearch via docker compose (project + meilisearch)

Usage:

```bash
cd /opt/tchalanet-infra
./scripts/remote/03-rotate-meili-master-key.sh staging
```

Attention: la rotation invalide les API keys Meili dérivées de l’ancienne master key. Prévoir une procédure de communication/renouvellement côté apps si nécessaire.
