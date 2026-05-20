# Specification: operational-context-resolution

## ADDED Requirements

### Requirement: POS operational context is a claim

The POS operational context transported by HTTP headers SHALL be treated as a claim/hint, not as business proof.

#### Scenario: Handler receives POS headers

- **GIVEN** a request carries terminal, outlet and sales session headers
- **WHEN** the request context is created
- **THEN** the context MAY carry an `OperationalContextHint`
- **AND** the hint SHALL NOT be treated as proof that those resources are valid.

### Requirement: Validated POS frame comes from core.session

Sensitive handlers SHALL resolve a validated POS frame late through `core.session`.

#### Scenario: Offline grant resolves POS context

- **GIVEN** an offline grant request has an operational hint
- **WHEN** the grant handler executes
- **THEN** it SHALL ask `ResolvePosOperationContextQuery`
- **AND** it SHALL use `ValidatedPosOperationContext` for grant issuance.

### Requirement: Admin POS selection is stateless in V1

V1 SHALL NOT persist an active admin POS selection.

#### Scenario: Admin uses multiple outlets

- **GIVEN** an admin has multiple POS tabs open
- **WHEN** each tab sends a POS action
- **THEN** each request SHALL be resolved from its own headers
- **AND** no server-side active POS selection SHALL affect another request.

### Requirement: Trust policy is action-aware

Each sensitive POS action SHALL explicitly define the minimum trust accepted.

#### Scenario: Offline grant receives weak claim

- **GIVEN** the action is `REQUEST_OFFLINE_GRANT`
- **AND** the hint trust is `WEAK`
- **WHEN** POS context is resolved
- **THEN** the request SHALL be rejected before grant issuance.
