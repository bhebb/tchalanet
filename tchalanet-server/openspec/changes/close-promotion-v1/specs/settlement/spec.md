# Specification — Settlement integration with Promotion V1

## ADDED Requirements

### Requirement: Settlement uses snapshots only

Settlement SHALL NOT call Promotion runtime.

Settlement SHALL use TicketLine snapshots: `payoutBaseAmount`, `oddsSnapshot`, `gameCode`, and line origin/pricing source where needed.

#### Scenario: Settling a promoted line
- **GIVEN** a TicketLine created from a promotion
- **WHEN** settlement calculates payout
- **THEN** settlement uses `payoutBaseAmount`
- **AND** settlement uses `oddsSnapshot`
- **AND** settlement does not re-evaluate campaign rules

#### Scenario: Campaign changed after sale
- **GIVEN** a campaign is modified after ticket sale
- **WHEN** settlement runs
- **THEN** the old ticket settlement is unaffected
