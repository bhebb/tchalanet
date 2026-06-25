# Domain — Core Pricing

`core.pricing` owns tenant-scoped runtime pricing rules that are not pure catalog defaults.

## Responsibility

- Store seller-terminal odds overrides.
- Resolve effective odds for a seller-terminal sale.
- Expose query/command contracts through `core.pricing.api`.

`catalog.pricing` remains the tenant default odds catalog. `core.pricing` composes those defaults
with seller-terminal overrides.

## Effective odds resolution

The only supported order for a seller-terminal sale is:

```text
seller-terminal active override -> tenant default pricing catalog -> error
```

API:

- `ResolveSellerTerminalOddsQuery`
- `SellerTerminalOddsResolutionView`
- `OddsSource.SELLER_TERMINAL_OVERRIDE`
- `OddsSource.TENANT_DEFAULT`

`core.sales` must call `ResolveSellerTerminalOddsQuery` when preparing ticket lines for a seller
terminal. This includes customer-paid lines and promotion-generated free game lines. It must then
snapshot only the effective odds on `TicketLine.oddsSnapshot`.

## Non-retroactivity

Odds changes are not retroactive:

- changing tenant default odds affects only future sales;
- changing seller-terminal override odds affects only future sales;
- result settlement and payout calculation use ticket line snapshots and never reread current odds.

## Boundaries

`core.pricing` does not:

- calculate winnings;
- calculate commissions;
- calculate ticket totals;
- apply promotions;
- publish financial analytics.

Those responsibilities remain in `core.sales`, `core.promotion` and `core.analytics`.
