# Domain — Core Pricing

`core.pricing` owns tenant-scoped runtime pricing rules.

## Responsibility

- Own tenant default payout rules as runtime pricing configuration.
- Store seller-terminal payout overrides.
- Resolve effective payout rules for a seller-terminal sale.
- Expose query/command contracts through `core.pricing.api`.

There is no platform pricing reference catalog in V0. Tenant default pricing is persisted runtime
configuration, and seller-terminal overrides resolve on top of it.

## Effective payout resolution

The only supported order for a seller-terminal sale is:

```text
seller-terminal active override -> tenant default rule -> error
```

Resolution is per pricing variant. A seller-terminal override for `MARRIAGE_EXACT_ORDER` does not
affect `MARRIAGE_REVERSE_ALLOWED`; the sibling variant falls back to its tenant default rule.
Overrides cannot be created without the matching tenant default rule.

API:

- `ListTenantPricingRulesQuery`
- `TenantPricingRuleView`
- `UpsertTenantPricingRuleCommand`
- `ListSellerTerminalPricingRuleOverridesQuery`
- `SellerTerminalPricingRuleOverrideView`
- `UpsertSellerTerminalPricingRuleOverrideCommand`
- `ResolveSellerTerminalPayoutRuleQuery`
- `SellerTerminalPayoutRuleResolutionView`
- `PayoutRuleType.STAKE_MULTIPLIER`
- `PayoutRuleType.FIXED_AMOUNT`
- `ResolveSellerTerminalOddsQuery`
- `SellerTerminalOddsResolutionView`
- `PricingVariantCode`
- `OddsSource.SELLER_TERMINAL_OVERRIDE`
- `OddsSource.TENANT_DEFAULT`

`core.sales` must resolve a `PricingVariantCode` from the commercial line and selection, then call
`ResolveSellerTerminalPayoutRuleQuery` when preparing ticket lines for a seller terminal. This
includes customer-paid lines and promotion-generated free game lines. It must snapshot the effective
rule in `TicketLine.settlementTermsSnapshot`.

`ResolveSellerTerminalOddsQuery` remains as a compatibility contract for legacy odds callers. For
new settlement flow, odds are only the `STAKE_MULTIPLIER` value; `FIXED_AMOUNT` rules settle from
their fixed amount snapshot.

Admin HTTP uses `/admin/controls/pricing-rules`. The old `/admin/controls/odds` surface is not kept
for V0 because fixed amounts and multipliers are both pricing rules.

V0 rule types are game-bound:

- `HT_MARYAJ_GRATIS` requires `PayoutRuleType.FIXED_AMOUNT`.
- All other lottery games require `PayoutRuleType.STAKE_MULTIPLIER`.
- The same rule applies to tenant defaults and seller-terminal overrides.
- Seller-terminal overrides cannot change the tenant default rule type for the same variant.

## Non-retroactivity

Pricing changes are not retroactive:

- changing tenant default pricing affects only future sales;
- changing seller-terminal override pricing affects only future sales;
- result settlement and payout calculation use ticket line snapshots and never reread current pricing.

## Boundaries

`core.pricing` does not:

- calculate winnings;
- calculate commissions;
- calculate ticket totals;
- apply promotions;
- publish financial analytics.

Those responsibilities remain in `core.sales`, `core.promotion` and `core.analytics`.
