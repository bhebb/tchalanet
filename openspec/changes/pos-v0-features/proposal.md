# OpenSpec Change — `pos-v0-features`

> **Status**: Proposed
> **Order**: ⓷ — depends on `pos-v0-foundation` > **Type**: feature delivery (BFF endpoints + Flutter integration)
> **Risk**: medium — new HTTP surface

## Why

The foundation provides the data model, domain logic, ports, events, permissions, and audit. This change ships the **BFF endpoints** that the Flutter POS app consumes, plus the conventions for integration.

The driver: **avoid client fan-out**. Each Flutter screen calls **one BFF endpoint** that aggregates everything it needs. The BFF orchestrates internally via `CommandBus` / `QueryBus`, using existing core domains.

## What Changes

- New feature slice `features.pos` with 8 BFF controllers, 8 BFF services, request/response DTOs under `/api/v1/tenant/pos/...` — **17 endpoints** covering 10 screens.
- `core.sales` extended with `TicketEntry` model (multi-entry tickets: `drawId` moves from `ticket` to `ticket_entry`). New tables `ticket` and `ticket_entry` (new migration file since the schema is entirely new — not a modification).
- `PlaceTicketCommandHandler` extended for multi-entry, with LimitPolicy + Autonomy integration, idempotency via `idempotency_key`, SMS fee, PENDING_APPROVAL flow.
- `ApproveBlockedTicketCommandHandler` and `CancelTicketCommandHandler` implemented.
- 4 read-model projections (dashboard KPIs, draw-with-stats, recent results, last ticket) fed by cross-domain listeners on `TicketPlacedEvent`, `DrawSettledEvent`, `SalesSessionClosedEvent`.
- BFF-level cache on dashboard (30 s), sale context (60 s), ticket detail (60 s), results (5 min), settings (60 s).
- Flutter contract: screen-to-endpoint map, error code to French message map, currency/countdown/receipt conventions.

## Capabilities

### New Capabilities

- `bff-endpoints`: 17 BFF HTTP endpoints under `features.pos`; orchestration only; no domain logic in BFF services.
- `sales-multi-entry`: `Ticket` + `TicketEntry` domain model in `core.sales`; `PlaceTicketCommand` multi-entry; approval and cancel flows; events.
- `flutter-integration`: Flutter integration contract — screen map, error codes, idempotency, currency, platform header, build flavors.
- `pos-screens`: 10 screens detailed flow (Login, Open Session, Dashboard, New Sale, Sale Approval, Ticket Detail, History, Results, Settings, Close Session).

### Modified Capabilities

_(none — builds on foundation; no existing capability spec modified)_

## Impact

- **Java sources**: new `features.pos` package (~8 controllers + 8 services + DTOs); `core.sales` extended with `TicketEntry` JPA entity, new handlers (`PlaceTicket`, `ApproveBlockedTicket`, `CancelTicket`); 4 projection listener classes; `TicketPlacedEvent` must carry `sessionId` (verify OQ-4 from `pos-v0-foundation`).
- **SQL migrations**: NEW migration file for `ticket` and `ticket_entry` tables (these are created from scratch — not rewriting existing tables); permissions seed already handled in `pos-v0-foundation`.
- **Flutter (mobile app)**: new screens, Riverpod providers/repositories for each BFF endpoint, `package:decimal` for amounts.
- **Cache**: 5 new named BFF-level caches.
- **OpenAPI**: 17 new endpoints documented.
- **Tests**: BFF controller tests (MockMvc); PlaceTicketCommandHandler unit tests; listener tests.
