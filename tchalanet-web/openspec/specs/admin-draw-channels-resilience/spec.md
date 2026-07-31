# admin-draw-channels-resilience Specification

## Purpose
TBD - created by archiving change draw-channels-admin-resilience-v1. Update Purpose after archive.
## Requirements
### Requirement: Draw channel errors have local ownership

The draw-channel admin page SHALL render provider-load failures at page level and provider-save
failures inside the configuration dialog, using the shared web error contract.

#### Scenario: Provider load failure

- **GIVEN** the provider request fails
- **WHEN** the draw-channel page is opened
- **THEN** the page renders a normalized error state with retry
- **AND** it does not render an empty provider grid as if no providers existed.

#### Scenario: Provider save failure

- **GIVEN** the configuration dialog is open
- **WHEN** the save request fails
- **THEN** the dialog remains open
- **AND** the normalized save error is rendered in the dialog without duplicate shell feedback.

