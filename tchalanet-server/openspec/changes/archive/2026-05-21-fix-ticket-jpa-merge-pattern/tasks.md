# Tasks — Fix P0 JPA merge pattern for sensitive aggregates

## Phase 0 — P0 sensitive entity audit

- [x] Classify the bug as a backend P0 persistence pattern, not a ticket-only defect.
- [x] Add `p0-sensitive-entity-analysis.md` covering ticket, draw, drawresult, payout,
      terminal, outlet, session, limit policy, ledger, and offline sync.
- [x] Decide the allowlist for explicit guarded SQL writers (`DrawResultWriterJdbcAdapter`,
      draw bulk lifecycle methods, append-only ledger insert).
- [x] Promote the rule into a backend persistence convention:
      existing sensitive rows must use managed mutation or guarded SQL, never detached merge.

## Phase 1 — Introduce the ticket mutator

- [x] Create `core.sales.internal.infra.persistence.mapper.TicketAggregateMutator`
      (or extend `TicketJpaMapper` with a `applyTo(TicketJpaEntity managed, Ticket domain)`
      method, kept distinct from `toEntity(Ticket)` which stays for the create case).
- [x] Implement `applyTo` for the ticket's mutable scalar fields: money, lifecycle, print
      state, settlement state, origin's mutable parts.
- [x] Implement `applyTo` for lines (diff: keep, mutate mutable fields, add new, orphan
      remove deleted).
- [x] Implement `applyTo` for charges (same diff shape).
- [x] Assert immutable invariants in `applyTo`: throw if the caller tries to change
      `identity`, `context.outletId/terminalId/sellerUserId/salesSessionId/drawId`,
      `codes.ticketCode/publicCode/verificationCode`, `origin.channel`. These should
      never change; if they do it's a bug upstream.
- [x] Maintain an explicit Ticket mutable/immutable field matrix in tests or helper
      assertions:
      immutable = tenantId, ticketId, initial outletId, initial terminalId,
      sellerId, salesSessionId, drawId, businessDate, codes, createdAt/createdBy,
      currency; mutable = sale status, print state, cancel/void traces, result state,
      settlement state, payout/paid refs where applicable, and audit update fields via
      listeners only.
- [x] Unit-test the mutator without a database:
  - status transition PLACED → APPROVED preserves all immutable fields
  - marking printed updates `print.printedAt` only
  - line result update copies `resultStatus` + `payoutAmount`
  - removing a line marks it for orphan removal
  - adding a line on update inserts with correct tenant via the listener

## Phase 2 — Switch `TicketJpaAdapter#save` to load-managed-and-mutate

- [x] In `TicketJpaAdapter#save`, branch on existence: `findWithLinesById + findWithChargesById`.
- [x] If absent → INSERT path = `mapper.toEntity(ticket)` then `repository.save`.
- [x] If present → mutate the managed entity via `TicketAggregateMutator.applyTo` and
      return `mapper.toDomain(managed)` directly (no explicit save needed; managed
      entity flushes at tx commit).
- [x] Drop `lineRepository.findVersionsByTicketId` / `chargeRepository.findVersionsByTicketId`
      calls. Delete those methods + projection views from the repos.
- [x] Drop `ticketRepository.findVersionById` call. Keep the method as private API for
      now in case other consumers need it (audit later).
- [x] Remove `TicketLineJpaRepository.TicketLineVersionView` /
      `TicketChargeJpaRepository.TicketChargeVersionView` projection interfaces.
- [x] Revert `TenantEntityListener#preUpdate` to the strict behavior: throw on missing
      entity tenant. Add an explicit code comment explaining why the auto-fill is gone.
- [x] In `TicketJpaMapper#toEntity`, stop setting `tenantId` on lines/charges
      (defensive code becomes dead weight once the mutator owns updates).

## Phase 3 — Validate end-to-end

- [ ] Run E2E `tests/cashier/test_happy_path.py` — sell, preview, print, send, list,
      get all green.
      Attempted 2026-05-21 with `uv run pytest tests/cashier/test_single_ticket.py
      tests/cashier/test_happy_path.py -v -s`; blocked by local Keycloak unavailable
      (`https://auth.localtest.me/...` connection refused) and Docker daemon unavailable.
