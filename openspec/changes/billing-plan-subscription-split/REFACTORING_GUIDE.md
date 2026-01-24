# Refactoring Guide — catalog/billing → catalog/plan + core/subscription

## Vue d'ensemble

Le code existant dans `catalog/billing` doit être refactoré selon le split architectural :

- **catalog/plan** : référentiel global (plans disponibles)
- **core/subscription** : lifecycle tenant (souscriptions)

---

## Fichiers à déplacer vers `catalog/plan`

### ✅ Domain model Plan (si nécessaire)

- `catalog/billing/domain/model/Plan.java` → `catalog/plan/domain/model/Plan.java` (optionnel)
- **Simplifier** : retirer logique métier, garder structure de données pure

### ✅ Persistence

**Créer nouveau** (pas de migration directe) :

- `catalog/plan/internal/persistence/PlanJpaEntity.java`
  - Mapper depuis `billing_plan` table (nouvelle structure)
  - Colonnes : `code`, `name`, `price_amount`, `currency`, `billing_period`, `limits_json`, `features_json`, `active`, `is_default`
- `catalog/plan/internal/persistence/PlanJpaRepository.java`
  - `findByCodeAndDeletedAtIsNull(String code)`
  - `findAllByDeletedAtIsNullAndActiveTrue()`

### ✅ API publique (nouveau)

- `catalog/plan/api/PlanCatalog.java` (interface)

  - `List<PlanView> listActive()`
  - `Optional<PlanView> findByCode(String code)`
  - `Optional<PlanView> findById(PlanId id)`

- `catalog/plan/api/PlanView.java` (record immutable)
  - Tous champs exposés (id, code, name, price, limits, features, active)

### ✅ Admin service (nouveau)

- `catalog/plan/internal/write/PlanAdminService.java`
  - `create(PlanCreateRequest)` → `PlanView`
  - `update(PlanId, PlanUpdateRequest)` → `PlanView`
  - `deactivate(PlanId)` → void
  - `softDelete(PlanId)` → void

### ✅ Mapper

- `catalog/plan/internal/mapper/PlanMapper.java` (MapStruct)
  - `@Mapper(componentModel = "spring", uses = {CommonIdMapper.class})`
  - `toView(PlanJpaEntity)` → `PlanView`

### ✅ Cache

- `catalog/plan/internal/cache/PlanCacheNames.java`
  - `ACTIVE_PLANS = "catalog.plan.cache.ACTIVE_PLANS"`
  - `PLAN_BY_CODE = "catalog.plan.cache.PLAN_BY_CODE"`

### ✅ Web controller (admin)

- `catalog/plan/internal/web/PlanAdminController.java`
  - `@PreAuthorize("hasAuthority('SUPER_ADMIN')")`
  - Endpoints : POST/PUT/DELETE plans

---

## Fichiers à déplacer vers `core/subscription`

### ✅ Domain model

**Source** : `catalog/billing/domain/model/Subscription.java`  
**Destination** : `core/subscription/domain/model/Subscription.java`

**Changements requis** :

```java
// AVANT (catalog/billing)
public record Subscription(
    SubscriptionId id,
    TenantId tenantId,
    PlanId planId,  // ❌ référence directe
    SubscriptionStatus status,
    // ...
) {}

// APRÈS (core/subscription)
public record Subscription(
    SubscriptionId id,
    TenantId tenantId,
    String planCode,  // ✅ soft reference (pas de PlanId)
    SubscriptionStatus status,
    Instant startedAt,
    Instant endsAt,
    Instant trialEndsAt,
    Instant canceledAt,
    Map<String, String> metadata,
    long version,
    Instant createdAt,
    Instant updatedAt,
    String createdBy
) {
  // Retirer logique métier (scheduleCancellation, cancelNow, etc.)
  // → déplacer vers command handlers
}
```

**Méthodes métier à retirer du record** :

- `scheduleCancellation()` → `CancelSubscriptionCommandHandler`
- `cancelNow()` → `CancelSubscriptionCommandHandler`
- `resume()` → `ResumeSubscriptionCommandHandler`
- `suspend()` → `SuspendSubscriptionCommandHandler`
- `changePlan()` → `ChangePlanCommandHandler`
- `renew()` → `RenewSubscriptionCommandHandler`

### ✅ Enum SubscriptionStatus

**Source** : `catalog/billing/domain/model/SubscriptionStatus.java`  
**Destination** : `core/subscription/domain/model/SubscriptionStatus.java`

**Vérifier les valeurs** :

```java
public enum SubscriptionStatus {
  TRIAL,
  ACTIVE,
  SUSPENDED,
  CANCELED,
  EXPIRED
}
```

