# admin-draw-detail-resilience Specification

## ADDED Requirements

### Requirement: Draw detail optional requests degrade locally

The generated-draw detail page SHALL keep the required draw content usable when activity or
top-selection requests fail, while normalizing each optional error at its owning section.

#### Scenario: Optional activity failure

- **GIVEN** the draw detail request succeeds
- **WHEN** the activity request fails
- **THEN** the draw detail remains visible
- **AND** the activity section shows a localized retryable error.

#### Scenario: Manual result save failure

- **GIVEN** the result form is open
- **WHEN** saving the result fails
- **THEN** the form remains actionable
- **AND** the drawer shows normalized actionable feedback without raw server prose.
