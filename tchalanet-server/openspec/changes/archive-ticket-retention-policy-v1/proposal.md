# Proposal - archive ticket retention policy v1

## Why

The current archive and purge flow is guarded but period-based. It can archive and purge complete
ticket, draw and draw-result periods after verified archive objects exist, but it does not express
the business distinction between losing tickets, winning tickets and unresolved tickets.

For POS usage, hot ticket growth will be driven mostly by losing tickets. Operators need a safe way
to remove losing tickets from hot tables shortly after results are final, while keeping winning,
pending and disputed tickets online for longer. Draws and results also need explicit retention rules
so cleanup does not break ticket verification, public result history, payout workflows or reports.

## What

- Define business retention classes for tickets:
  - losing/no-payout tickets can become purge candidates after a short retention window;
  - winning, payout-pending, paid, reversed and disputed tickets stay online longer;
  - unresolved tickets are never purge candidates.
- Require ticket purge eligibility to be based on ticket IDs and business status, not only on a
  broad period.
- Keep verified archive, lookup index, legal-hold checks, dry-run-first behavior and bounded child
  deletion as mandatory guardrails.
- Define draw and draw-result cleanup dependencies:
  - draws cannot be purged while hot tickets still reference them;
  - draw results cannot be purged while draws still reference them;
  - draw channels are reference/configuration data and are not normal purge targets.
- Document verification behavior after hot purge so public code and QR lookup do not silently fail.

## Impact

- Backend only: `platform.archive`, `core.sales`, `core.draw`, `core.drawresult`.
- No immediate schema migration is required by this spec.
- Future implementation should add focused integration tests around eligibility, legal holds,
  archive verification, and verification lookup after purge.

## Non-goals

- No automatic deletion is introduced by this proposal.
- No change to payout expiry or payment rules.
- No deletion of `draw_channel` rows.
- No frontend/admin UI changes beyond exposing dry-run plans in a later implementation.
