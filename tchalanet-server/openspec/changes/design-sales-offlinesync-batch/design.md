# Design: Sales + Offline Sync + Cashier Feature

## Boundary rules

```text
HTTP route exposure does not imply Java API exposure.

api/
  = stable Java contract consumed by another module

internal/
  = use cases used only by controllers, schedulers, listeners, adapters in the same bounded context
```

## Components

```text
features.cashier
  -> cashier pages and orchestration
  -> uses sales/offlinesync APIs
  -> no business invariants

core.sales
  -> official tickets
  -> sell/lifecycle/verification/print snapshot
  -> accepts/rejects offline submissions as official tickets

core.offlinesync
  -> offline grants
  -> device proof/signature/grant token checks
  -> offline_submission intake and status
  -> scheduler dispatch to sales
```

## Sales controller target

### 1. TicketLifecycleController

Responsibilities:

- sell ticket
- approve pending sale
- reject pending sale
- cancel ticket
- void ticket later if needed

Notes:

- The duplicated `TicketSalesController` sell endpoint must be removed or merged.
- Sell command stays internal if only called by the sales HTTP controller.
- Use `@CurrentContext TchRequestContext` for tenant/user/POS hint mapping.
- Do not accept terminal/outlet/session IDs in body for POS context.

### 2. TicketQueryController

Responsibilities:

- tenant/internal list
- ticket details

Correction:

- If `features.cashier` consumes ticket list and details through Java, then expose cashier-oriented queries in `core.sales.api.query`.
- The controller may still use those public queries if desired, but internal controller-only query variants should remain internal.

### 3. TicketPrintController

Responsibilities:

- return ticket print view/snapshot
- mark printed
- reprint if needed

Decision:

- `GetTicketPrintViewQuery` belongs in `core.sales.api.query` because `features.cashier` will call it and then call `platform.document` or local print orchestration.
- Sales owns the business snapshot; document owns rendering.

## Offlinesync controller target

### OfflineGrantController

Responsibilities:

- request/issue offline sales grant
- get current/grant detail
- revoke grant

Correction:

- Commands used only by this controller stay internal unless another module needs them.
- The controller should not expose domain entity `OfflineSalesGrant` directly.
- Request must use POS headers via `ctx.operationalContextRequired()`, not body fields for terminal/outlet/session.
- Offline grant must require strong device proof, not only weak client claims.

### OfflineSyncController

Responsibilities:

- receive offline sync batches
- validate technical proofs
- persist submissions
- return batch receipt

Decision:

- It does not call sales immediately.
- It saves `offline_submission` with `READY_FOR_SALES`, `TECH_REJECTED`, or `REVIEW_REQUIRED`.
- Later scheduler dispatches bounded items to sales.

### OfflineSubmissionAdminController

Responsibilities:

- list submissions
- detail
- retry
- manual reject/review decision
- ops force-dispatch

## Batch flow

```text
OfflineSyncController
  -> ReceiveOfflineBatchCommandHandler
  -> persist submissions
  -> READY_FOR_SALES

OfflineSubmissionSalesProcessingScheduler
  -> DispatchReadyOfflineSubmissionsCommandHandler
  -> lock N submissions
  -> commandBus.execute(ProcessOfflineSubmissionForSalesCommand)

core.sales
  -> ProcessOfflineSubmissionForSalesCommandHandler
  -> queryBus.ask(GetOfflineSubmissionForSalesQuery)
  -> validate draw/cutoff/pricing/limit/session
  -> create ticket or reject/review
  -> return result and/or publish event

core.offlinesync
  -> RecordOfflineSubmissionSalesDecisionCommandHandler
  -> SALES_ACCEPTED / SALES_REJECTED / REVIEW_REQUIRED
```

## API exposure matrix

### core.sales.api

Expose because consumed outside sales:

- `ProcessOfflineSubmissionForSalesCommand`
- `GetTicketPrintViewQuery`
- `GetCashierTicketDetailsQuery` or `GetTicketDetailsQuery`
- `ListCashierTicketsQuery` or `ListTicketsQuery` if features.cashier uses it
- `GetTicketForPayoutQuery`
- `GetTicketForDrawSettlementQuery`
- `TicketPlacedEvent`
- `OfflineSubmissionAcceptedAsTicketEvent`
- `OfflineSubmissionRejectedBySalesEvent`

Keep internal if only controller uses it:

- `SellTicketCommand`
- `ApproveTicketSaleCommand` if only lifecycle controller uses it
- `RejectTicketSaleCommand` if only lifecycle controller uses it
- `CancelTicketCommand`
- `ReprintTicketCommand` unless features invokes it directly
- `MarkTicketPrintedCommand` unless features invokes it directly

### core.offlinesync.api

Expose because consumed outside offlinesync:

- `GetOfflineSubmissionForSalesQuery`
- `OfflineSubmissionForSalesView`
- events that are consumed by platform/features/reporting

Keep internal if only controller/scheduler/listener uses it:

- `IssueOfflineSalesGrantCommand` / `RequestOfflineGrantCommand`
- `RevokeOfflineSalesGrantCommand`
- `ReceiveOfflineBatchCommand`
- `DispatchReadyOfflineSubmissionsCommand`
- `RecordOfflineSubmissionSalesDecisionCommand`
- `GetOfflineGrantQuery` unless features/admin BFF uses it through Java

## Suggested package layout

### core.sales

```text
core/sales/
  api/
    command/
    query/
    event/
    model/
  internal/
    domain/
      model/ticket/
      model/lifecycle/
      model/print/
      service/sell/
      service/lifecycle/
      service/print/
      service/offline/
    application/
      command/model/
      command/handler/sell/
      command/handler/lifecycle/
      command/handler/print/
      query/model/
      query/handler/ticket/
      query/handler/print/
      service/sell/
      service/offline/
      service/print/
      port/out/
    infra/
      web/
      web/model/
      web/mapper/
      persistence/
      event/
      scheduler/
      batch/
      config/
```

### core.offlinesync

```text
core/offlinesync/
  api/
    query/
    event/
    model/
  internal/
    domain/
      model/grant/
      model/submission/
      model/device/
      service/grant/
      service/submission/
      service/device/
    application/
      command/model/
      command/handler/
      query/model/
      query/handler/
      service/submission/
      port/out/
    infra/
      web/
      web/model/
      web/mapper/
      persistence/
      event/
      scheduler/
      batch/
      config/
```

### features.cashier

```text
features/cashier/
  tickets/
    web/
      CashierTicketsController.java
    app/
      CashierTicketPrintService.java
      CashierTicketPageService.java
    model/
      CashierTicketPageResponse.java
      CashierTicketDetailsResponse.java
      CashierTicketPrintResponse.java
    mapper/
      CashierTicketMapper.java
```

## Document / print boundary

```text
core.sales
  -> TicketPrintView business snapshot

features.cashier
  -> calls GetTicketPrintViewQuery
  -> calls platform.document if server rendering is needed

platform.document
  -> renders PDF/ESC-POS/QR bytes
  -> no ticket lifecycle decision
```
