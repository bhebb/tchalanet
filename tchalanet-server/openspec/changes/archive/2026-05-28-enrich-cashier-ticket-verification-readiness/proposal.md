# Change: enrich-cashier-ticket-verification-readiness

## Why

Cashier/POS must support the real field flow:

- the printed ticket QR/barcode contains a public verification URL or public ticket code, not an internal ticket id;
- the POS must verify that scanned value securely and contextually;
- the POS should show lightweight readiness/attention notifications without interrupting normal selling;
- the POS must receive translation keys and actions from the backend instead of hardcoding business messages.

This change keeps V1 simple: no acknowledgement workflow, no review table, no "I paid everything" button. The only financial action remains `core.payout.ExecutePayoutCommand`.

## What changes

- Add a POS ticket verification endpoint under `features.cashier`.
- Add a cashier readiness/home endpoint returning blockers, badges, and non-blocking attention notifications.
- Normalize scanned ticket values: full public URL, raw public code, or legacy scan format.
- Return contextual status + `titleKey` + `messageKey` + `params` + `availableActions`.
- Show a non-blocking notification when previous draws have unpaid/open payout claims.
- Keep public ticket verification separate from authenticated POS verification.

## Out of scope

- Refactoring payout internals.
- Creating payout claims.
- Reconciliation storage and repair actions.
- Operational acknowledgement/proof workflow for V1.
- Tenant-admin email notifications.

## Event reminders

Cashier does not own the event chain, but its responses depend on it:

```text
DrawSettledEvent
  -> Sales settles tickets
  -> TicketWinningSettlementCreatedEvent
  -> PayoutClaimOpenedEvent
  -> Cashier verification can show PAYABLE
  -> ExecutePayoutCommand
  -> PayoutPaidEvent
```

Cashier must not create payout claims and must not mark claims as paid except by dispatching `ExecutePayoutCommand`.
