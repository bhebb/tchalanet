# Proposal: billing-plan-subscription-split

**Change-id**: `billing-plan-subscription-split`

## Résumé

Ce changement introduit une séparation architecturale claire entre les **Plans de facturation** (référentiel global `catalog/plan`) et les **Souscriptions tenant** (lifecycle `core/subscription`).

**Objectif** : découpler les données de référence (plans) du cycle de vie métier (souscriptions tenant), permettre l'évolution indépendante, et garantir une architecture maintenable conforme aux principes catalog/core.

---

## Motivation

Actuellement, les concepts de "plan" et "subscription" peuvent être mélangés ou mal séparés, créant des risques :

- Couplage entre référentiel stable et lifecycle transactionnel
- Difficulté à cacher les plans sans impacter les souscriptions
- Pas de distinction claire entre "qu'est-ce qu'un plan offre" (catalog) et "quel plan un tenant utilise" (core)
- Risque d'événements métier dans le catalog (violation de principe)

---

## Décision (normative)

### D1 — Catalog "Plan" = référentiel pur

`catalog/plan` MUST être un **catalogue pur** :

- Stocke les plans disponibles (tiers/offres)
- Lecture seule côté API publique (`PlanCatalog`)
- Admin CRUD interne (`PlanAdminService`)
- **Aucun événement métier**
- **Aucune logique de lifecycle**
- **Aucune orchestration**

Actions autorisées :

- `listActive()`, `findByCode()`, `findById()`
- Admin : `create`, `update`, `deactivate`, `softDelete`

Filtrage reads :

- Tous les reads filtrent `deleted_at IS NULL`
- `listActive()` filtre `deleted_at IS NULL AND active=true`
- `findByCode()` retourne les plans inactifs aussi (avec flag `active=false`)

---

### D2 — Core "Subscription" = lifecycle tenant

`core/subscription` MUST gérer le **cycle de vie tenant** :

- Appliquer un plan à un tenant
- Gérer les états (trial/active/suspended/canceled/expired)
- Valider les transitions de statut
- Publier des événements after-commit (`TenantSubscriptionUpdatedEvent`)
- Garantir l'idempotence des commandes
- Respecter RLS (tenant-scoped)

---

### D3 — Séparation stricte des responsabilités

**Catalog ne doit PAS** :

- Gérer des souscriptions tenant
- Émettre des événements métier
- Orchestrer des workflows
- Appeler `core/subscription`

**Core ne doit PAS** :

- Accéder aux tables catalog directement
- Dépendre de `catalog/plan/internal/**`
- Utiliser `PlanJpaEntity` ou `PlanJpaRepository`

**Core DOIT** :

- Valider l'existence du plan via `PlanCatalog.findByCode()`
- Utiliser uniquement l'API publique du catalog

---

### D4 — Unicité et contraintes

**Plan `code`** MUST être globalement unique :

- Contrainte DB `UNIQUE` recommandée
- Admin retourne erreur lisible (409 Conflict) sur duplicate

**Subscription par tenant** :

- Un tenant a UNE souscription active à la fois (ou NULL)
- Clé composite : `(tenant_id)` (ou `(tenant_id, status)` si historique)
- RLS MUST être appliqué

---

## Portée

### Catalog/plan

- Structure : `catalog/plan/{api,internal/{read,write,mapper,cache,persistence,web}}`
- Table : `billing_plan` (ou `plan`)
- API : `PlanCatalog` (interface publique)
- Admin : `PlanAdminService` + `PlanAdminController`
- Cache : lectures cachées, invalidation sur writes

### Core/subscription

- Structure : `core/subscription/{domain,application/{command,query,event,port},infra}`
- Table : `tenant_subscription` (RLS)
- Commands : `ApplyTenantPlanCommand`, `SuspendSubscriptionCommand`, etc.
- Queries : `ResolveTenantSubscriptionQuery`
- Events : `TenantSubscriptionUpdatedEvent` (after-commit)

### Tests

- Catalog : mapper unit tests, integration tests (jsonb si utilisé)
- Core : command handler unit tests, integration tests RLS + idempotency
- ArchUnit : guards `api -> internal`, `core -> catalog.internal`

---

## Non-objectifs

- Implémenter un moteur de pricing complexe (MVP : pricing déclaratif dans catalog)
- Gérer les renouvellements automatiques (peut être ajouté en Phase follow-up)
- Migration des données existantes (hors scope initial si greenfield)
- Intégration Stripe/paiement (séparé, future)

---

## Critères d'acceptation

- `catalog/plan` est un catalogue pur : pas d'événements, pas de workflows
- `core/subscription` valide les plans via `PlanCatalog` (API publique uniquement)
- Commands sont idempotentes (retry safe)
- Événements publiés after-commit
- ArchUnit guards passent
- Bootstrap peut inclure subscription + plan metadata sans dégradation perf

---

## Validation

Après implémentation :

```bash
# Structure catalog conforme
ls catalog/plan/api
ls catalog/plan/internal/{read,write,mapper,cache,persistence,web}

# ArchUnit guards
mvn test -Dtest=ArchitectureTest

# Integration tests
mvn test -Dtest=PlanCatalogIntegrationTest
mvn test -Dtest=SubscriptionCommandHandlerTest
```

---

## Références

- Spec : `openspec/specs/catalog-plan/spec.md`
- Spec : `openspec/specs/core-subscription/spec.md`
- Conventions : `docs/conventions/{typed_ids,command_query_handlers,cache}.md`
- Inspiration : `openspec/changes/catalog-theme-presets/`
