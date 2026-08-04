## ADDED Requirements

### Requirement: V0 sales have no approval workflow

The V0 sale path SHALL either create a directly `APPROVED` ticket or reject the request. A limit
policy result of `REQUIRE_APPROVAL` SHALL reject the request without creating a ticket.

#### Scenario: Limit policy requires approval

- **GIVEN** a sale reaches a limit policy configured with `REQUIRE_APPROVAL`
- **WHEN** the cashier prepares or confirms the sale
- **THEN** the API SHALL return the stable limit-blocked business error
- **AND** no ticket SHALL be persisted.

### Requirement: Seller live statistics count official tickets only

Seller-terminal daily statistics SHALL count only tickets whose sale status is `APPROVED`.

#### Scenario: A rejected sale has no official ticket

- **GIVEN** a seller request is rejected by the sale policy within the current business day
- **WHEN** the POS home loads seller daily statistics
- **THEN** no ticket SHALL be included in sales, ticket count, commission, or per-draw totals for that request.

### Requirement: Seller-terminal KPI consumers share analytics truth

POS, tenant-admin dashboards, seller-terminal summaries, and reporting surfaces SHALL obtain
KPI/reportable metrics from `core.analytics` projections. The seller-terminal identity used to
look up a seller projection SHALL be `SellerTerminalId`; a user identity SHALL NOT be substituted
for that dimension.

#### Scenario: POS and report read the same seller-terminal day

- **GIVEN** an `analytics_daily` seller-terminal row and its matching
  `analytics_seller_terminal_draw` rows exist for a tenant business day
- **WHEN** the POS daily statistics and tenant seller report request that terminal and date
- **THEN** both consumers SHALL expose the same ticket count, gross sales, and seller commission
- **AND** the POS reader SHALL NOT aggregate `sales_ticket` directly for those KPIs.

### Requirement: Unverifiable financial metrics are unavailable

The analytics domain SHALL expose a trust state for each requested reporting scope. A consumer
MUST NOT render financial KPI values as zero when the relevant scope is unavailable or awaiting
reconciliation.

#### Scenario: Reconciliation detects orphaned projections

- **GIVEN** a reporting scope has analytics projection rows that cannot be reconciled with the
  transactional `APPROVED` ticket source
- **WHEN** a POS or reporting BFF requests that scope
- **THEN** the response SHALL identify the metric section as unavailable
- **AND** SHALL include a stable degradation notice
- **AND** SHALL NOT present the projected monetary values as trustworthy KPI data.

#### Scenario: Reporting suppresses exports for an unavailable scope

- **GIVEN** a tenant-admin reporting response identifies its requested scope as unavailable
- **WHEN** the web client renders that response
- **THEN** it SHALL render the localized unavailable state instead of KPI values or report rows
- **AND** SHALL disable CSV and PDF exports until a refresh returns a trustworthy scope.

#### Scenario: Projection coverage is missing before reconciliation exists

- **GIVEN** a requested tenant, seller-terminal, draw, or platform business-date scope has no
  analytics projection row for one of its requested dates
- **WHEN** the trust-state query evaluates that scope
- **THEN** it SHALL return `UNAVAILABLE` with the missing business dates
- **AND** it SHALL NOT treat the missing rows as zero-valued metrics.

#### Scenario: Draw scope is a single business occurrence

- **GIVEN** an analytics trust query targets a draw or seller-terminal/draw scope
- **WHEN** the query is constructed
- **THEN** it SHALL require exactly one business date for that draw occurrence.

### Requirement: Analytics repair is explicit and auditable

An operator-authorized repair SHALL rebuild projections only from the transactional source and
shall record the selected scope, reason, initiator, before/after reconciliation result, and
watermark.

#### Scenario: Operator repairs a mismatched business day

- **GIVEN** a platform operator has identified a mismatched tenant business day
- **WHEN** the operator launches a recompute for that tenant and date
- **THEN** the system SHALL rebuild daily, draw, and seller-terminal projections from official
  tickets in the selected scope
- **AND** SHALL retain an auditable repair record
- **AND** SHALL mark the scope ready only after the post-repair reconciliation succeeds.

