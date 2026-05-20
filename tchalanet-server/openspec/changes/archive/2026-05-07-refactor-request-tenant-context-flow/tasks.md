# Tasks: Refactor request tenant context flow

## 1. Characterization tests

- [ ] Add tests for current public anonymous request behavior.
- [ ] Add tests for public authenticated request with tenant claim.
- [ ] Add tests for tenant/admin route without tenant.
- [ ] Add tests for tenant/admin route with tenant.
- [ ] Add tests for platform route without tenant.
- [ ] Add tests for super-admin tenant override.
- [ ] Add tests for non-super-admin override rejection.
- [x] Add tests proving temporary context switching restores previous context.
- [x] Add tests proving PageModel runtime providers run after document resolution with the original HTTP context.
- [ ] Add tests for startup tenant context used by PageModel tenant seed.
- [ ] Add tests documenting that catalog PageModel template seed does not require tenant context.

## 2. Split request context construction

- [x] Extract `AuthContextExtractor` from `TchContextFilter`.
- [x] Extract `TenantContextResolver`.
- [x] Extract `ActorContextResolver` for app user bootstrap enforcement.
- [x] Extract `TchRequestContextFactory`.
- [x] Introduce an explicit `TchContextScope`/binder API for temporary, startup, batch, and platform scopes.
- [x] Keep `TchContextFilter` as orchestration only.
- [ ] Keep existing public/tenant/admin/platform behavior unless explicitly changed by this spec.
- [x] Keep `UserBootstrapFilter` as request enrichment initially, but align it with the future `ActorContextResolver` boundary.

## 3. Clarify tenant policy

- [x] Document policy matrix in backend conventions.
- [ ] Ensure public anonymous requests use default tenant `tchalanet`.
- [ ] Ensure public authenticated requests prefer JWT tenant when present.
- [ ] Ensure tenant/admin requests require tenant.
- [ ] Ensure platform requests do not bind default tenant implicitly.
- [ ] Ensure super-admin override is explicit and audited/logged.
- [ ] Decide whether missing default public tenant fails startup or fails public requests.

## 4. Context holder and resolver cleanup

- [x] Add/keep restore-safe temporary context switching.
- [x] Replace generic `TchContextRunner` application use with explicit context scope methods.
- [ ] Rename or wrap `TchContext` as a holder concept if practical.
- [x] Add `currentOrThrow()` and `withContext(...)` helper methods if missing.
- [ ] Reduce direct `TchContext.get()` and `TchContext.currentOrNull()` usage in application code.
- [ ] Prefer `TchContextResolver` in services/handlers.
- [ ] Keep direct holder access only in infra/context/batch/RLS/test helpers.

## 5. `TchRequestContext` naming cleanup

- [x] Add clearer accessors:
  - [x] `effectiveTenantIdOrNull()`
  - [x] `effectiveTenantIdRequired()`
  - [x] `hasTenant()`
  - [x] `isPlatformScope()`
- [ ] Add tenant source metadata if accepted:
  - [ ] `DEFAULT_PUBLIC`
  - [ ] `JWT_CLAIM`
  - [ ] `SUPER_ADMIN_OVERRIDE`
  - [ ] `BATCH_PARAM`
  - [ ] `STARTUP_TENANT`
  - [ ] `STARTUP_PLATFORM`
  - [ ] `SCHEDULER`
  - [ ] `FALLBACK_DEFAULT_TENANT`
- [ ] Deprecate ambiguous accessors only after callers are migrated.
- [ ] Avoid removing compatibility methods in the first implementation pass.

## 6. RLS bridge verification

- [ ] Add focused tests for RLS session variables from context.
- [ ] Verify public default tenant applies `app.current_tenant`.
- [ ] Verify platform no-tenant resets `app.current_tenant`.
- [ ] Verify super-admin override sets tenant and `app.is_super_admin`.
- [ ] Verify batch tenant/platform contexts map correctly to RLS settings.

## 7. Batch/startup alignment

- [ ] Align `BatchTchContextBinder` with the same context factory where feasible.
- [ ] Keep batch tenant bootstrap through trusted tenant catalog/provider.
- [ ] Ensure cross-tenant batch work explicitly iterates tenant contexts.
- [ ] Ensure batch platform jobs do not accidentally write tenant-scoped rows.

## 8. Documentation

- [x] Update `docs/conventions/api/request_context_usage.md` or create it if needed.
- [ ] Update `docs/conventions/persistence/rls.md` with the final context/RLS matrix.
- [ ] Update `docs/PLAYBOOK.md` with concise context rules.
- [ ] Update `tchalanet-server/AGENTS.md` only if a new canonical context doc is added.
- [x] Document bootstrap context rules for PageModel template seed, PageModel tenant seed, Keycloak bootstrap, scheduler, and events.

## 9. Validation

- [x] Run focused context/security tests.
- [ ] Run affected RLS tests.
- [ ] Run public PageModel smoke test.
- [ ] Run `./mvnw test` or document any unrelated test blockers.
- [x] Run `openspec validate refactor-request-tenant-context-flow --strict`.
