# Request Context Usage

> **Status**: NORMATIVE  
> **Scope**: tchalanet-server (`common.context`, `common.security`, web, batch)  
> **Audience**: Backend developers, reviewers, ops  
> **Last reviewed**: 2026-05-05  
> **Related**:
>
> - `docs/conventions/api/routing_and_path.md`
> - `docs/conventions/security_permissions.md`
> - `docs/conventions/persistence/rls.md`
> - `docs/conventions/timezone.md`
> - `docs/conventions/batch/batch.md`

---

## 1. Purpose

`TchRequestContext` is the runtime context used by web, persistence, audit, batch, and selected
startup flows.

The project distinguishes three concepts:

1. context creation at a system boundary;
2. context enrichment inside an existing request;
3. temporary context switching for deliberate cross-tenant work.

Do not hide these concepts behind a generic “run as tenant” helper.

---

## 2. HTTP pipeline

HTTP context creation is owned by `TchContextFilter`.

Current filter order:

```text
BearerTokenAuthenticationFilter
  -> UserBootstrapFilter
  -> TchContextFilter
```

Rules:

- `UserBootstrapFilter` may enrich the request with `BOOTSTRAPPED_APP_USER_ID`.
- `UserBootstrapFilter` does not decide the effective tenant.
- `TchContextFilter` creates and binds the canonical HTTP `TchRequestContext`.
- `TchContextFilter` may attach an operational context parsed from request headers or trusted
  server-side inputs, but it does not validate terminal/outlet/session business state.
- `TchContextFilter` binds request attribute, `ThreadLocal`, and MDC.
- `TchContextFilter` clears `ThreadLocal` and MDC in `finally`.
- No separate `OperationalContextFilter` is allowed.

Over time, `UserBootstrapFilter` behavior may move behind an `ActorContextResolver`, but it remains
actor enrichment, not tenant resolution.

Canonical package ownership:

```text
common.context             -> neutral runtime request context
common.context.web         -> HTTP context filter, @CurrentContext resolver, web parsers
common.context.tenant      -> tenant lookup interface and tenant context resolver
common.context.system      -> system/startup context configuration
common.context.operational -> neutral operational context types, headers and parser results
```

`common.context.operational` must not import repositories, `CommandBus`, `QueryBus`, platform,
core, catalog or features. Operational context values are request inputs, not resource validity.

---

## 3. Tenant policy

| Scope                                        | Tenant behavior                                                         |
| -------------------------------------------- | ----------------------------------------------------------------------- |
| `PUBLIC`, anonymous                          | Bind configured default public tenant, currently `tchalanet`.           |
| `PUBLIC`, authenticated with tenant claim    | Prefer JWT tenant over default public tenant.                           |
| `PUBLIC`, authenticated without tenant claim | Fall back to default public tenant.                                     |
| `TENANT`                                     | Tenant required from authenticated context or allowed override.         |
| `ADMIN`                                      | Tenant required from authenticated context or allowed override.         |
| `PLATFORM`                                   | No tenant by default.                                                   |
| `SDR`                                        | No tenant by default unless policy explicitly resolves one.             |
| `SUPER_ADMIN` + override                     | Effective tenant comes from the allowed override and must be auditable. |

Client-provided tenant ids in request bodies are not trusted for tenant-scoped queries or writes.

Super-admin tenant override is per request. The preferred headers are:

```text
X-Tch-Tenant-Override
X-Tch-Override-Reason
```

The reason is required for sensitive tenant-scoped operations. The legacy `X-Tenant-Id` header may
exist during migration, but new code should use the `X-Tch-*` names.

---

## 4. Boundary Matrix

