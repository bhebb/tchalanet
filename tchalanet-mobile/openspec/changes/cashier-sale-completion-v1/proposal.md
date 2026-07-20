# Cashier Sale Completion V1

## Why

The confirmed sale screen already prints the ticket without issuing another
sale request, but it exposes French literals even when the seller selected
Haitian Creole or English.

## What changes

- Localize the confirmed-sale screen in HT, FR, and EN.
- Keep the existing one-shot post-confirm print action and its manual retry.
- Keep the path back to the POS home as the new-ticket action.

## Non-goals

- No change to sale preparation, confirmation, idempotency, or print audit
  policy.
- No SMS, email, or WhatsApp delivery in V0.
