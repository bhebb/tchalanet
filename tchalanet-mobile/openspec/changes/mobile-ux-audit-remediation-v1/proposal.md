# Proposal — Mobile UX Audit Remediation

## Why

The UX audit of Tchalanet Mobile identifies several small but repeated
frictions for a seller working quickly on a phone or POS terminal. The highest
impact items are the extra navigation after a sale, missing feedback on
interactive controls, silent empty/error states, incomplete history/reporting,
and settings that save without confirmation.

The separate print change
`pos-fast-sale-direct-print-v1` owns collapsing the `prepare` + `confirm`
two-step flow into a single seller action and triggering direct print immediately
after confirmation creates the ticket. This change owns the remaining
screen-level UX corrections from the audit.

Post-sale delivery channels (SMS, WhatsApp, email) are outside this change and
outside `pos-fast-sale-direct-print-v1`; they belong to the separate
`post-sale-delivery-channels-v1` specification.

## Goals

- Make frequent seller actions obvious and tactile.
- Make errors, empty states, and saved settings visible.
- Preserve the seller's active terminal and sale context.
- Make history and reports reliable beyond the first 50 tickets.
- Remove misleading or hardcoded UI text.

## Non-goals

- Changing sale pricing, promotion, Maryaj gratis generation, or confirmation
  semantics.
- Replacing the generic printer architecture or post-sale delivery channel
  contract.
- Adding unsupported QR scanning without a real scanner implementation.