- [ ] Run E2E `tests/cashier/test_single_ticket.py` — focused single-ticket print/send.
      Attempted 2026-05-21 with the command above; blocked by local Keycloak unavailable.
- [x] Run the existing core integration tests for sales (`SellTicketCommandHandlerTest`,
      `CancelTicketCommandHandlerTest`, `ApproveTicketSaleCommandHandlerTest`,
      `RecordTicketPrintCommandHandlerTest`).
      No tests with those exact names exist in `tchalanet-core`; ran the targeted
      replacement coverage:
      `./mvnw -pl tchalanet-core -Dtest=TicketAggregateMutatorTest,TicketCommandMutationCoverageTest,SensitiveJpaUpdateConventionTest test`.
- [x] Add/adjust command-level integration tests that exercise real handlers and assert
      both expected mutation and immutable-field preservation:
      `RecordTicketPrintCommandHandler`, `ApproveTicketSaleCommandHandler`,
      `CancelTicketCommandHandler`, `VoidTicket` flow if exposed, and
      `CreateTicketFromOfflineSubmissionCommandHandler` / offline sync promotion if applicable.
- [ ] Add a new integration test that does sell → record-print → cancel in sequence to
      exercise the multi-update flow.

## Phase 4 — Generalize to other P0 entities

- [x] Change `SalesSessionWriterJpaAdapter#save` to load managed by tenant+id, then call
      `SalesSessionMapper#applyToEntity`; assert tenant/outlet/terminal/openedBy/businessDate
      are immutable after creation.
- [x] Replace or constrain `DrawLifecycleJpaAdapter#save(Draw)`: update path must use a
      managed `DrawMutator` or be removed in favor of explicit guarded SQL lifecycle ports.
- [x] Keep `DrawResultWriterJdbcAdapter` SQL-first, but verify all update paths increment
      `version` and preserve `CONFIRMED`/`OVERRIDDEN` guards.
- [x] Harden `PayoutJpaWriterAdapter`: if `payout.id()` is present and no row exists,
      fail instead of creating; assert tenant/ticket/amount/currency immutability.
- [x] Harden `JpaTerminalWriterAdapter`: tenant-scoped lookup for all writes; assert
      immutable outlet/kind fields; make block toggles explicit managed mutations.
- [x] Complete `OutletPersistenceAdapter` operational mutators with managed entity updates
      for status/sales/payout/offline blocks.
- [x] Harden `LimitAssignmentRepositoryAdapter`: managed mutation for existing assignments;
      scope/rule immutable; create-only through natural key path.
- [x] Mark `JpaLedgerRepositoryAdapter` append-only: fail on duplicate id and never update
      an existing ledger entry.
- [ ] Verify offline sync adapters that use `toEntity(domain, existing)` always pass a
      managed existing entity on updates and fail on unexpected missing rows.

## Phase 5 — Convention enforcement

- [x] Add an ArchUnit / convention check: core adapters must not call
      `save(mapper.toEntity(...))` for update-capable sensitive entities.
- [x] Allowlist create-only and append-only paths explicitly.
- [x] Allowlist guarded SQL writers explicitly when they include tenant/status/version
      predicates or equivalent natural-key/idempotency guards.

## Decisions to challenge with codex (record outcomes here)

- [x] Was the rebuild-from-domain pattern chosen deliberately? If yes, what's the
      reasoning? If no, who introduced it and when?
      Outcome: no explicit ADR or code comment found showing this was deliberate.
      The change treats it as an unsafe persistence shortcut and replaces it with
      managed mutation plus convention guard.
- [x] Should the domain `Ticket` carry the `version` value? Pro: more explicit. Con:
      leaks persistence concerns into the domain.
      Outcome: no. Keep optimistic locking/version in persistence; the domain ticket
      remains persistence-agnostic.
- [x] Should we keep `TicketWriterPort#flushPending`? Yes for the SELL view-read trick,
      but rename to something like `flushPendingForViewRead`.
      Outcome: keep for now to avoid widening the change; rename/deprecate in a
      follow-up because it is orthogonal to the P0 merge fix.
- [x] Same pattern audit for other aggregates — go now or defer? Go now for P0
      classification; implement in phases after Ticket and SalesSession.
