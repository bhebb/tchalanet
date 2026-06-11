# Tasks

> Sequencing: **P0 foundations first** (unblocks everything), then the three dashboards in parallel
> (one branch/PR each, 3 commits each), then P2 finish. Check `[ ]` → `[x]` in real time.

## 0. P0 — Foundations (transverse, do first)

### 0.1 i18n keys (loader owned by web-runtime-bootstrap-state-i18n)
- [x] Depends on `web-runtime-bootstrap-state-i18n`: i18n arrives via the bootstrap-filled store.
      This change does NOT rework the loader.
- [x] Added all backend-referenced keys to `fr/en/ht` (full 3-way parity): the 48 dashboard/KPI/
      action/CTA/surface/layout keys **and** the flat sidebar `nav.*` + `nav.section.*` keys the
      fragments use (`nav.dashboard`, `nav.users`, `nav.section.administration`, `nav.toggle_menu`,
      `readiness.noIssues`, …). Root cause of raw-key labels in the screenshot.
- [x] Missing keys degrade safely: `LabelPipe`/`tchLabel` renders a stable key-derived fallback.

### 0.2 Material Symbols renderer (self-hosted)
- [x] Self-hosted `public/assets/fonts/material-symbols-outlined.woff2` (312 KB, full Outlined
      instance → covers **all** icons referenced by web source AND backend pagemodel
      fragments/templates). Reproducible via `npm run fonts:material-symbols`. No Google CDN.
- [x] Verified the woff2 keeps icon ligatures (4254 ligature substitutions under `rlig`, extension-
      wrapped) — `dashboard`/`translate`/`workspace_premium`/`dark_mode`/`rule`/`warning`… all present.
      (NB: a glyph-subset was rejected — it silently stripped the `rlig` feature → raw ligature text.)
- [x] `@font-face` + `.material-symbols-outlined` base in `libs/ui/styles/_icons.scss` (forwarded →
      emitted globally). `font-display: block` avoids ligature-text flash.
- [x] Fixed `TchSidebarNav` icon span (missing `material-symbols-outlined` class); root cause was the
      absent `@font-face`. Confirmed in `nx build`: CSS emitted + woff2 ships to `dist`.
- [ ] (Note) instance is non-variable (fixed weight 400 / FILL 0) — fine for current usage; switch to
      the variable font if a FILL/weight toggle is needed later.

### 0.3 Widget data binding `{source, path}`
- [x] `WidgetBinding {source:'dynamic', path}` + `isBinding`/`resolvePath`/`resolveBinding` in
      `libs/page-model/widget.contract.ts` (re-exported via `@tch/page-model`). Proto-pollution safe.
- [x] Optional + backward-compatible: a literal value passes through; widgets without a binding are unchanged.
- [x] Unit tests in `widget.contract.spec.ts` (path resolution, missing path, `__proto__` guard, literals).
- [x] `KpiGridWidget` resolves each item `value` via binding, falling back to the legacy flat
      `dynamic[id]` lookup. (page-model 14 + widgets 10 tests green.)

### 0.4 API + registry wiring
- [x] No separate cashier route needed: `PrivateShellService` serves cashier via `getTenantPage()`
      (`/tenant/dashboard`); `PageModelApi` exposes `/tenant/dashboard` + `/platform/dashboard`.
- [x] Registry registers `KpiGrid`/`Alerts`/`QuickActions`/`ReadinessSummary` (`libs/widgets/widget-registry.ts`).
- [x] `WidgetHostComponent` passes `config` + resolved `dynamic` to each widget (confirmed in c1 path).

## 1. Tenant Admin dashboard (branch/PR, 3 commits)
- [x] c1 — `/app/admin` routes to `PrivateDashboardPage` (lazy) → `PageModelComponent` fed by
      `PrivateShellService` (`PageModelApi.getTenantPage()` → `/tenant/dashboard`). Deleted
      `admin-dashboard.{service,mock,model,page}` + `tenant-admin-dashboard.page` (orphaned mocks).
- [x] c2 — sidebar already tokenized (`--tch-*`/`--comp-*`); `nav.admin.*` + `dashboard.*` i18n keys present in fr/en/ht (full parity). Quick-action / KPI / CTA labels cross-checked against the backend pagemodel templates+fragments and added to fr/en/ht.
- [~] c3 — empty/loading/error in place; **mobile burger + off-canvas drawer** added to `PrivateShellPage` (burger in top bar, slide-in drawer + backdrop, closes on nav/Escape/backdrop, desktop unchanged). Skeletons still to finalize against the real payload.

## 2. Platform Super-Admin dashboard (branch/PR, 3 commits)
- [x] c1 — `/app/platform` routes to `PrivateDashboardPage` (lazy) → widget engine
      (`PageModelApi.getPlatformPage()` → `/platform/dashboard`). Deleted
      `platform-dashboard.{service,mock,model,page}` + `components/*` + `super-admin-dashboard.page`.
- [x] c2 — sidebar tokenized; `nav.*` + `dashboard.*` i18n keys present in fr/en/ht (full parity). Quick-action / KPI / CTA labels cross-checked against the backend pagemodel templates+fragments and added to fr/en/ht.
- [~] c3 — empty/loading/error in place; mobile burger overlay added (see above). Skeletons to finalize against the real payload.

## 3. Cashier dashboard (branch/PR, 3 commits)
- [x] c1 — `/app/cashier` routes to `PrivateDashboardPage` (lazy) → widget engine. Deleted
      `cashier-dashboard.{service,mock,model,page}` + `shared/cashier-ui/*` mock cards.
- [x] c2 — sidebar tokenized; `nav.*` + `dashboard.*` i18n keys present in fr/en/ht (full parity). Quick-action / KPI / CTA labels cross-checked against the backend pagemodel templates+fragments and added to fr/en/ht.
- [~] c3 — empty/loading/error in place; mobile burger overlay added (see above). Skeletons to finalize against the real payload.

> Note: c1 routing was already wired (all three role routes lazy-load `PrivateDashboardPage` under
> the active `PrivateShellPage` → `TchSidebarNav`, which renders brand + primary + titled sections +
> secondary). This pass removed the orphaned mock-only dashboard clusters + dead dashboard-only
> `admin-ui`/`cashier-ui` components, orphaned `role-dashboard.page`, and the **orphaned 2nd private
> shell** (`private/shell/{private-shell,private-sidenav,private-topbar}.component`) — kept
> `private-shell.service` + `private-navigation.model`. Sidenav nesting (`item.children`) is now rendered
> as an **accordion** in `TchSidebarNav`: parent toggles (never navigates), auto-opens when a child
> matches the route, children indented + smaller as normal routerLinks. (Unit spec deferred —
> `ui-components` has no `@tch/api`-aware test target yet.)

## 4. Validation
- [x] No remaining `*-dashboard.mock.ts` / mock-only dashboard service.
- [x] No hardcoded colors in private dashboards/shell/widgets (only `--tch-*`/`--comp-*` with literal fallbacks).
- [x] Public bundle unaffected (dashboards lazy-loaded; `nx build` green).
- [ ] `pagemodel.md` §16 PR checklist passes for touched surfaces.
- [~] `nx build` green; `nx test` 69 passed / **1** pre-existing WIP failure (`app.spec` — App
      component WIP, fully mocks `AppRuntimeStore`, not touched here). Deleting the orphan shell
      cleared the 4 `private-shell.component.spec` failures. `nx lint` pending.
