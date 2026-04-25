# Spec: PageModel Feature — Architecture Hexagonale

**Capability:** `feature-pagemodel`
**Last synced from change:** `90-finish-pagemodel-migration-20260424`
**Synced date:** 2026-04-25

---

## Purpose

Le feature PageModel est un CMS headless JSON-driven qui définit la structure des pages de l'application Tchalanet. Il gère 5 pages (logicalIds) avec un système de fallback 3 niveaux (tenant DB → DEFAULT_TENANT DB → classpath JSON), des providers dynamiques pour enrichir les widgets, et une architecture hexagonale stricte respectant `core/` → `catalog/` → `features/`.

---

## Architecture

### Couches

| Couche   | Package                      | Responsabilité                               |
| -------- | ---------------------------- | -------------------------------------------- |
| catalog  | `catalog/pagemodeltemplate/` | Templates de référence (stables, read-only)  |
| core     | `core/pagemodel/`            | Aggregate, CQRS handlers, ports, adapter JPA |
| features | `features/pagemodel/`        | Orchestration BFF par slice                  |

### Structure `features/pagemodel/`

```
features/pagemodel/
  publicpage/    — slice anonyme (GET /public/pagemodel/{logicalId})
  dashboard/     — slice authentifiée (tenant + platform)
  onboarding/    — seeding des pagemodels par défaut au démarrage
  dynamic/       — providers dynamiques + resolver
    providers/   — DrawsProvider, PlansProvider, HeroProvider, CashierOverviewProvider, PublicNewsProvider
  shared/        — LangResolver, PageDynamicPayload, WidgetDynamicError
```

### logicalIds (5 pages)

| logicalId                        | Scope    |
| -------------------------------- | -------- |
| `public.home`                    | PUBLIC   |
| `private.dashboard.superadmin`   | PLATFORM |
| `private.dashboard.tenant_admin` | TENANT   |
| `private.dashboard.operator`     | TENANT   |
| `private.dashboard.cashier`      | TENANT   |

---

## Requirements

### Requirement: Route publique PageModel sans conflit Spring

Le système SHALL exposer exactement un controller gérant `GET /api/v1/public/pagemodel/{logicalId}`.

#### Scenario: Résolution d'une page publique

- **GIVEN** un logicalId valide (`public.home`, etc.)
- **WHEN** `GET /api/v1/public/pagemodel/{logicalId}?lang=fr` est appelé (anonymous)
- **THEN** `PublicPageModelController` gère la requête
- **AND** la réponse est `ApiResponse<PublicPageModelResponse>` avec `{ currentLang, langs, pageModel: PageModelDoc, dynamic: PageDynamicPayload }`

#### Scenario: Fallback 3 niveaux

- **GIVEN** aucun pagemodel pour ce tenant en base
- **WHEN** `ResolveEffectivePageModelQuery` est dispatché
- **THEN** le système tente : tenant DB → DEFAULT_TENANT_UUID DB → classpath JSON
- **AND** retourne `null` uniquement si aucun claspath JSON n'existe

---

### Requirement: Routes dashboard authentifiées

Le système SHALL exposer les routes dashboard via `DashboardPageModelController`.

#### Scenario: Dashboard par rôle (tenant)

- **GIVEN** un utilisateur authentifié avec rôle CASHIER sur tenant X
- **WHEN** `GET /api/v1/tenant/pagemodel/dashboard` est appelé
- **THEN** `PageModelTypeResolver.forDashboard(CASHIER)` retourne `private.dashboard.cashier`
- **AND** la réponse contient le pagemodel résolu pour ce logicalId

#### Scenario: Dashboard superadmin (platform)

- **GIVEN** un utilisateur SUPER_ADMIN
- **WHEN** `GET /api/v1/platform/pagemodel/dashboard` est appelé
- **THEN** le logicalId `private.dashboard.superadmin` est résolu

---

### Requirement: Onboarding PageModel au démarrage

Le système SHALL créer les pagemodels par défaut pour chaque nouveau tenant.

#### Scenario: Tenant sans pagemodels

- **GIVEN** un tenant sans instances pagemodel en base
- **WHEN** `PageModelOnboardingRunner` s'exécute (@Order(20), après SeedRunner @Order(10))
- **THEN** 5 instances `DRAFT` sont créées (une par `PageModelType`)
- **AND** aucune dépendance `core/` → `features/` n'est introduite

---

### Requirement: Isolation tenant via RLS

Le système SHALL appliquer le Row Level Security sur la table `page_model`.

#### Scenario: Requête cross-tenant bloquée

- **GIVEN** un utilisateur sur tenant A
- **WHEN** `PageModelPersistenceAdapter.list()` est appelé
- **THEN** seules les instances du tenant A sont retournées (via `findAllByDeletedAtIsNull()` + RLS)
- **AND** la policy RLS `tenant_isolation` sur `page_model` filtre automatiquement via `tenant_id`

