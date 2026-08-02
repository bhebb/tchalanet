# Design

## Domain model

```java
record AppliedSettlementSnapshot(
    int schemaVersion,
    List<AppliedSettlementTermSnapshot> terms) {}

record AppliedSettlementTermSnapshot(
    SettlementRuleCode ruleCode,
    SettlementTermSource source,
    Money realizedAmount) {}
```

The existing `SettlementTermsSnapshot` is captured during sale preparation/confirmation and is
never updated when a draw result is recorded. It contains the complete pre-result coverage and the
terms used by the calculator. `AppliedSettlementSnapshot` is written once the result is applied
and is also immutable afterward, except through the existing controlled result replay flow. The
applied snapshot references the captured rule code/source and stores only the realized amount, so
the payout rule is not duplicated.

## Coverage rules

- Bòlèt: one commercial selection covers `MATCH_1_2D`, `MATCH_2_2D` and `MATCH_3_2D`.
- Maryaj: one commercial selection exposes exact and reverse possibilities when the configured
  selection policy allows both.
- Loto games: expose every technically applicable option for the selected game/selection.
- Resolve each rule independently using seller-terminal override first, then tenant default.
- Store the effective value and source, never only a rule id.

The current model treats Bòlèt as three separate `BetType` values. The implementation must add a
commercial coverage representation or an equivalent planner output so that a single Bòlèt line can
carry all three possibilities without changing the settlement meaning of an already persisted
line.

## Result application and payout calculation

The calculator evaluates the same complete settlement terms that the UI displays as pre-result
coverage. There is no separate display-only coverage list that could drift from settlement.

For every winning settlement term:

- `STAKE_MULTIPLIER`: `payoutBaseAmount * multiplier`, rounded to 2 decimals with `HALF_UP`.
- `FIXED_AMOUNT`: use `fixedAmount`, rounded to 2 decimals with `HALF_UP`.

Combination rules remain explicit:

- `ALTERNATIVE`: keep the highest winning payout and its applied term.
- `CUMULATIVE`: add all winning payouts and retain all applied terms.
- no winning term: line status `LOST`, realized amount zero, no applied term.

The ticket winning amount remains the sum of realized line payouts. Commission remains calculated
at sale time from the ticket stake and the captured seller commission rate; it is not deducted from
the payout calculation and is not re-resolved at result time.

## API contract

The POS/admin ticket detail response should expose both concepts per line:

```json
{
  "pricingCoverage": { "terms": [] },
  "appliedSettlement": { "terms": [] },
  "resultStatus": "PENDING",
  "payoutAmountCents": 0
}
```

Before results, the UI maps the captured `SettlementTermsSnapshot.terms` to `pricingCoverage`.
After results, it displays the applied term(s) and realized payout. The captured terms remain
available for audit and historical inspection.

## Examples

For a Bòlèt selection `12`, stake `10 HTG`, with seller snapshot `65/35/15`:

- pending: `Barèm Vandè: 65-35-15`;
- lot 1 wins: applied rule `MATCH_1_2D`, multiplier `65`, payout `10 * 65 = 650 HTG`;
- lot 2 wins: applied rule `MATCH_2_2D`, multiplier `35`, payout `350 HTG`;
- no match: no applied rule, payout `0 HTG`.

If the seller changes the configuration after the sale, these values do not change.

## Verification findings

The current calculator already implements the amount formulas and alternative/cumulative policy in
`TicketWinningCalculator`, but `TicketLineResult` currently returns only status and total payout.
It does not preserve which term won. The new applied snapshot must therefore be carried through the
result command, `TicketLine`, persistence, mapper and detail API. The only new pricing persistence
needed is the applied result; the complete coverage uses the existing settlement terms snapshot.
