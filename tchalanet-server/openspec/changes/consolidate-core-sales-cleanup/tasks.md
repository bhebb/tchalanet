# Tasks

## 0. Consolidation guardrails

- [x] Treat this change as the authoritative sales cleanup plan.
- [x] Do not implement from superseded sales OpenSpecs unless the item is restated here.
- [x] Keep implementation in narrow passes and avoid broad unrelated refactors.
- [x] Preserve current HTTP routes where possible while moving Java ownership to the correct package.
- [x] Follow pre-go-live migration policy: update existing migrations/views/audit tables unless a new migration is explicitly approved.
- [x] Before implementation, read and apply:
  - [x] `docs/ARCHITECTURE.md`.
  - [x] `docs/PLAYBOOK.md`.
  - [x] `docs/NAMING.md`.
  - [x] `docs/conventions/command_query_handlers.md`.
  - [x] `docs/conventions/api/web_api.md`.
  - [x] `docs/conventions/api/pagination.md`.
  - [x] `docs/conventions/api/routing_and_path.md`.
  - [x] `docs/conventions/api/api_response.md`.
  - [x] `docs/conventions/typed_ids.md`.
  - [x] `docs/conventions/inter_domain_calls.md`.
  - [x] `docs/conventions/event_model.md`.
  - [x] `docs/conventions/persistence/persistence.md`.
  - [x] `docs/conventions/persistence/jpa_entities.md`.
  - [x] `docs/conventions/persistence/rls.md`.
  - [x] `docs/conventions/batch/batch.md` if touching batch/schedulers/ops.
- [x] Use `core.draw` as the model for core package structure and ownership.
- [x] Use `features.ops` as the model for feature/BFF orchestration.

## 0.5 Communication/document alignment

- [x] Apply implementation order:
  - [x] 1. `tchalanet-edge-service`: replace `/internal/notifications/send` with `/internal/messages/send` and enforce HMAC.
  - [x] 2. `tchalanet-server`: extract `common.communication`.
  - [x] 3. `tchalanet-server`: extract `common.document`.
  - [x] 4. `tchalanet-server`: continue `core.sales` cleanup.
  - [x] 5. `tchalanet-server`: implement/finish features (`features.receipt`, `features.cashier`, `features.ticketverify`).
- [x] Read `tchalanet-server/openspec/changes/p0-extract-common-communication-document`.
- [x] Read `tchalanet-edge-service/openspec/changes/p1-rename-notification-route-to-messages`.
- [x] Treat `p0-extract-common-communication-document` as authoritative for:
  - [x] `common.communication`.
  - [x] `common.document`.
  - [x] `features.receipt` naming.
  - [x] the ban on `features.cashier -> features.receipt`.
- [x] Treat `p1-rename-notification-route-to-messages` as authoritative for:
  - [x] canonical edge route `/internal/messages/send`.
  - [x] removed legacy edge route `/internal/notifications/send`.
  - [x] identical HMAC raw-body verification behavior.
- [x] Remove the earlier local exception allowing `features.ticketdelivery -> features.ticketreceipt`.
- [x] Remove/migrate any existing `features.ticketdelivery` code; external sending belongs to `common.communication`.
- [x] Add/keep validation tests that cashier code does not import receipt feature packages once the common primitives exist.

## 1. P0 bugs

- [x] Fix `TicketSettlementQueryRepository.existsPending` SQL: `t.status` -> `t.sale_status`.
- [x] Fix `TicketSettlementQueryRepository.countPending` SQL: `t.status` -> `t.sale_status`.
- [x] Add focused tests for pending settlement query SQL behavior.
- [x] Update `TicketSettlementJpaRepository.findBatchForDrawWithLines` to accept `Pageable`.
- [x] Update `TicketSettlementJpaAdapter.findNextBatchForDraw` to pass page size to repository and remove Java-side `.limit(pageSize)`.
- [x] Add focused test proving settlement batch does not fetch beyond requested page size.
- [x] Update `SalesLedgerListener` ordering to `alreadyProcessed -> commandBus.send -> markProcessedIfAbsent`.
- [x] Add tests for ledger dispatch failure not marking processed and success marking processed.

## 2. Ticket receipt feature boundary

