# Backend Testing — Events and Projectors

Use this guidance when testing event publishing, listeners, and projectors.

## Event publishing rules

1. Events are published **after commit only** (use `@TransactionalEventListener(phase = AFTER_COMMIT)`).
2. If transaction rolls back, event is not published.
3. Event payload contains enough data for the listener to act without additional queries when possible.

## Event listener / projector rules

1. Listener is **thin** — delegates to a handler or service.
2. Listener is **idempotent** — duplicate event is skipped based on processed event record.
3. Handler key is **stable** — uses event type + aggregate ID or explicit idempotency key.
4. Processed event lookup uses **RLS-safe query** (tenant_id in WHERE clause or RLS enforced).

## Required test scenarios

### Event publishing

```java
@Test
void successfulCommand_publishesEvent() { }

@Test
void transactionRollback_doesNotPublishEvent() { }
```

### Event listener

```java
@Test
void newEvent_processesSuccessfully() { }

@Test
void duplicateEvent_skipped() { }

@Test
void eventFromOtherTenant_ignored() { }
```

## Test structure

Use `@DomainEvents` + `@AfterDomainEventPublication` for testing event publish, or verify via event capture listener.

For projector/listener tests, either:

- Use Spring integration test with real event bus.
- Unit test the handler directly with a fake processed event repository.

## Avoid

- Testing private event publishing logic.
- Asserting internal event sequence unless ordering matters for correctness.
- Publishing events synchronously during transaction (use after-commit).
