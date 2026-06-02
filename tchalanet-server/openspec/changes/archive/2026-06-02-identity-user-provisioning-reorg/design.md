# Design: identity-user-provisioning-reorg

## Architecture boundary

`platform.identity` is a platform capability.

It follows the platform shape:

```text
platform/<capability>/
  api/
    XxxApi.java
    model/
  internal/
    service/
    persistence/
    web/
    event/
    adapter/
    cache/
    config/
```

It exposes only `platform.identity.api`.

Other modules must not import `platform.identity.internal`.

## Target package structure

```text
platform/identity/
  api/
    IdentityApi.java
    model/
      request/
        BootstrapCurrentUserRequest.java
        UpdateUserProfileRequest.java
        CreateTenantUserRequest.java
      view/
        CurrentUserView.java
        UserProfileView.java
        TenantMembershipView.java
        TenantUserAdminView.java
      result/
        TenantUserProvisioningResult.java

  internal/
    model/
      AppUser.java
      TenantMembership.java
      UserPreference.java
      TenantUserRow.java
      UserRow.java

    service/
      AppUserService.java
      CurrentUserProfileService.java
      TenantMembershipService.java
      TenantUserProvisioningService.java
      TenantUserAdministrationService.java
      UserBootstrapService.java

    service/keycloak/
      KeycloakBootstrapSyncService.java
      KeycloakBootstrapSyncListener.java
      KeycloakBootstrapProperties.java

    persistence/
      entity/
        AppUserJpaEntity.java
        TenantMembershipJpaEntity.java
        UserPreferenceJpaEntity.java
      repository/
      adapter/
        AppUserJpaAdapter.java
        TenantMembershipJpaAdapter.java
        UserPreferenceJpaAdapter.java
      mapper/

    adapter/
      IdentityApiAdapter.java

    web/me/
      CurrentUserProfileController.java

    web/admin/
      IdentityUserAdminController.java

    web/ops/
      PlatformIdentitySyncOpsController.java
```

## TenantMembershipService

Membership-only.

Allowed:

```text
list tenant members
find tenant/user membership
require active tenant membership
assign membership
unassign membership
find membership createdAt
```

Forbidden:

```text
setRole
assign permission
deny permission
calculate effective permissions
call AccessControlApi
```

Target method shape:

```java
public TenantMembership assign(
    TenantId tenantId,
    UserId userId,
    OutletId outletId,
    TerminalId terminalId,
    boolean owner
)
```

Remove or deprecate:

```java
assign(... RoleId roleId ...)
setRole(...)
```

## TenantUserProvisioningService

Owns orchestration:

```text
create/link app_user
create tenant membership
call AccessControlApi.assignRoleToUser(...)
return admin view/result
```

It may inject:

```text
AppUserService
TenantMembershipService
AccessControlApi
TenantUserAdministrationService
```

It must not copy role permissions into user rows.

## TenantUserAdministrationService

Owns admin view composition:

```text
profile from identity
membership from identity
roles/effective permissions from accesscontrol
invitation/sync status
```

This removes `loadAndMap(...)` from the controller.

## RoleId migration

If `TenantMembership.roleId` represents an access-control role:

```text
remove it from membership
use tenant_user_role instead
```

If it is a legacy membership type:

```text
rename it to avoid confusion
```

V1 recommendation: remove `role_id` from membership and move role assignment to `tenant_user_role`.

## CurrentUserProfileController

Allowed:

```text
read current profile
bootstrap current user from Keycloak subject
update profile/preferences
expose available surfaces
```

Caution:

```text
capabilities exposed to frontend are UI hints only unless they come from AccessControlApi.effectivePermissions
```

Do not make UI capabilities the authorization source of truth.

## PlatformIdentitySyncOpsController

Rename from generic `PlatformSyncOpsController`.

Path remains:

```text
/platform/ops/sync/identity/keycloak-bootstrap-users
```

Security:

```text
SUPER_ADMIN role gate + platform.ops.execute permission
```

Audit required.

## Operational context boundary

Do not move operational context into identity.

Operational context remains:

```text
terminalId
outletId
salesSessionId
source
```

Sensitive actions require:

```text
permission
+ trusted operational context
+ terminal/outlet/session validation
```

No permission named `operational-context.valid`.
