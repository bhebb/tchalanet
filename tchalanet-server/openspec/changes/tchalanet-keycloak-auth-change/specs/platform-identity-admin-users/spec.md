# Spec — platform-identity-admin-users

## Intent

Make Tchalanet the tenant-admin user management interface. Keycloak remains transparent to tenant admins and users.

## Canonical surface

```http
GET    /admin/identity/users
POST   /admin/identity/users
GET    /admin/identity/users/{userId}
PUT    /admin/identity/users/{userId}
PATCH  /admin/identity/users/{userId}/preferences
PUT    /admin/identity/users/{userId}/membership
DELETE /admin/identity/users/{userId}/membership
PUT    /admin/identity/users/{userId}/role
POST   /admin/identity/users/{userId}/suspend
POST   /admin/identity/users/{userId}/reactivate
POST   /admin/identity/users/{userId}/send-invitation
POST   /admin/identity/users/{userId}/resync-keycloak
```

## Controller cleanup

Replace/merge the current duplicate surfaces:

```text
/admin/users
/admin/membership
```

Target controller:

```text
platform.identity.internal.web.admin.IdentityUserAdminController
```

Target path:

```text
/admin/identity/users
```

## Required removals

- Remove `/admin/users/tenant/{tenantId}` from tenant-admin scope.
- Remove `/admin/membership/bootstrap`.
- Do not accept tenant id in tenant-admin path/body by default.
- Current-user bootstrap belongs under `/tenant/me/profile/bootstrap`.

## User creation flow

```text
Tenant admin creates user in Tchalanet
-> platform.identity creates local app user
-> membership assigned to current tenant
-> role assigned
-> after commit provisioning event/request creates or updates Keycloak user
-> keycloakSub stored
-> invitation sent if requested
```

## Keycloak sync state

User admin response must expose enough operational state:

```text
userId
displayName
email
phone
status
role
membershipStatus
outletId
terminalId
keycloakSub
keycloakSyncStatus
invitationStatus
createdAt
```

Suggested sync states:

```text
PENDING
SYNCED
FAILED
NOT_REQUIRED
```

## Security rules

- `/admin/identity/users/**` requires `TENANT_ADMIN` or `SUPER_ADMIN`.
- Fine-grained permission annotations may be added per action.
- Tenant admin cannot create or assign `SUPER_ADMIN`.
- Tenant admin cannot manage users outside the effective tenant.
- Super admin should use tenant override or platform-specific admin surfaces.

## Email update rule

P0: email is read-only after creation unless a dedicated Keycloak-synchronized email update flow exists.

## Audit rules

All writes must be audited:

```text
create user
update profile
update preferences
assign/unassign membership
set role
suspend/reactivate/delete
send invitation
resync Keycloak
```

## Acceptance criteria

- Tenant admin can create user from Tchalanet UI/API.
- Keycloak user is provisioned in background or marked failed with retry.
- Tenant id is resolved from context, not path.
- Duplicate old controllers are removed or deprecated.
- All write endpoints validate request bodies and are audited.
