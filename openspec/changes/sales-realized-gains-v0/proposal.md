# Sales Realized Gains V0

## Why

Issue #254 asks the V0 sale flow to stop presenting sale-time potential gains and to rely on
realized gains after settlement.

## What Changes

- Cashier legacy preview/sell BFF endpoints are removed.
- POS sale uses the canonical prepared-sale flow only: prepare then confirm.
- Prepared sales validate games, POS visibility, bet options, selection policy, and stake bounds against tenant game config.
- POS ticket details and receipt print no longer expose or print max/potential payout.

## Impact

- No database migration in this slice.
- Internal sales/settlement fields remain available for existing result calculation.
- Public POS surfaces move toward realized settlement gains only.
