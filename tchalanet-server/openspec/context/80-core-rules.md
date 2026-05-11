# OpenSpec — Core Rules (80)

> **Scope**: Backend (`tchalanet-server`)  
> **Applies to**: all modules under `com.tchalanet.server.core.*`  
> **Status**: **NORMATIVE**  
> **Purpose**: structural + technical rules (**NOT functional**)

---

## 1. What a Core module IS (definition)

A **Core module** represents a **business domain**.

It owns:

- business invariants
- lifecycle rules
- transactional consistency
- domain decisions
- side-effects and outcomes

A core module is the **source of truth** for a domain.

👉 Functional meaning lives in `DOMAIN_<X>.md`.

---

## 2. What a Core module is NOT

A core module is NOT:

- a reference data catalog
- a CRUD façade
- a pure data registry
- a passive read-only module

If a module:

- only exposes lookup data
- has no lifecycle
- has no invariants
- has no side-effects

➡️ it belongs in **`catalog/`**, not in `core/`.

---

## 3. Mandatory layering (KEY RULE)

Every core module **MUST follow the same internal layering**.

### 3.1 Mandatory package structure

core/<domain>/
├─ domain/
│ ├─ model/
│ ├─ exception/
│ └─ service/ (optional, domain services only)
├─ application/
│ ├─ command/
│ │ ├─ model/
│ │ └─ handler/
│ ├─ query/
│ │ ├─ model/
│ │ └─ handler/
│ └─ event/
├─ port/
│ └─ out/
└─ infra/
├─ persistence/
├─ web/
├─ batch/
├─ event/
└─ cache/

🚫 **Forbidden**:

- skipping layers
- collapsing command/query
- accessing infra from domain

---

## 4. Domain layer (PURE)

The **domain layer** is the heart of the core module. It contains ONLY business rules.

### 4.1 What belongs in domain/

- Aggregates and entities (state + business rules)
- Value objects (immutable, dumb data)
- Domain services (pure policies, calculators, rules)
- Domain exceptions
- Domain events (value objects representing facts)

### 4.2 Domain layer rules (STRICT ENFORCEMENT)

The domain layer:

- ✅ is **framework-free** (no Spring, no JPA, no XML config)
- ✅ is **persistence-agnostic** (no repositories, no adapters)
- ✅ is **deterministic** (same inputs → same outputs)
- ✅ uses **typed IDs exclusively** (no UUID, just IDs)
- ✅ enforces **all business invariants**
- ✅ throws domain exceptions for violations

The domain layer MUST NOT:

- ❌ access repositories or ports
- ❌ publish events
- ❌ depend on Spring framework
- ❌ depend on JPA/Hibernate
- ❌ depend on `application/` or `infra/` packages
- ❌ make I/O calls (HTTP, DB, files)

### 4.3 Domain services (optional, pure)

Allowed ONLY when:

- logic does not belong to a single aggregate
- logic is pure and deterministic
- input/output are domain types (value objects, typed IDs)

```java
package com.tchalanet.server.core.ticketing.domain.service;

public class TicketPricingCalculator {
  // Pure, no Spring, no injection
  public BigDecimal calculateFinalPrice(
      BigDecimal basePrice,
      Percentage commission,
      Percentage tax
  ) {
    return basePrice
        .multiply(commission.factor())
        .multiply(tax.factor());
  }
}
```

Domain services MUST NOT:

- ❌ orchestrate multi-step flows
- ❌ access I/O (repositories, ports, buses)
- ❌ publish events
- ❌ manage transactions

**When a domain service needs data**: The application handler loads it first (e.g., from a repository port), then passes it to the domain service as immutable snapshots/value objects.

---

## 5. Application layer (CQRS boundary)

The **application layer** orchestrates use-cases via **CommandBus** (writes) and **QueryBus** (reads).

### 5.1 Commands

Commands represent **intent to change state**.

