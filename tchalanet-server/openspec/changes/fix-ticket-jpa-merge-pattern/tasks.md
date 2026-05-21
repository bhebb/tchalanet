# Tasks — Fix TicketJpaAdapter merge pattern

## Phase 1 — Introduce the mutator

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
- [ ] Add a new integration test that does sell → record-print → cancel in sequence to
      exercise the multi-update flow.

## Phase 4 — Generalize (optional, decide after Phase 3)

- [ ] Audit other aggregates for the same anti-pattern:
  - `core.draw` — does the draw adapter rebuild from domain on save?
  - `core.outlet` / `core.terminal` — same question.
  - `core.offlinesync` (grants, submissions) — same question.
- [ ] If any of them share the pattern, file a follow-up change.
- [ ] Add an ArchUnit / convention check: any `XxxJpaAdapter` implementing a write port
      whose target entity extends `BaseTenantEntity` AND has `@Version` MUST use the
      load-managed-and-mutate pattern (smoke test: grep for `mapper.toEntity` + immediate
      `save` in adapters).

## Decisions to challenge with codex (record outcomes here)

- [ ] Was the rebuild-from-domain pattern chosen deliberately? If yes, what's the
      reasoning? If no, who introduced it and when?
- [ ] Should the domain `Ticket` carry the `version` value? Pro: more explicit. Con:
      leaks persistence concerns into the domain.
- [ ] Should we keep `TicketWriterPort#flushPending`? Yes for the SELL view-read trick,
      but rename to something like `flushPendingForViewRead`.
- [ ] Same pattern audit for other aggregates — go now or defer?
