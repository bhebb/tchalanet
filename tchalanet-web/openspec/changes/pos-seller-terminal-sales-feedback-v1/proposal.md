# Change: pos-seller-terminal-sales-feedback-v1

## Why

Seller terminal creation and POS sale flows need clearer operational ergonomics for tenant admins and must keep POS code extractable for a future `pos-portal`.

## What Changes

- Make seller terminal creation codes easier to understand and validate.
- Promote successful entity creation feedback to shell-level confirmation.
- Keep POS sale files under `features/pos`, not under admin seller terminal folders.
- Prepare sale warnings handling from backend notices without blocking successful sales.
- Improve POS number entry for Maryaj and Loto 3/4/5.
- Show a post-sale dialog with print/reprint and tenant-enabled communication actions.
- Add ticket-list reprint support using the sale terminal context.

## Impact

- Admin portal seller terminal creation UX.
- Shared shell feedback severity model.
- POS feature placement and routes.
- POS sale entry UX and ticket list actions.
