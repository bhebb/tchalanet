# Tasks — secure-login-terminal-context

## Phase 1 — Context and contracts

- [ ] Add/confirm `OperationalContextSource` enum.
- [ ] Add/confirm `OperationalRequestContext` record.
- [ ] Add `trustedOperationalContextRequired()` helper if missing.
- [ ] Extend `TchRequestContext` to carry operational context and idempotency key.
- [ ] Ensure `TchContextFilter` extracts `Idempotency-Key` and operational headers.
- [ ] Implement `OperationalContextResolver` with CLIENT_CLAIM default.

## Phase 2 — core.terminal domain

- [ ] Add terminal aggregate/model.
- [ ] Add terminal assignment model.
- [ ] Add terminal device binding model.
- [ ] Add terminal activation challenge model.
- [ ] Add typed ids if missing: `TerminalBindingId`, `TerminalActivationChallengeId`.
- [ ] Add enums: `TerminalType`, `TerminalStatus`, `TerminalCapability`, `BindingStatus`, `ActivationChallengeStatus`.
- [ ] Add Flyway migrations with RLS.
- [ ] Add JPA entities and repositories in infra.persistence.
- [ ] Add mappers.
- [ ] Add ports.

## Phase 3 — Commands and Queries

- [ ] `CreateTerminalCommand`.
- [ ] `AssignTerminalToUserCommand`.
- [ ] `LockTerminalCommand`.
- [ ] `RevokeTerminalCommand`.
- [ ] `CreateTerminalActivationChallengeCommand`.
- [ ] `VerifyTerminalActivationChallengeCommand`.
- [ ] `BindPhysicalTerminalDeviceCommand`.
- [ ] `ActivateVirtualPhoneTerminalCommand`.
- [ ] `ResolveOperationalContextQuery`.
- [ ] `ValidateTerminalForOperationQuery`.
- [ ] `GetCurrentOperationalContextQuery`.
- [ ] `ListTerminalsQuery`.

## Phase 4 — Platform integrations

- [ ] Integrate `platform.communication.api` for OTP delivery.
- [ ] Integrate `platform.accesscontrol` permission keys.
- [ ] Integrate `platform.audit` on all sensitive actions.
- [ ] Integrate `platform.idempotence` / `@RequireIdempotency` for sales.
- [ ] Integrate `core.entitlement`/plan checks for phone sales.

## Phase 5 — Web/API endpoints

- [ ] Add admin terminal endpoints under `/admin/terminals`.
- [ ] Add POS pairing endpoints under `/tenant/terminals/{terminalId}/...`.
- [ ] Add virtual phone activation endpoints under `/tenant/virtual-terminals/phone/...`.
- [ ] Add current operational context endpoints under `/tenant/me/operational-context`.
- [ ] Add OpenAPI annotations and request/response DTOs.

## Phase 6 — Sales enforcement

- [ ] Update `SellTicketCommandHandler` to require trusted operational context.
- [ ] Validate terminal/outlet/session before sale.
- [ ] Enforce phone sales permission and entitlement when source/channel is phone.
- [ ] Ensure sales use idempotency scope `SALES_SELL_TICKET`.
- [ ] Audit successful and denied sensitive operations as designed.

## Phase 7 — Frontend/mobile contracts

- [ ] Angular uses Keycloak login and sends Bearer token.
- [ ] Flutter stores refresh token and binding in secure storage.
- [ ] Flutter uses local auth only to unlock secure storage.
- [ ] Flutter handles refresh token missing/expired -> login required.
- [ ] Flutter sends binding and operational headers.

## Phase 8 — Tests

- [ ] Unit tests for terminal policies.
- [ ] Handler tests for commands/queries.
- [ ] Integration tests for RLS and permission evaluator.
- [ ] Integration tests for POS and phone sales flows.
- [ ] Idempotency retry tests.
- [ ] Concurrency tests.
- [ ] Audit assertions.

