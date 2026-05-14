# Specification: platform-audit

## ADDED Requirements

### Requirement: Sensitive POS actions audit validated frame

The system SHALL audit the effective validated POS frame for sensitive actions.

#### Scenario: Raw hint is not audited as truth

- **GIVEN** a request with POS headers
- **WHEN** a sensitive action succeeds or fails after POS resolution
- **THEN** audit records the `ValidatedPosOperationContext` values
- **AND** audit does not describe raw headers as validated truth

### Requirement: Tenant override metadata is audited

The system SHALL audit tenant override metadata when a super-admin override is used.

#### Scenario: Override reason included

- **GIVEN** a request with authorized tenant override
- **WHEN** a sensitive action is performed
- **THEN** audit includes override tenant and override reason
