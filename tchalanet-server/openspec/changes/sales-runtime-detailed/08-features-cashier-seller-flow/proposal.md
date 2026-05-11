# Change: features.cashier Seller Flow

## Why

Seller/cashier screens need a BFF that aggregates session, outlet, terminal, draws, games, limits, and recent tickets.

## What

Implement feature endpoints for seller UX only.

## Non-goals

- Do not implement sell business rules here.
- Do not calculate payouts/limits here.
