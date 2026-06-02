# Design: controller-permission-application-e2e

## Controller architecture rules

Controllers do only:

```text
Bean Validation
request -> command/query/service request mapping
@CurrentContext injection
security annotations
audit annotations
dispatch to service/bus
response mapping
```

Controllers do not:

```text
business logic
manual authorization if/else
repository/adapters calls
tenant resolution from client payload
role-permission calculations
```

## Permission annotation strategy

V1 can use raw Spring expression:

```java
@PreAuthorize("hasPermission('ticket.sell')")
```

Optional project meta-annotation backlog:

```java
@RequiredPermission("ticket.sell")
```

If introduced, it should wrap `@PreAuthorize` or be integrated with method security without bypassing `TchPermissionEvaluator`.

## Endpoint permission mapping

### IdentityUserAdminController

```text
GET    /admin/identity/users
GET    /admin/identity/users/{userId}
→ user.read

POST   /admin/identity/users
→ user.create

PUT    /admin/identity/users/{userId}
PATCH  /admin/identity/users/{userId}/preferences
→ user.update

PUT    /admin/identity/users/{userId}/membership
DELETE /admin/identity/users/{userId}/membership
→ user.membership.manage

PUT    /admin/identity/users/{userId}/role
→ user.role.assign

POST   /admin/identity/users/{userId}/suspend
→ user.disable

POST   /admin/identity/users/{userId}/reactivate
→ user.update

POST   /admin/identity/users/{userId}/send-invitation
→ user.invite

POST   /admin/identity/users/{userId}/resync-keycloak
→ user.sync
```

### CurrentUserProfileController

V1 acceptable:

```text
role gate for authenticated tenant users
```

Preferred:

```text
GET   /tenant/me/profile           → profile.read
POST  /tenant/me/profile/bootstrap → profile.bootstrap
PATCH /tenant/me/profile           → profile.update
```

If `profile.*` permissions are not seeded V1, keep role gate temporarily.

### PlatformIdentitySyncOpsController

```text
POST /platform/ops/sync/identity/keycloak-bootstrap-users
→ platform.ops.execute
→ audit required
```

### AccessControlAdminController

Move path:

```text
/admin/access-control/**
```

Mapping:

```text
GET    /admin/access-control/roles
→ role.read

GET    /admin/access-control/permissions
→ permission.read

GET    /admin/access-control/roles/{roleId}/permissions
→ role.read

POST   /admin/access-control/users/{userId}/roles/{roleCode}
→ user.role.assign

DELETE /admin/access-control/users/{userId}/roles/{roleCode}
→ user.role.assign

PUT    /admin/access-control/users/{userId}/permissions/{permissionCode}/grant
→ user.permission.manage

PUT    /admin/access-control/users/{userId}/permissions/{permissionCode}/deny
→ user.permission.manage

DELETE /admin/access-control/users/{userId}/permissions/{permissionCode}/override
→ user.permission.manage
```

Not supported in tenant admin V1:

```text
POST /admin/access-control/roles
PUT  /admin/access-control/roles/{roleId}
PUT  /admin/access-control/roles/{roleId}/permissions
```

These may exist only as platform/SUPER_ADMIN maintenance endpoints if needed.

## Admin tenant rule

`/admin/**` must use the tenant from `TchRequestContext`.

Do not accept `tenantId` query/body parameter by default.

If SUPER_ADMIN needs to manage another tenant, use platform override flow:

```text
/platform/**
+ explicit tenant override
+ audit
```

## ApiResponse rule

All custom JSON 2xx endpoints should return:

```text
ApiResponse<T>
```

Errors remain:

```text
ProblemDetail
```

## Operational context E2E

Operational context is not a permission.

Sensitive POS actions require:

```text
permission
+ trusted operational context
```

Test matrix:

```text
ticket.sell + trusted context          -> sale accepted
no ticket.sell + trusted context       -> 403
ticket.sell + untrusted/no context     -> operational context error
```

Trusted sources:

```text
SERVER_BOOTSTRAP
SIGNED_DEVICE_BINDING
ADMIN_SELECTION
```

Untrusted:

```text
CLIENT_CLAIM
NONE
```

## Python E2E fixture files

Recommended:

```text
tests/e2e/fixtures/
  access-control.v1.json
  users.v1.json
  operational-context.v1.json
```

`access-control.v1.json` mirrors the backend role-permission matrix for assertions.

It is not the backend runtime source of truth.

## Python E2E scenarios

### Tenant A normal onboarding

```text
SUPER_ADMIN creates Tenant A
SUPER_ADMIN creates first TENANT_ADMIN
TENANT_ADMIN creates outlet
TENANT_ADMIN creates terminal
TENANT_ADMIN creates CASHIER
TENANT_ADMIN opens/prepares session
CASHIER sells ticket with valid trusted operational context
```

### Permission denial

```text
CASHIER role assigned
DENY ticket.sell
attempt sale
expect 403
assert no ticket created
```

### Role removal denial

```text
CASHIER role removed
attempt sale
expect 403
assert no ticket created
```

### Operational context denial

```text
CASHIER has ticket.sell
no trusted context
attempt sale
expect operational context error
assert no ticket created
```

### Tenant isolation

```text
Tenant A admin cannot manage Tenant B users/resources
```

### Access-control matrix assertion

```text
get effective permissions for CASHIER
assert expected role permissions are present
apply DENY ticket.sell
assert ticket.sell absent or denied
```