### ✅ Exceptions domain

**Source** : `catalog/billing/domain/exception/*`  
**Destination** : `core/subscription/domain/exception/*`

Déplacer :

- `SubscriptionAlreadyCanceledException`
- `SubscriptionCannotBeCanceledException`
- `SubscriptionCannotBeResumedException`
- `SubscriptionCannotBeSuspendedException`

### ✅ Persistence

**Source** : `catalog/billing/infra/persistence/SubscriptionJpaEntity.java`  
**Destination** : `core/subscription/infra/persistence/SubscriptionJpaEntity.java`

**Changements requis** :

```java
// AVANT
@Entity
@Table(name = "subscription")
public class SubscriptionJpaEntity extends BaseEntity {
  private UUID tenantId;
  private UUID planId;  // ❌ FK vers plan
  private String status;
  private Instant currentPeriodStart;
  private Instant currentPeriodEnd;
  private boolean cancelAtPeriodEnd;
  private String billingProvider;  // ❌ à retirer
  private String billingExternalId;  // ❌ à retirer
  private Map<String, Object> meta;
}

// APRÈS
@Entity
@Table(name = "tenant_subscription")
public class SubscriptionJpaEntity extends BaseEntity {
  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(name = "plan_code", nullable = false, length = 128)
  private String planCode;  // ✅ soft reference (string)

  @Column(name = "status", nullable = false, length = 50)
  private String status;  // TRIAL|ACTIVE|SUSPENDED|CANCELED|EXPIRED

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "ends_at")
  private Instant endsAt;

  @Column(name = "trial_ends_at")
  private Instant trialEndsAt;

  @Column(name = "canceled_at")
  private Instant canceledAt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata_json", columnDefinition = "jsonb")
  private Map<String, String> metadataJson;

  // Pas de billingProvider, billingExternalId
  // → déplacer vers un module billing-integration séparé si besoin
}
```

**Repository** :
**Source** : `catalog/billing/infra/persistence/SubscriptionJpaRepository.java`  
**Destination** : `core/subscription/infra/persistence/SubscriptionJpaRepository.java`

```java
public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionJpaEntity, UUID> {
  Optional<SubscriptionJpaEntity> findByTenantIdAndDeletedAtIsNull(UUID tenantId);
  // RLS enforced via Postgres policies
}
```

### ✅ Ports (application)

**Source** : `catalog/billing/application/port/out/*`  
**Destination** : `core/subscription/application/port/out/*`

Déplacer et renommer :

- `SubscriptionReaderPort` → conserver
- `SubscriptionWriterPort` → conserver ou fusionner dans `SubscriptionPersistencePort`

**Créer nouveaux ports** :

- `core/subscription/application/port/out/SubscriptionPersistencePort.java`
- `core/subscription/application/port/out/SubscriptionReaderPort.java`

### ✅ Adapters

**Créer** : `core/subscription/infra/persistence/SubscriptionPersistenceAdapter.java`

```java
@Component
@RequiredArgsConstructor
public class SubscriptionPersistenceAdapter
    implements SubscriptionPersistencePort, SubscriptionReaderPort {

  private final SubscriptionJpaRepository repository;

  @Override
  public Subscription save(Subscription subscription) {
    var entity = repository.findByTenantIdAndDeletedAtIsNull(subscription.tenantId().value())
      .orElse(new SubscriptionJpaEntity());

    entity.setTenantId(subscription.tenantId().value());
    entity.setPlanCode(subscription.planCode());  // ✅ string, pas UUID
    entity.setStatus(subscription.status().name());
    entity.setStartedAt(subscription.startedAt());
    entity.setEndsAt(subscription.endsAt());
    entity.setTrialEndsAt(subscription.trialEndsAt());
    entity.setCanceledAt(subscription.canceledAt());
    entity.setMetadataJson(subscription.metadata());

    var saved = repository.save(entity);
    return toDomain(saved);
  }

  @Override
  public Optional<Subscription> findByTenantId(TenantId tenantId) {
    return repository.findByTenantIdAndDeletedAtIsNull(tenantId.value())
      .map(this::toDomain);
  }

  private Subscription toDomain(SubscriptionJpaEntity entity) {
    return new Subscription(
      SubscriptionId.of(entity.getId()),
      TenantId.of(entity.getTenantId()),
      entity.getPlanCode(),  // ✅ string
      SubscriptionStatus.valueOf(entity.getStatus()),
      entity.getStartedAt(),
      entity.getEndsAt(),
      entity.getTrialEndsAt(),
      entity.getCanceledAt(),
      entity.getMetadataJson(),
      entity.getVersion(),
      entity.getCreatedAt(),
      entity.getUpdatedAt(),
      entity.getCreatedBy() != null ? entity.getCreatedBy().toString() : "system"
    );
  }
}
```

