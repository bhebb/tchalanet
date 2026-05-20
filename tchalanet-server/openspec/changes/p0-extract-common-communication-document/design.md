# Design: P0 — Server communication/document extraction

## Final boundary

```text
common.communication
  = generic outbound external communication primitives and edge adapter

common.document
  = generic technical document/receipt rendering primitives

core.notification
  = in-app notification center and functional notification dispatch

core.sales
  = ticket lifecycle and canonical ticket receipt/read models

features.cashier
  = cashier UI/BFF orchestration

features.receipt
  = receipt print/download/preview UI/API orchestration
```

## Dependency rule

Allowed:

```text
features.cashier -> core.sales
features.cashier -> common.document
features.cashier -> common.communication

features.receipt -> core.sales
features.receipt -> common.document

core.notification -> common.communication
```

Forbidden:

```text
features.cashier -> features.receipt
features.receipt -> features.cashier
common -> core/features/catalog
core.sales -> features.*
core.notification edge adapter naming remaining as notification transport
```

## Communication model

`core.notification` has a functional channel enum:

```java
NotificationChannel {
  IN_APP,
  EMAIL,
  SMS,
  WHATSAPP,
  SLACK
}
```

`common.communication` has only external transport channels:

```java
CommunicationChannel {
  EMAIL,
  SMS,
  WHATSAPP,
  SLACK
}
```

Rule:

```text
NotificationChannel.IN_APP
  -> core.notification persists/updates notification center state

NotificationChannel.EMAIL/SMS/WHATSAPP/SLACK
  -> mapped to common.communication.api.OutboundMessageRequest
  -> common.communication.edge.EdgeCommunicationGatewayAdapter
  -> edge-service internal endpoint
```

`IN_APP` must never be sent to edge-service.

## Edge adapter rename

Move/rename:

```text
core.notification.infra.external.EdgeNotificationGatewayAdapter
  -> common.communication.edge.EdgeCommunicationGatewayAdapter

EdgeNotificationProperties
  -> EdgeCommunicationProperties

NotificationGatewayPort
  -> OutboundMessageGateway

SendNotificationPayload
  -> OutboundMessageRequest

NotificationTarget
  -> OutboundRecipient
```

`EdgeHmacSigner` should move to:

```text
common.communication.edge.EdgeHmacSigner
```

unless it is already shared by multiple edge integrations, in which case a later ADR may move it to `common.edge.security`.

## HMAC preservation

The edge adapter must preserve the current signing behavior:

```text
OutboundMessageRequest
  -> EdgeCommunicationRequest
  -> EdgeHmacSigner.sign(hmacSecret, edgeRequest)
  -> POST signed.rawJsonBody()
```

Required headers:

```text
X-Request-Id
Idempotency-Key
X-Tch-Timestamp
X-Tch-Signature
```

Important:

- The exact JSON body that is signed must be the exact body sent to `RestClient`.
- Do not reserialize after signing.
- Do not change timestamp/signature semantics in the server refactor.

## Edge endpoint path

Server P0 should support the target path:

```text
/internal/messages/send
```

Property target:

```yaml
tch:
  communication:
    edge:
      enabled: true
      base-url: http://tchalanet-edge-service:3000
      messages-path: /internal/messages/send
      hmac-secret: ${EDGE_HMAC_SECRET}
```

Server common communication MUST target `/internal/messages/send` only. Do not keep a `notifications-path` fallback.

## Document model

Generic technical classes can live in `common.document`:

```text
common.document.receipt.ReceiptModel
common.document.receipt.ReceiptLine
common.document.receipt.ReceiptSpan
common.document.pdf.ReceiptPdfRenderer
common.document.qr.QrRenderer
common.document.escpos.EscPosReceiptRenderer
```

Ticket-specific classes remain outside common:

```text
core.sales.application.receipt.AbstractTicketReceiptFormatter
core.sales.application.receipt.TicketReceiptFormatter
core.sales.application.query.model.TicketReceiptView
core.sales.application.query.model.TicketReceiptLineView
```

`TicketPdfBuilder` must be renamed because it accepts a generic `ReceiptModel`:

```text
common.print.pdf.TicketPdfBuilder
  -> common.document.pdf.ReceiptPdfRenderer
```

`common.qr.QrRenderer` becomes:

```text
common.document.qr.QrRenderer
```

## Receipt orchestration

`features.receipt` and `features.cashier` must not call each other.

Both may use the same lower-level primitives:

```text
QueryBus GetTicketReceiptViewQuery
common.document renderer
common.communication gateway when needed
```

Admin print/download flow:

```text
features.receipt.web
  -> QueryBus GetTicketReceiptViewQuery
  -> common.document
  -> file/bytes response
```

Cashier sell + optional receipt/delivery flow:

```text
features.cashier.web/app
  -> SellTicketCommand
  -> if receipt requested: GetTicketReceiptViewQuery + common.document
  -> if delivery requested: common.communication
  -> SaleResponse
```

## Non-goals

- Do not make generic outbound communication persistent in this P0.
- Do not redesign notification center tables.
- Do not target the removed legacy edge route from server common communication.
- Do not move `NotificationFlowRouter` to common.
- Do not make `common.document` aware of tickets, payouts, draws, approvals, or tenants.
