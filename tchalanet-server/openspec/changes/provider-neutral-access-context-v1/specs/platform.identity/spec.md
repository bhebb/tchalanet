# platform.identity Delta Specification

## ADDED Requirements

### Requirement: External authentication is provider-neutral

`platform.identity` SHALL normalize a validated provider principal into provider, issuer, external
subject, and safe profile facts without treating provider authorization claims as Tchalanet
business authority.

#### Scenario: Provider token contains roles

- **GIVEN** a validated external token contains provider roles, groups, permissions, or tenant claims
- **WHEN** application identity is resolved
- **THEN** those claims SHALL NOT become Tchalanet roles, permissions, or effective tenant
- **AND** business access SHALL be resolved from Tchalanet-owned data

### Requirement: Application identity is resolved before access

Authenticated external identity SHALL resolve through Tchalanet-owned external identity mapping to
an active AppUser before tenant membership or permissions are resolved.

#### Scenario: External identity is linked to an active AppUser

- **GIVEN** a technically authenticated provider principal
- **AND** a matching active external identity mapping and AppUser
- **WHEN** identity bootstrap runs
- **THEN** it SHALL expose the resolved AppUser fact to the access-context pipeline
- **AND** it SHALL NOT resolve tenant, roles, permissions, operational context, or RLS

#### Scenario: AppUser is disabled or locked

- **GIVEN** a technically authenticated provider principal maps to a disabled or locked AppUser
- **WHEN** identity bootstrap runs
- **THEN** the request SHALL be denied before controller execution

### Requirement: Actor type is selected by a client-type hint without fallback

A tenant-scoped path is shared by AppUser and SellerTerminal callers. Identity bootstrap SHALL select
the resolver from the `X-Tch-Client-Type` hint and SHALL NOT fall back to the other resolver. The hint
selects a resolver only; it SHALL NOT grant identity, access, tenant, or status.

#### Scenario: POS hint resolves a terminal only

- **GIVEN** an authenticated request with `X-Tch-Client-Type: POS`
- **WHEN** identity bootstrap runs
- **THEN** only the SellerTerminal resolver SHALL be used
- **AND** if no terminal mapping exists the request SHALL be denied without trying the AppUser resolver

#### Scenario: Absent hint resolves an app user only

- **GIVEN** an authenticated request with no `X-Tch-Client-Type: POS`
- **WHEN** identity bootstrap runs
- **THEN** only the AppUser resolver SHALL be used
- **AND** if no AppUser mapping exists the request SHALL be denied without trying the SellerTerminal resolver