---

### Requirement: Validation de schéma JSON à l'upsert

Le système SHALL valider le `modelJson` contre le schéma du template avant de persister.

#### Scenario: Schéma non vide — payload invalide

- **GIVEN** un template avec `schema` non-vide (JSON Schema valide)
- **WHEN** `UpsertPageModelCommand` est dispatché avec un `modelJson` invalide
- **THEN** `PageModelSchemaViolationException` est levée avec la liste des violations `[{ path, message }]`

#### Scenario: Schéma vide `{}` — pas de validation

- **GIVEN** un template avec `schema: {}`
- **WHEN** `UpsertPageModelCommand` est dispatché
- **THEN** aucune validation n'est effectuée (activation progressive)

---

### Requirement: Invariant publication unique

Le système SHALL garantir un seul `PUBLISHED` par (`tenant_id`, `logical_id`).

#### Scenario: Publication avec archivage de l'ancienne instance

- **GIVEN** un tenant avec une instance `PUBLISHED` pour `public.home`
- **WHEN** `PublishPageModelCommand` est dispatché pour une nouvelle instance
- **THEN** l'ancienne instance passe en `ARCHIVED`
- **AND** la nouvelle instance passe en `PUBLISHED`
- **AND** les deux opérations sont atomiques (@TchTx)

---

### Requirement: Providers dynamiques — Draws

Le système SHALL retourner les résultats de tirages récents dans le widget `DrawsWidget`.

#### Scenario: Résultats disponibles

- **GIVEN** des résultats de tirages en base (7 derniers jours)
- **WHEN** `DrawsProvider.load()` est appelé (source: `results_by_game` ou `draws`)
- **THEN** retourne `{ draws: [{ name, results[], drawnAt }] }` limité à 4 entrées

#### Scenario: Aucun résultat

- **GIVEN** aucun résultat en base
- **WHEN** `DrawsProvider.load()` est appelé
- **THEN** retourne `{ draws: [] }` sans lever d'exception (graceful fallback)

---

### Requirement: Providers dynamiques — Plans

Le système SHALL retourner les plans actifs du catalog dans le widget `PlansWidget`.

#### Scenario: Plans actifs disponibles

- **GIVEN** des plans actifs dans `PlanCatalog`
- **WHEN** `PlansProvider.load()` est appelé (source: `plans`)
- **THEN** retourne `{ plans: [{ code, name, description, price, currency, billingPeriod, features, isDefault }] }`

---

### Requirement: Providers dynamiques — Hero

Le système SHALL retourner un payload hero contextuel dans le widget `HeroWidget`.

#### Scenario: Contexte tenant disponible

- **GIVEN** un utilisateur authentifié sur tenant X
- **WHEN** `HeroProvider.load()` est appelé (source: `hero`)
- **THEN** retourne `{ tagline, ctaLinks, stats: { tenantCode }, backgroundUrl }`

#### Scenario: Contexte anonyme

- **GIVEN** aucun contexte tenant
- **WHEN** `HeroProvider.load()` est appelé
- **THEN** retourne `{ tagline, ctaLinks: [{register}, {learn_more}], stats: {}, backgroundUrl }`

---

### Requirement: Providers dynamiques — Cashier Overview

Le système SHALL retourner les données de session caissier en cours.

#### Scenario: Session ouverte

- **GIVEN** un caissier avec une session ouverte
- **WHEN** `CashierOverviewProvider.load()` est appelé (source: `cashier.overview`, logicalId: `private.dashboard.cashier`)
- **THEN** retourne `{ ticketsToday, totalAmount, sessionOpen: true, sessionId, openedAt }`

#### Scenario: Aucune session ouverte ou contexte manquant

- **GIVEN** aucune session ouverte ou contexte null
- **WHEN** `CashierOverviewProvider.load()` est appelé
- **THEN** retourne `{ ticketsToday: 0, totalAmount: 0, sessionOpen: false }` (fallback gracieux)

---

## Invariants

- Cycle de vie `PageModelInstance` : `DRAFT → PUBLISHED → ARCHIVED` — immuable
- `PageModelDoc` format JSON (meta/theme/shell/content/widgets) — immuable
- `core/` ne dépend jamais de `features/`
- `catalog/` ne dépend jamais de `core/` ni `features/`
- Pas de `@RepositoryRestResource` dans le module pagemodel
- RLS via `BaseTenantEntity` + policy PostgreSQL — jamais via code Java

---

## Related files

- `tchalanet-server/src/main/resources/pagemodel/*.json` — templates classpath
- `tchalanet-server/src/main/resources/db/migration/V40__rls_policies.sql` — RLS page_model
- `tchalanet-docs/docs/02-features/FEATURE_PAGEMODEL.md` — documentation métier
- `tchalanet-docs/docs/02-features/PAGEMODEL-ARCHITECTURE-CIBLE.md` — architecture cible
