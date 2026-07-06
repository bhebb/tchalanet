## ADDED Requirements

### Requirement: Ticket settlement supports an explicit bet option matrix

Core sales SHALL settle only the bet options that are explicitly supported by the catalog contract.

#### Scenario: Supported v1 options are declared

- **GIVEN** the backend exposes supported bet options
- **WHEN** web, mobile, sales validation, pricing, or settlement needs the supported matrix
- **THEN** the v1 matrix includes:
  - `MARRIAGE_2D2D` option `1`: exact order;
  - `MARRIAGE_2D2D` option `2`: reverse allowed;
  - `LOTTO3_3D` option `1`: exact;
  - `LOTTO3_3D` option `2`: box/permuted;
  - `LOTTO4_PATTERN` option `1`: exact;
  - `LOTTO4_PATTERN` option `2`: box/permuted;
  - `LOTTO4_PATTERN` option `3`: front pair;
  - `LOTTO4_PATTERN` option `4`: back pair;
  - `LOTTO5_PATTERN` option `1`: lot1 + lot2;
  - `LOTTO5_PATTERN` option `2`: lot1 + lot3;
  - `LOTTO5_PATTERN` option `3`: mixed lot1/lot2/lot3.
- **AND** any provider-specific combination not listed is treated as unsupported for selling and
  settlement.

#### Scenario: Settlement variant separates sale option from computed variant

- **GIVEN** a line uses `LOTTO4_PATTERN` option `2`
- **AND** the canonical selection is `1234`
- **WHEN** backend resolves settlement metadata
- **THEN** the sale option remains `Permuté`
- **AND** the computed settlement variant is `LOTTO4_BOX_24_WAY`
- **AND** the seller terminal is not required to display `24-way`.

#### Scenario: Unsupported provider combination is not silently accepted

- **GIVEN** a provider page lists a combination such as `3-way straight/box`, `6-way straight/box`,
  `12-way box`, or `24-way box`
- **WHEN** that combination has no explicit `BetOption` contract
- **THEN** sales validation rejects it before persistence
- **AND** settlement does not infer support from display labels or provider examples.

#### Scenario: Loto 3 box handles duplicate digits according to supported semantics

- **GIVEN** `LOTTO3_3D` option `2`
- **WHEN** the drawn Pick 3 contains duplicate digits
- **THEN** settlement uses the supported box/permutation semantics defined by backend tests
- **AND** the backend may compute `LOTTO3_BOX_3_WAY` or `LOTTO3_BOX_6_WAY`
- **AND** those variants are not exposed as seller choices.

#### Scenario: Loto 4 box handles duplicate digits according to supported semantics

- **GIVEN** `LOTTO4_PATTERN` option `2`
- **WHEN** the drawn Pick 4 contains repeated digits
- **THEN** settlement uses the supported box/permutation semantics defined by backend tests
- **AND** the backend may compute `LOTTO4_BOX_4_WAY`, `LOTTO4_BOX_6_WAY`,
  `LOTTO4_BOX_12_WAY`, or `LOTTO4_BOX_24_WAY`
- **AND** those variants are not exposed as seller choices.

#### Scenario: Loto 4 front and back pair settle against Pick 4

- **GIVEN** `LOTTO4_PATTERN` option `3`
- **WHEN** the selected two front digits match the first two digits of Pick 4
- **THEN** the line wins.

- **GIVEN** `LOTTO4_PATTERN` option `4`
- **WHEN** the selected two back digits match the last two digits of Pick 4
- **THEN** the line wins.

#### Scenario: Bòlèt lot-specific matching does not cross lots

- **GIVEN** a Bòlèt line uses `MATCH_1_2D`
- **WHEN** the selected 2D value appears in lot2 or lot3 but not as the last 2 digits of lot1
- **THEN** the line loses.

- **GIVEN** a Bòlèt line uses `MATCH_2_2D`
- **WHEN** the selected 2D value appears in lot1 or lot3 but not lot2
- **THEN** the line loses.

- **GIVEN** a Bòlèt line uses `MATCH_3_2D`
- **WHEN** the selected 2D value appears in lot1 or lot2 but not lot3
- **THEN** the line loses.

### Requirement: Settlement variants are resolved by a pure domain service

Core sales SHALL resolve settlement variants with deterministic domain logic.

#### Scenario: Resolver computes Loto 4 box variant

- **GIVEN** `LOTTO4_PATTERN` option `2`
- **WHEN** the canonical selection has four different digits
- **THEN** the resolver returns `LOTTO4_BOX_24_WAY`.

#### Scenario: Resolver rejects invalid variant inputs

- **GIVEN** an option-bearing bet type and an invalid selection shape
- **WHEN** the resolver is called
- **THEN** it rejects the variant resolution deterministically
- **AND** it does not call repositories, buses, controllers, or external services.

### Requirement: Sale preview may expose computed variant for admin/debug consumers

Sale preview SHALL keep seller/client labels simple while allowing backend/admin consumers to inspect
the computed variant when needed.

#### Scenario: Preview resolves computed variant

- **GIVEN** a valid sale preview request includes game code, bet type, bet option, selection, and
  stake
- **WHEN** backend evaluates the preview
- **THEN** it validates the sale option
- **AND** it resolves the computed settlement variant
- **AND** it returns the commercial sale label
- **AND** it may include the computed variant for admin/debug consumers.

#### Scenario: Customer receipt hides technical variants

- **GIVEN** a sold ticket line has computed variant `LOTTO4_BOX_24_WAY`
- **WHEN** a customer receipt is formatted
- **THEN** the receipt shows `Permuté`
- **AND** it does not show `LOTTO4_BOX_24_WAY` by default.

#### Scenario: Result explanation uses current result facts

- **GIVEN** an admin/support result page requests combinations and rules for a specific draw result
- **WHEN** backend explains settlement variants or winning combinations
- **THEN** the explanation is derived from that draw result's facts and winning numbers
- **AND** it is not a static provider documentation matrix detached from the selected result.

### Requirement: Bet option labels are not settlement logic

Settlement SHALL use `BetType` and `BetOption` semantics, not translated labels.

#### Scenario: A bet option helper displays a translated label

- **GIVEN** a client displays `Exact`, `Permuté`, `Deux premiers`, or `Deux derniers`
- **WHEN** settlement evaluates the ticket
- **THEN** it uses the numeric option code and backend `BetOption`
- **AND** changing a label does not change the winning calculation.
