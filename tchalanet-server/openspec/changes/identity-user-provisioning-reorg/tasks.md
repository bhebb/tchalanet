# Tasks: identity-user-provisioning-reorg

## 1. Package cleanup

- [ ] Move identity domain-ish records from `internal.service` to `internal.model`.
- [ ] Move Keycloak sync classes to `internal.service.keycloak`.
- [ ] Move `DefaultIdentityApi` to `internal.adapter`.
- [ ] Keep controllers under `internal.web.me`, `internal.web.admin`, `internal.web.ops`.

## 2. Tenant membership cleanup

- [ ] Remove `AccessControlApi` dependency from `TenantMembershipService`.
- [ ] Remove `setRole(...)` from `TenantMembershipService`.
- [ ] Change `assign(...)` to not accept access-control `RoleId`.
- [ ] Add `requireTenantMember(...)` or equivalent.
- [ ] Ensure `tenant_user` is membership-only.

## 3. Provisioning service

- [ ] Create `TenantUserProvisioningService`.
- [ ] Implement tenant user creation orchestration.
- [ ] Call `AccessControlApi.assignRoleToUser(...)`.
- [ ] Keep role-permission defaults in access-control.
- [ ] Return `TenantUserProvisioningResult`.

## 4. Admin view service

- [ ] Create `TenantUserAdministrationService` or assembler.
- [ ] Move `loadAndMap(...)` out of controller.
- [ ] Compose profile + membership + roles/effective permissions.
- [ ] Preserve invitation/sync status behavior.

## 5. Controllers

- [ ] Update `IdentityUserAdminController.create(...)` to call provisioning service.
- [ ] Update `setRole(...)` to call access-control service/API, not membership.
- [ ] Move tenant-scope assertion into service.
- [ ] Keep controller thin.

## 6. Tests

- [ ] Test membership assignment does not call access-control.
- [ ] Test provisioning creates user + membership + role assignment.
- [ ] Test admin view composition.
- [ ] Test tenant-scoped user guard.
