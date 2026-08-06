# Backup et restauration PostgreSQL

Tchalanet héberge sa propre base PostgreSQL dans tous les environnements
(voir `openspec/changes/postgres-mandatory-remove-neon`). Les garanties qu'un
PostgreSQL managé fournit d'office — backups automatiques, PITR, bascule — sont
donc à notre charge. Ce document est le contrat.

## Objectifs

| Objectif | Valeur | Signification |
| --- | --- | --- |
| **RPO** | prod 24 h · staging 7 j | Perte maximale acceptée : les écritures depuis le dernier backup. Staging est hebdomadaire — ses données sont recréables et ne justifient pas le coût d'un backup quotidien. |
| **RTO** | 1 h | Délai visé entre la décision de restaurer et le service rétabli. |
| Rétention | 14 quotidiens + 12 mensuels | ~26 objets en régime stable, par environnement. Les deux niveaux sont purgés. |
| Chiffrement | `age`, asymétrique | La VM ne détient que la clé publique. |
| Destination | Cloudflare R2 | Fournisseur distinct de Hetzner : une panne Hetzner ne touche pas les backups. |

Le RPO de 24 h est un choix, pas une fatalité : il n'y a pas de PITR entre deux
snapshots. Si le volume de ventes rend une journée de pertes inacceptable, la
réponse est d'augmenter la fréquence, ou de revenir à un PostgreSQL managé avec
PITR — pas d'espérer mieux du dispositif actuel.

## Comment ça marche

`.github/workflows/db-backup.yml` exécute `scripts/remote/pg-backup.sh` sur la
VM par SSH, selon deux cadences :

| Environnement | Cadence | Cron |
| --- | --- | --- |
| prod | quotidien | `7 8 * * *` UTC |
| staging | hebdomadaire, dimanche | `7 9 * * 0` UTC |

### Pourquoi 08:07 UTC en prod

Le créneau est calé **après la clôture métier**, pas au hasard :

- les tirages du jour sont réglés — `settle` démarre 10 min après tirage ;
- la génération du lendemain (00:05 local) et l'ouverture (00:15) sont passées ;
- pas de collision avec la purge analytics (03:15 UTC) ni la purge batch
  (04:00 UTC le dimanche).

08:07 UTC vaut 03:07 à Port-au-Prince en hiver et 04:07 en été. GitHub cron
ignore l'heure d'été : la marge d'une heure de part et d'autre est volontaire,
elle absorbe le décalage saisonnier d'Haïti sans avoir à toucher au cron deux
fois par an.

Un environnement sans hôte configuré (prod non provisionnée) est **ignoré avec
une note**, pas en échec : un workflow rouge tous les jours pour un
environnement qui n'existe pas encore apprend à ignorer les échecs, ce qui
annule l'intérêt d'alerter. Ajouter `PROD_SERVER_HOST` suffit à l'activer.

Le script :

1. `pg_dumpall --globals-only` — rôles et privilèges. Sans eux, une base
   restaurée n'a ni `app_user` ni ses GRANT.
2. `pg_dump -Fc` de la base applicative — format custom, restauration
   sélective et parallèle possible.
3. Avant le dump, normalise le propriétaire des quatre projections analytics
   (`analytics_daily`, `analytics_draw`, `analytics_selection` et
   `analytics_seller_terminal_draw`) vers `APP_DB_USER`.
4. **Vérifie par restauration réelle** dans un conteneur jetable, puis compte
   les tables. Un contrôle de taille ne prouve rien : un gzip de message
   d'erreur pèse aussi quelques octets. Le backup échoue si la restauration
   échoue ou si zéro table en sort.
5. Chiffre avec `age` puis pousse vers `r2:<bucket>/<env>/<AAAA>/<MM>/` avec
   `rclone`. Le 1er du mois, promeut la copie du jour en archive mensuelle sous
   `<env>/monthly/` — **avant** la purge, pour qu'une archive fraîche ne soit
   jamais éligible à l'effacement. Puis purge les deux niveaux et journalise le
   nombre d'objets restants.

### Rétention

| Niveau | Conservé | Variable |
| --- | --- | --- |
| Quotidien | 14 jours | `BACKUP_RETAIN_DAILY_DAYS` |
| Mensuel | 12 mois | `BACKUP_RETAIN_MONTHLY_MONTHS` |

Environ 26 objets par environnement en régime stable. Les mensuelles étaient
auparavant conservées sans limite — le stockage croissait indéfiniment. Les deux
niveaux sont maintenant bornés, et chaque run journalise le total restant.

### Outils

`rclone` suffit : binaire unique, backend Cloudflare R2 natif, multipart et
reprises gérés, configuration entièrement par variables d'environnement — donc
aucun fichier de config à protéger sur la VM.

> R2 expose une API compatible S3. Aucun compte, aucune facture et aucun outil
> AWS ne sont impliqués : « S3 » désigne ici le protocole, pas le fournisseur.

Dépendances sur la VM : `docker`, `age`, `rclone`.

Une **répétition de restauration** tourne après chaque backup planifié, sur un
runner isolé, en déchiffrant le dernier objet et en le restaurant pour de vrai.
Elle ne touche à aucun environnement. Son rôle est de faire découvrir un backup
mort *avant* qu'on en ait besoin.

Le backup tourne depuis GitHub Actions et non depuis un timer sur la VM pour que
l'échec soit visible. Un backup silencieusement mort est pire que pas de backup :
on croit être couvert.

## Restaurer

```bash
cd tchalanet-infra
export CLOUDFLARE_ACCOUNT_ID=... R2_ACCESS_KEY_ID=... R2_SECRET_ACCESS_KEY=... R2_BUCKET=...
export BACKUP_AGE_PRIVATE_KEY_FILE=~/.config/tchalanet/backup-age.key
ENV=staging ./scripts/remote/pg-restore.sh
```

