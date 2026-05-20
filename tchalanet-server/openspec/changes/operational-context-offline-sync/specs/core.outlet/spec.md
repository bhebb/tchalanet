# core.outlet spec delta

## ADDED Requirements

### Requirement: Outlet aggregate exposes operational flags

The `Outlet` domain record SHALL carry `status`, `dayClosed`, `salesBlocked`, `payoutBlocked`, `offlineSalesBlocked` and their audit fields (`*Reason`, `*At`, `*By`).

#### Scenario: Outlet record matches snapshot

- **GIVEN** an outlet persisted with payout and offline-sales blocks
- **WHEN** the aggregate is loaded
- **THEN** the domain record SHALL expose `payoutBlocked` and `offlineSalesBlocked` directly
- **AND** validators SHALL NOT need to read `OutletOperationSnapshot` separately

### Requirement: Outlet operation rules

`ValidateOutletForOperationQuery` SHALL enforce operation-specific outlet rules.

#### Scenario: SELL gating

- **GIVEN** an outlet
- **WHEN** the query is asked for `SELL`
- **THEN** it SHALL accept only when `status == ACTIVE`, `!dayClosed`, `!salesBlocked`

#### Scenario: PAYOUT gating

- **WHEN** the query is asked for `PAYOUT`
- **THEN** it SHALL accept only when `status == ACTIVE`, `!dayClosed`, `!payoutBlocked`

#### Scenario: OFFLINE_GRANT gating

- **WHEN** the query is asked for `OFFLINE_GRANT`
- **THEN** it SHALL accept only when `status == ACTIVE`, `!dayClosed`, `!salesBlocked`, `!offlineSalesBlocked`

#### Scenario: OFFLINE_SYNC accepts blocked outlets for audit

- **GIVEN** an outlet that is `dayClosed` or `salesBlocked` but not `ARCHIVED`
- **WHEN** the query is asked for `OFFLINE_SYNC`
- **THEN** it SHALL accept the technical reception for audit purposes
- **AND** any business decision SHALL be deferred to `core.sales`
