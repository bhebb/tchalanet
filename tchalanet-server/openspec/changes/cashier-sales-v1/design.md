# Design — Cashier Features & Core Sales Receipt V1

## Overview

This design reorganizes `features.cashier`, strengthens `core.sales` as the owner of official ticket receipt content, and keeps platform capabilities technical.

```text
Cashier feature
  -> maps POS HTTP requests
  -> validates/request context at boundary
  -> orchestrates with bus + platform APIs

Sales core
  -> evaluates sale acceptance
  -> owns ticket aggregate and ticket truth
  -> owns official ticket receipt content and formatter
  -> owns backup shareable proof content

Document platform
  -> renders bytes from generic document requests

Communication platform
  -> enqueues text messages and applies delivery dedup
```

---

## Package structure

### `features.cashier`

```text
features.cashier
  session/
    web/CashierSessionController
    app/CashierSessionService
    model/

  draws/
    web/CashierDrawsController
    app/CashierDrawsService
    model/

  tickets/
    web/CashierTicketsController
    app/
      CashierTicketsService
      CashierTicketReceiptService
    model/
      CashierSellTicketRequest
      CashierSellTicketResponse
      CashierTicketPreviewResponse
      CashierTicketDetailsResponse
      CashierTicketCancelRequest
      CashierTicketBackupView
      PrintTicketRequest
      SendTicketReceiptRequest
      SendTicketReceiptResponse
    mapper/

  offline/
    web/CashierOfflineController
    app/CashierOfflineService
    model/

  operationalcontext/
    web/CashierOperationalContextController
    app/CashierOperationalContextService
    model/
```

`CashierTicketsService` handles preview/sell/list/get/cancel.

`CashierTicketReceiptService` handles print/send and injects `DocumentApi` and `CommunicationApi`.

### `core.sales.api`

```text
core.sales.api
  command.sell/
    SellTicketCommand
    SellTicketLineInput
    SellTicketResult
    SellTicketOutcome        // ACCEPTED | REJECTED | PENDING_APPROVAL

  command.cancel/
    CancelTicketCommand
    CancelTicketResult

  query.preview/
    PreviewTicketSaleQuery
    TicketSalePreviewResult

  query.receipt/
    GetTicketReceiptViewQuery
    FormatTicketReceiptPrintQuery
    FormatTicketReceiptMessageQuery

  query.verification/
    VerifyTicketByPublicCodeQuery

  model.sale/
    SaleDecision             // ACCEPTABLE | REQUIRES_CHANGES | REJECTED_FINAL
    SaleIssueView
    SaleIssueSeverity        // ERROR | WARNING | INFO
    SaleActionAvailability
    TicketBackupInfo
    SaleIssueCatalog.md

  model.receipt/
    TicketReceiptView
    TicketReceiptGameSectionView
    TicketReceiptLineView
    TicketReceiptPrintContent
    TicketReceiptMessageContent

  model.verification/
    TicketVerificationView
    CustomerTicketStatus
```

### `core.sales.internal`

```text
core.sales.internal.application
  sale/
    SaleAcceptanceEvaluator
    SaleIssueFactory
    SaleExposurePlanner
    SaleEvaluationMode       // PREVIEW | FINAL

  receipt/
    TicketReceiptAssembler
    TicketReceiptPrintFormatter
    TicketReceiptMessageFormatter
    TicketBackupAssembler

  receipt.formatter/
    TicketDrawLabelFormatter
    DefaultTicketDrawLabelFormatter
    TicketReceiptMoneyFormatter
    TicketPublicCodeFormatter
    TicketVerificationUrlBuilder
```

---

## Files to remove

```text
core.sales.internal.application.port.out.TicketReceiptFormatter
core.sales.internal.application.port.out.TicketPrintReaderPort
core.sales.api.query.GetPublicTicketVerificationRecordQuery
core.sales.internal.application.query.handler.GetPublicTicketVerificationRecordQueryHandler
```

---

## HTTP endpoints

### Session

```http
GET  /tenant/cashier/session/current
POST /tenant/cashier/session/open
POST /tenant/cashier/session/close
```

### Draws

```http
GET /tenant/cashier/draws/available
```

### Tickets

```http
POST /tenant/cashier/tickets/preview
POST /tenant/cashier/tickets/sell                 Idempotency-Key required
POST /tenant/cashier/tickets/{ticketId}/cancel
GET  /tenant/cashier/tickets
GET  /tenant/cashier/tickets/{ticketId}
POST /tenant/cashier/tickets/{ticketId}/print
POST /tenant/cashier/tickets/{ticketId}/send
```

### Offline

```http
GET  /tenant/cashier/offline/grant/current
POST /tenant/cashier/offline/submissions
```

