# tenant-provisioning Delta

## MODIFIED Requirements

### Requirement: Provisioning result includes readiness

Provisioning result SHALL return readiness/next steps.

#### Scenario: Tenant provisioned with default profile

- **WHEN** provisioning completes
- **THEN** the result includes per-domain status
- **AND** readiness may still require outlet, terminal and seller setup.

#### Scenario: Default profile creates sellable tenant games

- **GIVEN** a tenant is provisioned with the default Haiti lottery profile
- **WHEN** default tenant games are created
- **THEN** each default tenant game is enabled
- **AND** each default tenant game is visible on POS
- **AND** each default tenant game has default stake bounds
- **AND** each default tenant game can still be edited or disabled by the tenant admin after provisioning.
