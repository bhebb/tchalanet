# FEATURE_TENANT_ADMIN_TERMINALS

> **Status**: MIGRATED
> **Former feature slice**: `com.tchalanet.server.features.tenantadmin.terminals` > **Current owner**: `com.tchalanet.server.core.terminal.infra.web.admin`

The Tenant Admin terminals surface is no longer a `features/tenantadmin` vertical slice.

Mono-domain terminal administration is owned by `core.terminal` and exposed by:

- `core.terminal.infra.web.admin.TerminalAdminController`
- Routes rooted at `/admin/terminals`

Terminal lifecycle, lock/unlock behavior, metadata updates, heartbeat handling, and device security rules belong in `core.terminal`.
Only future composite screens that aggregate terminals with other domains may be added back under `features/tenantadmin`.
