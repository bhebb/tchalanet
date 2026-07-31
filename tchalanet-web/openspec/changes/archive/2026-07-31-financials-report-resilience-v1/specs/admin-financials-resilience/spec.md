# admin-financials-resilience Specification

## ADDED Requirements

### Requirement: Financial report owns normalized page feedback

The financial report SHALL preserve the normalized load error in its page state and SHALL not
emit duplicate generic shell feedback for that page-owned request.

#### Scenario: Financial report load failure

- **GIVEN** the financial report request fails
- **WHEN** the report page renders
- **THEN** the existing error panel displays the normalized title and message
- **AND** the report page remains the owner of the feedback.
