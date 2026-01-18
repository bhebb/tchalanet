# Authorization & Permissions

## Principle

- Authorization decisions belong to `core.accesscontrol` domain.
- Web layer expresses requirements, domain decides.

## Method security

- Use Spring Method Security with:
  - `@PreAuthorize(...)` + `hasPermission(..)` OR
  - custom meta-annotation `@RequiredPermission("perm.key")`
- Controllers MUST NOT implement permission checks manually.

## Permission evaluation

- `TchPermissionEvaluator` is the single adapter.
- It MUST:
  - extract principal from Authentication (TchRequestContext)
  - normalize permission key
  - call `CheckUserPermissionsHandler` with `(tenantId, userId, perms)`
  - return false on deny (no exceptions leaking)

## Tenant resolution

- TenantId and UserId MUST use wrappers (TenantId.of(ctx.principal().tenantUuid()), UserId.of(...)).
- Effective tenant comes from `TchRequestContext` (supports SUPER_ADMIN override).