### ✅ Command handlers (nouveaux)

**Créer** dans `core/subscription/application/command/handler/` :

1. **ApplyTenantPlanCommandHandler**

```java
@UseCase
@RequiredArgsConstructor
public class ApplyTenantPlanCommandHandler
    implements CommandHandler<ApplyTenantPlanCommand, ApplyTenantPlanResult> {

  private final PlanCatalog planCatalog;  // ✅ API publique catalog/plan
  private final SubscriptionPersistencePort persistencePort;
  private final SubscriptionReaderPort readerPort;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock clock;

  @Override
  @TchTx
  public ApplyTenantPlanResult handle(ApplyTenantPlanCommand cmd) {
    // 1. Valider plan via catalog (API publique)
    var plan = planCatalog.findByCode(cmd.planCode())
      .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + cmd.planCode()));

    if (!plan.active()) {
      throw new IllegalArgumentException("Plan is inactive: " + cmd.planCode());
    }

    // 2. Lire subscription existante
    var existing = readerPort.findByTenantId(cmd.tenantId());

    // 3. Créer/mettre à jour
    var subscription = new Subscription(
      existing.map(Subscription::id).orElse(SubscriptionId.of(UUID.randomUUID())),
      cmd.tenantId(),
      cmd.planCode(),  // ✅ string, pas PlanId
      SubscriptionStatus.ACTIVE,
      cmd.effectiveAt() != null ? cmd.effectiveAt() : Instant.now(clock),
      null, // ends_at
      null, // trial_ends_at
      null, // canceled_at
      Map.of(),
      existing.map(s -> s.version() + 1).orElse(1L),
      existing.map(Subscription::createdAt).orElse(Instant.now(clock)),
      Instant.now(clock),
      "system" // TODO: from security context
    );

    var saved = persistencePort.save(subscription);

    // 4. Event after-commit
    AfterCommit.run(() -> {
      eventPublisher.publishEvent(new TenantSubscriptionUpdatedEvent(
        saved.tenantId(),
        saved.planCode(),
        saved.status(),
        saved.version(),
        Instant.now(clock),
        "system"
      ));
    });

    return new ApplyTenantPlanResult(saved.id(), saved.status());
  }
}
```

2. **CancelSubscriptionCommandHandler**
3. **ChangePlanCommandHandler**
4. **RenewSubscriptionCommandHandler**
5. **ResumeSubscriptionCommandHandler**
6. **SuspendSubscriptionCommandHandler**

(Patterns similaires, déplacer logique depuis `Subscription` domain model)

### ✅ Query handlers

**Créer** : `core/subscription/application/query/handler/ResolveTenantSubscriptionQueryHandler.java`

```java
@UseCase
@RequiredArgsConstructor
public class ResolveTenantSubscriptionQueryHandler
    implements QueryHandler<ResolveTenantSubscriptionQuery, SubscriptionView> {

  private final SubscriptionReaderPort readerPort;

  @Override
  public SubscriptionView handle(ResolveTenantSubscriptionQuery query) {
    return readerPort.findByTenantId(query.tenantId())
      .map(s -> new SubscriptionView(
        s.tenantId(),
        s.planCode(),  // ✅ string
        s.status(),
        s.startedAt(),
        s.endsAt(),
        s.version(),
        s.updatedAt()
      ))
      .orElse(null);
  }
}
```

---

## Fichiers à NE PAS déplacer (hors scope billing split)

### ❌ BillingProvider integration

**Fichiers** :

- `catalog/billing/application/port/out/BillingProviderPort.java`
- `catalog/billing/domain/model/BillingProvider.java`
- `catalog/billing/infra/service/*` (si implémentations Stripe/etc.)

**Action** : Laisser dans `catalog/billing` ou créer un nouveau module `integration/billing-provider` séparé.

**Rationale** : L'intégration avec providers externes (Stripe, PayPal) n'est pas couverte par le split catalog/plan + core/subscription. C'est un concern orthogonal.

### ❌ Batch jobs

**Fichiers** :

- `catalog/billing/infra/batch/RenewSubscriptionsTasklet.java`

**Action** : Déplacer vers `core/subscription/infra/batch/` si c'est un job de renouvellement automatique, ou laisser hors scope MVP.

---

## Changements critiques à faire

### 1. Remplacer référence PlanId → planCode (string)

**Partout dans subscription** :

