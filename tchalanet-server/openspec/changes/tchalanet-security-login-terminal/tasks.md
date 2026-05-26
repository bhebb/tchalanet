# Tasks — Security Login, Terminal Binding, and Transaction Context

## Phase 0 — Documentation and OpenSpec alignment

- [x] Identify `terminal_binding.md` as the near-code source of truth.
- [x] Move/sync OpenSpec artifacts to the expected change root.
- [x] Align legacy `DOMAIN_TERMINAL.md` with `terminal_binding.md`.
- [x] Replace stale `core.entitlement` references with `platform.entitlement`.
- [x] Validate OpenSpec artifacts strictly.

## Phase 1 — Request context and contracts

- [x] Add or confirm `OperationalContextSource`.
- [x] Add or confirm `OperationalRequestContext`.
- [x] Add `trustedOperationalContextRequired()` if missing.
- [x] Extend `TchRequestContext` to carry operational context and idempotency key.
- [x] Ensure `TchContextFilter` extracts `Idempotency-Key` and operational headers.
- [x] Implement `OperationalContextResolver` with `CLIENT_CLAIM` default.
- [x] Add unit tests for trusted/untrusted source classification.

## Phase 2 — core.terminal domain model

- [x] Finalize target naming against current implementation (`TerminalKind + TerminalSurface`, `TerminalStatus`, legacy `TerminalState`).
- [x] Add/confirm terminal aggregate.
- [x] Add terminal assignment aggregate/model.
- [x] Add terminal device binding aggregate/model.
- [x] Add terminal activation challenge aggregate/model.
- [x] Add typed ids if missing: `TerminalBindingId`, `TerminalActivationChallengeId`.
- [x] Add enums: `TerminalSurface`, `TerminalStatus`, `TerminalCapability`, `TerminalOperation`, `TerminalBindingType`, `TerminalBindingStatus`, `TerminalChallengeType`, `TerminalChallengeChannel`, `TerminalChallengeStatus`.
- [x] Add terminal lifecycle policy/state-machine tests.
- [x] Add activation challenge policy tests for expiry, attempts, and single-use behavior.

## Phase 3 — Persistence

- [x] Confirm migration strategy before adding or editing Flyway files.
- [x] Add/align terminal assignment table/entity/repository.
- [x] Add/align terminal device binding table/entity/repository.
- [x] Add/align terminal activation challenge table/entity/repository.
- [x] Add terminal capability relational storage if not already present.
- [x] Add tenant-aware indexes and uniqueness constraints for active assignment/binding/challenge rules.
- [x] Add RLS policies for all tenant-scoped terminal tables.
- [x] Add audit table changes where entities are audited.
- [x] Add mappers and output ports.

## Phase 4 — Commands and queries

- [ ] `CreateTerminalCommand`.
- [ ] `AssignTerminalToUserCommand`.
- [ ] `LockTerminalCommand`.
- [ ] `RevokeTerminalCommand`.
- [x] `CreateTerminalActivationChallengeCommand`.
- [x] `VerifyTerminalActivationChallengeCommand`.
- [ ] `BindPhysicalTerminalDeviceCommand` or composite pair command.
- [ ] `ActivateVirtualPhoneTerminalCommand`.
- [x] `ResolveOperationalContextQuery`.
- [x] `ValidateTerminalForOperationQuery`.
- [x] `GetCurrentOperationalContextQuery`.
- [ ] `ListTerminalsQuery`.
- [ ] Handler tests for each command/query.

## Phase 5 — Platform integrations

- [x] Integrate `platform.communication.api` for OTP delivery after challenge creation.
- [x] Integrate `platform.accesscontrol` permission keys.
- [ ] Integrate `platform.audit` for terminal lifecycle, binding, activation, selection, sale denial, and sensitive successes.
- [x] Integrate `platform.idempotence` / `@RequireIdempotency` for sales.
- [x] Integrate `platform.entitlement.api` for phone sales and terminal limits.

## Phase 6 — Web/API endpoints

- [x] Add admin terminal endpoints under `/admin/terminals`.
- [x] Add POS pairing endpoints under `/tenant/terminals/{terminalId}/...`.
- [x] Add virtual phone activation endpoints under `/tenant/virtual-terminals/phone/...`.
- [x] Add current operational context endpoints under `/tenant/me/operational-context`.
- [x] Add request/response DTOs without exposing secrets.
- [x] Add OpenAPI annotations.

## Phase 7 — Sales and transaction enforcement

- [x] Update ticket sale entry point to require trusted operational context.
- [x] Validate terminal, assignment, binding, capability, outlet, and session before sale.
- [x] Resolve seller from actor user, outlet, and session before sale persistence.
- [ ] Enforce `ticket.sell.phone` and phone-sales entitlement when source/channel is phone.
- [x] Ensure sales use idempotency scope `SALES_SELL_TICKET`.
- [ ] Audit successful and denied sensitive operations.
- [ ] Add retry/idempotency tests for sale.

## Phase 8 — Client contracts

- [ ] Document Angular login and admin operational-context selection contract.
- [ ] Document Flutter secure storage, refresh token, and local auth contract.
- [ ] Document signed binding header contract for physical POS.
- [ ] Document signed virtual terminal binding contract for phone sales.
- [x] Document seller onboarding and sale operation flow.
- [x] Document terminal onboarding and operation usage.

## Phase 9 — End-to-end and regression tests

- [x] Unit tests for terminal domain policies.
- [ ] Handler tests for terminal commands/queries.
- [ ] Integration tests for RLS and permission evaluator.
- [ ] Integration tests for POS pairing and trusted sale flow.
- [ ] Integration tests for phone terminal activation and phone sale flow.
- [ ] Concurrency tests for challenge verification and re-binding.
- [ ] Audit assertions for success and denial paths.
