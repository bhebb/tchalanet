# Web Placement Guide — Tchalanet

> **Status**: DRAFT v0.2
> **Scope**: frontend placement rules for `tchalanet-web` / `tch-portal`
> **Related**: `../ARCHITECTURE.md`, `naming.md`, `state-management.md`, `pagemodel.md`

---

## 1. Rule

Place code by **ownership** and **lifetime**, not by generic folder names.

```text
Contract / HTTP client     -> libs/api
Reusable UI component      -> libs/ui/components
Runtime theme              -> libs/ui/theme
Compile-time SCSS          -> libs/ui/styles
Runtime settings / flags   -> libs/shared-config
Routed page                -> app feature now, future web lib
Feature state              -> next to the feature page
Cross-surface state        -> owning runtime capability, extracted only when stable
```

Do not create a lib just because a target architecture mentions it.
Create a lib only when the boundary is real and the slice moves code into it.

---

## 2. Current active structure

```text
tchalanet-web/
├── apps/
│   └── tch-portal/
│       └── src/app/
│           ├── core/
│           ├── features/
│           └── shared/
└── libs/
    ├── api/
    │   └── src/lib/
    │       ├── contracts/
    │       └── http/
    ├── page-model/
    ├── shared-config/
    ├── web/
    ├── widgets/
    └── ui/
        ├── components/
        ├── styles/
        └── theme/
```

`page-model`, `widgets`, and the reusable part of `web` are extracted. App-coupled shell
orchestration, `shared-auth`, and `shared-i18n` continue to move slice by slice.

---

## 3. Placement table

| Element                                     | Placement                                                                                |
| ------------------------------------------- | ---------------------------------------------------------------------------------------- |
| `ApiResponse`, `ProblemDetail`, `ApiNotice` | `libs/api/src/lib/contracts`                                                             |
| `ActionItem`, `NavigationDestination`       | `libs/api/src/lib/contracts`                                                             |
| HTTP interceptors                           | `libs/api/src/lib/http`, auth-specific interceptor may live near auth during migration   |
| Generic API helpers                         | `libs/api/src/lib/http`                                                                  |
| Runtime config                              | `libs/shared-config`                                                                     |
| Feature flags                               | `libs/shared-config`                                                                     |
| Theme runtime / active mode                 | `libs/ui/theme`                                                                          |
| Theme API / theme preset registry           | `libs/ui/theme` or `libs/api/http` + `ui/theme` depending ownership                      |
| Theme SCSS generation                       | `libs/ui/theme/src/scss`                                                                 |
| Shared SCSS mixins/functions/breakpoints    | `libs/ui/styles`                                                                         |
| Material global overrides                   | `libs/ui/styles` unless directly tied to M3 preset generation                            |
| Button / Card / Badge / Loading / Errors    | `libs/ui/components`                                                                     |
| Brand / Nav / OverlayNav / SidebarNav       | `libs/ui/components`                                                                     |
| Public shell                                | reusable presentation in `libs/web`; app-specific orchestration in the app               |
| Private shell                               | app feature now, future `libs/web`                                                       |
| Public home page                            | `apps/tch-portal/src/app/features/public/home`                                           |
| Cashier dashboard page                      | `apps/tch-portal/src/app/features/cashier/dashboard`                                     |
| Tenant admin dashboard page                 | `apps/tch-portal/src/app/features/admin/dashboard`                                       |
| Platform dashboard page                     | `apps/tch-portal/src/app/features/platform/dashboard`                                    |
| PageModel runtime API contract              | `libs/page-model`                                                                        |
| PageModel API client                        | `libs/page-model`                                                                        |
| PageModel renderer                          | `libs/page-model`                                                                        |
| Widget registry / concrete widgets          | `libs/widgets`                                                                           |
| PageModel editor screen                     | `apps/tch-portal/src/app/features/platform/page-models`                                  |
| Auth session store                          | app `core/auth` now, future `shared-auth`                                                |
| Auth guards                                 | app `core/auth` now, future `shared-auth`                                                |
| Login page                                  | `apps/tch-portal/src/app/features/auth/login` or `features/public/login` if public route |
| Locale store                                | app `core/i18n` now, future `shared-i18n`                                                |
| Language switcher UI                        | `libs/ui/components`                                                                     |
| Backend i18n API contract/client            | `libs/api`                                                                               |
| Platform i18n admin screen                  | `apps/tch-portal/src/app/features/platform/i18n-overrides`                               |
| Test helpers                                | only create `testing` area when reused by multiple tests                                 |

