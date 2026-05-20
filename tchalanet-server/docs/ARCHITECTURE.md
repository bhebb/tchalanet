# Tchalanet – Architecture Applicative (Server)

Ce document décrit l'architecture du backend **tchalanet-server**.
Il est **structurel** : frontières, responsabilités, règles non négociables.
Les détails d'implémentation et patterns concrets vivent dans `docs/conventions/*`.

## Objectifs

- Stabiliser l'architecture (hexagonal + CQRS) dans les domaines critiques (`core/`)
- Livrer vite via des **vertical slices** (`features/`)
- Garder `common/` strictement technique
- Clarifier le rôle de `catalog/` (référentiels read-mostly)
- Uniformiser le **contrat HTTP** : 2xx = `ApiResponse<T>` ; erreurs = `ProblemDetail` (RFC7807)

## Admin feature boundaries

Admin controllers follow the same layer ownership rules as the rest of the backend:

- Mono-domain tenant admin CRUD lives in the owning core under `core/<bc>/infra/web/admin`.
- Mono-catalog platform admin CRUD lives in the owning catalog under `catalog/<bc>/internal/web`.
- `features/tenantadmin` and `features/platformadmin` are reserved for composite BFF endpoints that aggregate at least two cores/catalogs.

Current notable placements:

| Surface                        | Active placement                                                  |
| ------------------------------ | ----------------------------------------------------------------- |
| Tenant admin users             | `core.tenantuser.infra.web.admin.TenantUserAdminController`       |
| Tenant admin outlets           | `core.outlet.infra.web.admin.OutletAdminController`               |
| Tenant admin terminals         | `core.terminal.infra.web.admin.TerminalAdminController`           |
| Tenant admin limits            | `core.limitpolicy.infra.web.admin.LimitPolicyAdminController`     |
| Tenant admin autonomy          | `core.autonomy.infra.web.admin.AutonomyAdminController`           |
| Tenant admin policies overview | `features.tenantadmin.policies.web.TenantAdminPoliciesController` |
| Platform admin i18n            | `catalog.i18n.internal.web.PlatformI18nOverridesController`       |
| Platform admin settings        | `catalog.settings.internal.web.PlatformSettingsController`        |
| Platform admin theme presets   | `catalog.theme.internal.web.ThemeAdminController`                 |
| Platform admin overview        | `features.platformadmin.overview.PlatformAdminOverviewController` |

## Hiérarchie de documentation (source de vérité)

En cas de conflit :

1. `docs/ARCHITECTURE.md` (structure & frontières)
2. `docs/PLAYBOOK.md` (workflow & DoD)
3. `docs/conventions/*` (normes techniques détaillées)
4. OpenSpec (dérivé, jamais source de vérité)

---

## 1) 5-Layer Architecture

```
common/       ← Technical/transversal (Bus, EventPublisher, TypedIDs, Cache, TchContext)
catalog/      ← Reference data / lookup (read-mostly, no domain events)
platform/     ← Cross-cutting application services (audit, identity, tenantconfig, notification…)
core/         ← Business domains (sales, draw, drawresult, uslottery, haiti)
features/     ← Vertical slices / BFF (ops, tenantadmin, publicdraw, reporting)
```

### Layer Rules

- **common**: Technical only. No domain, no events. Shared by all layers.
- **catalog**: Read-mostly reference data. Admin CRUD via `/platform/**` (SUPER_ADMIN). No business invariants. No events.
- **platform**: Transversal application services with state or lifecycle. Not business-critical. Exposes only `api/`. Cannot depend on `core/` or `features/`.
- **core**: Domain logic, aggregates, value objects. Hexagonal architecture (domain, application, infra). Events published. May consume `platform.<capability>.api`.
- **features**: Orchestration, BFF, vertical slices. Multi-domain composition. Thin application layer.

### Dependency Graph

```text
common
  ↑
catalog
  ↑
platform
  ↑
core
  ↑
features
```

| From | May depend on |
|---|---|
| common | external libraries only |
| catalog | common |
| platform | common, catalog api |
| core | common, catalog api, platform api, other core api (where allowed) |
| features | common, catalog api, platform api, core api |

### Module Archetypes

Each layer uses a distinct internal architectural pattern:

| Layer | Archetype | Internal Pattern |
|---|---|---|
| common | Technical Shared Kernel | Utilities, bus interfaces, context, typed IDs, no business logic |
| catalog | Simple DDD / Reference Catalog | `api/` + `internal/{read,write,persistence,mapper,cache,web}/` |
| platform | Application Service Module | `api/` (XxxApi interface) + `internal/` (service, persistence, web, event…) |
| core | Clean Architecture / Hexagonal / CQRS | `api/` + `internal/{domain,application,infra}/`, CommandBus, QueryBus |
| features | Vertical Slice / BFF Leaf Module | `<feature>/<slice>/{web,app,model,mapper}/`, no Java API exposed |

See: `docs/modules/` for the complete description of each archetype.

---

## 1.5) Common Layer — Technical Shared Kernel

**Common** provides shared technical primitives used by ALL layers. It contains ONLY infrastructure — no domain, no business rules.

```
common/
  ├── bus/                 ← CommandBus, QueryBus (interfaces + in-memory impl)
  ├── event/               ← DomainEventPublisher, AfterCommit
  ├── context/             ← TchRequestContext, TchContext, thread-local management
  ├── types/id/            ← Typed ID base classes, ID factories
  ├── cache/               ← CacheSpecProvider, cache abstractions
  ├── security/            ← TchPermissionEvaluator (technical glue only)
  ├── persistence/         ← RLS, soft-delete, audit infrastructure
  ├── web/                 ← ApiResponse, TchPage, ProblemRest
  ├── tx/                  ← @TchTx, AfterCommit, transaction helpers
  ├── stereotype/          ← @UseCase, @Adapter, shared annotations
  └── config/              ← Spring @Configuration, shared beans
```

**Rules**:

- ✅ NO business domain, NO domain events
- ✅ NO access control rules, NO user profile, NO tenant config, NO communication delivery
- ✅ Technical primitives only — if it has a table, a workflow, or a business policy, it belongs in `platform/`
- ✅ Thread-safe (context, caches, bus)
- ❌ MUST NOT depend on core, catalog, platform, or features

See: `docs/modules/common.md`

---

## 1.6) Catalog Layer — Simple DDD / Reference Catalog

**Catalog** provides stable **reference data and lookup tables** with NO business invariants, NO lifecycle, NO events.

Archetype: **Simple DDD / Reference Catalog** (NOT hexagonal — read/write separation).

```
catalog/<name>/
  api/
    XxxCatalog.java        ← Read-only interface consumed by core/platform/features
    model/                 ← Immutable DTOs (*View, *SummaryView, *Row, *SearchCriteria)
  internal/
    read/                  ← CatalogImpl (implements XxxCatalog, reads + caches)
    write/                 ← AdminService (updates + cache eviction)
    persistence/           ← JPA entities, repositories
    mapper/                ← Entity → View mapping
    web/                   ← Admin CRUD controllers (thin)
    cache/                 ← Cache specs
```

**Rules**:

- ✅ `api/` is the only public surface — immutable DTOs, typed IDs, no JPA entities
- ✅ Consumed by `core/`, `platform/`, and `features/` via the `XxxCatalog` interface
- ✅ Cache aggressively (`@Cacheable` in `read/`, evict in `write/`)
- ✅ Admin CRUD via thin controllers (`/api/v1/platform/**`, SUPER_ADMIN)
- ❌ NO business invariants, NO domain events, NO lifecycle rules

**Example Catalog Modules**:

- `catalog.game` — Game metadata (rules, odds, descriptions)
- `catalog.pricing` — Pricing templates, tax rates
- `catalog.resultslot` — Draw result slot definitions
- `catalog.drawchannel` — Draw channel configurations
- `catalog.theme` — UI theme presets
- `catalog.i18n` — i18n strings (overrides)

See: `docs/modules/catalog.md`, `openspec/context/75-catalog-rules.md`

---

## 1.7) Platform Layer — Transversal Application Services

**Platform** hosts cross-cutting application service capabilities that are stateful or lifecycle-bearing but do NOT own core business-critical invariants.

A wrong silent result belongs in `core` if it can cause direct financial loss, regulatory dispute, wrong winner, wrong payout, wrong draw/result, wrong settlement, or wrong limit decision. Everything else that is shared and stateful belongs in `platform`.

