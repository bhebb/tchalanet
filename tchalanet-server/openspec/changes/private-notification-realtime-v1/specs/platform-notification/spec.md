## ADDED Requirements

### Requirement: Realtime private notification change stream

The platform SHALL expose authenticated Server-Sent Event streams for platform and tenant-admin
notification scopes. A stream event SHALL contain no notification content and SHALL instruct the
client only to refresh its authorized notification snapshot.

#### Scenario: Tenant admin receives a newly published tenant notification

- **WHEN** a notification for `TENANT_ADMINS` is published and committed for a tenant
- **THEN** connected admin streams for that effective tenant receive one `notification-change` event
- **AND** admin streams for other tenants receive no event

#### Scenario: Platform administrator receives a platform notification

- **WHEN** a notification for `PLATFORM_ADMINS` is published and committed
- **THEN** connected platform streams receive one `notification-change` event
- **AND** tenant-admin streams receive no event

#### Scenario: Client reconnects after a transient stream failure

- **WHEN** an authenticated stream disconnects unexpectedly
- **THEN** the web client reconnects with the same bearer and, where applicable, support override headers
- **AND** it refreshes notification content only through the existing authorized REST endpoints
