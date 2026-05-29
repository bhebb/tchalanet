# Specification — Ledger and Stats integration with Promotion V1

## ADDED Requirements

### Requirement: Events expose promotion snapshot fields

Sales events SHOULD expose promotion-related line snapshot fields where useful: line origin, pricing source, promotion decision id, payout base amount, and odds snapshot.

#### Scenario: Stats consume ticket event
- **GIVEN** TicketPlacedEvent contains line snapshot fields
- **WHEN** stats consume the event
- **THEN** stats can distinguish paid customer lines from promotional lines

### Requirement: Stats distinguish promotion effects

Stats SHOULD distinguish paid customer lines, promotional lines, boosted odds lines, and waived charges.

#### Scenario: SMS waived
- **GIVEN** a sale has a waived SMS charge
- **WHEN** stats process the event
- **THEN** waived charge can be counted separately

### Requirement: Ledger V1 does not require full promo cost accounting

Ledger cost allocation for promotion is not required in V1.

#### Scenario: Promotional line event
- **GIVEN** a promotional line is sold
- **WHEN** ledger/stat events are emitted
- **THEN** events preserve enough information for future cost accounting
