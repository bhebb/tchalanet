# catalog-game Spec Delta

## ADDED Requirements

### Requirement: Bet options are exposed as stable catalog reference data

`catalog.game` SHALL expose `com.tchalanet.server.catalog.game.api.model.BetOption`
as the source of truth for cashier/sales bet options.

Each option SHALL include:

- `BetType betType`
- `short code`
- `String label`
- `String description`

#### Scenario: Option metadata is available for supported bet types

Given the POS requests available game metadata
When backend builds the option list for `LOTTO4_PATTERN`
Then the returned options include exact, box, front pair, and back pair labels
And each option has a stable short code scoped to `LOTTO4_PATTERN`.

### Requirement: Bet option helpers validate option support

`BetOption` SHALL provide:

- `List<BetOption> allowedFor(BetType betType)`
- `boolean requiresOption(BetType betType)`
- `BetOption from(BetType betType, Short code)`

#### Scenario: Bet type does not support options

Given `MATCH_1_2D`
When `BetOption.from(MATCH_1_2D, null)` is called
Then it returns `null`.

#### Scenario: Missing required option is rejected

Given `LOTTO4_PATTERN`
When `BetOption.from(LOTTO4_PATTERN, null)` is called
Then an `IllegalArgumentException` is thrown.

#### Scenario: Unsupported option code is rejected

Given `LOTTO4_PATTERN`
When `BetOption.from(LOTTO4_PATTERN, 5)` is called
Then an `IllegalArgumentException` is thrown.

### Requirement: BetType delegates option knowledge to BetOption

`BetType` SHALL expose option support by delegating to `BetOption`.

#### Scenario: Loto 4 supports option 4

Given `BetType.LOTTO4_PATTERN`
When checking `supportsOption((short) 4)`
Then the result is true
And `betOptionMax()` is `4`.
