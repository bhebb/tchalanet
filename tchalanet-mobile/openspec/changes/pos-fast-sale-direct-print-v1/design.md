# Design — POS Fast Sale And Direct Print

## Scope boundary

This change owns collapsing the `prepare` + `confirm` two-step flow into a
single seller action and triggering direct print immediately after confirmation
creates the ticket.

Post-sale delivery channels are outside this change and outside the scope of
`pos-fast-sale-direct-print-v1`. Defining post-sale delivery channels (SMS,
WhatsApp, email) according to tenant-enabled channels and terminal
capabilities is the responsibility of the separate
`post-sale-delivery-channels-v1` specification.

## Current State

### Sale

Mobile currently exposes the prepared-sale flow as two visible seller actions:

1. `CashierTicketService.prepare()` posts to `/tenant/sales/preparations`.
2. `CashierTicketService.confirm()` posts to
   `/tenant/sales/preparations/{preparationId}/confirm` with `Idempotency-Key`.

That matches backend Maryaj-gratis rules. `ConfirmPreparedSaleCommand` confirms
exactly the persisted preparation and does not accept client lines again.

Backend also still has `POST /tenant/tickets`, but that endpoint creates a
ticket directly from request lines. It is not the safe quick-sale replacement
for mobile because it bypasses the prepared/pinned promotion confirmation
contract.

### Print

Mobile currently posts:

```json
{
  "recordPrint": true,
  "deliveryOptions": ["RETURN_FILE"]
}
```

No `printOptionsRequest` is sent, so backend defaults resolve to PDF/A4. Mobile
then calls `Printing.layoutPdf`, which opens the system print/PDF UI. Here
`RETURN_FILE` means return rendered print bytes to the client; it does not mean
post-sale SMS, WhatsApp, or email delivery.

Backend already supports the target POS payload:

```json
{
  "printOptionsRequest": {
    "outputFormat": "ESC_POS",
    "paperSize": "RECEIPT_58MM"
  },
  "recordPrint": true,
  "deliveryOptions": ["RETURN_FILE"],
  "buyerLocale": "ht"
}
```

`ESC_POS + A4` is rejected by backend profile resolution. `ESC_POS` returns
`application/octet-stream` and `.bin`; PDF returns `application/pdf`.

## Target UX

### Explicit Mode

Default behavior remains:

```text
Ajouter lignes -> Verifye -> preview -> Konfime -> success -> auto-print
```

### Quick Sale Mode

When enabled:

```text
Ajouter lignes -> Vann -> prepare -> if accepted and no review needed, confirm -> success -> auto-print
```

Quick mode still uses both backend calls internally. "One tap" is a UX contract,
not a literal database transaction across two HTTP requests. Mobile SHALL call
`prepare` once, then call `confirm` only after the preparation is accepted and
eligible for automatic confirmation.

If prepare fails or returns rejected, show the existing localized error/preview.
If prepare returns generated promotion content, warnings, or any seller-review
condition, stop on the preview screen and require explicit confirmation.

Prepare success is never a sale-success signal. The completion screen titled
`Siksè` is reachable only after a positive confirm response containing a
ticket id. While confirm is pending, the seller sees `Vente en cours` and the
sale action is disabled so a second intent cannot be started accidentally.

The confirmation state must distinguish known business failure from unknown
network outcome:

```text
editing
  -> preparing
  -> prepared
  -> confirming
       -> sold(ticketId)
       -> confirmRejected(problem, no ticket known)
       -> confirmUnknown(preparationId, idempotencyKey)
```

- A known stock, limit, cutoff, validation, or expired-preparation response is
  a failed confirmation. Show the backend error, keep the seller off the
  success screen, and allow an explicit edit/discard/reprepare path.
- A timeout, connection loss, or 5xx response is an unknown outcome. The
  client must not claim that no ticket exists. It keeps the preparation id and
  idempotency key, offers `Vérifier/reprendre la vente`, and retries the same
  confirm operation. It must not call prepare again.
- A session refresh or app restart must preserve a pending confirmation record
  long enough to reconcile it, or require an explicit support/recovery path
  before the seller can start another sale.

### Preparation lifecycle and Maryaj gratis

The preparation is the anti-regeneration boundary. Mobile must keep the
`preparationId` and the stable sale-intent idempotency key in the current sale
attempt:

```text
sale intent -> prepare once -> stored preparation -> confirm(preparationId, key)
                                      |
                                      +-> optional explicit regeneration
                                          before confirm only
```

For Maryaj gratis, the generated promotion line is part of the stored
preparation. Mobile must not reconstruct it, send it back as client lines, or
call `prepare` again when `confirm` times out or fails. A confirm retry uses the
same `preparationId` and same `Idempotency-Key`; a successful replay returns the
same ticket rather than generating another Maryaj line.

Regeneration is a separate seller decision. It calls the dedicated
`POST /tenant/sales/preparations/{preparationId}/promotion-lines/{lineRef}/regenerate`
endpoint, updates the same preparation, and then confirms that updated
preparation. Once confirmation starts or the preparation is confirmed,
regeneration is no longer allowed.

## Review Gate

Implement a local gate such as:

```text
canAutoConfirm(preview):
  preview.isAccepted
  && preview.promotionLines.isEmpty
  && preview.notices.isEmpty
  && preview.preparationId is present
```

If backend later adds an explicit `requiresSellerReview` field, prefer that
field over local inference.

## Printer Architecture

Add feature-local abstractions:

```text
features/cashier/tickets/printing/
  printer_service.dart
  printer_adapter.dart
  printer_capability.dart
  printer_diagnostic.dart
  adapters/
    bluetooth_escpos_printer_adapter.dart
    sunmi_printer_adapter.dart
    android_raw_escpos_adapter.dart
    system_pdf_print_adapter.dart
```

`PrinterService.printTicket(ticketId, intent)` is responsible for:

1. reading POS/profile settings;
2. probing adapters;
3. selecting output format/paper size;
4. requesting bytes from backend;
5. dispatching bytes to the selected adapter;
6. returning a structured `PrintResult`.

The UI never calls `Printing.layoutPdf` directly. It only calls
`PrinterService`.

## Adapter Selection

Priority:

1. Bluetooth ESC/POS adapter when a paired external printer such as NETUM is
   selected and reachable.
2. Sunmi adapter when running on a Sunmi device and service is available.
3. Generic raw ESC/POS adapter when the Android device exposes a supported raw
   print channel.
4. System PDF adapter only when configured as fallback or when the user manually
   chooses fallback after a failure.

This keeps both a purchased Sunmi and an external NETUM-style Bluetooth printer
useful without making the app vendor-specific. Future PAX/Urovo/network printer
adapters plug into the same interface.

## Backend Request Mapping

Physical POS direct print:

```json
{
  "printOptionsRequest": {
    "outputFormat": "ESC_POS",
    "paperSize": "RECEIPT_58MM"
  },
  "recordPrint": true,
  "deliveryOptions": ["RETURN_FILE"],
  "buyerLocale": "ht"
}
```

Phone/system fallback:

```json
{
  "printOptionsRequest": {
    "outputFormat": "PDF",
    "paperSize": "RECEIPT_58MM"
  },
  "recordPrint": true,
  "deliveryOptions": ["RETURN_FILE"],
  "buyerLocale": "ht"
}
```

Manual reprint keeps `reprintReason`.

## State Model

Add print state separate from sale state:

```text
idle
requestingBytes
printing(copyIndex, copyCount)
printed
failed(error, diagnostic, canFallback)
fallbackPrinting
```

Sale success is never downgraded by print failure. The completion screen keeps
showing ticket code and new-ticket action.

## Settings

Use a hierarchy, not one flat settings page:

### Tenant/admin settings

These are policy and receipt-rendering defaults. They belong in tenant/admin
configuration because they should be consistent for the bank/tenant:

- receipt template key;
- receipt header and footer messages;
- QR visibility;
- default receipt paper size (`RECEIPT_58MM` or `RECEIPT_80MM`);
- allowed print output modes (`PDF`, `ESC_POS`) if product wants to restrict
  devices.

Backend already has `TenantInternalDocumentConfig.ReceiptConfig` for:
`enabled`, `headerMessage`, `footerMessage`, `defaultPaperSize`, `showQrCode`,
and `defaultTemplateKey`.

### Terminal/device settings

