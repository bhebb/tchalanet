## ADDED Requirements

### Requirement: Draw and ticket lifecycle notification policy

The notification capability SHALL apply the lifecycle policy below for draw and ticket events.

Normal draw generation, opening, closing, result application and successful settlement SHALL NOT
create a tenant or platform notification. Normal ticket sale, approval, cancellation, result
calculation and successful payment SHALL NOT create an administrator notification.

Their state SHALL remain visible through the appropriate list, detail, dashboard, receipt or report
surface.

#### Scenario: Normal ticket sale remains silent

- **GIVEN** a ticket sale is approved for a tenant draw
- **WHEN** `TicketPlacedEvent` is processed by projections and integrations
- **THEN** neither tenant administration nor platform supervision receives an in-app or Slack
  notification solely because of the sale
- **AND** the sale remains visible through the sales and reporting surfaces.

#### Scenario: Normal draw settlement remains silent

- **GIVEN** a confirmed result has been applied and all tickets for a tenant draw are terminal
- **WHEN** the draw becomes `SETTLED`
- **THEN** neither tenant administration nor platform supervision receives a notification solely
  because the settlement succeeded
- **AND** the settled state remains visible on the draw detail and reports.

### Requirement: Result lifecycle notification recipients

The system SHALL send result lifecycle notices according to the following audience rules:

- Missing manual results, overdue automatic results and stuck provisional results target platform
  supervision only, as action-required notices.
- A newly available confirmed global result targets the tenant administration audience of every
  affected tenant, as an informational notice.
- A corrected global result targets platform supervision and the tenant administration audience of
  every affected tenant, as a warning.

Tenant administration SHALL include active `TENANT_OWNER` and `TENANT_ADMIN` memberships for the
target tenant. Platform supervision SHALL include active `SUPER_ADMIN` users only.

#### Scenario: Result available informs the affected tenant administration

- **GIVEN** a confirmed global result has a matching non-cancelled tenant draw
- **WHEN** the result becomes available
- **THEN** the tenant owner and tenant administrators receive one tenant-scoped WEB notification
- **AND** the notification says that the result is available
- **AND** it does not say that tickets are settled, paid, or final for payout.

#### Scenario: Missing result does not notify the tenant administration

- **GIVEN** a result is missing after its operational threshold
- **WHEN** an action-required reminder is created
- **THEN** only platform supervision receives the WEB and configured Slack alert
- **AND** no tenant owner or tenant administrator receives that operational incident alert.

### Requirement: Settlement attention is aggregated

When settlement or payout processing remains blocked after the configured retry threshold, the
system SHALL create at most one actionable notification per tenant draw and failure episode.

The tenant notice SHALL be created only when a tenant action is required. Platform supervision
SHALL receive the actionable operational notice.

#### Scenario: Repeated ticket failures do not create notification noise

- **GIVEN** several tickets of one tenant draw cannot complete settlement
- **WHEN** processing reaches the retry threshold
- **THEN** the system creates one correlated settlement-attention notification for that draw
- **AND** it does not create a notification per ticket
- **AND** the payload exposes the draw and aggregate count without ticket selections or personal
  data.
