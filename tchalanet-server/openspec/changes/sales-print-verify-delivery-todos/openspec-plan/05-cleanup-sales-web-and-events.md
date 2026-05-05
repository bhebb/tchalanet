# OpenSpec change: cleanup-sales-web-and-events

## Why

Sales web layer and events need cleanup to match Tchalanet standards: thin controllers, safe context, no body impersonation, idempotent listeners, stable query pagination.

## What

- Split controllers.
- Fix `TicketWebMapper`.
- Add idempotence to `SalesLedgerListener`.
- Fix generators collision retry.
- Harden bridge adapter.

## Tasks

- [ ] Split controllers.
- [ ] Fix override mapping.
- [ ] Replace `PageRequest` in application query with `TchPageRequest`.
- [ ] Invalid status -> 400.
- [ ] Add `ProcessedEventPort` to ledger listener.
- [ ] Add DB unique constraints/retry for ticket codes.
- [ ] Fix `SalesTicketAdminAdapter` empty session/no-op methods.

## Acceptance

- No sensitive action accepts `performedBy` from body.
- Query handlers side-effect free.
- Ledger listener idempotent.
- Ticket code/publicCode uniqueness enforced by DB + retry.
