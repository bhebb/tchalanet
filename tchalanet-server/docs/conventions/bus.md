# CommandBus & QueryBus — Canonical API

> **Status**: NORMATIVE  
> **Applies to**: All code dispatching commands and queries  
> **Last reviewed**: 2026-05-07

This document defines the canonical API and semantics for Tchalanet's CommandBus and QueryBus.

---

## 1) Bus Purpose & Semantics (MUST)

### 1.1 Bus are in-process synchronous dispatchers

`CommandBus` and `QueryBus` are **in-process synchronous dispatchers**, not external service buses.

- Commands and queries are dispatched to their handlers **in the same JVM call path**
- The bus performs **O(1) map lookup** by exact message class
- There is **NO async/eventual delivery, NO Kafka, NO RabbitMQ, NO SQS** at the bus layer

**External async integration belongs to:**

- Domain event publishing (via `DomainEventPublisher` + Spring events for MVP)
- Future outbox patterns (separate from command/query buses)

### 1.2 Canonical language

- **CommandBus executes** a command
- **QueryBus asks** a query
- **Handlers handle** messages (internal method)

This language distinction clarifies intent:

- `execute` = perform an action with side effects
- `ask` = request information read-only
- `handle` = internal handler implementation detail

---

## 2) Bus API (MUST)

### 2.1 CommandBus interface

```java
public interface CommandBus {

  /**
   * Executes a command and returns the result synchronously.
   *
   * @param command the command to execute
   * @return the command result
   * @throws NullPointerException if command is null
   * @throws NoHandlerException if no handler is registered for this command type
   */
  <R> R execute(Command<R> command);
}
```

**Usage example:**

```java
@RestController
@RequiredArgsConstructor
public class TicketController {

  private final CommandBus commandBus;

  @PostMapping("/tickets/{id}/cancel")
  public ResponseEntity<CancelTicketResponse> cancel(@PathVariable String id, @RequestBody CancelRequest req) {
    var command = new CancelTicketCommand(toTicketId(id), req.reason());
    var result = commandBus.execute(command);
    return ResponseEntity.ok(toResponse(result));
  }
}
```

### 2.2 QueryBus interface

```java
public interface QueryBus {

  /**
   * Asks a query and returns the result synchronously.
   *
   * @param query the query to ask
   * @return the query result
   * @throws NullPointerException if query is null
   * @throws NoHandlerException if no handler is registered for this query type
   */
  <R> R ask(Query<R> query);
}
```

**Usage example:**

```java
@RestController
@RequiredArgsConstructor
public class DrawController {

  private final QueryBus queryBus;

  @GetMapping("/draws/active")
  public ResponseEntity<List<DrawView>> getActive() {
    var result = queryBus.ask(new GetActiveDrawsQuery());
    return ResponseEntity.ok(result.draws());
  }
}
```

### 2.3 Legacy methods (DEPRECATED)

The following methods exist ONLY as temporary migration bridges:

```java
// ❌ DEPRECATED — do not use in new code
commandBus.send(command);    // use execute() instead
commandBus.handle(command);  // use execute() instead
queryBus.send(query);        // use ask() instead
queryBus.handle(query);      // use ask() instead
```

**Migration:**

- Replace all `commandBus.send(...)` with `commandBus.execute(...)`
- Replace all `queryBus.send(...)` with `queryBus.ask(...)`
- Replace all `bus.handle(...)` with `execute(...)`/`ask(...)`

**ArchUnit enforcement:**  
`BusNamingArchUnitTest` fails the build if new code calls deprecated bus methods.

---

## 3) Handler Registration (MUST)

### 3.1 One handler per message type

Every command/query class MUST have **exactly one handler**.

The bus detects duplicates at startup and **fails fast** with `DuplicateHandlerException`.

**Invalid (duplicate handlers):**

```java
// ❌ WRONG — two handlers for the same command
public class SellTicketHandler implements CommandHandler<SellTicketCommand, SellTicketResult> { }
public class AlternateSellTicketHandler implements CommandHandler<SellTicketCommand, SellTicketResult> { }
// Startup fails: DuplicateHandlerException
```