```java
// ✅ Define as immutable record
public record SellTicketCommand(
    TenantId tenantId,
    TerminalId terminalId,
    BigDecimal amount
) {}

// ✅ Handler: @UseCase + CommandHandler<C, R>
@UseCase
@RequiredArgsConstructor
public class SellTicketHandler implements CommandHandler<SellTicketCommand, TicketResult> {

  private final TicketWriterPort writer;
  private final DomainEventPublisher events;

  @Override
  @TchTx  // ← Transaction boundary
  public TicketResult handle(SellTicketCommand cmd) {
    // 1. Load, validate, mutate via ports/domain logic
    var ticket = writer.sell(cmd.tenantId(), cmd.terminalId(), cmd.amount());

    // 2. Publish side-effects AFTER commit (not during)
    AfterCommit.run(() -> events.publish(
        new TicketSoldEvent(cmd.tenantId(), ticket.id())
    ));

    return new TicketResult(ticket.id(), ticket.status());
  }
}
```

**Location**:

- application/command/model — command records
- application/command/handler — command handlers

**Rules**:

- ✅ one handler per command
- ✅ handler has single responsibility
- ✅ @TchTx marks transaction boundary
- ✅ events published via AfterCommit.run()
- ✅ return result DTO, never domain entity
- ❌ NO state mutations after return
- ❌ NO events published during transaction

See: `docs/conventions/command_query_handlers.md`

### 5.2 Queries

Queries represent **questions**, read-only.

```java
// ✅ Define as immutable record
public record GetTicketSummaryQuery(TenantId tenantId, TicketId ticketId) {}

// ✅ Handler: @UseCase + QueryHandler<Q, R>
@UseCase
@RequiredArgsConstructor
public class GetTicketSummaryHandler implements QueryHandler<GetTicketSummaryQuery, TicketSummaryView> {

  private final TicketReaderPort reader;

  @Override
  // ← NO @TchTx — read-only
  public TicketSummaryView handle(GetTicketSummaryQuery q) {
    return reader.findSummaryBy(q.tenantId(), q.ticketId());
  }
}
```

**Location**:

- application/query/model — query records
- application/query/handler — query handlers

**Rules**:

- ✅ never mutate state
- ✅ never publish events
- ✅ side-effect free
- ✅ MAY use projections
- ✅ MUST respect RLS implicitly
- ❌ MUST NOT reuse command handlers
- ❌ MUST NOT call CommandBus

See: `docs/conventions/command_query_handlers.md`

### 5.3 Application events

Application events represent **facts that happened** after a command commits.

```java
public record TicketSoldEvent(
    TenantId tenantId,
    TicketId ticketId,
    ZonedDateTime occurredAt
) {}
```

**Rules**:

- ✅ published **after transaction commit** only
- ✅ idempotent listeners
- ✅ failures MUST NOT rollback the command
- ✅ listeners execute their own commands
- ❌ NO business decision in listeners
- ❌ NO bypassing CommandBus

### 5.4 Application services (orchestrators, optional)

Application services MAY exist for complex multi-step orchestration.

```java
@Service
@RequiredArgsConstructor
public class CheckoutOrchestrator {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  public CheckoutResult checkout(CheckoutRequest req) {
    var ctx = TchContext.current();

    // Step 1: Get ticket
    var ticket = queryBus.ask(
        new GetTicketQuery(ctx.tenantId(), req.ticketId())
    );

    // Step 2: Reserve payout
    var payout = commandBus.execute(new ReservePayoutCommand(
        ctx.tenantId(),
        ticket.drawId(),
        req.amount()
    ));

    // Step 3: Complete sale
    commandBus.execute(new CompleteSaleCommand(
        ctx.tenantId(),
        req.ticketId(),
        payout.id()
    ));

    return new CheckoutResult(ticket.id(), payout.id());
  }
}
```

**Rules**:

- ✅ MAY be named with `-Orchestrator`, `-Assembler`, `-Planner`
- ✅ MAY inject ports, buses, other services
- ✅ used by handlers to compose multi-step logic
- ❌ MUST NOT contain domain rules
- ❌ MUST NOT bypass CommandBus

---

## 6. Ports (Hexagonal contracts)

### 6.1 Output ports ONLY

Core modules define **output ports only** — interfaces for adapters (infra).

**Location**:

- application/port/out OR port/out (choose one, stay consistent)

### 6.2 What output ports represent

Ports may represent:

- persistence access (e.g., `TicketWriterPort`, `TicketReaderPort`)
- external service calls (e.g., `PaymentGatewayPort`)
- messaging (e.g., `MessageBrokerPort`)
- time/clock (e.g., `Clock`)
- ID generation (e.g., `IdGenerator`)

