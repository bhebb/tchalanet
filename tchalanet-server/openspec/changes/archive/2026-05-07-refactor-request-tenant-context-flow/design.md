# Design: Request tenant context flow

## 1. Current pain points

### 1.1 `TchContextFilter` is doing too much

`TchContextFilter` currently owns:

- scope classification;
- auth/JWT extraction;
- super-admin override validation;
- default public tenant selection;
- tenant bootstrap lookup;
- app user bootstrap enforcement;
- request attribute binding;
- ThreadLocal binding;
- MDC binding.

This makes small changes risky because policy and plumbing are interleaved.

### 1.2 Context data is duplicated

`TchRequestContext` carries both UUID/code fields and typed tenant fields:

- `originalTenantCode`
- `originalTenantUuid`
- `effectiveTenantCode`
- `effectiveTenantUuid`
- `tenantId`
- `tenantZoneId`
- `tenantCurrency`

The duplication makes it unclear which field is canonical. The current practical canonical value is
`tenantIdSafe()`, but the name reads like a fallback rather than the intended effective tenant.

### 1.3 ThreadLocal is too directly accessible

Many layers can call `TchContext.currentOrNull()` or `TchContext.get()` directly. That makes it hard
to audit context usage and bypasses the request attribute fallback used by `@CurrentContext`.

### 1.4 Temporary context switching is necessary but dangerous

Some flows need a temporary tenant context:

- startup seeding;
- batch work;
- fallback page model lookup;
- deliberate cross-tenant/platform reads.

The safe primitive is not “set then clear”; it is “set then restore previous”.

### 1.5 Context creation is mixed with context enrichment

The current request pipeline has two filters with different jobs:

- `UserBootstrapFilter` looks up the persisted `app_user` from the authenticated Keycloak subject and
  writes `BOOTSTRAPPED_APP_USER_ID` onto the request.
- `TchContextFilter` builds the canonical `TchRequestContext`, then reads that request attribute to
  attach `appUserId`.

That split can remain during migration, but the design language must stay precise:

- user bootstrap enriches a request with actor data;
- tenant context creation decides scope, tenant, actor, request metadata, and binding.

### 1.6 PageModel has three different flows

PageModel currently has separate flows that must not be collapsed into one generic runner:

| Flow                                | Boundary                                               | Tenant context                                              |
| ----------------------------------- | ------------------------------------------------------ | ----------------------------------------------------------- |
| PageModel template seed             | `ApplicationRunner` in `catalog.pagemodeltemplate`     | No tenant by default; global catalog seed.                  |
| PageModel tenant seed               | `ApplicationRunner` in `features.pagemodel.onboarding` | Explicit startup tenant context for the default tenant.     |
| Runtime public PageModel resolution | HTTP request                                           | Use the already-bound HTTP context from `TchContextFilter`. |

Runtime providers such as `DrawsProvider` are Spring singleton beans constructed at startup. Their
`load(...)` method runs during the HTTP request after the PageModel document is resolved. Therefore
PageModel document resolution must not accidentally clear or replace the HTTP context before dynamic
providers execute.

## 2. Recommended architecture

### 2.1 Keep one request context, but split construction

Do not replace `TchRequestContext` in the first implementation step. Instead, introduce smaller
collaborators that produce it.

Recommended classes:

| Class                                      | Responsibility                                                                     |
| ------------------------------------------ | ---------------------------------------------------------------------------------- |
| `ApiScopeResolver`                         | Keep existing path -> scope classification.                                        |
| `AuthContextExtractor`                     | Extract keycloak user, tenant claim, roles, custom roles from Spring Security/JWT. |
| `TenantContextResolver`                    | Resolve effective tenant policy and bootstrap info.                                |
| `ActorContextResolver`                     | Resolve app user/bootstrap requirement.                                            |
| `TchRequestContextFactory`                 | Assemble `TchRequestContext` from request metadata, auth, tenant, actor, scope.    |
| `TchContextHolder` or evolved `TchContext` | Bind/current/restore/clear ThreadLocal context.                                    |
| `TchContextBinder`                         | Bind request attribute + ThreadLocal + MDC for HTTP requests.                      |

This keeps the servlet filter thin:

```java
var scope = apiScopeResolver.resolve(req);
var auth = authExtractor.extract(req, scope);
var tenant = tenantResolver.resolve(req, scope, auth);
var actor = actorResolver.resolve(req, scope, auth, tenant);
var ctx = contextFactory.create(req, scope, auth, tenant, actor);
contextBinder.bind(req, ctx);
try {
  chain.doFilter(req, res);
} finally {
  contextBinder.clear();
}
```

### 2.2 Context producers and enrichment

The implementation should model context boundaries explicitly.

| Source           | Producer                                                      | Expected context                                                                                  |
| ---------------- | ------------------------------------------------------------- | ------------------------------------------------------------------------------------------------- |
| HTTP request     | `TchContextFilter` via factory/resolvers                      | `PUBLIC`, `TENANT`, `ADMIN`, `PLATFORM`, or `SDR` according to route/auth policy.                 |
| User bootstrap   | `UserBootstrapFilter` initially, later `ActorContextResolver` | Enriches actor/app-user data; does not decide tenant.                                             |
| Batch job        | `BatchTchContextBinder`                                       | `TENANT` when `tenant_id` is present; `PLATFORM` when global.                                     |
| Scheduler tick   | Scheduler itself or launched batch                            | Prefer launching explicit batch jobs; direct DB/service work needs explicit scheduler context.    |
| Startup runner   | Startup context scope/factory                                 | `STARTUP_TENANT` for tenant seeds; `STARTUP_PLATFORM` for platform/bootstrap work.                |
| Event/listener   | Event payload or synchronous ambient context                  | Required tenant/actor/correlation data should be carried explicitly when async/retry is possible. |
| Async/thread hop | Task decorator or explicit reconstruction                     | Do not assume ThreadLocal propagation across threads.                                             |