Prérequis locaux : `rclone`, `age`, `docker` (`brew install rclone age` sur macOS).

Sans argument, le dernier backup est utilisé. Pour un backup précis, passer la
clé d'objet R2. Le script demande de taper `restore <env>` avant d'écraser quoi
que ce soit.

Le dump reste portable et utilise `--no-owner`. La restauration réapplique donc
explicitement le propriétaire `APP_DB_USER` sur les quatre projections
analytics, puis échoue si l'une d'elles reste possédée par un autre rôle. La
normalisation avant backup garantit que la base source est saine; elle ne
remplace pas cette garde post-restore.

Répétition sans rien toucher :

```bash
ENV=staging DRY_RUN=1 ./scripts/remote/pg-restore.sh
```

## Clés

La paire `age` se génère une fois :

```bash
age-keygen -o backup-age.key
```

La **clé publique** va dans le secret GitHub `BACKUP_AGE_PUBLIC_KEY` — elle
suffit à chiffrer, donc une compromission de la VM ne donne pas accès aux
backups déjà poussés. La **clé privée** va dans `BACKUP_AGE_PRIVATE_KEY` et dans
le gestionnaire de mots de passe de l'équipe. Elle ne doit jamais être déployée
sur un serveur applicatif.

> Perdre la clé privée rend tous les backups définitivement illisibles. Elle doit
> exister à au moins deux endroits hors de GitHub.

## Secrets requis

| Secret | Usage |
| --- | --- |
| `CLOUDFLARE_ACCOUNT_ID` | Identifiant de compte — **déjà présent**, partagé avec le worker lottery-proxy |
| `R2_ACCESS_KEY_ID` / `R2_SECRET_ACCESS_KEY` | Token R2 dédié (Object Read & Write), limité au bucket |
| `R2_BUCKET` | Bucket de destination |
| `BACKUP_AGE_PUBLIC_KEY` | Chiffrement (workflow de backup) |
| `BACKUP_AGE_PRIVATE_KEY` | Déchiffrement (répétition de restauration) |

### Ce qui se réutilise, et ce qui ne se réutilise pas

Le dépôt pilote déjà Cloudflare : le worker `lottery-proxy` et les déploiements
Pages du web s'appuient sur `CLOUDFLARE_ACCOUNT_ID` et `CLOUDFLARE_API_TOKEN`.
Les backups **réutilisent `CLOUDFLARE_ACCOUNT_ID`** — même compte, un secret de
moins à créer et à faire tourner.

`CLOUDFLARE_API_TOKEN` en revanche ne convient pas : c'est un token d'API
Cloudflare, pas un credential S3. L'accès objet R2 exige une paire clé/secret
créée depuis **R2 » Manage API tokens**. C'est une limite de Cloudflare, pas un
choix de conception. Créer ce token restreint au seul bucket de backup est de
toute façon préférable : il ne donne accès qu'aux backups, alors que le token
API porte les droits Workers et Pages.

## Backup et archive : ne pas confondre

Les deux écrivent dans R2, mais ils répondent à des questions différentes.

| | Backup | Archive |
| --- | --- | --- |
| Question | « Le serveur est perdu, comment tout remonter ? » | « Comment garder l'historique sans faire enfler la base ? » |
| Contenu | Photo complète de la base | Vieilles lignes **sorties** de la base |
| Effet sur la base | Aucun, lecture seule | Elle rétrécit — archive puis purge |
| Format | `pg_dump` chiffré `age` | `jsonl.gz` par table et période |
| Bucket | bucket de backup | `tch-archive` |
| Secrets | GitHub | Doppler |
| Piloté par | `db-backup.yml` | l'API (`/platform/archive/**`) |

Ils se complètent : archiver réduit la base, donc les backups deviennent plus
petits et plus rapides.

La différence qui compte : un backup est une **copie** — l'original reste en
base. Une archive, une fois la purge passée, devient la **seule copie
existante**. C'est pourquoi le pipeline d'archive vérifie checksum et nombre de
lignes, respecte les *legal holds*, et garde la purge en dry-run désactivée par
défaut. Et c'est pourquoi son stockage devait sortir de la VM avant toute
planification : conserver l'unique copie sur la machine qu'on cherche à
protéger n'a pas de sens.

Détail complet du partage des secrets :
[`runbooks/RB-00-secrets-checklist.md`](runbooks/RB-00-secrets-checklist.md).

## Limites assumées

- **Pas de PITR.** On restaure au dernier snapshot, pas à l'instant précédant
  l'incident.
- **Pas de bascule automatique.** La perte de la VM implique une restauration
  manuelle : le RTO d'1 h suppose une personne disponible.
- **Rétention non immuable.** Un attaquant disposant des clés R2 peut supprimer
  les backups. Activer le verrouillage d'objets R2 corrigerait ce point.
- **La répétition ne teste que la base**, pas la reconstruction du serveur. La
  procédure complète est dans
  [`runbooks/RB-06-disaster-recovery.md`](runbooks/RB-06-disaster-recovery.md),
  à dérouler au moins une fois sur staging avant d'en avoir besoin en prod.

## Ce que ce dispositif remplace

`staging-backup.sh` appelait `pg_dumpall -U postgres` alors que le superuser est
`admin`, sans mot de passe : il échouait. `staging-destroy.sh` porte le même
défaut et son garde-fou `[ -s "$BACKUP_FILE" ]` accepte un fichier non vide,
donc aussi un gzip de message d'erreur — la destruction se croyait protégée par
un backup inexistant. Les deux écrivaient par ailleurs sur le poste du
développeur : jamais planifiés, jamais hors-site.
