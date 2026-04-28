## ADDED Requirements

### Requirement: Sales events are published via DomainEventPublisher AfterCommit

All domain events emitted by `core.sales` (`TicketPlacedEvent`, `TicketCancelledEvent`, `TicketResultedEvent`, `TicketResultOverriddenEvent`, `TicketPaidEvent`, `TicketPaymentPendingEvent`) SHALL be published using:

```java
AfterCommit.run(() -> publisher.publish(event));
```

where `publisher` is `com.tchalanet.server.common.event.DomainEventPublisher`. Sales SHALL NOT use a domain-specific publisher port (`TicketEventPublisherPort` is removed). Events SHALL NOT be published synchronously (no `publisher.publish(...)` outside `AfterCommit.run`).

#### Scenario: TicketPlacedEvent is published after commit

- **WHEN** a sell command commits successfully
- **THEN** `DomainEventPublisher.publish(TicketPlacedEvent)` is invoked AFTER the transaction commits
- **AND** the event is NOT published if the transaction rolls back

#### Scenario: No TicketEventPublisherPort exists

- **WHEN** the project compiles
- **THEN** `TicketEventPublisherPort` does not exist in `core.sales.application.port.out.*`

### Requirement: Cross-domain listeners use AFTER_COMMIT

Cross-domain listeners that consume sales events SHALL be annotated `@TransactionalEventListener(phase = AFTER_COMMIT)`. Synchronous `@EventListener` (in-tx) SHALL NOT be used for cross-domain side effects.

This applies to listeners inside `core.sales` itself when they react to events from another domain (e.g., `DrawResultedEventListener` consuming `DrawResultAppliedEvent`).

#### Scenario: SalesLedgerListener uses AFTER_COMMIT

- **WHEN** the listener `SalesLedgerListener` is reflected
- **THEN** the handler method carries `@TransactionalEventListener(phase = AFTER_COMMIT)`
- **AND** does NOT carry `@EventListener` alone

#### Scenario: DrawResultedEventListener uses AFTER_COMMIT

- **WHEN** the listener `DrawResultedEventListener` is reflected
- **THEN** the handler method carries `@TransactionalEventListener(phase = AFTER_COMMIT)`

### Requirement: Cross-domain side effects go through CommandBus

Cross-domain side effects triggered by a sales event SHALL be dispatched via `CommandBus.send(...)` to a command in the target domain. Sales SHALL NOT call port-in interfaces of other bounded contexts directly.

Specifically, `SalesLedgerListener` SHALL send `RecordTicketSaleLedgerCommand` (defined in `core.ledger.application.command.model.*`) instead of calling `RecordLedgerFromSalesPort.recordTicketSale(...)` (the port-in is removed).

#### Scenario: Ledger recording goes through command bus

- **WHEN** `TicketPlacedEvent` is received by `SalesLedgerListener`
- **THEN** the listener calls `commandBus.send(new RecordTicketSaleLedgerCommand(...))`
- **AND** does NOT call `ledgerPort.recordTicketSale(...)` directly

#### Scenario: RecordLedgerFromSalesPort is removed

- **WHEN** the project compiles
- **THEN** `RecordLedgerFromSalesPort` does not exist in `core.ledger.application.port.in.*`

### Requirement: Listeners do not silently absorb exceptions

Cross-domain listeners SHALL NOT wrap their dispatch in a global `try { ... } catch (Exception e) { log.error(...) }` block that absorbs the exception. If a downstream call fails, the exception SHALL propagate to the listener framework, which handles failure (retry policy, dead-letter, etc.).

Targeted catch blocks for KNOWN, EXPECTED exceptions are allowed (e.g., `catch (DataAccessException e) { meterRegistry.counter(...).increment(); throw new RetryableException(e); }`), provided they re-throw or convert to a typed exception.

#### Scenario: SalesLedgerListener has no global catch

- **WHEN** `SalesLedgerListener.onTicketPlaced(...)` is reflected
- **THEN** the method body does NOT contain a `try { ... } catch (Exception e) { log.error(...) }` block
- **AND** any exception thrown by `commandBus.send(...)` propagates to the caller

### Requirement: performedBy fields in events use typed UserId

`TicketCancelledEvent.performedBy` and `TicketResultOverriddenEvent.performedBy` SHALL be of type `UserId` (typed wrapper). Raw `UUID` SHALL NOT appear in event field signatures for actor identification.

#### Scenario: Event constructors require UserId

- **WHEN** `new TicketCancelledEvent(..., performedBy, ...)` is invoked
- **THEN** the parameter type for `performedBy` is `UserId`, not `UUID`

#### Scenario: JSON serialization of UserId is a string UUID

- **WHEN** `TicketCancelledEvent` is serialized to JSON
- **THEN** the `performedBy` field is a string representation of the underlying UUID
- **AND** consumers can deserialize via `UserId.of(UUID.fromString(json))`
