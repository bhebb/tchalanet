## Why

Several active OpenSpec changes cover overlapping parts of `core.sales` (sell, print, public verification, settlement, events, repositories, and controller cleanup). They also contain an outdated boundary decision: older plans keep ticket printing in `core.sales`, while the validated target is to move ticket receipt rendering to a feature slice.

This change consolidates the sales cleanup into one authoritative backend OpenSpec so implementation can proceed in safe passes without conflicting instructions.

This consolidation is refined by, and must stay compatible with:

- `tchalanet-server/openspec/changes/p0-extract-common-communication-document`
- `tchalanet-edge-service/openspec/changes/p1-rename-notification-route-to-messages`

Those changes are authoritative for generic outbound communication, generic document rendering primitives, and the edge-service canonical route naming.

## What Changes

- Consolidate and supersede:
  - `harden-core-sales-sell-print`
  - `fix-sales-pipeline-audit-gaps`
  - `harden-ticket-settlement-integrity`
  - `harden-public-ticket-verification`
- Fix priority sales bugs:
  - `TicketSettlementQueryRepository` native SQL uses `t.status`; it must use `t.sale_status`.
  - `TicketSettlementJpaAdapter.findNextBatchForDraw` limits in Java after loading; it must pass `Pageable` to the JPA query.
  - `SalesLedgerListener` currently marks an event processed before ledger command dispatch; it must only mark after successful dispatch.
- Split the oversized `TicketController` into narrow controllers:
  - `TicketSalesController` for sell/cancel/approve/reject.
  - `TicketQueryController` for list/details.
  - `AdminTicketController` for override/admin-only operations.
  - Public verification moves to `features.ticketverify`.
  - Receipt endpoints move to target `features.receipt`.
- Move ticket receipt rendering out of `core.sales`:
  - Target feature slice is `features.receipt`, aligned with `p0-extract-common-communication-document`.
  - During transition, any existing `features.ticketreceipt` implementation must be renamed or treated as temporary compatibility work.
  - `core.sales` owns the canonical ticket receipt/read model and ticket-specific formatter.
  - Generic PDF/QR/ESC/POS rendering primitives move to `common.document` under the P0 communication/document extraction change.
- Remove `features.ticketdelivery` as a target feature:
  - SMS/Slack/email/WhatsApp transport is extracted from notification into `common.communication`.
  - `core.notification` becomes responsible for in-app notifications only.
  - Cashier or other feature flows send external messages by calling `common.communication`.
  - No web/mobile direct calls to edge-service, no internal HTTP between Spring controllers, and no controller-to-controller calls.
- Keep public verification as `features.ticketverify`:
  - JSON only.
  - Masked public DTOs.
  - No PDF/QR generation.
- Add/prepare `features.cashier` as the seller/POS BFF:
  - Screen-oriented endpoints for dashboard, sell context, session UX, sell action, recent tickets, and receipt/reprint actions.
  - It delegates to core commands/queries and common primitives; it does not own sales invariants.
  - It must not call `features.receipt`; receipt/communication affordances use action links or lower-level `core.sales` + `common.document` / `common.communication` where the cashier flow intentionally inlines an action.
- Keep `features.receipt` global:
  - It owns receipt/document/print endpoints and artifacts, not only ticket-specific receipt screens.
  - Ticket-specific read model/formatting remains in `core.sales`; generic print/QR/ESC/POS/PDF primitives live in `common.document`.
- Move reporting/export concerns out of `core.sales`:
  - `JpaTicketRepositoryAdapter.exportDailySalesCsv` moves to `features.reporting` or a dedicated export slice.
- Clean up sales persistence/repository shape:
  - Extract inline `JpaTicketRepositoryAdapter.search()` specifications to `TicketSpecifications`.
  - Segment `SpringTicketJpaRepository` into focused repositories.
  - Remove `findByPublicCode` without `DeletedAtIsNull`; keep only the soft-delete-safe lookup.
  - Respect the `Pageable` passed into `JpaTicketRepositoryAdapter.search()`.
- Clean up sales domain/application conventions:
  - Replace raw `UUID approvalRequestId` with `ApprovalRequestId`.
  - Decide and apply one currency type strategy; prefer ISO currency length 3 at persistence.
  - Remove redundant Lombok getter aliases from `Ticket`.
  - Rename overloaded `Ticket.forceResult(payout, when)` to clarify semantics.
  - Remove web response context mutation from `SellTicketCommandHandler`; return notices through `SellTicketResult`.
  - Replace direct autonomy service injection from sales policy with a query through `QueryBus`.
  - Align not-found/bad-request/conflict behavior with backend API conventions.
- Verify database safety:
  - Add RLS to `ticket_line` if missing.
  - Verify indexes for `ticket.public_code`, `ticket.draw_id`, `ticket.created_by`/`user_id`, and `ticket_line.ticket_id`.
  - Verify `TicketLine.oddsSnapshot` semantics for fixed-payout games.

## Capabilities

### New Capabilities

- `sales-ticket-lifecycle`: Canonical ticket lifecycle, sale boundaries, domain cleanup, typed IDs, currency, and repository behavior.
- `sales-ticket-settlement`: Pending settlement queries, batch pagination, idempotent draw result settlement, and override safeguards.
- `sales-event-publishing`: Sales event publication and cross-domain listener idempotency, especially ledger dispatch ordering.
- `sales-ticket-api`: Tenant/admin ticket controller split and HTTP boundary cleanup.
- `receipt-rendering`: Global receipt/document/print feature slice and its relationship to `core.sales` and `common.document`.
- `communication-document-alignment`: Compatibility with common communication/document extraction and edge-service `/internal/messages/send`.
- `public-ticket-verification`: Public ticket verification feature contract, JSON-only response, masking, and payout status.
- `cashier-sales-bff`: Cashier/POS BFF boundaries for seller workflows.

### Modified Capabilities

- None. The backend OpenSpec workspace currently has no active main sales specs; this change introduces consolidated sales capabilities as new delta specs.

## Impact

- **Backend code**:
  - `core.sales.domain`, `core.sales.application`, `core.sales.infra.persistence`, `core.sales.infra.web`, `core.sales.infra.event`.
  - `features.receipt` / temporary `features.ticketreceipt`, `features.ticketverify`, `features.cashier`, `features.reporting`.
  - `common.document` and `common.communication` are owned by `p0-extract-common-communication-document`; this sales change must consume their contracts once available.
- **API**:
  - Existing ticket receipt URLs may remain stable, but the owning Java controller moves to `features.receipt`.
  - Public verification remains `/public/tickets/**` style JSON only.
  - No dedicated `features.ticketdelivery` API is introduced by this sales plan; external communication flows go through Spring features backed by `common.communication`.
  - Cashier/POS may add `/tenant/cashier/**` BFF endpoints.
  - Spring-to-edge outbound communication must target `/internal/messages/send` after the edge P1 change; `/internal/notifications/send` is removed.
- **Breaking/internal**:
  - Java package ownership changes for receipt rendering.
  - Admin override request shape may be simplified to result-only semantics.
  - `SellTicketRequest` body cleanup may be breaking for clients if context-derived fields are removed.
- **Database**:
  - Pre-go-live migration rules apply: update existing migrations/views/audit tables rather than adding a new migration unless explicitly approved.
- **Supersedes**:
  - `harden-core-sales-sell-print`
  - `fix-sales-pipeline-audit-gaps`
  - `harden-ticket-settlement-integrity`
  - `harden-public-ticket-verification`
