# Ticket detail pricing display v1

## Why

The admin ticket detail already displays the draw and played selections, but it drops the
settlement terms captured on each ticket line. This makes a seller-specific pricing override
invisible during audit and makes the ticket detail less useful than the persisted ticket data.

## What

- Extend the POS ticket-detail response with the immutable settlement terms used by each line.
- Keep the customer receipt and print contract unchanged: no seller/tenant pricing source is
  printed.
- Render the admin detail with one shared section per game, and show the effective payout rule
  on each line when variants differ.
- Show whether the rule came from the seller-terminal override or the tenant default.

## Impact

- Backend: `features.pos.tickets` mapping and response contract; no persistence or pricing
  recalculation changes.
- Web: POS ticket detail data model and shared ticket-selection presentation.
- Tests: backend mapper/contract coverage and web detail grouping/rendering coverage.

## Non-goals

- Changing pricing resolution or settlement behavior.
- Recalculating old tickets from current configuration.
- Adding technical pricing-source information to the printed receipt.

## References

- `tchalanet-server/openspec/changes/v0-pricing-settlement-snapshot-flow/`
- `tchalanet-web/docs/conventions/feature-playbook.md` — ticket detail and shared ticket cards
- `tchalanet-web/docs/conventions/style.md`
- `tchalanet-web/docs/conventions/theme.md`
