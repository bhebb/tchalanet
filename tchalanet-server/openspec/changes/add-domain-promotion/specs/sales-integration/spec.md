# Specification: sales-integration

## ADDED Requirements

### Requirement: Sales preview evaluates promotions

Sales preview SHALL evaluate promotions and return notices/effects to the UI.

#### Scenario: Cart qualifies for free Maryaj

- GIVEN a tenant has a free Maryaj rule for cart totals >= 1000 HTG
- AND the cart paid total is 1000 HTG
- WHEN preview is requested
- THEN preview SHALL return a promotion notice
- AND a `FreeLineGrant` for MARYAJ.

### Requirement: Sales confirmation revalidates promotions

Sales confirmation SHALL re-evaluate promotions server-side before creating the ticket.

#### Scenario: Client sends forced free line without eligibility

- GIVEN the cart paid total is below the threshold
- AND the client sends a free MARYAJ line
- WHEN sale confirmation is requested
- THEN the command SHALL reject the sale with a business error.

### Requirement: Sales stores applied snapshots

Sales SHALL store canonical snapshots for applied promotion effects.

#### Scenario: Christmas multiplier applies

- GIVEN sale time qualifies for first prize 60x
- WHEN the ticket is confirmed
- THEN `ticket_line_applied_rule` SHALL store a `PAYOUT_MULTIPLIER_OVERRIDE` snapshot
- AND the snapshot SHALL include the applied multiplier.

### Requirement: Sales totals distinguish paid and effective stake

Sales SHALL distinguish paid amount from effective stake amount.

#### Scenario: Free Maryaj line is added

- GIVEN a free MARYAJ line value is 50 HTG
- WHEN the ticket is sold
- THEN `paid_amount = 0`
- AND `effective_stake_amount = 50`.
