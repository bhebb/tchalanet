# Backend Rules & Architecture Map

**This file is the entry point for agents implementing backend features.**

It answers: _"Where does my code go? What rules apply? Where do I read?"_

---

## Quick Start: Where Does My Code Go?

**Ask yourself**: What is the business value without a UI?

| Scenario                                              | Layer       | Read First                                                           |
| ----------------------------------------------------- | ----------- | -------------------------------------------------------------------- |
| New business domain (rules, invariants, lifecycle)    | `core/`     | `docs/ARCHITECTURE.md` → `DOMAIN_*.md`                               |
| Orchestration, workflow, multi-domain coordination    | `features/` | `docs/ARCHITECTURE.md` 2.5 + `openspec/context/81-features-rules.md` |
| Reference data, lookup, configuration (no invariants) | `catalog/`  | `openspec/context/75-catalog-rules.md`                               |
| Authentication, caching, bus, common utilities        | `common/`   | `docs/ARCHITECTURE.md` 1.5                                           |

---

## Architecture Overview (4 Layers)

```
common/       ← Technical transversal (Bus, Events, Context, Cache, Security)
catalog/      ← Reference data (read/write separation, NO events)
core/         ← Business domains (hexagonal: domain→application→infra)
features/     ← Orchestration layer (vertical slices, thin controllers)
```

### 1. `common/` — Framework & Glue

**Contains**: CommandBus, QueryBus, TchRequestContext, error handling, auditing, utilities.

**Rules**:

- ✅ Technical only (no domain logic)
- ❌ NO business invariants
- ✅ Used by all layers

**When to add**: Shared infrastructure, framework patterns.

**Path**: `src/main/java/com/tchalanet/server/common/`

**Reference**: `docs/ARCHITECTURE.md` § 1.5

---

### 2. `catalog/` — Reference Data (Read/Write Separation)

**Contains**: Game metadata, pricing, result slots, themes, i18n — stable reference data with NO lifecycle or events.

**Structure**:

```
catalog/<name>/
  ├── api/              ← Read-only public contract (XCatalog interface)
  ├── api/model/        ← DTOs (*View, *SummaryView, *Row)
  ├── internal/read/    ← CatalogImpl (reads + caches)
  ├── internal/write/   ← AdminService (updates, cache eviction)
  ├── internal/web/     ← Admin thin controllers
  ├── internal/persistence/  ← JPA entities, repositories
  └── internal/mapper/  ← Mapping (internal only)
```

**Rules**:

- ✅ Strict read/write separation
- ✅ Aggressive caching
- ✅ No events, no invariants
- ✅ Admin CRUD via `/api/v1/platform/**` (SUPER_ADMIN)
- ❌ Core modules read ONLY from `api/` package

**When to add**: Static/reference data that changes rarely via admin only.

**Path**: `src/main/java/com/tchalanet/server/catalog/`

**Reference**: `docs/ARCHITECTURE.md` § 1.6 + `docs/PLAYBOOK.md` § 5 + `openspec/context/75-catalog-rules.md`

---

### 3. `core/` — Business Domains (Hexagonal)

**Contains**: Aggregates, invariants, state machines, commands, queries, domain events.

**Structure**:

```
core/<domain>/
  ├── domain/          ← Pure business (aggregates, value objects, services)
  ├── application/     ← CQRS (commands, queries, ports)
  └── infra/           ← Spring, JPA, HTTP (web, persistence, batch, events)
```

**Rules**:

- ✅ Strict layering (domain never depends on infra)
- ✅ Commands with `@TchTx`, queries read-only
- ✅ Events after commit via `AfterCommit.run()`
- ✅ Typed IDs everywhere (no raw UUID outside infra)
- ✅ Controllers thin (mapping, validation, dispatch only)
- ✅ No cross-domain writes (use events + listeners)
- ❌ NO business logic in controllers
- ❌ NO direct repository calls from domain/application

**When to add**: New critical business domain (sales, draw, payout, session, etc.).

**Path**: `src/main/java/com/tchalanet/server/core/<domain>/`

**Reference**: `docs/ARCHITECTURE.md` § 2 + `docs/PLAYBOOK.md` § 6-8 + `openspec/context/80-core-rules.md`

---

### 4. `features/` — Orchestration & BFF (Vertical Slices)

**Contains**: REST controllers, orchestration services, UI models — NOT business logic.

**Structure**:

```
features/<feature_key>/
  └── <slice_key>/
      ├── web/        ← Thin HTTP controllers
      ├── app/        ← Orchestration services
      ├── model/      ← UI contracts (*Request, *Response, *View)
      └── mapper/     ← Mapping
```