```java
// ❌ AVANT
PlanId planId;
var plan = planRepository.findById(subscription.planId());

// ✅ APRÈS
String planCode;
var plan = planCatalog.findByCode(subscription.planCode());
```

### 2. Valider plan via PlanCatalog (API publique)

**Dans tous les handlers** :

```java
// ❌ INTERDIT
@Autowired PlanJpaRepository planRepository;
@Autowired PlanService planService;

// ✅ CORRECT
@Autowired PlanCatalog planCatalog;  // API publique catalog/plan
```

### 3. Retirer logique métier du record Subscription

**Déplacer vers handlers** :

- `scheduleCancellation()` → `CancelSubscriptionCommandHandler`
- `resume()` → `ResumeSubscriptionCommandHandler`
- etc.

### 4. Adapter table name et colonnes

**Entity** :

```java
@Entity
@Table(name = "tenant_subscription")  // ✅ nouveau nom
public class SubscriptionJpaEntity {
  // Colonnes adaptées à V21__core_billing.sql
}
```

---

## Checklist de refactoring

### Phase 1 : catalog/plan (nouveau module)

- [ ] Créer structure `catalog/plan/{api,internal/{read,write,mapper,cache,persistence,web}}`
- [ ] Créer `PlanJpaEntity` (table `billing_plan`)
- [ ] Créer `PlanJpaRepository`
- [ ] Créer `PlanCatalog` interface (API publique)
- [ ] Créer `PlanCatalogImpl` avec cache
- [ ] Créer `PlanAdminService`
- [ ] Créer `PlanMapper` (MapStruct + CommonIdMapper)
- [ ] Créer `PlanCacheNames`
- [ ] Créer `PlanAdminController`
- [ ] Tests unitaires + intégration

### Phase 2 : core/subscription refactoring

- [ ] Déplacer `Subscription` domain model
- [ ] Retirer logique métier du record (vers handlers)
- [ ] Changer `PlanId planId` → `String planCode`
- [ ] Déplacer `SubscriptionStatus` enum
- [ ] Déplacer exceptions domain
- [ ] Adapter `SubscriptionJpaEntity` (table `tenant_subscription`)
- [ ] Adapter `SubscriptionJpaRepository`
- [ ] Créer `SubscriptionPersistenceAdapter`
- [ ] Créer ports (`SubscriptionPersistencePort`, `SubscriptionReaderPort`)

### Phase 3 : Command handlers

- [ ] Créer `ApplyTenantPlanCommandHandler` (avec `PlanCatalog`)
- [ ] Créer `CancelSubscriptionCommandHandler`
- [ ] Créer `ChangePlanCommandHandler`
- [ ] Créer `RenewSubscriptionCommandHandler`
- [ ] Créer `ResumeSubscriptionCommandHandler`
- [ ] Créer `SuspendSubscriptionCommandHandler`

### Phase 4 : Query handlers

- [ ] Créer `ResolveTenantSubscriptionQueryHandler`

### Phase 5 : Events

- [ ] Créer `TenantSubscriptionUpdatedEvent`
- [ ] Créer `TenantSubscriptionCanceledEvent`
- [ ] Créer `TenantSubscriptionRenewedEvent`
- [ ] Créer `TenantSubscriptionResumedEvent`
- [ ] Créer `TenantSubscriptionSuspendedEvent`

### Phase 6 : Tests

- [ ] Tests unitaires handlers
- [ ] Tests intégration (Testcontainers Postgres)
- [ ] ArchUnit guards

### Phase 7 : Nettoyage

- [ ] Supprimer `catalog/billing/domain/model/Subscription.java` (ancien)
- [ ] Supprimer `catalog/billing/infra/persistence/SubscriptionJpaEntity.java` (ancien)
- [ ] Supprimer ancien controller si existe
- [ ] Vérifier aucune référence vers `catalog/billing` dans subscription

---

## Ordre d'exécution recommandé

1. **Créer catalog/plan** (greenfield, pas de migration)
2. **Adapter subscription persistence** (entity + repository)
3. **Créer adapters** (ports implementation)
4. **Refactorer domain model** (retirer méthodes métier)
5. **Créer command handlers** (un par un)
6. **Créer query handlers**
7. **Migrer tests**
8. **Supprimer ancien code**

---

## Conformité

- ✅ `typed_ids.md` : `PlanId` in API, `String planCode` in subscription
- ✅ `command_query_handlers.md` : `@UseCase`, `@TchTx`, `AfterCommit`
- ✅ `inter_domain_calls.md` : `PlanCatalog` API publique uniquement
- ✅ `75-catalog-rules.md` : catalog/plan structure conforme
- ✅ Spec P1-P5, S1-S8 : tous requirements respectés