### Current platform capabilities

```text
platform.audit          ← Audit trail (was core.audit)
platform.accesscontrol  ← Permission checks, role assignment (was core.accesscontrol)
platform.identity       ← User/tenant identity context (was core.tenantuser → core.usercontext)
platform.tenantconfig   ← Tenant configuration (was core.tenantconfig)
platform.tenanttheme    ← Theme management (was core.tenanttheme)
platform.tenantgame     ← Tenant game settings
platform.communication  ← Email/SMS/push delivery
platform.notification   ← In-app notifications
platform.document       ← Document generation/storage
platform.idempotence    ← Persistent idempotency records
platform.address        ← Address validation/formatting
```

### Module shape

```text
platform/<capability>/
  api/
    XxxApi.java          ← Only public surface exposed to other modules
    model/               ← Immutable records; no JPA entities, no Spring MVC types
  internal/
    service/
    persistence/
    web/
    event/
    adapter/
    cache/
    config/
```

### Rules

- ✅ Expose only `api/` to other modules
- ✅ Hide all implementation under `internal/`
- ✅ Platform services join caller transaction by default
- ✅ `platform.audit` may use `REQUIRES_NEW` for failure audit
- ✅ Platform may listen to core events
- ❌ MUST NOT depend on `core/` or `features/`
- ❌ `platform.<a>.internal` MUST NOT import `platform.<b>.internal`
- ❌ Core MUST NOT listen to platform events

### How core consumes platform

```java
// core/<domain>/application/command/handler/SomeCommandHandler.java
@UseCase
@RequiredArgsConstructor
public class SomeCommandHandler implements CommandHandler<SomeCommand, SomeResult> {

  private final AuditApi auditApi;        // ← Inject via platform.audit.api
  private final AccessControlApi acl;     // ← Inject via platform.accesscontrol.api

  @Override
  @TchTx
  public SomeResult handle(SomeCommand cmd) {
    acl.assertPermission(cmd.actorId(), Permission.SOME_ACTION);
    // ... business logic ...
    auditApi.record(AuditEntry.of("SOME_ACTION", cmd.entityId()));
    return result;
  }
}
```

See: `docs/modules/platform.md`, `openspec/context/78-platform-rules.md`

---

## 2) Core Layer — Clean Architecture / Hexagonal / CQRS

Archetype of `core/` only — other layers do NOT follow this pattern.

Each core domain has two top-level packages: `api/` (public surface) and `internal/` (everything else). See `docs/modules/core.md`, `docs/conventions/clean_architecture.md`.

```
core/<domain>/
  api/                        ← Public Java surface consumed by other modules
    command/                  ← XxxCommand records
    query/                    ← XxxQuery records + XxxResult / XxxRow
    event/                    ← XxxEvent records (published after commit)
    model/                    ← Shared read models
  internal/                   ← Hidden from other modules
    domain/
      model/                  ← Aggregates, value objects (pure Java, no Spring)
      service/                ← Domain policies, calculators (no injection, no I/O)
      event/                  ← Internal domain events
      exception/              ← Domain exceptions
    application/
      command/handler/        ← @UseCase @TchTx — load, mutate, persist, AfterCommit
      query/handler/          ← @UseCase — read-only
      port/out/               ← Output port interfaces (persistence, external)
      service/                ← Application orchestrators (optional)
    infra/
      persistence/            ← JPA entities, repositories, JpaAdapters
      web/                    ← Thin controllers (CommandBus/QueryBus dispatch)
      event/                  ← Idempotent listeners, AfterCommit publishers
      batch/                  ← Spring Batch jobs (optional)
      scheduler/              ← Scheduled tasks (optional)
      cache/                  ← Cache adapters (optional)
      config/                 ← Spring @Configuration for the domain
```

**Module boundary rule**: other modules may only import `core.<domain>.api.*`. Importing `core.<domain>.internal.*` is forbidden.

### Internal rules (ENFORCED by ArchUnit)

#### api/ — public surface only

- ✅ Commands, queries, events, read models (immutable records)
- ❌ NO domain aggregates, JPA entities, repositories, handlers, ports, controllers
- ❌ NO `internal.*` imports

#### internal/domain/ — pure Java

