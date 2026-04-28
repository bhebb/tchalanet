# FEATURE_TENANT_ADMIN_OUTLETS

> **Status**: MIGRATED
> **Former feature slice**: `com.tchalanet.server.features.tenantadmin.outlets` > **Current owner**: `com.tchalanet.server.core.outlet.infra.web.admin`

The Tenant Admin outlets surface is no longer a `features/tenantadmin` vertical slice.

Mono-domain outlet administration is owned by `core.outlet` and exposed by:

- `core.outlet.infra.web.admin.OutletAdminController`
- Routes rooted at `/admin/outlets`

Any outlet lifecycle rule, validation, persistence behavior, or tenant-scoped query belongs in `core.outlet`.
Only future composite screens that aggregate outlets with other domains may be added back under `features/tenantadmin`.
