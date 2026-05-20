# Change: core.payout Alignment

## Why

Payout must be independent from selling session being open. A winning ticket can be paid after the seller's sale session closed.

## What

Refactor payout workflow, DB, endpoints, and policy integration.

## Non-goals

- Do not require selling session to be OPEN for payout.
- Do not take tenantId from request body.
