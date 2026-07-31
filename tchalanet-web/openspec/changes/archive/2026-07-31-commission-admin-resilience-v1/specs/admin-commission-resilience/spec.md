# admin-commission-resilience Specification

## ADDED Requirements

### Requirement: Commission sections fail independently

The commission admin page SHALL keep the overview and seller-list sections independent when one
of their requests fails.

#### Scenario: Seller list failure preserves overview

- **GIVEN** the commission overview request succeeds
- **WHEN** the seller-list request fails
- **THEN** the overview remains visible
- **AND** the seller section renders its normalized local error with retry

#### Scenario: Overview failure preserves seller list

- **GIVEN** the seller-list request succeeds
- **WHEN** the overview request fails
- **THEN** the seller list remains visible
- **AND** the overview section renders its normalized local error with retry.
