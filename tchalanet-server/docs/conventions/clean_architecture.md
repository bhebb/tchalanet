# Clean Architecture — Package Structure & Boundaries

> **Status**: NORMATIVE  
> **Applies to**: `core/**`, `catalog/**`, `common/**`, `features/**`  
> **Enforced by**: ArchUnit (`architecture` test suite)  
> **Last reviewed**: 2026-05-07

This document defines the mandatory package structure and dependency rules for Tchalanet backend modules.

---

## 1) Core Domain Package Structure (MUST)

Every core domain SHALL be organized around domain, application, ports, and infra layers.

**Canonical package structure:**

```text
core.<domain>
  domain
    model          — aggregates, entities, value objects
    service        — pure domain policies, calculators, rules
    event          — domain events
    exception      — domain-specific exceptions
  application
    command.model     — command DTOs
    command.handler   — command handlers
    query.model       — query DTOs
    query.handler     — query handlers
    port.out          — outbound ports (interfaces for infra adapters)
    service           — application orchestrators/assemblers (optional)
    exception         — application-specific exceptions
  infra
    web               — REST controllers, request/response mappers
    persistence       — JPA entities, repositories, adapters
    event             — event listeners, publishers
    batch             — batch job implementations
    scheduler         — scheduled task implementations
    cache             — cache adapters
    config            — Spring configuration for the domain
```

**Alternative port placement:**  
`core.<domain>.port.out` is acceptable if `application.port.out` feels too nested. Choose one convention per domain and keep it consistent.

---

## 2) Layer Dependency Rules (MUST — Enforced by ArchUnit)

### 2.1 Domain layer MUST remain pure

The `domain` layer SHALL NOT depend on:

- `application` packages
- `infra` packages
- Spring framework (`org.springframework.*`)
- JPA/Hibernate (`jakarta.persistence.*`, `org.hibernate.*`)
- Web/MVC types
- Cache, batch, scheduler

**Domain services are pure policies:**

- No repository injection
- No port injection
- No CommandBus/QueryBus
- No Spring beans
- Deterministic and side-effect free

**When a domain service needs data:**

- The application handler loads it first
- The handler passes a snapshot/value object to the domain service
- The domain service calculates/validates using only domain primitives

### 2.2 Application layer MUST NOT depend on infra

The `application` layer SHALL NOT import:

- `..infra.web..`
- `..infra.persistence..`
- `..infra.cache..`
- `..infra.batch..`
- `..infra.scheduler..`
- JPA entities
- Spring Data repositories
- MVC request/response types

**When a handler needs persistence:**

- It calls an `application.port.out` interface
- An infra adapter implements that port
- The port signature uses domain/application types, NOT JPA entities

### 2.3 Infrastructure adapters MAY depend inward only

Infrastructure adapters MAY depend on `application` and `domain` packages, but domain/application SHALL NOT depend on infra.

**Example (persistence adapter):**

```java
package com.tchalanet.server.core.sales.infra.persistence;

import com.tchalanet.server.core.sales.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.domain.model.Ticket;

@Component
public class JpaTicketAdapter implements TicketWriterPort {
  @Override
  public Ticket save(Ticket ticket) {
    var entity = toEntity(ticket);
    var saved = repository.save(entity);
    return toDomain(saved);
  }
}
```

### 2.4 Web must not depend directly on persistence

Controllers in `..infra.web..` SHALL NOT import or depend on `..infra.persistence..`.

Controllers MUST dispatch through CommandBus/QueryBus instead of calling repositories or persistence adapters directly.

---

## 3) Port Rules (MUST)

### 3.1 `port.in` is forbidden by default

Core modules SHALL NOT define `application.port.in` packages by default because **CommandBus** and **QueryBus** are the canonical application entry points.

If an exceptional need for `port.in` arises:

- Document it in an ADR (`docs/03-adr/`)
- Add an ArchUnit allowlist exception with a TODO pointing to the ADR

### 3.2 `port.out` is the standard outbound contract

Use `application.port.out` (or `port.out`) for interfaces implemented by infra adapters:

- Persistence adapters
- External service clients
- Cache adapters
- Message brokers

**Port signatures MUST use domain/application types**, NOT infra types (JPA entities, HTTP clients, etc.).

---

## 4) Controller Rules (MUST)

### 4.1 Controllers MUST be thin

Controllers SHALL only:

- Validate request input (via `@Valid` and Jakarta Bean Validation)
- Map request DTOs to commands/queries
- Resolve context (tenant, user, locale, timezone)
- Dispatch commands/queries via buses
- Map handler results to HTTP responses
- Declare audit/security metadata (`@AuditLog`, `@PreAuthorize`)

**Controllers MUST NOT:**

