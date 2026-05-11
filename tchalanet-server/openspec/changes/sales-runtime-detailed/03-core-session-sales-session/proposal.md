# Change: core.session SalesSession

## Why

Sales session is seller-scoped. It is not primarily terminal-scoped.

Tickets remain attached to the sales session after it closes. Payout can happen after the selling session is closed.

## What

Refactor session commands, queries, DB, and endpoints around `SalesSession`.

## Non-goals

- Do not block payout only because selling session is closed.
- Do not make feature overview the only way to read sessions.
