# Tasks: identity-user-provisioning-reorg

<!-- Completed 2026-06-02: create() now delegates to TenantUserProvisioningService (single
     orchestration path, incl. KC realm-role mirroring), loadAndMap + tenant-scope guard moved to
     TenantUserAdminViewAssembler, and the new services/assembler have unit tests (8 green). -->

## 1. Package cleanup

- [x] Move identity domain-ish records from `internal.service` to `internal.model`.
- [x] Move Keycloak sync classes to `internal.service.keycloak`.
- [x] Move `DefaultIdentityApi` to `internal.adapter` (renamed `IdentityApiAdapter` per addendum 2026-06-01).
- [x] Keep controllers under `internal.web.me`, `internal.web.admin`, `internal.web.ops` (`PlatformSyncOpsController` → `PlatformIdentitySyncOpsController`).

## 2. Tenant membership cleanup

- [x] Remove `AccessControlApi` dependency from `TenantMembershipService`.
- [x] Remove `setRole(...)` from `TenantMembershipService`.
- [x] Change `assign(...)` to not accept access-control `RoleId`.
- [x] Add `requireTenantMember(...)` or equivalent. *(equivalent: `TenantUserAdminViewAssembler.assertTenantScoped` → 403; kept at the web boundary so the service layer stays free of `common.web.error`)*
- [x] Ensure `tenant_user` is membership-only.

## 3. Provisioning service

- [x] Create `TenantUserProvisioningService`.
- [x] Implement tenant user creation orchestration.
- [x] Call `AccessControlApi.assignRoleToUser(...)`.
- [x] Keep role-permission defaults in access-control.
- [x] Return `TenantUserProvisioningResult`. *(actual: `ProvisionTenantUserResult`)*

## 4. Admin view service

- [x] Create `TenantUserAdministrationService` or assembler. *(`TenantUserAdminViewAssembler`)*
- [x] Move `loadAndMap(...)` out of controller. *(now `TenantUserAdminViewAssembler.load`)*
- [x] Compose profile + membership + roles/effective permissions. *(profile + membership + invitation/sync; roles intentionally served by `/admin/access-control/users/{id}/roles`, not inlined)*
- [x] Preserve invitation/sync status behavior.

## 5. Controllers

- [x] Update `IdentityUserAdminController.create(...)` to call provisioning service.
- [x] Update `setRole(...)` to call access-control service/API, not membership.
- [x] Move tenant-scope assertion into service. *(`TenantUserAdminViewAssembler.assertTenantScoped`)*
- [x] Keep controller thin.

## 6. Tests

- [x] Test membership assignment does not call access-control. *(`TenantUserProvisioningServiceTest`)*
- [x] Test provisioning creates user + membership + role assignment. *(`TenantUserProvisioningServiceTest`)*
- [x] Test admin view composition. *(`TenantUserAdminViewAssemblerTest`)*
- [x] Test tenant-scoped user guard. *(`TenantUserAdminViewAssemblerTest` + `IdentityUserAdminControllerTest`)*
