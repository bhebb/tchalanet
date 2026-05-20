# Design — Harden Internal Command/Query Bus

## Context

Tchalanet uses two central in-process buses:

- `CommandBus` for write/use-case intents;
- `QueryBus` for read/use-case questions.

These buses are not external service buses. They are synchronous dispatchers from a message object to its single handler bean.

Current risk points:

- `resolveGenericParameter(...)` is home-grown reflection;
- unresolved handler generic types may be ignored;
- internal maps are mutable fields;
- exceptions are generic `IllegalStateException`;
- there is no explicit startup report;
- there is no architectural enforcement around bus usage.

## Target design

### Package shape

```text
common.bus
  Command
  Query
  CommandBus
  QueryBus
  CommandHandler
  VoidCommandHandler
  QueryHandler
  SimpleCommandBus
  SimpleQueryBus
  HandlerRegistry
  HandlerTypeResolver
  BusRegistrationException
  DuplicateHandlerException
  NoHandlerException
```

### Handler registry

`HandlerRegistry` is an internal utility used by both `SimpleCommandBus` and `SimpleQueryBus`.

Responsibilities:

- discover handler beans provided by Spring;
- resolve the concrete message type;
- detect duplicates;
- produce an immutable `Map<Class<?>, HandlerBinding>`;
- log counts and registration duration.

It must not:

- call handlers;
- enforce business rules;
- know about tenant, permissions, transactions, or domains.

### Type resolution

Use Spring `ResolvableType` against the target class of the bean:

```java
Class<?> targetClass = AopUtils.getTargetClass(bean);
ResolvableType type = ResolvableType.forClass(targetClass).as(CommandHandler.class);
Class<?> messageType = type.getGeneric(0).resolve();
```

Equivalent logic applies for:

- `VoidCommandHandler<C>` -> generic index 0;
- `QueryHandler<Q, R>` -> generic index 0.

If resolution returns `null`, fail startup with `BusRegistrationException`.

### Immutability

The registry map must be local during startup and assigned as an immutable copy:

```java
this.handlers = Map.copyOf(discovered);
```

The field may be initialized to `Map.of()` and assigned once during `@PostConstruct`.

### Dispatch

Dispatch remains:

```java
handler = handlers.get(message.getClass())
```

This means:

- no polymorphic dispatch by superclass/interface;
- no nearest-match resolution;
- exact message class only.

Rationale: exact dispatch avoids ambiguity and makes one message = one handler enforceable.

### Exceptions

Use project-specific runtime exceptions for diagnostics:

- `BusRegistrationException`: startup/configuration issue.
- `DuplicateHandlerException`: two beans handle the same message class.
- `NoHandlerException`: runtime dispatch with no handler.
- `InvalidHandlerException` if a registry entry is corrupted or incompatible.

These exceptions may extend `IllegalStateException` to preserve current failure semantics.

### Observability

At startup, log once per bus:

```text
CommandBus initialized: commandHandlers=143, voidCommandHandlers=27, totalMessages=170, initTimeMs=42
QueryBus initialized: queryHandlers=96, totalMessages=96, initTimeMs=18
```

Optional debug log:

```text
Registered command SellTicketCommand -> SellTicketCommandHandler
```

No per-dispatch logging by default.

### Performance stance

Runtime dispatch is O(1) `Map.get(...)` and must not be optimized prematurely.

Add a regression/performance-style test that registers a large number of fake handlers or directly builds the registry with many bindings and verifies dispatch remains simple and fast. This test should be stable and not rely on tight machine-dependent thresholds.

## Clean architecture relation

The bus hardening does not enforce the entire clean architecture by itself. It only guarantees correct dispatch.

Clean architecture must be enforced by ArchUnit tests:

- `common` must not depend on `core`, `catalog`, or `features`;
- `core` must not depend on `features`;
- `features` must not depend on repositories/JPA entities;
- `domain` must not depend on Spring, JPA, web, or infra;
- controllers must not access repositories;
- controllers should dispatch use-cases via `CommandBus` / `QueryBus`;
- query handlers must not publish events;
- command handlers with state changes must use `@TchTx`.

## Alternatives considered

### External service bus for commands

Rejected for this change. Tchalanet commands are use-case intents that often require immediate transaction outcomes. External service bus would change semantics to eventual processing and is not appropriate for core synchronous operations such as selling a ticket.

### Lazy handler lookup

Rejected. The bus is P0 infrastructure and must fail-fast at startup rather than fail on first production call.

### Polymorphic command dispatch

Rejected. Exact message class dispatch avoids ambiguity and keeps the model testable.

### Keep reflection helper

Rejected. Spring already provides `ResolvableType`, which handles common generic hierarchy cases better and reduces custom reflection risk.
