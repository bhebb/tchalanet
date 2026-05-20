# Tasks: P0 — Server extraction

## 1. Create common.communication package

- [x] Create `com.tchalanet.server.common.communication.api`.
- [x] Add `CommunicationChannel` with external-only channels:
  - `EMAIL`
  - `SMS`
  - `WHATSAPP`
  - `SLACK`
- [x] Add `OutboundRecipient`.
- [x] Add `OutboundMessageRequest`.
- [x] Add `OutboundMessageResult` only if existing code needs returned status; otherwise defer.
- [x] Add `OutboundMessageGateway`.

## 2. Move/rename edge transport adapter

- [x] Move `EdgeNotificationGatewayAdapter` to `common.communication.edge.EdgeCommunicationGatewayAdapter`.
- [x] Rename `EdgeNotificationProperties` to `EdgeCommunicationProperties`.
- [x] Rename `EdgeNotificationException` to `EdgeCommunicationException`.
- [x] Move `EdgeHmacSigner` to `common.communication.edge.EdgeHmacSigner`.
- [x] Rename injected `RestClient` bean to `edgeCommunicationClient` if necessary.
- [x] Update configuration prefix to `tch.communication.edge`.
- [x] Add canonical property `messagesPath` / `messages-path`.
- [x] Remove old `notificationsPath`; target `messagesPath` only.

## 3. Preserve HMAC behavior

- [x] Keep signing before HTTP call.
- [x] Keep using `signed.rawJsonBody()` as the POST body.
- [x] Keep headers:
  - `X-Request-Id`
  - `Idempotency-Key`
  - `X-Tch-Timestamp`
  - `X-Tch-Signature`
- [x] Ensure the signed JSON is not reserialized after signing.
- [x] Ensure request ID and idempotency key are stable within a single send operation.
- [x] Add/adjust tests for HMAC headers and raw signed body.

## 4. Keep NotificationFlowRouter in core.notification

- [x] Do not move `NotificationFlowRouter` to `common`.
- [x] Keep it in `core.notification.application.flow` or rename package to `application.routing` only if desired.
- [x] Keep draw-result/draw-settled routing logic unchanged.
- [x] Keep `NotificationFlowProperties` in notification config.
- [x] Keep behavior for:
  - watched slots;
  - Slack info toggles;
  - email detail toggles;
  - default Slack channel for batch draw notifications.

## 5. Add notification -> outbound mapper

- [x] Create `core.notification.application.mapper.OutboundMessageMapper`.
- [x] Map `SendNotificationCommand + NotificationRecipient` to `OutboundMessageRequest`.
- [x] Map external channels only:
  - `EMAIL`
  - `SMS`
  - `WHATSAPP`
  - `SLACK`
- [x] Reject or skip `IN_APP` in this mapper.
- [x] Preserve metadata:
  - eventId;
  - severity;
  - title;
  - message;
  - requestId;
  - channelKey;
  - tenantId;
  - userId;
  - locale;
  - type/template key.

## 6. Update SendNotificationCommandHandler

- [x] Dispatch by `NotificationChannel`.
- [ ] For `IN_APP`, create/persist notification center item in `core.notification`.
- [ ] For external channels, map to `OutboundMessageRequest` and call `OutboundMessageGateway`.
- [x] Ensure `IN_APP` is never sent to edge-service.
- [ ] Ensure external send failures do not corrupt in-app notification persistence unless current behavior intentionally fails the command.

## 7. Create common.document package

- [x] Create `com.tchalanet.server.common.document.receipt`.
- [x] Move/rename generic receipt classes:
  - `common.print.receipt.ReceiptModel` -> `common.document.receipt.ReceiptModel`
  - `common.print.receipt.ReceiptLine` -> `common.document.receipt.ReceiptLine`
  - `common.print.receipt.ReceiptSpan` -> `common.document.receipt.ReceiptSpan`
- [x] Move `common.qr.QrRenderer` to `common.document.qr.QrRenderer`.
- [x] Rename `common.print.pdf.TicketPdfBuilder` to `common.document.pdf.ReceiptPdfRenderer`.
- [x] Rename method:
  - `buildReceiptPdf(...)` -> `renderReceiptPdf(...)` or `render(...)`.
- [x] Update imports everywhere.

## 8. Keep ticket receipt formatting in core.sales

- [x] Keep `AbstractTicketReceiptFormatter` out of `common`.
- [ ] Move it to `core.sales.application.receipt` if not already there.
- [x] Update imports from `common.print.receipt.*` to `common.document.receipt.*`.
- [x] Keep ticket-specific logic there:
  - `TicketPrintView`;
  - `TicketPrintLine`;
  - `BetType`;
  - ticket code/public code;
  - terminal/outlet/draw labels;
  - Haitian game code display.
- [ ] Add TODO/follow-up for timezone: avoid fixed UTC receipt timestamps if tenant/outlet zone should be displayed.

## 9. Update features.receipt

- [x] Ensure receipt endpoints live in `features.receipt`.
- [x] Receipt endpoint calls `GetTicketReceiptViewQuery` or existing `TicketPrintView` query from `core.sales`.
- [x] Receipt endpoint uses `common.document` to render PDF/ESC_POS/HTML.
- [x] Receipt endpoint does not mutate ticket lifecycle.
- [x] Receipt endpoint does not send external communication.

## 10. Update features.cashier

- [ ] Ensure cashier flow does not call `features.receipt`.
- [ ] For sell flow, call `SellTicketCommand`.
- [ ] If receipt is requested, call the same lower-level sales query and `common.document`.
- [ ] If delivery is requested, call `common.communication`.
- [ ] Return receipt action URLs/metadata when appropriate.
- [ ] Do not duplicate receipt business read model assembly.

## 11. Tests

- [x] HMAC signature headers are added by `EdgeCommunicationGatewayAdapter`.
- [x] Raw signed JSON body is sent.
- [x] `IN_APP` is not sent to edge.
- [x] `SLACK` maps to `channelKey`.
- [x] `EMAIL`, `SMS`, `WHATSAPP` map to recipient address.
- [ ] `NotificationFlowRouter` behavior is unchanged.
- [x] `ReceiptPdfRenderer` renders a simple `ReceiptModel`.
- [x] `QrRenderer` package move does not break existing renderers.
- [ ] Cashier does not depend on receipt feature package.
- [x] No class in `common.document` imports `core.sales`.
- [x] No class in `common.communication` imports `core.notification`.

## 12. Cleanup

- [x] Remove old `common.notification` generic transport classes if replaced.
- [x] Remove old `core.notification.infra.external.EdgeNotificationGatewayAdapter`.
- [x] Remove old `common.print` package only if all classes moved.
- [ ] Update package docs/README if present.
- [ ] Run server tests and compilation.
