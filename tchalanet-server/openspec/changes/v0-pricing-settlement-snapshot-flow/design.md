# Design

## Verified Current State

### Pricing

`core.pricing` is odds-only today:

- `TenantPricingOdds` and `SellerTerminalOddsOverride` persist `odds`.
- `SellerTerminalOddsResolutionView` returns `tenantDefaultOdds`, `sellerTerminalOdds`,
  `effectiveOdds`, and `OddsSource`.
- `DOMAIN_PRICING.md` says sales snapshots only effective odds and settlement never rereads current
  odds.

This supports `Lot 1 x50`, but not a first-class fixed payout such as "pay 2000 Gdes" independent
of stake/base.

### Sales Preparation

`TicketLinePreparationService` currently:

- plans one or more `TicketLineCoverage` entries;
- resolves odds for each coverage;
- calculates `coverageStake * odds`;
- stores summary fields: line odds, line settlement, min/max/total settlement.

This violates issue #255 because sale-time code still computes payout amounts before official
results.

### Promotion

`PromotionTicketLineFactory` currently:

- creates `FREE_GAME_LINE` ticket lines;
- for `HT_MARYAJ_GRATIS`, picks bet option `2`;
- resolves odds;
- calculates `payoutBase * odds`.

Issue #255 says Maryaj gratis is a promotion that generates a free Maryaj line, and its exact/permuted
fixed amounts are promotion configuration, not the normal paid Maryaj odds.

### Settlement

`TicketWinningCalculator` already has the right settlement posture:

- it reads ticket line coverages;
- it checks winning facts from the official draw result projection;
- it chooses the best alternative or adds cumulative payouts;
- it returns realized `TicketLineResult` amounts.

The target design should preserve that boundary while changing what is snapshotted.

## Target Model

### Payout Rule

Introduce typed payout rules in pricing/settlement contracts:

```java
enum PayoutRuleType {
  STAKE_MULTIPLIER,
  FIXED_AMOUNT
}

record PayoutRuleSnapshot(
  PayoutRuleType type,
  BigDecimal multiplier,
  BigDecimal fixedAmount
) {}
```

Rules:

- `STAKE_MULTIPLIER`: settlement amount is line stake or configured payout base multiplied by
  `multiplier`.
- `FIXED_AMOUNT`: settlement amount is `fixedAmount`, regardless of stake.
- Currency stays on the ticket parent.
- Snapshots contain effective values, not only ids.

### Settlement Terms Snapshot

Add a typed immutable JSONB snapshot to `sales_ticket_line`, for example:

```java
record SettlementTermsSnapshot(
  int schemaVersion,
  SelectionPolicy selectionPolicy,
  List<SettlementTermSnapshot> terms
) {}

record SettlementTermSnapshot(
  PricingVariantCode ruleCode,
  Short sourceBetOption,
  PayoutRuleSnapshot payoutRule,
  WinMode winMode,
  SettlementTermSource source,
  UUID sourceRuleId
) {}
```

The JSONB replaces the authoritative role of `TicketLineCoverageJpaEntity`. Migration can keep
legacy columns temporarily, but new settlement should read the typed snapshot.

### Pricing Resolution

Replace odds-only resolution with payout-rule resolution while keeping adapter compatibility during
the migration:

```text
SellerTerminal + selection
Tenant + selection
SellerTerminal general
Tenant general
```

Seller terminal overrides may be partial but must not arbitrarily change the payout rule type in V0
unless a product decision explicitly allows it.

### Sale Preparation

Create or evolve a planner:

```text
TicketLineSettlementTermsPlanner
```

Responsibilities:

- inspect `SelectionPolicy`;
- resolve commercial/technical options;
- resolve payout rules from pricing and promotion configuration;
- deduplicate terms;
- produce `SettlementTermsSnapshot`;
- avoid calculating final money amounts except copying configured fixed values or multipliers into
  the snapshot.

`IMPLICIT_BEST_MATCH`:

- `betOption` remains null;
- all enabled applicable technical rules are snapshotted;
- stake is not divided across alternatives.

`EXPLICIT_ONLY`:

- `betOption` is required;
- only selected commercial option terms are snapshotted;
- commercial option label is snapshotted for print/reprint.

### Maryaj Gratis

The issue and current code disagree on naming:

- Issue #255 target: line is `gameCode=HT_MARYAJ`, `origin=PROMOTION`,
  `promotionCode=MARYAJ_GRATIS`, `stakeAmount=0`.
- Current code: promotion creates a line with `gameCode=HT_MARYAJ_GRATIS`, `origin=PROMOTION`.

Implementation must choose one path before coding. Recommended target: align to issue #255 and make
Maryaj gratis a promotion flavor of Maryaj, not a separate game code. If the separate game code is
kept for compatibility, it must be treated as a wrapper over Maryaj terms and not as a separate
pricing concept.

Maryaj gratis terms:

- eligibility and quantity come from promotion config;
- selection generation/manual choice comes from promotion config;
- payout terms come from effective promotion/game configuration;
- line stake is zero;
- terms are snapshotted on the generated line.

### Settlement

Settlement reads `SettlementTermsSnapshot` only:

- evaluate each term against official draw facts;
- for `STAKE_MULTIPLIER`, compute realized amount using the snapshotted multiplier;
- for `FIXED_AMOUNT`, use the snapshotted amount;
- for `ALTERNATIVE`, keep the best winning term;
- for `CUMULATIVE`, sum only cumulative winning terms;
- persist `ticket_line.payout_amount`, line result, and `sales_ticket.winning_amount`;
- publish settlement/payout analytics events once.

### Print/Reprint

Print and reprint remain customer-safe:

- never show potential gain;
- never show odds/multipliers;
- hide option in implicit mode;
- show snapshot commercial label in explicit mode;
- render Maryaj as `12 x 34`/`12 × 34` depending receipt formatter support;
- render Maryaj gratis with promotion markers (`*`, `GRATIS`) from sale snapshot.

### Reporting

Before result:

- sales totals;
- stakes;
- ticket/line counts;
- promotion line counts;
- pending counts.

After result:

- realized payouts from `ticket_line.payout_amount`;
- ticket `winning_amount`;
- winner/loser counts;
- realized margin.

No potential payout KPI in V0.

## Migration Strategy

1. Add new payout rule and settlement snapshot model/contracts.
2. Add JSONB column to `sales_ticket_line`.
3. Write new sale-time snapshots while keeping legacy fields populated if needed for compatibility.
4. Move settlement to the JSONB snapshot.
5. Move print/reprint to snapshot data where needed.
6. Move reports/stats to realized persisted values.
7. Backfill or guard legacy tickets.
8. Remove/rename legacy potential/odds/coverage columns only after readers no longer depend on them.

## Risks

- Mixing fixed and multiplier rules incorrectly can create money loss.
- Existing tickets may lack the new snapshot; settlement needs an explicit legacy strategy.
- Maryaj gratis naming (`HT_MARYAJ_GRATIS` vs `HT_MARYAJ` + promotion code) affects print, reports,
  and migration.
- Selection-specific overrides increase lookup complexity and cache invalidation needs.
- Reporting must not parse JSON for historical aggregates.