---

## 4. Operational context placement

Operational context is a runtime capability used by cashier/POS flows.

Examples:

```text
current tenant
current outlet
current terminal
current sales session
seller/cashier identity
business date
readiness/status
```

### V1 placement

Contracts and API client:

```text
libs/api/src/lib/contracts/operational-context.ts
libs/api/src/lib/http/operational-context-api.service.ts
```

Cashier page/state:

```text
apps/tch-portal/src/app/features/cashier/operational-context/
  operational-context.routes.ts
  operational-context.page.ts
  operational-context.store.ts
```

If only the cashier dashboard uses it, keep the store local to the cashier feature.

If several cashier pages need it, place the store at the cashier feature boundary:

```text
apps/tch-portal/src/app/features/cashier/
  cashier-operational-context.store.ts
```

If admin, cashier, sale, payout, terminal binding, and shell all need the same runtime state, extract later to an owning runtime area.

Possible future placement:

```text
apps/tch-portal/src/app/core/operational-context
```

or, when stable enough:

```text
libs/shared-operational-context
```

Do not create `shared-operational-context` until there is a real multi-surface need.

### Route wiring

Routes connect pages to stores/providers.

Example:

```ts
export const cashierRoutes: Routes = [
  {
    path: '',
    providers: [CashierOperationalContextStore],
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./dashboard/cashier-dashboard.page').then(m => m.CashierDashboardPage),
      },
      {
        path: 'sale',
        loadComponent: () =>
          import('./sale/cashier-sale.page').then(m => m.CashierSalePage),
      },
    ],
  },
];
```

Rule:

```text
API client lives in libs/api.
Store lives next to the owning feature or runtime capability.
Route provides the store when its lifetime should follow the route.
Page consumes the store.
UI components receive inputs only.
```

---

## 5. Header and shell placement

Do not split into “visual header” and “connected header” using old `core/shell` terminology.

Preferred model:

```text
PublicShell
  -> PublicHeader
  -> main
  -> PublicFooter

PrivateShell
  -> PrivateTopAppBar
  -> SidebarNav
  -> main
```

App-owned orchestration:

```text
apps/tch-portal/src/app/features/public/shell
apps/tch-portal/src/app/features/dashboard/shell
```

Reusable public shell presentation:

```text
libs/web/src/lib/public-shell
```

Shared visual parts:

```text
libs/ui/components/brand
libs/ui/components/nav
libs/ui/components/overlay-nav
libs/ui/components/sidebar-nav
libs/ui/components/lang-switcher
libs/ui/components/lang-theme-group
```

---

## 6. Footer placement

Public footer rendering belongs to the public shell area.

Reusable visual primitives can live in `ui/components`, but the footer composition itself is shell-specific.

Current reusable footer:

```text
libs/web/src/lib/public-shell/public-footer.ts
```

The footer consumes a resolved runtime shell fragment:

```text
shell.footer.brand
shell.footer.descriptionKey
shell.footer.columns
shell.footer.social
```

The footer must not resolve `fileKey`.

---

## 7. PageModel placement

Current placement:

```text
libs/page-model
```

Responsibilities:

```text
PageModel runtime contract helpers
layout renderer
rows/columns renderer
WidgetHost abstraction
unsupported-widget fallback
invalid-widget fallback
```

Not responsibilities:

```text
PublicHeader
PublicFooter
PrivateShell
SidebarNav
Theme runtime
i18n bootstrap
backend fileKey/jsonFile resolution
concrete widgets
```

---

## 8. Widgets placement

Current:

```text
libs/widgets
```

Rules:

```text
Widget = props + dynamic data + local error
Widget does not call HTTP directly
Widget does not receive full PageRuntimeResponse
Widget type maps directly from backend type string
```

---

## 9. i18n placement

Runtime locale state:

```text
apps/tch-portal/src/app/core/i18n
```

Future:

```text
libs/shared-i18n
```

Language switcher UI:

```text
libs/ui/components/lang-switcher
```

Backend i18n contracts/client:

```text
libs/api
```

Platform i18n admin screen:

```text
apps/tch-portal/src/app/features/platform/i18n-overrides
```

---

## 10. Config placement

