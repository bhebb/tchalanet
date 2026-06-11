# Change: Route Private Dashboards Through the Widget Engine

## Status

Proposed

## Why

The private dashboards (cashier, tenant admin, platform super-admin) currently render through
hardcoded feature pages backed by local mocks:

```ts
// features/cashier/pages/dashboard/cashier-dashboard.service.ts
load() {
  // Backend endpoint not yet available — return local mock data.
  return of(CASHIER_DASHBOARD_MOCK);
}
```

This diverges from the PageModel widget engine that already exists (`PageModelApi`,
`PageModelComponent`/`WidgetHostComponent`, `WIDGET_REGISTRY`, and the `KpiGrid`/`Alerts`/
`QuickActions`/`ReadinessSummary` widgets in `libs/widgets`). It is the exact anti-pattern called
out by `minimal-public-admin-widget-ui` ("No mock-only dashboards that diverge from backend
contracts") and by `docs/conventions/pagemodel.md` §15.

The backend already serves the runtime payloads (`PageModelApi` routes `/tenant/dashboard` and
`/platform/dashboard`; the cashier endpoint now returns sidenav + quick-action data too). On login
the app makes two calls: `GET /tenant/runtime/private-bootstrap` (shell: sidenav, top bar, quick actions,
**i18n bundle**) and the per-role dashboard page call (`content.widgets[id]` config +
`dynamic.widgets[id]` data). The dashboards must consume that contract instead of mocks.

> **Dependency**: i18n delivery and the bootstrap mechanic are owned by
> `web-runtime-bootstrap-state-i18n` (frontend) and backend `runtime-state-and-public-bootstrap-v1`.
> This change consumes the bootstrap-delivered i18n store and only **adds dashboard keys** to the
> `fr/en/ht` bundles; it does not rework the loader.

Two cross-cutting gaps remain in this change's scope:

- **Material icons**: nav icons do not load (no self-hosted Material Symbols font).
- **Widget data binding**: widgets read `dynamic.widgets[id]` ad-hoc; there is no declared
  `{source, path}` convention to map a backend dynamic field to a widget value.

## What changes

- Route cashier, tenant-admin and platform dashboards through `PageModelApi` →
  `PageModelComponent`/`WidgetHostComponent` → registered widgets. Remove the hardcoded
  `*-dashboard.service.ts`, `*-dashboard.mock.ts` and bespoke `*-dashboard.page` rendering.
- Add a declared widget data-binding convention (`{source, path}` over `dynamic.widgets[id]`),
  optional and backward-compatible with existing public widgets.
- Self-host Material Symbols as a **subsetted variable font** and provide one reusable icon
  rendering path used by sidenav/topbar/widgets.
- Resolve the private i18n surface and add dashboard keys to `fr`, `en`, `ht` bundles.
- Tokenize the private sidebar styling (`--tch-*` / `--comp-*`) and wire real quick-action labels.
- Add empty / loading / error states inside the widgets (using `WidgetState`), responsive
  mobile→tablet→desktop using `libs/ui/styles/_breakpoints.scss`.
- Keep all dashboards lazy-loaded so the public bundle is unaffected.

## Impact

- Touches `apps/tch-portal` (features cashier/admin/platform dashboards, private shell, core/i18n),
  `libs/page-model` (widget binding contract), `libs/widgets` (widget states/bindings),
  `libs/ui/components` (sidenav/nav icon rendering), `libs/ui/styles` + `libs/ui/theme`
  (icon font, tokens).
- Coordinates with backend change `extend-pagemodel-runtime-role-dashboards` for the `dynamic`
  payload shape. No new backend contract is introduced by this web change.
- Establishes the reusable private-dashboard rendering pattern for later role surfaces.

## Non-goals

- No new widget vocabulary diverging from backend `type` strings.
- No CMS / widget-builder / theme-builder UI.
- No analytics-heavy dashboards beyond the existing KPI/alerts/quick-actions/readiness widgets.
- No backend implementation (payload work belongs to the backend change).
- No full Material Symbols font shipped (subset only).
