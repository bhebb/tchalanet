# Tasks — billing-plan-subscription-split

## Objectif

Implémenter la séparation architecturale catalog/plan (référentiel) + core/subscription (lifecycle tenant) avec validation, événements et idempotence.

---

## Phase A — Catalog/plan (référentiel global)

### A1 — Structure et persistence

- [ ] A1.1 Créer structure `catalog/plan` conforme `75-catalog-rules.md`

  ```
  catalog/plan/
    api/
      PlanCatalog.java
      PlanView.java
    internal/
      read/PlanCatalogImpl.java
      write/PlanAdminService.java
      mapper/PlanMapper.java
      cache/PlanCacheNames.java
      persistence/PlanJpaEntity.java
      persistence/PlanJpaRepository.java
      web/PlanAdminController.java
  ```

- [ ] A1.2 Créer `PlanId` dans `common.types.id` (typed ID)

  - Pattern canonique : `record PlanId(UUID value)`
  - Méthodes : `of()`, `nullableOf()`, `parse()`

- [ ] A1.3 Ajouter mapping dans `CommonIdMapper`

  - `mapToPlanId(UUID)` / `mapFromPlanId(PlanId)`

- [ ] A1.4 Créer converter Spring `StringToPlanIdConverter`
  - Délègue à `PlanId.parse()`

### A2 — Entity et repository

- [ ] A2.1 Créer `PlanJpaEntity extends BaseEntity`

  - `code` (String, unique, indexed)
  - `name` (String)
  - `description` (String, nullable)
  - `priceAmount` (BigDecimal, nullable) ou `priceJson` (jsonb)
  - `currency` (String, nullable)
  - `billingPeriod` (String, nullable) ex: "MONTHLY", "YEARLY"
  - `limitsJson` (jsonb, nullable)
  - `featuresJson` (jsonb, nullable)
  - `active` (boolean, default true)
  - `defaultPlan` (boolean, default false) — optionnel si "plan par défaut"
  - Audit columns (BaseEntity) : created_at, updated_at, version, deleted_at

- [ ] A2.2 Créer `PlanJpaRepository extends JpaRepository`
  - `findFirstByCodeIgnoreCaseAndDeletedAtIsNull(String code)`

### A3 — API publique (read contract)

- [ ] A3.1 Créer `PlanView` (record immutable)

  - Tous les champs du plan (id, code, name, price, limits, features, active)

- [ ] A3.2 Créer `PlanCatalog` (interface publique dans `api/`)

  - `List<PlanView> listActive()`
  - `Optional<PlanView> findByCode(String code)`
  - `Optional<PlanView> findById(PlanId id)`

- [ ] A3.3 Créer `PlanCatalogImpl` (dans `internal/read`)

  - Implémente `PlanCatalog`
  - Filtre `deleted_at IS NULL` sur tous les reads
  - `listActive()` filtre aussi `active=true`
  - Cache : `@Cacheable` sur méthodes

- [ ] A3.4 Créer `PlanCacheNames`
  - `ACTIVE_PLANS = "catalog.plan.cache.ACTIVE_PLANS"`
  - `PLAN_BY_CODE = "catalog.plan.cache.PLAN_BY_CODE"`
  - `PLAN_BY_ID = "catalog.plan.cache.PLAN_BY_ID"`

### A4 — Admin service (write)

- [ ] A4.1 Créer `PlanAdminService` (dans `internal/write`)

  - `create(PlanCreateRequest)` → `PlanView`
  - `update(PlanId, PlanUpdateRequest)` → `PlanView`
  - `deactivate(PlanId)` → void (set `active=false`)
  - `softDelete(PlanId)` → void (set `deleted_at=now()` + `active=false`)
  - Cache eviction : `@CacheEvict` sur toutes les méthodes

- [ ] A4.2 Créer `PlanAdminController` (dans `internal/web`)
  - `POST /platform/plans` → create
  - `PUT /platform/plans/{id}` → update
  - `DELETE /platform/plans/{id}` → softDelete
  - `POST /platform/plans/{id}/deactivate` → deactivate
  - Security : `@PreAuthorize("hasAuthority('SUPER_ADMIN')")`

### A5 — Mapper

- [ ] A5.1 Créer `PlanMapper` (MapStruct)
  - `@Mapper(componentModel = "spring", uses = {CommonIdMapper.class})`
  - `toView(PlanJpaEntity)` → `PlanView`
  - Gestion jsonb → JsonNode si nécessaire (via ObjectMapper)

---

## Phase B — Core/subscription (lifecycle tenant)

### B1 — Structure et domain

- [ ] B1.1 Créer structure `core/subscription`
  ```
  core/subscription/
    domain/
      model/Subscription.java
      model/SubscriptionStatus.java (enum)
      exception/
    application/
      command/
        model/ApplyTenantPlanCommand.java
        model/SuspendSubscriptionCommand.java (optionnel MVP)
        handler/ApplyTenantPlanCommandHandler.java
      query/
        model/ResolveTenantSubscriptionQuery.java
        model/SubscriptionView.java
        handler/ResolveTenantSubscriptionQueryHandler.java
      event/TenantSubscriptionUpdatedEvent.java
      port/out/
        SubscriptionPersistencePort.java
        SubscriptionReaderPort.java
    infra/
      persistence/
        SubscriptionJpaEntity.java
        SubscriptionJpaRepository.java
        SubscriptionPersistenceAdapter.java
      web/SubscriptionController.java (optionnel)
  ```