### 6.3 Port signatures MUST use domain types

```java
// ✅ CORRECT — port uses domain types
public interface TicketWriterPort {
  Ticket save(Ticket ticket);  // Ticket has typed IDs
}

// ✅ CORRECT — port uses domain DTOs
public interface TicketReaderPort {
  TicketView findBy(TenantId tenantId, TicketId ticketId);
}

// ❌ WRONG — port leaks implementation details
public interface TicketWriterPort {
  TicketJpaEntity save(TicketJpaEntity entity);  // ← JPA entity forbidden
}
```

### 6.4 No input ports

🚫 Core MUST NOT define `application/port/in` by default.

**Why**: CommandBus and QueryBus are the canonical application entry points.

**Exception**: If an exception is needed, document it in an ADR (`docs/03-adr/`).

---

## 7. Infrastructure layer (Adapters)

Infrastructure implements ports and provides Spring integration.

```
infra/
  ├── persistence/         ← JPA entities, repositories, adapters (implements writer/reader ports)
  ├── web/                 ← REST controllers (dispatch via CommandBus/QueryBus)
  ├── event/               ← Event publishers, listeners (after-commit publishing)
  ├── batch/               ← Batch jobs, scheduled tasks (thin orchestration)
  ├── cache/               ← Cache adapters
  ├── adapter/             ← Outbound adapters (HTTP clients, external services)
  └── config/              ← Spring @Configuration, beans
```

### 7.1 Persistence adapters

**Location**: `infra/persistence/`

**Responsibilities**:

- JPA entities (NOT domain entities)
- Spring Data repositories
- Mapping between JPA entities ↔ domain model
- Implementing output ports

**Rules**:

- ✅ JPA/JDBC entities only here
- ✅ UUID allowed ONLY here (not in domain)
- ✅ RLS enforced at DB level
- ✅ Soft-delete (`deleted_at` column) preferred
- ✅ Mapping isolated from domain
- ❌ NO business logic
- ❌ NO validation rules
- ❌ NO domain types

```java
// ✅ JPA entity in persistence infra
@Entity
@Table(name = "tickets")
public class TicketJpaEntity {
  @Id
  private UUID id;  // ← UUID only here

  private UUID tenantId;  // ← Raw UUID, tenant filters, RLS

  // ...
}

// ✅ Adapter implements port
@Component
public class JpaTicketAdapter implements TicketWriterPort {

  @Override
  public Ticket save(Ticket ticket) {
    var entity = toEntity(ticket);
    var saved = repository.save(entity);
    return toDomain(saved);  // ← Convert back to domain model
  }

  private TicketJpaEntity toEntity(Ticket ticket) { /* ... */ }
  private Ticket toDomain(TicketJpaEntity entity) { /* ... */ }
}
```

### 7.2 Web controllers

**Location**: `infra/web/`

**Responsibilities**:

- HTTP boundary only
- Request validation
- Context injection
- Dispatch to CommandBus / QueryBus
- Response mapping

**Rules**:

- ✅ thin delegation
- ✅ `@RestController` in infra/web
- ✅ return `ApiResponse<T>` or let advice wrap DTOs
- ✅ inject `@CurrentContext TchRequestContext`
- ✅ dispatch via buses
- ❌ NO business logic
- ❌ NO repository access
- ❌ NO domain access
- ❌ NO transaction handling

```java
@RestController
@RequestMapping("/api/v1/tenant/tickets")
@RequiredArgsConstructor
public class TicketController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  @PostMapping
  public ApiResponse<TicketResultDto> sellTicket(
      @CurrentContext TchRequestContext ctx,
      @Valid @RequestBody SellTicketRequest req
  ) {
    var cmd = new SellTicketCommand(
        ctx.effectiveTenantIdRequired(),
        TerminalId.of(req.terminalId()),
        req.amount()
    );

    var result = commandBus.execute(cmd);
    return ApiResponse.success(toDto(result));
  }

  @GetMapping("/{ticketId}")
  public ApiResponse<TicketDetailDto> getTicket(
      @CurrentContext TchRequestContext ctx,
      @PathVariable String ticketId
  ) {
    var q = new GetTicketDetailQuery(
        ctx.effectiveTenantIdRequired(),
        TicketId.of(ticketId)
    );

    var result = queryBus.ask(q);
    return ApiResponse.success(toDto(result));
  }
}
```

