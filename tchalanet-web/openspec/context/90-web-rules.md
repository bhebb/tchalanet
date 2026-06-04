# OpenSpec — Web Rules (90)

> Scope: Tchalanet Web / Angular / Nx  
> Status: DRAFT NORMATIVE  
> Purpose: structural and architectural rules, not functional requirements.

## 1. Canonical families

Tchalanet Web uses five canonical families:

```text
core
features
data-access
ui
shared
```

No additional top-level family should be introduced without explicit documentation.

## 2. Core

`core` contains global application infrastructure:

- auth/session/permissions
- config/runtime config
- http/api infrastructure
- i18n runtime
- shell/navigation containers
- guards/interceptors/error handling

`core` MUST NOT contain business feature pages.

## 3. Features

Features are UI screens, routes and flows.

Canonical path:

```text
features/<scope>/<feature>
```

Allowed scopes:

```text
public
tenant
admin
platform
```

Features MAY remain under `apps/tch-web/src/app/features` until they become shared or strategic.

## 4. Data access

`data-access` owns backend-facing contracts:

- API services
- API models
- reusable API state/cache

`data-access` MUST NOT depend on features.

## 5. UI

`ui` owns presentational components:

- design-system components
- layout components
- visual page renderers
- visual widgets

`ui` MUST NOT depend on data-access or features.

## 6. Shared

`shared` contains generic utilities only:

- utils
- validators
- testing helpers
- low-level generic types

`shared` MUST NOT depend on core, features, data-access or ui.

## 7. Header/footer/sidebar split

Visual components live in `ui/layout`.
Connected containers live in `core/shell`.

## 8. PageModel split

> Normative detail for the PageModel runtime, widget registry, theme token mapping, and
> feature/entitlement gating lives in the living convention docs:
> [`docs/conventions/pagemodel.md`](../../docs/conventions/pagemodel.md),
> [`theme.md`](../../docs/conventions/theme.md), [`settings.md`](../../docs/conventions/settings.md).
> Those docs track the **real backend `PageModelDoc` contract** and are updated in the same commit as
> any code that changes a rule.

PageModel is not a top-level family.

- data/API: `data-access/page-model`
- visual renderer: `ui/page-renderer`
- widgets: `ui/widgets`
- public runtime page: `features/public/dynamic-page`
- admin editor: `features/platform/page-models`

## 9. State placement

- Component-local state: component signal.
- Screen state: feature store.
- Reusable API state: data-access state.
- Global app state: core.

## 10. Nx enforcement

When code is in Nx libs, projects SHOULD be tagged with:

```text
type:<core|feature|data-access|ui|shared>
scope:<public|tenant|admin|platform|shared|core|domain-name>
```

Module boundaries SHOULD enforce:

```text
ui -> shared only
data-access -> core/http + shared
features -> data-access + ui + core + shared
shared -> no internal project dependencies
```
