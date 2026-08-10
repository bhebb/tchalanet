# Tasks

## 0 — Backend Contract Verification

- [x] Verify prepared-sale endpoints and confirm idempotency contract.
- [x] Verify legacy direct sell exists but is not safe for prepared promotion quick-sale.
- [x] Verify print endpoint accepts `printOptionsRequest`.
- [x] Verify backend supports `DocumentFormat.ESC_POS` and receipt paper sizes.

## 1 — Mobile Sale UX

- [x] Add quick-sale setting model and default it OFF.
- [x] Add controller method for one-tap quick sale: prepare, classify preview, confirm when eligible.
- [x] Implement one-tap chaining as `prepare once -> confirm existing preparation`; do not model it as a client-side transaction or retry prepare after confirm uncertainty.
- [x] Preserve explicit `Verifye -> Konfime` mode and existing edit-prepared-ticket path.
- [x] Show localized prepare/rejection/API errors inline without navigating away.
- [x] Preserve `preparationId` and the same sale-intent idempotency key across confirm retries.
- [x] Model prepared, confirming, sold, known-confirm-rejected, and unknown-confirm-outcome states separately; never show sale success after prepare alone.
- [x] Map known confirm business errors separately from timeout/5xx unknown outcomes.
- [ ] Persist/recover the pending preparation id and idempotency key across session refresh or app restart where feasible.
- [ ] Block a second sale while confirmation is pending or unresolved.
- [ ] Handle Maryaj gratis promotion lines from the stored preparation; never reconstruct or regenerate them implicitly.
- [ ] Expose explicit bounded Maryaj gratis regeneration before confirm and block it after confirm.
- [x] Add tests for accepted/no-review and unknown confirm outcome; remaining Maryaj regeneration and persistence cases are still pending.

## 2 — Print Contract

- [x] Extend mobile print request model/service to send `printOptionsRequest.outputFormat`.
- [x] Extend mobile print request model/service to send `printOptionsRequest.paperSize`.
- [x] Preserve `recordPrint`, `reprintReason`, `RETURN_FILE`, and locale behavior for the binary print response; do not add external delivery channels here.
- [ ] Add service tests for ESC/POS and PDF payloads.

## 3 — Generic Printer Service

- [x] Create `PrinterAdapter`, `PrinterCapability`, `PrinterDiagnostic`, and `PrinterService`.
- [x] Implement `SystemPdfPrintAdapter` as the existing `printing` fallback.
- [x] Implement paired Bluetooth ESC/POS adapter for external printers such as NETUM.
- [ ] Implement Sunmi adapter behind the generic interface.
- [ ] Add generic Android raw ESC/POS adapter only if a stable channel/plugin is selected.
- [ ] Add deterministic adapter-selection tests.

## 4 — Sale Completion Print UX

- [x] Replace direct `printTicket()` calls on success screen with `PrinterService`.
- [ ] Show inline print state: printing, printed, failed, retry, manual fallback.
- [x] Ensure auto-print on POS direct never opens PDF/system print UI.
- [ ] Keep ticket code and `Nouvo tikè` usable after print failure.
- [ ] Keep reprint audit dialog for seller-initiated reprints.

## 5 — Settings And Diagnostics

- [ ] Confirm whether tenant/admin receipt defaults are already editable in the admin portal; if not, create/link a backend/web OpenSpec follow-up.
- [x] Extend terminal-scoped backend settings for `printerMode`, `paperSize`, and optional adapter preference in `seller_terminal_settings`.
- [x] Keep Bluetooth address, Sunmi capability, permissions, and last test result local to `(tenantId, sellerTerminalId, appInstallationId)`; never treat a phone-global printer as a terminal identity.
- [ ] Reuse the existing `X-Tch-Act-As-Terminal` bridge when an authorized admin opens a seller terminal POS.
- [ ] Ensure mobile stores the active terminal only in the POS session, sends the bridge context consistently, and clears it on logout, tenant change, or terminal switch.
- [ ] Verify backend tenant/terminal eligibility and audit attribution for admin bridge requests.
- [ ] Do not use a client-supplied terminal id in sale/print request bodies as a substitute for the server-side context.
- [x] Add settings UI for printer mode and paper size.
- [x] Add test-print action.
- [ ] Store and expose last print diagnostic for support copy.
- [x] Localize all new labels in HT/FR/EN.

