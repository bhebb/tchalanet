# Specification — Sales integration with Promotion V1

## ADDED Requirements

### Requirement: Sales materializes promotion decisions

Sales SHALL materialize promotion decisions on TicketLine and MoneyBreakdown.

Sales SHALL NOT delegate ticket ownership to Promotion.

#### Scenario: Normal line
- **GIVEN** a customer line without promotion
- **WHEN** Sales prepares the ticket
- **THEN** the line has `origin=CUSTOMER`
- **AND** `pricingSource=STANDARD`
- **AND** `payoutBaseAmount=stakeAmount`
- **AND** `promotionDecisionId=null`

#### Scenario: Free game line
- **GIVEN** Promotion returns `FREE_GAME_LINE`
- **WHEN** Sales applies the effect
- **THEN** Sales adds a promotional TicketLine
- **AND** `origin=PROMOTION`
- **AND** `pricingSource=PROMOTION`
- **AND** `stakeAmount=0`
- **AND** `payoutBaseAmount` comes from the effect
- **AND** odds snapshot comes from tenant pricing odds

#### Scenario: Boost odds
- **GIVEN** Promotion returns `BOOST_ODDS`
- **WHEN** Sales applies the effect
- **THEN** Sales updates the line odds snapshot
- **AND** pricing odds configuration remains unchanged

#### Scenario: Waive charge
- **GIVEN** Promotion returns `WAIVE_CHARGE`
- **WHEN** Sales applies the effect
- **THEN** Sales updates MoneyBreakdown charges
- **AND** pricing odds are not modified

### Requirement: Sale preparation order

Sale preparation SHALL apply promotion before final limits/autonomy.

#### Scenario: Prepare sale with promotion
- **GIVEN** a sale request
- **WHEN** Sales prepares the sale
- **THEN** it builds normal lines
- **AND** computes initial charges and money
- **AND** resolves promotion
- **AND** applies promotion effects
- **AND** computes final money
- **AND** runs limits/autonomy