### 7.3 Batch & Scheduler

**Location**: `infra/batch/`

**Responsibilities**:

- Orchestration only
- No business logic
- Set context manually

**Rules**:

- ✅ use CommandBus for mutations
- ✅ set `TchContext.set(ctx)` manually
- ✅ thin orchestration
- ❌ NO business logic
- ❌ NO direct persistence access
- ❌ NO bypassing CommandBus

```java
@Component
@RequiredArgsConstructor
public class DailyDrawOpenScheduler {

  private final CommandBus commandBus;
  private final TenantProvider tenants;

  @Scheduled(cron = "0 0 5 * * *")  // 5 AM daily
  public void openDailyDraws() {
    for (var tenant : tenants.findAll()) {
      var ctx = new TchRequestContext(tenant.id(), ...);
      TchContext.set(ctx);

      try {
        commandBus.execute(new OpenDailyDrawCommand(tenant.id()));
      } finally {
        TchContext.clear();
      }
    }
  }
}
```

### 7.4 Event listeners

**Location**: `infra/event/`

**Responsibilities**:

- Subscribe to application events
- Execute local commands
- Idempotent handling

**Rules**:

- ✅ idempotent (can be called multiple times)
- ✅ separate transaction per listener
- ✅ failures logged, not rethrown (idempotent)
- ✅ execute own domain's commands
- ❌ NO state mutation outside commands
- ❌ NO cross-domain writes

```java
@Component
@RequiredArgsConstructor
public class TicketSoldEventListener {

  private final CommandBus commandBus;

  @EventListener(TicketSoldEvent.class)
  @Transactional  // ← Separate transaction
  public void onTicketSold(TicketSoldEvent event) {
    try {
      commandBus.execute(new ApplyTicketToDrawCommand(
          event.tenantId(),
          event.drawId(),
          event.ticketId()
      ));
    } catch (Exception e) {
      // Idempotent: log, don't rethrow
      logger.warn("Failed to apply ticket to draw", e);
    }
  }
}
```

---

## 8. RLS & Tenant scoping (CRITICAL)

### 8.1 Read side

- NO tenant filters in Java code
- tenant scoping is 100% SQL
- repositories rely on `app.current_tenant`

🚫 Forbidden:

- `findByTenantId(...)` in read-side queries

---

### 8.2 Write side

- tenantId MAY be passed explicitly
- write repositories MAY filter by tenant
- admin use-cases MAY override context

---

## 9. Soft delete & lifecycle

Rules:

- `deleted_at` = logical removal
- existence checks MUST use `...AndDeletedAtIsNull`
- commands MUST handle resurrect/recreate logic

Seed/bootstrap MUST reason in terms of **live rows**.

---

## 10. Typed IDs (MANDATORY)

Rules:

- all domain/application layers use typed IDs
- UUID only in infra/persistence
- no String/UUID leakage upward

---

## 11. Cross-Domain Calls & Integration (CRITICAL RULES)

Core modules MUST NOT directly depend on each other. All cross-domain integration happens via **defined contracts**.

### 11.1 Cross-domain READS: Use QueryBus

When module A needs data from module B, use stable queries.

```java
// ❌ WRONG — direct repository access
@Service
class BadCheckoutService {
  private final TicketRepository repo;  // ← FORBIDDEN from another domain

  void checkout() {
    repo.findById(...);  // ← VIOLATES architecture
  }
}

// ✅ CORRECT — query via QueryBus
@UseCase
@RequiredArgsConstructor
public class CheckoutHandler implements CommandHandler<CheckoutCommand, CheckoutResult> {

  private final QueryBus queryBus;

  @Override
  @TchTx
  public CheckoutResult handle(CheckoutCommand cmd) {
    // Ask Sales domain for ticket metadata
    var ticket = queryBus.ask(
        new GetTicketQuery(cmd.tenantId(), cmd.ticketId())
    );

    // Use result in our logic
    if (ticket.isSold()) {
      throw new TicketAlreadySoldException();
    }

    return processCheckout(ticket);
  }
}
```

**Rules for reads**:

- ✅ Query must be exposed by owning domain
- ✅ Result is safe snapshot (DTO, view)
- ✅ Caller gets immutable, stable contract
- ❌ NEVER access repositories from another domain
- ❌ NEVER depend on another domain's JPA entities
- ❌ NEVER bypass QueryBus

### 11.2 Cross-domain EFFECTS: Use Events + CommandBus

When module A's action should trigger work in module B, use events.

**Pattern: Event-driven after-commit effects**

```
┌─────────────────────────────────────────────────────────────────┐
│ Step 1: Source domain (Sales) executes command                 │
├─────────────────────────────────────────────────────────────────┤
│ 1a. Validate invariants                                        │
│ 1b. Mutate state (via domain → persistence)                    │
│ 1c. Return result                                              │
│ 1d. [TRANSACTION COMMITS]                                      │
└─────────────────────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────────────────────┐
│ Step 2: After-commit publishing (guaranteed after step 1)       │
├─────────────────────────────────────────────────────────────────┤
│ 2. Publish event (e.g., TicketSoldEvent)                        │
└─────────────────────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────────────────────┐
│ Step 3: Target domain (Draw) listener reacts                    │
├─────────────────────────────────────────────────────────────────┤
│ 3a. @EventListener receives event (separate transaction)        │
│ 3b. Extract tenant/IDs from event                               │
│ 3c. Execute own CommandBus command                              │
│ 3d. [SEPARATE TRANSACTION COMMITS]                              │
└─────────────────────────────────────────────────────────────────┘
```

**Implementation**:

```java
// Step 1: Source domain (Sales) publishes event after commit
@UseCase
@RequiredArgsConstructor
public class SellTicketHandler implements CommandHandler<SellTicketCommand, TicketResult> {

  private final TicketWriterPort writer;
  private final DomainEventPublisher events;

  @Override
  @TchTx
  public TicketResult handle(SellTicketCommand cmd) {
    // 1. Sell ticket (write to repo)
    var ticket = writer.sell(cmd.tenantId(), cmd.drawId(), cmd.amount());

    // 2. Publish event AFTER commit (not during)
    AfterCommit.run(() -> events.publish(
        new TicketSoldEvent(
            cmd.tenantId(),
            ticket.drawId(),
            ticket.id(),
            cmd.amount()
        )
    ));

    return new TicketResult(ticket.id());
  }
}

// Step 2: Target domain (Draw) listens to event
@Component
@RequiredArgsConstructor
public class TicketSoldEventListener {

  private final CommandBus commandBus;

  @EventListener(TicketSoldEvent.class)
  @Transactional  // ← Separate transaction
  public void onTicketSold(TicketSoldEvent event) {
    try {
      // Execute Draw's own command
      commandBus.execute(new ApplyTicketToDrawCommand(
          event.tenantId(),
          event.drawId(),
          event.ticketId(),
          event.amount()
      ));
    } catch (Exception e) {
      // Idempotent: log failure, don't rethrow
      logger.warn("Failed to apply ticket to draw (idempotent)", e);
    }
  }
}
```

**Rules for effects**:

- ✅ Source publishes event in `AfterCommit.run()`
- ✅ Event contains tenant + IDs (no sensitive data)
- ✅ Target listens via `@EventListener`
- ✅ Listener has `@Transactional` (separate transaction)
- ✅ Listener executes own CommandBus command
- ✅ Listener is idempotent (can run multiple times safely)
- ✅ Listener catches and logs failures (never rethrows)
- ❌ NO direct handler-to-handler calls
- ❌ NO nested transactions
- ❌ NO bypassing CommandBus

### 11.3 Feature orchestration (multi-domain coordination)

Features MAY orchestrate cross-domain flows (NO business logic, pure composition).

```java
@Service
@RequiredArgsConstructor
public class CompleteCheckoutService {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  public CheckoutResultDto completeCheckout(CompleteCheckoutRequest req) {
    var ctx = TchContext.current();

    // Step 1: Get ticket summary from Sales
    var ticket = queryBus.ask(
        new GetTicketQuery(ctx.tenantId(), req.ticketId())
    );

    // Step 2: Reserve payout from Draw
    var payout = commandBus.execute(new ReservePayoutCommand(
        ctx.tenantId(),
        ticket.drawId(),
        req.amount()
    ));

    // Step 3: Complete sale in Sales
    commandBus.execute(new CompleteSaleCommand(
        ctx.tenantId(),
        req.ticketId(),
        payout.id()
    ));

    // Step 4: Return aggregated response
    return new CheckoutResultDto(
        ticket.id(),
        payout.amount(),
        payout.status()
    );
  }
}
```