## 6 — Validation

- [x] Run focused Flutter unit/widget tests for sale and print.
- [ ] Add automated test coverage for the sale/print matrix: phone context, terminal bridge header, ESC/POS, PDF fallback, failure isolation, idempotency, terminal switching, and reprint audit.
- [x] Run `flutter analyze`.
- [ ] Run manual NETUM Bluetooth smoke for TEL-02, TEL-06, and TEL-16: pair printer, test print, sale auto-print, manual print, and reprint.
- [ ] Run manual Sunmi device smoke for TEL-05 and TEL-16: sale auto-print, manual print, and print failure/retry.
- [ ] Run regular-phone/admin-bridge smoke for TEL-03 and TEL-04.
- [ ] Run unsupported Android smoke for TEL-01, TEL-07, and TEL-11: fallback PDF remains manual and the sale screen survives.
- [ ] Run admin-bridge smoke from a regular phone against a terminal configured for Sunmi; verify no remote Sunmi claim and verify local printer/PDF fallback.

## 7 — Test Cases

The following cases are the acceptance matrix for local testing. A sale is
successful only after `confirm` succeeds; a printer failure must never undo or
duplicate the ticket.

| ID | Context | Setup | Action | Expected result |
|---|---|---|---|---|
| TEL-01 | Regular phone, no printer | `printerMode=AUTO`, no paired device | Sell a valid ticket | Ticket is created; no PDF or system print screen opens; seller can use manual PDF. |
| TEL-02 | Regular phone + NETUM classic SPP | Pair NETUM in Android settings; select it in POS settings | Send test print | Test receipt prints; selected device is saved locally. |
| TEL-03 | Regular phone + NETUM BLE | Select the BLE advertisement in POS settings | Send test print and sell | ESC/POS bytes print without leaving Tchalanet. |
| TEL-04 | Admin opens seller terminal | Admin uses seller-terminal list > `Ouvrir POS` | Configure/print from the admin phone | Server terminal context remains the seller terminal; printer pairing remains on the admin phone only. |
| TEL-05 | Direct POS printer unavailable | `printerMode=POS_DIRECT`, no selected device | Confirm a sale | Ticket remains sold; inline print error is shown; no PDF is opened automatically. |
| TEL-06 | Direct print transport failure | Selected printer powered off after preparation | Confirm a sale | Ticket is not recreated; retry/manual print is offered; same ticket/idempotency intent is preserved. |
| TEL-07 | Explicit PDF | `printerMode=SYSTEM_PDF` or manual fallback | Tap manual print | PDF/system print surface opens only after the seller action. |
| TEL-08 | SMS/email without printing | Tenant delivery channel enabled; printer auto print disabled | Confirm a sale and send delivery | Delivery is handled by the separate delivery flow; print adapter is not invoked. |
| TEL-09 | Prepare succeeds, confirm unknown | Simulate timeout/5xx after confirm request | Observe result and retry | No success screen is shown; seller sees unresolved confirmation and can retry confirm without prepare again. |
| TEL-10 | Maryaj gratis | Sale includes free promotion lines | Prepare, then confirm or retry confirm | Promotion lines come from the preparation; they are not regenerated implicitly. |
| TEL-11 | Reprint | Existing ticket, printer selected or unavailable | Reprint with an audit reason | Reprint uses the selected direct adapter or explicit PDF fallback and preserves the audit reason. |
| TEL-12 | Terminal switch | Two terminals, one phone | Configure printer on terminal A, switch to B | Terminal policy and local printer association do not leak across terminal contexts. |

Automated coverage currently verifies quick-sale success, unresolved confirm,
success-screen behavior, reprint errors, Flutter analysis, Android debug build,
and the backend POS profile service. TEL-02/TEL-03/TEL-04/TEL-05/TEL-06/TEL-10/TEL-12
still require physical or integration smoke testing.
