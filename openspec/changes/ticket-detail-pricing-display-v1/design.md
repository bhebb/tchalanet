# Design

## Contract

Each ticket line returns a `pricingTerms` list because one game can have multiple applicable
variants. Each term contains:

- settlement rule code;
- bet option and label when applicable;
- payout rule type;
- multiplier or fixed amount;
- source (`SELLER_TERMINAL_OVERRIDE` or `TENANT_DEFAULT`).

The response is a read projection of the ticket snapshot. It must never query current pricing
configuration or recompute values.

## Web presentation

The detail keeps the existing order: actions, identity summary, sale/draw blocks, then played
lines. The played-lines area is grouped by `gameCode`:

```text
Game section: Bòlèt
  12 · Boul · 10 HTG · Barème x20 · Vendeur

Game section: Maryaj
  12-34 · Exact · 10 HTG · Barème x50 · Tenant
  12-34 · Inversé · 10 HTG · Barème x30 · Vendeur
```

The source is an admin/audit detail, not customer-facing receipt content. If all terms in a game
section share one rule, the UI may show a compact section-level summary; otherwise it renders the
rule on each line.
