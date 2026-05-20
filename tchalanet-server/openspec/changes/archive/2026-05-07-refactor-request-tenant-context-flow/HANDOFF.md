# Handoff: request tenant context flow

This note captures decisions from the design discussion so the work can resume cleanly after a pause.

## Current decision

Do not model everything as `TchContextRunner.runAsTenant(...)`.

Separate:

1. context creation at system boundaries;
2. context enrichment inside an existing request;
3. temporary context switching for deliberate cross-tenant work.

## Boundary matrix

| Flow                     | Current component                                             | Decision                                                                                              |
| ------------------------ | ------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------- |
| HTTP request             | `TchContextFilter`                                            | Owns canonical HTTP context creation.                                                                 |
| User app bootstrap       | `UserBootstrapFilter`                                         | Enriches request with `appUserId`; does not decide tenant.                                            |
| PageModel template seed  | `PageModelTemplateSeedRunner`                                 | Catalog/global startup work; no default tenant context by default.                                    |
| PageModel tenant seed    | `PageModelOnboardingRunner`                                   | Startup tenant work; use explicit startup tenant context.                                             |
| Runtime public PageModel | `PublicPageModelService` / `ResolveEffectivePageModelHandler` | Use already-bound HTTP context for normal path.                                                       |
| Dynamic providers        | `PageModelDynamicResolver` / `DrawsProvider`                  | Providers are Spring singletons; `load(...)` runs during HTTP request and must keep original context. |
| Keycloak bootstrap sync  | `KeycloakBootstrapSyncListener`                               | Startup/platform work, not HTTP tenant work.                                                          |
| Batch                    | `BatchTchContextBinder`                                       | Creates tenant/platform batch context from job parameters.                                            |
| Scheduler                | `@Scheduled` classes                                          | Prefer launching batch jobs; direct work needs explicit scheduler/platform/tenant context.            |
| Events/listeners         | Spring events                                                 | Carry tenant/actor/correlation explicitly if async/retry/thread hop is possible.                      |

## PageModel bug lesson

The bug happened because document resolution temporarily changed context and then cleared the HTTP
ThreadLocal. Dynamic providers ran after that and persistence saw no context.

Correct rule:

- normal PageModel runtime reads use the current HTTP context;
- fallback to the default tenant may use a temporary switch only when intentionally reading a
  different tenant;
- temporary switch must restore previous context before providers execute.

## First implementation slice

Recommended low-risk order:

1. Add characterization tests for restore-safe context switching and public PageModel provider context.
2. Introduce explicit context scope API names while keeping old behavior compatible.
3. Remove `TchContextRunner` from the normal PageModel current-tenant read path.
4. Replace startup PageModel seed usage with an explicit startup-tenant scope.
5. Extract context creation collaborators from `TchContextFilter`.

## Avoid

- Do not let application services repair global context.
- Do not rely on ThreadLocal crossing scheduler, async, batch, or retry boundaries.
- Do not bind the public default tenant to platform/startup work accidentally.
