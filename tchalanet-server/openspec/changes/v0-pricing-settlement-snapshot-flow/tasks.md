# Tasks

## 0. Decisions

- [ ] Decide Maryaj gratis representation: `HT_MARYAJ` + `promotionCode=MARYAJ_GRATIS` vs keeping `HT_MARYAJ_GRATIS` compatibility.
- [ ] Decide where fixed Maryaj gratis payout terms are authored: pricing, promotion, or a sale-time resolver combining both.
- [ ] Decide legacy-ticket settlement behavior when `settlementTermsSnapshot` is missing.

## 1. Pricing Rules

- [ ] Add typed payout rule model supporting `STAKE_MULTIPLIER` and `FIXED_AMOUNT`.
- [ ] Add persistence for tenant default payout rules.
- [ ] Add persistence for seller-terminal payout rule overrides.
- [ ] Add selection-specific override lookup support.
- [ ] Replace or wrap `ResolveSellerTerminalOddsQuery` with a payout-rule resolution query.
- [ ] Preserve compatibility for existing odds/multiplier configs during migration.
- [ ] Update pricing docs and focused pricing tests.

## 2. Sales Snapshot

- [ ] Add typed `SettlementTermsSnapshot` records.
- [ ] Add Flyway migration for `sales_ticket_line.settlement_terms_snapshot jsonb`.
- [ ] Add serializer/deserializer adapter for the snapshot.
- [ ] Create/evolve `TicketLineSettlementTermsPlanner`.
- [ ] Remove sale-time realized payout calculation from `TicketLinePreparationService`.
- [ ] Keep `EXPLICIT_ONLY` snapshot behavior: one selected commercial option.
- [ ] Keep `IMPLICIT_BEST_MATCH` snapshot behavior: all applicable terms, no bet option, no stake splitting.
- [ ] Add focused sale preparation tests for explicit/implicit/fixed/multiplier terms.

## 3. Maryaj Gratis

- [ ] Resolve effective tenant + seller-terminal promotion config.
- [ ] Generate free Maryaj line only after paid line validation.
- [ ] Exclude free lines from their own eligibility calculation.
- [ ] Snapshot generated/manual selection and promotion decision metadata.
- [ ] Snapshot fixed exact/permuted payout terms for Maryaj gratis where configured.
- [ ] Add tests for auto/manual generation, regeneration limits, and terminal overrides.

## 4. Settlement

- [ ] Move `TicketWinningCalculator` to read `SettlementTermsSnapshot`.
- [ ] Implement realized amount calculation for `STAKE_MULTIPLIER`.
- [ ] Implement realized amount calculation for `FIXED_AMOUNT`.
- [ ] Preserve `ALTERNATIVE` best-win behavior.
- [ ] Preserve/add `CUMULATIVE` behavior only for explicitly cumulative terms.
- [ ] Persist line `payout_amount`, line result status, and ticket `winning_amount`.
- [ ] Verify event publication remains idempotent and no double payout occurs.
- [ ] Add tests for exact, permuted, implicit best match, fixed Maryaj gratis, config change after sale, and settlement replay.

## 5. Print / Reprint

- [ ] Ensure receipt/reprint reads only ticket snapshots.
- [ ] Ensure no potential payout, odds, multiplier, or technical variant is printed.
- [ ] Preserve explicit option label display from snapshot.
- [ ] Preserve implicit option hiding.
- [ ] Preserve Maryaj and Maryaj gratis formatting.
- [ ] Add reprint test showing current pricing/promotion changes do not affect old ticket rendering.

## 6. Stats / Reporting

- [ ] Remove any potential payout KPI from tenant/admin reports.
- [ ] Ensure pre-result stats include sales/stakes/counts only.
- [ ] Ensure post-result stats use persisted realized payout amounts.
- [ ] Ensure free promotion lines do not increase paid stake.
- [ ] Add tests for realized payouts by draw, game, and seller terminal.

## 7. Legacy Cleanup

- [ ] Identify readers of `odds_snapshot`, `payout_base_amount`, coverage summary fields, and `TicketLineCoverageJpaEntity`.
- [ ] Add compatibility/backfill strategy for old tickets.
- [ ] Rename `potential_gain_snapshot`/coverage naming if the table remains temporarily.
- [ ] Drop obsolete columns/classes only after all readers are migrated.

## 8. Validation

- [ ] Run focused module tests for pricing, promotion, sales, settlement, print, and reporting.
- [ ] Run `./mvnw -pl tchalanet-app -am test -DskipTests` compile gate.
- [ ] Run targeted integration/smoke tests for sale -> print -> result -> settlement -> report.

