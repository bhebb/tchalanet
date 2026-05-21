# Fix TicketJpaAdapter merge pattern (rebuild-from-domain loses JPA state)

## Why

The `TicketJpaAdapter.save(Ticket)` flow rebuilds the full JPA entity graph from the
domain aggregate at every save:

```java
public Ticket save(Ticket ticket) {
    var entity = mapper.toEntity(ticket);  // FRESH entity, no Hibernate identity
    var saved = ticketRepository.save(entity);
    return mapper.toDomain(saved);
}
```

This works for INSERT (new ticket) because Hibernate has no previous state to compare
against. **It breaks on UPDATE** (e.g. `RecordTicketPrintCommand`, `ApproveTicketSale`,
`CancelTicket`) because the freshly-mapped entity has no:

- `tenant_id` set on lines/charges → `TenantEntityListener#preUpdate` throws
  `Missing entity tenant while updating TicketLineJpaEntity`
- `@Version` populated on ticket/lines/charges → Hibernate compares version=0 to the DB
  row's actual version → throws `ObjectOptimisticLockingFailureException`
- Audit columns (`created_at`, `created_by`) populated → potentially overwritten with
  nulls depending on Hibernate behavior

We've been patching these symptoms one at a time:

1. `TicketJpaMapper.toEntity` now writes `tenantId` on the parent and on every line/charge.
2. `TenantEntityListener#preUpdate` was made symmetric with `prePersist` and auto-fills
   `tenantId` from `TchContext` when missing on the entity.
3. `TicketJpaAdapter#save` now queries the current `@Version` of the parent + each line
   + each charge from DB and transplants them onto the fresh entities before save.

This is **band-aid on top of band-aid**. Every column with `updatable=false` semantics,
every audit/versioning field, every cascading collection becomes a new potential leak.
The design treats the JPA entity like a DTO instead of a managed persistence object.

## What Changes

Replace the rebuild-from-domain pattern with a **load-managed-then-mutate** pattern in
`TicketJpaAdapter#save`:

1. If the ticket exists in DB → load the managed `TicketJpaEntity` (with lines + charges).
2. Apply only the domain-level mutations from the input `Ticket` onto the managed entity
   (status transitions, print state, money, lifecycle, etc.).
3. For lines/charges, diff in place: keep existing rows, mutate their mutable fields,
   add new rows for new lines, remove rows for deleted lines (cascade orphan-removal).
4. Let Hibernate handle the dirty-checking + version bump + audit fields + tenant
   propagation automatically — no manual transplant.

For brand-new tickets, the flow stays as today: build a fresh entity, save once.

## Impact

### Removed (after this change)

- `TicketJpaMapper#toEntity` no longer sets `tenantId` on lines/charges manually.
- `TicketJpaAdapter#save` no longer queries versions via `findVersionById` /
  `findVersionsByTicketId`.
- `TicketLineJpaRepository#findVersionsByTicketId` + `TicketLineVersionView` —
  delete (only used by the band-aid).
- `TicketChargeJpaRepository#findVersionsByTicketId` + `TicketChargeVersionView` —
  delete.
- `TenantEntityListener#preUpdate` auto-fill on null entity tenant — revert to strict
  "throw if missing" (it's a real bug if the listener has to compensate; let's not hide
  future regressions).
- `TicketWriterPort#flushPending` + `SellTicketCommandHandler#ticketWriter.flushPending()`
  — required only because the fresh-entity flow needs an explicit flush before reading
  back through SQL views. With load-managed-and-mutate, the existing row is already
  visible (managed entity = DB row).

### Added

- A `TicketAggregateMutator` (or in-place merge inside the adapter) that takes a managed
  `TicketJpaEntity` + a domain `Ticket` and applies the diff field-by-field.
- New tests in `tchalanet-core/src/test/java/.../persistence/adapter/`:
  - update path (record print, cancel, approve) does not lose tenant/version/audit
  - new sale insert path still works (no managed entity exists yet)
  - line removal triggers orphan-removal
  - line add appends a fresh row with correct tenant

### Risks

- Lots of mutation surface: `TicketJpaMapper#toEntity` is the single rebuild point for
  every field. The mutator must mirror every mutable field. Missing one → state isn't
  saved → silent bug.
- Mitigation: write the mutator alongside the existing rebuild, run both in parallel in
  a feature-flagged path for one or two iterations, compare the DB output. Or replace
  outright and rely on integration tests covering each command handler.

### Decisions to challenge (asked of codex)

1. **Why was the rebuild-from-domain pattern chosen for an entity with @Version + envers
   audit + tenant invariants?** It's a known anti-pattern in JPA. Was it intentional
   (e.g. for offline replay symmetry) or accidental (mapper-driven design)?
2. **Why does the domain `Ticket` not carry a `version` field?** Other aggregates in
   the codebase that need optimistic locking expose it.
3. **Is `mapper.toEntity` ever called for a true "create from scratch" path** other than
   `SellTicketCommandHandler#handle`? If yes, the load-managed-and-mutate path has to
   special-case "no existing row" — easy, but worth listing.
4. **Are there other aggregates with the same pattern** (Draw, Outlet, Terminal,
   OfflineSubmission)? If so we should fix the pattern across the board, not just for
   Ticket.

## Out of Scope

- Rewriting the domain model.
- Splitting `Ticket` into write-side aggregate and read-side projection.
- Touching the SQL views (`sales_ticket_print_header_v` etc.).
