## Overview

This change re-centers `core.sales` on the ticket lifecycle and moves user-facing composition out to feature slices. The goal is not to add new business behavior first; it is to remove conflicting plans and establish a single implementation path.

## Reference Model

Implementation MUST follow these reference documents before local judgment:

- `docs/ARCHITECTURE.md` — layer ownership, controller ownership, `core` vs `features`.
- `docs/PLAYBOOK.md` — contribution workflow, controllers, handlers, DoD.
- `docs/NAMING.md` — commands/queries, handlers, ports, adapters, controllers, DTOs, events, batch naming.
- `docs/conventions/command_query_handlers.md` — handler placement, `@UseCase`, `@TchTx`, bus dispatch, query side-effect rules.
- `docs/conventions/api/web_api.md` — controller responsibilities, typed IDs, `ApiResponse`, `ProblemDetail`, audit declaration.
- `docs/conventions/api/pagination.md` — `@TchPaging`, `TchPageRequest`, `TchPage`, sort allowlist, DB pagination.
- `docs/conventions/api/routing_and_path.md` — logical paths only; controllers do not include `/api/v1`.
- `docs/conventions/api/api_response.md` — success and error response envelope rules.
- `docs/conventions/typed_ids.md` — no raw UUID outside persistence.
- `docs/conventions/inter_domain_calls.md` — feature orchestration through `CommandBus` / `QueryBus`, no cycles.
- `docs/conventions/event_model.md` — event producer/consumer packages, after-commit, idempotent listeners.
- `docs/conventions/persistence/persistence.md` — ports/adapters, Flyway, Envers, pre-go-live migration policy.
- `docs/conventions/persistence/jpa_entities.md` — `BaseTenantEntity`, relation rules, documented `@OneToMany` exceptions.
- `docs/conventions/persistence/rls.md` — tenant isolation and RLS expectations.
- `docs/conventions/batch/batch.md` — scheduler/batch/ops gate/context rules.

Concrete code models:

- Use `core.draw` as the structural model for a core domain: `domain`, `application/command`, `application/query`, `application/port/out`, `infra/web`, `infra/persistence`, `infra/event`, `infra/batch|scheduler` where needed.
- Use `features.ops` as the structural model for a feature/BFF: thin controllers/services, orchestration only, no feature command handlers for business mutations, no core persistence writes.

```text
common.document
  Generic technical PDF / ESC-POS / QR / receipt primitives

common.communication
  Generic outbound external communication primitives and edge adapter

core.sales
  Ticket lifecycle, sale commands, settlement, internal ticket queries,
  canonical ticket receipt/read model, ticket-specific receipt formatter

features.receipt
  Receipt/document/print artifact endpoints, including ticket receipts

features.ticketverify
  Public JSON ticket verification

features.cashier
  Cashier/POS screen BFF

features.reporting
  Sales reports and exports
```

This sales plan MUST stay aligned with:

- `p0-extract-common-communication-document` for `common.document`, `common.communication`, `features.receipt`, and the ban on `features.cashier -> features.receipt`.
- `p1-rename-notification-route-to-messages` for replacing edge-service `/internal/notifications/send` with `/internal/messages/send`.

## Boundaries

### core.sales

`core.sales` owns durable ticket truth:

- `Ticket` and `TicketLine` lifecycle.
- Sell, cancel, approve, reject, result, override, settle.
- Settlement idempotency and state transitions.
- Internal queries needed by features.

It must not own:

- Generic PDF/QR/ESC/POS rendering.
- Email/SMS/WhatsApp delivery.
- Public verification response masking/composition.
- CSV/report export formatting.
- Cashier screen aggregation.

`core.sales` implementation MUST mirror the `core.draw` shape unless there is a documented local reason:

```text
core.sales.domain.model
core.sales.domain.event
core.sales.domain.exception
core.sales.application.command.model
core.sales.application.command.handler
core.sales.application.query.model
core.sales.application.query.handler
core.sales.application.port.out
core.sales.infra.web
core.sales.infra.web.mapper
core.sales.infra.web.model
core.sales.infra.persistence
core.sales.infra.persistence.adapter
core.sales.infra.persistence.mapper
core.sales.infra.persistence.repository
core.sales.infra.event
core.sales.infra.batch|scheduler only if needed
```

Handlers MUST follow `docs/conventions/command_query_handlers.md`: one command/query record, one handler, `@UseCase`, `@TchTx` on writes, projections for query outputs, no JPA/Spring MVC types in handler signatures.