- ✅ Aggregates, value objects, domain services, domain events, exceptions
- ✅ Pure Java — no Spring, no JPA
- ❌ MUST NOT depend on `application/` or `infra/`
- ❌ Domain services: no injection, no buses, no I/O (handler loads data, passes it in)

#### internal/application/ — orchestration, no infra

- ✅ CommandHandlers (`@UseCase @TchTx`), QueryHandlers (`@UseCase`)
- ✅ Output port interfaces (`port/out/`) — signed with domain/api types only
- ✅ Application services (optional orchestrators)
- ❌ MUST NOT depend on `infra.*`, JPA entities, repositories, HTTP clients
- ❌ MUST NOT import `application.*` from other modules

#### internal/infra/ — adapters only

- ✅ Implements output ports, handles Spring/JPA/HTTP
- ✅ Thin controllers dispatch to CommandBus/QueryBus
- ❌ Domain/application MUST NOT depend on infra

---

## 2.5) Features Layer — Vertical Slices (NOT Hexagonal)

Features follow a **completely different** architectural style: **Vertical Slice Architecture**.

```
features/<feature_key>/
  ├── <slice_key>/
  │   ├── web/              ← HTTP controllers (BFF boundary)
  │   ├── app/              ← Orchestration services
  │   ├── model/            ← UI contracts (XxxRequest, XxxResponse, XxxView)
  │   ├── mapper/           ← Mapping logic
  │   ├── dynamic/          ← Providers / plug-ins (optional)
  │   └── shared/           ← Internal helpers (optional)
  └── <other_slice_key>/    ← If feature has multiple UI areas
```

### Feature Responsibilities

- ✅ Orchestrate multiple core commands
- ✅ Sequence cross-domain queries
- ✅ Aggregate results into UI models
- ✅ Expose endpoints oriented around screens or flows
- ✅ Build page-level payloads (dashboards, wizards, summaries)
- ✅ Use typed IDs and respect request context

- ❌ MUST NOT define business invariants
- ❌ MUST NOT own lifecycle rules
- ❌ MUST NOT access repositories or JPA entities
- ❌ MUST NOT mutate domain state directly
- ❌ MUST NOT compute money, limits, or payouts

### Feature Dependency Rules

```
common ↑ catalog
  ↑       ↑
 core ↑──┘→ features
```

- ✅ features MAY depend on `core`, `catalog`, `common`
- ✅ features MAY call CommandBus/QueryBus
- ❌ core MAY NOT depend on features
- ❌ features MAY NOT depend on repositories or JPA entities

See: `openspec/context/81-features-rules.md` for feature full rules.

---

All HTTP endpoints are prefixed with:

```
${app.base-path}/${app.api-version}  (default: /api/v1)
```

### Scope-based path prefixes

- **PUBLIC**: `/api/v1/public/**` — no auth required
- **TENANT**: `/api/v1/tenant/**` — tenant-scoped context (from JWT)
- **ADMIN**: `/api/v1/admin/**` — tenant admin scope
- **PLATFORM**: `/api/v1/platform/**` — platform-level admin (SUPER_ADMIN role)
- **SDR**: `/api/v1/_sdr/**` — Spring Data REST (platform reference data CRUD)

No "naked" routes (`/tenant`, `/draw`, etc.) by convention.

---

---

## 4) Controllers — Request Mapping & Entry Point Rules

Controllers are the HTTP boundary layer — they MUST be thin and delegate to CommandBus/QueryBus.

### 4.1 Controller placement and ownership

- **Core domain controllers**: `core/<domain>/infra/web/`
- **Feature controllers**: `features/<slice_key>/web/`
- **Catalog admin controllers**: `catalog/<name>/infra/web/` or Spring Data REST (SDR)

### 4.2 Canonical controller structures

**Tenant-scoped controller**:

```java
@RestController
@RequestMapping("/api/v1/tenant/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets • Tenant", description = "Ticket selling and management")
public class TicketController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  @PostMapping
  @Operation(summary = "Sell a ticket", description = "Sell a ticket to a customer")
  @PreAuthorize("hasPermission('ticket.sell')")           // ← Permission
  @AuditLog(entity = "ticket", action = "SELL", idExpression = "#result.id()")
  public ApiResponse<TicketResultDto> sellTicket(
      @CurrentContext TchRequestContext ctx,
      @Valid @RequestBody SellTicketRequest request       // ← Validation
  ) {
    var cmd = new SellTicketCommand(
        ctx.effectiveTenantIdRequired(),
        TerminalId.of(request.terminalId()),
        request.amount()
    );
    return ApiResponse.success(commandBus.execute(cmd));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get ticket details")
  @PreAuthorize("hasPermission('ticket.read')")
  public ApiResponse<TicketDetailDto> getTicket(
      @CurrentContext TchRequestContext ctx,
      @PathVariable String id
  ) {
    var query = new GetTicketDetailQuery(
        ctx.effectiveTenantIdRequired(),
        TicketId.of(id)
    );
    return ApiResponse.success(queryBus.ask(query));
  }
}
```

**Admin controller with @PreAuthorize hierarchy**:

```java
@RestController
@RequestMapping("/api/v1/admin/payouts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'SUPER_ADMIN')")  // ← Class: roles
@Tag(name = "Payouts • Admin")
@Validated
public class PayoutAdminController {

  private final CommandBus commandBus;

  @PostMapping("/{payoutId}/approve")
  @Operation(summary = "Approve a payout", description = "Admin approves a pending payout")
  @PreAuthorize("hasPermission(null, 'PAYOUT_APPROVE')")   // ← Method: permission
  @AuditLog(entity = "payout", action = "APPROVE", idExpression = "#payoutId")
  public ApiResponse<PayoutWorkflowResponse> approve(
      @CurrentContext TchRequestContext ctx,
      @PathVariable PayoutId payoutId
  ) {
    var cmd = new ApprovePayoutCommand(
        ctx.effectiveTenantIdRequired(),
        payoutId,
        ctx.userId()
    );
    return ApiResponse.success(commandBus.execute(cmd));
  }

  @PostMapping("/{payoutId}/reject")
  @Operation(summary = "Reject a payout")
  @PreAuthorize("hasPermission(null, 'PAYOUT_REJECT')")    // ← Method: permission
  @AuditLog(entity = "payout", action = "REJECT", idExpression = "#payoutId")
  public ApiResponse<PayoutWorkflowResponse> reject(
      @CurrentContext TchRequestContext ctx,
      @PathVariable PayoutId payoutId,
      @Valid @RequestBody RejectPayoutRequest body        // ← Validation
  ) {
    var cmd = new RejectPayoutCommand(
        ctx.effectiveTenantIdRequired(),
        payoutId,
        ctx.userId(),
        body.reason()
    );
    return ApiResponse.success(commandBus.execute(cmd));
  }
}
```

### 4.2.1 @PreAuthorize Hierarchy for Admin Controllers

For **admin endpoints**, use two-level authorization:

**Class level** = role/authority gates (WHO):

```java
@RestController
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'SUPER_ADMIN')")
public class PayoutAdminController { }
```

**Method level** = permission gates (WHAT):

```java
@PostMapping("/{id}/approve")
@PreAuthorize("hasPermission(null, 'PAYOUT_APPROVE')")
public ApiResponse<...> approve(...) { }
```

**Rationale**:

- ✅ Class: broad role checks (access area)
- ✅ Method: fine-grained permission checks (action allowed)
- ✅ Combines Spring Security roles + domain permissions
- ❌ NO ad-hoc role checks in code

### 4.2.2 Swagger / OpenAPI Documentation

**On classes**:

```java
@Tag(
    name = "Tickets • Tenant",
    description = "Ticket selling and management"
)
public class TicketController { }
```

**On methods**:

```java
@PostMapping
@Operation(
    summary = "Sell a ticket",
    description = "Sell a ticket to a customer",
    tags = {"Tickets • Tenant"}
)
public ApiResponse<TicketResultDto> sellTicket(...) { }
```

Auto-generated at:

- `GET /api-docs` — JSON spec
- `GET /swagger-ui.html` — Interactive Swagger UI

### 4.2.3 Jakarta Bean Validation — Detailed

**Request models with @Schema (Swagger)**:

```java
public record SellTicketRequest(
    @NotBlank(message = "Terminal ID required")
    @Schema(
        description = "Terminal identifier",
        example = "550e8400-e29b-41d4-a716-446655440000"
    )
    String terminalId,

    @NotNull(message = "Amount required")
    @DecimalMin(value = "0.01", inclusive = true)
    @Digits(integer = 10, fraction = 2, message = "Max 10 digits, 2 decimal places")
    @Schema(description = "Sale amount in cents", example = "10000")
    BigDecimal amount,

    @Email(message = "Invalid email format")
    @Schema(description = "Customer email", example = "customer@example.com")
    String notificationEmail
) {}

public record RejectPayoutRequest(
    @NotBlank(message = "Reason required")
    @Size(
        min = 10,
        max = 500,
        message = "Reason must be between 10 and 500 characters"
    )
    @Schema(description = "Rejection reason", example = "Insufficient documentation")
    String reason
) {}
```

**Commands/Queries also validated**:

```java
public record SellTicketCommand(
    TenantId tenantId,
    TerminalId terminalId,
    @NotNull(message = "Amount required")
    @DecimalMin(value = "0.01", inclusive = true)
    BigDecimal amount
) {}
```

**In controller**:

```java
public ApiResponse<TicketResultDto> sellTicket(
    @CurrentContext TchRequestContext ctx,
    @Valid @RequestBody SellTicketRequest request    // ← @Valid triggers validation
) { }
```

**Jakarta constraints**:

- `@NotNull` — non-null
- `@NotBlank` — non-null and non-empty string
- `@NotEmpty` — non-null and non-empty collection
- `@Min(n)` / `@Max(n)` — numeric min/max
- `@DecimalMin(n)` / `@DecimalMax(n)` — decimal min/max
- `@Size(min, max)` — string/collection length
- `@Email` — email format
- `@Pattern(regex)` — regex match
- `@Digits(int, frac)` — decimal format (e.g., monetary)
- `@Valid` — recursive validation

**Rules**:

- ✅ Apply constraints to BOTH request models AND commands
- ✅ Add `@Schema` for Swagger documentation
- ✅ Use `@Valid` on `@RequestBody` parameters
- ✅ Custom `require*` for business logic ONLY
- ❌ NO manual validation for structural constraints

---

## 5) Typed IDs (Wrappers)

Every HTTP request is wrapped with **`TchRequestContext`**:

```java
public interface TchRequestContext {
  TenantId tenantId();                      // from JWT, guaranteed non-null
  TenantId effectiveTenantId();             // same as tenantId() (tenant-scoped)
  UserId userId();                          // from JWT
  Set<Role> roles();                        // TENANT_ADMIN, TENANT_USER, SUPER_ADMIN
  Locale locale();                          // from Accept-Language or config
  ZoneId timezone();                        // from user profile or tenant config
}
```

### Context Injection

```java
@RestController
public class MyController {
  @GetMapping
  public ApiResponse<MyResult> myEndpoint(
      @CurrentContext TchRequestContext ctx,
      @RequestBody MyRequest req
  ) {
    var cmd = new MyCommand(ctx.effectiveTenantIdRequired(), ...);
    return ApiResponse.success(commandBus.execute(cmd));
  }
}
```

### Rules

- ✅ Inject via `@CurrentContext` annotation
- ✅ RLS (Row-Level Security) filters data at DB level
- ✅ Never pass `tenantId` from client—use context
- ✅ For batch/scheduled jobs: manually call `TchContext.set(ctx)`
- ❌ MUST NOT read JWT directly in handlers
- ❌ MUST NOT resolve tenant elsewhere

See: `docs/conventions/api/request_context_usage.md`, `docs/conventions/security_permissions.md`

---

## 6) CQRS — Command/Query Bus Pattern

The canonical entry point for all business operations is **CommandBus** (writes) or **QueryBus** (reads).

### CommandBus

