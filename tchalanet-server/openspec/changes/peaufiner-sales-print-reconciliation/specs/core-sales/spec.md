# Spec — core.sales changes

## Requirement: Sell result must not expose internal aggregate

`SellTicketResult` in `core.sales.api.command.sell` MUST NOT expose `core.sales.internal.*` types.

### Scenario: sell succeeds

Given a ticket is accepted and persisted
When the sell handler returns
Then the result contains public identifiers and public DTOs only
And the result does not expose `Ticket` aggregate
And no public API imports `core.sales.internal.*`.

## Requirement: Promotion effects are materialized as sales snapshots

Sales MUST persist promotion effects in ticket lines and charges so payout/reconciliation never re-evaluates promotion rules.

### Scenario: BOOST_ODDS applies

Given a promotion decision with `BOOST_ODDS`
And the effect targets a game code
When sales applies the effect
Then all matching existing ticket lines have updated odds snapshot
And potential payout is recomputed from payout base amount and boosted odds
And promotion decision id, label, effect type, and pricing source are set on the line.

### Scenario: BOOST_ODDS invalid amount

Given a promotion decision with `BOOST_ODDS`
And effect amount is null or non-positive
When sales applies the effect
Then the sale fails with a business problem
And no ticket is persisted.

### Scenario: FREE_GAME_LINE applies

Given a promotion decision with `FREE_GAME_LINE`
When sales applies the effect
Then one or more promotion-origin ticket lines are created
And each line has stake amount zero
And payout base amount comes from the promotion effect
And selection source is explicit
And no hardcoded default raw selection is used.

### Scenario: free game line needs customer selection

Given a free game line requires a selected number
And the command does not provide promotion choice
And the effect does not provide a configured selection
When sales applies promotion
Then the sale fails with `promotion.free_game_selection_required`.

### Scenario: WAIVE_CHARGE applies

Given a ticket has a buyer SMS charge
And a promotion decision waives buyer SMS
When sales applies the effect
Then the charge remains stored
And the charge is marked waived
And the original amount remains available for print/audit
And ticket total excludes the waived buyer-facing charge.

## Requirement: Limit/autonomy uses final promoted basis

Limit/autonomy evaluation MUST use final lines and final money after promotions.

### Scenario: odds boost increases risk

Given a sale has a normal potential payout
And a boost odds promotion increases potential payout
When limits are evaluated
Then the limit context uses the boosted potential payout amount.

## Requirement: Expected reconciliation outcomes are exposed by sales

`core.sales` MUST expose read queries for reconciliation.

### Query: ListExpectedTicketOutcomesForDrawResultQuery

Input:

```java
public record ListExpectedTicketOutcomesForDrawResultQuery(
    DrawResultId drawResultId
) implements Query<List<ExpectedTicketOutcomeRow>> {}
```

Output:

```java
public record ExpectedTicketOutcomeRow(
    TicketId ticketId,
    String ticketCode,
    String publicCode,
    String displayCode,
    DrawId drawId,
    DrawResultId drawResultId,
    boolean shouldWin,
    TicketResultStatus expectedResultStatus,
    TicketSettlementStatus expectedSettlementStatus,
    BigDecimal expectedPayoutAmount,
    int winningLineCount
) {}
```

Rules:

- compute using official draw result + sales snapshots;
- never read current pricing odds;
- include public/display codes for CSV/reporting;
- exclude cancelled/voided/rejected tickets unless they are needed as explicit non-payable rows.

### Query: ListActualTicketStatesForDrawQuery

Input:

```java
public record ListActualTicketStatesForDrawQuery(
    DrawId drawId
) implements Query<List<ActualTicketStateRow>> {}
```

Output:

```java
public record ActualTicketStateRow(
    TicketId ticketId,
    String ticketCode,
    String publicCode,
    String displayCode,
    TicketSaleStatus saleStatus,
    TicketResultStatus resultStatus,
    TicketSettlementStatus settlementStatus,
    BigDecimal actualPotentialPayout,
    Instant placedAt,
    boolean cancelled,
    boolean voided
) {}
```

### Query: GetSalesOutcomeSummaryForDrawQuery

Input:

```java
public record GetSalesOutcomeSummaryForDrawQuery(
    DrawId drawId
) implements Query<SalesOutcomeSummaryForDrawRow> {}
```

Output:

```java
public record SalesOutcomeSummaryForDrawRow(
    DrawId drawId,
    long resultedTicketCount,
    long winnerCount,
    long loserCount,
    long pendingResultCount,
    BigDecimal totalPotentialPayout,
    BigDecimal totalSettledPayout
) {}
```

## Requirement: Receipt content is canonical in core.sales

`core.sales` MUST assemble receipt content from canonical print views.

### Scenario: receipt backup after sale

Given a ticket is persisted
When the sell handler builds backup
Then it reads `TicketPrintView`
And assembles `TicketReceiptView`
And formats backup from that same receipt model.

## Requirement: Receipt assembler is deterministic

`TicketReceiptAssembler` SHOULD NOT read `TchContext` implicitly.

### Scenario: requested locale missing

Given requested locale is null
And print metadata has a locale
When receipt is assembled
Then metadata locale is used
And no ambient request context is required.

## Requirement: Receipt branding display rules live in formatter

`TicketReceiptBrandingFormatter` MUST own name/header display decisions.

### Scenario: tenant has custom header in AUTO mode

Given tenant display mode is AUTO
And tenant receipt header is present
When receipt is formatted
Then the tenant header is printed
And tenant display name is not repeated by default.

### Scenario: tenant mode NAME_AND_HEADER

Given tenant display mode is NAME_AND_HEADER
When receipt is formatted
Then tenant display name and tenant receipt header are both printed.
