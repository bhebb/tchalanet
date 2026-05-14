## ADDED Requirements

### Requirement: Tenant Config Implements Context Lookup

`platform.tenantconfig` SHALL provide the implementation of `common.context.tenant.TenantContextLookup`.

The implementation SHALL live under `platform.tenantconfig.internal.context`.

#### Scenario: Context resolver needs tenant metadata

- **GIVEN** common context creation has an extracted tenant id or tenant code
- **WHEN** tenant metadata is required
- **THEN** the common resolver calls `TenantContextLookup`
- **AND** `TenantConfigContextLookup` resolves the tenant using platform tenant configuration persistence.

### Requirement: Tenant Lookup Resolves Already Extracted Tenant Inputs

Tenant context lookup SHALL resolve tenants from values already extracted by the request pipeline, such as tenant id, tenant code or an authorized override input.

It SHALL NOT place complex routing or product business policy inside `common.context`.

#### Scenario: JWT contains tenant claim

- **GIVEN** a JWT tenant claim has already been extracted
- **WHEN** the context resolver needs the tenant
- **THEN** tenantconfig lookup resolves the tenant info from that id or code.

#### Scenario: Public routing requires tenant policy

- **GIVEN** a public route, subdomain or product policy determines tenant behavior
- **WHEN** that policy is more than an id/code lookup
- **THEN** the policy belongs outside `common.context`
- **AND** common receives only the extracted tenant input or default tenant decision.

### Requirement: Tenant Lookup Does Not Own Operational Validation

Tenant config lookup SHALL NOT validate POS operational resources.

#### Scenario: Operational context contains outlet and terminal

- **GIVEN** a request has tenant, outlet, terminal and session inputs
- **WHEN** tenant config lookup resolves the effective tenant
- **THEN** it returns tenant context info only
- **AND** terminal/outlet/session validation remains in the owning core domains.