### 3.2 Concrete type parameters required

Handlers MUST implement interfaces with **concrete type parameters**.

**Valid:**

```java
// ✅ CORRECT — concrete types
public class SellTicketHandler implements CommandHandler<SellTicketCommand, SellTicketResult> { }
```

**Invalid (raw types):**

```java
// ❌ WRONG — raw type
@SuppressWarnings("rawtypes")
public class SellTicketHandler implements CommandHandler { }
// Startup fails: InvalidHandlerException
```

**Invalid (unresolved generics):**

```java
// ❌ WRONG — generic parameter not resolved
public abstract class BaseHandler<C> implements CommandHandler<C, String> { }
public class MyHandler extends BaseHandler { } // missing type argument
// Startup fails: InvalidHandlerException
```

**Valid (base class with resolved generics):**

```java
// ✅ CORRECT — generic parameters resolved
public abstract class BaseHandler<C extends Command<String>> implements CommandHandler<C, String> { }
public class MyHandler extends BaseHandler<MyCommand> { }
```

### 3.3 Type resolution mechanism

Buses use Spring `ResolvableType` to extract generic type parameters at startup.

If type resolution fails, startup fails with `InvalidHandlerException` + diagnostic message.

---

## 4) Dispatch Contract (MUST)

### 4.1 Exact class lookup

Buses perform **exact class matching**, not polymorphic lookup.

```java
commandBus.execute(command);
// Looks up handler by command.getClass() exactly
// NOT by superclass or interface
```

**Implication:**  
If you dispatch a command using a base type variable, the lookup uses `getClass()`, so the concrete class handler is found.

### 4.2 Null rejection

Sending `null` throws `NullPointerException` immediately.

```java
commandBus.execute(null); // ❌ throws NullPointerException
```

### 4.3 Missing handler

Dispatching a command/query with no registered handler throws `NoHandlerException`.

```java
commandBus.execute(new UnknownCommand());
// ❌ throws NoHandlerException: No handler registered for UnknownCommand
```

### 4.4 Synchronous execution

All bus dispatch is **synchronous and blocking**.

```java
var result = commandBus.execute(command);
// Handler runs in the same thread, same transaction context
// Result is available immediately after the call returns
```

**No async/callback/future patterns:**

- If you need async execution, use `@Async` on a service method or a separate async executor
- The bus itself is always synchronous

---

## 5) Startup Behavior (MUST)

### 5.1 Fail-fast on duplicate handlers

If two handlers are registered for the same message type, startup fails with:

```
BusRegistrationException: Duplicate handler for command SellTicketCommand:
  - SellTicketHandler
  - AlternateSellTicketHandler
```

### 5.2 Fail-fast on unresolved types

If a handler's generic type cannot be resolved, startup fails with:

```
InvalidHandlerException: Cannot resolve message type for handler MyHandler.
Handler must implement CommandHandler/QueryHandler with concrete type parameters.
```

### 5.3 Registry immutability

Handler registries are built during `@PostConstruct` and sealed using `Map.copyOf(...)`.

**No runtime handler registration or mutation is allowed.**

### 5.4 Startup logging

Buses log a summary at startup:

```
CommandBus initialized: commandHandlers=143, voidCommandHandlers=27, totalMessages=170, initTimeMs=42
QueryBus initialized: queryHandlers=96, totalMessages=96, initTimeMs=18
```

Optional debug logs show each registered handler mapping:

```
DEBUG: CommandHandler registered: SellTicketCommand → SellTicketHandler
DEBUG: VoidCommandHandler registered: PublishDrawResultsCommand → PublishDrawResultsHandler
DEBUG: QueryHandler registered: GetActiveDrawsQuery → GetActiveDrawsHandler
```

---

## 6) VoidCommandHandler (Optional Pattern)

For commands that return no result, use `VoidCommandHandler<C>`.

```java
public interface VoidCommandHandler<C extends VoidCommand> {
  void handle(@Valid C command);
}
```

