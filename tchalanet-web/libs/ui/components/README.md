# UI Components

Reusable Angular UI components shared by Web shells and pages.

## Responsibilities

- render reusable UI behavior without owning PageModel or shell runtime resolution;
- consume navigation and action contracts from this library;
- consume global theme tokens under `--tch-*`;
- expose component-local customization under `--comp-*` with `--tch-*` fallbacks.

Runtime theme selection and DOM token application belong to `libs/ui/theme`. Shared SCSS primitives
and global Material overrides belong to `libs/ui/styles`.

The public API is exported from `src/index.ts`.
