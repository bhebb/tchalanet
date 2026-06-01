# Design: access-control-v1

## Architecture boundary

`platform.accesscontrol` owns:

```text
roles
permissions
role_permission defaults
tenant user role assignments
user permission overrides
effective permission calculation
TchPermissionEvaluator integration
bootstrap of system role-permission matrix
```

It exposes only `platform.accesscontrol.api`.

Other modules must not import `platform.accesscontrol.internal`.

## Target package structure

```text
platform/accesscontrol/
  api/
    AccessControlApi.java
    PermissionKeys.java
    model/
      request/
        CheckUserPermissionsRequest.java
        GetEffectivePermissionsRequest.java
        ListPermissionsRequest.java
        ListRolesRequest.java
        ListRolePermissionsRequest.java
        AssignRoleToUserRequest.java
        RemoveRoleFromUserRequest.java
        GrantUserPermissionRequest.java
        DenyUserPermissionRequest.java
        RemoveUserPermissionOverrideRequest.java
        BootstrapAccessControlRequest.java
      result/
        CheckUserPermissionsResult.java
        BootstrapAccessControlResult.java
      view/
        PermissionView.java
        RoleView.java
        RolePermissionView.java
        EffectivePermissionsView.java
        UserPermissionOverrideView.java

  internal/
    model/
      Permission.java
      AppRole.java
      RolePermission.java
      TenantUserRole.java
      UserPermissionOverride.java
      PermissionEffect.java       # GRANT / DENY

    service/
      PermissionCatalogService.java
      RoleCatalogService.java
      RolePermissionService.java
      TenantUserRoleService.java
      UserPermissionOverrideService.java
      EffectivePermissionService.java
      AccessControlBootstrapService.java

    bootstrap/
      AccessControlBootstrapRunner.java
      AccessControlBootstrapProperties.java
      DefaultRolePermissionMatrixLoader.java
      DefaultRolePermissionMatrix.java

    persistence/
      entity/
        PermissionJpaEntity.java
        AppRoleJpaEntity.java
        RolePermissionJpaEntity.java
        TenantUserRoleJpaEntity.java
        UserPermissionOverrideJpaEntity.java
      repository/
      adapter/
      mapper/

    adapter/
      DefaultAccessControlApi.java

    web/admin/
      AccessControlAdminController.java

    web/platform/
      AccessControlPlatformOpsController.java
```

## JSON default matrix

File:

```text
src/main/resources/access-control/default-role-permissions.v1.json
```

Purpose:

```text
versioned backend bootstrap input
not runtime source of truth
not E2E-only fixture
```

The runtime source remains the database tables.

## Bootstrap modes

`AccessControlBootstrapService` supports:

```text
VALIDATE
APPLY_MISSING
SYNC_SYSTEM
```

### VALIDATE

Reads the JSON and verifies:

- valid codes;
- no duplicates;
- roles reference existing or declared permissions;
- system roles are valid;
- no custom roles in V1.

### APPLY_MISSING

Creates missing:

- permissions;
- system roles;
- role_permission links.

Does not remove anything.

Recommended for local/test startup.

### SYNC_SYSTEM

Strictly synchronizes system roles with the JSON:

- add missing links;
- remove links no longer present;
- only for `system=true` roles;
- protected by `platform.ops.execute`;
- requires audit and reason if exposed through ops endpoint.

## Permission keys V1

```text
platform.access
platform.ops.read
platform.ops.execute

tenant.create
tenant.read
tenant.update
tenant.activate
tenant.suspend
tenant.admin.create
tenant.override

admin.access
dashboard.read

user.read
user.create
user.update
user.disable
user.invite
user.sync
user.membership.manage
user.role.assign
user.permission.manage

role.read
role.manage
role.permission.manage
permission.read

outlet.read
outlet.create
outlet.update
outlet.disable

terminal.read
terminal.create
terminal.update
terminal.disable
terminal.bind
terminal.unbind

session.read
session.open
session.close
session.force-close

settings.read
settings.update

game-pricing.read
game-pricing.update

limit.read
limit.manage

promotion.read
promotion.manage

report.read
audit.read

cashier.access
operator.access

operational-context.read
operational-context.select

ticket.sell
ticket.read
ticket.print
ticket.resend
ticket.verify
ticket.cancel-own

payout.read
payout.review
payout.execute

sync.read
sync.submit

offline.grant.request
offline.sync.submit
```

## Role mapping V1

### SUPER_ADMIN

```text
platform.access
platform.ops.read
platform.ops.execute

tenant.create
tenant.read
tenant.update
tenant.activate
tenant.suspend
tenant.admin.create
tenant.override

audit.read
```

### TENANT_ADMIN

```text
admin.access
dashboard.read

user.read
user.create
user.update
user.disable
user.invite
user.sync
user.membership.manage
user.role.assign
user.permission.manage

role.read
permission.read

outlet.read
outlet.create
outlet.update
outlet.disable

terminal.read
terminal.create
terminal.update
terminal.disable
terminal.bind
terminal.unbind

session.read
session.open
session.close
session.force-close

settings.read
settings.update

game-pricing.read
game-pricing.update

limit.read
limit.manage
promotion.read
promotion.manage

operational-context.read
operational-context.select

report.read
```

