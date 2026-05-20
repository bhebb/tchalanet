# Change: core.sales Alignment

## Why

Sales must use the stabilized outlet/terminal/session/policy boundaries.

## What

Refactor sell, ticket, ticket_line, print/reprint, and list/detail reads.

## Non-goals

- Do not put sell rules in `features.cashier`.
- Do not let feature layer compute limits/payouts.
