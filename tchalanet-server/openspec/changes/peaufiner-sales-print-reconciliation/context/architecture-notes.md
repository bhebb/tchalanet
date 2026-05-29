# Architecture Notes

## Module owner

Use `core.reconciliation`, not `platform.reconciliation`, because reconciliation needs to consume `core.*.api` read queries. Platform must not depend on core.

HTTP path can still be `/platform/ops/reconciliation/**` because HTTP platform scope means SUPER_ADMIN / ops surface, not Java package owner.

## Data sources V1

```text
core.drawresult -> official resulted draws
core.sales      -> expected outcome + actual ticket state from snapshots
core.payout     -> payout claims + payments
core.ledger     -> future accounting reconciliation
```

## Why counts are not enough

Counts and totals can match while the wrong tickets are marked as winners. Reconciliation must always have ticket-level checks for recent/resulted draws.

Example:

```text
Ticket A should win but is LOST
Ticket B should lose but is WON
Expected winner count = 1
Actual winner count = 1
Summary looks correct, but the system is wrong.
```

## No silent repair

Reconciliation creates anomalies only.

Future repair actions must be:

- explicit;
- audited;
- SUPER_ADMIN/ops authorized;
- reason required;
- idempotent;
- never automatic by default.
