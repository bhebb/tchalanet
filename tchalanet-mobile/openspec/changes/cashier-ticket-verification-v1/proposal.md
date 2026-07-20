# Cashier Ticket Verification V1

## Why

The POS verifies ticket eligibility with `features.pos`, but the current
surface exposes a fake payout action and ignores the localized title/message
keys already returned by the backend.

## What changes

- Render verification title, message, and safe amount parameters from the
  server's stable localization keys.
- Remove the non-functional payout affordance: sellers pay a winner outside
  the application in V0.
- Use the verified ticket identifier to enable the existing ticket-detail page.

## Non-goals

- No payout or cash ledger workflow.
- No QR camera integration; manual code and URL entry remain supported.
