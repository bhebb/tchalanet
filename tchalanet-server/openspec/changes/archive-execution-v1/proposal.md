# Proposal — platform.archive execution V1

## Why

The archive page exists, but the backend archive execution path is still a scaffold. High-volume
tables such as `ticket`, `ticket_line`, `draw`, `draw_result`, audit tables, `_aud` tables and batch
logs will grow quickly. We need the archive rules now, before production data volume makes cleanup
risky.

## What

- Implement `platform.archive` as the archive orchestrator.
- Keep dataset-specific reads in the owning modules through `ArchiveDatasetProvider`.
- Use monthly PostgreSQL partitions for high-volume append-heavy data.
- Export verified old periods to compressed `jsonl.gz` archive objects.
- Keep `archive_lookup_index` online for ticket, public code, payout, audit and legal lookup.
- Support legal hold so disputed data cannot be purged or partition-cleaned.
- Add read-from-archive as the default access path; restore tables are only for exceptional legal or
  operational investigations.

## Retention Defaults

| Dataset | Hot retention | Archive access | Notes |
|---|---:|---:|---|
| `ticket`, `ticket_line`, charges, snapshots | 6-12 months | lookup by ticket id/public code | 12 months default; tenant policy can reduce after review |
| `draw`, `draw_result` | 12 months | lookup by draw/date/game | keep enough online for normal reports |
| `audit_log`, batch logs | 12 months | lookup by actor/entity/action/date | partitioned; no dashboard read by default |
| `_aud` tables | table-specific | legal/admin only | avoid Envers on high-volume transactional rows |

## Impact

- Requires Flyway changes for partition support, archive metadata and legal hold state.
- Requires API hardening for `/api/v1/platform/archive/**` and `/api/v1/admin/archive/**`.
- Requires focused integration tests for archive run, lookup, tenant isolation and legal hold.

## Non-goals

- No data warehouse.
- No tenant-facing archive browser in V1.
- No automatic deletion before checksum, row-count verification and legal-hold checks pass.
