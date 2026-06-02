# Change: access-control-v1

## Decision

Tchalanet V1 uses a hybrid access-control model:

```text
Role = system-defined business profile / default permission bundle.
Permission = effective backend action right.
Effective permissions = active role permissions + user GRANT overrides - user DENY overrides.
```

V1 does **not** support tenant custom roles.

System roles are fixed and platform-managed:

- `SUPER_ADMIN`
- `TENANT_ADMIN`
- `OPERATOR`
- `CASHIER`

Tenant admins may:

- list available roles and permissions;
- assign/remove system roles to users;
- add/remove user-level permission overrides such as `DENY ticket.sell`.

Tenant admins may not:

- create custom roles;
- edit system role-permission defaults;
- create custom permissions.

Custom tenant roles are deferred to roadmap 2027.

## Why

E2E runtime and tenant onboarding require a stable, testable access-control model.

The previous seed/migration approach mixed permissions, roles, role-permission mappings, local/dev users, tenant membership, and role assignment.

The new model separates:

```text
permission catalog      -> what the backend can protect
system roles            -> official profiles
role_permission          -> default capabilities of each role
tenant_user_role         -> roles assigned to a user in a tenant
user_permission_override -> explicit GRANT/DENY exceptions
```

## What

Add or complete:

- permission catalog in `platform.accesscontrol`;
- system roles;
- role-permission default matrix;
- user role assignment/removal;
- user permission overrides `GRANT` / `DENY`;
- effective permission calculation;
- `AccessControlApi` methods;
- `TchPermissionEvaluator` alignment;
- versioned backend JSON for default role-permission matrix;
- idempotent bootstrap task for permissions, roles, and system mappings.

## Impact

Runtime:

- controllers can use `@PreAuthorize("hasPermission('permission.key')")`;
- `TchPermissionEvaluator` delegates to access-control;
- users get permissions through roles and overrides;
- no role-permission list is copied into user rows at creation time.

Database:

- `tenant_user` becomes membership-only;
- role assignment moves to `tenant_user_role`;
- permission overrides move to `user_permission_override`.

E2E:

- Python tests can assert that role defaults are applied;
- removing role `CASHIER` blocks sale;
- `DENY ticket.sell` blocks sale even if user remains `CASHIER`.

## Non-goals

- Tenant custom roles.
- Tenant custom permissions.
- Full role designer UI.
- Permission per UI button.
- Moving authorization decisions into controllers.
- Replacing operational context validation with permissions.

## Out of scope

- Business-domain authorization rules outside `platform.accesscontrol`.
- Keycloak role model redesign.
- Full frontend role/permission management UI.

## Backlog / Roadmap 2027

- Tenant custom roles.
- Clone system role into tenant role.
- Tenant-managed role-permission matrix.
- Permission grouping UX.
- Role templates and approval workflow.
