# Request Context & Tenant Resolution

- Context is published by `TchContextFilter`.
- Code accesses it via:
  - `@CurrentContext TchRequestContext`
  - `TchContext.currentOrNull()`

Rules:

- Effective tenant comes from context.
- SUPER_ADMIN may override tenant via `X-Tenant-Id` / `tenantId`.
- `deleted_visibility` is allowed only for SUPER_ADMIN and is enforced by context.

Persistence:

- `TenantEntityListener` sets `tenant_id` for `BaseTenantEntity` from `TchContext`.
- Never pass `tenant_id` from client into entities directly.
