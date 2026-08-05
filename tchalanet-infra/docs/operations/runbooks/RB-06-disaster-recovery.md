# RB-06 — Reprise après sinistre total

Que faire quand le serveur est perdu : disque corrompu, VM détruite, compte
Hetzner inaccessible, suppression accidentelle. La base ne survit pas à la VM —
elle vit dans le volume Docker `pgdata-<env>`. Le seul état qui subsiste est ce
qui a été poussé dans Cloudflare R2.

À lire avec :
- [`../BACKUP-RESTORE.md`](../BACKUP-RESTORE.md) — fonctionnement des backups.
- [`RB-01-staging-provision.md`](./RB-01-staging-provision.md) — provisioning serveur.
- [`RB-00-secrets-checklist.md`](./RB-00-secrets-checklist.md) — inventaire des secrets.

---

## 0. Avant le sinistre — ce qui doit exister ailleurs

Ces éléments **ne doivent pas** vivre uniquement sur la machine perdue ni dans
un seul service. Sans eux, aucune reprise n'est possible.

| Élément | Où il doit exister | Sans lui |
| --- | --- | --- |
| Clé privée `age` | Secret GitHub **+** gestionnaire de mots de passe | Tous les backups sont illisibles. Définitif. |
| Clés R2 (`R2_ACCESS_KEY_ID`/`SECRET`) | Secrets GitHub, régénérables depuis Cloudflare | Régénérables — pas bloquant |
| Clé SSH (`~/.ssh/tchalanet_<env>`) | Secret GitHub + poste opérateur | Recréable via la console Hetzner |
| Token Doppler | Secret GitHub, régénérable | Régénérable |
| `HCLOUD_TOKEN` | Secret GitHub, régénérable | Régénérable |

> **Un seul point est irrécupérable : la clé privée `age`.** Tout le reste se
> régénère. Vérifier sa présence hors GitHub est la seule action de prévention
> qui compte réellement.

Test de vérification, à faire une fois par trimestre :

```bash
age -d -i <clé-du-gestionnaire> -o /dev/null <un-backup-.age>
```

S'il déchiffre, la copie hors ligne est bonne.

---

## 1. Évaluer

Répondre à ces trois questions avant d'agir :

1. **La VM est-elle vraiment perdue ?** Si elle répond encore en SSH, une
   restauration en place (§4) suffit — inutile de tout reconstruire.
2. **Quel est le dernier backup exploitable ?**

   ```bash
   export CLOUDFLARE_ACCOUNT_ID=... R2_ACCESS_KEY_ID=... R2_SECRET_ACCESS_KEY=... R2_BUCKET=...
   export RCLONE_CONFIG_R2_TYPE=s3 RCLONE_CONFIG_R2_PROVIDER=Cloudflare
   export RCLONE_CONFIG_R2_ACCESS_KEY_ID="$R2_ACCESS_KEY_ID"
   export RCLONE_CONFIG_R2_SECRET_ACCESS_KEY="$R2_SECRET_ACCESS_KEY"
   export RCLONE_CONFIG_R2_ENDPOINT="https://${CLOUDFLARE_ACCOUNT_ID}.r2.cloudflarestorage.com"
   rclone lsl "r2:${R2_BUCKET}/prod" --recursive --include '*.age' | sort -k4
   ```

3. **Quelle perte accepte-t-on ?** L'écart entre le dernier backup et maintenant
   est définitivement perdu. En prod, jusqu'à 24 h. En staging, jusqu'à 7 jours.

---

## 2. Recréer le serveur

```bash
cd tchalanet-infra
make staging-create        # ou provisioning prod équivalent
```

Le serveur obtient une **nouvelle IP**. Reporter immédiatement :

- Cloudflare DNS : `api.<env>.tchalanet.com`, `flags.`, `stats.`
- Secret GitHub `SERVER_HOST` (ou `PROD_SERVER_HOST`)

Tant que le DNS n'est pas propagé, les certificats Let's Encrypt ne peuvent pas
se renouveler : les challenges ACME échouent sur l'ancienne IP.

---

## 3. Remonter l'infrastructure

```bash
make push-staging
```

```bash
ssh -i ~/.ssh/tchalanet_stg tch@<nouvelle-ip> \
  'cd /opt/tchalanet-infra && make doppler-download ENV=staging && make env-merge ENV=staging'
```

