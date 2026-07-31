# admin-draw-result-detail-resilience Specification

## Purpose
TBD - created by archiving change draw-result-detail-resilience-v1. Update Purpose after archive.
## Requirements
### Requirement: Result detail load errors use the shared contract

The draw-result detail page SHALL map load failures to the shared error view model while
preserving its existing not-found and navigation behavior.

#### Scenario: Result lookup fails

- **GIVEN** the result detail route is valid
- **WHEN** the result request fails with a backend or transport error
- **THEN** the page renders the normalized error panel with retry
- **AND** it does not replace the error with a hardcoded generic message.

#### Scenario: Result is not found

- **GIVEN** the result request returns not-found
- **WHEN** the page handles the response
- **THEN** the existing not-found state and navigation behavior remain unchanged.

