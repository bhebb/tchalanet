# Web Development Architecture — Tchalanet

> Status: DRAFT v0.1
> Scope: `tchalanet-web`
> Goal: define the development architecture we will apply while building the
> Angular/Nx web app.

## 1. Baseline

Tchalanet Web is an Angular/Nx workspace. Runtime and framework lines are owned
by `../../VERSIONS.md`.

Current development line:

- Angular 20.x
- Nx 21.x
- TypeScript 5.8.x
- pnpm lockfile is authoritative for resolved package versions

Upgrades stay inside those lines unless a dedicated version change updates
`../../VERSIONS.md` and the related build/runtime files.

## 2. Architecture Intent

The web app should stay easy to navigate while it grows.

The target architecture is intentionally small:

```text
core         application wiring and global runtime services
features     routed screens and user flows
data-access  backend contracts, API clients and reusable API state
ui           presentational components and renderers
shared       generic utilities and low-level types
```

Every new file should have one obvious home. If a file does not fit, we decide
whether the concept belongs to an existing family before creating a new one.

## 3. Development Model

Features start close to the app when they are product-specific:

```text
apps/tchalanet-portal/src/app/features/<scope>/<feature>
```

Allowed scopes:

```text
public
tenant
admin
platform
```

Extract a feature into an Nx lib only when it becomes shared, large, strategic,
or needs isolated boundaries and tests.

## 4. Runtime Flow

The default flow for a screen is:

```text
route -> feature page -> feature store -> data-access API -> backend
                  |
                  +-> ui components
```

Rules:

- Pages compose the screen and route data.
- Feature stores own screen state such as filters, pagination, selected item,
  loading and errors.
- API calls live in `data-access`, not directly in UI components.
- UI components receive data through inputs and emit user intent through
  outputs.
- Critical business rules stay on the backend; the web app can guide, validate
  and present, but must not become the source of business truth.

## 5. State Placement

Use the narrowest useful state owner:

| Need                                         | Placement                    |
| -------------------------------------------- | ---------------------------- |
| Local toggle, tab, dialog state              | Component `signal()`         |
| Screen state                                 | Feature store                |
| Reusable API cache                           | `data-access/<domain>/state` |
| Session, permissions, locale, runtime config | `core`                       |

NgRx is available, but not the default answer for every screen. Prefer Angular
signals and explicit stores until a flow clearly needs global action-based
state.

## 6. Contracts And Backend Integration

Backend-facing code belongs in `data-access`.

Expected shape:

```text
data-access/<domain>/
  api/
  model/
  state/
```

Frontend DTOs should mirror backend contracts:

- successful responses use the backend `ApiResponse<T>` shape when applicable
- errors use `ProblemDetail`
- paginated responses use the shared page shape

Mapping to view models can live in the feature when it is screen-specific. A
mapper moves to `data-access` only when several features reuse it.

## 7. UI And Theming

`ui` is for presentation:

- no `HttpClient`
- no auth token or tenant resolution
- no backend orchestration
- no critical business decisions

Theme and layout must remain mobile-first, token-themed and i18n-aware. Shared
layout primitives live in `ui/layout`; connected shell containers live in
`core/shell`.

## 8. Nx Boundaries

Nx is the enforcement layer, not the architecture itself.

When a concept becomes an Nx lib, tag it with:

```text
type:<core|feature|data-access|ui|shared>
scope:<public|tenant|admin|platform|shared|core|domain-name>
```

Default allowed dependencies:

```text
features    -> data-access, ui, core, shared
core        -> shared, selected ui layout primitives
data-access -> core/http, shared
ui          -> shared
shared      -> no internal project dependencies
```

## 9. Development Checklist

Before adding or changing a feature:

- Confirm whether an OpenSpec change is required.
- Choose the scope: `public`, `tenant`, `admin` or `platform`.
- Put backend calls in `data-access`.
- Put screen state in a feature store when component signals are not enough.
- Keep reusable visual pieces pure and move them to `ui` only when reuse is
  real.
- Add or adjust Nx tags when a new lib is created.
- Validate with the narrowest relevant Nx target first.

## 10. Open Decisions

These decisions should be resolved as development progresses:

- exact path for future app-local features under `apps/tchalanet-portal`
- whether PageModel editing becomes a platform feature lib or remains app-local
- whether reusable API state uses plain service stores or a formal SignalStore
  helper
- final ESLint module-boundary rules once the current libs are normalized
- migration path for any legacy `libs/web/*` structure that does not match the
  five-family model

## 11. Related Docs

- `WEB_ARCHITECTURE.md` — model, families and responsibilities
- `WEB_STATE_MANAGEMENT.md` — state placement rules
- `WEB_NX_BOUNDARIES.md` — Nx tags and dependency constraints
- `WEB_FEATURE_PLAYBOOK.md` — feature creation workflow
- `WEB_PLACEMENT_GUIDE.md` — placement guide for common frontend concepts
