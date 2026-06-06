# Tasks

## 1. Architecture + contracts (PR1)

- [ ] Replace legacy architecture reference with `docs/ARCHITECTURE.md`.
- [ ] Confirm `ActionItem` and `NavigationDestination` are defined in
      `libs/api/src/lib/contracts`.
- [ ] Export contracts from `libs/api/src/index.ts`.
- [ ] Add `navigation.helpers.ts` with:
      `actionText`, `actionRoute`, `actionHref`, `isExternalAction`, `isRouteAction`.
- [ ] Keep migration scope bounded (do not move all consumers in this PR).

## 2. UI styles + theme docs (PR2)

- [ ] Create/complete `libs/ui/styles/src/lib/_breakpoints.scss`.
- [ ] Create/complete `libs/ui/styles/src/lib/_functions.scss`.
- [ ] Create/complete `libs/ui/styles/src/lib/_mixins.scss`.
- [ ] Create/complete `libs/ui/styles/src/lib/_typography.scss`.
- [ ] Create/complete `libs/ui/styles/src/lib/_index.scss`.
- [ ] Add/update `libs/ui/styles/src/lib/README.md` (library usage only, no conventions).
- [ ] Align theme/style decisions in `docs/conventions/theme.md` and
      `docs/conventions/style.md`.
- [ ] Avoid runtime-theme changes in this PR except trivial fixes.

## 3. Navigation components migration (PR3)

- [ ] Migrate `brand` to `ActionItem`.
- [ ] Migrate desktop `nav` to `ActionItem`.
- [ ] Migrate `overlay-nav` to `ActionItem`.
- [ ] Migrate `sidebar-nav` to `ActionItem`.
- [ ] Remove new `TchLink` usage in these components.
- [ ] Handle internal route vs external URL through shared helpers.
- [ ] Preserve current visual design.

## 4. Shell integration (PR4)

- [ ] Update `PublicShell` to read `shell.header` and `shell.footer`.
- [ ] Update `PrivateShell` to read `shell.topAppBar` and `shell.navigationDrawer`.
- [ ] Remove legacy `shell.header.component = PrivateShell` behavior.
- [ ] Remove dependency on `dynamic.widgets['shell.header']`.
- [ ] Verify sidenav visibility for tenant admin, superadmin, and cashier.
- [ ] Keep `PageModel` limited to `content.layout.rows[].columns[].widgets`.

## 5. Error components (PR5)

- [ ] Add `tch-page-error`.
- [ ] Keep `tch-error-panel` for section/widget/card errors.
- [ ] Add `tch-field-error` or standardize field errors on `mat-error`.
- [ ] Migrate `NotFoundComponent` to `tch-page-error`.

## 6. Validation

- [ ] Run focused Nx lint/test/build for touched web surfaces.
- [ ] Validate route-vs-url behavior in migrated navigation components.
- [ ] Validate shell role scenarios for private sidenav access.
