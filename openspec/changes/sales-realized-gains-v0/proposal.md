# Sales Realized Gains V0

## Why

Issue #254 asks the V0 sale flow to stop presenting sale-time potential gains and to rely on
realized gains after settlement.

## What Changes

- Cashier legacy preview/sell BFF endpoints are removed.
- POS sale uses the canonical prepared-sale flow only: prepare then confirm.
- Prepared sales validate games, POS visibility, bet options, selection policy, and stake bounds against tenant game config.
- Ticket line preparation plans implicit best-match coverages from enabled tenant options and keeps the full stake on each alternative.
- POS game runtime exposes selection policy so the web sale form hides bet options for implicit best-match games.
- Ticket lines snapshot commercial selection policy and explicit bet option label for stable reprint.
- Seller-terminal sale validation blocks terminals that are not active or still require a PIN change.
- POS sale/preparation, cashier recent tickets, ticket details, receipt, and print DTOs no longer expose or print max/potential payout.

## Impact

- No database migration in this slice.
- Internal sales/settlement fields remain available for existing result calculation.
- Public POS sale/print surfaces move toward realized settlement gains only.
