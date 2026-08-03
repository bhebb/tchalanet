# Design - notification audience role parity

## Audience policy

| Audience | Members | Inbox scope | External recipient resolution |
|---|---|---|---|
| `TENANT_ADMINS` | Active `TENANT_OWNER` and `TENANT_ADMIN` memberships for the target tenant | Tenant admin inbox | Both roles |
| `PLATFORM_ADMINS` | Active `SUPER_ADMIN` users | Platform inbox | Superadministrators only |

The in-app repository already implements the first two visibility rules. The identity recipient
query must use the same tenant role set as the visibility query.

## Scope isolation

Tenant notifications carry a tenant id and remain available through the tenant admin API and its
`ADMIN` realtime scope. Platform notifications have no tenant id and remain available through the
platform API and its `PLATFORM` realtime scope.

Global `ALL_APP_USERS` notifications have no tenant id and remain visible in both scopes. They are
the sole null-tenant notification type exposed in a tenant inbox.

`SUPER_ADMIN` is not an implicit tenant role. A superadministrator only sees tenant notifications
when acting in a tenant context with an authorized tenant membership. This prevents cross-tenant
inbox disclosure.

## Implementation

Update the native recipient query to select distinct active users with either `TENANT_OWNER` or
`TENANT_ADMIN`. `distinct` prevents duplicate delivery attempts when a user has both roles.

All app-user list, summary, unread-count, and mark-all-read reads receive the current tenant scope.
The persistence query accepts only the current tenant's notifications, platform notifications in
platform scope, and global `ALL_APP_USERS` notifications in tenant scope.

No database migration is required. The query change applies to future delivery resolution; existing
in-app visibility remains unchanged.

## Verification

- Verify tenant owner and tenant administrator are both resolved for a tenant audience.
- Verify sellers, operators, inactive users, and users from another tenant are excluded.
- Verify platform audience remains limited to superadministrators.
- Verify a tenant notification cannot be read from the platform scope, and vice versa.
