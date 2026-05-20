# OpenSpec change: harden-ticket-print-pipeline

## Why

Print PDF/ESC-POS/QR est central au POS. Le pipeline actuel fonctionne MVP mais mélange QR public API path, handlers, locale/context, publicCode lookup, PDF height fixed, ASCII issues.

## What

- Garder print dans `core.sales`.
- Corriger QR URL vers `/ticket/{publicCode}`.
- Renommer `TicketPrintViewPort` -> `TicketPrintReaderPort`.
- Supprimer QR by publicCode, remplacer par QR by ticketId.
- Durcir PDF/ESC-POS builders.

## Tasks

- [ ] Add `TicketVerificationUrlBuilder` under `core.sales.application.print`.
- [ ] Add config `tch.tickets.public.base-url` and `ticket-path-template`.
- [ ] Update PDF handler.
- [ ] Update ESC/POS handler.
- [ ] Replace QR PNG query by `GetTicketQrPngQuery(ticketId,sizePx)`.
- [ ] Dynamic PDF height.
- [ ] ESC/POS ASCII folding.
- [ ] Tests.

## Acceptance

- QR payload does not contain `/api/v1`.
- PDF/ESC/POS can be generated for ticket with multiple lines.
- ESC/POS bytes include init, QR commands, feed, cut.
- PDF does not silently truncate receipt lines.