These are operational preferences for the seller terminal, not for an
arbitrary phone. The backend already scopes `seller_terminal_settings` by
`(tenant_id, seller_terminal_id)` and resolves the terminal from the
authenticated seller-terminal identity:

- a phone is only the client device;
- the seller-terminal identity is the operational actor;
- a client request must not switch terminals by sending another
  `sellerTerminalId`;
- sale, print, reprint, audit, limits, and financial attribution keep the
  authenticated seller-terminal context.

The terminal-level settings are:

- `receipt.autoPrint` existing.
- `receipt.copyCount` existing.
- `receipt.printerMode`: `AUTO | POS_DIRECT | SYSTEM_PDF`.
- `receipt.paperSize`: `RECEIPT_58MM | RECEIPT_80MM`.
- `receipt.adapterPreference`: optional stable adapter id, e.g. `sunmi`,
  `bluetooth_escpos`, `sunmi`, `android_raw_escpos`, `system_pdf`.

Backend already persists terminal-level `autoPrint` and `copyCount` in
`seller_terminal_settings`. Persisting `printerMode`, `paperSize`, and adapter
preference should extend that same terminal settings surface, because a tenant
can have mixed hardware.

The Bluetooth printer identity is different. A Bluetooth address, Sunmi
capability, permission state, and last test result belong to the physical app
installation and SHALL be stored locally, keyed by:

```text
tenantId + sellerTerminalId + appInstallationId
```

This prevents one phone used for two terminals from silently reusing the wrong
printer. The local binding is operational only; it cannot change the tenant's
receipt policy or the terminal that the backend attributes the sale to.

### Phone used for a seller terminal

There are two supported authentication cases:

1. A seller logs into the phone using the seller-terminal identity. The backend
   resolves the seller terminal normally, and the phone loads that terminal's
   settings.
2. An admin opens a seller terminal from the terminal list and enters its POS.
   The existing admin-to-terminal bridge sends
   `X-Tch-Act-As-Terminal: <sellerTerminalId>`. The backend accepts this only
   for the allowed admin roles and injects the selected terminal into
   `TchRequestContext` before POS services run.

The active terminal must be part of the mobile POS session and visible in the
UI. Every POS request in that session must carry the bridge context through the
authenticated client/header mechanism. The mobile app must not expose a free
terminal-id field in the sale or print request, and it must not allow a stale
terminal selection to survive a logout or tenant change.

The server remains responsible for checking that the selected terminal belongs
to the effective tenant and is eligible for the operation. Audit data should
retain both the admin actor and the operational seller terminal when the bridge
is used.

### Resolution order

For each print:

```text
tenant receipt defaults
  -> terminal/device override
  -> local printer binding for this terminal on this phone
  -> runtime adapter capability probe
  -> manual fallback choice
```

Examples:

- Terminal default paper is `RECEIPT_58MM`, Sunmi probe reports a 58mm
  printer: use 58mm for POS direct unless the terminal setting says otherwise.
- Terminal default paper is `RECEIPT_58MM`, the selected NETUM printer is a
  58mm Bluetooth printer: use `ESC_POS + RECEIPT_58MM` for that terminal.
- Tenant allows PDF and ESC/POS, phone has no POS adapter: use PDF fallback.
- Terminal A is normally used on a Sunmi, but an admin opens terminal A from a
  regular phone: do not claim that the phone can reach the Sunmi engine. Use a
  locally paired printer if one is available, otherwise offer manual PDF. A
  remote/network printer bridge would be a separate adapter and deployment
  capability.
- Terminal A is configured for 80mm, but terminal B has a 58mm built-in printer:
  the terminal setting wins for that terminal only.

## Testing

Unit tests:

- quick sale auto-confirms only accepted/no-review previews;
- quick sale stops on rejected or review-required previews;
- idempotency key is stable across retry of the same quick-sale intent;
- print service requests `ESC_POS + RECEIPT_58MM/80MM` for POS direct;
- print service requests PDF only for fallback;
- adapter selection is deterministic;
- print failure does not reset or invalidate sale success.

Widget tests:

- sale button changes based on quick-sale setting;
- success screen shows inline printing states and retry;
- fallback PDF is manual on POS.

Manual device tests:

- Sunmi physical print from sale success.
- Sunmi reprint with reason.
- Unsupported Android phone falls back to system PDF without breaking sale.