**Rules**:

- ✅ Orchestrates multi-step workflows across domains
- ✅ Delegates all business logic to core commands
- ✅ Uses catalog queries (read-only)
- ✅ Aggregates results into UI models
- ❌ NO business invariants
- ❌ NO direct repository access
- ❌ NO JPA entities

**When to add**: Multi-domain workflow, BFF endpoint, dashboard, wizard.

**Path**: `src/main/java/com/tchalanet/server/features/<feature_key>/`

**Reference**: `docs/ARCHITECTURE.md` § 2.5 + `openspec/context/81-features-rules.md`

---

## Controllers — Essential Rules

**EVERY controller MUST have** (non-negotiable):

```java
@RestController
@RequestMapping("/api/v1/tenant/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets • Tenant")                        // ← Swagger doc
@Validated                                              // ← Jakarta validation
public class TicketController {

  @PostMapping
  @Operation(summary = "...", description = "...")     // ← Swagger
  @PreAuthorize("hasPermission('ticket.sell')")        // ← Security (method)
  @AuditLog(entity = "ticket", action = "SELL", idExpression = "#result.id()")  // ← Audit
  public ApiResponse<TicketDto> sellTicket(
      @CurrentContext TchRequestContext ctx,           // ← Tenant from JWT
      @Valid @RequestBody SellTicketRequest req        // ← Validation (Jakarta)
  ) {
    var cmd = new SellTicketCommand(...);
    return ApiResponse.success(commandBus.execute(cmd));
  }
}
```

**Admin controllers add class-level @PreAuthorize**:

```java
@RestController
@RequestMapping("/api/v1/admin/payouts")
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'SUPER_ADMIN')")  // ← Class level (roles)
@Tag(name = "Payouts • Admin")
public class PayoutAdminController {

  @PostMapping("/{id}/approve")
  @PreAuthorize("hasPermission(null, 'PAYOUT_APPROVE')")        // ← Method level (permissions)
  @AuditLog(entity = "payout", action = "APPROVE", idExpression = "#id")
  public ApiResponse<PayoutDto> approve(
      @CurrentContext TchRequestContext ctx,
      @PathVariable PayoutId id,
      @Valid @RequestBody ApproveRequest req
  ) { ... }
}
```

Reference: `docs/PLAYBOOK.md` § 6 + `docs/ARCHITECTURE.md` § 4

---

## Multi-Tenant & Security

**Tenant resolution**:

- ✅ `TchRequestContext` (injected into each request via filter)
- ✅ Contains: `tenantId()`, `userId()`, `roles()`, `locale()`, `timezone()`
- ✅ Set from JWT claims (NEVER from request body)
- ✅ RLS (PostgreSQL Row-Level Security) is last line of defense

**Authorization**:

- ✅ Declare via `@PreAuthorize("hasPermission(...)")`
- ✅ Evaluated by `TchPermissionEvaluator` calling domain query
- ✅ NO manual role checks in code
- ✅ Deny = HTTP 403 (not exception)

Reference: `docs/PLAYBOOK.md` § 6.2 + `docs/conventions/security_permissions.md`

---

## Data Persistence

**Rules**:

- ✅ UUID allowed ONLY in JPA entities, repositories, Flyway migrations
- ✅ Typed ID wrappers everywhere else (`TenantId`, `TicketId`, ...)
- ✅ Flyway migrations only (NO `ddl-auto=update`)
- ✅ Tenant scoping via RLS (not Java filter logic)
- ✅ Soft-delete preferred (`deleted_at` column)
- ✅ Audit via Envers + `@AuditLog` annotation

Reference: `docs/ARCHITECTURE.md` § 5 + `docs/conventions/persistence.md`

---

## CQRS Pattern (Commands & Queries)

**Commands** (mutations):

```java
public record SellTicketCommand(TenantId tenantId, TerminalId terminalId, BigDecimal amount) {}

@UseCase
public class SellTicketHandler implements CommandHandler<SellTicketCommand, TicketResult> {
  @Override
  @TchTx  // ← write transaction
  public TicketResult handle(SellTicketCommand c) {
    var ticket = writer.sell(...);
    AfterCommit.run(() -> events.publish(new TicketSoldEvent(...)));
    return new TicketResult(ticket.id());
  }
}

// Execute from controller
commandBus.execute(new SellTicketCommand(...));
```

**Queries** (read-only):

