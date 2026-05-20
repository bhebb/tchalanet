# Tasks

## P0 — Proposal validation

- [ ] Run `openspec validate enforce-clean-architecture-archunit-all --strict`.
- [ ] Review package rules against current repo structure.
- [ ] Identify legacy exceptions that require temporary ArchUnit allowlist.

## P0 — Bus API migration

- [ ] Update `CommandBus` interface: primary method `execute(C command)`.
- [ ] Update `QueryBus` interface: primary method `ask(Q query)`.
- [ ] Update all controllers, listeners, schedulers, features, and tests:
  - command calls → `commandBus.execute(...)`
  - query calls → `queryBus.ask(...)`

## P0 — ArchUnit dependencies

- [ ] Add ArchUnit dependency if missing.
- [ ] Add `CleanArchitectureArchUnitTest`.
- [ ] Enforce:
  - domain does not depend on Spring/JPA/Web/infra/application;
  - application does not depend on infra/web/persistence/batch/cache/scheduler;
  - infra may depend on application/domain;
  - web does not depend on persistence;
  - no controller accesses repositories/adapters directly.
- [ ] Add package placement checks:
  - `@RestController` only in `..infra.web..` or approved feature web package;
  - `@Entity` only in `..infra.persistence..`;
  - repositories only in `..infra.persistence..`;
  - handlers only in command/query handler packages.
- [ ] Add a rule forbidding `..port.in..` unless allowlisted by ADR.

## P0 — Inter-domain boundaries

- [ ] Add rule: no module depends on another module's `..infra..`.
- [ ] Add rule: no domain depends on another domain’s application/infra.
- [ ] Document accepted read patterns:
  - QueryBus;
  - stable API/read model;
  - catalog read API.
- [ ] Document accepted effect pattern:
  - DomainEvent after-commit;
  - consumer listener idempotent;
  - consumer dispatches local command.

## P1 — Service cleanup

- [ ] Review `domain/service` classes:
  - no Spring;
  - no repositories;
  - no ports;
  - no bus;
  - pure deterministic rules only.
- [ ] Review `application/service` classes:
  - no vague `XxxService` if it is a god service;
  - prefer `Orchestrator`, `Assembler`, `Planner`, `Coordinator`.
- [ ] Move misplaced rules:
  - business invariant → domain model/service;
  - orchestration → handler/application service;
  - technical adapter → infra.

## P1 — Exceptions / getById norm

- [ ] Standardize reader ports:
  - `Optional<X> findById(XId id)`
  - `X getById(XId id)`
- [ ] Remove domain-specific `XxxNotFoundException` classes when they only duplicate `getById`.
- [ ] Keep domain exceptions only for real business invariant failures.

## P1 — Documentation

- [ ] Add/update `docs/conventions/clean_architecture.md`.
- [ ] Add/update `docs/conventions/bus.md`.
- [ ] Update `docs/conventions/command_query_handlers.md`.
- [ ] Update `docs/conventions/inter_domain_calls.md`.
- [ ] Add examples for payout needing tickets/draws.

## Validation

- [ ] `./mvnw test`
- [ ] `./mvnw -DskipTests=false test`
- [ ] `openspec validate enforce-clean-architecture-archunit --strict`
- [ ] No new architecture violations without ADR.
