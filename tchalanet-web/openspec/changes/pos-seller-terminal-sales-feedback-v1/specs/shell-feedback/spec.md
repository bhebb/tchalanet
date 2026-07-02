# shell-feedback Spec Delta

## ADDED Requirements

### Requirement: Shell success confirmations

Shell feedback SHALL support success confirmations for completed user actions that should remain visible across the current shell.

#### Scenario: Feature emits a successful action confirmation

- **WHEN** a feature completes a user-triggered create/update operation
- **THEN** it MAY add a `success` shell feedback item
- **AND** the shell feedback outlet renders it as a confirmation distinct from info, warning, and error states.
