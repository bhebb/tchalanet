# Tasks: supported-bet-options-combinations-v1

## 0. Spec Alignment

- [x] 0.1 Add OpenSpec proposal `supported-bet-options-combinations-v1`.
- [x] 0.2 Document supported sale options for Bòlèt, Maryaj, Loto 3, Loto 4, and Loto 5.
- [x] 0.3 Document unsupported provider options: straight/box combined, wheel, fireball/bonus,
      provider-specific boosters, and explicit 3-way/6-way/12-way/24-way as seller choices.
- [x] 0.4 Document the separation between `BetType`, `BetOption`, and `SettlementVariant`.

## 1. Backend Domain Model

- [x] 1.1 Add `SettlementVariant`.
- [x] 1.2 Add pure `SettlementVariantResolver`.
- [x] 1.3 Add unit tests for `SettlementVariantResolver`.
- [x] 1.4 Keep `BetOption` as sale option contract.
- [x] 1.5 Do not add 3-way/6-way/12-way/24-way to `BetOption`.
- [x] 1.6 Keep the resolver deterministic and side-effect free: no Spring injection, repositories,
      or bus usage.

## 2. Winning Calculation

- [x] 2.1 Refactor `TicketWinningCalculator` to use `SettlementVariant`.
- [x] 2.2 Correct Bòlèt matching: `MATCH_1_2D` -> lot1 only, `MATCH_2_2D` -> lot2 only,
      `MATCH_3_2D` -> lot3 only.
- [x] 2.3 Derive `lot1_2d` from the last 2 digits of lot1.
- [x] 2.4 Support Loto 3 exact, box 3-way, and box 6-way.
- [x] 2.5 Support Loto 4 exact, box 4-way, box 6-way, box 12-way, box 24-way, front pair, and back
      pair.
- [x] 2.6 Support Maryaj exact order and reverse allowed.
- [x] 2.7 Support Loto 5 lot1+lot2, lot1+lot3, and mixed 1/2/3.
- [x] 2.8 Preserve current public/application APIs unless the persisted variant decision requires
      an explicit contract change.

## 3. Sale Preview / Confirmation

- [x] 3.1 Resolve `SettlementVariant` on demand via `SettlementExplanationApi.explain(...)`.
- [x] 3.2 Expose computed variant + commercial/admin labels through `ComputedSettlementVariantView`
      for admin/debug consumers (NOT injected into the seller preview response contract).
- [x] 3.3 **Decision: do NOT persist** `computed_settlement_variant`. Payout is already snapshotted
      via odds/potential payout at sale time; the variant is informational only → recompute.
- [x] 3.4 No Flyway migration (persistence not adopted).
- [x] 3.5 Settlement resolves the variant from `betType + betOption + selection` (no persisted read).
- [x] 3.6 Seller terminal preview contract unchanged; technical variants not exposed by default.

## 4. API / Admin Visibility

- [x] 4.1 Admin ticket detail: `GET /admin/tickets/{id}/settlement-variants`
      (`AdminTicketSettlementController` → `AdminTicketSettlementService`) consumes
      `SettlementExplanationApi` and returns per-line computed variant + admin label.
- [x] 4.2 Support/debug: same admin endpoint reports unsupported/legacy lines as
      `resolved=false` + error instead of failing the response.
- [x] 4.3 Customer receipts unchanged — no technical variant added to seller/receipt surfaces; only the dedicated admin endpoint exposes them.
- [x] 4.4 Admin labels (`Permuté · 3/6/12/24-way`, `Maryaj · …`, lot labels) defined on
      `SettlementVariant.adminLabel()` and surfaced via `ComputedSettlementVariantView.adminLabel()`.
- [x] 4.5 Client/customer labels stay commercial (`consoleBetLabel` / `BetOption.label()`); technical variant is admin-only.

## 5. Web / Mobile Alignment

- [x] 5.1 Seller flows keep sale labels: `consoleBetLabel`/`consoleBetOptionLabel` unchanged; no
      technical variant in seller paths.
- [x] 5.2 Ticket/receipt views stay customer-safe: `consoleBetVariationRows` exposes the commercial
      label by default; the technical `variantLabel` is a separate, admin-only field.
- [x] 5.3 Admin/support can show technical variants: `consoleSettlementVariantLabel` renders the
      backend `adminLabel` (e.g. `Permuté · 24-way`).
- [x] 5.4 Web helpers `consoleSettlementVariantLabel` + `consoleBetVariationRows` added in
      `@tch/web/console` (`console-game-display.ts`); they format backend `SettlementExplanationApi`
      rows, not a static provider-doc matrix. Unit-tested (7 tests).
- [x] 5.5 `Combinaisons & règles` tab added to the admin draw-result detail (`mat-tab-group`, 3 tabs:
      Résultats default / Combinaisons / Résultat brut). Page is a thin orchestrator; each tab is a
      stateless component (`draw-result-summary`, `draw-result-combinations`, `draw-result-raw`).
      Combinations computed via `drawCombinationRows` (Tchalanet-supported options only). i18n keys in
      `feature-admin.json` (fr/en/ht). Build + lint clean.

## 6. Tests

- [x] 6.1 Test Bòlèt lot-specific matching.
- [x] 6.2 Test Maryaj exact order.
- [x] 6.3 Test Maryaj reverse allowed.
- [x] 6.4 Test Loto 3 exact.
- [x] 6.5 Test Loto 3 box 3-way.
- [x] 6.6 Test Loto 3 box 6-way.
- [x] 6.7 Test Loto 4 exact.
- [x] 6.8 Test Loto 4 box 4-way.
- [x] 6.9 Test Loto 4 box 6-way.
- [x] 6.10 Test Loto 4 box 12-way.
- [x] 6.11 Test Loto 4 box 24-way.
- [x] 6.12 Test Loto 4 front pair.
- [x] 6.13 Test Loto 4 back pair.
- [x] 6.14 Test Loto 5 option 1.
- [x] 6.15 Test Loto 5 option 2.
- [x] 6.16 Test Loto 5 option 3.
- [x] 6.17 Test incomplete result behavior, especially missing lot2/lot3.
- [x] 6.18 Test unsupported option codes are **rejected at sale** (preview/confirm), not settled:
      `LOTTO4_PATTERN` + betOption `99` → `sales.bet_option_out_of_range`, no ticket line created.
      Settlement does NOT catch → LOST; the resolver exception propagates (fail hard).

## 7. Documentation

- [x] 7.1 Supported options documented in `docs/02-functional/domains/sales/rules.md` §10 (portal +
      near-code link to `SETTLEMENT_VARIANTS.md`).
- [x] 7.2 Display rules (seller/client vs admin/support) documented in rules.md §10.
- [x] 7.3 Unsupported provider options documented in rules.md §10.
- [x] 7.4 rules.md §10 states provider docs are inputs, not the Tchalanet contract.

## 9. Future (out of v1 scope)

- [ ] 9.1 Add `TicketLineResultStatus.ERROR` / `SETTLEMENT_FAILED` for legacy/corrupted lines whose
      option is no longer supported at settlement time (today: fail hard, no LOST conversion).
- [ ] 9.2 Add settlement reason code `UNSUPPORTED_BET_OPTION`.

## 8. Validation

- [x] 8.1 Catalog game tests green (54 tests incl. `BetOptionTest`).
- [x] 8.2 Core sales settlement tests green (64 tests).
- [x] 8.3 `git diff --check` clean.
