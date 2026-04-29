## Why

La stack de développement local (Postgres 18.1 + Keycloak custom + Traefik v3.6.5 +
Redis 8.4.0) contient plusieurs blocages opérationnels et incohérences de configuration
qui empêchent un nouveau développeur de démarrer sans intervention manuelle non documentée.
L'analyse du 2026-04-27 (`tchalanet-infra/docs/spec-local-dev-stack-2026-04-27.md`)
a identifié 5 blocages bloquants (B1–B5) et 3 avertissements (W1–W3).

## What Changes

- **Fix `setup-api-env.sh`** : corrige les valeurs incorrectes générées (mauvaise DB, mauvais
  realm Keycloak, `ddl-auto=update` interdit). **BREAKING** pour les développeurs utilisant
  ce script actuellement.
- **Fix `traefik/traefik.yml`** : `ping.entryPoint` passe de `websecure` à `web` pour éviter
  la dépendance TLS au démarrage (healthcheck Traefik plus fiable).
- **Créer `QUICK-START.md`** dans `tchalanet-infra/` : procédure de démarrage complète pour
  un nouveau développeur (prérequis → stack Docker → API Spring Boot → vérifications).
- **Documenter `make rebuild-keycloak`** : la dépendance à l'image Keycloak custom n'est
  documentée nulle part pour un démarrage sans accès GHCR.
- **Mettre à jour `README.md` infra** : section démarrage actuelle fait référence à `up-all`
  mais ne mentionne pas les prérequis mkcert ni les réseaux Docker.
- **Ajouter `start_period: 15s` sur Traefik** dans `docker-compose-traefik.yml` : fiabilise
  le healthcheck lors du premier démarrage.

## Capabilities

### New Capabilities

- `local-dev-quickstart` : Procédure de démarrage local formalisée. Prérequis, commandes
  dans l'ordre, vérifications. Couvre Option A (tout Docker), Option B (API en IDE local),
  Option C (minimal sans auth). Référence canonique pour les nouveaux développeurs.

### Modified Capabilities

_(Aucune spec-level behavior change — changes purement opérationnels/infra.)_

## Impact

### Code / config modifiés

| Fichier                                              | Modification                         |
| ---------------------------------------------------- | ------------------------------------ |
| `tchalanet-infra/scripts/local/setup-api-env.sh`     | DB, user, realm, `ddl-auto` corrigés |
| `tchalanet-infra/traefik/traefik.yml`                | `ping.entryPoint: web`               |
| `tchalanet-infra/compose/docker-compose-traefik.yml` | `start_period: 15s` sur healthcheck  |

### Fichiers créés

| Fichier                                                   | Contenu                                     |
| --------------------------------------------------------- | ------------------------------------------- |
| `tchalanet-infra/QUICK-START.md`                          | Procédure démarrage local (prérequis → DoD) |
| `tchalanet-infra/docs/spec-local-dev-stack-2026-04-27.md` | Spec d'analyse existante (déjà créée)       |

### Systèmes affectés

- **Développeurs locaux** : `setup-api-env.sh` génère désormais des valeurs correctes
  → breaking si quelqu'un avait un `.env` API basé sur les anciennes valeurs.
- **Healthcheck Traefik** : plus fiable au démarrage (boot TLS non bloquant).
- **Onboarding** : nouveau développeur peut démarrer via `QUICK-START.md` sans consulter
  plusieurs fichiers README éparpillés.

### Non scope

- Migration Postgres ou changement de version d'image
- Ajout de services (Meilisearch, monitoring)
- Modification du realm Keycloak ou des scopes OIDC
- Configuration CI/CD ou staging
