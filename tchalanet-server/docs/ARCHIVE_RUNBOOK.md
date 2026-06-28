# Archive, Partitioning, Restore and Purge Runbook

Status: operational guidance for `platform.archive`.

## Scope

This runbook covers:

- archive execution;
- partition planning and cleanup;
- legal holds;
- restore for investigations;
- emergency purge options;
- Spring Batch history cleanup.

Archive is platform-admin only. Normal access to old records should read archive objects through the
backend. Restore tables are exceptional and temporary.

## Dataset Policy

| Dataset | Hot target | Archive lookup | Cleanup policy |
| --- | ---: | --- | --- |
| `sales_ticket` | 6-12 months | ticket id, public code | cleanup only after verified archive and no legal hold |
| `sales_ticket_line` | 6-12 months | parent ticket id/public code | same as ticket |
| `sales_ticket_charge` | 6-12 months | parent ticket id/public code | same as ticket |
| `draw` | 12 months | draw id/date | cleanup only after verified archive and no legal hold |
| `draw_result` | 12 months | result id/date/source hash | cleanup only after verified archive and no legal hold |
| `audit_log` | 12 months | actor/entity/action/date | partition cleanup supported |
| Envers `_aud` | dataset-specific | revision/entity | purge only after legal review |
| Spring Batch metadata | 30-90 days | job execution aggregate | may be purged aggressively in an incident |

## Archive Execution

Trigger a manual archive run:

```bash
TOKEN="$(scripts/local-jwt-token.sh --user super_admin)"
curl -sS -X POST 'http://127.0.0.1:8093/api/v1/platform/archive/runs' \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -H 'X-Request-Id: archive-manual-YYYYMMDD-1' \
  --data '{
    "strategy": "MANUAL",
    "periodStart": "2025-01-01",
    "periodEnd": "2025-02-01",
    "reason": "Operational archive validation"
  }'
```

Verify the run:

```sql
SELECT id, status, strategy, started_at, completed_at, error_message
FROM archive_run
ORDER BY started_at DESC
LIMIT 10;

SELECT table_name, status, row_count, byte_size, checksum_sha256 IS NOT NULL AS has_checksum
FROM archive_object
WHERE archive_run_id = '<run-id>'
ORDER BY table_name;
```

No cleanup is allowed unless every relevant `archive_object` is `VERIFIED`, row counts match, checksum
exists, and storage contains the object.

## Partitioning

### Current Safe State

`audit_log` is partitioned monthly by `occurred_at`.

`V247__archive_partition_maintenance.sql` installs:

- `archive_month_partition_name(parent, month)`;
- `archive_ensure_month_partition(parent, month)`;
- `archive_ensure_month_partitions(parent, start_month, month_count)`;
- 13 months of forward `audit_log` partitions from the current month.

Create future partitions manually:

```sql
SELECT public.archive_ensure_month_partitions(
  'audit_log'::regclass,
  date_trunc('month', CURRENT_DATE)::date,
  18
);
```

### Tickets, Draws and Draw Results

Do not force-convert `sales_ticket`, `sales_ticket_line`, `sales_ticket_charge`, `draw` or
`draw_result` to date-range partitioned tables with ad-hoc DDL.

Reason: PostgreSQL requires every primary key or unique constraint on a partitioned table to include
the partition key. Current tables use id-only primary keys and id-only foreign keys:

- `sales_ticket_line.ticket_id -> sales_ticket(id)`;
- `sales_ticket_charge.sales_ticket_id -> sales_ticket(id)`;
- `draw.draw_result_id -> draw_result(id)`;
- analytics tables reference `draw(id)`.

Safe options for those datasets:

1. Keep hot tables intact and purge by batched deletes only after verified archive and no legal hold.
2. Redesign tables pre-go-live so partition keys are part of child references and uniqueness strategy.
3. Add separate historical tables partitioned by month, move old rows there after archive verification,
   and keep lookup indexes online.

The current implementation supports option 1 safely now. Option 2 or 3 requires a dedicated
OpenSpec change and end-to-end migration test.

## Partition Cleanup

Plan cleanup:

```bash
curl -sS \
  'http://127.0.0.1:8093/api/v1/platform/archive/partition-cleanup/plan?tableName=audit_log&retentionCutoff=2025-01-01' \
  -H "Authorization: Bearer ${TOKEN}"
```

Execute dry-run:

```bash
curl -sS -X POST \
  'http://127.0.0.1:8093/api/v1/platform/archive/partition-cleanup/execute?partitionName=audit_log_2024_01&mode=DRY_RUN' \
  -H "Authorization: Bearer ${TOKEN}"
```

Execute detach or drop only when all are true:

- cleanup is enabled in config;
- partition is allowlisted in `tch.archive.cleanup.cleanable-tables`;
- plan says `eligible=true`;
- no active legal hold overlaps the period;
- archived row count matches hot row count;
- no `INVALID` archive object exists for the period.

Production-like config for controlled cleanup:

