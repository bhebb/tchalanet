# Change: init-ops-reconciliation

## Why

Tchalanet needs an operational reconciliation capability because payout automation, ticket settlement, offline sync, and projections can fail independently. The system must detect inconsistencies without depending on sellers/admins to manually notice them.

Reconciliation is a platform/Ops concern. It compares domain truths, creates anomalies, optionally triggers owner-domain repairs, and notifies tenant admins when attention is required.

## What changes

- Add `platform.reconciliation` or equivalent Ops capability for reconciliation runs, check results, anomalies, and repair actions.
- Add a daily tenant-local reconciliation run around midnight for the previous business date.
- Compare:
  - resulted/settled draws vs sales ticket settlement;
  - winning settled tickets vs payout claims;
  - payout claims vs payout payments;
  - offline submissions vs tickets created from offline sync;
  - daily submission counts vs offline-created ticket counts.
- Notify tenant admins by email when critical or attention-required anomalies are detected.
- Expose Ops endpoints to list runs/anomalies and trigger controlled repair actions.

## Out of scope

- Implementing every repair automatically.
- Ledger/statistics full reconciliation.
- Complex fraud scoring engine.
- Replacing the owning domains' invariants.
- Direct table mutation in Sales/Payout/Offline from reconciliation.

## Event reminders

Reconciliation is not part of the hot path, but it observes the same chain:

```text
DrawSettledEvent
  -> Sales TicketSettledEvent / TicketWinningSettlementCreatedEvent
  -> PayoutClaimOpenedEvent
  -> PayoutPaidEvent / PayoutReversedEvent
  -> Offline submission accepted/rejected/ticket-created events
```

Reconciliation can also run batch comparisons directly through public read queries.
