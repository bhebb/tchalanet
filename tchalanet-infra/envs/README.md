# Env files - structure et conventions

Ce dossier regroupe les variables d’environnement utilisées par l’infra Docker Compose.

## Stratégie générale

- common = valeurs de production par défaut (non secrètes), par service: `envs/common/*.env`.
- envs/<ENV> = overrides minimaux (seulement ce qui diffère): `envs/dev|staging|prod/*.env`.
- secrets = exclusivement dans `envs/<ENV>/.secrets` (non versionnés), fournis par Doppler/CI.
- compose.env = variables de build/compose-time propres à l’environnement (ex: `ENV`, `IMAGE_TAG`, réseaux), passées à `docker compose --env-file`.
- .env.merged = résultat de la fusion common + overrides par env, chargé dans les `env_file:` des services.

## Ordre de merge (.env.merged)

Le script `scripts/utils/merge-envs.sh` fusionne les fichiers `.env` (last wins):
1. `envs/common/*.env`
2. `envs/<ENV>/*.env`

Sortie: `envs/<ENV>/.env.merged`

Notes:
- `.secrets` n’est jamais inclus dans le merge (chargé séparément).
- Les lignes vides et commentaires sont ignorés.
- Les compose consomment systématiquement:
  - `env_file: ../envs/${ENV}/.env.merged`
  - `env_file: ../envs/${ENV}/.secrets`

## Déduplication automatique

Le script `scripts/utils/dedupe-envs.sh` supprime dans `envs/<ENV>/*.env`:
- toute ligne clé=val identique à `envs/common/*.env` (déduplication),
- les placeholders/secrets dans les envs spécifiques (`*_PASSWORD`, `*_TOKEN`, `*_SECRET`, `*_MASTER_KEY`, ou valeurs `__REPLACE_ME*`).

S’il ne reste aucune paire clé=val, le fichier est supprimé. Un rapport est généré dans `envs/<ENV>/.dedupe_report.txt`.

Utilisation pratique:
```bash
# Nettoyer et fusionner pour un env
make -C tchalanet-infra env-prepare ENV=staging
# Ou seulement fusionner
make -C tchalanet-infra env-merge ENV=staging
```

## Syntaxe standard

- Format `clé=valeur` (sans `${…:-…}` dans les fichiers `.env`).
- Pas de secrets dans le dépôt: utilisez `envs/<ENV>/.secrets` (injecté par Doppler).
- Échapper correctement les valeurs contenant espaces/caractères spéciaux.

## Conventions de nommage

- Préfixes par domaine:
  - `API_` (ex: `API_MEMORY_LIMIT`)
  - `KC_` / `KEYCLOAK_` (ex: `KC_HOST`, `KC_DB_PASSWORD`)
  - `PG_` / `POSTGRES_` (ex: `PG_SHARED_BUFFERS`, `POSTGRES_PASSWORD`)
  - `REDIS_` (ex: `REDIS_HOST`, `REDIS_PASSWORD`)
  - `MEILI_` (ex: `MEILI_MASTER_KEY`)
  - `UNLEASH_` et `DATABASE_` (ex: `UNLEASH_SERVER_TOKEN`, `DATABASE_PASSWORD`)
  - `TRAEFIK_` (ex: `TRAEFIK_ACME_EMAIL`)
- Variables compose-time: `ENV`, `IMAGE_TAG`, `DOCKER_NETWORK_EDGE`, `DOCKER_NETWORK_BACK`, `DOCKER_LABELS` → dans `compose.env`.
- Variables runtime: dans `common/*.env` (prod) + overrides par env.

## Exemples

- Générer le merged pour staging:
```bash
make -C tchalanet-infra env-merge ENV=staging
```
- Préparer (dédup + merge) pour staging:
```bash
make -C tchalanet-infra env-prepare ENV=staging
```
- Utilisation dans Compose (déjà câblé):
```yaml
services:
  redis:
    env_file:
      - ../envs/${ENV}/.env.merged
      - ../envs/${ENV}/.secrets
```

## Workflows CI/CD

- `infra.yml`: déploie l’infra en modes `create` (tout) ou `update` (services CSV). Exécute `env-prepare` côté serveur et récupère `.secrets` via Doppler.
- `deploy-stg.yml` / `deploy-prod.yml`: déploient l’API uniquement; exécutent `env-prepare` + téléchargement `.secrets` via Doppler côté serveur, puis `docker compose` pour l’API.

### Note importante: compose.env (build-time)

- `envs/common/compose.env` contient les valeurs de build-time (ex: `IMAGE_TAG`, `TRAEFIK_IMAGE`, `OAUTH2_PROXY_UNLEASH_REDIRECT_URL`, ...). Ces valeurs sont concaténées avec `envs/<ENV>/compose.env` et passées à `docker compose --env-file` lors du build en local (via `docker-compose.local-build.yml`) ou en CI (workflow build-and-deploy).
- Le workflow CI met à jour `envs/staging/compose.env` avec `IMAGE_TAG=<tag>` après avoir poussé l'image vers le registre (GHCR). Le serveur utilise ensuite ce `compose.env` pour effectuer `docker compose pull`.
- `docker-compose.local-build.yml` est réservé au développement local et aux builds locaux uniquement — il ne doit pas être exécuté par les workflows CI/production.

## Bonnes pratiques

- common = prod, toujours. Dev/staging n’ajoutent que des overrides nécessaires.
- Pas de secrets ni placeholders dans les envs versionnés (utiliser `.secrets`).
- Éviter les doublons: laissez la déduplication faire son travail; si un fichier devient vide, il est supprimé.
- Variablez par service: ex. `keycloak.env` porte `KC_HOST`, `KC_REALM`; `unleash.env` porte `UNLEASH_URL`, etc.
- Compose-time vs runtime: mettez `ENV`, `IMAGE_TAG`, réseaux, labels dans `compose.env`; réservez `.env` aux overrides runtime.

# Gestion des fichiers .env et .secrets

## MEILI_MASTER_KEY (staging/prod)

- Stockage: `envs/<env>/.secrets` (non versionné)
- Génération initiale (sur le serveur):
```bash
cd /opt/tchalanet-infra
./scripts/remote/04-generate-meili-master-key.sh staging
```
- Rotation (sur le serveur):
```bash
cd /opt/tchalanet-infra
./scripts/remote/03-rotate-meili-master-key.sh staging
```
- Compose: `compose/docker-compose-meilisearch.yml` charge les `env_file` (dont `.secrets`) → la variable est disponible au runtime.

Notes:
- Différencier les clés entre `staging` et `production`.
- En CI, préférer injecter la valeur via secrets (Doppler / GitHub Secrets) et l’écrire dans `envs/<env>/.secrets` avant `docker compose up`.
