# Seller terminal ticket visibility

## Why

The cashier ticket report currently relies on an optional `sellerTerminalId` query
parameter. When a seller terminal omits that parameter, the tenant-scoped query
returns tickets from every seller terminal in the tenant, while the seller
terminal KPI query remains scoped to the authenticated terminal.

## What

- Derive the ticket-list terminal filter from the authenticated seller-terminal
  context for seller-terminal actors.
- Preserve tenant-wide visibility and explicit terminal filtering for tenant
  administrators and platform administrators.
- Add regression coverage for both actor types.

## Impact

This closes a seller-to-seller data visibility leak in the POS ticket report.
No database migration is required because the existing tenant RLS boundary is
supplemented by application-level seller-terminal scoping.

## Non-goals

- Changing tenant-admin reporting behavior.
- Changing analytics projection or reconciliation logic.
- Changing ticket verification rules for scanned public tickets.