### OPERATOR

```text
operator.access
dashboard.read

operational-context.read
operational-context.select

outlet.read
terminal.read

session.read
session.open
session.close

ticket.read
ticket.verify
ticket.print
ticket.resend

payout.read
payout.review
payout.execute

report.read
```

### CASHIER

```text
cashier.access
operational-context.read

ticket.sell
ticket.read
ticket.print
ticket.resend
ticket.verify

payout.read
payout.execute

offline.grant.request
offline.sync.submit
```

## Database target

### `permission`

```sql
permission (
  id uuid primary key,
  code varchar(128) unique not null,
  name varchar(160) not null,
  description text null,
  category varchar(80) null,
  system boolean not null default true,
  active boolean not null default true,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  deleted_at timestamptz null
)
```

### `app_role`

```sql
app_role (
  id uuid primary key,
  tenant_id uuid null,
  code varchar(80) not null,
  name varchar(160) not null,
  description text null,
  scope varchar(32) not null, -- PLATFORM / TENANT
  system boolean not null default true,
  custom boolean not null default false,
  active boolean not null default true,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  deleted_at timestamptz null
)
```

V1:

```text
custom=false
tenant_id=null for system roles
```

### `role_permission`

```sql
role_permission (
  role_id uuid not null references app_role(id),
  permission_code varchar(128) not null references permission(code),
  created_at timestamptz not null,
  created_by uuid null,
  primary key(role_id, permission_code)
)
```

### `tenant_user_role`

```sql
tenant_user_role (
  id uuid primary key,
  tenant_id uuid not null references tenant(id),
  user_id uuid not null references app_user(id),
  role_id uuid not null references app_role(id),
  assigned_at timestamptz not null,
  assigned_by uuid null,
  deleted_at timestamptz null
)
```

Unique active assignment:

```sql
unique(tenant_id, user_id, role_id) where deleted_at is null
```

### `user_permission_override`

```sql
user_permission_override (
  id uuid primary key,
  tenant_id uuid not null references tenant(id),
  user_id uuid not null references app_user(id),
  permission_code varchar(128) not null references permission(code),
  effect varchar(16) not null, -- GRANT / DENY
  reason text null,
  created_at timestamptz not null,
  created_by uuid null,
  deleted_at timestamptz null
)
```

Unique active override:

```sql
unique(tenant_id, user_id, permission_code) where deleted_at is null
```

## Effective permission algorithm

```text
effective = permissions from active roles
          + active user GRANT overrides
          - active user DENY overrides
```

Precedence:

```text
DENY wins over role grants and explicit GRANT.
```

## API changes

Add to `AccessControlApi`:

```java
void assignRoleToUser(AssignRoleToUserRequest request);
void removeRoleFromUser(RemoveRoleFromUserRequest request);

void grantUserPermission(GrantUserPermissionRequest request);
void denyUserPermission(DenyUserPermissionRequest request);
void removeUserPermissionOverride(RemoveUserPermissionOverrideRequest request);

EffectivePermissionsView getEffectivePermissions(GetEffectivePermissionsRequest request);

BootstrapAccessControlResult bootstrap(BootstrapAccessControlRequest request);
```

Keep or adapt:

```java
CheckUserPermissionsResult checkPermissions(CheckUserPermissionsRequest request);
List<RoleView> listRoles(ListRolesRequest request);
List<PermissionView> listPermissions(ListPermissionsRequest request);
List<RolePermissionView> listRolePermissions(ListRolePermissionsRequest request);
```

## TchPermissionEvaluator

Rules:

- single Spring Security adapter;
- extracts tenant/user from `TchRequestContext`;
- normalizes permission key;
- calls `AccessControlApi.checkPermissions(...)`;
- returns `false` on deny or evaluation error;
- does not call repositories directly;
- does not cache without `CacheSpecProvider`.

## Admin endpoints V1

Tenant admin:

```text
GET    /admin/access-control/permissions
GET    /admin/access-control/roles
GET    /admin/access-control/roles/{roleId}/permissions

POST   /admin/access-control/users/{userId}/roles/{roleCode}
DELETE /admin/access-control/users/{userId}/roles/{roleCode}

PUT    /admin/access-control/users/{userId}/permissions/{permissionCode}/grant
PUT    /admin/access-control/users/{userId}/permissions/{permissionCode}/deny
DELETE /admin/access-control/users/{userId}/permissions/{permissionCode}/override
```

Not supported in tenant admin V1:

```text
POST /admin/access-control/roles
PUT  /admin/access-control/roles/{roleId}
PUT  /admin/access-control/roles/{roleId}/permissions
```

Platform ops:

```text
POST /platform/ops/access-control/bootstrap/validate
POST /platform/ops/access-control/bootstrap/apply-missing
POST /platform/ops/access-control/bootstrap/sync-system
```

## Migration from current V42

Current V42 mixes:

- local users;
- permissions;
- roles;
- role_permission mappings;
- tenant_user membership + role assignment.

Target split:

```text
schema migration          -> RBAC tables and constraints
system bootstrap JSON     -> default role-permission matrix
bootstrap runner          -> idempotent create/update/sync
local/e2e fixtures        -> super_admin/admin/cashier local users
```

Local users must not be seeded in production migrations.