```java
// Define: an immutable record
public record SellTicketCommand(
    TenantId tenantId,
    TerminalId terminalId,
    BigDecimal amount
) {}

// Implement: @UseCase + CommandHandler
@UseCase
@RequiredArgsConstructor
public class SellTicketHandler implements CommandHandler<SellTicketCommand, TicketResult> {

  private final TicketWriterPort writer;
  private final DomainEventPublisher events;

  @Override
  @TchTx  // marks write transaction
  public TicketResult handle(SellTicketCommand cmd) {
    // 1. Load, validate, mutate
    var ticket = writer.sellAndSave(cmd.tenantId(), cmd.terminalId(), cmd.amount());

    // 2. Publish side-effects AFTER commit
    AfterCommit.run(() -> events.publish(
        new TicketSoldEvent(cmd.tenantId(), ticket.id())
    ));

    return new TicketResult(ticket.id(), ticket.status());
  }
}

// Execute: from controller or feature orchestrator
var result = commandBus.execute(new SellTicketCommand(...));
```

**Rules**:

- ✅ Command = immutable record
- ✅ Handler = `@UseCase` + implements `CommandHandler<C, R>`
- ✅ Mutations use `@TchTx` annotation
- ✅ Events published via `AfterCommit.run(...)`
- ❌ NO state mutations after response
- ❌ NO partial transactions

See: `docs/conventions/command_query_handlers.md`

### QueryBus

```java
// Define: an immutable record
public record GetTicketSummaryQuery(TenantId tenantId, TicketId ticketId) {}

// Implement: @UseCase + QueryHandler (read-only)
@UseCase
@RequiredArgsConstructor
public class GetTicketSummaryHandler implements QueryHandler<GetTicketSummaryQuery, TicketSummaryView> {

  private final TicketReaderPort reader;

  @Override
  public TicketSummaryView handle(GetTicketSummaryQuery q) {
    // pure read, no transaction needed
    return reader.findSummaryBy(q.tenantId(), q.ticketId());
  }
}

// Execute: from controller
var view = queryBus.ask(new GetTicketSummaryQuery(...));
```

**Rules**:

- ✅ Query = immutable record
- ✅ Handler = `@UseCase` + implements `QueryHandler<Q, R>`
- ✅ Queries are read-only (no mutations)
- ✅ No transaction annotation needed
- ✅ Queries MAY use projections for UI models
- ❌ MUST NOT call CommandBus
- ❌ MUST NOT publish events

---

## 6.5) Cross-Domain Calls (Inter-Domain Integration)

### Read-Side: Queries across domains

**Pattern**: Domain A asks Domain B for data via stable query APIs.

```java
// ✅ CORRECT — Feature orchestrates via queries
@RequiredArgsConstructor
public class CheckoutService {
  private final QueryBus queryBus;

  public CheckoutResultDto checkout(CheckoutRequest req) {
    var ctx = TchContext.current();

    // 1. Get ticket from sales domain
    var ticket = queryBus.ask(
        new GetTicketQuery(ctx.tenantId(), req.ticketId())
    );

    // 2. Get payout options from payout domain
    var payouts = queryBus.ask(
        new ListPayoutOptionsQuery(ctx.tenantId(), ticket.category())
    );

    // 3. Aggregate into UI model
    return new CheckoutResultDto(ticket, payouts);
  }
}
```

**Rules for cross-domain reads**:

- ✅ Use stable `GetXxxQuery(tenantId, id)` from owning domain
- ✅ Never read from another domain's repositories
- ✅ Consume read-models / projections
- ❌ MUST NOT access infra/persistence from another domain
- ❌ MUST NOT depend on another domain's JPA entities

### Write-Side: Events after commit

**Pattern**: Domain A publishes event, Domain B subscribes and executes its own command.

```java
// Step 1: Domain A (Sales) publishes event after commit
@UseCase
public class SellTicketHandler implements CommandHandler<SellTicketCommand, TicketResult> {
  private final TicketWriterPort writer;
  private final DomainEventPublisher events;

  @Override
  @TchTx
  public TicketResult handle(SellTicketCommand cmd) {
    var ticket = writer.sell(...);

    // Publish AFTER commit (not during)
    AfterCommit.run(() -> events.publish(
        new TicketSoldEvent(cmd.tenantId(), ticket.id(), ticket.drawId())
    ));

    return new TicketResult(ticket.id());
  }
}

// Step 2: Domain B (Draw) listens in a separate transaction
@Component
@RequiredArgsConstructor
public class TicketSoldEventListener {
  private final CommandBus commandBus;

  @EventListener(TicketSoldEvent.class)
  @Transactional  // separate transaction
  public void onTicketSold(TicketSoldEvent event) {
    // Execute own command with tenant context
    commandBus.execute(new ApplyTicketToDrawCommand(
        event.tenantId(),
        event.drawId(),
        event.ticketId()
    ));
  }
}
```

