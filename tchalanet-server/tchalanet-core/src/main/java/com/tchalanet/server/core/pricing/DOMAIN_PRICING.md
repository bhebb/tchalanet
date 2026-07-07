# Domain — Core Pricing

`core.pricing` owns tenant-scoped runtime pricing rules.

## Responsibility

- Own tenant default odds as runtime pricing configuration.
- Store seller-terminal odds overrides.
- Resolve effective odds for a seller-terminal sale.
- Expose query/command contracts through `core.pricing.api`.

There is no platform pricing reference catalog in V0. Tenant default odds are persisted runtime
configuration, and seller-terminal overrides resolve on top of them.

## Effective odds resolution

The only supported order for a seller-terminal sale is:

```text
seller-terminal active override -> tenant default odds -> error
```

API:

- `ResolveSellerTerminalOddsQuery`
- `SellerTerminalOddsResolutionView`
- `PricingVariantCode`
- `OddsSource.SELLER_TERMINAL_OVERRIDE`
- `OddsSource.TENANT_DEFAULT`

`core.sales` must resolve a `PricingVariantCode` from the commercial line and selection, then call
`ResolveSellerTerminalOddsQuery` when preparing ticket lines for a seller terminal. This includes
customer-paid lines and promotion-generated free game lines. It must then snapshot only the
effective odds on `TicketLine.oddsSnapshot`.

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
