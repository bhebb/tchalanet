# Handlers (CQRS) — Commands & Queries

> **Status**: NORMATIVE  
> **Applies to**: `core/**`, `catalog/**` (light CQRS), `features/**` (orchestration only)  
> **Last reviewed**: 2026-01-20

This document defines the canonical way to implement **CommandHandlers** and **QueryHandlers** in Tchalanet.

---

## 1) Placement (MUST)

- Commands: `core.<bc>.application.command.handler`
- Command models: `core.<bc>.application.command.model`
- Queries: `core.<bc>.application.query.handler`
- Query models: `core.<bc>.application.query.model`

Ports:

- `core.<bc>.application.port.out.*` OR `core.<bc>.port.out.*` (choose one convention and keep it consistent)

Infra adapters:

- `core.<bc>.infra.persistence`
- `core.<bc>.infra.web`
- `core.<bc>.infra.event`
- `core.<bc>.infra.batch`

---

## 2) Shared rules (Commands & Queries) — MUST

- Handlers are **application layer**. No JPA entities, no repositories, no Spring MVC types in signatures.
- Inputs/outputs are `record` types.
- Typed IDs are mandatory (`TenantId`, `TicketId`, ...). No raw UUID outside persistence.
- One message type => **exactly one handler** (enforced by the buses at startup).
- Constructor injection only (final fields).
- No static business utilities.

---

## 3) Commands (write) — `CommandHandler<C extends Command<R>, R>`

### 3.1 Contract

- Commands perform **state changes**.
- Commands may publish domain events, but **only AFTER COMMIT**.
- Transaction boundary is mandatory for write commands.

### 3.2 Rules (MUST)

- Handler annotated with `@UseCase`
- `handle(...)` MUST be transactional via `@TchTx` (or equivalent)
- Validate:
  - structural validation via `@Valid` (already on the interface)
  - business invariants inside domain/services (not in controller)
- Side effects:
  - use `AfterCommit.run(...)` for publishing events / audit / async triggers
  - never do cross-domain writes inside the same critical transaction

### 3.3 Template (command)

```java
package com.tchalanet.server.core.sales.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sales.application.command.model.CancelTicketCommand;
import com.tchalanet.server.core.sales.application.command.model.CancelTicketResult;
import com.tchalanet.server.core.sales.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.domain.event.TicketCancelledEvent;
import com.tchalanet.server.core.common.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CancelTicketHandler implements CommandHandler<CancelTicketCommand, CancelTicketResult> {

  private final TicketWriterPort writer;
  private final DomainEventPublisher events;

  @Override
  @TchTx
  public CancelTicketResult handle(CancelTicketCommand c) {
    // 1) call domain / ports
    var res = writer.cancel(c.tenantId(), c.ticketId(), c.reason());

    // 2) publish AFTER COMMIT
    AfterCommit.run(() -> events.publish(new TicketCancelledEvent(c.tenantId(), c.ticketId())));

    return res;
  }
}
```

Command model:

```java
package com.tchalanet.server.core.sales.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CancelTicketCommand(
  @NotNull TenantId tenantId,
  @NotNull TicketId ticketId,
  @NotBlank String reason
) implements Command<CancelTicketResult> {}
```

---

## 4) Queries (read) — `QueryHandler<Q, R>`

### 4.1 Contract

- Queries are **read-only**.
- Queries MUST NOT produce side effects.
- Queries MUST NOT publish events.

### 4.2 Rules (MUST)

- Handler annotated with `@UseCase`
- No `@TchTx` unless you have a documented need (rare). Prefer read-only queries without explicit tx.
- Output should be a projection/view (record), not a domain aggregate.
- Paging:
  - input may carry `Pageable` or `TchPageRequest` (depending on your paging standard)
  - output is `TchPage<R>` for list endpoints

### 4.3 Template (query)

```java
package com.tchalanet.server.core.sales.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.query.model.GetTicketStatusQuery;
import com.tchalanet.server.core.sales.application.query.model.TicketStatusView;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetTicketStatusHandler implements QueryHandler<GetTicketStatusQuery, TicketStatusView> {

  private final TicketReaderPort reader;

  @Override
  public TicketStatusView handle(GetTicketStatusQuery q) {
    return reader.findStatusByPublicCode(q.tenantId(), q.publicCode())
      .orElseThrow(() -> /* ProblemRest.notFound(...) */ new IllegalStateException("not_found"));
  }
}
```

Query model:

```java
package com.tchalanet.server.core.sales.application.query.model;

import com.tchalanet.server.common.types.id.TenantId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GetTicketStatusQuery(
  @NotNull TenantId tenantId,
  @NotBlank String publicCode
) {}
```

---

## 5) Dispatch via buses (MUST)

Use cases MUST be executed through:

- `CommandBus.send(command)`
- `QueryBus.send(query)`

Each message class must have exactly one handler (startup fails otherwise).

Controllers do mapping + validation + bus dispatch. No business logic in controllers.

---

## 6) Testing expectations (MUST)

- Unit test each handler with in-memory/fake ports when possible.
- AssertJ only, JUnit 5, `@Nested` for scenarios.
- Integration tests only for critical flows (see TESTING.md).
