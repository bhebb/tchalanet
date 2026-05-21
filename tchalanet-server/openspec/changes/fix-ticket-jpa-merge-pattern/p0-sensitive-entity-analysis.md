# P0 Sensitive Entity Analysis

## Problem class

The production risk is not "Ticket has a bad mapper". The risk is broader:

```text
domain aggregate snapshot
        │
        ▼
fresh JPA entity with existing id
        │
        ▼
repository.save(...) / Hibernate merge
        │
        ├─ loses managed @Version state
        ├─ can null or overwrite updatable=false/audit fields
        ├─ can miss tenant_id on child rows
        ├─ can resurrect deleted/missing rows
        └─ can replace lifecycle state without a DB-level concurrency guard
```

Any P0 entity with tenant ownership, money, lifecycle status, audit history, or
settlement/session controls must not rely on detached `merge` as its update mechanism.

## Rule

Sensitive persistence writes must choose one of three explicit patterns.

| Pattern | Allowed for | Rule |
| --- | --- | --- |
| Create-only mapping | new rows only | `mapper.toEntity(domain)` is allowed only when the row cannot already exist. |
| Managed mutation | JPA updates | Load the existing entity in the same transaction, assert immutable fields, mutate only allowed fields, let Hibernate own audit/version. |
| Guarded SQL | bulk/status/concurrency updates | Use explicit `where` predicates for tenant/status/version/natural key and update `version = version + 1` when the table has a version column. |

Forbidden for P0: `mapper.toEntity(domain)` followed by `save` where the domain object
may represent an existing row.

## Entity audit

| Area | Current signal | Risk | Required solution |
| --- | --- | --- | --- |
| Ticket / line / charge | `TicketJpaAdapter#save` rebuilds entity graph, then transplants versions | P0 active bug: tenant/version/audit/orphan-removal corruption | Implement `TicketAggregateMutator`; update path loads managed ticket with lines/charges and diffs collections in place. |
| Draw | `DrawLifecycleJpaAdapter#save` does `repo.save(mapper.toEntity(draw))`; bulk open/close already use SQL | P0 for corrections/result application/cancel lifecycle if JPA save updates existing rows | Replace JPA save update path with managed `DrawMutator` or remove generic `save(Draw)` in favor of guarded SQL lifecycle methods. |
| DrawResult | Writer uses JDBC upsert and `markAsOverridden` repository update; entity is audited/versioned but not tenant-scoped | P0 if future JPA save is introduced; current SQL mostly avoids merge | Keep writer SQL-first; ensure every update increments version and has final/overridden guards. Add a regression guard preventing generic JPA save for updates. |
| Payout | Writer loads existing entity or creates new, then `updateEntity` | Mostly correct shape, but absent existing id creates a new entity with caller-supplied id | Harden: if `payout.id()` is present and not found, fail; assert immutable tenant/ticket/amount/currency after creation; mutate only workflow fields. |
| Terminal | Writer loads existing entity or creates new; block toggles use managed references | Mostly correct shape | Harden: tenant-scoped loads for all writes, assert outlet/kind immutable where appropriate, ensure block methods are transactional and reason validation is symmetric. |
| Outlet | Writer loads existing entity or creates new; several operational mutators are empty | Persistence shape is mostly correct, but lifecycle/block operations are not implemented | Implement managed mutators for status/sales/payout/offline blocks with tenant-scoped lookup; no rebuild update path. |
| SalesSession | `SalesSessionWriterJpaAdapter#save` calls `mapper.toEntity(session)` then `repo.save` | P0 same class as Ticket: open/close/finalize updates can lose version/audit/tenant assumptions | Change `save` to load managed by tenant+id; use existing `SalesSessionMapper#applyToEntity`; assert immutable tenant/outlet/terminal/openedBy/businessDate. |
| LimitAssignment | `repo.save(mapper.toEntity(assignment))` | P0/P1 depending on policy enforcement path; can corrupt active/deleted policy state | Use managed mutation for existing assignments; create-only for new natural keys; assert scope/rule immutable, mutate effective dates/status/deleted fields only. |
| LedgerEntry | `repository.save(mapper.toEntity(entry))` | Safe only if truly append-only | Enforce append-only: fail if id already exists, never use save as update. |
| Offline sync records | Adapters pass an existing entity into mapper (`toEntity(domain, existing)`) | Better shape, still sensitive because offline replay is idempotent/concurrent | Verify each adapter loads existing managed entity; fail on missing existing id where an update is expected; keep payload hash/natural-key guards. |

## Recommended implementation sequence

1. Fix active P0 bug: Ticket managed mutation, strict tenant listener, remove version transplant.
2. Fix same active anti-pattern: SalesSession managed mutation.
3. Fix Draw generic `save(Draw)`: either managed mutation or delete/replace with explicit guarded lifecycle ports.
4. Harden Payout, Terminal, Outlet, LimitAssignment: no "id present but missing row means create"; assert immutable identity/context fields.
5. Add convention tests:
   - flag core adapters that call `save(mapper.toEntity(...))` on `BaseTenantEntity`/`BaseEntity` types with inherited `@Version`;
   - whitelist append-only create paths and explicit guarded SQL writers;
   - fail when a P0 writer has a generic rebuild update path.

## Design stance

This should become a backend persistence invariant, not a Ticket-specific patch:

```text
P0 update = managed entity mutation OR guarded SQL
P0 create = new entity mapping
P0 append-only = insert once, fail on duplicate id
P0 never = detached merge of a rebuilt existing row
```
