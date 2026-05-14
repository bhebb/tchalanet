# platform-idempotence Specification

## Purpose

`platform.idempotence` owns retry/replay safety for HTTP writes and event consumers.

HTTP idempotency and processed-event idempotence are separate mechanisms. Neither replaces domain
state transitions, locks, unique constraints, or business invariants.

## Requirements

### Requirement: HTTP idempotency API and annotation

`platform.idempotence` SHALL provide HTTP idempotency through `@RequireIdempotency(scope=...)` and
public idempotence APIs.

#### Scenario: Missing required Idempotency-Key

- **GIVEN** an endpoint requires idempotency
- **WHEN** the request has no `Idempotency-Key`
- **THEN** the server SHALL return `400`
- **AND** the error code SHALL be `idempotency.missing`.

#### Scenario: Replay with same payload

- **GIVEN** an idempotency record exists for `(tenant, scope, key)` with the same request hash and status `COMPLETED`
- **WHEN** the client retries the request
- **THEN** the server SHALL return the same resource/result semantics
- **AND** it SHALL NOT execute the write command again.

#### Scenario: Replay with different payload

- **GIVEN** an idempotency record exists for `(tenant, scope, key)`
- **WHEN** the request hash differs
- **THEN** the server SHALL return `409`
- **AND** the error code SHALL be `idempotency.payload_mismatch`.

#### Scenario: Request in progress

- **GIVEN** an idempotency record exists with status `IN_PROGRESS`
- **WHEN** another request with same `(tenant, scope, key)` arrives
- **THEN** the server SHALL return `409`
- **AND** the error code SHALL be `idempotency.in_progress`.

### Requirement: Dangerous write endpoints declare idempotency

Write endpoints that can create costly or externally visible duplicate effects SHALL declare HTTP
idempotency.

#### Scenario: Sell ticket retry after timeout

- **GIVEN** the first sell request created a ticket but the client timed out
- **WHEN** the client retries with the same `Idempotency-Key` and same payload
- **THEN** the server SHALL return the same ticket result
- **AND** it SHALL NOT create a second ticket.

#### Scenario: Payout retry after network failure

- **GIVEN** a payout operation is idempotency-protected
- **WHEN** the client retries with the same key and payload
- **THEN** payout state transition SHALL NOT execute twice
- **AND** domain payout invariants SHALL still be checked.

### Requirement: Event consumers are idempotent

Projectors and event consumers SHALL use processed-event idempotence for at-least-once delivery.

#### Scenario: Duplicate event delivery

- **GIVEN** a consumer already processed `(tenant, handler_key, event_id)`
- **WHEN** the same event is delivered again
- **THEN** the consumer SHALL skip processing
- **AND** it SHALL NOT create duplicate side effects.

### Requirement: Handler keys are stable constants

Processed-event handler keys SHALL be stable constants owned by code.

#### Scenario: Projector defines handler key

- **GIVEN** a projector consumes domain events
- **WHEN** it checks processed-event idempotence
- **THEN** it SHALL use a stable constant such as `notification.payout.requested`
- **AND** it SHALL NOT accept handler key from clients.

### Requirement: Idempotency is not a business invariant substitute

Idempotency SHALL NOT replace domain rules, locks, unique constraints, or state transitions.

#### Scenario: Duplicate payout payment

- **GIVEN** an endpoint is idempotent
- **WHEN** the domain state already marks the payout paid
- **THEN** `core.payout` SHALL still enforce payout state invariants
- **AND** idempotence SHALL NOT be the only protection against double payment.

### Requirement: Tenant-safe idempotency persistence

Idempotency tables SHALL be tenant-scoped and RLS-compatible.

#### Scenario: Lookup existing idempotency record

- **GIVEN** an idempotency record lookup runs under tenant context
- **WHEN** the repository searches by scope and key
- **THEN** RLS SHALL enforce tenant isolation
- **AND** application code SHALL NOT trust tenant ids from clients.

### Requirement: Expiry behavior is explicit

Expired idempotency records SHALL have documented cleanup and replay behavior.

#### Scenario: Retry after record expiry

- **GIVEN** a client retries after the idempotency record expired
- **WHEN** the endpoint receives the old key
- **THEN** behavior SHALL be explicit in code and docs
- **AND** the endpoint SHALL still rely on domain invariants to prevent unsafe duplicates.