### features.receipt

`features.receipt` is the global feature for receipt/document/print artifacts exposed through Spring APIs. For sales, it owns ticket receipt artifacts:

- PDF bytes.
- ESC/POS bytes.
- QR PNG.
- Receipt endpoint orchestration.

It may call `core.sales` via `QueryBus` and use `common.document`.

It must not:

- Persist ticket state.
- Own ticket invariants.
- Send SMS/Slack/email/WhatsApp itself.
- Become a generic printing framework in this change.

Its shape MUST follow existing feature slices, especially `features.ticketverify` and `features.ops`. Use root-level controller/service/mapper when small; introduce `app/`, `model/`, or `config/` only when the local rule-of-3 justifies it. Do not introduce hexagonal `port/out` or `infra/persistence` packages for this feature.

If an interim implementation exists under `features.ticketreceipt`, it MUST be renamed or explicitly treated as temporary compatibility; the target package name is `features.receipt`.

### common.communication

There is no `features.ticketdelivery` target in this plan.

External message transport is technical infrastructure extracted from notification:

- EMAIL.
- SMS.
- WHATSAPP.
- SLACK.
- Edge-service payload construction.
- HMAC signed edge-service calls.

`core.notification` owns in-app notifications only. External channels are routed through `common.communication`.

Feature flows such as `features.cashier` may call `common.communication` when they need to send an external message. They must not call edge-service directly from web/mobile clients.

### features.ticketverify

`features.ticketverify` owns the public verification API:

- Code normalization.
- Public response shape.
- Public masking.
- Public payout status.
- Noindex/cache-control headers.

It returns JSON only and must not render PDF/QR.

### features.cashier

`features.cashier` is the seller/POS BFF:

- Cashier dashboard.
- Sell context.
- Current/open/close session UX.
- `POST /tenant/cashier/sell`.
- Recent tickets and reprint/receipt actions.

It delegates to core commands/queries and feature services. It must not compute sales limits, payouts, cutoff rules, or settlement.

`features.cashier` MUST be modeled after `features.ops`: screen/BFF orchestration, no business command handlers, no direct core persistence writes, no durable invariants.

`features.cashier` MUST NOT call `features.receipt`. For receipt or external communication inline actions it must use action links, or explicitly use lower-level `core.sales` receipt query/read model plus `common.document` / `common.communication`.

## Controller Target

Replace the oversized `TicketController` with focused controllers:

```text
core.sales.infra.web.TicketSalesController
  POST /tenant/tickets
  POST/PATCH /tenant/tickets/{ticketId}/cancel
  POST /tenant/tickets/{ticketId}/approve
  POST /tenant/tickets/{ticketId}/reject

core.sales.infra.web.TicketQueryController
  GET /tenant/tickets
  GET /tenant/tickets/{ticketId}

core.sales.infra.web.AdminTicketController
  PATCH /admin/tickets/{ticketId}/result/override

features.receipt.ReceiptController
  GET /tenant/tickets/{ticketId}/print.pdf
  GET /tenant/tickets/{ticketId}/print.escpos
  GET /tenant/tickets/{ticketId}/qr

features.ticketverify.TicketVerifyController
  GET /public/tickets/verify/{code}

features.cashier or another explicit feature flow
  optional external message actions via common.communication
```

The exact route spelling may preserve current API compatibility, but ownership must follow the list above.

Controllers MUST follow `docs/conventions/api/web_api.md`:

- validate web inputs with Bean Validation;
- inject `@CurrentContext TchRequestContext` where context is needed;
- map request/query params to command/query or criteria;
- dispatch through `CommandBus` / `QueryBus`;
- map application views to web responses;
- return `ApiResponse<T>` or `ApiResponse<TchPage<T>>` for JSON success;
- emit `ProblemDetail` errors through project-standard exception handling;
- never call repositories/adapters directly.

List controllers MUST follow `docs/conventions/api/pagination.md`: `@TchPaging`, allowed sort allowlist, default sort, `TchPageRequest`, DB-side pagination, and response `TchPage<T>`.

## Communication and Document Alignment

Generic document rendering is not owned by this sales cleanup. It is owned by `p0-extract-common-communication-document`:

```text
common.document.receipt.ReceiptModel
common.document.pdf.ReceiptPdfRenderer
common.document.qr.QrRenderer
common.document.escpos.EscPosReceiptRenderer
```

Sales-specific receipt assembly stays in `core.sales`:

```text
core.sales.application.receipt.TicketReceiptFormatter
core.sales.application.query.model.TicketReceiptView
core.sales.application.query.model.TicketReceiptLineView
```

External message transport is not owned by `core.sales`, is not a `features.ticketdelivery` slice, and must not be modeled as notification-center behavior. It uses `common.communication`:

```text
common.communication.api.OutboundMessageRequest
common.communication.api.OutboundMessageGateway
common.communication.edge.EdgeCommunicationGatewayAdapter
```

The edge-service canonical path is:

```text
POST /internal/messages/send
```

The legacy path `/internal/notifications/send` is removed by the edge rename and must not be targeted by server common communication.

## Persistence Cleanup

`JpaTicketRepositoryAdapter` must return to ticket persistence concerns only:

- Save/load ticket aggregate.
- Search using extracted specifications.
- Soft-delete-safe public-code lookup.
- No CSV formatting.
- No receipt rendering orchestration.

`SpringTicketJpaRepository` should split by role when the rule-of-3 is met:

- CRUD/basic aggregate access.
- Search/list access.
- Close-day stats/count methods.
- Settlement batch access.

`TicketSettlementJpaRepository.findBatchForDrawWithLines` must accept `Pageable` so the database limits rows.

Ports and adapters MUST follow `docs/NAMING.md`:

- read ports: `XxxReaderPort`;
- write ports: `XxxWriterPort`;
- JPA adapters: `XxxJpaAdapter`;
- JDBC adapters: `XxxJdbcAdapter`;
- Spring Data repositories: `XxxJpaRepository`;
- persistence mappers: `XxxPersistenceMapper` or current local `XxxMapper` if established.

Persistence MUST remain technical: no business decisions, no response DTOs, no controller types, no feature orchestration.

## Event Idempotency

The existing `ProcessedEventPort.markProcessedIfAbsent` convention protects against duplicate processing by marking first. That is acceptable for projections where the projection write is included in the same local operation. It is not acceptable for `SalesLedgerListener` when the side effect is a command dispatch to ledger: if dispatch fails after marking, ledger can be permanently skipped.

For `SalesLedgerListener`, use:

```text
alreadyProcessed -> if true skip
commandBus.send(...)
markProcessedIfAbsent
```

This is a documented exception to the generic "mark first" convention because the listener dispatches a critical financial side effect.

All sales events/listeners MUST follow `docs/conventions/event_model.md`:

- event classes live with the producer domain;
- cross-domain listeners live with the consumer;
- publish after commit;
- consume after commit;
- make consumers idempotent;
- listeners stay thin and dispatch commands for real work.

Batch/scheduler work MUST follow `docs/conventions/batch/batch.md`: schedulers decide when, bind context correctly, check gates where applicable, and call commands through the bus.

## Error Handling

Current docs are not perfectly consistent: some backend docs say handlers may throw `ProblemRest`, while other OpenSpec context says domain/use cases should throw domain exceptions mapped by `ErrorHandler`.

For this change:

- Controllers and feature services may use `ProblemRest` for HTTP boundary errors.
- Domain models must not use `ProblemRest`.
- Core command/query handlers should prefer domain/application exceptions when the error is business meaning, and existing project conventions may be harmonized in a follow-up if needed.
- Do not return `null` from query handlers for not-found; return `Optional` or throw a mapped exception.

## Database Notes

Pre-go-live migration policy applies:

- Do not create a new `V*.sql` without explicit approval.
- Update original migrations (`V100`, `V101`, `V103`, `V105`, `V108`) when schema/view/index/RLS changes are required.
- If changing `ticket` or `ticket_line`, check entity mappings, audit tables, read views, and seeds.

Known checks:

- `ticket_line` currently appears not to have RLS in `V105__configure_rls.sql`; verify and add if missing.
- `ticket.public_code` has a unique constraint.
- `ticket.draw_id` is covered by `(tenant_id, draw_id)`.
- `ticket_line.ticket_id` is indexed.
- `ticket.created_by` does not appear to have a dedicated index; decide whether `user_id` replaces this query path or add an index.

## Follow-Up Outside This Change

Generic communication and document rendering are not a new follow-up invented by this sales plan. They are governed by `p0-extract-common-communication-document`.

Sales implementation must therefore consume or wait for:

- `common.document` for generic receipt/PDF/QR/ESC/POS primitives;
- `common.communication` for outbound external messages;
- the edge-service `/internal/messages/send` migration from `p1-rename-notification-route-to-messages`.

Sales-specific receipt composition remains outside `common.document`.
