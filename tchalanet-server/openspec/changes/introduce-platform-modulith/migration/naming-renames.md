# Naming and Rename Plan

## Package renames

| Old package                               | New package                                   |
| ----------------------------------------- | --------------------------------------------- |
| `com.tchalanet.server.core.audit`         | `com.tchalanet.server.platform.audit`         |
| `com.tchalanet.server.core.accesscontrol` | `com.tchalanet.server.platform.accesscontrol` |
| `com.tchalanet.server.core.tenantuser`    | `com.tchalanet.server.platform.identity`      |
| `com.tchalanet.server.core.tenantconfig`  | `com.tchalanet.server.platform.tenantconfig`  |
| `com.tchalanet.server.core.tenanttheme`   | `com.tchalanet.server.platform.tenanttheme`   |

## Capability names

Use these canonical capability keys:

```text
audit
accesscontrol
identity
tenantconfig
tenanttheme
document
communication
idempotence
```

## API naming

Platform public APIs:

```text
AuditApi
AccessControlApi
IdentityApi
TenantConfigApi
TenantThemeApi
DocumentApi
CommunicationApi
IdempotencyApi
```

Platform internal services:

```text
AuditService
AccessControlService
IdentityService
TenantConfigService
TenantThemeService
DocumentService
CommunicationService
IdempotencyService
```

## Rename principles

- Rename packages first, then classes only when the old name is misleading.
- Preserve HTTP routes unless an API-change spec explicitly approves a route change.
- Prefer `identity` over `tenantuser` when the capability includes profile, actor, preferences, Keycloak mapping, or effective context.
- Keep `accesscontrol`, not generic `security`, for application authorization decisions.