- Implement business logic
- Call repositories or persistence adapters directly
- Orchestrate multi-step flows (use `features/` or application handlers instead)

### 4.2 Controllers belong in web packages

Classes annotated with `@RestController` or `@Controller` MUST reside in:

- `..infra.web..`
- `..web..` (approved alternative for feature controllers)

---

## 5) JPA Rules (MUST)

### 5.1 Entities and repositories MUST stay in persistence infra

Classes annotated with `@Entity` SHALL reside in `..infra.persistence..`.

Spring Data repositories SHALL reside in `..infra.persistence..`.

**Invalid:**

```java
// ❌ WRONG — entity in domain
package com.tchalanet.server.core.sales.domain.model;
@Entity public class TicketEntity { }
```

**Valid:**

```java
// ✅ CORRECT — entity in persistence infra
package com.tchalanet.server.core.sales.infra.persistence;
@Entity public class TicketEntity { }
```

---

## 6) Service Naming & Placement (MUST)

### 6.1 Domain services = pure policies

A `domain/service` class MUST be a pure policy, calculator, or rule:

- No Spring injection
- No repositories, ports, buses
- Deterministic
- No I/O

**Example (valid domain service):**

```java
package com.tchalanet.server.core.draw.domain.service;

public class DrawCutoffRule {
  public void requireBeforeCutoff(ZonedDateTime drawTime, ZonedDateTime requestTime) {
    if (requestTime.isAfter(drawTime.minusMinutes(5))) {
      throw new DrawCutoffViolationException();
    }
  }
}
```

### 6.2 Application services = orchestration helpers

An `application/service` class MAY be an orchestrator, assembler, planner, or coordinator:

- Used by handlers to compose multi-step logic
- May inject ports, buses, other services
- Should have a descriptive name (avoid vague `XxxService`)

**Prefer specific names:**

- `TicketPricingOrchestrator`
- `PayoutAssembler`
- `DrawSchedulePlanner`

---

## 7) Inter-Module Boundaries (MUST)

### 7.1 Modules MUST NOT depend on another module's infra

A domain SHALL NOT import `core.<other>.infra.*`.

**Invalid:**

```java
// ❌ WRONG — draw depends on sales infra
package com.tchalanet.server.core.draw.*;
import com.tchalanet.server.core.sales.infra.persistence.TicketRepository;
```

**Valid:**

```java
// ✅ CORRECT — draw asks sales for data through a query
var ticketView = queryBus.ask(new GetTicketForDrawQuery(ticketId));
```

### 7.2 Cross-domain reads use stable contracts

See [inter_domain_calls.md](./inter_domain_calls.md) for full rules.

**Approved patterns for cross-domain reads:**

- `QueryBus.ask(new GetXxxQuery(...))`
- Stable read API exposed by the owner domain
- Catalog API for read-mostly reference data

### 7.3 Cross-domain effects use events after commit

See [inter_domain_calls.md](./inter_domain_calls.md) for full rules.

**Approved pattern for cross-domain effects:**

1. Source domain publishes a domain event after commit
2. Target domain consumes it in a separate transaction
3. Target domain executes a local command/handler for the effect

---

## 8) Enforcement (ArchUnit)

All rules in this document are enforced by:

- `com.tchalanet.server.architecture.CleanArchitectureArchUnitTest`

Tests run automatically on every build.

**If a test fails:**

1. Fix the violation (move class, remove dependency, refactor)
2. If the violation is legacy and requires gradual migration:
   - Add a temporary ArchUnit exception with `allowEmptyShould(true)` or explicit package exclusion
   - Add a TODO comment pointing to an ADR or a migration plan
   - Document the exception in the test

**Do NOT disable ArchUnit or suppress violations without review and ADR approval.**

---

## 9) Migration From Legacy Code

If existing code violates these rules:

1. Document the violation in the ArchUnit test with a TODO
2. Create a migration plan (issue, ADR, or OpenSpec change)
3. Migrate incrementally
4. Remove the exception once the migration is complete

**Do NOT introduce NEW violations.** All new code MUST follow these rules.

---

## 10) Checklist for New Domains

When adding a new core domain:

- [ ] Use canonical package structure (domain, application, infra)
- [ ] Domain services are pure (no Spring, no ports, no repositories)
- [ ] Application handlers use `port.out` for infra dependencies
- [ ] No `port.in` packages (use CommandBus/QueryBus)
- [ ] Controllers live in `..infra.web..` and dispatch via buses
- [ ] JPA entities live in `..infra.persistence..`
- [ ] No cross-module infra dependencies
- [ ] ArchUnit tests pass

---

## Related Documentation

- [Command & Query Handlers](./command_query_handlers.md)
- [Inter-Domain Calls](./inter_domain_calls.md)
- [Bus](./bus.md)
- [Testing Conventions](./testing.md)