**Rules for features**:

- ✅ Orchestrate via CommandBus/QueryBus
- ✅ Compose read-models into UI payloads
- ✅ Sequence calls explicitly
- ❌ NEVER access repositories
- ❌ NEVER define business logic
- ❌ NEVER mutate core state directly

---

## 12. Global Dependency Rules (STRICT)

```
common
  ↑
catalog ←── core
  ↑         ↑
  └─ features
```

**Allowed dependencies**:

- `common` ← used by all
- `catalog` ← used by core, features
- `core` ← used by features via queries/commands
- `features` ← NO dependencies from core/catalog

**Prohibited**:

- ❌ core → features
- ❌ catalog → features
- ❌ catalog → core (write direction)
- ❌ features → repositories
- ❌ features → JPA entities
- ❌ core → another core's infra

---

## 13. RLS & Tenant scoping (CRITICAL)

### 13.1 Read-side RLS

- ✅ NO tenant filters in Java code
- ✅ tenant scoping is 100% SQL (via RLS policies)
- ✅ repositories rely on `app.current_tenant` PostgreSQL setting
- ❌ FORBIDDEN: `findByTenantId(...)` in read-side queries

### 13.2 Write-side flexibility

- ✅ tenantId MAY be passed explicitly
- ✅ write repositories MAY filter by tenant
- ✅ admin use-cases MAY override context

---

## 14. Soft delete & lifecycle

### 14.1 Logical removal pattern

- `deleted_at` column = logical removal
- Existence checks MUST use `...AndDeletedAtIsNull`
- Commands MUST handle resurrect/recreate logic

```java
// ✅ Correct repository query
public interface TicketRepository extends JpaRepository<TicketJpaEntity, UUID> {
  Optional<TicketJpaEntity> findByIdAndDeletedAtIsNull(UUID id);
}
```

### 14.2 Seed/bootstrap reasoning

Seed and bootstrap logic MUST reason in terms of **live rows** only.

---

## 15. Typed IDs (MANDATORY EVERYWHERE)

### 15.1 Where to use typed IDs

- ✅ domain layer
- ✅ application layer
- ✅ DTOs and API contracts
- ✅ queries and commands

### 15.2 Where to use raw UUID

- ✅ JPA entities (ONLY)
- ✅ persistence adapters (mapping layer)
- ✅ SQL/Flyway migrations

See: `docs/conventions/typed_ids.md`

---

## 16. Documentation split (VERY IMPORTANT)

This document (`openspec/context/80-core-rules.md`) defines:

- **structure** (folders, packages, layers)
- **layering rules** (domain → application → infra)
- **technical constraints** (no Spring in domain, typed IDs, etc.)

**`DOMAIN_<X>.md`** (in `src/main/java/com/tchalanet/server/core/<domain>/`) defines:

- **business meaning** (what the domain does)
- **invariants** (rules, constraints)
- **lifecycle rules** (state machines, transitions)
- **examples** (scenarios, edge cases)

🚫 Never mix both. Each plays a distinct role.

---

## 17. Enforcement (ArchUnit REQUIRED)

ArchUnit tests MUST enforce:

- ✅ no domain → infra access
- ✅ no application → infra imports
- ✅ no controller → repository access
- ✅ no UUID outside infra
- ✅ no tenant filters in read-side code
- ✅ correct dependency direction (no reverse dependencies)

Violations require:

- ✅ refactor, OR
- ✅ explicit ADR with business justification

🚫 NEVER disable ArchUnit without review.

---

## 18. Mental model (TL;DR)

- **Domain** = business rules & invariants (pure, deterministic)
- **Application** = CQRS handlers + ports (orchestration entry point)
- **Infra** = Spring, JPA, HTTP, adapters (implementations)
- **Command** = intent to change state (transactional)
- **Query** = question to get data (read-only)
- **Event** = fact that happened (published after commit)
- **Port** = contract between application & infra
- **Feature** = UI-oriented orchestration of core domains

If a class does not fit clearly into one role → **it is misplaced**.

---
