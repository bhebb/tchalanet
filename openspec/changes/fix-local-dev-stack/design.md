## Context

La stack locale Tchalanet utilise Docker Compose fragmenté en plusieurs fichiers
(`docker-compose-{service}.yml`) orchestrés par `up-seq.sh` via `make up-all`.
L'analyse du 2026-04-27 a révélé 5 blocages bloquants et plusieurs fichiers de
configuration contenant des valeurs incorrectes ou sous-documentées.

Les corrections sont purement opérationnelles — aucune modification d'architecture
applicative, de schéma de base de données ou de contrat API n'est impliquée.

## Goals / Non-Goals

**Goals:**

- Corriger `setup-api-env.sh` pour générer des valeurs valides (DB, realm, `ddl-auto`)
- Fiabiliser le healthcheck Traefik (ping sur `web` au lieu de `websecure`)
- Augmenter `start_period` Traefik à 15s pour absorber le boot TLS au premier démarrage
- Créer `QUICK-START.md` comme document de référence unique pour le démarrage local
- Tous les services `healthy` via `make up-all ENV=dev` sans intervention manuelle

**Non-Goals:**

- Modifier les versions d'images Docker (Postgres, Keycloak, Redis, Traefik)
- Changer la structure des fichiers compose ou le Makefile
- Modifier la configuration Spring Boot `application.yaml` ou les profils
- Modifier le realm Keycloak ou les scopes OIDC
- Traiter les environnements staging ou prod

## Decisions

### D1 — `ping.entryPoint: web` (HTTP) plutôt que `websecure` (HTTPS)

**Problème** : Traefik démarre le healthcheck dès le lancement du processus, mais
l'entrypoint `websecure` requiert que le provider TLS (fichier) ait chargé les
certificats mkcert. Sur une machine fraîche, ce chargement peut prendre quelques
secondes, causant un healthcheck prématurément `unhealthy`.

**Décision** : `ping.entryPoint: web` — l'endpoint ping HTTP est disponible
immédiatement sans dépendance TLS.

**Alternative écartée** : Augmenter uniquement `start_period`. Insuffisant si Traefik
est redémarré sans `start_period` appliqué (le healthcheck reprend à 0).

### D2 — `start_period: 15s` sur Traefik

Sur une machine sous charge (premier `docker compose up`), les 5s actuelles peuvent
être insuffisantes pour que les certs soient chargés ET le ping réponde.
15s réduisent les faux-négatifs sans rallonger significativement le boot.

### D3 — `QUICK-START.md` dans `tchalanet-infra/` (racine du sous-projet)

Emplacement canonique visible dès `ls tchalanet-infra/`. Le `README.md` reste
le document général (architecture, CI, staging) ; `QUICK-START.md` est scoped
au démarrage local uniquement.

**Alternative écartée** : Mettre à jour `README.md` directement. Le README est déjà
long (242 lignes) et mélange local + staging + prod. Un fichier dédié est plus lisible.

### D4 — `setup-api-env.sh` corrigé (pas déprécié)

Le script reste utile pour les développeurs qui veulent un `.env` fichier pour leur
IDE (IntelliJ EnvFile plugin, etc.). Il est corrigé plutôt que supprimé.

**Valeurs corrigées** :

| Variable                        | Avant                                         | Après                                           |
| ------------------------------- | --------------------------------------------- | ----------------------------------------------- |
| `SPRING_DATASOURCE_URL`         | `jdbc:postgresql://localhost:5432/postgres`   | `jdbc:postgresql://localhost:5432/tchalanet_db` |
| `SPRING_DATASOURCE_USERNAME`    | `postgres`                                    | `app_user`                                      |
| `SPRING_DATASOURCE_PASSWORD`    | `${POSTGRES_PASSWORD}`                        | `${APP_DB_PASSWORD}`                            |
| Issuer URI                      | `http://localhost:8082/realms/tchalanet-$ENV` | `https://auth.localtest.me/realms/tchalanet`    |
| `KEYCLOAK_REALM`                | `tchalanet-$ENV`                              | `tchalanet`                                     |
| `KEYCLOAK_AUTH_SERVER_URL`      | `http://localhost:8082`                       | `https://auth.localtest.me`                     |
| `SERVER_PORT`                   | `8081`                                        | `8083` (aligné `application-local-ide.yaml`)    |
| `SPRING_PROFILES_ACTIVE`        | `$ENV` (ex: `dev`)                            | `local-ide`                                     |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update`                                      | `validate`                                      |

## Risks / Trade-offs

**[Risque] `setup-api-env.sh` breaking pour les développeurs existants**
→ Mitigation : documenter dans `QUICK-START.md` la procédure de régénération.
Les développeurs with un `.env` existant basé sur les anciennes valeurs doivent
le régénérer : `./scripts/local/setup-api-env.sh dev`.

**[Risque] `ping: web` — si le port 80 est occupé sur la machine hôte**
→ Mitigation : le port 80 est exposé par `docker-compose.override.yml` (col `80:80`).
Si ce port est occupé, Traefik ne démarre pas de toute façon (pas un nouveau problème
introduit par cette décision).

**[Trade-off] `start_period: 15s` allonge le délai avant que Docker signale Traefik healthy**
→ Acceptable : Traefik est opérationnel avant que le healthcheck soit positif.
Les services qui dépendent de `traefik: condition: service_healthy` attendent 15s max.
Aucun service dans la stack actuelle ne déclare cette dépendance.

## Migration Plan

1. Appliquer les 3 fixes (fichiers modifiés)
2. Créer `QUICK-START.md`
3. Les développeurs existants : recréer leur `.env` API via `setup-api-env.sh dev`
4. Rollback trivial : `git revert` sur les fichiers modifiés

## Open Questions

_Aucune — toutes les décisions techniques sont résolues._
