# bet-coverage Specification

## Purpose
Define multi-coverage ticket lines, per-coverage snapshots, settlement modes, and customer-facing gain summaries.
## Requirements
### Requirement: TicketLineCoverage models multi-variant coverage

A ticket line SHALL represent the commercial bet bought by the customer, and each way it can be paid
SHALL be modelled as a `TicketLineCoverage` row carrying a `PricingVariantCode`, a stake amount, an
`odds_snapshot`, a `potential_gain_snapshot`, and a `win_mode` (`ALTERNATIVE` or `CUMULATIVE`).
Settlement SHALL compute per winning coverage as `stake_amount * odds_snapshot` and apply the
commercial payout mode. Maryaj and Loto combined coverages SHALL pay `BEST_OF`; Bòlèt multi-lot
coverages SHALL pay `CUMULATIVE`. Coverage resolution at sale time SHALL produce a
`CoverageResolution` (a list of coverages), replacing single-variant resolution. Combined options MAY
be exposed only after coverage snapshots, settlement, receipt/reprint, verification, refund/cancel and
minimal reporting understand coverage. `EXACT_PLUS_BOX` satisfies that gate in this change.
`IMPLICIT_BEST_MATCH` SHALL remain disabled until its product behavior is explicitly decided.

#### Scenario: Exact plus Permuté produces two coverages

- **WHEN** a Loto 3 line plays `123` as "Exact + Permuté"
- **THEN** two coverages are created: `LOTTO3_STRAIGHT` and `LOTTO3_BOX_6_WAY`
- **AND** each coverage carries its own stake, odds snapshot and potential gain
- **AND** if multiple coverages match, settlement pays the best winning coverage only

#### Scenario: Bòlèt all lots produces three coverages

- **WHEN** a Bòlèt line plays `12` as "tous les lots"
- **THEN** coverages `MATCH_1_2D`, `MATCH_2_2D`, `MATCH_3_2D` are created with their own odds snapshots
- **AND** if `12` appears in multiple lots, settlement pays the sum of every matching coverage

#### Scenario: Dekabès is a derived Bòlèt settlement label

- **WHEN** a Bòlèt player wins twice, possibly on different numbers, lines, or lots
- **THEN** the system MAY label the outcome as Dekabès in post-settlement/reporting surfaces
- **AND** Dekabès SHALL NOT be modelled as a POS `BetOption` or as a `CoverageResolution` branch in V0

#### Scenario: Implicit disabled until validated

- **WHEN** `IMPLICIT_BEST_MATCH` product behavior is not explicitly decided and tested
- **THEN** `IMPLICIT_BEST_MATCH` remains disabled

#### Scenario: Multi-coverage sale snapshots every coverage

- **WHEN** `LOTTO3_EXACT_PLUS_BOX` or `LOTTO4_EXACT_PLUS_BOX` is resolved
- **THEN** the resolver returns multiple `CoverageVariant` entries
- **AND** sale preparation resolves odds for every coverage
- **AND** sale preparation stores per-coverage stake, odds and potential gain snapshots

### Requirement: Potential gain is stored per coverage and summarized on the line

Each `ticket_line_coverage` SHALL store `stakeSnapshot`, `oddsSnapshot`, `potentialGainSnapshot`, and
`winMode`. `TicketLine` SHALL store a summary: `potentialGainMode`, `minPotentialGain`,
`maxPotentialGain`, and `totalPotentialGain` only for cumulative mode. A single coverage SHALL display
a single potential gain; multiple best-of coverages SHALL display a min–max range; multiple
cumulative coverages SHALL display a total. Maryaj/Loto composed options SHALL default to `BEST_OF`;
Bòlèt composed options SHALL default to `CUMULATIVE`. Preview, receipt, and settlement SHALL read
the same snapshots.

#### Scenario: Alternative coverages show a range

- **WHEN** a line has multiple alternative coverages
- **THEN** the potential gain is displayed as a min–max range from the coverage snapshots
- **AND** the receipt shows the same commercial coverage, never technical variant codes

#### Scenario: Potential gain mode is typed

- **WHEN** preview, print, or receipt exposes the gain summary
- **THEN** `PotentialGainMode` is exposed as a typed enum value, not a free-form string

### Requirement: All line-consuming surfaces understand coverage

Once `TicketLineCoverage` exists, every surface that displays or sums ticket lines SHALL understand
the commercial line plus its technical coverages, total stake, per-coverage stake, and min/max gain.
This SHALL cover sale, preview, confirmation, ticket detail, receipt/reprint, cancellation/refund
where applicable, settlement, payout, and minimal reporting. Modifying settlement alone SHALL NOT be
considered sufficient.

#### Scenario: Ticket detail, verification and receipt reflect coverage

- **WHEN** a combined or multi-lot line is shown in ticket detail, public verification or on a receipt
- **THEN** the commercial line, its total stake, and its coverage are presented consistently
- **AND** totals are computed from per-coverage snapshots, not from a single line odds
- **AND** technical `PricingVariantCode` values are not exposed to the customer

#### Scenario: Print header view stays header-only

- **WHEN** a ticket is printed or reprinted
- **THEN** `sales_ticket_print_header_v` supplies only header-level ticket context
- **AND** ticket lines and coverages are loaded from the ticket aggregate
- **AND** the view does not need a join to `sales_ticket_line_coverage`
