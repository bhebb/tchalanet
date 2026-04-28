# FEATURE_TENANT_ADMIN_USERS

> **Status**: MIGRATED
> **Former feature slice**: `com.tchalanet.server.features.tenantadmin.users` > **Current owner**: `com.tchalanet.server.core.tenantuser.infra.web.admin`

The Tenant Admin users surface is no longer a `features/tenantadmin` vertical slice.

Mono-domain tenant user administration is owned by `core.tenantuser` and exposed by:

- `core.tenantuser.infra.web.admin.TenantUserAdminController`
- Routes rooted at `/admin/users`

User membership, role assignment, preferences, bootstrap, and tenant-scoped user listing belong in `core.tenantuser`.
Only future composite screens that aggregate users with other domains may be added back under `features/tenantadmin`.
