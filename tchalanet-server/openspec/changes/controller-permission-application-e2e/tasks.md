# Tasks: controller-permission-application-e2e

## 1. Controller audit

- [ ] Produce endpoint -> permission table for identity/accesscontrol/ops.
- [ ] Mark endpoints requiring audit.
- [ ] Mark endpoints requiring operational context.
- [ ] Mark endpoints currently accepting tenantId from client.

## 2. Identity controllers

- [ ] Add method permissions to `IdentityUserAdminController`.
- [ ] Keep class-level role gate if useful.
- [ ] Replace create flow with `TenantUserProvisioningService`.
- [ ] Move `loadAndMap` into admin service/assembler.
- [ ] Move tenant member assertion into service.
- [ ] Keep current profile controller role gate or add `profile.*` permissions if seeded.

## 3. Access-control controller

- [ ] Move path to `/admin/access-control`.
- [ ] Remove `tenantId` query param.
- [ ] Return `ApiResponse<T>`.
- [ ] Remove raw `UUID` return.
- [ ] Add role assignment endpoints.
- [ ] Add user permission override endpoints.
- [ ] Disable tenant custom role CRUD in V1.

## 4. Ops controller

- [ ] Rename to `PlatformIdentitySyncOpsController` if appropriate.
- [ ] Add `platform.ops.execute` permission.
- [ ] Add audit.
- [ ] Ensure only platform/SUPER_ADMIN scope.

## 5. Python E2E fixtures

- [ ] Add `access-control.v1.json`.
- [ ] Add `users.v1.json`.
- [ ] Add `operational-context.v1.json`.
- [ ] Ensure E2E fixtures match backend JSON or assert mismatch.

## 6. Python E2E tests

- [ ] Tenant A onboarding normal path.
- [ ] CASHIER default role can sell.
- [ ] DENY `ticket.sell` blocks sale with 403.
- [ ] Removing CASHIER role blocks sale with 403.
- [ ] Missing/untrusted operational context blocks sale with operational-context error.
- [ ] Tenant admin cannot manage another tenant.
- [ ] Platform ops access-control bootstrap requires proper permission.

## 7. Build and verification

- [ ] Backend tests pass.
- [ ] ArchUnit security tests pass.
- [ ] Python E2E critical scenarios pass.
