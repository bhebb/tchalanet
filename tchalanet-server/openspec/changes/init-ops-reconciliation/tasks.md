# Tasks

## 1. Platform reconciliation foundation

- [ ] Create `platform.reconciliation` module/package or approved Ops equivalent.
- [ ] Add run model: `ReconciliationRun`.
- [ ] Add check result model: `ReconciliationCheckResult`.
- [ ] Add anomaly model: `ReconciliationAnomaly`.
- [ ] Add repair action model: `ReconciliationRepairAction`.
- [ ] Add migrations and RLS/audit fields.

## 2. Daily scheduler

- [ ] Add scheduler that runs per active tenant at tenant-local midnight window.
- [ ] Compute `businessDate = yesterday` in tenant timezone.
- [ ] Use `Instant` for timestamps and `LocalDate` for business date.
- [ ] Keep scheduler thin: create/run command, log summary, no reconciliation logic in scheduler.

## 3. Checks V1

- [ ] Check resulted/settled draws against unsettled sales tickets.
- [ ] Check winning settled tickets against payout claims.
- [ ] Check payout claim paid status against payout payment records/events.
- [ ] Check accepted offline submissions against tickets created.
- [ ] Check rejected offline submissions do not have active tickets.
- [ ] Check daily offline submission counts vs offline-created ticket counts.

## 4. Read APIs needed from owning domains

- [ ] Sales exposes settled ticket rows/snapshots by draw/date.
- [ ] Sales exposes winning settled ticket payout snapshots.
- [ ] Payout exposes claims by ticket ids and claim/payment summaries.
- [ ] OfflineSync exposes submission summaries and submission-ticket links.
- [ ] Draw exposes resulted/settled draws by business date if needed.

## 5. Repair action orchestration

- [ ] Define repair actions:
  - `OPEN_PAYOUT_CLAIM`
  - `RUN_SETTLEMENT_FOR_DRAW`
  - `CREATE_TICKET_FROM_OFFLINE_SUBMISSION`
  - `BLOCK_PAYOUT_CLAIM`
  - `FLAG_FOR_REVIEW`
- [ ] Repairs must dispatch owner-domain commands.
- [ ] Repairs must be audited and reason-required.
- [ ] Critical anomalies should not auto-repair without policy approval.

## 6. Tenant-admin email notification

- [ ] Publish `ReconciliationRunCompletedEvent` after run completion.
- [ ] Add listener/command to notify tenant admins if critical or attention-required anomalies exist.
- [ ] Resolve tenant admin recipients via identity/tenant-user API.
- [ ] Send email via platform.communication, not direct SMTP.
- [ ] Avoid email when run is clean.

## 7. Ops endpoints

- [ ] `GET /platform/ops/reconciliation/runs`
- [ ] `GET /platform/ops/reconciliation/runs/{runId}`
- [ ] `GET /platform/ops/reconciliation/anomalies`
- [ ] `POST /platform/ops/reconciliation/runs`
- [ ] `POST /platform/ops/reconciliation/anomalies/{anomalyId}/repair`
- [ ] `POST /platform/ops/reconciliation/anomalies/{anomalyId}/ack`
- [ ] `POST /platform/ops/reconciliation/anomalies/{anomalyId}/ignore`

## 8. Tests

- [ ] Run with no anomalies.
- [ ] Winning ticket without payout claim creates anomaly.
- [ ] Paid claim without payment consistency creates anomaly.
- [ ] Accepted offline submission without ticket creates anomaly.
- [ ] Critical anomalies trigger tenant-admin email.
- [ ] Clean run does not trigger email.
