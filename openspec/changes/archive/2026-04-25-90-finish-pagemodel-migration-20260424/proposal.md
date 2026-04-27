# Proposal: Finir la migration PageModel vers l'architecture hexagonale

**Change ID:** `90-finish-pagemodel-migration-20260424`
**Created:** 2026-04-24
**Status:** Draft
**Scope:** Backend Spring Boot uniquement

---

## Problem Statement

La migration du feature pagemodel vers l'architecture hexagonale est à 60% complète.
L'ancienne implémentation (`features/pagemodel/`, `features/pagemodel_backup/`) est encore
active en parallèle de la nouvelle (`core/pagemodel/`, `features/pagemodelruntime/`).

Cette coexistence crée des conflits Spring bloquants en production :

- **3 controllers déclarent la même route** `GET /api/v1/public/pagemodel/{logicalId}` —
  le comportement à l'exécution est indéterminable statiquement
- **2 `PageModelAdminController`** en conflit sur `POST /admin/pagemodels`
- **`@RepositoryRestResource` sur `PageModelRepository`** — expose les entités JPA brutes
  (violation critique de convention : FORBIDDEN)
- **`core/PageModelBootstrapService` importe depuis `features/`** — violation de la règle
  "core/ ne dépend jamais de features/"
- **`features.PageModel` record = doublon exact de `core.PageModelDoc`** — bridge de 150 lignes
  dans `PageModelDynamicResolver.toShared()`
- **`PageModelPersistenceAdapter.list()`** appelle `findAll()` sans filtre RLS — cross-tenant
- **Policy RLS sur `page_model`** non confirmée dans V40/V41

## Contexte existant

| Module                       | État                                                                          | Action                        |
| ---------------------------- | ----------------------------------------------------------------------------- | ----------------------------- |
| `core/pagemodel/`            | ✅ Bien fait — aggregate, handlers CQRS, ports, adapter JPA, controller admin | Garder + corrections mineures |
| `catalog/pagemodeltemplate/` | ✅ Complet et clean                                                           | Garder + fix import           |
| `features/pagemodelruntime/` | ✅ Bridge vers nouveau core — correct mais mal placé                          | Migrer dans slice `public/`   |
| `features/pagemodel/`        | ⚠️ Ancienne implémentation encore active                                      | Supprimer sélectivement       |
| `features/pagemodel_backup/` | ❌ Dead code — controller Spring actif en conflit                             | Supprimer entièrement         |

## Proposed Solution

Finaliser la migration en 5 phases ordonnées :

1. **Cleanup bloquant** : supprimer tout ce qui crée des conflits Spring (backup, admin dupliqué, controllers en double)
2. **Résoudre les doublons de types** : éliminer `features.PageModel`, `features.PageModelType`, etc.
3. **Créer les nouveaux slices** : structurer `features/pagemodel/` en slices par scope (`public/`, `dashboard/`, `onboarding/`, `dynamic/`, `shared/`)
4. **Corrections techniques** : RLS, TypedId, fix PersistenceAdapter, import catalog
5. **Providers dynamiques manquants** : brancher les 4 providers (Draws, Plans, Hero, Cashier fix)

La nouvelle architecture cible est documentée dans :
`tchalanet-docs/docs/02-features/PAGEMODEL-ARCHITECTURE-CIBLE.md`

## Scope

### In Scope (BACKEND UNIQUEMENT)

- Suppression des fichiers en conflit (phases 1–2)
- Création des slices `features/pagemodel/{public,dashboard,onboarding,dynamic,shared}/`
- Migration de `PageModelBootstrapService` vers `features/pagemodel/onboarding/`
- Correction `PageModelPersistenceAdapter.list()` (findAll → scope RLS)
- Vérification + création policy RLS sur table `page_model`
- Création `PageModelId` TypedId
- Fix `PublishPageModelHandler` : archiver l'instance PUBLISHED précédente
- Fix import `PageModelType` dans `PageModelTemplateSeedRunner`
- Providers dynamiques : `DrawsProvider`, `PlansProvider`, `HeroProvider`, fix `CashierOverviewProvider`

### Out of Scope

