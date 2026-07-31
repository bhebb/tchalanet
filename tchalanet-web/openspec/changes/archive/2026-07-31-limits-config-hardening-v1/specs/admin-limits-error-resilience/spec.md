# admin-limits-error-resilience Specification

## ADDED Requirements

### Requirement: Limits console normalizes every HTTP failure

The limits overview, child pages, system catalog, tables, and upsert dialog SHALL map HTTP
failures through the shared ProblemDetail mapper before rendering them.

#### Scenario: Child-page load failure

- **GIVEN** a limits child page is opened
- **WHEN** its assignments request fails
- **THEN** the page renders a localized normalized error with retry
- **AND** it does not display the failure as an empty successful list.

#### Scenario: Upsert validation failure

- **GIVEN** the upsert dialog is open
- **WHEN** the save request returns a validation ProblemDetail
- **THEN** field or dialog feedback identifies the invalid input
- **AND** the dialog remains open for correction.
