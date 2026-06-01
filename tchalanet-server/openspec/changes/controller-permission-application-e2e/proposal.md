# Change: controller-permission-application-e2e

## Decision

Apply the access-control model to critical controllers and prove it with Python E2E tests.

Controllers must declare requirements through annotations:

```java
@PreAuthorize("hasPermission('permission.key')")
```

or a future project meta-annotation:

```java
@RequiredPermission("permission.key")
```

Controllers do not make manual authorization decisions.

## Why

The permission model is only useful if:

- controllers declare protected actions consistently;
- `TchPermissionEvaluator` evaluates through access-control;
- E2E proves role grants, role removal, DENY overrides, and operational-context failure.

## What

- Audit critical controllers.
- Add endpoint → permission mapping.
- Update `IdentityUserAdminController`.
- Update `AccessControlAdminController`.
- Update `PlatformIdentitySyncOpsController`.
- Ensure `ApiResponse<T>` for 2xx JSON responses.
- Remove tenant ID query params from `/admin/**`.
- Add Python E2E fixtures and tests.

## Non-goals

- Apply permissions to every controller in the system in one patch.
- Build frontend permission UI.
- Tenant custom roles.
- Replace operational context with permission.
