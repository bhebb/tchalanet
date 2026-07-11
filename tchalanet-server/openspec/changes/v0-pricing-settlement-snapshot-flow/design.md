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

`TicketLinePreparationService` previously calculated potential payouts before results.

This violated issue #255 because sale-time code computed payout amounts before official results.

### Promotion

`PromotionTicketLineFactory` currently:

- creates `FREE_GAME_LINE` ticket lines;
- for `HT_MARYAJ_GRATIS`, picks bet option `2`;
- resolves odds;
- calculates `payoutBase * odds`.

Maryaj gratis is currently modeled as `HT_MARYAJ_GRATIS`, which is the V0 target. The bug is not the
game code; the bug is that the promotion factory multiplies a configured amount by odds instead of
letting pricing resolve fixed settlement terms for that game.

Promotion does not currently support SellerTerminal overrides:

- `PromotionEvaluationContext` has tenant/user/agent/zone fields, but no `SellerTerminalId`.
- `SalePreparationOrchestrator.evaluatePromotion(...)` does not pass the seller terminal into
  `EvaluatePromotionQuery`.
- `promotion_rule`, `promotion_rule_effect`, and related entities have no terminal override table.
- The only terminal-level override in this flow today is pricing odds resolution inside sales
  (`ResolveSellerTerminalOddsQuery`) when constructing paid or free ticket lines.

Therefore the issue #255 requirement for partial SellerTerminal promotion overrides is new backend
capability, not a wiring fix.

Implemented target: Promotion resolves SellerTerminal effect overrides as partial overrides over
tenant campaign defaults. The override row targets one campaign/rule/effect/game tuple and may
disable the effect or override quantity, choice mode, generation strategy, and regeneration limits.
It does not copy campaign rules or eligibility. The effective promotion decision hash includes the
active terminal override hash so re-evaluation can distinguish tenant default config from
terminal-specific config.

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
- V0 rule type is constrained by game:
  - `HT_BOLET`, `HT_MARYAJ`, `HT_LOTO3`, `HT_LOTO4`, and `HT_LOTO5` use
    `STAKE_MULTIPLIER`.
  - `HT_MARYAJ_GRATIS` uses `FIXED_AMOUNT`.
- SellerTerminal overrides must not change the rule type in V0. For example, a tenant
  `FIXED_AMOUNT` term cannot be overridden as `STAKE_MULTIPLIER`; only the amount value may change.

### Settlement Terms Snapshot

Add a typed immutable JSONB snapshot to `sales_ticket_line`, for example:

```java
record SettlementTermsSnapshot(
  int schemaVersion,
  SelectionPolicy selectionPolicy,
  List<SettlementTermSnapshot> terms
) {}

record SettlementTermSnapshot(
  SettlementRuleCode ruleCode,
  Short sourceBetOption,
  PayoutRuleSnapshot payoutRule,
  WinMode winMode,
  SettlementTermSource source,
  UUID sourceRuleId
) {}
```

The JSONB replaces the authoritative role of legacy line coverage persistence. Potential payout
summary columns and line coverage compatibility objects are removed. New settlement reads the typed
snapshot.

Snapshot rules:

- no gain is calculated at sale time;
- no min/max/total payout value is stored;
- no `settlementPayoutSnapshot` is stored;
- no line odds summary is authoritative;
- snapshot terms contain only the parameters needed for future settlement;
- currency stays on the ticket parent;
- implicit terms keep `betOption=null`;
- explicit terms snapshot the selected rule code and commercial label;
- rendering must not infer print behavior from the number of terms.

### Pricing Resolution

Replace odds-only resolution with payout-rule resolution while keeping adapter compatibility during
the migration:

```text
SellerTerminal + selection
Tenant + selection
SellerTerminal general
Tenant general
```

Resolution is evaluated term by term. For example, if Exact has a SellerTerminal override and
Permuted does not, Exact uses the terminal value while Permuted falls back to the tenant default.
Seller terminals do not have to recopy the whole game configuration.

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

V0 decision: `HT_MARYAJ_GRATIS` is a distinct game.

The promotion does not define how this game wins. It only decides when one or more
`HT_MARYAJ_GRATIS` lines are added to the ticket.

