# Tasks

## 1. Architecture + contracts (PR1)

- [ ] Replace legacy architecture reference with `docs/ARCHITECTURE.md`.
- [x] Confirm `ActionItem` and `NavigationDestination` are defined in
      `libs/api/src/lib/contracts`.
- [x] Export contracts from `libs/api/src/index.ts`.
- [ ] Add `navigation.helpers.ts` with:
      `actionText`, `actionRoute`, `actionHref`, `isExternalAction`, `isRouteAction`.
- [ ] Keep migration scope bounded (do not move all consumers in this PR).

## 2. UI styles + theme docs (PR2)

- [x] Create/complete `libs/ui/styles/src/lib/_breakpoints.scss`.
- [x] Create/complete `libs/ui/styles/src/lib/_functions.scss`.
- [x] Create/complete `libs/ui/styles/src/lib/_mixins.scss`.
- [x] Create/complete `libs/ui/styles/src/lib/_typography.scss`.
- [x] Create/complete `libs/ui/styles/src/lib/_index.scss`.
- [x] Add/update `libs/ui/styles/src/lib/README.md` (library usage only, no conventions).
- [x] Align theme/style decisions in `docs/conventions/theme.md` and
      `docs/conventions/style.md`.
- [ ] Avoid runtime-theme changes in this PR except trivial fixes.

## 3. Navigation components migration (PR3)

- [x] Migrate `brand` to `ActionItem`.
- [x] Migrate desktop `nav` to `ActionItem`.
- [x] Migrate `overlay-nav` to `ActionItem`.
- [x] Migrate `sidebar-nav` to `ActionItem`.
- [x] Remove new `TchLink` usage in these components.
- [x] Handle internal route vs external URL through shared helpers.
- [ ] Preserve current visual design.

## 4. Shell integration (PR4)

- [x] Update `PublicShell` to read `shell.header` and `shell.footer`.
- [x] Update `PrivateShell` to read `shell.topAppBar` and `shell.navigationDrawer`.
- [x] Remove legacy `shell.header.component = PrivateShell` behavior.
- [x] Remove dependency on `dynamic.widgets['shell.header']`.
- [ ] Verify sidenav visibility for tenant admin, superadmin, and cashier.
- [x] Keep `PageModel` limited to `content.layout.rows[].columns[].widgets`.

## 5. Error components (PR5)

- [x] Add `tch-page-error`.
- [x] Keep `tch-error-panel` for section/widget/card errors.
- [x] Add `tch-field-error` or standardize field errors on `mat-error`.
- [x] Migrate `NotFoundComponent` to `tch-page-error`.

## 6. Validation

- [ ] Run focused Nx lint/test/build for touched web surfaces.
- [x] Validate route-vs-url behavior in migrated navigation components.
- [ ] Validate shell role scenarios for private sidenav access.
