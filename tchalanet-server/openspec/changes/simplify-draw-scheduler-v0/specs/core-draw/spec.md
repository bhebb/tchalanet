## MODIFIED Requirements

### Requirement: Sales-open-time based open today

The system SHALL generate draws at 00:05 and start the daily opening run at 00:15 in the configured
draw scheduler operating timezone, which defaults to `America/Port-au-Prince`.

The daily opening run SHALL process every active tenant and SHALL select only scheduled, unlocked,
non-expired draws in a bounded upcoming horizon. It SHALL NOT depend on a provider-local
`sales_open_time` condition.

#### Scenario: Daily opening covers provider draws with different timezones

- **WHEN** the V0 opening run starts at 00:15 in the operating timezone
- **THEN** an eligible upcoming draw is selected by its resolved `scheduledAt` and `cutoffAt`
- **AND** the provider timezone does not delay its opening.

### Requirement: Safe daily opening

The system SHALL preserve the current provider-calendar cancellation check before opening a draw.
It SHALL bulk-open only still-scheduled, unlocked draws and SHALL not reopen a draw that has already
been manually closed, cancelled, resulted, or settled.

#### Scenario: Re-running the opening job

- **WHEN** the same opening batch is run again
- **THEN** already-open draws are not selected
- **AND** no duplicate lifecycle transition occurs.

### Requirement: No inactive scheduler windows

The system SHALL NOT expose a draw scheduler windows setting or instantiate an unused scheduler
window evaluator. The processing cadence and bounded candidate queries are the effective runtime
policy.

#### Scenario: Runtime starts without scheduler windows

- **WHEN** the draw runtime configuration is bound at application startup
- **THEN** no scheduler windows property or evaluator is required
- **AND** the daily lifecycle and processing schedulers remain independently configurable.
