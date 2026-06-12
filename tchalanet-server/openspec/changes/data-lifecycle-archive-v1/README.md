# OpenSpec Change — data-lifecycle-archive-v1

## Goal

Define the data lifecycle strategy for Tchalanet so high-volume transactional data can grow without
degrading operational performance: partitioned hot tables, stats projections for dashboards, selective
Envers, functional audit log hardening, low-cost object-storage archival, and archive retrieval paths.

## Scope

`tchalanet-server` — `platform.archive`, `platform.audit`, `core.sales`, `core.payout`,
`platform.notification`, stats projections, PostgreSQL partitioning, Flyway migrations.

## Firebase note

The ongoing Keycloak → Firebase migration does **not** change the audit or archive schema.
`audit_log.actor_id` is always the internal `app_user.id` (UUID), never a provider subject.
`archive_run.requested_by` is likewise the internal user UUID.
Both identity providers resolve to the same internal UUID before any audit write.

## Specs included

```text
proposal.md   — full spec (data classes, audit taxonomy, partitioning, archive architecture)
tasks.md      — phase breakdown and implementation slices
```

## Related

- `docs/ARCHITECTURE.md`
- `docs/PLAYBOOK.md`
- `docs/conventions/persistence/rls.md`
- `docs/conventions/security_permissions.md`
- `docs/conventions/idempotency.md`
- `docs/conventions/api/routing_and_path.md`
- `docs/conventions/cache.md`
- `docs/conventions/event_model.md`
- `openspec/changes/tchalanet-keycloak-auth-change/` (identity migration in progress)
