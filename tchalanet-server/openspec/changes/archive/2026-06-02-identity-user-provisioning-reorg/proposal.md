# Change: identity-user-provisioning-reorg

## Decision

`platform.identity` is reorganized so it owns identity only:

```text
AppUser
User profile
User preference
Keycloak subject mapping
Tenant membership
Current user profile
User bootstrap/sync
Tenant user provisioning orchestration
```

It does not own:

```text
roles
permissions
role-permission mappings
user permission overrides
effective permissions
runtime operational-context validation
```

Those belong to `platform.accesscontrol` and the operational-context runtime flow.

## Why

The current identity services/controllers mix identity, tenant membership, role assignment, profile updates, preferences, invitations, and Keycloak sync.

Example problem:

```java
TenantMembershipService.setRole(...)
```

This makes membership depend on access-control and encourages controllers to orchestrate too much.

## What

Reorganize `platform.identity` packages and responsibilities:

- move internal models out of `internal.service`;
- make `TenantMembershipService` membership-only;
- remove `AccessControlApi` from `TenantMembershipService`;
- create `TenantUserProvisioningService`;
- create `TenantUserAdministrationService` or response assembler;
- route role assignment through `AccessControlApi` from provisioning/admin orchestration, not membership service;
- move Keycloak sync into dedicated package;
- keep operational context out of identity.

## Impact

Controllers become thinner.

User creation flow becomes:

```text
Identity:
- create/link app_user
- create tenant_membership

AccessControl:
- assign system role
- effective permissions come from role + overrides
```

No direct permission list is copied into the user.

## Non-goals

- Full Keycloak redesign.
- Tenant custom roles.
- UI implementation for permissions.
- Moving operational context into identity.
