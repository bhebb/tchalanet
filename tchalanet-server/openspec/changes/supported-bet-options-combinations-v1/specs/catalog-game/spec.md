## ADDED Requirements

### Requirement: Catalog exposes supported bet options as product options

Catalog game SHALL expose only supported bet options as sellable product options by default.

#### Scenario: A game has option-bearing bet types

- **GIVEN** a game supports an option-bearing bet type such as `LOTTO4_PATTERN`
- **WHEN** a client requests available game/bet options
- **THEN** the response includes option code, short label, long label or description, selection
  shape, and whether the option is sellable
- **AND** the options match backend settlement support.

#### Scenario: A provider documentation option is not supported

- **GIVEN** a provider documentation page lists additional combinations
- **WHEN** the catalog has no explicit `BetOption` for that combination
- **THEN** the option is not exposed as sellable
- **AND** client helpers may show it only in documentation/debug contexts marked unsupported.

#### Scenario: Box way variants are not catalog bet options

- **GIVEN** Loto 3 and Loto 4 support box/permuted sale options
- **WHEN** catalog exposes sellable options
- **THEN** it exposes `Permuté` as the sale option
- **AND** it does not expose `3-way`, `6-way`, `12-way`, or `24-way` as seller choices.

#### Scenario: Catalog keeps sale labels separate from settlement variants

- **GIVEN** a client requests game option metadata
- **WHEN** the metadata is for seller/client display
- **THEN** labels are sale-friendly labels such as `Exact`, `Permuté`, `Deux premiers`,
  `Deux derniers`, `Ordre exact`, `Revers / double`, `1er + 2e lot`, `1er + 3e lot`, and `Mixte`
- **AND** `SettlementVariant` names are not required for normal seller terminal display.