The bus dispatches `VoidCommandHandler` the same way but returns `null`.

**Usage:**

```java
public class NotifyUserHandler implements VoidCommandHandler<NotifyUserCommand> {
  @Override
  @TchTx
  public void handle(NotifyUserCommand c) {
    // Send notification
    notificationService.send(c.userId(), c.message());
  }
}
```

**Dispatch:**

```java
commandBus.execute(new NotifyUserCommand(userId, message));
// Returns null
```

---

## 7) Error Handling (MUST)

### 7.1 Handler exceptions propagate

If a handler throws an exception, it propagates to the caller:

```java
try {
  commandBus.execute(command);
} catch (BusinessRuleException e) {
  // Handler threw this exception
  log.error("Business rule failed", e);
  return ResponseEntity.status(422).body(toError(e));
}
```

### 7.2 No exception wrapping

Buses do NOT wrap handler exceptions in a generic `BusException`.

**Propagation is transparent:**

- Domain exceptions propagate as-is
- Application exceptions propagate as-is
- Infra exceptions propagate as-is

### 7.3 Bus-specific exceptions

Buses throw their own exceptions ONLY for bus-level failures:

- `NoHandlerException` — no handler registered for the message type
- `DuplicateHandlerException` — multiple handlers registered (startup only)
- `InvalidHandlerException` — handler type cannot be resolved (startup only)
- `BusRegistrationException` — generic registration failure (startup only)

---

## 8) Transaction Boundaries (MUST)

### 8.1 Commands are transactional

Command handlers MUST be annotated with `@TchTx` (or equivalent transactional boundary).

```java
@UseCase
@RequiredArgsConstructor
public class SellTicketHandler implements CommandHandler<SellTicketCommand, SellTicketResult> {

  @Override
  @TchTx  // ← Transaction boundary
  public SellTicketResult handle(SellTicketCommand c) {
    // Write operations
  }
}
```

**Bus does NOT manage transactions.**  
The handler is responsible for declaring the transaction boundary.

### 8.2 Queries are NOT transactional by default

Query handlers SHOULD NOT use `@TchTx` unless there is a documented need (e.g., complex read-only transaction isolation).

```java
@UseCase
@RequiredArgsConstructor
public class GetActiveDrawsHandler implements QueryHandler<GetActiveDrawsQuery, ActiveDrawsView> {

  @Override
  public ActiveDrawsView handle(GetActiveDrawsQuery q) {
    // Read-only, no @TchTx
    return reader.findActive();
  }
}
```

### 8.3 Nested bus calls share the transaction

If a handler calls another command/query via the bus, they share the same transaction context:

```java
@TchTx
public SellTicketResult handle(SellTicketCommand c) {
  // This query runs in the same transaction
  var limits = queryBus.ask(new GetSellerLimitsQuery(c.sellerId()));

  // Business logic
  ticket.validate(limits);

  return save(ticket);
}
```

**Implication:**  
The inner query does NOT start a new transaction. It runs in the current transaction.

---

## 9) Performance (MUST)

### 9.1 O(1) dispatch

Bus dispatch is **O(1) map lookup** by exact class.

No linear scanning, no polymorphic matching, no reflection at dispatch time.

**Type resolution happens once at startup**, not on every call.

### 9.2 No overhead vs direct method call

Calling `commandBus.execute(command)` has negligible overhead vs calling `handler.handle(command)` directly.

**Benchmark target (for 500+ handlers):**

- Dispatch time: < 1μs per call
- Startup time: < 100ms for full registry build

### 9.3 Thread safety

Buses are **thread-safe** and immutable after startup.

Multiple threads can dispatch concurrently without synchronization overhead.

---

## 10) Testing (MUST)

### 10.1 Unit test handlers directly

In unit tests, test handlers directly without the bus:

```java
@Test
void shouldSellTicket() {
  var handler = new SellTicketHandler(mockWriter, mockPricer);
  var command = new SellTicketCommand(tenantId, ticketData);

  var result = handler.handle(command);

  assertThat(result.ticketId()).isNotNull();
}
```