### B2 — Domain model

- [ ] B2.1 Créer `SubscriptionStatus` (enum)

  - `TRIAL`, `ACTIVE`, `SUSPENDED`, `CANCELED`, `EXPIRED`

- [ ] B2.2 Créer `Subscription` (domain model, record)
  - `tenantId` (TenantId)
  - `planCode` (String)
  - `status` (SubscriptionStatus)
  - `startedAt` (Instant, nullable)
  - `endsAt` (Instant, nullable)
  - `trialEndsAt` (Instant, nullable)
  - `canceledAt` (Instant, nullable)
  - `metadataJson` (Map<String, String>)
  - `version` (long)
  - `createdAt`, `updatedAt`, `createdBy`

### B3 — Persistence (RLS)

- [ ] B3.1 Créer `SubscriptionJpaEntity extends BaseEntity`

  - `tenantId` (UUID, indexed, RLS)
  - `planCode` (String)
  - `status` (String ou @Enumerated)
  - `startedAt`, `endsAt`, `trialEndsAt`, `canceledAt`
  - `metadataJson` (jsonb via @JdbcTypeCode)
  - Audit columns (version, created_at, updated_at, created_by)

- [ ] B3.2 Créer `SubscriptionJpaRepository`

  - `findByTenantId(UUID)` (RLS enforced)

- [ ] B3.3 Créer `SubscriptionPersistenceAdapter`
  - Implémente `SubscriptionPersistencePort` + `SubscriptionReaderPort`
  - `save(Subscription)` → `Subscription`
  - `findByTenantId(TenantId)` → `Optional<Subscription>`

### B4 — Commands

- [ ] B4.1 Créer `ApplyTenantPlanCommand`

  - `tenantId` (TenantId)
  - `planCode` (String)
  - `effectiveAt` (Instant, nullable)
  - `idempotencyKey` (String, nullable) — future idempotence

- [ ] B4.2 Créer `ApplyTenantPlanCommandHandler`
  - Inject : `PlanCatalog`, `SubscriptionPersistencePort`, `SubscriptionReaderPort`, `ApplicationEventPublisher`, `Clock`
  - `@TchTx` (transaction)
  - Valider plan via `PlanCatalog.findByCode(planCode)`
  - Rejeter si plan soft-deleted
  - Accepter plan inactif (policy : legacy tenants)
  - Persist/update subscription avec version++
  - Publish `TenantSubscriptionUpdatedEvent` via `AfterCommit.run(...)`

### B5 — Queries

- [ ] B5.1 Créer `SubscriptionView` (record)

  - `tenantId`, `planCode`, `status`, `startedAt`, `endsAt`, `version`, `updatedAt`

- [ ] B5.2 Créer `ResolveTenantSubscriptionQuery`

  - `tenantId` (TenantId)

- [ ] B5.3 Créer `ResolveTenantSubscriptionQueryHandler`
  - Inject : `SubscriptionReaderPort`
  - Read-only (pas de `@TchTx`)
  - Retourne `SubscriptionView` ou null si pas de subscription

### B6 — Events

- [ ] B6.1 Créer `TenantSubscriptionUpdatedEvent`

  - `tenantId`, `planCode`, `status`, `version`, `timestamp`, `initiator`

- [ ] B6.2 Wiring event publisher
  - `AfterCommit.run(() -> eventPublisher.publishEvent(event))`

---

## Phase C — Integration & bootstrap

### C1 — Bootstrap DTO

- [ ] C1.1 Ajouter champs subscription dans `TenantBootstrapResponse`

  - `subscription` (SubscriptionView, nullable)
  - `planName` (String, optionnel via catalog lookup)

- [ ] C1.2 Modifier `BootstrapQueryHandler`
  - Appeler `ResolveTenantSubscriptionQuery`
  - Optionnel : lookup plan name via `PlanCatalog.findByCode()`

### C2 — Tests

- [ ] C2.1 Tests unitaires `PlanMapper`
- [ ] C2.2 Tests unitaires `ApplyTenantPlanCommandHandler`

  - Mock `PlanCatalog`, `SubscriptionPersistencePort`
  - Vérifier validation plan
  - Vérifier event publication

- [ ] C2.3 Tests intégration `SubscriptionCommandHandlerTest`

  - Testcontainers Postgres
  - RLS verification
  - Idempotency retries

- [ ] C2.4 ArchUnit guards
  - `catalog.plan.api` ne dépend pas de `catalog.plan.internal`
  - `core.subscription` ne dépend pas de `catalog.plan.internal`

---

## Critères d'acceptation

- [ ] `catalog/plan` ne contient aucun événement métier
- [ ] `core/subscription` valide plans via `PlanCatalog` API uniquement
- [ ] Commands idempotentes (retry safe)
- [ ] Events publiés after-commit (`AfterCommit.run`)
- [ ] ArchUnit guards passent
- [ ] Bootstrap retourne subscription + plan metadata
- [ ] Tests unitaires + intégration passent

---

## Notes

- Migration SQL pour `billing_plan` et `tenant_subscription` à créer séparément
- RLS policies Postgres pour `tenant_subscription` à définir
- Idempotence strategy : version optimiste (@Version JPA) ou idempotency key (future)
- Plan pricing : MVP simple (fields directs), engine complexe en follow-up si besoin
