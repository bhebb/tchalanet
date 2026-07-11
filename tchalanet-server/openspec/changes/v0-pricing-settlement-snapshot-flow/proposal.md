# V0 Pricing/Promotion -> Sale Snapshot -> Settlement/Stats Flow

## Why

Issue #255 asks to simplify the V0 money flow so sale-time code no longer computes or transports
potential winnings. The backend should resolve live configuration at sale time, persist an immutable
minimal snapshot on the ticket line, print/reprint from that snapshot, and calculate only realized
winnings after official results arrive.

Current code verification shows the backend is close in some areas but still mixes concepts:

- `core.pricing` currently owns tenant/seller-terminal odds only (`pricing_odds.odds`,
  `seller_terminal_pricing_odds_override.odds`, `ResolveSellerTerminalOddsQuery`).
- `TicketLinePreparationService` used to resolve odds and calculate `settlementPayoutSnapshot`,
  `minSettlement`, `maxSettlement`, `totalSettlement`, line settlement, and line odds during sale.
- `PromotionTicketLineFactory` treats Maryaj gratis through `HT_MARYAJ_GRATIS` and now snapshots
  payout terms without calculating a pre-result payout.
- `TicketWinningCalculator` calculates realized winnings from persisted settlement terms after draw
  result facts are available.
- Ticket print work already moved toward customer-safe output with no odds or potential winnings.

## What

Introduce a backend refactor where pricing resolves payout rules instead of odds-only values, sales
persists a typed immutable settlement terms snapshot, and settlement is the only place that turns a
winning match into money.

Required behavior:

- Games support payout rules:
  - `STAKE_MULTIPLIER`
  - `FIXED_AMOUNT`
- V0 constrains rule types by game:
  - `HT_BOLET`, `HT_MARYAJ`, `HT_LOTO3`, `HT_LOTO4`, and `HT_LOTO5` use
    `STAKE_MULTIPLIER`.
  - `HT_MARYAJ_GRATIS` uses `FIXED_AMOUNT`.
- Pricing resolution priority:
  1. seller terminal + selection
  2. tenant + selection
  3. seller terminal general
  4. tenant general
- Seller-terminal overrides are evaluated term by term and may override values partially without
  copying the full tenant game configuration. They must not change the rule type in V0.
- `EXPLICIT_ONLY` snapshots only the seller-selected commercial option.
- `IMPLICIT_BEST_MATCH` snapshots every enabled applicable technical rule while keeping `betOption`
  null and without splitting the customer stake across alternatives.
- Maryaj gratis is a distinct game code: the promotion only decides when one or more
  `HT_MARYAJ_GRATIS` lines are added to the ticket. It does not define how that game wins.
- Maryaj gratis lines keep `gameCode=HT_MARYAJ_GRATIS`, `origin=PROMOTION`, `stakeAmount=0`, and the
  promotion decision reference.
- Print/reprint read ticket line snapshots and never recalculate pricing or promotion rules.
- Settlement reads snapshots, compares against official results, persists only realized
  `ticket_line.payout_amount` and `sales_ticket.winning_amount`.
- Reporting/stats must use persisted sales and realized payout amounts, never potential winnings.
- Preview returns a prepared basket and promotion decision, but confirm must re-resolve the
  authoritative snapshot. A `configurationHash` should detect stale previews and return
  `409 sales.preparation_stale` when effective pricing/promotion configuration changed.

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

Responsibility split:

- `core.promotion`: eligibility, tiers, quantity, generation, and regeneration.
- `platform.tenantgame`: active options and selection policy (`IMPLICIT_BEST_MATCH` or
  `EXPLICIT_ONLY`).
- `core.pricing`: payout rules, fixed Exact/Permuted amounts, and SellerTerminal overrides.
- `core.sales`: effective resolution, immutable snapshot, and settlement orchestration.

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

- No historical ticket-line compatibility columns are kept in V0; `settlement_terms_snapshot` is the source of truth.
- Should selection-specific overrides be available immediately in V0 for every game, or only for
  Maryaj/Maryaj gratis first?
- Should confirm always reject stale previews with `409 sales.preparation_stale`, or can selected
  POS flows opt into accepting the new effective configuration returned by confirm?
