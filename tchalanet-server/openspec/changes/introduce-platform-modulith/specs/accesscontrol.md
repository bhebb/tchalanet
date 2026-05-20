# Spec — Migrate core.accesscontrol to platform.accesscontrol

## Goal

Move access-control application capability out of `core` into `platform`.

## Old package

```text
com.tchalanet.server.core.accesscontrol.*
```

## New package

```text
com.tchalanet.server.platform.accesscontrol.api.*
com.tchalanet.server.platform.accesscontrol.internal.*
```

## Public API

```text
platform.accesscontrol.api.AccessControlApi
platform.accesscontrol.api.model.PermissionCheckRequest
platform.accesscontrol.api.model.PermissionCheckResult
platform.accesscontrol.api.model.UserPermissionView
platform.accesscontrol.api.model.RoleAssignmentView
```

## Internal implementation

```text
internal/app/AccessControlService
internal/app/PermissionResolver
internal/app/AccessPolicyEvaluator
internal/persistence/*JpaEntity
internal/persistence/*Repository
internal/web/AccessControlAdminController
internal/cache/*CacheSpecs
```

## Rename rules

```text
core.accesscontrol.application.query.model.CheckUserPermissionsQuery
  -> platform.accesscontrol.api.model.PermissionCheckRequest OR keep as internal query only if bus remains used

core.accesscontrol.application.query.handler.CheckUserPermissionsHandler
  -> platform.accesscontrol.internal.app.AccessControlService / internal query handler if retained

common.security.TchPermissionEvaluator dependency target
  -> platform.accesscontrol.api.AccessControlApi
```

## Dependency rule

`platform.accesscontrol` must not depend on `core` or `features`.

## Migration tasks

- [ ] Create `AccessControlApi` facade.
- [ ] Move permission/role assignment persistence to internal persistence.
- [ ] Move permission check logic to internal app.
- [ ] Update Spring Security permission evaluator wiring.
- [ ] Replace imports from `core.accesscontrol` to `platform.accesscontrol.api`.
- [ ] Add Modulith named interface for `api`.
- [ ] Remove legacy package and allowlist.

## Verification

- [ ] Permission checks pass.
- [ ] Controllers still use `@PreAuthorize` normally.
- [ ] No core import from platform.
- [ ] No external import of `platform.accesscontrol.internal`.
