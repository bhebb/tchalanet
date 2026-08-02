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

The POS feature detail response also returns immutable outcome fields for each line:

- `resultStatus` (`PENDING`, `WON`, `LOST`, `VOID`, or `OVERRIDDEN` when available);
- `payoutAmountCents`, the applied payout amount captured for that line.

These fields let the web client distinguish a winning line from a pending or non-winning line
without inferring the result from pricing terms. The current contract deliberately does not guess
an exact winning rule when the backend has not persisted its rule code; it exposes the applied
amount and keeps the full pricing snapshot available for audit.

## Web presentation

The detail keeps the existing order: actions, identity summary, sale/draw blocks, then played
lines. The played-lines area is grouped by `gameCode`:

```text
Game section: Bòlèt
  12 · Boul · 10 HTG · [info]

Game section: Maryaj
  12-34 · Exact · 10 HTG · [info]
  12-34 · Inversé · 10 HTG · [info]
```

The source is an admin/audit detail, not customer-facing receipt content. Pricing terms are not
rendered inline. Each line exposes an accessible information control whose tooltip lists all
snapshot terms, their commercial label, amount/multiplier, and source. A `WON` line additionally
shows the translated `Barème appliqué` label next to the control; the tooltip remains the source
of truth for the complete snapshot. Pending, lost, void, and overridden lines do not claim that a
specific rule was applied.

Receipt/print payloads remain unchanged at the public presentation layer. The extra result fields
are internal projection data used by the POS feature response and admin detail only.
