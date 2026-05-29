# Specification — Payout integration with Promotion V1

## ADDED Requirements

### Requirement: Payout pays settled result

Payout SHALL pay only the settled payout result.

Payout SHALL NOT call Promotion runtime.

#### Scenario: Payout promoted winning line
- **GIVEN** a winning line produced by promotion
- **WHEN** payout is executed
- **THEN** payout pays the settled amount
- **AND** payout does not re-evaluate promotion rules

### Requirement: Payout display

Payout receipts MAY display promotion-origin lines for transparency.

#### Scenario: Receipt display
- **GIVEN** a payout contains promotional lines
- **WHEN** receipt is generated
- **THEN** promotional origin may be displayed
- **AND** payout amount remains based on settlement snapshot
