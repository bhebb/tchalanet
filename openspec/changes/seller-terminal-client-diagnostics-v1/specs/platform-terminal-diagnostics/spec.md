# platform-terminal-diagnostics Delta

## ADDED Requirements

### Requirement: Seller terminal diagnostics are explicitly activated

The `platform.clientdiagnostics` capability SHALL allow diagnostics to be
enabled for a specific seller terminal only with an expiry time and operator
reason.

#### Scenario: Enable diagnostics for one terminal

- **GIVEN** a tenant admin has diagnostics management permission
- **WHEN** they enable diagnostics for a seller terminal with duration and reason
- **THEN** the terminal has an active diagnostics policy until the computed expiry
- **AND** other seller terminals in the tenant are unaffected.
- **AND** the computed expiry does not exceed the backend maximum activation duration.

#### Scenario: Diagnostics expire automatically

- **GIVEN** a seller terminal diagnostics policy expired
- **WHEN** the POS profile is loaded
- **THEN** diagnostics are reported as disabled
- **AND** new diagnostic events for that terminal are rejected or ignored according to server policy.

### Requirement: POS diagnostic events are context-bound

The platform SHALL ingest POS diagnostic event batches only for the
authenticated terminal context and SHALL NOT trust tenant or terminal
identifiers supplied by the client.

#### Scenario: Client submits diagnostic event

- **GIVEN** diagnostics are active for the authenticated seller terminal
- **WHEN** the POS submits a diagnostic event batch to `/tenant/client-diagnostics/events`
- **THEN** the server stores the event with tenant, seller terminal, and seller resolved from request context
- **AND** the client payload does not define those authority fields.

#### Scenario: Diagnostics disabled

- **GIVEN** diagnostics are disabled for the seller terminal
- **WHEN** the POS submits a diagnostic event
- **THEN** the event is not stored
- **AND** the POS receives a response that allows it to stop retrying.

### Requirement: Diagnostic payloads are closed and bounded

The platform SHALL accept only a closed diagnostic event schema and SHALL reject
or sanitize unsafe diagnostic content.

#### Scenario: Event includes unsafe metadata

- **GIVEN** a diagnostic event includes a token, password, secret, raw body, unrestricted stack trace, or oversized data
- **WHEN** the event is submitted
- **THEN** the server rejects or sanitizes the unsafe content
- **AND** no secret is persisted.

#### Scenario: Event uses unsupported field

- **GIVEN** a diagnostic event contains fields outside the whitelisted schema
- **WHEN** the event is submitted
- **THEN** the unsupported fields are ignored or rejected
- **AND** they are not persisted.

### Requirement: Diagnostic events are short-lived operational data

Diagnostic events SHALL be retained for at most 7 days and SHALL be searchable
by support-oriented correlation fields.

#### Scenario: Admin inspects recent terminal events

- **GIVEN** diagnostic events exist for a seller terminal
- **WHEN** an authorized admin opens the terminal diagnostics view
- **THEN** they can filter recent events by severity, category, request ID, and ticket ID
- **AND** events outside the 7-day retention window are not returned.
