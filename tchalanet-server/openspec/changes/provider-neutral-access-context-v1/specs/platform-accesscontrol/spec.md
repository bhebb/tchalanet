# platform-accesscontrol Delta Specification

## ADDED Requirements

### Requirement: Effective access snapshot is Tchalanet-owned

`platform.accesscontrol` SHALL resolve effective tenant, roles, and permissions exclusively from
Tchalanet-owned access data for the resolved AppUser.

#### Scenario: Normal tenant user access is resolved

- **GIVEN** an active normal AppUser with one active tenant membership
- **WHEN** effective access is resolved
- **THEN** the snapshot SHALL contain that effective tenant
- **AND** active Tchalanet role and permission facts
- **AND** provider authorization claims SHALL NOT affect the result

#### Scenario: Membership is missing or ambiguous

- **GIVEN** a tenant-scoped request for a normal AppUser
- **WHEN** no active membership or more than one active membership can be resolved
- **THEN** access SHALL be denied before controller execution

### Requirement: Super-admin tenant override is explicit and auditable

A platform `SUPER_ADMIN` SHALL have no effective tenant by default and SHALL enter tenant scope only
through an explicit, validated, audited override.

#### Scenario: Super-admin uses platform scope

- **GIVEN** a resolved platform `SUPER_ADMIN`
- **WHEN** no tenant override is requested
- **THEN** the effective tenant SHALL be absent
- **AND** only approved platform roles and permissions SHALL be effective

#### Scenario: Super-admin requests tenant override

- **GIVEN** a resolved platform `SUPER_ADMIN`
- **WHEN** an explicit tenant override is requested
- **THEN** accesscontrol SHALL validate the tenant and override permission
- **AND** record the effective tenant decision for audit
- **AND** deny invalid or unauthorized override attempts

#### Scenario: Super-admin tenant override is active

- **GIVEN** a validated `SUPER_ADMIN` tenant override
- **WHEN** tenant-scope authorities are resolved
- **THEN** the override SHALL grant only the configured override authority set
- **AND** SHALL NOT bypass authorization
- **AND** the V1 authority set SHALL initially be equivalent to `TENANT_ADMIN`

### Requirement: Spring authorities derive from effective access

The HTTP access-context pipeline SHALL map approved Tchalanet roles to `ROLE_*` and permissions to
`PERM_*` authorities.

#### Scenario: Effective access is attached to authentication

- **GIVEN** a valid effective access snapshot
- **WHEN** Spring `Authentication` is enriched
- **THEN** its business authorities SHALL correspond to the snapshot
- **AND** technical principal facts SHALL remain available

### Requirement: Effective tenant follows API scope

Effective tenant resolution SHALL deny ambiguous or invalid scope/tenant combinations before
controller execution.

#### Scenario: Tenant or admin scope has no tenant

- **GIVEN** an authenticated `TENANT` or `ADMIN` request
- **WHEN** no effective tenant can be resolved
- **THEN** access SHALL be denied before controller execution

#### Scenario: Platform scope has no override

- **GIVEN** an authenticated `PLATFORM` request
- **WHEN** no explicit tenant override is validated
- **THEN** the effective tenant SHALL be absent

### Requirement: Effective tenant is resolved by a dedicated resolver, not header parsing

Effective tenant SHALL be produced by an `EffectiveTenantResolver` within access resolution that
validates access. A normal AppUser's effective tenant SHALL come from the user's single active
membership; a request header SHALL NOT select the tenant for a normal user.

#### Scenario: Normal user tenant comes from membership

- **GIVEN** a normal AppUser with one active tenant membership
- **WHEN** effective tenant is resolved
- **THEN** the effective tenant SHALL be that membership's tenant
- **AND** any `X-Tenant-Id` header on the request SHALL be ignored

#### Scenario: SellerTerminal tenant comes from the bootstrapped actor

- **GIVEN** a resolved `SELLER_TERMINAL` actor
- **WHEN** effective tenant is resolved
- **THEN** the tenant SHALL be taken from the bootstrapped actor
- **AND** SHALL NOT be chosen from request headers

#### Scenario: SellerTerminal may not select or override a tenant by header

- **GIVEN** a resolved `SELLER_TERMINAL` actor
- **WHEN** the request carries `X-Tenant-Id` or `X-Tch-Tenant-Override`
- **THEN** the request SHALL be denied with `terminal.tenant_selection_not_allowed`

### Requirement: Super-admin tenant override uses a dedicated header and a reason

A `SUPER_ADMIN` SHALL have no effective tenant by default and SHALL enter a tenant only through a
dedicated override header (the only header-driven tenant selection in the pipeline). The override
SHALL require the `platform.tenant.override` permission, a valid target tenant, and a non-blank
reason, and SHALL be audited.

#### Scenario: Valid super-admin override

- **GIVEN** a resolved `SUPER_ADMIN` with `platform.tenant.override`
- **AND** `X-Tch-Tenant-Override` is a valid UUID of an ACTIVE tenant
- **AND** `X-Tch-Override-Reason` is present and non-blank
- **WHEN** effective tenant is resolved
- **THEN** the effective tenant SHALL be the target tenant
- **AND** `tenantOverride` SHALL be true
- **AND** the override decision SHALL be audited

#### Scenario: Super-admin without override has no tenant

- **GIVEN** a resolved `SUPER_ADMIN`
- **WHEN** no `X-Tch-Tenant-Override` header is present
- **THEN** the effective tenant SHALL be absent regardless of any other header

#### Scenario: Override is missing the reason or permission

- **GIVEN** a resolved `SUPER_ADMIN` requesting a tenant override
- **WHEN** `X-Tch-Override-Reason` is blank or `platform.tenant.override` is absent
- **THEN** the override SHALL be denied