The generated line remains:

- `gameCode=HT_MARYAJ_GRATIS`;
- `origin=PROMOTION`;
- `stakeAmount=0`;
- promotion decision id/code captured in the sale snapshot.

Maryaj gratis terms:

- eligibility and quantity come from promotion config;
- selection generation/manual choice comes from promotion config;
- active options and selection policy come from tenant game configuration;
- fixed Exact/Permuted payout terms come from `core.pricing`;
- SellerTerminal payout overrides come from `core.pricing`;
- line stake is zero;
- terms are snapshotted on the generated line.

SellerTerminal overrides for Maryaj gratis pricing must be partial: a terminal may override Exact
without overriding Permuted, or the opposite, without copying the whole game configuration.
Eligibility promotion overrides are a separate capability from pricing overrides.

For example, if tenant Maryaj gratis gives one automatic free line to everyone, terminal Jean can
override only that FREE_GAME_LINE effect to two seller-selected free lines, without recopying the
campaign. If Jean needs different fixed Exact/Permuted payout amounts, that remains a
`core.pricing` SellerTerminal payout-rule override, not a promotion override.

### Preview and Confirm

Preview should return an estimated basket and a prepared promotion decision, but confirm must
re-resolve the authoritative settlement snapshot.

To avoid a silent financial surprise when configuration changes between preview and confirm:

- preview returns a `configurationHash` covering effective pricing, tenant game policy, and
  promotion decision inputs;
- confirm verifies the hash before persisting;
- if the hash changed, confirm returns `409 sales.preparation_stale` and the POS reloads preview;
- confirm idempotency guarantees retries return the same ticket and same snapshot.

### Settlement

Settlement reads `SettlementTermsSnapshot` only:

- evaluate each term against official draw facts;
- for `STAKE_MULTIPLIER`, compute realized amount using the snapshotted multiplier;
- for `FIXED_AMOUNT`, use the snapshotted amount;
- for `ALTERNATIVE`, keep the best winning term;
- for `CUMULATIVE`, sum only cumulative winning terms;
- persist `ticket_line.payout_amount`, line result, and `sales_ticket.winning_amount`;
- publish settlement/payout analytics events once.

Legacy behavior:

- snapshot present: use modern settlement;
- snapshot absent: fail strongly (`SETTLEMENT_FAILED` or equivalent error state), never mark the
  line as `LOST` to hide a data problem.

### Print/Reprint

Print and reprint remain customer-safe:

- never show potential gain;
- never show odds/multipliers;
- hide option in implicit mode;
- show snapshot commercial label in explicit mode;
- render Maryaj as `12 x 34`/`12 × 34` depending receipt formatter support;
- render Maryaj gratis with promotion markers (`*`, `GRATIS`) from sale snapshot, for example
  `* 12 × 34   GRATIS`;
- do not print fixed Exact/Permuted amounts before result;
- result screens after settlement use persisted `payout_amount`, not settlement terms.

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
- `HT_MARYAJ_GRATIS` remains identifiable as a game in aggregations.
- `origin=PROMOTION` distinguishes promotional lines.
- `stake_amount=0` never increases paid sales.
- `payout_amount` increases realized winnings after settlement.

No potential payout KPI in V0.

## Migration Strategy

1. Add new payout rule and settlement snapshot model/contracts.
2. Add JSONB column to `sales_ticket_line`.
3. Write new sale-time snapshots while keeping legacy fields populated if needed for compatibility.
4. Move settlement to the JSONB snapshot.
5. Move print/reprint to snapshot data where needed.
6. Move reports/stats to realized persisted values.
7. Backfill or guard legacy tickets.
8. Remove legacy potential columns and remaining odds/coverage compatibility objects.

## Risks

- Mixing fixed and multiplier rules incorrectly can create money loss.
- Existing tickets may lack the new snapshot; settlement needs an explicit legacy strategy.
- Selection-specific overrides increase lookup complexity and cache invalidation needs.
- Reporting must not parse JSON for historical aggregates.
- Promotion terminal overrides require careful effective-config hashing; sale preview and confirm
  must resolve the same terminal-scoped terms unless configuration changes between the two calls and
  that behavior is explicitly accepted.
