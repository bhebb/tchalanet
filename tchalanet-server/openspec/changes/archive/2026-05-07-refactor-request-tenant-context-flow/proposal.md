# Change: Refactor request tenant context flow

## Why

The backend request context currently works, but the flow is too implicit and too easy to break.
The recent public PageModel issue exposed the problem:

- `TchContextFilter` correctly bound the public default tenant context.
- `ResolveEffectivePageModelHandler` temporarily switched tenant via `TchContextRunner`.
- `TchContextRunner` cleared the ThreadLocal instead of restoring the previous HTTP context.
- Later widget providers and persistence adapters saw `TchContext.currentOrNull() == null`.

The immediate bug was fixed, but the larger design still mixes several responsibilities:

- API scope detection.
- JWT/auth extraction.
- default public tenant resolution.
- super-admin tenant override.
- tenant bootstrap lookup.
- application user bootstrap enforcement.
- ThreadLocal binding.
- MDC binding.
- RLS session variable application through the datasource.
- batch/startup tenant context creation.

This makes it hard to reason about the intended tenant behavior:

- anonymous public traffic should use the default public tenant, currently `tchalanet`;
- authenticated users should use their own tenant when present;
- tenant/admin endpoints should require a tenant;
- platform endpoints should not imply a tenant unless explicitly requested;
- batch/platform/public multi-tenant flows should be deliberate and documented.

## What

Refactor the context/authentication pipeline into explicit, testable steps.

Recommended target flow:

```text
HttpServletRequest
  -> ApiScopeResolver
  -> AuthContextExtractor
  -> TenantContextResolver
  -> ActorContextResolver
  -> TchRequestContextFactory
  -> TchContextHolder / request attribute / MDC
  -> RLS-aware datasource reads current context
```

This change MUST separate three concepts that are currently easy to confuse:

- context creation at system boundaries;
- context enrichment during an existing request;
- temporary context switching for deliberate cross-tenant work.

The change should preserve existing runtime behavior where correct, while making the intended policy explicit:

- `PUBLIC` anonymous request -> default tenant `tchalanet`.
- `PUBLIC` authenticated request with tenant claim -> authenticated tenant.
- `PUBLIC` authenticated request without tenant claim -> default tenant `tchalanet`.
- `TENANT` / `ADMIN` request -> tenant is required.
- `PLATFORM` request -> no tenant by default.
- `SUPER_ADMIN` with override header -> explicit effective tenant.
- temporary context switches must restore the previous context.
- PageModel runtime resolution must use the already-bound HTTP context for the normal path.
- startup PageModel seeding must use an explicit startup tenant context.
- events/listeners must not depend on an ambient ThreadLocal unless they are documented synchronous
  in-thread listeners.

## Scope

### In Scope

- Refactor common request context classes.
- Split `TchContextFilter` responsibilities.
- Define context creation rules for HTTP, batch, scheduler, startup, and event/listener flows.
- Clarify the difference between `UserBootstrapFilter` enrichment and canonical context creation.
- Clarify PageModel template seed versus tenant PageModel seed versus runtime PageModel resolution.
- Clarify and test tenant resolution policy.
- Clarify `TchContext`, `TchContextResolver`, and temporary context switching.
- Keep RLS behavior aligned with the effective tenant context.
- Update backend conventions/docs for context usage.

### Out of Scope

- Changing Keycloak realm structure.
- Changing JWT claim names unless required by tests.
- Changing tenant membership/domain rules.
- Changing frontend/mobile authentication flows.
- Rewriting RLS SQL policies unless a specific bug is found.
- Introducing multi-tenant public routing beyond the current default tenant and authenticated tenant behavior.

## Impact

| Component                | Change Required | Details                                                                                           |
| ------------------------ | --------------- | ------------------------------------------------------------------------------------------------- |
| `common/security`        | Yes             | Split `TchContextFilter` into smaller collaborators.                                              |
| `common/context`         | Yes             | Clarify holder/resolver/factory roles and temporary context restore semantics.                    |
| `common/persistence/rls` | Maybe           | Verify it reads only canonical context and handles public default tenant.                         |
| Batch context            | Yes             | Align batch/startup context creation with the canonical factory or tenant bootstrap resolver.     |
| Startup bootstrap        | Yes             | PageModel tenant seed and Keycloak bootstrap need explicit startup/platform context decisions.    |
| Events/listeners         | Maybe           | Ensure required tenant/actor/correlation data is carried explicitly when async/retry is possible. |
| API controllers          | Maybe           | Prefer `@CurrentContext`; reduce direct `TchContext` reads where possible.                        |
| Database                 | No by default   | No schema change expected.                                                                        |
| UI                       | No              | Server-only change.                                                                               |

## Success Criteria

- [ ] Public anonymous `/api/v1/public/**` binds default tenant `tchalanet`.
- [ ] Public authenticated request with tenant claim binds the claimed tenant.
- [ ] Tenant/admin requests without tenant are rejected.
- [ ] Platform requests do not accidentally bind the default tenant.
- [ ] Super-admin override changes effective tenant only when allowed.
- [ ] Temporary tenant context switching restores the previous context.
- [ ] RLS session variables match the effective context for HTTP and batch flows.
- [ ] PageModel runtime providers keep the HTTP context after PageModel document resolution.
- [ ] PageModel template seed runs without tenant context unless persistence requires one.
- [ ] PageModel tenant seed runs under an explicit startup tenant context.
- [ ] Keycloak bootstrap is documented as startup/platform work, not HTTP tenant work.
- [ ] Services stop using raw `TchContext.get()` except in low-level context/infra code.
- [ ] Context flow is covered by focused unit/integration tests.

## Risks

| Risk                                                 | Probability | Impact | Mitigation                                                                       |
| ---------------------------------------------------- | ----------- | ------ | -------------------------------------------------------------------------------- |
| Public endpoints lose tenant context                 | Medium      | High   | Add tests for public PageModel/draw widgets and RLS variables.                   |
| Platform endpoints accidentally become tenant-scoped | Medium      | High   | Explicit scope matrix and tests.                                                 |
| Batch jobs lose context behavior                     | Medium      | High   | Align `BatchTchContextBinder` with factory/resolver tests.                       |
| Refactor changes auth behavior silently              | Medium      | High   | Golden tests for anonymous/authenticated/super-admin flows before code movement. |
| Too much rename churn                                | Medium      | Medium | Refactor in small phases; keep compatibility methods temporarily.                |

## Open Questions

- Should authenticated `PUBLIC` requests always prefer JWT tenant over default tenant, even for marketing/public pages?
- Should the default public tenant code be hard-required at startup, or should public tenant resolution fail lazily per request?
- Should platform batch jobs use `SYSTEM` + `PLATFORM`, or should each cross-tenant batch explicitly iterate tenant-scoped child contexts?
- Should `TchRequestContext` be flattened for now, or decomposed into nested `TenantContext`, `ActorContext`, `RequestMeta`, and `AccessContext` records?
