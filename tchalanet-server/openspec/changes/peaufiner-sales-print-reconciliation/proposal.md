# Change: Peaufiner Sales / Print / Core Reconciliation

## Status

Draft for implementation.

## Motivation

Tchalanet is reaching the point where selling, promotions, printing, payouts, and operational trust must be tightened together. A tenant must be able to trust that:

- a sold ticket is materialized with immutable snapshots;
- promotional effects are captured in `core.sales`, not re-evaluated later;
- receipts are canonical, short, and consistent across print/backup/send;
- winning tickets, payout claims, and payments can be verified after result application;
- operational anomalies are detected and reported without silent correction.

## Key decisions

### Sales and promotions

Promotion V1 remains intentionally limited:

- `FREE_GAME_LINE`
- `BOOST_ODDS`
- `WAIVE_CHARGE`

Promotion config decides. `core.sales` materializes. Payout/reconciliation consume snapshots.

### Print and receipt

The canonical receipt content belongs to `core.sales`. `features.cashier` chooses channel/format and delegates rendering/delivery, but must not decide receipt content.

Receipt branding rules live in `TicketReceiptBrandingFormatter`, not `TicketReceiptAssembler`.

### Reconciliation

Introduce `core.reconciliation` as a core verification domain with batch execution.

`settlement` materializes financial results. Reconciliation verifies, detects, stores anomalies, and alerts. Since `core.settlement` does not exist in the current system, V1 reconciliation uses:

- `core.drawresult` for official draw/result facts;
- `core.sales` for expected outcome and actual ticket state;
- `core.payout` for claims and payments;
- `core.ledger` later for accounting checks.

Reconciliation must never correct silently.

## Non-goals

- No full promotion rule engine.
- No advanced multi-stacking promotion engine.
- No ledger accounting reconciliation in V1.
- No automatic repair actions in V1.
- No correction of sales/payout state by reconciliation without explicit audited repair command.

## Architecture summary

```text
core.reconciliation
  api/
    query/model views if needed later
  internal/
    domain/
      model/ ReconciliationRun, ReconciliationAnomaly
      service/ anomaly fingerprint/severity policy
    application/
      service/ DailyReconciliationRunner, ReconciliationCheckExecutor
      port/out/ ReconciliationRunWriterPort, ReconciliationAnomalyWriterPort
    infra/
      batch/ DailyReconciliationJobConfig, reader, processor, writer
      scheduler/ DailyReconciliationScheduler
      web/ ReconciliationOpsController
      persistence/ JPA entities/repositories/adapters
      notification/ CSV/email generation or platform.communication adapter
```

HTTP force-run scope:

```http
POST /platform/ops/reconciliation/daily-runs
```

Java owner remains `core.reconciliation` even though HTTP scope is `/platform/ops/**`.

