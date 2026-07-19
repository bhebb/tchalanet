# Cashier Ticket Builder Surface

## Why

The current POS sale form exposes the server contract as a succession of form
sections. A seller is actually composing a lottery ticket: the active draw must
be compact, number entry must follow the selected game shape, and the growing
basket must read like the thermal receipt that will be printed.

## What changes

- Replace the horizontal draw cards with one compact selected-draw strip and a
  `Change` action.
- Replace per-digit desktop fields with one grouped numeric field per selection
  segment; a Maryaj uses two visible groups, while a single-number game uses one.
- Render committed lines as a receipt-style ticket with a fixed total.
- Keep `prepare` server-side and present it as an inline verification state;
  the primary action then becomes confirmation of the prepared sale.
- Keep errors compact and contextual. A business rejection is not rendered as
  a page-failure surface.

## Non-goals

- Changing sales preparation, confirmation, pricing, limits, promotions or
  idempotency semantics.
- Computing totals, promotion lines or validation rules in Flutter.
- Introducing a new backend endpoint.
