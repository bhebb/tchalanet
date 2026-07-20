# Seller Ticket History V1

## Why

The seller history already calls the authoritative `features.pos` ticket APIs,
but the mobile surface still renders French literals, filters only after loading
an arbitrary local slice, and exposes a reprint button that does nothing.

`POST /tenant/cashier/tickets/{ticketId}/print` is auditable: after the first
print, `core.sales` requires a reprint reason of at least ten characters. The
mobile reprint action pre-fills a stable seller-request reason. The seller can
select another common reason or edit the value before confirmation.

## What Changes

- Query the existing `features.pos` ticket list with its date and text filters.
- Localize history, detail, print/reprint, empty, and recovery states in HT/FR/EN.
- Make the History reprint action and ticket-detail print action invoke the
  native PDF print workflow with a pre-filled audit reason and presets.
- Keep the initial post-sale print reason-free; only seller-initiated reprints
  require a reason.

## Non-goals

- No new backend endpoint, print policy, or delivery channel.
- No change to ticket cancellation rules or settlement.
- No local inference of whether a reprint is required: the History and detail
  actions are explicitly reprints and always submit the confirmed audit reason.

## Impact

- Mobile: cashier ticket service, history/detail views, printing helper,
  translations, and widget/unit tests.
- Backend: read-only contract verification; no code change expected.
