# Tasks — harden-internal-command-query-bus

## 1. Inventory

- [x] Locate all implementations of `CommandHandler`, `VoidCommandHandler`, and `QueryHandler`.
- [x] Identify handlers whose generic message type is indirect, raw, unresolved, or inherited through an unusual hierarchy.
- [x] Fix raw handler declarations before hardening the registry.

## 2. Bus exceptions

- [x] Add `BusRegistrationException` in `common.bus`.
- [x] Add `DuplicateHandlerException` in `common.bus`.
- [x] Add `NoHandlerException` in `common.bus`.
- [x] Optional: add `InvalidHandlerException` in `common.bus`.
- [x] Ensure messages include message type, handler bean class, and previous handler class where relevant.

## 3. Type resolution

- [x] Add `HandlerTypeResolver` using `AopUtils.getTargetClass(bean)` + `ResolvableType`.
- [x] Support `CommandHandler<C, R>`.
- [x] Support `VoidCommandHandler<C>`.
- [x] Support `QueryHandler<Q, R>`.
- [x] Fail if the message type cannot be resolved.
- [x] Fail if the resolved type is not a concrete class.
- [ ] Add focused unit tests for direct and inherited generic handlers.

## 4. Registry

- [x] Add internal `HandlerRegistry` or equivalent utility.
- [x] Build handler map locally during `@PostConstruct`.
- [x] Use `Map.copyOf(...)` for final assigned registry.
- [x] Detect duplicate command handlers.
- [x] Detect duplicate query handlers.
- [x] Detect duplicate command vs void-command handlers for the same command class.
- [x] Add startup summary logging with handler counts and init duration.

## 5. SimpleCommandBus

- [x] Reject `null` command with `Objects.requireNonNull`.
- [x] Use immutable registry.
- [x] Keep exact-class lookup: `handlers.get(command.getClass())`.
- [x] Throw `NoHandlerException` when no handler exists.
- [x] Dispatch `CommandHandler<C, R>` and `VoidCommandHandler<C>` safely.
- [x] Preserve `VoidCommandHandler` returning `null`.

## 6. SimpleQueryBus

- [ ] Reject `null` query with `Objects.requireNonNull`.
- [ ] Use immutable registry.
- [ ] Keep exact-class lookup: `handlers.get(query.getClass())`.
- [ ] Throw `NoHandlerException` when no handler exists.
- [ ] Dispatch `QueryHandler<Q, R>` safely.

## 7. Unit tests

- [ ] CommandBus dispatches to the correct command handler.
- [ ] CommandBus dispatches to the correct void command handler.
- [ ] CommandBus fails when no handler exists.
- [ ] CommandBus fails when duplicate handlers exist.
- [ ] CommandBus fails when handler generic type cannot be resolved.
- [ ] QueryBus dispatches to the correct query handler.
- [ ] QueryBus fails when no handler exists.
- [ ] QueryBus fails when duplicate handlers exist.
- [ ] QueryBus fails when handler generic type cannot be resolved.
- [ ] Null command/query is rejected.
- [ ] Registry maps are immutable after initialization.

## 8. Performance/regression test

- [ ] Add a non-flaky regression test for large handler registry size.
- [ ] Verify lookup remains exact-class map lookup.
- [ ] Log or assert a broad sanity threshold only if stable in CI.

## 9. Clean architecture tests

- [ ] Add ArchUnit dependency if not already present.
- [ ] Add `CleanArchitectureRulesTest`.
- [ ] Enforce `common` has no dependency on `core`, `catalog`, or `features`.
- [ ] Enforce `core` has no dependency on `features`.
- [ ] Enforce `catalog.api` has no dependency on `catalog.internal`.
- [ ] Enforce `features` do not depend on JPA repositories/entities.
- [ ] Enforce `domain` packages do not depend on Spring/JPA/web/infra.
- [ ] Enforce controllers do not depend on repositories.
- [ ] Add optional bus-use rule: controllers dispatch use-cases through `CommandBus` / `QueryBus`.

## 10. Documentation

- [ ] Update `docs/conventions/command_query_handlers.md` with bus P0 rules.
- [ ] Document that `CommandBus` / `QueryBus` are in-process synchronous dispatchers, not external brokers.
- [ ] Document that commands remain synchronous and events handle after-commit side effects.
- [ ] Document startup failure cases and how to fix unresolved handlers.

## 11. Validation

- [ ] Run unit tests for `common.bus`.
- [ ] Run architecture tests.
- [ ] Run full backend test suite if feasible.
- [ ] Start application locally and confirm bus startup summary logs.
- [ ] Manually call representative endpoints: one command endpoint and one query endpoint.
