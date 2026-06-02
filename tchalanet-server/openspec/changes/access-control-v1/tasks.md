# Tasks: access-control-v1

## 1. Schema

- [ ] Add or adjust `permission` fields: `category`, `system`, `active`, timestamps, soft delete if missing.
- [ ] Add or adjust `app_role` fields: `scope`, `system`, `custom`, `active`.
- [ ] Add `tenant_user_role`.
- [ ] Add `user_permission_override`.
- [ ] Remove or deprecate `tenant_user.role_id`.
- [ ] Add indexes and unique constraints.
- [ ] Add updated_at triggers if project convention requires them.
- [ ] Keep raw UUID only in persistence/SQL.

## 2. JSON bootstrap

- [ ] Create `src/main/resources/access-control/default-role-permissions.v1.json`.
- [ ] Add all V1 permission keys.
- [ ] Add system roles: `SUPER_ADMIN`, `TENANT_ADMIN`, `OPERATOR`, `CASHIER`.
- [ ] Add V1 role-permission mapping.
- [ ] Validate no custom role is present in V1 JSON.

## 3. Bootstrap service

- [ ] Add `DefaultRolePermissionMatrixLoader`.
- [ ] Add `AccessControlBootstrapService`.
- [ ] Add modes: `VALIDATE`, `APPLY_MISSING`, `SYNC_SYSTEM`.
- [ ] Make bootstrap idempotent.
- [ ] Ensure `SYNC_SYSTEM` only touches `system=true` roles.
- [ ] Ensure bootstrap does not create tenant custom roles.

## 4. Services

- [x] Add `PermissionCatalogService` (implemented as `PermissionRegistryService`).
- [x] Add `RoleCatalogService`.
- [x] Add `TenantUserRoleService`.
- [x] Add `UserPermissionOverrideService`.
- [x] Add `EffectivePermissionService`.
- [x] Implement DENY precedence.
- [x] Add `internal/adapter/AccessControlApiAdapter` implementing `AccessControlApi` (delegates only, per addendum 2026-06-01); remove `DefaultAccessControlService` monolith.

## 5. API

- [ ] Extend `AccessControlApi`.
- [ ] Add request/result/view records under `api/model`.
- [ ] Keep API models immutable and typed-ID based.
- [ ] Do not expose JPA entities.

## 6. Permission evaluator

- [ ] Ensure `TchPermissionEvaluator` uses `TchRequestContext`.
- [ ] Ensure it calls `AccessControlApi.checkPermissions`.
- [ ] Ensure missing tenant/user returns false.
- [ ] Ensure exceptions return false and log warn.
- [ ] Ensure no repository access inside evaluator.

## 7. Web endpoints

- [ ] Move admin endpoints to `/admin/access-control/**`.
- [ ] Add platform ops bootstrap endpoints.
- [ ] Return `ApiResponse<T>`.
- [ ] Use typed IDs in path variables.
- [ ] Remove `tenantId` query param from `/admin/**`.
- [ ] Add method permissions.
- [ ] Add audit on writes and ops bootstrap.

## 8. Tests

- [ ] Unit test JSON loader validation.
- [ ] Unit test bootstrap idempotency.
- [ ] Unit test effective permissions from role.
- [ ] Unit test user GRANT.
- [ ] Unit test user DENY winning over role grants.
- [ ] Integration test evaluator with context.
