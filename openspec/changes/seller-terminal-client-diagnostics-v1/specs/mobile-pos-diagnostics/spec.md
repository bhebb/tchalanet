# mobile-pos-diagnostics Delta

## ADDED Requirements

### Requirement: Mobile POS honors terminal diagnostics policy

The Mobile POS SHALL send client diagnostic events only while the current seller
terminal has an active diagnostics policy.

#### Scenario: Diagnostics disabled

- **GIVEN** the POS profile has no active diagnostics policy
- **WHEN** an API, connectivity, sale, print, scanner, printer config, Flutter, or async error occurs
- **THEN** the app does not send a diagnostic event
- **AND** normal user-facing error handling continues.

#### Scenario: Diagnostics active

- **GIVEN** the POS profile includes an active diagnostics policy
- **WHEN** an API, connectivity, sale, print, scanner, printer config, Flutter, or async error occurs
- **THEN** the app queues a whitelisted diagnostic event with category, operation, error code when available, request ID when available, and device/app context.

### Requirement: Mobile diagnostics never block POS workflows

Diagnostic collection SHALL be best-effort and SHALL NOT block sale, print, scan,
or navigation workflows.

#### Scenario: Diagnostic endpoint unavailable

- **GIVEN** diagnostics are active
- **AND** the diagnostic endpoint is unavailable
- **WHEN** a client error is captured
- **THEN** the app stores at most a bounded retry queue
- **AND** the original POS workflow continues without waiting for diagnostics.

#### Scenario: Diagnostics expire with queued events

- **GIVEN** diagnostics were active and events are queued
- **WHEN** the diagnostics `expiresAt` time passes
- **THEN** the app stops sending diagnostics
- **AND** queued events are destroyed.

### Requirement: Mobile diagnostics are batched

The Mobile POS SHALL submit diagnostic events in bounded batches instead of one
HTTP request per error.

#### Scenario: Multiple events queued

- **GIVEN** diagnostics are active
- **AND** multiple client errors are queued
- **WHEN** the reporter flushes diagnostics
- **THEN** it submits a batch of 5-20 events when available
- **AND** it drops oldest events first if the local queue is full.

### Requirement: Mobile print failures are diagnosable

The Mobile POS SHALL emit normalized print diagnostic events when diagnostics are
active.

#### Scenario: Server rejects a print as reprint without reason

- **GIVEN** diagnostics are active
- **WHEN** the print API returns `ticket.reprint.reason_required`
- **THEN** the app queues a diagnostic event with category `PRINT`
- **AND** the event includes operation, error code, request ID, printer provider, printer service, printer state, and paper size
- **AND** the event does not include ticket PDF bytes.

#### Scenario: Direct printer adapter unavailable

- **GIVEN** diagnostics are active
- **WHEN** POS direct printing has no available adapter
- **THEN** the app queues a diagnostic event with category `PRINT`
- **AND** the event identifies the configured printer mode and available adapter state.

### Requirement: Mobile global errors are redacted

The Mobile POS SHALL capture Flutter framework and uncaught async errors while
diagnostics are active, with sanitized stack traces.

#### Scenario: Flutter error captured

- **GIVEN** diagnostics are active
- **WHEN** a Flutter framework error is caught
- **THEN** the app queues a diagnostic event with category `FLUTTER`
- **AND** the stack is represented as bounded sanitized `stackFrames[]`.
