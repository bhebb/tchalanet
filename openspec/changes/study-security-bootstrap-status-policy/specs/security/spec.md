# Security Bootstrap Status Policy Delta

## ADDED Requirements

### Requirement: User bootstrap status policy SHALL be explicit

The system SHALL define the status assigned to an `app_user` created by runtime authentication
bootstrap and SHALL document whether that user can access protected APIs immediately.

#### Scenario: First authenticated login

- **WHEN** a valid JWT subject has no `app_user`
- **THEN** the selected bootstrap status is applied consistently in code, database defaults, and docs

#### Scenario: Pending or suspended user

- **WHEN** an authenticated subject maps to a non-active `app_user`
- **THEN** the security pipeline behavior is documented and tested

### Requirement: Permission enforcement boundary SHALL be explicit

The system SHALL define whether business permissions are enforced at controllers only, at unit
action command handlers, or both.

#### Scenario: Privileged unit action

- **WHEN** a command handler performs a privileged unit action
- **THEN** its permission enforcement location follows the documented boundary
