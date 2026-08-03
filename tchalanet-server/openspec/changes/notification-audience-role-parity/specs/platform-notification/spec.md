## MODIFIED Requirements

### Requirement: Notification audience model

Notifications SHALL support audience types:

- SPECIFIC_ACTORS
- PLATFORM_ADMINS
- ALL_APP_USERS
- TENANT_ADMINS
- TENANT_APP_USERS
- TENANT_SELLER_TERMINALS

`TENANT_ADMINS` SHALL contain each active `TENANT_OWNER` and `TENANT_ADMIN` membership for the
target tenant. `PLATFORM_ADMINS` SHALL contain only active `SUPER_ADMIN` users.

#### Scenario: Tenant owner and administrator receive the same audience

- **GIVEN** a tenant has an active owner, an active administrator, and an active seller
- **WHEN** a notification targets `TENANT_ADMINS`
- **THEN** the owner and administrator can see it in the tenant inbox
- **AND** both are included when external recipients are resolved
- **AND** the seller is excluded

#### Scenario: Tenant audience is isolated from platform scope

- **GIVEN** a tenant notification targets `TENANT_ADMINS`
- **WHEN** an unrelated superadministrator lists platform notifications
- **THEN** the tenant notification is not returned

#### Scenario: Platform audience remains superadministrator-only

- **GIVEN** a notification targets `PLATFORM_ADMINS`
- **WHEN** it is persisted and recipients are resolved
- **THEN** only active `SUPER_ADMIN` users receive it
- **AND** tenant owners and tenant administrators cannot read it from their tenant inbox
