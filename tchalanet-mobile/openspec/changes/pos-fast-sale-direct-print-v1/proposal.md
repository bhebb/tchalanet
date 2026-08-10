# pos-fast-sale-direct-print-v1

## Status

Proposed — 2026-08-10

## Why

The current POS sale path still forces an experienced machann through two
visible actions: `Verifye` then `Konfime`. That remains safe, but it is slow on
the highest-volume path and the UX audit explicitly called it out as a daily
friction.

The current print path is also not POS-native. After a sale, mobile calls
`POST /tenant/cashier/tickets/{ticketId}/print`, receives bytes, and passes them
to `Printing.layoutPdf`. On Android POS terminals this launches an external or
system print/PDF surface that visually replaces Tchalanet, so the seller has to
force their way back into the app before continuing.

Backend check completed:

- Prepared sale remains the canonical Maryaj-gratis/promotions-safe flow:
  `POST /tenant/sales/preparations` then
  `POST /tenant/sales/preparations/{preparationId}/confirm`.
- Legacy direct sell exists at `POST /tenant/tickets`, but it re-runs the sale
  command from client lines and does not preserve the prepared/pinned promotion
  review contract. Mobile quick sale must not switch to that path.
- Ticket print already accepts `PrintTicketRequest.printOptionsRequest` and the
  document platform supports `DocumentFormat.ESC_POS` with
  `PaperSize.RECEIPT_58MM` / `RECEIPT_80MM`.
- Mobile currently omits `printOptionsRequest` and only sends
  `deliveryOptions: ['RETURN_FILE']`, so backend defaults resolve to PDF/A4.

## What

This change owns collapsing the `prepare` + `confirm` two-step flow into a
single seller action and triggering direct print immediately after confirmation
creates the ticket.

### A — One-tap quick sale

- Add a seller setting for quick sale mode. Default OFF.
- When OFF, keep the current explicit `Verifye` then `Konfime` flow.
- When ON, the primary action is one tap: prepare the sale, surface any
  validation/rejection error, and only auto-confirm when the prepared sale is
  accepted and has no seller decision still required.
- Preserve the same idempotency key semantics already used by confirm. Retry
  after a network failure must not create a second ticket.
- Keep the prepared preview screen as the fallback whenever the backend returns
  notices, promotion lines, or a state that requires seller review before
  confirmation.

### B — Generic POS printer abstraction

- Introduce a mobile `PrinterService` / `PrinterAdapter` boundary. App code
  depends on a generic interface, not on Sunmi APIs.
- Add adapters in priority order:
  1. `BluetoothEscPosPrinterAdapter` for external ESC/POS printers such as the
     NETUM portable thermal printer.
  2. `SunmiPrinterAdapter` for integrated Sunmi terminals.
  3. `AndroidRawEscPosAdapter` or equivalent generic raw ESC/POS channel where
     supported.
  4. `SystemPdfPrintAdapter` fallback using `printing` for phones, emulators,
     or unsupported devices.
- Add a printer capability probe that reports: available, vendor/model,
  supported formats, paper width, and failure reason.
- Request ESC/POS bytes from backend on physical POS:
  `printOptionsRequest: { outputFormat: 'ESC_POS', paperSize: 'RECEIPT_58MM' }`
  or `RECEIPT_80MM` based on device/settings.
- Keep PDF fallback explicit and non-blocking. POS auto-print must not open a
  PDF or external print UI unless the seller manually chooses fallback.

### C — Sale completion print UX

- After successful sale, stay inside Tchalanet and show print status inline:
  `printing`, `printed`, `failed`, and `fallback available`.
- Auto-print uses the configured copy count and records the print once per copy
  request according to the existing backend policy.
- Manual reprint keeps the existing audit-reason dialog and sends the confirmed
  reason.
- If printing fails after the sale is confirmed, do not imply sale failure.
  Show the ticket code, keep `Nouvo tikè` available, and expose `Réessayer
  impression`.

### D — Settings and diagnostics

