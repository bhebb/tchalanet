# core-analytics Spec

## MODIFIED Requirements

### Requirement: Dashboard analytics views are reusable by dashboards and reports

`core.analytics.api` SHALL expose dashboard views that can be consumed by PageModel dashboard
providers and reporting features without direct SQL access from features.

#### Scenario: Platform dashboard requests analytics

- **WHEN** `GetPlatformDashboardStatsQuery` is handled for a date window
- **THEN** the returned view includes summary totals, daily trend points, game breakdown contract,
  and top tenant ranking
- **AND** consumers can use the same view for dashboard widgets and report summaries.

#### Scenario: Tenant dashboard requests analytics

- **WHEN** `GetTenantDashboardStatsQuery` is handled for a date window
- **THEN** the returned daily points include gross sales and estimated net revenue
- **AND** the view includes a game breakdown sourced from analytics projections.

### Requirement: Analytics distinguishes calculated winnings from paid payouts

`core.analytics` SHALL distinguish money calculated by result settlement from money actually paid to
customers.

#### Scenario: Winning settlement is created

- **WHEN** `TicketWinningSettlementCreatedEvent` is consumed
- **THEN** analytics increments `winningsCalculated`
- **AND** analytics does not increment `payoutsPaid`.

#### Scenario: Winning ticket is marked paid

- **WHEN** `TicketPayoutPaidEvent` is consumed
- **THEN** analytics increments `payoutsPaid`.

#### Scenario: Paid payout is reversed

- **WHEN** `TicketPayoutReversedEvent` is consumed
- **THEN** analytics reverses the V1 paid payout total.

### Requirement: Analytics derives financial dashboard metrics from sale snapshots

`core.analytics` SHALL derive tenant-admin and seller-terminal financial metrics from ticket sale
snapshots and ticket lifecycle events, not from current configuration tables.

#### Scenario: Tenant admin requests commission totals

- **WHEN** tenant dashboard or reporting queries request commission totals for a day or draw
- **THEN** analytics uses the seller commission amount snapshot captured on approved ticket sales
- **AND** analytics can break the total down by seller terminal.

#### Scenario: Seller terminal requests its commission

- **WHEN** a seller-terminal surface requests its commission for a date range
- **THEN** analytics returns totals from that seller terminal's projected sale snapshots
- **AND** the value does not change when tenant or seller-terminal commission settings change later.

#### Scenario: Tenant admin requests net business performance

- **WHEN** tenant dashboard or reporting queries request net performance by day or draw
- **THEN** analytics can expose gross sales, calculated winnings, paid payouts, seller commissions,
  charge totals by payer, and promotion-funded amounts as separate facts
- **AND** derived net values subtract the relevant payout, commission and tenant-paid/operator-paid
  costs without hiding the source components.

#### Scenario: Tenant admin opens financial drilldowns

- **WHEN** tenant admin requests financial breakdowns for a date window
- **THEN** analytics returns summary totals, daily rows, draw rows, seller-terminal daily rows, and
  seller-terminal-by-draw rows
- **AND** each row keeps seller commissions, buyer/seller/tenant/waived charges, promotion exposure,
  and net revenue fields separated
- **AND** features consume the view through `core.analytics.api` instead of reading analytics tables.

#### Scenario: Tenant admin needs seller-terminal by draw commission

- **WHEN** tenant admin needs exact commission rows for a seller terminal within a specific draw
- **THEN** analytics uses the dedicated `analytics_seller_terminal_draw` projection
- **AND** dashboard code must not infer that exact cross-axis value by combining daily terminal rows
  with draw rows.

#### Scenario: Ticket sale includes communication charges

- **WHEN** an approved ticket sale includes non-waived charges paid by buyer, seller, or tenant
- **THEN** analytics projects those amounts into separate buyer, seller and tenant charge totals
- **AND** buyer-paid charges remain separate from game gross sales
- **AND** tenant-paid charges are subtracted from tenant net revenue.

#### Scenario: Ticket sale includes waived charges

- **WHEN** a ticket charge is waived by a promotion rule
- **THEN** analytics projects the original charge amount into waived charge totals
- **AND** the waived amount is not treated as buyer-paid revenue.

#### Scenario: Draw financial projection receives lifecycle events

- **WHEN** an approved ticket is placed for a draw
- **THEN** analytics increments draw-level gross sales, stake total and seller commission from the
  sale snapshot
- **AND** draw-level estimated net revenue subtracts seller commission immediately.
- **WHEN** winning settlement and payout events are published for that draw
- **THEN** analytics increments draw-level calculated winnings and paid payouts
- **AND** draw-level net revenue fields are adjusted from those event deltas.

#### Scenario: Promotions generate free or fixed-payout play

- **WHEN** a sale includes free, generated-selection, odds-boost or fixed-payout promotion lines
- **THEN** analytics aggregates those line and charge snapshots by promotion metadata
- **AND** dashboard/reporting consumers can show promotional cost and promotional sales impact
  without a separate promotion-specific calculation path.

#### Scenario: Promotion exposure is projected

- **WHEN** an approved ticket sale includes promotion-created lines
- **THEN** analytics increments promotion line count, promotion payout base and promotion potential
  payout from the sale-time line snapshots
- **AND** these exposure metrics are not subtracted from net revenue directly.

#### Scenario: Promotion modifies pricing on a customer line

- **WHEN** an approved ticket sale includes a customer line with promotion pricing source
- **THEN** analytics increments promotion-priced line count
- **AND** winnings and payouts are still derived later from ticket settlement lifecycle events.