```java
public record GetTicketSummaryQuery(TenantId tenantId, TicketId ticketId) {}

@UseCase
public class GetTicketSummaryHandler implements QueryHandler<GetTicketSummaryQuery, TicketView> {
  @Override  // ← NO @TchTx
  public TicketView handle(GetTicketSummaryQuery q) {
    return reader.findSummary(q.tenantId(), q.ticketId());
  }
}

// Execute from controller
queryBus.ask(new GetTicketSummaryQuery(...));
```

Reference: `docs/PLAYBOOK.md` § 7 + `docs/conventions/command_query_handlers.md`

---

## Cross-Domain Integration

**Read from another domain**:

```java
// ✅ CORRECT — use QueryBus
var ticket = queryBus.ask(new GetTicketQuery(tenantId, ticketId));
```

**Write to another domain**:

```java
// ✅ CORRECT — source publishes event after commit
AfterCommit.run(() -> events.publish(new TicketSoldEvent(...)));

// ✅ CORRECT — limitScopeRef listens and executes own command
@EventListener(TicketSoldEvent.class)
@Transactional  // separate transaction
public void onTicketSold(TicketSoldEvent event) {
  commandBus.execute(new ApplyTicketToDrawCommand(...));
}
```

❌ NEVER call handler-to-handler, bypass CommandBus, or mix transactions.

Reference: `docs/PLAYBOOK.md` § 8 + `docs/conventions/inter_domain_calls.md`

---

## API Conventions

**All endpoints return**:

- ✅ **2xx**: `ApiResponse<T>` (success)
- ✅ **4xx/5xx**: `ProblemDetail` (RFC7807, `application/problem+json`)

**Paths**:

- `/api/v1/public/**` — no auth
- `/api/v1/tenant/**` — tenant-scoped (from JWT)
- `/api/v1/admin/**` — tenant admin
- `/api/v1/platform/**` — platform admin (SUPER_ADMIN)

**Collections**:

- Use `TchPage<T>` (NOT Spring `Page`)
- Pagination via `@TchPaging` annotation

Reference: `docs/PLAYBOOK.md` § 4 + `docs/conventions/web_api.md`

---

## Source of Truth (Priority Order)

| Question                 | Answer                             | File                                                                               |
| ------------------------ | ---------------------------------- | ---------------------------------------------------------------------------------- |
| "Where does my code go?" | This file                          | `20-backend-rules.md`                                                              |
| "What's the structure?"  | Hexagonal + CQRS + vertical slices | `docs/ARCHITECTURE.md`                                                             |
| "How do I implement?"    | Patterns, examples, templates      | `docs/PLAYBOOK.md`                                                                 |
| "What are the rules?"    | Strict technical rules             | `openspec/context/80-core-rules.md`, `75-catalog-rules.md`, `81-features-rules.md` |
| "What's the business?"   | Domain invariants, lifecycle       | `DOMAIN_<X>.md` (near code)                                                        |
| "How do I test?"         | JUnit5, AssertJ, nested classes    | `docs/conventions/testing.md`                                                      |
| "How do I X?"            | Specific how-to                    | `docs/conventions/*.md`                                                            |

---

## Typical Workflow (Agent Checklist)

1. **Understand the requirement** ✅
2. **Decide: Core / Catalog / Feature?** ✅
   - Has business invariants? → Core
   - Reference data only? → Catalog
   - Multi-domain workflow? → Feature
3. **Load the right doc** ✅
   - Core: read `DOMAIN_*.md`, then `docs/ARCHITECTURE.md` § 2
   - Catalog: read `openspec/context/75-catalog-rules.md`
   - Feature: read `openspec/context/81-features-rules.md`
4. **Implement following layer rules** ✅
   - Thin controllers (validation, context, dispatch)
   - BusinessLogic via commands/queries
   - Events after commit
5. **Add all annotations** ✅
   - `@Tag` + `@Operation` (Swagger)
   - `@PreAuthorize` (security)
   - `@AuditLog` (write operations)
   - `@Valid` (validation)
6. **Validate before PR** ✅
   - Build (`mvn verify`)
   - Tests pass
   - No ArchUnit violations
   - No tenant leakage

---

## Key Non-Negotiables

- ❌ Never raw UUID outside persistence
- ❌ Never business logic in controllers
- ❌ Never cross-domain direct calls (use events)
- ❌ Never trust client tenantId (use context)
- ❌ Never write without `@TchTx` in commands
- ❌ Never publish events during transaction (use AfterCommit)
- ✅ Always validate inputs (Jakarta Bean Validation)
- ✅ Always declare security (`@PreAuthorize`)
- ✅ Always audit writes (`@AuditLog`)
- ✅ Always use typed IDs
- ✅ Always dispatch via CommandBus/QueryBus
