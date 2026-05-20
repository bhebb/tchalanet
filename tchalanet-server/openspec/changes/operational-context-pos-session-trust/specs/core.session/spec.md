# Specification: core-session

## ADDED Requirements

### Requirement: core.session owns POS operation resolution

The system SHALL resolve and validate authoritative POS operational context through `core.session`.

#### Scenario: Resolve POS operation context

- **GIVEN** a tenant, actor user, operational hint, and POS action
- **WHEN** `ResolvePosOperationContextQuery` is asked
- **THEN** `core.session` validates terminal, outlet, session, user, trust, and action coherence
- **AND** returns `ValidatedPosOperationContext`

### Requirement: POS resolution is a query

The system SHALL expose POS operational context resolution as a read-only query.

#### Scenario: Caller uses QueryBus

- **GIVEN** a handler requiring a validated POS frame
- **WHEN** it resolves the frame
- **THEN** it calls `queryBus.ask(new ResolvePosOperationContextQuery(...))`
- **AND** it MUST NOT call `commandBus.execute(...)` or `queryBus.execute(...)`

### Requirement: POS validation order is fail-fast

The system SHALL validate POS context in a deterministic fail-fast order.

#### Scenario: Terminal missing fails before session status

- **GIVEN** a hint with an unknown terminal
- **WHEN** POS resolution is asked
- **THEN** the query fails on terminal existence
- **AND** it does not attempt to treat the session as authoritative

### Requirement: Action policy controls trust acceptance

The system SHALL centralize trust requirements by POS action.

#### Scenario: Offline grant requires strong trust

- **GIVEN** a hint with `CLIENT_CLAIM/WEAK`
- **WHEN** the action is `REQUEST_OFFLINE_GRANT`
- **THEN** POS resolution or downstream action policy rejects the request

#### Scenario: Admin POS V1 may accept weak trust

- **GIVEN** a hint with `CLIENT_CLAIM/WEAK`
- **AND** the action is `ADMIN_POS_SELL`
- **WHEN** terminal/outlet/session/user/action validations pass
- **THEN** the query MAY return a valid POS context