```yaml
tch:
  archive:
    cleanup:
      enabled: true
      mode: DETACH_ONLY # use DROP only after detach verification
      retention-months: 12
      cleanable-tables:
        - audit_log
```

## Legal Holds

Create a legal hold before any dispute, chargeback, lawsuit, regulator request, or internal
investigation:

```bash
curl -sS -X POST 'http://127.0.0.1:8093/api/v1/platform/archive/legal-holds' \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  --data '{
    "datasetCode": "sales_ticket",
    "entityType": "TICKET",
    "entityId": "aaaaaaaa-0000-0000-0000-000000001001",
    "periodStart": "2025-01-01",
    "periodEnd": "2025-02-01",
    "reason": "Customer dispute case ABC-123"
  }'
```

List active holds:

```bash
curl -sS 'http://127.0.0.1:8093/api/v1/platform/archive/legal-holds/active?limit=50' \
  -H "Authorization: Bearer ${TOKEN}"
```

Release requires a reason:

```bash
curl -sS -X POST 'http://127.0.0.1:8093/api/v1/platform/archive/legal-holds/<hold-id>/release' \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  --data '{"reason":"Case ABC-123 closed and retention approved for release"}'
```

Never purge records covered by an active legal hold.

## Restore

Restore is temporary. Prefer archive lookup/read when possible.

Restore audit rows for an investigation:

```bash
curl -sS -X POST 'http://127.0.0.1:8093/api/v1/platform/archive/restore/audit-log' \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  --data '{
    "tenantId": "00000000-0000-0000-0000-000000000003",
    "entityType": "TICKET",
    "entityId": "aaaaaaaa-0000-0000-0000-000000001001",
    "from": "2025-01-01",
    "to": "2025-02-01",
    "reason": "Legal investigation case ABC-123"
  }'
```

Restore rows are TTL-bound and cleaned by the scheduled restore cleanup.

Ticket restore is not a hot-table reinsert flow. Ticket disputes should use archive lookup first.
If a complete ticket litigation bundle is needed, export from archive objects by ticket lookup and
attach the bundle to the case record.

## Normal Purge for Non-Partitioned Ticket Tables

Use only after archive verification and legal hold checks.

Preferred path: use the platform archive ticket purge endpoint. It is `DRY_RUN` by default and
refuses execution unless:

- `archive_object` rows for `sales_ticket`, `sales_ticket_line`, and `sales_ticket_charge` are
  `VERIFIED`;
- hot row counts match archived row counts;
- no active legal hold overlaps the period;
- `tch.archive.cleanup.enabled=true` when `mode=DELETE`;
- a reason of at least 10 characters is supplied.

Dry run:

```bash
curl -sS -X POST 'http://127.0.0.1:8093/api/v1/platform/archive/ticket-purge' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "periodStart": "2025-01-01",
    "periodEnd": "2025-02-01",
    "batchSize": 5000,
    "mode": "DRY_RUN",
    "reason": "monthly verified ticket archive purge dry-run"
  }'
```

Execute:

```bash
curl -sS -X POST 'http://127.0.0.1:8093/api/v1/platform/archive/ticket-purge' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "periodStart": "2025-01-01",
    "periodEnd": "2025-02-01",
    "batchSize": 5000,
    "mode": "DELETE",
    "reason": "monthly verified ticket archive purge approved by ops"
  }'
```

Manual SQL fallback order:

1. verify `archive_object` for `sales_ticket`, `sales_ticket_line`, `sales_ticket_charge`;
2. verify lookup rows for ticket id/public code;
3. check legal holds for period and entity;
4. purge child rows before parent rows in small batches;
5. vacuum/analyze during a maintenance window.

Example for one period:

```sql
BEGIN;

-- Safety checks should return zero.
SELECT COUNT(*)
FROM archive_legal_hold
WHERE released_at IS NULL
  AND dataset_code IN ('sales_ticket', 'sales_ticket_line', 'sales_ticket_charge')
  AND daterange(period_start, period_end, '[)') && daterange(DATE '2025-01-01', DATE '2025-02-01', '[)');

-- Child rows first.
DELETE FROM sales_ticket_charge c
USING sales_ticket t
WHERE c.sales_ticket_id = t.id
  AND t.sold_at >= TIMESTAMPTZ '2025-01-01 00:00:00+00'
  AND t.sold_at <  TIMESTAMPTZ '2025-02-01 00:00:00+00';

DELETE FROM sales_ticket_line l
USING sales_ticket t
WHERE l.ticket_id = t.id
  AND t.sold_at >= TIMESTAMPTZ '2025-01-01 00:00:00+00'
  AND t.sold_at <  TIMESTAMPTZ '2025-02-01 00:00:00+00';

DELETE FROM sales_ticket
WHERE sold_at >= TIMESTAMPTZ '2025-01-01 00:00:00+00'
  AND sold_at <  TIMESTAMPTZ '2025-02-01 00:00:00+00';

COMMIT;

VACUUM (ANALYZE) sales_ticket;
VACUUM (ANALYZE) sales_ticket_line;
VACUUM (ANALYZE) sales_ticket_charge;
```

