# Spec: core.analytics

## ADDED Requirements

### Requirement: Analytics owns persistent projections

The system SHALL provide a `core.analytics` domain that owns analytical projections, KPIs, recompute and purge.

#### Scenario: Tenant admin requests dashboard KPIs

- **WHEN** the tenant admin dashboard needs KPI data
- **THEN** the feature SHALL ask `core.analytics` through `QueryBus`
- **AND** the dashboard SHALL NOT query analytics tables directly
- **AND** it SHALL NOT depend on `features.stats`.

#### Scenario: Cashier POS requests session dashboard KPIs

- **WHEN** cashier POS dashboard loads
- **THEN** it SHALL obtain session/ticket/payout attention stats from stable core queries
- **AND** the response SHALL be fast enough for POS/mobile use.

### Requirement: Analytics derives from source-of-truth domains

Analytics projections SHALL be derived from source-of-truth domains.

#### Scenario: Payouts paid metric is computed

- **WHEN** analytics computes `payoutsPaid`
- **THEN** it SHALL use payout payment posted/reversed facts
- **AND** it SHALL NOT treat payable claims as paid payouts.

#### Scenario: Winnings metric is computed

- **WHEN** analytics computes calculated winnings
- **THEN** it SHALL use settlement/resulted ticket facts
- **AND** it SHALL NOT rely on a pre-settlement estimate unless explicitly marked estimated.

### Requirement: Analytics projectors are idempotent

Every analytics event consumer SHALL be idempotent.

#### Scenario: Same event is received twice

- **GIVEN** an analytics projector already processed an event ID for its handler key
- **WHEN** the same event is received again
- **THEN** the projector SHALL skip without applying deltas again.

### Requirement: Analytics supports recompute

Analytics SHALL support recomputation from source data.

#### Scenario: Admin recomputes a date window

- **WHEN** an authorized ops/admin user triggers recompute for a date range
- **THEN** analytics SHALL rebuild the affected projection rows from source-of-truth data
- **AND** it SHALL not rely on the processed-event log.

### Requirement: Analytics supports purge

Analytics SHALL provide configurable purge for processed-event markers and old projections.

#### Scenario: Daily purge runs

- **WHEN** the analytics maintenance scheduler runs
- **THEN** it SHALL execute a command handler
- **AND** delete only data older than configured retention
- **AND** log a summary of deleted rows.

### Requirement: Analytics SQL uses technical atomic primitives only

SQL functions in analytics SHALL be limited to atomic persistence primitives.

#### Scenario: Daily counter increment is needed

- **WHEN** multiple projectors increment the same analytics row concurrently
- **THEN** the system MAY use an SQL function with `INSERT ... ON CONFLICT DO UPDATE`
- **AND** Java projectors SHALL still decide the business meaning of the increments.
