# Design — Analytics Dashboard Reporting Projections V1

## Responsibility

`core.analytics` owns derived read models. It consumes public domain events after commit and exposes
query contracts through `core.analytics.api`.

`core.sales` owns ticket and settlement lifecycle truth. Because payout state now lives on the
ticket lifecycle, sales must publish public events when payout state changes.

## Money Semantics

| Metric | Meaning | Source event |
|---|---|---|
| `winningsCalculated` | Winning amount calculated when official result is applied | `TicketWinningSettlementCreatedEvent` |
| `payoutsPaid` | Amount actually marked paid to customer | `TicketPayoutPaidEvent` |
| seller commission | Seller terminal commission snapshot taken at sale time | `TicketPlacedEvent` context snapshot |
| customer/operator fees | Ticket charge snapshots grouped by payer | `TicketPlacedEvent.money.charges` |
| payout reversal V1 | A paid payout was reversed | `TicketPayoutReversedEvent` as negative paid delta |
| `netRevenueEstimated` | Gross sales minus calculated winnings | analytics row derived field |
| `netRevenuePaidBasis` | Gross sales minus paid payouts | analytics row derived field |

V1 uses existing `analytics_daily.payouts_paid_cents` for paid/reversed net paid basis. A future
schema change may add `payouts_reversed_cents` if reporting needs explicit reversal totals.

## Financial Snapshot Boundaries

`core.pricing` owns odds resolution. A sale must ask pricing for the effective odds in this order:
seller-terminal override, then tenant default. `core.sales` snapshots only the effective odds on
each `TicketLine`; result settlement calculates winnings from that snapshot and must not reread
current pricing. This keeps old tickets stable when a tenant later changes odds from, for example,
50-20-10 to 60-20-10.

Commissions follow the same rule. The seller terminal commission rate and amount are captured in
`TicketContext` when the ticket is sold. Tenant dashboard/reporting totals should project those
snapshots by day, draw and seller terminal. They should not recompute commissions from current
tenant defaults or seller-terminal overrides.

Fees are ticket charge snapshots. Reporting must group them by `TicketChargeType` and `ChargePaidBy`
so tenant-paid SMS fees, customer-paid SMS fees and waived/promotion-funded charges can be shown
without changing sales history.

V1 projects charge totals by payer:

- `buyerCharge`: non-waived charges paid by the customer; buyer-facing pass-through, not game stake.
- `sellerCharge`: non-waived charges absorbed by the seller terminal; visible for seller net
  commission reporting.
- `tenantCharge`: non-waived charges absorbed by the tenant; subtracted from tenant net revenue.
- `waivedCharge`: original amount of waived charges; visible as promotion/discount cost signal and
  not included in the buyer-facing ticket total.

Seller commission remains percentage-configured but amount-projected. The sale snapshots the
effective seller terminal commission rate and amount. Analytics sums the amount; the percentage is
for audit/explanation, not for recomputing historical stats.

Promotions remain line/charge-level facts. Free or fixed-payout promotions, generated selections
and odds boosts should be represented through promotion line snapshots and ticket charge snapshots,
then aggregated by analytics rather than handled as a separate dashboard-specific flow.

## Query Reuse

Dashboard-specific queries stay acceptable when the result is a cohesive dashboard view. Reporting
queries should reuse the same projections and may expose a more generic breakdown query later:

```java
GetSalesBreakdownQuery(tenantId?, from, to, dimension, limit)
```

Do not put SQL aggregation in `features.reporting` or `features.pagemodel`.

## Game breakdown source

V1 reads game breakdowns from `analytics_selection`, because it already stores stable
`game_code` and line-level stakes. This avoids overloading `analytics_daily.dimension_id`, which is
UUID-shaped and better suited for tenant/outlet/seller style dimensions.
