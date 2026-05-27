# Design — Ops reconciliation V1

## Ownership

`platform.reconciliation` owns operational reconciliation records only:

- runs;
- check results;
- anomalies;
- repair action records.

It does not own Sales, Payout, Draw, or OfflineSync truth.

## Source of truth matrix

| Concern | Source of truth |
|---|---|
| Official result | `core.drawresult` / applied `core.draw` |
| Sold ticket | `core.sales` |
| Ticket settlement/winning amount | `core.sales` |
| Payout claim | `core.payout` |
| Payout payment | `core.payout` |
| Offline submission | `core.offlinesync` |
| Offline-created ticket | `core.sales` |
| Stats/Ledger | Derived projections, not source of truth |

## Tables

### `reconciliation_run`

```text
id
tenant_id nullable
scope
business_date
started_at
completed_at
status
triggered_by
triggered_by_user_id nullable
reason nullable
summary_json
audit fields
```

### `reconciliation_check_result`

```text
id
run_id
tenant_id nullable
check_key
status
severity
expected_count
actual_count
anomaly_count
summary_json
started_at
completed_at
```

### `reconciliation_anomaly`

```text
id
run_id
tenant_id
check_key
anomaly_type
severity
status
resource_type
resource_id
related_resource_type nullable
related_resource_id nullable
message_key
details_json
detected_at
resolved_at nullable
resolved_by nullable
resolution_reason nullable
audit fields
```

### `reconciliation_repair_action`

```text
id
anomaly_id
run_id
tenant_id
action_type
status
command_name
command_payload_json
executed_at nullable
executed_by nullable
failure_message nullable
audit fields
```

## Check keys V1

```text
sales.resulted_draws_unsettled_tickets
sales.winning_ticket_settlement_consistency
payout.claims_for_winning_tickets
payout.payment_claim_consistency
offlinesync.submission_ticket_consistency
offlinesync.daily_submission_counts
```

## Severity

```text
INFO
WARN
CRITICAL
```

Examples:

- Winning ticket without claim: WARN or CRITICAL depending amount/age.
- Paid claim without payment record: CRITICAL.
- Rejected offline submission with active ticket: CRITICAL.
- Old pending offline submissions: WARN.

## Email notifications

After a run completes:

```text
if criticalCount > 0 OR attentionRequiredCount > 0:
  notify tenant admins
else:
  no email
```

Email is sent through `platform.communication`.

Recipient lookup is done through identity/tenant-user API.

Subject key example:

```text
reconciliation.email.tenant_admin.anomalies.subject
```

Body key example:

```text
reconciliation.email.tenant_admin.anomalies.body
```

## Tenant-local midnight

Daily reconciliation runs per tenant business timezone.

```text
runAt tenant local around 00:05
businessDate = previous LocalDate in tenant zone
windowStart/windowEnd are Instants
```

Persist all moments as `Instant`; use `LocalDate` for business date.

## Repair rules

Reconciliation repairs must call owner-domain commands.

Allowed safe-ish repairs:

- Open missing payout claim from Sales settlement.
- Re-run settlement for resulted draw if no correction risk.
- Create missing offline ticket from accepted submission if gates remain valid.

Manual/Ops-confirmed repairs:

- Amount mismatch.
- Duplicate claim.
- Rejected submission with ticket.
- Paid claim without payment consistency.

No direct cross-domain table writes.
