# Design — billing-plan-subscription-split

## Vue d'ensemble

Ce design implémente la séparation architecturale entre :

- **catalog/plan** : référentiel global des plans de facturation (read-mostly, cache, pas d'événements)
- **core/subscription** : lifecycle tenant des souscriptions (transactionnel, événements, RLS)

---

## Décisions architecturales

### DA1 — Catalog = référentiel pur, pas de lifecycle

**Décision** : `catalog/plan` est un catalogue en lecture seule côté API publique, avec admin CRUD interne.

**Rationale** :

- Plans = données de référence stables
- Pas de cycle de vie métier → pas d'événements
- Cacheable sans risque de stale data (changes rares)
- Séparation claire : "ce qui existe" vs "ce qu'un tenant utilise"

**Implication** :

- `PlanCatalog` (API) expose `listActive()`, `findByCode()`, `findById()`
- `PlanAdminService` (internal) gère create/update/deactivate/softDelete
- Aucun `@ApplicationEvent`, aucun handler de commande CQRS
- Cache invalidation sur admin writes uniquement

---

### DA2 — Core subscription = tenant-scoped lifecycle avec événements

**Décision** : `core/subscription` gère l'état transactionnel des souscriptions tenant.

**Rationale** :

- Subscription = agrégat métier avec lifecycle (trial → active → suspended → canceled)
- Doit émettre événements pour intégrations (analytics, billing, notifications)
- Versioning optimiste pour concurrence
- RLS Postgres pour isolation tenant

**Implication** :

- Commands CQRS : `ApplyTenantPlanCommand`, `SuspendSubscriptionCommand`, etc.
- Queries CQRS : `ResolveTenantSubscriptionQuery`
- Events after-commit : `TenantSubscriptionUpdatedEvent`
- Idempotence via version optimiste (@Version JPA)

---

### DA3 — Validation plan au moment de l'application, tolérance à la résolution

**Décision** : Lors de `ApplyTenantPlanCommand`, on valide que le plan existe ET est actif. Lors de `ResolveTenantSubscriptionQuery`, on tolère plan inactif/retiré (lecture seule).

**Rationale** :

- Empêcher d'appliquer un plan déjà retiré
- Ne pas casser les tenants qui ont un plan devenu inactif (lecture OK)
- Policy : inactive plans = legacy support, mais pas de nouvelles souscriptions

**Implication** :

- `ApplyTenantPlanCommandHandler` rejette si `plan.deleted_at != NULL`
- `ApplyTenantPlanCommandHandler` MAY accepter `plan.active=false` (policy configurable)
- `ResolveTenantSubscriptionQueryHandler` lit subscription sans valider plan

---

### DA4 — Plan identification : code fonctionnel > UUID technique

**Décision** : Plan identifié par `code` (string unique) en externe, `PlanId` (typed UUID) en interne.

**Rationale** :

- `code` = clé fonctionnelle stable (ex: "pro-v1", "enterprise-2024")
- `PlanId` = clé technique pour relations internes
- Subscription stocke `planCode` (string) pour découplage (pas de FK hard)

**Implication** :

- `billing_plan.code` UNIQUE constraint
- `tenant_subscription.plan_code` (string, pas de FK)
- API publique préfère `findByCode()` sur `findById()`
- Admin API accepte `PlanId` pour updates

---

## Composants impactés

### 1. catalog/plan (nouveau module)

**Structure** :

```
catalog/plan/
  api/
    PlanCatalog.java          (interface publique)
    PlanView.java             (immutable DTO)
  internal/
    read/
      PlanCatalogImpl.java    (implémente PlanCatalog + cache)
    write/
      PlanAdminService.java   (admin CRUD + cache eviction)
    mapper/
      PlanMapper.java         (MapStruct + CommonIdMapper)
    cache/
      PlanCacheNames.java     (constantes)
    persistence/
      PlanJpaEntity.java
      PlanJpaRepository.java
    web/
      PlanAdminController.java (platform admin REST)
```

**Responsabilités** :

- Exposition read-only des plans via `PlanCatalog`
- Admin CRUD via `PlanAdminService`
- Cache sur reads (`ACTIVE_PLANS`, `PLAN_BY_CODE`)
- Aucun événement, aucun workflow

**Dépendances** :

- Aucune dépendance vers `core/subscription`
- Utilise `CommonIdMapper` pour `PlanId`

---

### 2. core/subscription (nouveau module)

**Structure** :

```
core/subscription/
  domain/
    model/
      Subscription.java              (record immutable)
      SubscriptionStatus.java        (enum: TRIAL/ACTIVE/SUSPENDED...)
    exception/
      SubscriptionNotFoundException.java
  application/
    command/
      model/
        ApplyTenantPlanCommand.java
      handler/
        ApplyTenantPlanCommandHandler.java
    query/
      model/
        ResolveTenantSubscriptionQuery.java
        SubscriptionView.java
      handler/
        ResolveTenantSubscriptionQueryHandler.java
    event/
      TenantSubscriptionUpdatedEvent.java
    port/out/
      SubscriptionPersistencePort.java
      SubscriptionReaderPort.java
  infra/
    persistence/
      SubscriptionJpaEntity.java
      SubscriptionJpaRepository.java
      SubscriptionPersistenceAdapter.java
    web/
      SubscriptionController.java (optionnel)
```

**Responsabilités** :

- Gérer lifecycle subscription tenant
- Valider plan via `PlanCatalog` (API publique)
- Publier événements after-commit
- Garantir idempotence (version optimiste)

**Dépendances** :

- `catalog/plan/api/PlanCatalog` (lecture seule)
- `common/types/id/{TenantId, PlanId}`
- Aucune dépendance vers `catalog/plan/internal`

---

## Flow diagrams (ASCII)

### Scénario 1 : Apply valid plan (happy path)

```
User/Admin → ApplyTenantPlanCommand(tenantId=T, planCode="pro-v1")
  ↓
ApplyTenantPlanCommandHandler (@TchTx)
  ↓
PlanCatalog.findByCode("pro-v1")
  → found + active=true
  ↓
SubscriptionPersistencePort.save(subscription)
  → tenant_subscription updated (version++)
  ↓
AfterCommit.run(() → publish TenantSubscriptionUpdatedEvent)
  ↓
Response: success
```

---

### Scénario 2 : Apply retired plan (rejection)

```
User/Admin → ApplyTenantPlanCommand(tenantId=T, planCode="old-v0")
  ↓
ApplyTenantPlanCommandHandler (@TchTx)
  ↓
PlanCatalog.findByCode("old-v0")
  → Optional.empty (soft-deleted)
  ↓
Throw IllegalArgumentException("Plan not found or retired")
  ↓
Response: 400 Bad Request (no write, no event)
```

---

### Scénario 3 : Resolve subscription (query)

```
User → ResolveTenantSubscriptionQuery(tenantId=T)
  ↓
ResolveTenantSubscriptionQueryHandler
  ↓
SubscriptionReaderPort.findByTenantId(T)
  → found: Subscription(planCode="pro-v1", status=ACTIVE)
  ↓
Return SubscriptionView(tenantId=T, planCode="pro-v1", status=ACTIVE, ...)
```

---

## Impacts base de données

### Table `billing_plan` (catalog, global)

```sql
CREATE TABLE billing_plan (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  code VARCHAR(128) NOT NULL UNIQUE,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  price_amount DECIMAL(19,2),
  currency VARCHAR(3),
  billing_period VARCHAR(50),
  limits_json JSONB,
  features_json JSONB,
  active BOOLEAN NOT NULL DEFAULT true,
  is_default BOOLEAN NOT NULL DEFAULT false,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now(),
  created_by UUID,
  updated_by UUID,
  deleted_at TIMESTAMP,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_billing_plan_code ON billing_plan(code) WHERE deleted_at IS NULL;
CREATE INDEX idx_billing_plan_active ON billing_plan(active) WHERE deleted_at IS NULL;
```

### Table `tenant_subscription` (core, tenant-scoped)

```sql
CREATE TABLE tenant_subscription (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id UUID NOT NULL,
  plan_code VARCHAR(128) NOT NULL,
  status VARCHAR(50) NOT NULL, -- TRIAL, ACTIVE, SUSPENDED, CANCELED, EXPIRED
  started_at TIMESTAMP,
  ends_at TIMESTAMP,
  trial_ends_at TIMESTAMP,
  canceled_at TIMESTAMP,
  metadata_json JSONB,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now(),
  created_by UUID,
  updated_by UUID,
  deleted_at TIMESTAMP,
  version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_tenant_subscription_tenant ON tenant_subscription(tenant_id) WHERE deleted_at IS NULL;
-- RLS policy: tenant_id = current_setting('app.current_tenant')::uuid
```

**Note** : Pas de FK `plan_code → billing_plan.code` volontairement (découplage, permet plan retiré).

---

## Impacts API

### Catalog Plan API (platform admin)

**Endpoints** :

- `GET /platform/plans` → list active plans
- `GET /platform/plans/{id}` → get plan by id
- `POST /platform/plans` → create plan
- `PUT /platform/plans/{id}` → update plan
- `DELETE /platform/plans/{id}` → soft-delete plan
- `POST /platform/plans/{id}/deactivate` → deactivate plan

**Security** : `@PreAuthorize("hasAuthority('SUPER_ADMIN')")`

### Core Subscription API (tenant)

**Endpoints** (optionnel, peut être via CommandBus interne) :

- `GET /tenant/subscription` → resolve effective subscription
- `POST /tenant/subscription/apply` → apply plan (admin tenant)
- `POST /tenant/subscription/suspend` → suspend (admin tenant)

**Security** : tenant-scoped, `@PreAuthorize("hasAuthority('TENANT_ADMIN')")`

---

## Observabilité

### Logs structurés

**Catalog admin writes** :

```json
{
  "level": "INFO",
  "message": "Plan created",
  "planId": "...",
  "code": "pro-v1",
  "active": true
}
```

**Subscription command** :

```json
{
  "level": "INFO",
  "message": "Subscription applied",
  "tenantId": "...",
  "planCode": "pro-v1",
  "status": "ACTIVE",
  "version": 2
}
```

### Événements

**TenantSubscriptionUpdatedEvent** :

```json
{
  "tenantId": "...",
  "planCode": "pro-v1",
  "status": "ACTIVE",
  "version": 2,
  "timestamp": "2026-01-23T18:00:00Z",
  "initiator": "admin@example.com"
}
```

---

## Sécurité

### Catalog/plan

- Admin CRUD : `SUPER_ADMIN` uniquement
- Read API : peut être exposé publiquement (liste plans disponibles)

### Core/subscription

- Apply/suspend : `TENANT_ADMIN` ou `TENANT_MANAGER`
- Read subscription : authenticated users within tenant

---

## Performance

### Catalog/plan

- Reads cachés (Caffeine/Spring Cache)
- Cache eviction sur admin writes
- Target : < 10ms pour `listActive()` (cache hit)

### Core/subscription

- Read subscription : < 50ms (query simple, index tenant_id)
- Write subscription : < 200ms (tx + event publication)
- Pas de cache sur writes (toujours fresh)

---

## Alternatives considérées et rejetées

### Alt 1 : FK hard `tenant_subscription.plan_code → billing_plan.code`

**Rejeté** : empêche soft-delete des plans, casse découplage, nécessite migrations complexes.

### Alt 2 : Stocker `plan_id` (UUID) au lieu de `plan_code`

**Rejeté** : `code` est plus stable et lisible en externe, `plan_id` est technique interne.

### Alt 3 : Subscription dans catalog

**Rejeté** : viole principe catalog (pas de lifecycle, pas d'événements).

---

## Future enhancements (hors scope)

- Historique des souscriptions (table `subscription_history`)
- Renouvellements automatiques (scheduler batch)
- Intégration Stripe/paiement
- Plan features evaluation engine (entitlements complexes)
- Multi-plans par tenant (stacking, add-ons)

---

## Références

- Inspiration : `catalog/theme` + `core/tenanttheme`
- Conventions : `docs/conventions/{typed_ids,command_query_handlers,cache,event_model}.md`
- Catalog rules : `openspec/context/75-catalog-rules.md`