- [x] Create interim `features.ticketreceipt` following existing feature format (`TicketReceiptController`, `app/`, `model/` only as needed).
- [x] Rename or migrate interim `features.ticketreceipt` to target `features.receipt`.
- [x] Move `GetTicketPrintPdfQueryHandler` behavior out of `core.sales` into a receipt feature.
- [x] Move `GetTicketPrintEscPosQuery` / handler behavior out of `core.sales` into a receipt feature.
- [x] Move `GetTicketQrPngQuery` / handler behavior out of `core.sales` into a receipt feature.
- [x] Move generic renderers from `common.print` / `common.qr` to `common.document` under the P0 communication/document extraction.
- [x] Keep ticket-specific receipt formatter/read model in `core.sales`, not in `common.document`.
- [x] Keep or adapt current routes:
  - [x] `GET /tenant/tickets/{ticketId}/print.pdf`
  - [x] `GET /tenant/tickets/{ticketId}/print.escpos`
  - [x] `GET /tenant/tickets/{ticketId}/qr`
- [x] Remove or deprecate `/tenant/tickets/{ticketId}/print` base64 `text/plain`.
- [x] Replace low-level print not-found exceptions with explicit optional/application-level not-found handling.
- [ ] Add receipt tests:
  - [ ] PDF endpoint returns bytes and `no-store`.
  - [ ] ESC/POS endpoint returns bytes and `no-store`.
  - [ ] QR endpoint returns PNG and `no-store`.
  - [ ] Missing ticket maps to not-found.

## 3. Communication, receipt, and verify alignment

- [x] Extract external sending from `core.notification` into `common.communication`.
- [ ] Ensure `core.notification` handles in-app notifications only.
- [x] Remove `features.ticketdelivery` as a target feature/package.
- [x] If an external message needs a receipt link or attachment, build it from `core.sales` receipt/read model plus `common.document`, not from receipt feature service.
- [x] Ensure Spring targets edge `/internal/messages/send` through communication config once P1 is available.
- [x] Remove any Spring fallback to `/internal/notifications/send`.
- [ ] Keep `features.ticketverify` JSON only.
- [ ] Ensure public verify response does not expose internal UUIDs.
- [ ] Ensure public payout status uses actual sale/result/settlement state.
- [ ] Add/adjust tests for outbound communication orchestration and public verify masking/status.

## 4. Ticket controller split

- [x] Replace `TicketController` with focused controllers:
  - [x] `TicketSalesController`.
  - [x] `TicketQueryController`.
  - [x] `AdminTicketController`.
- [x] Move public verification out of `core.sales` controller ownership if any remains.
- [x] Move print/QR out of `core.sales` controller ownership.
- [x] Replace `ResponseStatusException` in ticket controllers with project-standard problem handling.
- [x] Move details not-found behavior out of controller `null` checks.
- [x] Ensure controllers remain thin: validation/context/mapping/CommandBus/QueryBus only.
- [x] Ensure list endpoints use `@TchPaging`, allowed sort allowlist, default sort, `TchPageRequest`, and return `ApiResponse<TchPage<...>>`.
- [x] Ensure controller mappings use logical paths only (`/tenant`, `/admin`, `/public`) and do not include `/api/v1`.
- [x] Ensure typed IDs are used in controller signatures and no raw UUID parsing appears in controllers.
- [ ] Add web/controller tests for split routes and permissions.

## 5. Repository and persistence cleanup

- [x] Extract inline `JpaTicketRepositoryAdapter.search()` specifications into `TicketSpecifications`.
- [x] Remove hardcoded sort from `JpaTicketRepositoryAdapter.search()` and respect the incoming `Pageable`.
- [x] Replace `findByPublicCode` without `DeletedAtIsNull` with the soft-delete-safe method.
- [ ] Segment `SpringTicketJpaRepository` when the rule-of-3 is met:
  - [ ] basic CRUD/aggregate access.
  - [ ] search/list access.
  - [ ] close-day stats/count access.
  - [ ] settlement batch access.
