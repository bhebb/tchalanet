# Analytics Reconciliation Runbook

Status: operational V1.

## Scope

The reconciliation operation is restricted to `SUPER_ADMIN` and is tenant-scoped. It compares
immutable sales snapshots with the analytics projections for a bounded half-open business-date
window. It covers tenant daily rows, seller-terminal daily rows, draw rows, seller-terminal/draw
rows and selection rows.

The source snapshot is read through the sales public query boundary. The reconciler does not read
sales JPA entities directly and does not replay domain events.

## Endpoint

External route:

```text
POST /api/v1/platform/ops/analytics/reconciliation
```

Request:

```json
{
  "tenantId": "00000000-0000-0000-0000-000000000003",
  "from": "2026-07-01",
  "to": "2026-08-01",
  "mode": "VALIDATE"
}
```

`VALIDATE` is read-only and does not require an idempotency key. It returns `SUCCESS` when the
expected and observed projection sets match, otherwise `MISMATCH` with exact mismatch rows.

`REBUILD_AND_VALIDATE` requires a non-empty repair reason and the `Idempotency-Key` header:

```bash
curl -sS -X POST \
  'http://127.0.0.1:8093/api/v1/platform/ops/analytics/reconciliation' \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: analytics-repair-tenant-20260701-20260801-v1' \
  --data '{
    "tenantId": "00000000-0000-0000-0000-000000000003",
    "from": "2026-07-01",
    "to": "2026-08-01",
    "mode": "REBUILD_AND_VALIDATE",
    "repairReason": "STG dashboard mismatch investigation"
  }'
```

## Safety contract

- The request switches into the requested tenant context before any tenant-scoped read/write.
- A tenant advisory lock prevents a live analytics projector from racing the repair.
- Rebuild deletes and replaces only the selected analytics scope in one transaction.
- The response is `SUCCESS` only after an exact post-rebuild comparison.
- A failed comparison rolls back the replacement; it never clears `processed_event` and never
  republishes business events.
- Reusing an idempotency key with a different payload is rejected. A completed request is replayed
  for 24 hours.
- The endpoint audit annotation records the operational action. Persistent reconciliation-run
  history and automatic mismatch alerts are follow-up work.

## Operational sequence

1. Run `VALIDATE` for the affected tenant and business-date window.
2. Confirm the mismatch and record the returned `runId`.
3. Run `REBUILD_AND_VALIDATE` with an incident/deployment reason and a new idempotency key.
4. Require `SUCCESS` and an empty mismatch list before declaring the dashboard repaired.
5. Refresh the affected dashboard/report and compare tenant, draw, terminal and selection totals.
6. For `SOURCE_UNAVAILABLE` or `RECONCILIATION_REQUIRED`, do not edit analytics tables manually;
   repair the source/projection failure and rerun validation.
