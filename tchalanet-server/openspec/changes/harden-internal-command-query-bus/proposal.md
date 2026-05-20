# Change: harden-internal-command-query-bus

## Why

`CommandBus` and `QueryBus` are P0 infrastructure in Tchalanet. They are the central dispatch mechanism for controllers, features, batch jobs, event listeners, and cross-domain orchestration.

The current `SimpleCommandBus` / `SimpleQueryBus` implementation works for a small MVP, but it has risks that are unacceptable for a long-lived clean architecture:

- a handler whose generic message type cannot be resolved may be silently skipped;
- registration uses ad-hoc reflection instead of Spring's type model;
- handler registries are mutable after initialization;
- missing/duplicate handlers are detected, but diagnostics and test coverage are not strong enough;
- performance assumptions with hundreds/thousands of handlers are not measured;
- clean-architecture compliance is currently mostly convention-based, not enforced by architectural tests.

This change hardens the in-process bus layer without turning it into an external broker. Commands and queries remain synchronous in-process use-case dispatch. Domain events remain Spring in-process for MVP and are treated separately.

## What

- Replace ad-hoc generic type resolution with a robust Spring-based resolver (`ResolvableType` preferred).
- Make both buses fail-fast at startup when any handler bean cannot be resolved.
- Keep the invariant: one message class maps to exactly one handler.
- Build immutable handler registries after startup.
- Introduce explicit bus exceptions with clear diagnostics.
- Add logging/observability for discovered handlers and startup time.
- Add unit tests for registration, dispatch, duplicates, unresolved handlers, null inputs, and void commands.
- Add a lightweight performance/regression test or benchmark-style test for large handler counts.
- Add ArchUnit tests to protect bus usage and clean architecture boundaries.
- Update bus/conventions documentation to mark the buses as P0 infrastructure.

## Non-goals

- Do not introduce Kafka/RabbitMQ/Azure Service Bus/SQS.
- Do not make commands asynchronous by default.
- Do not move business rules into `common.bus`.
- Do not implement outbox in this change.
- Do not refactor domain/event listeners except where necessary to satisfy bus contract tests.
- Do not change public HTTP APIs.

## Decision

Tchalanet keeps a three-level execution model:

1. `CommandBus` / `QueryBus`: synchronous in-process use-case dispatch.
2. `DomainEventPublisher` + Spring events: in-process after-commit event dispatch for MVP.
3. Future outbox/external broker: durable async integration path, separate future change.

The bus layer stays in `common.bus` and remains purely technical. It enforces handler registration correctness but never enforces business rules, permissions, tenant policy, or transactional decisions.

## Impact

Affected area:

- `tchalanet-server/src/main/java/com/tchalanet/server/common/bus/**`
- `tchalanet-server/src/test/java/com/tchalanet/server/common/bus/**`
- `tchalanet-server/src/test/java/com/tchalanet/server/architecture/**`
- `tchalanet-server/docs/conventions/command_query_handlers.md`

Runtime impact:

- Startup may now fail if an existing handler is misdeclared.
- Runtime dispatch remains O(1) map lookup.
- Handler lookup remains synchronous and local to the JVM.

Migration impact:

- Misdeclared handlers must be fixed by implementing the concrete generic interface directly or via a supported resolvable hierarchy.
- Any duplicate handler for the same command/query must be resolved.