- Frontend Angular (widgets manquants, shell components) — proposal séparée
- Flutter mobile — proposal séparée
- Dashboard `private.dashboard.operator` (route Angular manquante — décision produit requise)
- Interface d'édition visuelle des pagemodels
- Nouveaux logicalIds (périmètre fixé aux 5 pages existantes)
- Tests unitaires de la couche domain (couverture globale — change #91)

## Impact Analysis

| Composant            | Changement                    | Détails                                                                                |
| -------------------- | ----------------------------- | -------------------------------------------------------------------------------------- |
| Base de données      | Oui (conditionnel)            | Migration V52 RLS sur `page_model` si policy absente de V40/V41                        |
| API publique         | Non — routes inchangées       | `GET /api/v1/public/pagemodel/{logicalId}` reste identique                             |
| API admin            | Non — routes inchangées       | `POST /admin/pagemodels` reste identique                                               |
| API dashboard        | Non — routes inchangées       | `/tenant/pagemodel/*`, `/platform/pagemodel/*` inchangées                              |
| Spring context       | Oui — résolution des conflits | Fewer beans, no ambiguity                                                              |
| Providers dynamiques | Oui — ajout                   | 4 nouveaux providers (DrawsProvider, PlansProvider, HeroProvider, CashierOverview fix) |

## Architecture Considerations

L'architecture cible est définie par les 4 couches strictes :

- `catalog/pagemodeltemplate/` — référentiel de templates (stable)
- `core/pagemodel/` — aggregate, CQRS handlers, ports (logique métier)
- `features/pagemodel/` — orchestration BFF par slice (public / dashboard / onboarding / dynamic / shared)

Règles non-négociables respectées par la cible :

- `core/` ne dépend pas de `features/` ✅
- `catalog/` ne dépend pas de `core/` ou `features/` ✅
- Pas de `@RepositoryRestResource` ✅
- RLS via `BaseTenantEntity` + policy PostgreSQL ✅
- Providers extensibles via interface `PageModelDynamicProvider` ✅

La `PageModelDynamicProvider` interface reste dans `features/pagemodel/dynamic/` et sert
de point d'extension pour les providers futurs (newsletter, analytics, etc.).

Voir `tchalanet-docs/docs/02-features/PAGEMODEL-ARCHITECTURE-CIBLE.md` pour le mapping
complet composant par composant.

## Success Criteria

- [ ] L'application démarre sans conflit Spring sur les routes pagemodel
- [ ] `GET /api/v1/public/pagemodel/public.home` retourne un payload valide (non null, non vide)
- [ ] `GET /api/v1/public/pagemodel/private.dashboard.cashier` retourne un payload valide avec dynamic.widgets
- [ ] Aucun `@RepositoryRestResource` dans le codebase pagemodel
- [ ] `core/` n'importe rien de `features/` dans le module pagemodel
- [ ] `PageModelPersistenceAdapter.list()` filtre correctement par tenant (via RLS)
- [ ] La table `page_model` a une policy RLS active (vérifiée par test de cross-tenant)
- [ ] `DrawsProvider` retourne les derniers résultats en base (non null, non vide)
- [ ] `PlansProvider` retourne les plans actifs du catalog
- [ ] `CashierOverviewProvider` retourne des données réelles (ticketsToday ≠ 0 si ventes existent)
- [ ] `./mvnw clean verify` passe sans erreur

## Risks & Mitigations

| Risque                                                   | Probabilité | Impact | Mitigation                                                          |
| -------------------------------------------------------- | ----------- | ------ | ------------------------------------------------------------------- |
| Spring démarre pas après cleanup partiel                 | Faible      | Élevé  | Faire Phase 1 en une seule PR — ne jamais merger partiellement      |
| Suppression d'un fichier encore référencé                | Moyen       | Moyen  | Compiler après chaque suppression avant de continuer                |
| RLS absente → fuite cross-tenant                         | Faible      | Élevé  | Vérifier V40/V41 AVANT de créer V52 pour éviter doublon             |
| DrawsProvider : query trop lente (derniers résultats)    | Faible      | Faible | Limiter à `LIMIT 4` (un par tirage majeur), ajouter index si besoin |
| CashierOverviewProvider : brisé si core/sales non stable | Moyen       | Faible | Ajouter fallback gracieux si QueryBus retourne null                 |

## Dépendances

- **Étape 0 globale** (Flyway V16/V22 conflicts) doit être mergée avant Phase 1
- `catalog/pagemodeltemplate/` doit rester stable pendant toute la migration
- `core/pagemodel/domain/model/PageModelDoc` est le type canonical — ne pas modifier sa structure