### 10.2 Integration test with real bus

In integration tests, use the real bus to verify registration and dispatch:

```java
@SpringBootTest
class BusIntegrationTest {

  @Autowired CommandBus commandBus;

  @Test
  void shouldExecuteCommand() {
    var command = new SellTicketCommand(tenantId, ticketData);

    var result = commandBus.execute(command);

    assertThat(result.ticketId()).isNotNull();
  }
}
```

### 10.3 Test duplicate handler detection

Startup tests should verify that duplicate handlers are detected:

```java
@Test
void shouldFailOnDuplicateHandler() {
  assertThatThrownBy(() -> new SimpleCommandBus(List.of(handler1, handler2)))
    .isInstanceOf(DuplicateHandlerException.class);
}
```

---

## 11) Migration Guide

### 11.1 Migrating from `send()` to `execute()`/`ask()`

**Before:**

```java
commandBus.send(command);
queryBus.send(query);
```

**After:**

```java
commandBus.execute(command);
queryBus.ask(query);
```

**Search and replace:**

- `commandBus.send(` → `commandBus.execute(`
- `queryBus.send(` → `queryBus.ask(`
- `commandBus.handle(` → `commandBus.execute(`
- `queryBus.handle(` → `queryBus.ask(`

### 11.2 ArchUnit enforcement

Once migration is complete, `BusNamingArchUnitTest` enforces that new code does NOT use deprecated methods.

**Test rule:**

```java
@Test
void newCodeShouldNotCallLegacyBusMethods() {
  noClasses()
    .that().resideOutsideOfPackage("..common.bus..")
    .should(callLegacyBusMethods())
    .check(classes);
}
```

### 11.3 Handler method remains `handle`

**Handler internal method does NOT change:**

```java
// ✅ Handler method is still handle()
public class MyHandler implements CommandHandler<MyCommand, MyResult> {
  @Override
  public MyResult handle(MyCommand c) { ... }
}
```

Only the **bus interface methods** changed:

- `CommandBus.send()` → `CommandBus.execute()`
- `QueryBus.send()` → `QueryBus.ask()`

---

## 12) Troubleshooting

### 12.1 Issue: NoHandlerException at runtime

**Symptom:**

```
NoHandlerException: No handler registered for command MyCommand
```

**Cause:**  
No handler implements `CommandHandler<MyCommand, R>`.

**Fix:**

1. Create a handler for `MyCommand`
2. Annotate it with `@UseCase` or `@Component`
3. Ensure it's picked up by component scanning

### 12.2 Issue: DuplicateHandlerException at startup

**Symptom:**

```
DuplicateHandlerException: Duplicate handler for command MyCommand
```

**Cause:**  
Two handlers implement `CommandHandler<MyCommand, R>`.

**Fix:**

1. Remove or rename one of the handlers
2. If both are intentional, refactor to use different command types

### 12.3 Issue: InvalidHandlerException at startup

**Symptom:**

```
InvalidHandlerException: Cannot resolve message type for handler MyHandler
```

**Cause:**  
Handler uses raw types or unresolved generic parameters.

**Fix:**

1. Remove `@SuppressWarnings("rawtypes")`
2. Add concrete type parameters: `implements CommandHandler<MyCommand, MyResult>`
3. If using a base class, ensure generics are preserved: `extends BaseHandler<MyCommand>`

### 12.4 Issue: Bus dispatch is slow

**Symptom:**  
Dispatch takes > 10μs per call.

**Cause:**  
Not a bus issue (dispatch is O(1) map lookup).

**Likely culprit:**

- Handler is slow (database, external API, heavy computation)
- Transaction overhead
- Logging overhead

**Fix:**

1. Profile the handler, not the bus
2. Optimize database queries
3. Cache read-only data
4. Use async for non-critical side effects

---

## Related Documentation

- [Command & Query Handlers](./command_query_handlers.md)
- [Clean Architecture](./clean_architecture.md)
- [Inter-Domain Calls](./inter_domain_calls.md)
- [Testing Conventions](./testing.md)
