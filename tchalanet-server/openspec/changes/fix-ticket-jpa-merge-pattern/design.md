# Design — Fix TicketJpaAdapter merge pattern

## Current state (what's broken and why)

### The flow today

```
SellTicketCommandHandler (new ticket)
    └── ticketWriter.save(ticket)
            └── TicketJpaAdapter.save
                    ├── mapper.toEntity(ticket)        ← FRESH entity, ID set, version=0
                    ├── (transplant tenant on lines/charges via mapper)
                    ├── (transplant @Version via lineRepo.findVersionsByTicketId, etc.)
                    └── ticketRepository.save(entity)  ← Hibernate merge

RecordTicketPrintCommandHandler (update existing ticket)
    └── reader.getRequired(ticketId)                   ← Managed load
    └── ticket.markPrinted(...)                        ← Domain mutation
    └── ticketWriter.save(ticket)                      ← SAME path as above
            └── TicketJpaAdapter.save
                    ├── mapper.toEntity(ticket)        ← FRESH entity AGAIN
                    │                                     (loses Hibernate identity, version, tenant on children, audit cols)
                    ├── transplant fields …
                    └── ticketRepository.save(entity)  ← merge fights with DB state
```

### The root issue

Spring Data JPA's `save()` does `merge()` when the entity has a non-null ID. `merge()`
copies the detached entity's state onto a fresh managed copy. The version mismatch and
audit/tenant nulls all come from the fact that **the detached entity passed to merge has
zero knowledge of the row in DB** because the mapper just made it up from the domain.

Every time we add a column or a `@PreUpdate` listener invariant, we have a new bug to
patch in the mapper or the adapter. We've already patched:

| Symptom                                                       | Patch                                                     |
| ------------------------------------------------------------- | --------------------------------------------------------- |
| `Missing entity tenant while updating TicketLineJpaEntity`    | `mapper.toEntity` sets `tenantId` on lines/charges        |
| Same on the parent                                            | `mapper.toEntity` sets `tenantId` on the ticket           |
| Future regression if anyone adds a tenant-bearing entity      | `TenantEntityListener#preUpdate` auto-fills from context  |
| `ObjectOptimisticLockingFailureException` on ticket update    | `TicketJpaAdapter.save` queries + transplants version     |
| Same on cascaded line update                                  | `TicketJpaAdapter.save` queries + transplants line versions |
| Same will hit charges                                         | Same patch for charges                                    |
| Future: `created_at`/`created_by` overwrites                  | not yet hit, but it WILL                                  |
| Future: any new `@Version`-bearing child entity               | a new findVersionsByTicketId for that table               |

This list grows linearly with every new column. The architecture is wrong, not the
individual columns.

## Proposed design

### Replace `save(Ticket)` with load-managed-and-mutate

```
TicketJpaAdapter.save(Ticket ticket):
    var id = ticket.identity().id().value()
    var existing = ticketRepository.findWithLinesById(id)
                        .ifPresent(e -> ticketRepository.findWithChargesById(id))

    if existing.isEmpty():
        // INSERT path: same as today, but limited to the new-aggregate case
        var entity = mapper.toEntity(ticket)
        return mapper.toDomain(ticketRepository.save(entity))

    // UPDATE path: mutate the managed entity in place
    var managed = existing.get()
    mutator.applyTo(managed, ticket)   // copies mutable fields from domain to managed
    return mapper.toDomain(managed)    // Hibernate dirty-checking handles the rest
```

### The mutator's responsibility

`TicketAggregateMutator` (or methods on the mapper) copies from the domain aggregate to
the managed entity only the **mutable** fields:

| Field family   | Behavior                                                                   |
| -------------- | -------------------------------------------------------------------------- |
| `identity`     | read-only — assert match, never overwrite                                  |
| `context`      | read-only after creation — assert match                                    |
| `codes`        | read-only after creation                                                   |
| `money`        | mutable (e.g. settlement adjustments) — copy                               |
| `lifecycle`    | mutable (status transitions) — copy                                        |
| `origin`       | read-only — assert match                                                   |
| `print`        | mutable (mark printed, set last printed at) — copy                         |
| `lines`        | mutable result/payout fields only (result_status, payout_amount) — diff   |
| `charges`      | mutable — diff                                                             |
| `audit`        | NEVER touched (handled by listeners)                                       |
| `tenantId`     | NEVER touched (immutable, listener handles it)                             |
| `version`      | NEVER touched (Hibernate handles it)                                       |

The mutator is the single source of truth for "what changes between two states of a
ticket". It's a small focused class that can be unit-tested without a database.

### Line/charge diff

For `lines`:

```
existingById = managed.getLines().stream().collect(toMap(TicketLineJpaEntity::getId))
for domainLine in ticket.lines():
    if existingById.containsKey(domainLine.id().value()):
        applyMutableFields(existingById.get(domainLine.id().value()), domainLine)
        existingById.remove(domainLine.id().value())
    else:
        managed.addLine(mapper.toLineEntity(domainLine))   // INSERT new line
// remaining entries in existingById → lines deleted from the domain
for orphan in existingById.values():
    managed.removeLine(orphan)   // cascade orphan-removal handles delete
```

Same shape for charges.

### Why this fixes everything in one go

- `tenantId` never re-set → no `preUpdate` failure (listener strict mode reinstated)
- `@Version` is the one managed by Hibernate on the loaded entity → no transplant
- `created_at`/`created_by` untouched → audit listener happy
- New columns added to entities → automatically managed, no mapper change needed
- Optimistic lock genuinely guards against concurrent edits instead of being defeated
  by our transplant

## Alternatives considered

### A. Keep rebuilding, patch every column

What we're doing today. Cost grows linearly with every column/listener added.

### B. Carry `version` on the domain `Ticket`

Less invasive than option (C), but still requires us to manually feed every column from
the domain back to the entity. Doesn't solve the audit / orphan-removal / `updatable=false`
quirks. Half-measure.

### C. Load-managed-and-mutate (recommended, this proposal)

Properly aligns with how JPA was designed to be used. One-time refactor cost; nothing
to maintain afterwards.

### D. Switch to JDBC / explicit SQL

Throws out JPA. Out of scope.

## Migration / compatibility

- `SellTicketCommandHandler#flushPending()` call can be removed: the managed entity is
  already visible to SQL views since the read-then-mutate-then-save flow goes through
  Hibernate's dirty-checking which flushes at commit boundary. We may still need a
  targeted flush before reading the projection in the same transaction as a CREATE —
  that case is unchanged.
- All consumers of `ticketWriter.save(...)` keep the same signature.
- Existing tests should still pass; if any test asserts that `created_at` is null after
  `save`, that test was lying.

## Open design questions

1. Should `TicketAggregateMutator` live in `infra/persistence` or in
   `application/service`? It's persistence-flavored (it knows JPA entities), so
   probably `infra/persistence/mapper/` next to `TicketJpaMapper`.
2. Should it be a separate class or a method on the mapper? Separate class better
   isolates the mutation rules from the create-rebuild rules.
3. Do we keep `TicketWriterPort#flushPending` for the SELL flow's view-read trick?
   Likely yes, but document it as "view-read flush helper" not "transplant escape hatch".
