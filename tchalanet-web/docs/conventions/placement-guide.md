# Web Placement Guide — Tchalanet

> **Status**: DRAFT v0.2
> **Scope**: frontend placement rules for `tchalanet-web` / `public-portal`, `admin-portal` et `platform-portal`
> **Related**: `../ARCHITECTURE.md`, `naming.md`, `state-management.md`, `pagemodel.md`

---

## 1. Rule

Place code by **ownership** and **lifetime**, not by generic folder names.

```text
Contract / HTTP client     -> libs/api
Reusable UI component      -> libs/ui/components
Web runtime errors         -> libs/web/errors
Web runtime shell          -> libs/web/shell
Auth / i18n runtime        -> libs/core/{auth,i18n}
Runtime theme              -> libs/ui/theme
Compile-time SCSS          -> libs/ui/styles
Shared static assets       -> libs/shared-assets
Runtime settings / flags   -> libs/shared-config
Routed page                -> apps/<portal>/src/app/features
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
│   ├── public-portal/
│   ├── admin-portal/
│   ├── platform-portal/
│   ├── web-e2e/
│   └── proxy.conf.cjs
└── libs/
    ├── api/
    │   └── src/lib/
    │       ├── contracts/
    │       ├── http/
    │       └── backend-client/
    ├── core/
    │   ├── auth/
    │   └── i18n/
    ├── page-model/
    ├── shared-assets/
    ├── shared-config/
    ├── web/
    │   ├── errors/
    │   └── shell/
    ├── widgets/
    └── ui/
        ├── components/
        ├── styles/
        └── theme/
```

`page-model`, `widgets`, `core/auth`, `core/i18n`, `web/errors`, and `web/shell` are extracted.
The root `libs/web` project remains as a compatibility façade while slices move to explicit libs.
`libs/api` remains a single Nx lib with internal folders; do not create `libs/api/contracts` or
`libs/api/backend-client` as separate projects.

---

## 3. Placement table

| Element                                     | Placement                                                                              |
| ------------------------------------------- | -------------------------------------------------------------------------------------- |
| `ApiResponse`, `ProblemDetail`, `ApiNotice` | `libs/api/src/lib/contracts`                                                           |
| `ActionItem`, `NavigationDestination`       | `libs/api/src/lib/contracts`                                                           |
| HTTP interceptors                           | `libs/api/src/lib/http`, auth-specific interceptor may live near auth during migration |
| Generic API helpers                         | `libs/api/src/lib/http`                                                                |
| `TchBackendClient`, request options         | `libs/api/src/lib/backend-client`                                                      |
| Runtime config                              | `libs/shared-config`                                                                   |
| Feature flags                               | `libs/shared-config`                                                                   |
| Shared static assets                        | `libs/shared-assets/public/assets`                                                     |
| Stable asset URL constants                  | `libs/shared-assets/src/lib`                                                           |
| Theme runtime / active mode                 | `libs/ui/theme`                                                                        |
| Theme API / theme preset registry           | `libs/ui/theme` or `libs/api/http` + `ui/theme` depending ownership                    |
| Theme SCSS generation                       | `libs/ui/theme/src/scss`                                                               |
| Shared SCSS mixins/functions/breakpoints    | `libs/ui/styles`                                                                       |
| Material global overrides                   | `libs/ui/styles` unless directly tied to M3 preset generation                          |
| Button / Card / Badge / Loading             | `libs/ui/components`                                                                   |
| Error models/copy/routing/components        | `libs/web/errors`                                                                      |
| Brand / Nav / OverlayNav / SidebarNav       | `libs/ui/components`                                                                   |
| Public shell                                | reusable primitives in `libs/web/shell`; app-specific orchestration in the app         |
| Admin/platform shell                        | reusable primitives in `libs/web/shell`; route/provider composition in the owning app  |
| Public home page                            | `apps/public-portal/src/app/features/public/home`                                    |
| POS pages V0                                | `apps/admin-portal/src/app/features/pos/...`                                           |
| Tenant admin dashboard page                 | `apps/admin-portal/src/app/features/admin/dashboard`                                   |
| Platform dashboard page                     | `apps/platform-portal/src/app/features/platform/dashboard`                             |
| PageModel runtime API contract              | `libs/page-model`                                                                      |
| PageModel API client                        | `libs/page-model`                                                                      |
| PageModel renderer                          | `libs/page-model`                                                                      |
| Widget registry / concrete widgets          | `libs/widgets`, grouped as `widgets/<surface>/<widget-name>/`                          |
| PageModel editor screen                     | `apps/platform-portal/src/app/features/platform/page-models`                         |
| Auth session store                          | `libs/core/auth` when shared; app-owned wiring stays in the app during migration       |
| Auth guards                                 | `libs/core/auth` when shared                                                           |
| Login page                                  | `libs/core/auth`                                                                       |
| Locale store                                | `libs/core/i18n` when shared; app-owned wiring stays in the app during migration       |
| Language switcher UI                        | `libs/ui/components`                                                                   |
| Local fallback i18n JSON                    | `libs/shared-assets/public/assets/i18n/{locale}`                                       |
| Backend i18n API contract/client            | `libs/api`                                                                             |
| Platform i18n admin screen                  | `apps/platform-portal/src/app/features/platform/i18n-overrides`                      |
| Test helpers                                | only create `testing` area when reused by multiple tests                               |

---

## 4. Operational context placement

Operational context is a runtime capability used by POS flows.

Examples:

```text
current tenant
current outlet
current terminal
current sales session
seller identity
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
apps/admin-portal/src/app/features/pos/operational-context/
  operational-context.routes.ts
  operational-context.page.ts
  operational-context.store.ts
```

If only the POS dashboard uses it, keep the store local to the POS feature.

If several POS pages need it, place the store at the POS feature boundary:

```text
apps/admin-portal/src/app/features/pos/
  pos-operational-context.store.ts
```

If admin, POS sale, payout, terminal binding, and shell all need the same runtime state, extract later to an owning runtime area.

Possible future placement:

```text
apps/admin-portal/src/app/core/operational-context
```

or, when stable enough:

```text
libs/core/operational-context
```

Do not create `core/operational-context` until there is a real multi-surface need.

### Route wiring

Routes connect pages to stores/providers.

Example:

```ts
export const sellerTerminalRoutes: Routes = [
  {
    path: '',
    providers: [CashierOperationalContextStore],
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./dashboard/pos-dashboard.page').then(m => m.PosDashboardPage),
      },
      {
        path: 'sale',
        loadComponent: () => import('./sale/pos-sale.page').then(m => m.PosSalePage),
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
apps/public-portal/src/app/features/public/shell
apps/<portal>/src/app/features/dashboard/shell
```

Reusable shell primitives, such as shell feedback models/stores that can be shared by
`public-portal`, `admin-portal`, `platform-portal`, or a future `pos-portal`, belong behind
`@tch/web/shell`. App routes and provider composition stay in the app.

Reusable public shell presentation:

```text
libs/web/shell/src/lib/public-shell
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
libs/web/shell/src/lib/public-shell/public-footer.ts
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

Concrete widgets follow the same surface taxonomy as i18n bundles:

```text
libs/widgets/src/lib/widgets/public/<widget-name>/
libs/widgets/src/lib/widgets/surface-admin/<widget-name>/
libs/widgets/src/lib/widgets/surface-platform/<widget-name>/
libs/widgets/src/lib/widgets/surface-seller-terminal/<widget-name>/
libs/widgets/src/lib/widgets/shared/<widget-name>/
```

Each concrete widget folder uses separate files:

```text
<widget-name>.widget.ts
<widget-name>.widget.html
<widget-name>.widget.scss
```

Rules:

```text
Widget = props + dynamic data + local error
Widget does not call HTTP directly
Widget does not receive full PageRuntimeResponse
Widget type maps directly from backend type string
Widget registry imports stay synchronous; widgets are rendered through PageModel, not lazy routes
```

---

## 9. i18n placement

Runtime locale state:

```text
libs/core/i18n
```

During migration, app-specific bootstrap may still live in:

```text
apps/<portal>/src/app/core/i18n
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
apps/platform-portal/src/app/features/platform/i18n-overrides
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
apps/<portal>/src/app/features/<surface>/<feature>
```

Do not put tenant config state in `ui`.

---

## 11. Auth placement

Shared auth:

```text
libs/core/auth
```

During migration, app-specific bootstrap may still live in:

```text
apps/<portal>/src/app/core/auth
```

Auth owns:

```text
AuthSessionStore
login/logout orchestration
guards
permission helpers
token/session storage
```

Shared login page:

```text
libs/core/auth/src/lib/login
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
libs/core/auth
libs/core/i18n
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
apps/admin-portal/src/app/features/admin/payouts/
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
apps/admin-portal/src/app/features/admin/payouts/payout-summary-card.ts
```

---

### Public home

Page:

```text
apps/public-portal/src/app/features/public/home/public-home.page.ts
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

### POS sale

Contracts/API:

```text
libs/api/src/lib/contracts/sale.ts
libs/api/src/lib/http/sale-api.service.ts
```

Page/state:

```text
apps/admin-portal/src/app/features/pos/sale/
  pos-sale.routes.ts
  pos-sale.page.ts
  pos-sale.store.ts
```

Operational context dependency:

```text
PosSaleStore injects PosOperationalContextStore
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

Do not read this as “never create a `core/*` lib”. `libs/core/auth` and `libs/core/i18n` are
valid because they are explicit runtime capabilities with public aliases and real consumers.
The anti-pattern is a vague catch-all `libs/core` dumping ground.

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
apps/<portal>/src/app/features/<surface>/<feature>
```

---

## 15. PR checklist

Before adding/moving code:

- [ ] Is this a contract? Put it in `libs/api/contracts`.
- [ ] Is this an HTTP client? Put it in `libs/api/http`.
- [ ] Is this a PageModel runtime contract/client/renderer/helper? Put it in `libs/page-model`.
- [ ] Is this a concrete PageModel widget or registry entry? Put it in `libs/widgets`.
- [ ] Is this reusable shell presentation without app services? Put it in `libs/web`.
- [ ] Is this a static asset shared by several deployable apps? Put it in `libs/shared-assets`.
- [ ] Is this reusable visual UI? Put it in `libs/ui/components`.
- [ ] Is this SCSS primitive? Put it in `libs/ui/styles`.
- [ ] Is this runtime theme? Put it in `libs/ui/theme`.
- [ ] Is this page-specific state? Keep it in the feature.
- [ ] Is this cross-surface runtime state? Put it in the owning runtime capability.
- [ ] Is there only one consumer? Do not extract yet.
- [ ] Are you creating a lib just to match a diagram? Do not.
- [ ] Does this make PageModel resolve backend bindings? Do not.
- [ ] Does this make UI components call HTTP? Do not.
