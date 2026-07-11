# Tasks

## 0. Decisions

- [x] Conserver `HT_MARYAJ_GRATIS` comme jeu distinct.
- [x] La promotion attribue des lignes de ce jeu sans définir comment le jeu gagne.
- [x] Les montants fixes Exact/Permuté appartiennent à `core.pricing`.
- [x] Les overrides SellerTerminal du pricing sont distincts des overrides d'éligibilité promotionnelle.
- [x] Settlement : snapshot moderne obligatoire, sinon échec fort du settlement.
- [ ] Décider la cohérence preview/confirm et le comportement final en cas de changement de configuration.

## 1. Pricing Rules

- [x] Add typed payout rule model supporting `STAKE_MULTIPLIER` and `FIXED_AMOUNT`.
- [x] Enforce V0 game rule types: paid games use `STAKE_MULTIPLIER`, `HT_MARYAJ_GRATIS` uses `FIXED_AMOUNT`.
- [x] Add persistence for tenant default payout rules.
- [x] Add persistence for seller-terminal payout rule overrides.
- [ ] Add selection-specific override lookup support.
- [x] Reject SellerTerminal overrides that change the tenant/default payout rule type.
- [x] Resolve overrides term by term so partial terminal values fall back to tenant defaults.
- [x] Replace or wrap `ResolveSellerTerminalOddsQuery` with a payout-rule resolution query.
- [x] Rename the admin/API configuration surface from odds to pricing rules.
- [x] Preserve compatibility for existing odds/multiplier configs inside the pricing storage model.
- [x] Update pricing docs and focused pricing tests.

## 2. Sales Snapshot

- [x] Add typed `SettlementTermsSnapshot` records.
- [x] Replace `PricingVariantCode` with `SettlementRuleCode` in the new snapshot.
- [x] Add Flyway migration for `sales_ticket_line.settlement_terms_snapshot jsonb`.
- [x] Add serializer/deserializer adapter for the snapshot.
- [ ] Create/evolve `TicketLineSettlementTermsPlanner`.
- [x] Remove sale-time realized payout calculation from `TicketLinePreparationService`.
- [x] Do not store calculated payout amounts, min/max/total values, `settlementPayoutSnapshot`, or authoritative line odds in the JSON.
- [x] Keep `EXPLICIT_ONLY` snapshot behavior: one selected commercial option.
- [x] In explicit mode, snapshot the selected rule code and commercial label.
- [x] Keep `IMPLICIT_BEST_MATCH` snapshot behavior: all applicable terms, no bet option, no stake splitting.
- [x] In implicit mode, keep `betOption=null`.
- [ ] Do not use the number of terms to decide print rendering.
- [x] Add focused sale preparation tests for explicit/implicit/fixed/multiplier terms.

## 3. Maryaj Gratis

- [x] Conserver `gameCode=HT_MARYAJ_GRATIS`.
- [x] Imposer `PayoutRuleType.FIXED_AMOUNT` pour ce jeu en V0.
- [x] Interdire aux overrides SellerTerminal de changer le type de règle.
- [x] Résoudre Exact et Permuté indépendamment afin de permettre des overrides partiels.
- [x] Garder `stakeAmount=0`.
- [x] Garder `origin=PROMOTION` et `promotionDecisionId`.
- [x] Add `sellerTerminalId` to promotion evaluation context or introduce a terminal-scoped promotion resolution query.
- [x] Add persistence for partial SellerTerminal promotion overrides without copying complete campaigns.
- [x] Define override merge order: seller-terminal override -> tenant campaign defaults.
- [x] Include terminal override identity/effective hash in the promotion decision snapshot.
- [x] Resolve effective tenant + seller-terminal promotion config.
- [x] Generate free Maryaj line only after paid line validation.
- [x] Exclude free lines from their own eligibility calculation.
- [x] Snapshot generated/manual selection and promotion decision metadata.
- [x] Snapshot fixed Exact/Permuted payout terms for Maryaj gratis from `core.pricing`.
- [x] Add tests for auto/manual generation, regeneration limits, and terminal overrides.