> `push-staging` écrase `.env.merged` avec une copie du dépôt. Toujours
> enchaîner `env-merge`, sinon la configuration déployée est celle du dépôt et
> non celle de l'environnement.

Puis démarrer la stack — Postgres en fait partie et se lance avant l'API :

```bash
ssh -i ~/.ssh/tchalanet_stg tch@<nouvelle-ip> \
  'cd /opt/tchalanet-infra && make up-staging'
```

À ce stade la base est **vide**. L'API démarrera et Flyway créera le schéma :
c'est sans importance, la restauration écrase.

---

## 4. Restaurer la base

Installer les outils sur la nouvelle VM si le provisioning ne l'a pas fait :

```bash
sudo apt-get update && sudo apt-get install -y age rclone
```

Depuis un poste disposant de la clé privée `age` :

```bash
cd tchalanet-infra
export CLOUDFLARE_ACCOUNT_ID=... R2_ACCESS_KEY_ID=... R2_SECRET_ACCESS_KEY=... R2_BUCKET=...
export BACKUP_AGE_PRIVATE_KEY_FILE=~/.config/tchalanet/backup-age.key
ENV=staging ./scripts/remote/pg-restore.sh
```

Pour une restauration opérée avec les secrets GitHub du bucket backup, utiliser
le workflow manuel **Restore staging database**. Il exige l'objet R2 exact et
la confirmation `restore staging`, déchiffre sur le runner, puis pilote le
Docker distant STG par SSH. La clé privée `age` ne doit jamais être copiée sur
la VM applicative.

Le script demande de taper `restore <env>` avant d'écraser. Il restaure les
rôles (`globals.sql`) puis la base — dans cet ordre, sinon les `GRANT` sur
`app_user` échouent.

**Répéter d'abord sans risque** pour confirmer que le backup est exploitable :

```bash
ENV=staging DRY_RUN=1 ./scripts/remote/pg-restore.sh
```

### Réaligner le mot de passe applicatif

Le rôle `app_user` restauré porte le mot de passe qu'il avait **au moment du
backup**, qui peut différer de celui de Doppler. Si l'API échoue ensuite sur
`password authentication failed for user "app_user"` :

```bash
SU=$(docker inspect tchl-postgres-<env> --format '{{range .Config.Env}}{{println .}}{{end}}' | grep '^POSTGRES_PASSWORD=' | cut -d= -f2-)
APP=$(grep '^APP_DB_PASSWORD=' /opt/tchalanet-infra/envs/<env>/.secrets | cut -d= -f2-)
docker exec -e PGPASSWORD="$SU" tchl-postgres-<env> \
  psql -U admin -d tchalanet_db -v pw="$APP" -c "ALTER USER app_user WITH PASSWORD :'pw';"
```

C'est le piège classique d'une restauration : la base revient dans un état
cohérent avec elle-même, mais pas nécessairement avec le coffre à secrets.

---

## 5. Redéployer le runtime

GitHub Actions → **Deploy Runtime Services** → `database_mode=configured`, avec
les tags d'images qui tournaient avant le sinistre. Ne pas utiliser `latest` :
il est refusé pour staging et prod.

---

## 6. Vérifier

```bash
curl -fsS https://api.<env>.tchalanet.com/api/v1/actuator/health
```

Puis, au minimum :

- connexion d'un terminal vendeur ;
- présence des tirages ;
- un ticket connu d'avant le sinistre est retrouvable ;
- `docker ps` ne montre aucun conteneur orphelin.

Relancer enfin un backup immédiatement, pour ne pas rester sans filet :

GitHub Actions → **Database Backup** → `rehearse_restore=true`.

---

## 7. Après coup

- Consigner ce qui a été perdu (fenêtre entre le dernier backup et le sinistre).
- Supprimer l'ancien serveur Hetzner s'il existe encore, pour ne pas payer deux fois.
- Si la cause est une suppression accidentelle, envisager le verrouillage
  d'objets R2 : aujourd'hui, quelqu'un détenant les clés R2 peut effacer les
  backups eux-mêmes.

---

## Limites connues

- **Pas de PITR.** On revient au dernier backup, pas à l'instant d'avant.
- **RTO d'1 h** suppose quelqu'un de disponible et le DNS déjà maîtrisé. Une
  première reprise sans répétition préalable prendra davantage.
- **La répétition automatique ne teste que la restauration de la base**, pas la
  reconstruction complète du serveur. Faire ce runbook en entier au moins une
  fois sur staging, avant d'en avoir besoin en prod.
