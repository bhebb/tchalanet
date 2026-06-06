# Change: establish-ui-shell-foundation

## Why

The active Angular application has a resolved PageModel runtime and an initial public/private shell,
but navigation helpers, shell components, responsive rules, and theme responsibilities are spread
between the application and legacy shared libraries.

This change establishes small, explicit UI boundaries so PageModel remains content-only and shell,
theme, and reusable UI behavior can evolve independently.

## What changes

- Add `libs/ui/styles` for shared SCSS primitives only.
- Move the existing runtime-theme implementation from `libs/shared/theme` to `libs/ui/theme`
  atomically after the shell/style/component boundaries are established.
- Add `libs/ui/components` for reusable shell/navigation/error components and the shared
  `ActionItem` contract/helpers.
- Type the resolved public/private shell runtime contracts.
- Migrate public and private shell rendering to reusable components and resolved shell payloads.
- Keep PageModel limited to rows, columns, widgets, and dynamic widget payloads.

## Impact

- Touches only `tchalanet-web`.
- Adds new Nx UI libraries and path aliases.
- Changes shell/component imports and runtime contract types.
- Does not require backend calls or frontend resolution of `fileKey`/`jsonFile`.

## Non-goals

- No full design system or visual redesign.
- No CMS, theme builder, advanced layout engine, or widget-wide migration.
- No backend implementation or internal binding resolution in Angular.
