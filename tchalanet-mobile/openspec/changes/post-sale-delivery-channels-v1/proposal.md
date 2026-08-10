# post-sale-delivery-channels-v1

## Status

Proposed — 2026-08-10

## Scope

This specification owns delivery after a ticket has been created: SMS,
WhatsApp, and email, according to the channels enabled by the tenant and the
capabilities/configuration of the active terminal.

It is separate from `pos-fast-sale-direct-print-v1`, which owns collapsing
`prepare` + `confirm` into one seller action and triggering direct print after
confirmation creates the ticket.

## Goals

- Define tenant channel policy and terminal availability clearly.
- Keep delivery independent from sale confirmation and physical printing.
- Give the seller truthful queued, sent, failed, and retry states.
- Make retries idempotent and auditable.

## Non-goals

- Changing sale preparation, confirmation, pricing, or Maryaj gratis behavior.
- Choosing or implementing Sunmi, Bluetooth, or ESC/POS printer adapters.
- Treating SMS, WhatsApp, or email as a physical print format.