Runtime settings and feature flags:

```text
libs/shared-config
```

Tenant config from backend:

```text
libs/api/contracts
libs/api/http
```

Feature-specific config:

```text
apps/tch-portal/src/app/features/<surface>/<feature>
```

Do not put tenant config state in `ui`.

---

## 11. Auth placement

During migration:

```text
apps/tch-portal/src/app/core/auth
```

Future:

```text
libs/shared-auth
```

Auth owns:

```text
AuthSessionStore
login/logout orchestration
guards
permission helpers
token/session storage
```

Login page:

```text
apps/tch-portal/src/app/features/auth/login
```

or if deliberately public-routed:

```text
apps/tch-portal/src/app/features/public/login
```

---

## 12. API/cache reusable state

Do not use the old mandatory pattern:

```text
data-access/<domain>/model
data-access/<domain>/api
data-access/<domain>/state
```

as a default.

Use active ownership instead.

If a cache/API state has one consumer:

```text
features/<surface>/<feature>/<feature>.store.ts
```

If it has multiple consumers and a stable API boundary:

```text
libs/api/src/lib/cache/<domain>-cache.store.ts
```

or an owning runtime lib if it is not generic API state:

```text
libs/ui/theme
libs/shared-config
future shared-auth
future shared-i18n
```

---

## 13. Feature examples

### Payouts

Contracts/API:

```text
libs/api/src/lib/contracts/payout.ts
libs/api/src/lib/http/payout-api.service.ts
```

Page/state:

```text
apps/tch-portal/src/app/features/admin/payouts/
  payouts.routes.ts
  payouts.page.ts
  payouts.store.ts
```

Reusable visual card, only if generic:

```text
libs/ui/components/status-badge
```

Business-specific card stays in feature:

```text
apps/tch-portal/src/app/features/admin/payouts/payout-summary-card.ts
```

---

### Public home

Page:

```text
apps/tch-portal/src/app/features/public/home/public-home.page.ts
```

PageModel runtime, API and renderer:

```text
libs/page-model
```

Concrete widget registry and widgets:

```text
libs/widgets
```

---

### Cashier sale

Contracts/API:

```text
libs/api/src/lib/contracts/sale.ts
libs/api/src/lib/http/sale-api.service.ts
```

Page/state:

```text
apps/tch-portal/src/app/features/cashier/sale/
  cashier-sale.routes.ts
  cashier-sale.page.ts
  cashier-sale.store.ts
```

Operational context dependency:

```text
CashierSaleStore injects CashierOperationalContextStore
```

UI components:

```text
libs/ui/components/loading
libs/ui/components/error-panel
```

Business-specific components stay in the feature until reused.

---

## 14. Anti-patterns

Do not create:

```text
libs/core
libs/data-access
libs/shared/components
libs/shared/facades
libs/shared/stores
libs/shared/utils
```

unless a specific, approved migration changes the architecture.

Avoid:

```text
ui/payout-card
core/shell/header-container
data-access/page-model/api
data-access/payout/model
shared/helpers
misc/utils
```

Prefer:

```text
libs/api/contracts
libs/api/http
libs/ui/components
libs/ui/styles
libs/ui/theme
apps/tch-portal/src/app/features/<surface>/<feature>
```

---

## 15. PR checklist

Before adding/moving code:

* [ ] Is this a contract? Put it in `libs/api/contracts`.
* [ ] Is this an HTTP client? Put it in `libs/api/http`.
* [ ] Is this a PageModel runtime contract/client/renderer/helper? Put it in `libs/page-model`.
* [ ] Is this a concrete PageModel widget or registry entry? Put it in `libs/widgets`.
* [ ] Is this reusable shell presentation without app services? Put it in `libs/web`.
* [ ] Is this reusable visual UI? Put it in `libs/ui/components`.
* [ ] Is this SCSS primitive? Put it in `libs/ui/styles`.
* [ ] Is this runtime theme? Put it in `libs/ui/theme`.
* [ ] Is this page-specific state? Keep it in the feature.
* [ ] Is this cross-surface runtime state? Put it in the owning runtime capability.
* [ ] Is there only one consumer? Do not extract yet.
* [ ] Are you creating a lib just to match a diagram? Do not.
* [ ] Does this make PageModel resolve backend bindings? Do not.
* [ ] Does this make UI components call HTTP? Do not.