### Requirement: Reconciliation distinguishes validation from repair

The analytics domain SHALL support a read-only `VALIDATE` mode and an explicit
`REBUILD_AND_VALIDATE` mode. A projection row existing for a requested date SHALL NOT by itself
produce a trustworthy state.

#### Scenario: Deleted analytics row is repaired

- **GIVEN** an official tenant ticket exists but its matching analytics projection row was deleted
- **WHEN** an operator executes `VALIDATE` for the tenant scope
- **THEN** the result SHALL report `MISMATCH`
- **AND** the affected reporting scope SHALL be unavailable.
- **WHEN** the operator executes `REBUILD_AND_VALIDATE` for the same scope
- **THEN** the system SHALL rebuild only that selected tenant scope from immutable source snapshots
- **AND** a second comparison SHALL be executed
- **AND** the operation SHALL report `SUCCESS` only when expected and observed metrics match
  exactly.

### Requirement: Reconciliation uses immutable financial truth

Expected metrics SHALL be derived from ticket, line, charge, result, settlement and seller
commission snapshots. It SHALL NOT calculate historical totals from current pricing, commission,
promotion or settlement configuration.

#### Scenario: Seller override remains historically correct

- **GIVEN** a ticket was sold with a seller-terminal commission or price override
- **WHEN** reconciliation calculates expected seller commission and stake
- **THEN** it SHALL use the ticket snapshots persisted at sale time
- **AND** changing the current seller configuration SHALL NOT change the reconciliation result.

### Requirement: V1 result application establishes the effective paid amount

V1 SHALL treat a ticket resolved as winning by a confirmed draw result as immediately paid. The
ticket SHALL persist both its calculated winning amount and its effective paid amount. The
calculated winning amount SHALL equal the sum of winning `ticket_line` outcomes and SHALL change
only when the draw result is applied or corrected. The initial effective paid amount SHALL equal
the calculated winning amount. A losing ticket SHALL have an effective paid amount of zero.
`ticket_line` SHALL remain the source of line selections and calculated game outcomes; it SHALL
NOT store payment corrections.

#### Scenario: Result application settles a winning ticket

- **GIVEN** an approved ticket is eligible for a confirmed draw result
- **WHEN** the result processor resolves the ticket as winning
- **THEN** the ticket SHALL persist its calculated winning amount, effective paid amount and
  settlement timestamp in the same result-application lifecycle
- **AND** the effective paid amount SHALL equal the calculated winning amount.

#### Scenario: Payment correction does not rewrite calculated line results

- **GIVEN** a settled winning ticket whose calculated winning amount equals its line outcomes
- **WHEN** an authorized operator corrects the effective paid amount
- **THEN** the calculated winning amount and every ticket-line outcome SHALL remain unchanged
- **AND** the ticket SHALL expose the calculated amount and the corrected effective paid amount as
  separate values
- **AND** the correction metadata SHALL explain why the values differ.

#### Scenario: Authorized correction changes the effective paid amount

- **GIVEN** a settled winning ticket has an effective paid amount
- **WHEN** an authorized operator corrects that amount with a reason
- **THEN** the service SHALL read the previous amount from the ticket
- **AND** SHALL persist the replacement paid amount, correction timestamp, actor and reason
- **AND** SHALL update analytics by the exact server-derived delta after commit
- **AND** reconciliation SHALL derive the repaired paid metric from the persisted ticket amount.

### Requirement: Result processing alerts before an unresolved draw is settled

The draw result processor SHALL settle a draw only after all eligible tickets have a resolved
winning or losing outcome. If pending or failed ticket processing remains after the configured
attention delay, the system SHALL send one deduplicated Web and Slack notification to platform
operators. This operational alert is distinct from analytics reconciliation.

#### Scenario: Ticket result processing remains incomplete

- **GIVEN** a confirmed draw result leaves eligible tickets pending or failed
- **WHEN** the configured attention delay has elapsed
- **THEN** the draw SHALL remain unsettled
- **AND** the notification SHALL identify the draw, pending ticket count and failed ticket count.
