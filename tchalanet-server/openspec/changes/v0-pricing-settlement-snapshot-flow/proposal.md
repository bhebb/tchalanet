# V0 Pricing/Promotion -> Sale Snapshot -> Settlement/Stats Flow

## Why

Issue #255 asks to simplify the V0 money flow so sale-time code no longer computes or transports
potential winnings. The backend should resolve live configuration at sale time, persist an immutable
minimal snapshot on the ticket line, print/reprint from that snapshot, and calculate only realized
winnings after official results arrive.

Current code verification shows the backend is close in some areas but still mixes concepts:

- `core.pricing` currently owns tenant/seller-terminal odds only (`pricing_odds.odds`,
  `seller_terminal_pricing_odds_override.odds`, `ResolveSellerTerminalOddsQuery`).
- `TicketLinePreparationService` still resolves odds and calculates `settlementPayoutSnapshot`,
  `minSettlement`, `maxSettlement`, `totalSettlement`, line settlement, and line odds during sale.
- `PromotionTicketLineFactory` treats Maryaj gratis through `HT_MARYAJ_GRATIS` and multiplies
  configured promotion amount by resolved odds.
- `TicketWinningCalculator` already calculates realized winnings from persisted line coverages after
  draw result facts are available.
- Ticket print work already moved toward customer-safe output with no odds or potential winnings.

## What

Introduce a backend refactor where pricing resolves payout rules instead of odds-only values, sales
persists a typed immutable settlement terms snapshot, and settlement is the only place that turns a
winning match into money.

Required behavior:

- Paid games support payout rules:
  - `STAKE_MULTIPLIER`
  - `FIXED_AMOUNT`
- Pricing resolution priority:
  1. seller terminal + selection
  2. tenant + selection
  3. seller terminal general
  4. tenant general
- `EXPLICIT_ONLY` snapshots only the seller-selected commercial option.
- `IMPLICIT_BEST_MATCH` snapshots every enabled applicable technical rule while keeping `betOption`
  null and without splitting the customer stake across alternatives.
- Maryaj gratis remains a promotion that creates free `HT_MARYAJ`/Maryaj lines with `origin=PROMOTION`;
  its effective terms come from promotion/game configuration and are snapshotted on the line.
- Print/reprint read ticket line snapshots and never recalculate pricing or promotion rules.
- Settlement reads snapshots, compares against official results, persists only realized
  `ticket_line.payout_amount` and `sales_ticket.winning_amount`.
- Reporting/stats must use persisted sales and realized payout amounts, never potential winnings.

## Impact

Backend modules:

- `tchalanet-core/core.pricing`
- `tchalanet-core/core.sales`
- `tchalanet-core/core.promotion`
- `tchalanet-core/core.analytics`
- `tchalanet-features/reporting`
- `tchalanet-features/pos/tickets`
- `tchalanet-platform/document` only if print document contracts need adjustment
- `tchalanet-app` Flyway migrations

Docs to update during implementation:

- `core/pricing/DOMAIN_PRICING.md`
- `core/sales/DOMAIN_SALES.md`
- `core/promotion/promotion_design.md`
- reporting feature docs if stats contracts change

## Non-goals

- Risk or actuarial dashboards before result.
- Recalculating historical tickets from current configuration.
- Free-form rule authoring per ticket.
- Web/mobile UI implementation in this backend change.
- Manual payout claim UX changes beyond preserving realized settlement data.

## Open Questions

- Should Maryaj gratis persist as `gameCode=HT_MARYAJ` with `promotionCode=MARYAJ_GRATIS`, as issue
  #255 states, or keep the existing `HT_MARYAJ_GRATIS` game code and treat it as a configured
  promotional game variant?
- Do fixed promotion payouts belong in `core.pricing`, `core.promotion`, or a shared
  sale-time `SettlementTermsSnapshot` resolved by sales from both domains?
- Which historical columns are kept for backward compatibility after the JSONB snapshot is added:
  `odds_snapshot`, `payout_base_amount`, `sales_ticket_line_coverage`, and
  `potential_gain_snapshot`?
- Should selection-specific overrides be available immediately in V0 for every game, or only for
  Maryaj/Maryaj gratis first?

