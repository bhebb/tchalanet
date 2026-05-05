# Index des Docker Compose (infra)

Vue d’ensemble des services, fichiers Compose, réseaux, ports exposés et dépendances.

> Note
>
> - Les ports « exposés (host) » marqués (override) proviennent de `compose/docker-compose.override.<ENV>.yml` et sont pensés pour le dev local (ENV=staging par défaut).
> - Par défaut (Makefile actuel), `up-all` n’inclut PAS API ni Umami. Ils peuvent être démarrés séparément si nécessaire.

| Service         | Compose file                      | Réseaux        | Ports exposés                           | Dépendances               |
| --------------- | --------------------------------- | -------------- | --------------------------------------- | ------------------------- |
| Traefik         | `docker-compose-traefik.yml`      | `edge`         | 80/443 (host)                           | -                         |
| Postgres        | `docker-compose-postgres.yml`     | `back`         | 5432 (host, override)                   | -                         |
| Redis           | `docker-compose-redis.yml`        | `back`         | 6379 (host, override)                   | -                         |
| Meilisearch     | `docker-compose-meilisearch.yml`  | `back`         | 7700 (host, override)                   | -                         |
| Unleash (admin) | `docker-compose-unleash.yml`      | `edge`, `back` | 4242 (host, override)                   | Postgres                  |
| Unleash Edge    | `docker-compose-unleash-edge.yml` | `edge`, `back` | 3063 (host, override)                   | Unleash                   |
| Keycloak        | `docker-compose-keycloak.yml`     | `edge`, `back` | 8080 (container), 8082 (host, override) | Postgres                  |
| API             | `docker-compose-api.yml`          | `edge`, `back` | 8080 (container, via Traefik)           | Keycloak, Redis, Postgres |

## Rappels utiles

- Fichiers Compose: chaque service a son propre fichier dans `compose/`.
- Variables:
  - Build-time (interpolation): `envs/<env>/compose.env` (ex: `ENV`, `DOCKER_PREFIX`, réseaux).
  - Runtime (conteneurs): `envs/common/*.env` + `envs/<env>/*.env` + `envs/<env>/.secrets` via `env_file:`.
- Réseaux:
  - `edge-<ENV>` (exposition publique via Traefik)
  - `back-<ENV>` (réseau interne des services)

## Démarrage rapide

- Stack « core » (sans API/Umami) avec override ports local:

```bash
make -C tchalanet-infra up-all ENV=staging
```

- Exemple: démarrer un service seul (avec ses env):

```bash
# Postgres seul
docker compose -f compose/docker-compose-postgres.yml \
  --env-file ../envs/staging/compose.env up -d
```

## Unleash Edge (front)

But: exposer un endpoint public optimisé pour les SDK (web/mobile) sans exposer l'admin Unleash.

- Service: `compose/docker-compose-unleash-edge.yml` (profile: `flags-edge`)
- Route Traefik: Host(`${EDGE_HOST}`) → port 3063
- Upstream (interne): `https://${FLAGS_HOST}` (serveur Unleash admin)
- Variables clés:
  - `EDGE_HOST` (envs/<env>/.env): host public de l'edge (ex: edge.stg.tchalanet.com)
  - `UNLEASH_SERVER_TOKEN` (secrets): token admin/API utilisé par Edge vers Unleash
  - `UNLEASH_FRONTEND_TOKEN` (secrets): token frontend exposé/consommé par les SDK front (ou monté pour l'Edge)
  - `UNLEASH_EDGE_CORS_ORIGIN` (secrets): origin autorisé pour CORS (mettre l'URL de votre front)
- Démarrer:

```bash
make -C tchalanet-infra up-flags-edge ENV=staging
```

- Health:

```bash
curl -I https://$EDGE_HOST/edge/health
```

- Exposer l'admin (optionnel):
  - `UNLEASH_TRAEFIK_ENABLE=true` (env) + `UNLEASH_DASHBOARD_AUTH` (secrets, htpasswd)
  - `make -C tchalanet-infra up-flags ENV=staging`

### Consommer les flags côté API et Front

- API (Spring Boot):

  - Variable: `TCH_FLAGS_URL=https://$EDGE_HOST/proxy` (définie en staging dans `envs/staging/api.env`).
  - Token: utiliser un token client Edge (à injecter en `.secrets` si nécessaire) ou un token serveur si vous gardez l’accès direct Unleash.
  - Alternative serveur (SDK Unleash): `TCH_FLAGS_URL=https://$FLAGS_HOST/api` + `TCH_FLAGS_TOKEN`.

- Front (Angular/Flutter):
  - SDK `unleash-proxy-client` (browser):
  - `url`: `https://$EDGE_HOST/proxy`
  - `clientKey`: `UNLEASH_FRONTEND_TOKEN` (ou un des tokens listés dans `/run/secrets/edge_tokens`)
  - `appName`: `tchalanet-web`
  - En prod, restreindre CORS via `UNLEASH_EDGE_CORS_ORIGIN`.

## Compose « dev-only » / overrides locaux

Certains compose sont destinés au développement local et ne doivent pas être utilisés tels quels en prod. Ils sont soit des overrides, soit des jobs ponctuels:

- `docker-compose.override.staging.yml` (override local)

  - Expose des ports host pratiques (5432, 6379, 8082, 4242, 7700) pour un usage local.
  - Activé automatiquement par le Makefile si `OVERRIDE=1` (par défaut). Désactiver avec `OVERRIDE=0`.
  - Utilisation manuelle:
    ```bash
    docker compose -f compose/docker-compose-postgres.yml \
      -f compose/docker-compose.override.staging.yml up -d
    ```

- `docker-compose-unleash-seeds.yml` (job de seed Unleash, one-shot)

  - Injecte projets/flags idempotents via l’API admin Unleash.
  - Pour dev/QA. Ne pas laisser tourner en boucle en prod.
  - Make cible: `make -C tchalanet-infra run-unleash-seeds ENV=staging`.

- `docker-compose.doppler.yml` (intégration secrets locale – optionnel)
  - A utiliser seulement si vous lancez Doppler côté dev pour injecter `.secrets` dynamiquement.
  - En CI/prod, secrets fournis par Doppler côté serveur sans ce compose.

Remarque: la convention de nommage est déjà alignée sur `override.staging`. Si d’autres overrides spécifiques sont ajoutés, préférez le schéma `docker-compose.override.<ENV>.yml`.