- [ ] Move `JpaTicketRepositoryAdapter.exportDailySalesCsv` to `features.reporting` or a dedicated export slice.
- [ ] Replace CSV `RuntimeException` with project-standard problem/error mapping at the feature boundary.
- [ ] Ensure ports/adapters follow naming conventions (`XxxReaderPort`, `XxxWriterPort`, `XxxJpaAdapter`, `XxxJdbcAdapter`, `XxxJpaRepository`).
- [ ] Ensure persistence adapters contain no business decisions, no web DTOs, and no feature orchestration.
- [ ] Ensure Spring `Page` remains internal and is converted to `TchPage`.
- [ ] Add repository/search tests for soft-delete filtering, pageable sort, and CSV/export owner.

## 6. Domain/application cleanup

- [x] Add `ApprovalRequestId` typed wrapper.
- [x] Replace raw `UUID approvalRequestId` outside persistence with `ApprovalRequestId`.
- [x] Update `SellTicketResult` and web response mapping for typed approval id.
- [ ] Decide currency strategy and apply consistently outside persistence.
- [x] Change `TicketEntity.currency` length to `3`.
- [x] Sync `ticket_aud.currency` length if required by migration/audit schema.
- [x] Add comment documenting why `TicketEntity` uses `@OneToMany` for `TicketLine` aggregate persistence.
- [x] Remove redundant `Ticket` alias getters (`id()`, `tenantId()`, `terminalId()`, `drawId()`) and migrate callers to Lombok getters.
- [x] Rename ambiguous `Ticket.forceResult(payout, when)` overload to `overrideAsResulted(payout, when)` or remove it.
- [ ] Remove `ApiResponseContext` usage from `SellTicketCommandHandler`; return notices through `SellTicketResult`.
- [ ] Replace direct `ResolveAutonomyPolicyService` injection in sales policy with `QueryBus` autonomy query.
- [x] Replace `SecurityException` in session validation with stable conflict semantics (`session.not_open`, `outlet.sales_blocked`).
- [ ] Verify `TicketLine.oddsSnapshot` coherence for fixed-payout games and add tests.
- [ ] Ensure every write command has a `record` command model, one `@UseCase` handler, and `@TchTx`.
- [ ] Ensure every query has a `record` query model, one `@UseCase` handler, side-effect-free behavior, and projection output.
- [ ] Ensure domain methods enforce ticket invariants and do not depend on Spring, JPA, web, commands, or features.

## 7. Cashier BFF

- [x] Create/prepare `features.cashier` following existing feature format.
- [ ] Add or specify `GET /tenant/cashier/dashboard`.
- [ ] Add or specify `GET /tenant/cashier/sell-context`.
- [ ] Add or specify current/open/close session UX endpoints delegating to `core.session`.
- [x] Add or specify `POST /tenant/cashier/sell` delegating to `core.sales`.
- [x] Return action-oriented sell response with receipt and communication affordances.
- [x] Ensure cashier does not import or call `features.receipt` / `features.ticketreceipt`.
- [x] For inline receipt rendering, use `core.sales` receipt/read model plus `common.document`.
- [x] For inline external communication, use `common.communication`.
- [x] Ensure `features.cashier` follows `features.ops` style: orchestration only, no business command handlers, no direct core persistence writes.
- [ ] Add tests proving cashier feature delegates and does not calculate sales invariants.

## 8. Database verification

- [ ] Verify whether `ticket_line` has RLS enabled.
- [ ] If missing, add `ticket_line` RLS to the existing RLS migration.
- [ ] Verify `ticket.public_code` lookup/index behavior.
- [ ] Verify `ticket.draw_id` index coverage.
- [ ] Decide whether `ticket.created_by` needs an index or whether `ticket.user_id` is the canonical query column.
- [ ] Verify `ticket_line.ticket_id` index.
- [ ] Check `V108__create_read_views.sql` after any ticket/table mapping change.

## 9. Documentation and validation

- [ ] Update `src/main/java/com/tchalanet/server/core/sales/DOMAIN_SALES.md`.
- [ ] Add/update near-code docs for:
  - [ ] `features.receipt`.
  - [ ] `features.ticketverify`.
  - [ ] `features.cashier`.
- [ ] Reference `p0-extract-common-communication-document` instead of documenting a separate `generalize-printing-foundation` path.
- [ ] Run focused tests for each pass.
- [x] Run `openspec validate consolidate-core-sales-cleanup --strict`.
- [ ] Run backend compile/tests relevant to touched packages.
- [ ] For any batch/event/API/persistence deviation from the reference docs, document the exception in the relevant near-code doc or ADR.
