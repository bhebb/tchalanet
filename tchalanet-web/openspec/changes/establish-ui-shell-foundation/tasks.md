# Tasks

## 1. Foundation

- [x] Add `libs/ui/styles` SCSS primitives and documentation.
- [x] Add `libs/ui/components` project, `ActionItem` contract/helpers, and breakpoint service.

## 2. Reusable components

- [x] Add brand, desktop nav, overlay nav, sidebar nav, language/theme group, loading, and error components.
- [x] Ensure component styles consume `--tch-*` tokens and expose local `--comp-*` variables.

## 3. Shell migration

- [x] Type public/private resolved shell runtime contracts.
- [x] Migrate public header/footer/bottom navigation to shared action helpers/components.
- [x] Migrate private shell to `navigationDrawer` and reusable sidebar/top-app-bar components.
- [x] Confirm PageModel remains shell-agnostic and runtime rendering has no `fileKey`/`jsonFile` dependency.

## 4. Validation

- [x] Add or adapt focused tests for navigation helpers and shell contract behavior.
- [x] Move `libs/shared/theme` atomically to `libs/ui/theme`; update imports, aliases, SCSS paths,
      scripts, tests, and the theme README without a facade or parallel API.
- [ ] Run focused Nx lint, test, and build validation.
