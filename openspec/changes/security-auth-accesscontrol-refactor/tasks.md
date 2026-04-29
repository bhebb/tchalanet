# Tasks — Security Auth & Access Control Refactor

## 1. Documentation

- [ ] Add `tchalanet-docs/docs/01-architecture/flows/authentication-flow.md`.
- [ ] Add `tchalanet-docs/docs/01-architecture/flows/permission-flow.md`.
- [ ] Add `openspec/context/90-security-flows-guide.md`.

## 2. Authentication / identity bootstrap

- [ ] Implement `UserBootstrapFilter`.
- [ ] Register it after JWT authentication and before `TchContextFilter`.
- [ ] Use `rawDataSource` for `app_user` bootstrap.
- [ ] Upsert `app_user` by `keycloak_sub`.
- [ ] Update safe identity fields and `last_login_at`.
- [ ] Validate `app_user.status`.
- [ ] Add tests for first-login, repeated-login, suspended and pending users.

## 3. Context

- [ ] Update `ApiScope` to include distinct `ADMIN` scope.
- [ ] Update `ApiScopeResolver` for `/api/v1/admin/**`.
- [ ] Ensure tenant is required for both `TENANT` and `ADMIN`.
- [ ] Adjust `TchContextFilter` to rely on guaranteed `appUserId` for protected requests.
- [ ] Enforce 403 for non-super-admin usage of sensitive override headers.
- [ ] Prefer header-only override behavior.

## 4. RLS

- [ ] Ensure `RlsAwareDataSource` sets `app.current_tenant`.
- [ ] Ensure `RlsAwareDataSource` sets `app.deleted_visibility`.
- [ ] Add `app.api_scope` variable.
- [ ] Add `app.is_super_admin` variable.
- [ ] Reset all RLS variables on connection close.
- [ ] Add integration tests for tenant isolation and deleted visibility.

## 5. Permissions

- [ ] Standardize new controllers on `@PreAuthorize("hasPermission('...')")`.
- [ ] Deprecate or remove `RequiresPermissionAspect`.
- [ ] Update `TchPermissionEvaluator` to use `TchContextResolver`.
- [ ] Update `TchPermissionEvaluator` to call `QueryBus`.
- [ ] Ensure deny returns `false` from evaluator.
- [ ] Keep role-to-permission mapping in `core.accesscontrol`.
- [ ] Add permission tests for `SUPER_ADMIN`, `TENANT_ADMIN`, `OPERATOR`, `CASHIER`.

## 6. SecurityConfig

- [ ] Keep public endpoints permitted.
- [ ] Keep tenant/admin endpoints authenticated.
- [ ] Keep platform endpoints super-admin gated.
- [ ] Remove fine-grained role business checks from request matchers.

## 7. Cleanup

- [ ] Remove any controller manual authorization checks.
- [ ] Remove JWT parsing outside security/context filters.
- [ ] Ensure typed IDs outside persistence.
- [ ] Add PR checklist references to the new flow docs.
