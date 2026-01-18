# Command Handler Template

Path / Package:

- `core.<bc>.application.command.handler`

Rules (MUST):

- Annotate the handler with `@UseCase`.
- Constructor injection only (use final fields).
- Inputs/outputs are `record` types (command model in `application.command.model`).
- Use ID wrappers (e.g. `TenantId`, `TicketId`) in command and domain APIs.
- Transaction boundary for write commands: annotate handler method with `@TchTx`.
- Publish domain events only after commit using `AfterCommit.run(...)` and a `DomainEventPublisher`.

Notes / Guidance:

- Prefer small handlers with a single responsibility. If the handler grows, extract services or collaborators.
- Validate command invariants early (prefer dedicated validators called from the handler or a decorator).
- Ensure idempotency when required (e.g. payments, issue ticket) using idempotency keys and unique constraints.
- Tests: provide a unit test for the handler using in-memory ports when possible. Use AssertJ, `@Nested` for scenarios, `assertAll(...)`.

Example (template):

```java
package core.<bc>.application.command.handler;

import core.<bc>.application.command.model.CancelTicketCommand;
import core.<bc>.application.command.model.CancelResult;
import core.<bc>.port.out.TicketWriterPort;
import core.common.events.DomainEventPublisher;
import core.common.tx.AfterCommit;
import core.common.tx.TchTx;
import core.common.usecase.UseCase;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CancelTicketHandler implements CommandHandler<CancelTicketCommand, CancelResult> {

  private final TicketWriterPort writer;
  private final DomainEventPublisher events;

  @Override
  @TchTx
  public CancelResult handle(CancelTicketCommand c) {
    // validation if needed (or call a validator)

    var res = writer.cancel(c.tenantId(), c.ticketId(), c.reason());

    // publish domain event AFTER commit
    AfterCommit.run(() -> events.publish(new TicketCancelledEvent(c.tenantId(), c.ticketId(), c.reason())));

    return res;
  }
}
```

Alternative (explicit constructor, no Lombok):

```java
@UseCase
public class CancelTicketHandler implements CommandHandler<CancelTicketCommand, CancelResult> {

  private final TicketWriterPort writer;
  private final DomainEventPublisher events;

  public CancelTicketHandler(TicketWriterPort writer, DomainEventPublisher events) {
    this.writer = writer;
    this.events = events;
  }

  @Override
  @TchTx
  public CancelResult handle(CancelTicketCommand c) {
    // ... same as above
  }
}
```

Testing checklist for this handler:

- Unit tests for happy path and error scenarios (`@Nested` sections).
- Test transaction boundary (simulated) and that AfterCommit publishes via a fake publisher.
- Idempotency tests if applicable.
- Verify no JPA entities are leaked into domain code (use ports/projections).

Template variables to replace:

- `<bc>` → business context (ex: `sales`, `payout`)
- Command/result types → place in `core.<bc>.application.command.model`

Conformance report (to be filled by implementer):

- Placement: core.<bc>/application/command/handler → Done/Deferred
- IDs: ID wrappers used in command and ports → Done/Deferred
- Transaction: `@TchTx` present on write handler → Done/Deferred
- Events: published via `AfterCommit.run(...)` → Done/Deferred
- Migration: any DB schema change added to Flyway → Done/Deferred