### Operational context

```http
GET    /tenant/cashier/operational-context/current
POST   /tenant/cashier/operational-context/select
DELETE /tenant/cashier/operational-context
```

---

## Sale evaluation pipeline

The same evaluator is used for preview and sell.

```text
1. Normalize input lines.
2. Validate structural shape.
3. Resolve trusted POS operational context.
4. Resolve draw eligibility.
5. Resolve game/bet-type eligibility.
6. Resolve pricing and stake constraints.
7. Compute basket exposure by ExposureKey.
8. Read existing exposure.
9. Apply cumulative limits.
10. Apply autonomy/approval policy.
11. Map approval-required POS decision to REQUIRES_CHANGES / APPROVAL_REQUIRED.
12. Determine final decision.
13. Build issues and seller instructions.
14. Compute action availability.
15. Return evaluation result.
```

### Preview mode

- Read-only.
- No locks.
- No reservation.
- Requires trusted POS context because exposure/autonomy depend on seller/session/terminal/outlet.
- Returns an indicative warning when acceptable.

### Final mode

When decision is acceptable:

```text
1. Acquire lock or atomic upsert on exposure rows per ExposureKey.
2. Recompute remaining exposure.
3. If exposure changed and remaining is insufficient, return REJECTED / EXPOSURE_CHANGED.
4. Create Ticket aggregate.
5. Persist ticket.
6. Persist exposure update.
7. Build TicketBackupInfo synchronously from the saved ticket/result.
8. AfterCommit: publish TicketPlacedEvent.
```

Backup is built before returning the HTTP response. Domain events are published after commit.

---

## Exposure model

Exposure is cumulative. A rule such as “max 250 HTG on BOLET 45” applies to:

```text
existing exposure + current basket exposure
```

This prevents bypassing limits by splitting a sale into multiple tickets.

### ExposureKey

Avoid raw `UUID` in application/core models. Use typed IDs or a normalized string reference.

```java
public record ExposureKey(
    DrawId drawId,
    String gameCode,
    String normalizedSelection,
    String betOption,
    ExposureScope scope,
    String scopeRef      // null or "TENANT" for tenant scope; typed id value string otherwise
) {}
```

`core.limitpolicy` owns rule definitions. `core.sales` asks the limit policy API and translates applicable policy rules to exposure lookups.

---

## Issue model

```java
public record SaleIssueView(
    String code,
    SaleIssueSeverity severity,
    String message,
    String sellerInstruction,
    int lineIndex,
    Map<String, Object> details
) {}
```

`lineIndex = -1` means basket-wide issue.

A documentation catalog maps issue codes to expected detail fields.

Examples:

```text
SELECTION_EXPOSURE_LIMIT_EXCEEDED
EXPOSURE_CHANGED
APPROVAL_REQUIRED
DRAW_CUTOFF_EXCEEDED
SESSION_CLOSED
TERMINAL_BLOCKED
INVALID_SELECTION_FORMAT
STAKE_TOO_HIGH
```

---

## Response shapes

### Preview accepted

```json
{
  "decision": "ACCEPTABLE",
  "canSell": true,
  "canEditAndRetry": true,
  "summary": {
    "lineCount": 3,
    "totalStake": "20.00 HTG",
    "totalCharges": "0.00 HTG",
    "totalAmount": "20.00 HTG",
    "potentialPayout": "39100.00 HTG"
  },
  "issues": [],
  "sellerInstruction": "Ce billet peut être vendu.",
  "warning": "Ce résultat est indicatif. D'autres ventes en cours peuvent modifier les limites disponibles."
}
```

### Sell accepted

```json
{
  "outcome": "ACCEPTED",
  "ticket": {
    "ticketId": "uuid",
    "ticketCode": "TCK-260520-025712-SQA4P8-6",
    "publicCode": "PSGV4AXJ",
    "displayCode": "PSGV-4AXJ",
    "placedAt": "2026-05-20T10:00:00Z",
    "totalAmount": "20.00 HTG",
    "potentialPayout": "39100.00 HTG"
  },
  "backup": {
    "displayCode": "PSGV-4AXJ",
    "verificationShortUrl": "app.tchalanet.com/ticket/PSGV-4AXJ",
    "shareableText": "Ticket Tchalanet valide\nCode: PSGV-4AXJ\n...",
    "primaryInstruction": "Votre code est PSGV-4AXJ.",
    "verificationInstruction": "Vérifier sur app.tchalanet.com/ticket/PSGV-4AXJ"
  },
  "actions": {
    "canPrint": true,
    "canSendSms": true,
    "canSendWhatsapp": true,
    "canSendEmail": true,
    "canCopy": true
  },
  "issues": [],
  "sellerInstruction": "Vente acceptée. Donnez le code au client ou imprimez/envoyez le ticket."
}
```

