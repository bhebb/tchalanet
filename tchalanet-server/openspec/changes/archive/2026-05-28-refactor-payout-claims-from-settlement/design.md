# Design — Payout claims from Sales settlement

## Core rule

Payout does not discover winners. Sales settlement is the source of winning ticket amount snapshots.

```text
Sales = ticket settlement truth
Payout = claim/payment truth
```

## Domain model

Preferred domain name:

```java
PayoutClaim
```

V1 statuses:

```java
public enum PayoutClaimStatus {
    OPEN,
    BLOCKED,
    PAID,
    CANCELLED,
    REVERSED
}
```

Sources:

```java
public enum PayoutClaimSource {
    SALES_SETTLEMENT,
    OPS_RECONCILIATION,
    MANUAL_ADMIN_CORRECTION
}
```

## Command: open claim

```java
public record OpenPayoutClaimFromSettlementCommand(
    EventId sourceEventId,
    TenantId tenantId,
    TicketId ticketId,
    DrawId drawId,
    Long amountCents,
    String currency,
    OutletId sellingOutletId,
    SalesSessionId sellingSessionId
) implements Command<OpenPayoutClaimResult> {}
```

The handler must be idempotent by event id and/or unique ticket/settlement constraint.

## Command: execute payout

Execution requires:

- authenticated actor;
- trusted operational context;
- terminal/outlet/session rechecks in transaction;
- locked payout claim row;
- claim status `OPEN`;
- Sales payout snapshot still matching amount/currency.

## Events

### `PayoutClaimOpenedEvent`

Published after a claim is created.

Consumers may include stats, public ticket projection, admin dashboards, reconciliation.

### `PayoutPaidEvent`

Published after actual payment.

Consumers:

- Sales: update payout status projection on ticket.
- Stats/Ledger: account payout paid.
- Reconciliation: close/validate anomalies.

### `PayoutReversedEvent`

Published after reversal.

Consumers:

- Sales: update payout projection as reversed.
- Stats/Ledger: account reversal.
- Reconciliation: flag or resolve anomalies.

## Manual actions

Manual actions are corrections/controls only:

```text
OPEN -> BLOCKED
BLOCKED -> OPEN
OPEN/BLOCKED -> CANCELLED
PAID -> REVERSED
```

No normal manual payout request flow in V1.

## Public/POS lookup

Payout lookup may support Cashier, but it must not accept public anonymous execution. Public pages can display safe status only. POS payment requires authenticated context.