For large periods, replace single deletes with looped batches using IDs from a temp table.

## Normal Purge for Draws, Draw Results and Envers

Use the platform archive domain purge endpoint. It supports:

- `DRAW` (`draw`, bounded by `scheduled_at`);
- `DRAW_RESULT` (`draw_result`, bounded by `occurred_at`);
- `ENTITY_REVISION` (`revinfo` plus `*_aud`, bounded by `rev_timestamp`).

Dry run:

```bash
curl -sS -X POST 'http://127.0.0.1:8093/api/v1/platform/archive/domain-purge' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "dataset": "DRAW",
    "periodStart": "2025-01-01",
    "periodEnd": "2025-02-01",
    "batchSize": 5000,
    "mode": "DRY_RUN",
    "reason": "monthly verified draw archive purge dry-run"
  }'
```

Safety rules:

- `DELETE` requires `tch.archive.cleanup.enabled=true`;
- matching `archive_object` must be `VERIFIED` and row counts must match;
- active legal holds block purge;
- `DRAW` purge is refused while tickets still reference matching draws;
- `DRAW_RESULT` purge is refused while draws still reference matching results;
- `ENTITY_REVISION` deletes `draw_result_aud`, `limit_assignment_aud`, `seller_terminal_aud`
  before `revinfo`.

## Emergency: Ticket Lines Too Large

If `sales_ticket_line` is causing an outage:

1. stop write traffic or put cashier sale in maintenance mode;
2. create or confirm a legal hold review for affected period;
3. run archive for the old period;
4. verify `sales_ticket_line` object status is `VERIFIED`;
5. purge old child rows first in batches;
6. keep `sales_ticket` headers if dispute lookup must remain online;
7. vacuum/analyze.

Never truncate `sales_ticket_line` unless the business accepts losing line-level litigation evidence
for all affected tickets and a backup/export exists.

## Emergency: Spring Batch Metadata Too Large

Spring Batch metadata is operational history. It can be purged aggressively when it threatens DB
health.

Preferred retention: keep 30-90 days.

Purge completed/failed/stopped executions older than 30 days:

```sql
BEGIN;

CREATE TEMP TABLE purge_batch_job_execution_ids AS
SELECT job_execution_id
FROM batch.batch_job_execution
WHERE create_time < now() - interval '30 days'
  AND status NOT IN ('STARTING', 'STARTED', 'STOPPING');

CREATE TEMP TABLE purge_batch_step_execution_ids AS
SELECT step_execution_id
FROM batch.batch_step_execution
WHERE job_execution_id IN (SELECT job_execution_id FROM purge_batch_job_execution_ids);

DELETE FROM batch.batch_step_execution_context
WHERE step_execution_id IN (SELECT step_execution_id FROM purge_batch_step_execution_ids);

DELETE FROM batch.batch_step_execution
WHERE step_execution_id IN (SELECT step_execution_id FROM purge_batch_step_execution_ids);

DELETE FROM batch.batch_job_execution_context
WHERE job_execution_id IN (SELECT job_execution_id FROM purge_batch_job_execution_ids);

DELETE FROM batch.batch_job_execution_params
WHERE job_execution_id IN (SELECT job_execution_id FROM purge_batch_job_execution_ids);

DELETE FROM batch.batch_job_execution
WHERE job_execution_id IN (SELECT job_execution_id FROM purge_batch_job_execution_ids);

DELETE FROM batch.batch_job_instance i
WHERE NOT EXISTS (
  SELECT 1
  FROM batch.batch_job_execution e
  WHERE e.job_instance_id = i.job_instance_id
);

COMMIT;

VACUUM (ANALYZE) batch.batch_job_execution;
VACUUM (ANALYZE) batch.batch_step_execution;
VACUUM (ANALYZE) batch.batch_job_instance;
```

If the DB is in immediate danger and batch history can be discarded entirely:

```sql
TRUNCATE TABLE
  batch.batch_step_execution_context,
  batch.batch_step_execution,
  batch.batch_job_execution_context,
  batch.batch_job_execution_params,
  batch.batch_job_execution,
  batch.batch_job_instance
RESTART IDENTITY;
```

Use full truncate only during a maintenance window and only when no Spring Batch job is running.

## If Something Fails

Archive run fails:

- inspect `archive_run.error_message`;
- inspect `archive_object` statuses;
- do not cleanup;
- rerun the same period only after fixing the provider/storage issue.

Object is `INVALID`:

- keep hot data;
- check row count, checksum and object storage size;
- delete or quarantine the bad archive object only after creating a replacement verified object.

Partition cleanup refused:

- read `ineligibleReason` from plan;
- check legal holds;
- check `archive_object` row counts and statuses;
- do not force `DROP`.

Migration V247 fails:

- no business table rewrite has happened;
- fix helper function issue and rerun migration;
- if local dev Flyway checksum history is stale, repair the dev DB only, never production.

Batch purge accidentally removes needed history:

- recover from DB backup if needed;
- batch metadata is not a legal source of truth; business data must remain in domain tables or archive
  objects.