**Rules for cross-domain effects**:

- ✅ Source domain publishes event in `AfterCommit`
- ✅ Target domain subscribes with `@EventListener`
- ✅ Listener executes local command (own transaction)
- ✅ Listener sets tenant context from event
- ❌ NO direct calls between domain handlers
- ❌ NO nested transactions
- ❌ NO bypassing CommandBus

See: `docs/conventions/inter_domain_calls.md`

---

## 7) Error Handling

- **2xx responses**: `ApiResponse<T>` or legacy DTO (auto-wrapped by `ApiResponseBodyAdvice`)
- **4xx/5xx responses**: `ProblemDetail` (RFC7807) in `application/problem+json`
- Throw `ProblemRest.*()` exceptions from handlers
- Do not wrap errors in `ApiResponse`

See: `docs/conventions/web_api.md`, `docs/conventions/api_response.md`

---

## 8) Pagination

All list endpoints use standard pagination:

- Request: `TchPageRequest` (via `@TchPaging` annotation)
- Response: `TchPage<T>` (wrapper with totalElements, pages, etc.)

See: `docs/conventions/pagination.md`

---

## 9) Event Publishing & After-Commit

**Principle**: Domain publishes events, infrastructure reacts, **publication happens after commit**.

```java
AfterCommit.run(() -> publisher.publish(event));
```

**Typical use cases**:

- Sale → ledger + stats
- DrawResultApplied → ticket settlement
- DrawSettled → payout finalization

See: `docs/conventions/handler_command.md`, `docs/conventions/idempotency.md`

---

## 10) Persistence & Migrations

- **ddl-auto**: `validate` (never auto-create)
- **Migrations**: Flyway (classpath:db/migration)
- **Entities**: JPA with `@Entity`, soft-delete support
- **Tenant-scoped tables**: `tenant_id` column + RLS policies
- **Audit**: Envers for critical tables (configurable)

See: `docs/conventions/persistence.md`, `docs/conventions/jpa_entities.md`, `docs/conventions/rls.md`

---

## 11) Batch & Scheduled Jobs

- Jobs live in `<domain>/infra/batch/`
- Auto-run disabled (`spring.batch.job.enabled: false`)
- Orchestration via scheduler or ops endpoints
- Context set manually for batch: `TchContext.set(ctx)`
- Cross-domain side-effects via after-commit events

See: `docs/conventions/batch.md`

---

## 12) Cache (Caffeine + Redis)

Two-level caching:

- **L1**: Caffeine (local JVM)
- **L2**: Redis (optional, feature-flagged)

- TTL and specs via `CacheSpecProvider` (no hardcoding)
- Feature flag: `tch.cache.redis.enabled`
- No cache on critical money operations

See: `docs/conventions/cache.md`

---

## 13) Spring Data REST (SDR)

**Acceptable use**:

- Simple admin CRUD on `catalog` reference data
- Endpoints under `/api/v1/_sdr/**`
- Security: SUPER_ADMIN role

**Forbidden**:

- Exposing core aggregates via SDR
- Mixing domain endpoints with SDR

See: `docs/conventions/routing_and_path.md`

---

## 14) Convention over Configuration

- Use naming conventions (see `docs/conventions/naming.md`)
- Follow `Rule of 3` for sub-package creation (don't over-modularize)
- If a local pattern exists, use it (don't invent new architecture)
- Document deviations in CLAUDE.md or issue comments

---

## 15) Additional Resources

- `docs/PLAYBOOK.md` — How to add features, DoD, templates
- `docs/AGENTS.md` — Multi-agent orchestration rules
- `docs/conventions/*` — Technical details per concern
- `openspec/context/80-core-rules.md` — Detailed core architecture rules
- `openspec/context/81-features-rules.md` — Detailed feature rules
- `VERSIONS.md` — Runtime, framework, library versions
- Each core domain has its own `CLAUDE.md` for scope & rules

---

**Last reviewed**: 2026-05-09  
**Status**: NORMATIVE (non-negotiable rules for all contributors)
