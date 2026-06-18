# Spec: core-sellerterminal

## ADDED Requirements

### Requirement: Seller terminal is the V0 operational seller unit

`core.sellerterminal` SHALL model the tenant-scoped operational unit used for V0 sales, controls and reporting.

#### Scenario: Active seller terminal can be used for sale

- **GIVEN** a seller terminal belongs to the current tenant
- **AND** its status is `ACTIVE`
- **WHEN** sales resolves operational seller identity
- **THEN** the seller terminal is eligible for sell-time validation.

#### Scenario: Blocked seller terminal cannot sell

- **GIVEN** a seller terminal has status `BLOCKED`
- **WHEN** a sell command attempts to use it
- **THEN** the sale is rejected before ticket persistence.

#### Scenario: Non-active seller terminal statuses cannot sell

- **GIVEN** a seller terminal has status `SUSPENDED`, `DISABLED` or `DELETED`
- **WHEN** sales resolves operational seller identity
- **THEN** the seller terminal is not eligible for sell-time validation
- **AND** the operation fails before ticket persistence.

#### Scenario: Cross-tenant seller terminal is rejected

- **GIVEN** a seller terminal belongs to tenant A
- **AND** the current request context is tenant B
- **WHEN** a sell command attempts to use that seller terminal
- **THEN** the operation is rejected.

### Requirement: Seller terminal persistence is tenant-scoped

The `seller_terminal` table SHALL include `tenant_id`, `terminal_code`, display/contact fields, optional Firebase/PIN credentials, commission, status, block reason, audit timestamps, soft-delete metadata, tenant-scoped uniqueness for `terminal_code`, required RLS policies and tenant indexes.

#### Scenario: Duplicate terminal code within tenant is rejected

- **GIVEN** tenant A already has seller terminal code `ST-001`
- **WHEN** tenant A creates another seller terminal with code `ST-001`
- **THEN** persistence rejects the duplicate.

#### Scenario: Same terminal code across tenants is allowed

- **GIVEN** tenant A has seller terminal code `ST-001`
- **WHEN** tenant B creates seller terminal code `ST-001`
- **THEN** persistence accepts it.

#### Scenario: Outlet and address are not required to create seller terminal

- **GIVEN** tenant admin creates a V0 seller terminal
- **WHEN** the command is validated
- **THEN** it does not require `outlet_id` or `address_id`.

### Requirement: Seller terminal admin operations are protected

Admin endpoints for seller terminal create, update, block, reset PIN, commission, limits and odds SHALL require explicit seller-terminal permissions.

#### Scenario: Unauthorized admin cannot block a seller terminal

- **GIVEN** an authenticated tenant admin lacks seller-terminal block permission
- **WHEN** they call the block endpoint
- **THEN** the request is denied by authorization before domain mutation.

#### Scenario: Authorized admin blocks a seller terminal

- **GIVEN** an authenticated tenant admin has seller-terminal block permission
- **WHEN** they block a seller terminal
- **THEN** the seller terminal becomes blocked
- **AND** a seller-terminal blocked event is published after commit.

### Requirement: Seller terminal slice replaces legacy core terminal slice

Current seller-terminal behavior SHALL move from `core.terminal` to `core.sellerterminal`, and the legacy `core.terminal` package SHALL be deleted after import, bean and build verification.

#### Scenario: Runtime imports use core sellerterminal

- **GIVEN** seller-terminal migration is complete
- **WHEN** the backend compiles
- **THEN** no runtime code imports `core.terminal` seller-terminal internals.

#### Scenario: Legacy terminal beans are not registered

- **GIVEN** the application context starts
- **WHEN** Spring scans backend beans
- **THEN** seller-terminal beans are registered from `core.sellerterminal`
- **AND** no replacement dependency requires the old `core.terminal` package.

### Requirement: Seller terminal exposes POS runtime profile

Seller-terminal POS runtime endpoints SHALL expose the authenticated terminal profile and operational context for the current POS actor.

#### Scenario: POS actor reads current seller terminal profile

- **GIVEN** an authenticated seller-terminal POS actor
- **WHEN** it calls `/tenant/seller-terminal/me`
- **THEN** the backend returns the current seller terminal id, tenant id, code, display name, commission and status facts required by POS runtime.

#### Scenario: POS actor reads operational context

- **GIVEN** an authenticated seller-terminal POS actor
- **WHEN** it calls `/tenant/seller-terminal/operational-context`
- **THEN** the backend returns the operational context for that seller terminal
- **AND** it does not require outlet or sales-session context.
