# Change: complete-navigation-shell-error-alignment

## Why

The current web architecture and conventions define clear boundaries for contracts, navigation helpers,
UI styles/theme responsibilities, shell integration, and error components, but implementation is still
inconsistent across modules.

We need one bounded change that aligns these surfaces without a visual redesign and without broad
runtime-theme rework.

## What changes

- Replace the legacy architecture document with the current `docs/ARCHITECTURE.md` version as the
  reference baseline for this slice.
- Ensure `ActionItem` and `NavigationDestination` are defined under
  `libs/api/src/lib/contracts` and exported from `libs/api/src/index.ts`.
- Add `navigation.helpers.ts` with `actionText`, `actionRoute`, `actionHref`,
  `isExternalAction`, and `isRouteAction`.
- Complete `libs/ui/styles` shared primitives (`_breakpoints.scss`, `_functions.scss`,
  `_mixins.scss`, `_typography.scss`, `_index.scss`) and align docs with:
  `docs/conventions/style.md` and `docs/conventions/theme.md`.
- Migrate navigation components (`brand`, desktop `nav`, `overlay-nav`, `sidebar-nav`) to
  `ActionItem` + navigation helpers and stop introducing new `TchLink` usage there.
- Integrate shell rendering so:
  - `PublicShell` reads `shell.header` and `shell.footer`;
  - `PrivateShell` reads `shell.topAppBar` and `shell.navigationDrawer`;
  - legacy `shell.header.component = PrivateShell` and `dynamic.widgets['shell.header']`
    dependencies are removed.
- Introduce page-level error component boundaries:
  - `tch-page-error` for page failures;
  - keep `tch-error-panel` for section/widget/card failures;
  - standardize field-level errors (`tch-field-error` or `mat-error`);
  - migrate `NotFoundComponent` to `tch-page-error`.

## Impact

- Scope: `tchalanet-web` only.
- Main areas: `libs/api`, `libs/ui/styles`, `libs/ui/components`, shell integration in app features.
- No backend API contract redesign.
- No broad runtime-theme behavior change (except trivial fixes if required).

## Non-goals

- No visual redesign.
- No full migration of all existing `TchLink` consumers in one PR.
- No expansion of `PageModel` scope beyond `content.layout.rows[].columns[].widgets`.
- No introduction of frontend resolution for backend `fileKey`/`jsonFile` bindings.

## Source docs

- `docs/ARCHITECTURE.md`
- `docs/conventions/style.md`
- `docs/conventions/theme.md`