## 3.5 Preview / Confirm

- [ ] Preview returns a prepared basket, prepared promotion decision, and configuration hash.
- [ ] Confirm re-resolves effective config before persisting the authoritative snapshot.
- [ ] Confirm rejects stale preview hashes with `409 sales.preparation_stale` unless a product decision allows accepting new config.
- [ ] Confirm idempotency returns exactly the same ticket and snapshot on retry.

## 4. Settlement

- [x] Move `TicketWinningCalculator` to read `SettlementTermsSnapshot`.
- [x] Implement realized amount calculation for `STAKE_MULTIPLIER`.
- [x] Implement realized amount calculation for `FIXED_AMOUNT`.
- [x] Preserve `ALTERNATIVE` best-win behavior.
- [x] Preserve/add `CUMULATIVE` behavior only for explicitly cumulative terms.
- [x] Persist line `payout_amount`, line result status, and ticket `winning_amount`.
- [x] Verify event publication remains idempotent and no double payout occurs.
- [x] Use modern snapshot when present.
- [x] Fail strongly when snapshot data is insufficient; never map data errors to `LOST`.
- [x] Add tests for exact, permuted, implicit best match, fixed Maryaj gratis, config change after sale, and settlement replay.

## 5. Print / Reprint

- [ ] Ensure receipt/reprint reads only ticket snapshots.
- [x] Ensure no potential payout, odds, multiplier, or technical variant is printed.
- [x] Preserve explicit option label display from snapshot.
- [x] Preserve implicit option hiding.
- [x] Preserve Maryaj and Maryaj gratis formatting.
- [x] Print Maryaj gratis as `* 12 × 34   GRATIS` or equivalent receipt-safe layout.
- [x] Do not print fixed Exact/Permuted amounts before result.
- [x] After settlement, result screens use `payout_amount`, not settlement terms.
- [x] Add reprint test showing current pricing/promotion changes do not affect old ticket rendering.

## 6. Stats / Reporting

- [x] Remove any potential payout KPI from tenant/admin reports.
- [x] Ensure pre-result stats include sales/stakes/counts only.
- [x] Ensure post-result stats use persisted realized payout amounts.
- [x] Ensure free promotion lines do not increase paid stake.
- [x] Keep `HT_MARYAJ_GRATIS` identifiable as a game in aggregations.
- [x] Use `origin=PROMOTION` to distinguish promotional lines.
- [x] Ensure `stake_amount=0` never increases paid sales.
- [x] Ensure realized settlement amount increases realized winnings after settlement.
- [ ] Add tests for realized payouts by draw, game, and seller terminal. Draw and seller-terminal coverage exists; game-level realized payouts require a dedicated game-result aggregate.

## 7. Legacy Cleanup

- [x] Identify readers of `odds_snapshot`, `payout_base_amount`, and legacy coverage summary fields.
- [x] Remove potential payout summary fields from `TicketLine`, line coverage domain objects, mappings, and Flyway schema.
- [x] Add compatibility/backfill strategy for old tickets. Not required for V0 pre-production; old potential-payout columns are dropped directly.
- [x] Rename `potential_gain_snapshot`/coverage naming if the table remains temporarily. Not applicable; `potential_gain_snapshot` is removed.
- [x] Drop remaining obsolete odds/coverage compatibility columns/classes after readers are migrated. Coverage table/JPA/archive/domain objects removed; event payload, promotion snapshot, preview, ticket-line persistence/archive, and sale-preparation promotion views no longer expose odds/payout base.

## 8. Validation

- [ ] Run focused module tests for pricing, promotion, sales, settlement, print, and reporting.
- [x] Run `./mvnw -pl tchalanet-app -am test -DskipTests` compile gate.
- [ ] Run targeted integration/smoke tests for sale -> print -> result -> settlement -> report.
