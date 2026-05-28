# Design — Sales, Print and Core Reconciliation

## 1. Sales sell flow

The sell handler must remain thin:

1. resolve context;
2. ask `SaleAcceptanceEvaluator` / `SalePreparationOrchestrator` for final prepared sale;
3. persist `Ticket` aggregate;
4. persist promotion snapshots when a decision was applied;
5. flush persistence so the print read model can see the ticket;
6. build canonical receipt backup from the same receipt model;
7. publish events and communication requests after commit;
8. return a public result DTO, not an internal aggregate.

### Must fix: no aggregate leak in public API

`SellTicketResult` under `core.sales.api.command.sell` must not expose `core.sales.internal.domain.model.ticket.Ticket`.

Use a public DTO/result shape:

```java
public record SellTicketResult(
    TicketId ticketId,
    String ticketCode,
    String publicCode,
    String displayCode,
    SellTicketOutcome outcome,
    ApprovalRequestId approvalRequestId,
    List<SalesNotice> notices,
    List<SaleIssue> issues,
    TicketReceiptPrintContent backup,
    ActionAvailability actionAvailability,
    SellerInstruction sellerInstruction
) {}
```

## 2. Promotion materialization

Sales must persist enough snapshots for payout and reconciliation to use without reading current pricing/config.

### Normal paid line

- `stakeAmount = paid stake`
- `payoutBaseAmount = stakeAmount`
- `oddsSnapshot = tenant odds from pricing catalog at sale time`
- `potentialPayoutAmount = payoutBaseAmount * oddsSnapshot`
- `origin = CUSTOMER`
- `pricingSource = STANDARD`
- `selectionSource = CUSTOMER_SELECTED`

### BOOST_ODDS

- target V1 = all existing lines matching `effect.gameCode`
- set `oddsSnapshot = effect.amount` (odds override)
- recompute `potentialPayoutAmount = payoutBaseAmount * oddsOverride`
- set promotion metadata on line:
  - `promotionDecisionId`
  - `promotionLabel`
  - `promotionEffectType = BOOST_ODDS`
  - `pricingSource = PROMOTION`

Validation required:

- `effect.amount != null`
- `effect.amount > 0`
- scale normalized to 4 decimals

### FREE_GAME_LINE

- add one or more `TicketLine` rows
- `origin = PROMOTION`
- `pricingSource = PROMOTION`
- `selectionSource = CUSTOMER_SELECTED` if customer chose the free line selection, otherwise `PROMOTION_DEFINED` / `PROMOTION_GENERATED`
- `stakeAmount = 0`
- `payoutBaseAmount = promotion-defined amount`
- `oddsSnapshot = tenant odds or effect odds according to V1 rule`
- `potentialPayoutAmount = payoutBaseAmount * oddsSnapshot`
- no hardcoded fallback selection such as `"1"`

If free-game line selection is required and absent, fail the sale with a business conflict.

### WAIVE_CHARGE

Do not remove the charge. Keep it for print/audit and mark it waived.

`TicketCharge` must retain:

- original charge type;
- original amount;
- paidBy;
- waived flag;
- waivedByPromotionDecisionId;
- waivedByPromotionRuleId;
- promotionEffectType = `WAIVE_CHARGE`;
- promotion label.

Money totals must exclude waived buyer-facing charges.

## 3. Print and receipt

`core.sales` owns canonical receipt content:

```text
TicketPrintView
  -> TicketReceiptAssembler
  -> TicketReceiptPrintFormatter
  -> TicketBackupAssembler
```

`features.cashier` may choose format/channel and request rendering/delivery, but must not decide header/footer/lines/promotions/money.

### Branding decision

`TicketReceiptAssembler` transports data:

- tenant display name;
- tenant receipt header;
- outlet name;
- outlet receipt header;
- settings if available.

`TicketReceiptBrandingFormatter` decides what to print.

Default settings:

```text
tenantDisplayMode = AUTO
outletDisplayMode = AUTO
```

Display modes V1:

```java
public enum ReceiptBrandingDisplayMode {
    AUTO,
    NAME_ONLY,
    HEADER_ONLY,
    NAME_AND_HEADER
}
```

`HIDDEN` stays out of V1 unless explicitly needed.

AUTO rule:

- if custom header exists, print header;
- otherwise print name;
- do not print name + header by default to avoid overly long receipts.

### Context rule

`TicketReceiptAssembler` should not call `TchContext.currentOrNull()` for normal behavior. Locale/timezone must come from parameters or `TicketPrintView.metadata()`.

## 4. Reconciliation V1

### Ownership

`core.reconciliation` owns:

- `reconciliation_run`
- `reconciliation_anomaly`
- batch jobs;
- scheduler;
- ops force-run controller;
- anomaly CSV/email report orchestration.

### Inputs

Read via QueryBus from domain public APIs:

- `core.drawresult`: resulted draws/draw results to verify;
- `core.sales`: expected outcome + actual ticket state;
- `core.payout`: claims/payments;
- `core.ledger`: not V1.

### Expected outcome source

Because there is no `core.settlement` in V1, expected outcome is provided by `core.sales`.

`core.sales` must compute expected ticket outcomes using:

- official draw result;
- ticket line snapshots;
- odds snapshot;
- payout base amount;
- promotion materialized line/charge metadata;
- not current pricing/catalog configuration.

### Two-level checks

1. Summary checks detect mismatches quickly.
2. Ticket-level checks identify exact ticket/claim/payment anomalies.

Counts are not enough because errors may cancel each other out.

## 5. Idempotence

Reconciliation runs and anomalies must be idempotent.

Recommended anomaly fingerprint:

```text
<ANOMALY_TYPE>:<drawId>:<ticketId>:<claimId?>:<paymentId?>
```

Unique index:

```sql
UNIQUE (tenant_id, fingerprint)
```

If the same anomaly is rediscovered, update `last_seen_at`, `run_id`, and counters rather than creating duplicates.

## 6. Notifications

After commit:

- if critical/high anomalies exist, send email to configured tenant/platform admins;
- attach CSV generated from persisted anomalies of the run;
- email must state that no automatic correction was applied.

