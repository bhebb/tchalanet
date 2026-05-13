# platform.identity Specification

## Purpose
TBD - created by archiving change refactor-operational-context-identity. Update Purpose after archive.
## Requirements
### Requirement: Persistent identity ownership

`platform.identity` SHALL own persistent application identity data: app user, profile, preferences, tenant membership, IdP subject mapping, current-user bootstrap, and user admin operations.

#### Scenario: Identity data is stored under platform identity

- **GIVEN** user/profile/membership/bootstrap code exists
- **WHEN** the platform migration is complete
- **THEN** the code SHALL live under `com.tchalanet.server.platform.identity`
- **AND** no user/profile/membership controller SHALL remain under `core.user` or `core.tenantuser`

### Requirement: Identity public API boundary

`platform.identity` SHALL expose only a minimal `IdentityApi` public surface to other modules, with request/view/result models under `platform.identity.api.model`.

#### Scenario: External module needs identity data

- **GIVEN** a module outside `platform.identity` needs user profile or membership facts
- **WHEN** it reads identity data
- **THEN** it SHALL depend on `platform.identity.api`
- **AND** it SHALL NOT import `platform.identity.internal..`

### Requirement: Identity does not own context or permissions

`platform.identity` SHALL NOT own request context, operational context, permission decisions, terminal validation, outlet validation, or session validation.

#### Scenario: Seller operation needs validation

- **GIVEN** a seller operation needs identity, permission, terminal, outlet, and session facts
- **WHEN** the operation frame is resolved
- **THEN** identity facts SHALL come from `platform.identity.api`
- **AND** permission checks SHALL go through `platform.accesscontrol.api`
- **AND** terminal/outlet/session validation SHALL remain outside `platform.identity`