### Sell rejected after exposure change

```json
{
  "outcome": "REJECTED",
  "ticket": null,
  "backup": null,
  "actions": {
    "canPrint": false,
    "canSendSms": false,
    "canSendWhatsapp": false,
    "canSendEmail": false,
    "canCopy": false
  },
  "issues": [
    {
      "code": "EXPOSURE_CHANGED",
      "severity": "ERROR",
      "message": "La limite a changé pendant la vente. Réessayez avec le nouveau montant disponible.",
      "sellerInstruction": "Réduisez la mise à 1.00 HTG sur BOLET 45 et réessayez.",
      "lineIndex": 1,
      "details": {
        "gameCode": "BOLET",
        "selection": "45",
        "allowedRemaining": "1.00 HTG"
      }
    }
  ],
  "sellerInstruction": "Vente refusée. Ajustez le panier et réessayez."
}
```

### Print

Returns binary content. Not wrapped in `ApiResponse`.

Required headers:

```text
Cache-Control: no-store
Content-Disposition: inline; filename="ticket-PSGV-4AXJ.pdf"
```

### Send

```json
{
  "ticketId": "uuid",
  "channel": "WHATSAPP",
  "queued": true,
  "deduplicated": false
}
```

---

## Receipt content

### Printed receipt

Grouped by game:

```text
BOLET
Nimewo              Mise
45                  5.00 HTG
12                  5.00 HTG

MARYAJ
Nimewo              Mise
23-44               10.00 HTG
```

QR payload is the customer verification short URL.

### Message receipt

V1 sends text only:

```text
Ticket Tchalanet valide
Code: PSGV-4AXJ
Tirage: Haïti • Texas • 10:00
Total: 20.00 HTG

Numéros:
- Bolet 45: 5.00 HTG
- Bolet 12: 5.00 HTG
- Maryaj 23-44: 10.00 HTG

Vérifier: app.tchalanet.com/ticket/PSGV-4AXJ
```

`TicketBackupInfo.shareableText` must be generated by the same formatter as `FormatTicketReceiptMessageQuery`.

---

## Print and send behavior

### Print

```http
POST /tenant/cashier/tickets/{ticketId}/print
```

- Ticket must exist in tenant scope.
- Ticket must be printable: accepted/sold and not voided/cancelled.
- Calls `FormatTicketReceiptPrintQuery`.
- Calls `DocumentApi.render(...)`.
- Returns binary response.
- Must audit `PRINT_TICKET`.
- On successful render, call `RecordTicketPrintCommand` or equivalent.

### Send

```http
POST /tenant/cashier/tickets/{ticketId}/send
```

- Ticket must exist in tenant scope.
- Ticket must be sendable.
- Calls `FormatTicketReceiptMessageQuery`.
- Calls `CommunicationApi.enqueue(...)`.
- Deduped by `(ticketId, channel, recipient)` within 60 seconds.
- V1 sends text only.

---

## Security and context

All cashier endpoints require:

- authenticated user with `CASHIER` or higher authority;
- bound `TchRequestContext`;
- trusted operational context for `preview`, `sell`, `cancel`, `print`, and `send`;
- no tenant ID in request body.

RLS remains the last line of defense.

---

## Testing and enforcement

### ArchUnit

Add a rule:

```text
No class under features.cashier may import core.sales.internal..*
```

### E2E minimum

```text
test_cashier_session_lifecycle.py
test_cashier_draws_available.py
test_cashier_preview_acceptable.py
test_cashier_preview_requires_changes_exposure_limit.py
test_cashier_preview_requires_changes_stake_too_high.py
test_cashier_preview_rejected_final_after_cutoff.py
test_cashier_preview_rejected_final_session_closed.py
test_cashier_sell_accepted_with_backup.py
test_cashier_sell_accepted_idempotency_same_key_same_payload.py
test_cashier_sell_accepted_idempotency_same_key_different_payload_conflict.py
test_cashier_sell_missing_idempotency_key_rejected.py
test_cashier_sell_rejected_exposure_changed_after_preview.py
test_cashier_sell_rejected_approval_required_surfaces_as_changes.py
test_cashier_sell_then_cancel_within_window.py
test_cashier_sell_then_cancel_after_window_rejected.py
test_cashier_print_accepted_ticket.py
test_cashier_print_voided_ticket_rejected.py
test_cashier_send_sms_text_only.py
test_cashier_send_dedup_within_60_seconds.py
test_cashier_backup_shareable_text_matches_send_content.py
```
