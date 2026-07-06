# Proposal: ticket-receipt-draw-identity-v1

## Summary

Expose structured draw identity fields on ticket verification and receipt/print models so web and
server receipt formatters do not treat legacy pre-composed draw channel labels as canonical.

## Goals

- Add result-slot/channel/provider identity fields to public ticket verification draw payloads.
- Add the same identity fields to server ticket print/receipt models.
- Format server receipt draw labels from structured fields when present, with legacy labels as
  fallback only.
- Keep server receipt formatting data-driven: no hard-coded provider-name map and no hidden
  slot translation switch in `core.sales`. Localized labels such as French or Haitian Creole are
  resolved through receipt i18n overrides first (`receipt.draw.identity.<SLOT_KEY>`,
  `receipt.draw.provider.<PROVIDER>`, `receipt.draw.slot.<SLOT>`), with stable codes as fallback.

## Non-Goals

- Do not change ticket sale, settlement, payout, or print authorization rules.
- Do not introduce a new Flyway version; this updates the existing pre-go-live read view source.
- Do not make `core.sales` depend on catalog display formatters.