- Use a two-level configuration model:
  - Tenant/admin defaults for receipt rendering policy: default paper size,
    QR visibility, template/header/footer, and allowed output modes.
  - Terminal/device settings for operational behavior: auto-print, copy count,
    printer mode, preferred paper width, selected adapter/device.
- Add receipt settings for terminal printer mode and paper width:
  `Auto`, `POS direct`, `System PDF fallback`; `58mm`, `80mm`.
- Add a test-print action from Settings/Profile.
- Store last print diagnostic locally enough to copy/share with support:
  adapter, output format, paper size, backend request id, content type, byte
  length, and platform error.

## Impact

- Mobile implementation change only unless backend contract gaps are discovered
  during implementation.
- Uses existing backend print endpoint and existing ESC/POS document renderer.
- Backend already has tenant document receipt config
  (`document.receipt.defaultPaperSize`, `showQrCode`, `defaultTemplateKey`,
  `headerMessage`, `footerMessage`) and terminal receipt settings
  (`receipt_auto_print`, `receipt_copy_count`).
- Persisting terminal `printerMode`, `paperSize`, and adapter choice requires
  extending `seller_terminal_settings` or a small device-settings store.
- Adds one device-specific adapter for Sunmi behind a generic interface.
- Changes sale-page controller behavior, sale-completion print behavior,
  receipt settings, tests, and i18n labels.

## Non-goals

- Do not remove the explicit review flow; it remains default and is still used
  whenever the server says review is needed.
- Do not use `POST /tenant/tickets` for the quick-sale path.
- Do not make backend send bytes directly to the physical printer. Printing is
  client-side.
- Do not implement Bluetooth printer pairing beyond a minimal generic adapter
  if the selected plugin/channel can list and connect to paired Bluetooth
  thermal printers.
- Do not add SMS/WhatsApp/email delivery changes.
- Post-sale delivery through SMS, WhatsApp, or email is explicitly outside this
  change and outside the scope of `pos-fast-sale-direct-print-v1`; it belongs to
  the separate `post-sale-delivery-channels-v1` specification.

## Context packs

- `tchalanet-mobile/AGENTS.md`
- `tchalanet-mobile/openspec/project.md`
- `.agents/skills/openspec-workflow/SKILL.md`

## Near-code references

- `tchalanet-mobile/lib/features/cashier/tickets/data/services/cashier_ticket_service.dart`
- `tchalanet-mobile/lib/features/cashier/tickets/presentation/view_models/sell_controller.dart`
- `tchalanet-mobile/lib/features/cashier/tickets/presentation/views/cashier_sell_page.dart`
- `tchalanet-mobile/lib/features/cashier/tickets/presentation/views/cashier_sell_success_page.dart`
- `tchalanet-mobile/lib/features/cashier/tickets/presentation/print_ticket_action.dart`
- `tchalanet-mobile/lib/features/cashier/home/presentation/views/seller_terminal_profile_page.dart`
- `tchalanet-mobile/lib/features/cashier/home/data/models/pos_profile_models.dart`

## Backend references verified

- `tchalanet-server/tchalanet-core/src/main/java/com/tchalanet/server/core/sales/internal/infra/web/SalePreparationController.java`
- `tchalanet-server/tchalanet-core/src/main/java/com/tchalanet/server/core/sales/internal/infra/web/TicketSalesController.java`
- `tchalanet-server/tchalanet-features/src/main/java/com/tchalanet/server/features/pos/tickets/PosTicketsController.java`
- `tchalanet-server/tchalanet-features/src/main/java/com/tchalanet/server/features/pos/tickets/model/PrintTicketRequest.java`
- `tchalanet-server/tchalanet-features/src/main/java/com/tchalanet/server/features/pos/tickets/app/PosTicketReceiptService.java`
- `tchalanet-server/tchalanet-platform/src/main/java/com/tchalanet/server/platform/document/api/model/DocumentFormat.java`
- `tchalanet-server/tchalanet-platform/src/main/java/com/tchalanet/server/platform/document/api/model/PaperSize.java`