| Flow                        | Context producer                              | Rule                                                                                   |
| --------------------------- | --------------------------------------------- | -------------------------------------------------------------------------------------- |
| HTTP request                | `TchContextFilter`                            | Creates canonical request context from route/auth/tenant/actor metadata.               |
| User app bootstrap          | `UserBootstrapFilter`                         | Enriches request with `appUserId`; does not decide tenant.                             |
| Batch job                   | `BatchTchContextBinder`                       | Creates `TENANT` or `PLATFORM` context from explicit job parameters.                   |
| Scheduler tick              | Scheduler or launched batch                   | Prefer launching batch; direct work needs explicit scheduler context.                  |
| Startup tenant work         | `TchContextScope.runStartupTenant(...)`       | Used for tenant seed/init work such as default PageModel onboarding.                   |
| Startup platform work       | Future explicit platform/startup scope        | Used for technical bootstrap such as Keycloak sync if context is required.             |
| Temporary cross-tenant read | `TchContextScope.runWithTemporaryTenant(...)` | Allowed only for deliberate fallback/cross-tenant work; must restore previous context. |
| Event/listener              | Event payload or documented sync context      | Carry tenant/actor/correlation explicitly when async/retry/thread hop is possible.     |

`ThreadLocal` context does not automatically cross scheduler, batch, async, retry, or new-thread
boundaries.

---

## 5. PageModel Rules

PageModel has three distinct flows.

| Flow                                          | Context rule                                                       |
| --------------------------------------------- | ------------------------------------------------------------------ |
| Template seed (`PageModelTemplateSeedRunner`) | Catalog/global startup seed; no default tenant context by default. |
| Tenant seed (`PageModelOnboardingRunner`)     | Explicit startup tenant context for the default tenant.            |
| Runtime public PageModel                      | Use the already-bound HTTP context for the normal path.            |

Dynamic providers are Spring singleton beans. They are constructed at startup, but `load(...)` runs
during the HTTP request after the PageModel document is resolved.

Therefore:

- runtime document resolution must not clear or replace the HTTP context before providers run;
- fallback to the default tenant may use a temporary context switch only when intentionally reading
  a different tenant;
- providers must not repair or recreate global context.

---

## 6. Temporary Context Switching

Use `TchContextScope` for explicit non-HTTP or temporary context work.

Allowed examples:

- `runStartupTenant(...)` for startup tenant seeding.
- `runWithTemporaryTenant(...)` for deliberate fallback/cross-tenant reads.
- `runWithContext(...)` for low-level infra/test helpers with a fully constructed context.

The implementation must be stack-safe:

```java
var previous = TchContext.currentOrNull();
TchContext.set(temporary);
try {
  return work.get();
} finally {
  restore(previous);
}
```

Never clear blindly when a previous context existed.

---

## 7. Direct Access Rules

Direct `TchContext` access is limited to:

- HTTP context binding;
- batch/startup context binding;
- RLS datasource bridge;
- entity listeners/audit infrastructure;
- idempotency infrastructure;
- tests.

Application services and handlers should prefer `TchContextResolver`, `@CurrentContext`, or explicit
command/query fields.

Forbidden in ordinary application code:

- parsing JWT;
- reading `SecurityContext` directly;
- resolving tenant from request payload;
- manipulating MDC;
- calling `set_config`;
- changing `TchContext` to make downstream code work.

---

## 8. RLS

RLS reads the canonical current context through the datasource bridge.

Expected mapping:

- public default tenant -> tenant id + `PUBLIC` scope;
- tenant/admin -> tenant id + tenant/admin scope;
- platform without tenant -> no tenant + `PLATFORM` scope;
- super-admin override -> override tenant id + super-admin flag;
- batch tenant -> tenant id + tenant scope;
- batch platform -> no tenant + platform scope.

RLS is the last line of defense, not routing logic.

---

## 9. Events

Events should carry required identity and tenant facts when listeners may execute outside the
original request thread.

Minimum event metadata for async/retry-sensitive flows:

- tenant id when tenant-scoped;
- actor/app user id when actor-sensitive;
- request/correlation id when traceability matters.

---

## 10. PR checklist — Context

- [ ] HTTP requests use `TchContextFilter` as canonical context producer.
- [ ] `UserBootstrapFilter` or actor resolver enriches actor only; tenant policy stays separate.
- [ ] Public/tenant/admin/platform tenant policy follows the matrix above.
- [ ] Startup tenant work uses explicit startup tenant context.
- [ ] Temporary tenant switches restore the previous context.
- [ ] PageModel runtime providers keep the original HTTP context.
- [ ] Scheduler/batch/event flows do not rely on ambient HTTP ThreadLocal.
- [ ] RLS session variables map to the effective context.