Avoid using one generic `runAsTenant` helper as the mental model for all these cases.

### 2.3 Preferred tenant policy

| Request                                      | Tenant result                                              |
| -------------------------------------------- | ---------------------------------------------------------- |
| `PUBLIC`, anonymous                          | default tenant from config, expected `tchalanet`.          |
| `PUBLIC`, authenticated with tenant claim    | JWT tenant claim.                                          |
| `PUBLIC`, authenticated without tenant claim | default tenant.                                            |
| `TENANT`                                     | tenant required from JWT or allowed override.              |
| `ADMIN`                                      | tenant required from JWT or allowed override.              |
| `PLATFORM`                                   | no tenant by default.                                      |
| `SDR`                                        | no tenant by default unless explicitly resolved by policy. |
| super-admin + `X-Tenant-Id`                  | explicit effective tenant override.                        |

Rationale:

- Public pages still need tenant-scoped data such as draw channels, labels, theme, timezone, and
  PageModel runtime configuration.
- Authenticated public calls should be able to show tenant-specific public data when the user is
  already scoped to a tenant.
- Platform calls should never become tenant-scoped just because a default tenant exists.

### 2.4 Better solution than only refactoring the filter

The better long-term solution is to treat tenant context as an explicit runtime capability:

```text
TenantContextInfo
  tenantId
  code
  timezone
  currency
  source = DEFAULT_PUBLIC | JWT_CLAIM | SUPER_ADMIN_OVERRIDE | BATCH_PARAM | STARTUP_TENANT | STARTUP_PLATFORM | SCHEDULER | FALLBACK_DEFAULT_TENANT
```

Then `TchRequestContext` can expose:

- `effectiveTenantIdOrNull()`
- `requireTenantId()`
- `tenantSource()`
- `isTenantScoped()`
- `isPlatformScope()`

This reduces ambiguity and helps logs explain _why_ a tenant was selected.

Do this after the first extraction phase to avoid a risky big-bang rewrite.

### 2.5 Runtime PageModel rule

Runtime PageModel resolution should follow this rule:

1. Use the current HTTP context for the normal read path.
2. If the current tenant is authenticated and different from the default public tenant, a fallback to
   the default tenant MAY use an explicit temporary context switch.
3. The temporary switch must restore the previous HTTP context before dynamic providers are resolved.
4. Providers must not create or repair context; they run under the context created by the boundary.

This avoids hidden side effects when a service is only trying to resolve a PageModel document.

## 3. Context holder rules

### 3.1 Allowed direct access

Direct `TchContext`/holder access should be limited to:

- HTTP context binding;
- batch/startup context binding;
- RLS datasource;
- entity listeners/audit infrastructure;
- test helpers.

Application services and handlers should prefer `TchContextResolver`.

### 3.2 Temporary context switching

The primitive must be stack-safe:

```java
var previous = currentOrNull();
set(temporary);
try {
  return work.get();
} finally {
  restore(previous);
}
```

Never clear blindly if a previous context existed.

The API name should make the danger visible. Prefer names such as:

- `TchContextScope.runWithTemporaryTenant(...)`
- `TchContextScope.runStartupTenant(...)`
- `TchContextScope.runPlatform(...)`

Avoid a broad `TchContextRunner.runAsTenant(...)` API in application code.

## 4. RLS alignment

`RlsAwareDataSource` should continue to read the canonical current context.

Expected outcomes:

- public default tenant -> `app.current_tenant = tchalanet tenant uuid`, `app.api_scope = public`;
- tenant/admin -> tenant uuid, scope tenant/admin;
- platform without tenant -> empty tenant, scope platform;
- super-admin override -> override tenant uuid, `app.is_super_admin = true`;
- batch tenant -> tenant uuid, scope tenant;
- batch platform -> empty tenant, scope platform.

## 5. Test strategy

### 5.1 Unit tests

- `ApiScopeResolverTest`
- `AuthContextExtractorTest`
- `TenantContextResolverTest`
- `TchContextRunnerTest`
- `TchRequestContextFactoryTest`

### 5.2 Servlet/filter tests

Use MockMvc or filter-level tests for:

- public anonymous page model request;
- public authenticated tenant request;
- tenant route without tenant claim;
- tenant route with tenant claim;
- platform route with no tenant;
- super-admin override allowed;
- non-super-admin override rejected.

### 5.3 RLS smoke tests

Use focused datasource tests to assert the session variables applied from context. Do not test every
domain query here; test the context-to-RLS bridge once.

## 6. Migration plan

1. Add tests around current policy before moving code.
2. Extract auth and tenant resolution with no behavior change except documented fixes.
3. Introduce restore-safe context holder methods.
4. Update callers to use `TchContextResolver`.
5. Add `tenantSource`/explicit effective tenant naming.
6. Remove compatibility methods only after call sites are migrated.

## 7. Bootstrap decisions captured

- `PageModelTemplateSeedRunner` is a catalog/global startup seed. It should not require tenant
  context unless persistence/audit explicitly demands a platform/startup context.
- `PageModelOnboardingRunner` is tenant-scoped startup work. It should use an explicit
  startup-tenant context for the default tenant.
- `KeycloakBootstrapSyncListener` is startup/platform work. If context is required, it should use a
  startup-platform/system actor context, not a default public tenant context.
- `UserBootstrapFilter` is HTTP actor enrichment. It should be folded behind an
  `ActorContextResolver` concept over time, but it should not decide effective tenant.
- Domain/application events must carry tenant/actor/correlation identifiers when listeners may run
  async, after retry, or outside the original request thread.
