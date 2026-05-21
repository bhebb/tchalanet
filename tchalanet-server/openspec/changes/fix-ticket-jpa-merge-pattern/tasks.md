# Tasks — Fix P0 JPA merge pattern for sensitive aggregates

## Phase 0 — P0 sensitive entity audit

- [x] Classify the bug as a backend P0 persistence pattern, not a ticket-only defect.
- [x] Add `p0-sensitive-entity-analysis.md` covering ticket, draw, drawresult, payout,
      terminal, outlet, session, limit policy, ledger, and offline sync.
- [ ] Decide the allowlist for explicit guarded SQL writers (`DrawResultWriterJdbcAdapter`,
      draw bulk lifecycle methods, append-only ledger insert).
- [x] Promote the rule into a backend persistence convention:
      existing sensitive rows must use managed mutation or guarded SQL, never detached merge.

## Phase 1 — Introduce the ticket mutator

- [ ] Create `core.sales.internal.infra.persistence.mapper.TicketAggregateMutator`
      (or extend `TicketJpaMapper` with a `applyTo(TicketJpaEntity managed, Ticket domain)`
      method, kept distinct from `toEntity(Ticket)` which stays for the create case).
- [ ] Implement `applyTo` for the ticket's mutable scalar fields: money, lifecycle, print
      state, settlement state, origin's mutable parts.
- [ ] Implement `applyTo` for lines (diff: keep, mutate mutable fields, add new, orphan
      remove deleted).
- [ ] Implement `applyTo` for charges (same diff shape).
- [ ] Assert immutable invariants in `applyTo`: throw if the caller tries to change
      `identity`, `context.outletId/terminalId/sellerUserId/salesSessionId/drawId`,
      `codes.ticketCode/publicCode/verificationCode`, `origin.channel`. These should
      never change; if they do it's a bug upstream.
- [ ] Maintain an explicit Ticket mutable/immutable field matrix in tests or helper
      assertions:
      immutable = tenantId, ticketId, initial outletId, initial terminalId,
      sellerId, salesSessionId, drawId, businessDate, codes, createdAt/createdBy,
      currency; mutable = sale status, print state, cancel/void traces, result state,
      settlement state, payout/paid refs where applicable, and audit update fields via
      listeners only.
- [ ] Unit-test the mutator without a database:
  - status transition PLACED → APPROVED preserves all immutable fields
  - marking printed updates `print.printedAt` only
  - line result update copies `resultStatus` + `payoutAmount`
  - removing a line marks it for orphan removal
  - adding a line on update inserts with correct tenant via the listener

## Phase 2 — Switch `TicketJpaAdapter#save` to load-managed-and-mutate

- [ ] In `TicketJpaAdapter#save`, branch on existence: `findWithLinesById + findWithChargesById`.
- [ ] If absent → INSERT path = `mapper.toEntity(ticket)` then `repository.save`.
- [ ] If present → mutate the managed entity via `TicketAggregateMutator.applyTo` and
      return `mapper.toDomain(managed)` directly (no explicit save needed; managed
      entity flushes at tx commit).
- [ ] Drop `lineRepository.findVersionsByTicketId` / `chargeRepository.findVersionsByTicketId`
      calls. Delete those methods + projection views from the repos.
- [ ] Drop `ticketRepository.findVersionById` call. Keep the method as private API for
      now in case other consumers need it (audit later).
- [ ] Remove `TicketLineJpaRepository.TicketLineVersionView` /
      `TicketChargeJpaRepository.TicketChargeVersionView` projection interfaces.
- [ ] Revert `TenantEntityListener#preUpdate` to the strict behavior: throw on missing
      entity tenant. Add an explicit code comment explaining why the auto-fill is gone.
- [ ] In `TicketJpaMapper#toEntity`, stop setting `tenantId` on lines/charges
      (defensive code becomes dead weight once the mutator owns updates).

## Phase 3 — Validate end-to-end

- [ ] Run E2E `tests/cashier/test_happy_path.py` — sell, preview, print, send, list,
      get all green.
- [ ] Run E2E `tests/cashier/test_single_ticket.py` — focused single-ticket print/send.
- [ ] Run the existing core integration tests for sales (`SellTicketCommandHandlerTest`,
      `CancelTicketCommandHandlerTest`, `ApproveTicketSaleCommandHandlerTest`,
      `RecordTicketPrintCommandHandlerTest`).
- [ ] Add/adjust command-level integration tests that exercise real handlers and assert
      both expected mutation and immutable-field preservation:
      `RecordTicketPrintCommandHandler`, `ApproveTicketSaleCommandHandler`,
      `CancelTicketCommandHandler`, `VoidTicket` flow if exposed, and
      `CreateTicketFromOfflineSubmissionCommandHandler` / offline sync promotion if applicable.
- [ ] Add a new integration test that does sell → record-print → cancel in sequence to
      exercise the multi-update flow.

## Phase 4 — Generalize to other P0 entities

- [ ] Change `SalesSessionWriterJpaAdapter#save` to load managed by tenant+id, then call
      `SalesSessionMapper#applyToEntity`; assert tenant/outlet/terminal/openedBy/businessDate
      are immutable after creation.
- [ ] Replace or constrain `DrawLifecycleJpaAdapter#save(Draw)`: update path must use a
      managed `DrawMutator` or be removed in favor of explicit guarded SQL lifecycle ports.
- [ ] Keep `DrawResultWriterJdbcAdapter` SQL-first, but verify all update paths increment
      `version` and preserve `CONFIRMED`/`OVERRIDDEN` guards.
- [ ] Harden `PayoutJpaWriterAdapter`: if `payout.id()` is present and no row exists,
      fail instead of creating; assert tenant/ticket/amount/currency immutability.
- [ ] Harden `JpaTerminalWriterAdapter`: tenant-scoped lookup for all writes; assert
      immutable outlet/kind fields; make block toggles explicit managed mutations.
- [ ] Complete `OutletPersistenceAdapter` operational mutators with managed entity updates
      for status/sales/payout/offline blocks.
- [ ] Harden `LimitAssignmentRepositoryAdapter`: managed mutation for existing assignments;
      scope/rule immutable; create-only through natural key path.
- [ ] Mark `JpaLedgerRepositoryAdapter` append-only: fail on duplicate id and never update
      an existing ledger entry.
- [ ] Verify offline sync adapters that use `toEntity(domain, existing)` always pass a
      managed existing entity on updates and fail on unexpected missing rows.

## Phase 5 — Convention enforcement

- [ ] Add an ArchUnit / convention check: core adapters must not call
      `save(mapper.toEntity(...))` for update-capable sensitive entities.
- [ ] Allowlist create-only and append-only paths explicitly.
- [ ] Allowlist guarded SQL writers explicitly when they include tenant/status/version
      predicates or equivalent natural-key/idempotency guards.

## Decisions to challenge with codex (record outcomes here)

- [ ] Was the rebuild-from-domain pattern chosen deliberately? If yes, what's the
      reasoning? If no, who introduced it and when?
- [ ] Should the domain `Ticket` carry the `version` value? Pro: more explicit. Con:
      leaks persistence concerns into the domain.
- [ ] Should we keep `TicketWriterPort#flushPending`? Yes for the SELL view-read trick,
      but rename to something like `flushPendingForViewRead`.
- [x] Same pattern audit for other aggregates — go now or defer? Go now for P0
      classification; implement in phases after Ticket and SalesSession.
