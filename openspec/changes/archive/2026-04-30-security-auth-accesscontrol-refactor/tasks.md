# Tasks — Security Auth & Access Control Refactor

## 1. Documentation

- [x] Add `tchalanet-docs/docs/01-architecture/flows/authentication-flow.md`.
- [x] Add `tchalanet-docs/docs/01-architecture/flows/permission-flow.md`.
- [x] Add `openspec/context/90-security-flows-guide.md`.

## 2. Authentication / identity bootstrap

- [x] Implement `UserBootstrapFilter`.
- [x] Register it after JWT authentication and before `TchContextFilter`.
- [x] Use `rawDataSource` for `app_user` bootstrap.
- [x] Upsert `app_user` by `keycloak_sub`.
- [x] Update safe identity fields and `last_login_at`.
- [x] Validate `app_user.status`.
- [ ] Add tests for first-login, repeated-login, suspended and pending users.

## 3. Context

- [x] Update `ApiScope` to include distinct `ADMIN` scope.
- [x] Update `ApiScopeResolver` for `/api/v1/admin/**`.
- [x] Ensure tenant is required for both `TENANT` and `ADMIN`.
- [x] Adjust `TchContextFilter` to rely on guaranteed `appUserId` for protected requests.
- [x] Enforce 403 for non-super-admin usage of sensitive override headers.
- [x] Prefer header-only override behavior.

## 4. RLS

- [x] Ensure `RlsAwareDataSource` sets `app.current_tenant`.
- [x] Ensure `RlsAwareDataSource` sets `app.deleted_visibility`.
- [x] Add `app.api_scope` variable.
- [x] Add `app.is_super_admin` variable.
- [x] Reset all RLS variables on connection close.
- [ ] Add integration tests for tenant isolation and deleted visibility.

## 5. Permissions

- [x] Standardize new controllers on `@PreAuthorize("hasPermission('...')")`.
- [x] Deprecate or remove `RequiresPermissionAspect`.
- [x] Update `TchPermissionEvaluator` to use `TchContextResolver`.
- [x] Update `TchPermissionEvaluator` to call `QueryBus`.
- [x] Ensure deny returns `false` from evaluator.
- [x] Keep role-to-permission mapping in `core.accesscontrol`.
- [ ] Add permission tests for `SUPER_ADMIN`, `TENANT_ADMIN`, `OPERATOR`, `CASHIER`.

## 6. SecurityConfig

- [x] Keep public endpoints permitted.
- [x] Keep tenant/admin endpoints authenticated.
- [x] Keep platform endpoints super-admin gated.
- [x] Remove fine-grained role business checks from request matchers.

## 7. Cleanup

- [ ] Remove any controller manual authorization checks.
- [x] Remove JWT parsing outside security/context filters.
- [ ] Ensure typed IDs outside persistence.
- [ ] Add PR checklist references to the new flow docs.
