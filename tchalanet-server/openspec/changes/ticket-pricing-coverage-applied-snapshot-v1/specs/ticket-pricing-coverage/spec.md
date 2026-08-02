# Ticket pricing coverage and applied settlement

## ADDED Requirements

### Requirement: Preserve complete pricing coverage at sale time
The sales flow MUST populate the existing `SettlementTermsSnapshot` with every effective payout
rule that can apply to the commercial selection, including the resolved value and whether it came
from a seller-terminal override or tenant default. The persisted terms MUST remain unchanged when
pricing configuration changes later.

#### Scenario: Bòlèt selection is pending
- **GIVEN** a seller sells selection `12` for `10 HTG`
- **AND** the effective seller coverage is lot 1 `65`, lot 2 `35`, lot 3 `15`
- **WHEN** the ticket is created
- **THEN** the ticket's settlement terms snapshot stores all three coverage terms
- **AND** the ticket detail can display `Barèm Vandè: 65-35-15`

#### Scenario: Pricing changes after sale
- **GIVEN** a ticket stores seller coverage `65-35-15`
- **WHEN** the seller configuration changes to `60-30-10`
- **THEN** the existing ticket still exposes `65-35-15`

### Requirement: Persist the applied settlement rule after result
The result application flow MUST persist the rule or rules that actually won, together with the
realized amount for each rule. A pending or losing line MUST NOT have an applied winning rule. The
applied snapshot MUST reference the captured settlement terms and MUST NOT duplicate their payout
configuration.

#### Scenario: Bòlèt lot 1 wins
- **GIVEN** a pending Bòlèt line has captured coverage and settlement terms
- **AND** the official result matches the line's lot 1 rule
- **WHEN** the result is applied
- **THEN** the line stores the applied rule `MATCH_1_2D`
- **AND** the line stores the realized payout amount

#### Scenario: No rule wins
- **GIVEN** no captured settlement term matches the official result
- **WHEN** the result is applied
- **THEN** the line status is `LOST`
- **AND** its realized payout is zero
- **AND** no applied settlement rule is stored

### Requirement: Calculate realized payout from captured settlement terms
Settlement MUST use captured terms and MUST NOT resolve current pricing configuration. A
stake-multiplier payout MUST equal `payoutBaseAmount * multiplier`, rounded to two decimals using
`HALF_UP`. A fixed payout MUST use the captured fixed amount.

#### Scenario: Multiplier payout
- **GIVEN** a captured payout base of `10 HTG`
- **AND** a captured multiplier of `65`
- **WHEN** the term wins
- **THEN** the realized payout is `650 HTG`

#### Scenario: Alternative terms
- **GIVEN** multiple captured terms match
- **AND** their win mode is `ALTERNATIVE`
- **WHEN** settlement is calculated
- **THEN** the highest realized payout is selected
- **AND** only the selected applied term is recorded

#### Scenario: Cumulative terms
- **GIVEN** multiple captured terms match
- **AND** their win mode is `CUMULATIVE`
- **WHEN** settlement is calculated
- **THEN** all realized payouts are added
- **AND** all applied terms are recorded

### Requirement: Preserve commission snapshot independence
Commission MUST continue to be calculated from the ticket stake and seller commission rate
captured at sale time. Pricing coverage and result settlement MUST NOT recalculate or overwrite that
commission snapshot.

#### Scenario: Commission changes after sale
- **GIVEN** a ticket stores its seller commission rate and amount
- **WHEN** the seller commission configuration changes
- **THEN** the ticket's stored commission rate and amount remain unchanged
- **AND** result settlement uses the ticket's captured pricing terms
