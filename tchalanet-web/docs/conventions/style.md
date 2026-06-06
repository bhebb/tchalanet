# Web Style Convention — Tchalanet

> **Status**: ACTIVE v0.1
> **Scope**: Angular SCSS, component styles, layout styles, shared style primitives
> **Related**: `theme-convention.md`, `web-naming.md`, `WEB_ARCHITECTURE.md`
> **Rule**: update this document in the same commit as any code that changes a style rule here.

---

## 1. Purpose

This document defines how styles are written in `tchalanet-web`.

It covers:

* class naming;
* component-local CSS variables;
* when to create shared SCSS primitives;
* px/rem usage;
* layout/container rules;
* card/surface patterns;
* responsive style rules;
* Material override placement.

It does **not** define runtime theme selection.
Runtime theme rules live in `theme-convention.md`.

---

## 2. Separation of responsibilities

```text
ui/theme
  runtime theme, presets, tokens, dark mode, ThemeDomApplier

ui/styles
  compile-time SCSS primitives, mixins, functions, typography, Material overrides

ui/components
  reusable components consuming --tch-* tokens and exposing --comp-* variables

features/web/pages/widgets
  screen-specific composition and local layout only
```

Rules:

* `libs/ui/theme` owns runtime token application.
* `libs/ui/styles` owns reusable SCSS primitives.
* `libs/ui/components` owns reusable visual components.
* Feature styles must not create new global design rules.

---

## 3. Class naming

Use BEM-like class names.

Pattern:

```text
.block
.block__element
.block--modifier
.block__element--modifier
```

Examples:

```scss
.public-footer {}
.public-footer__inner {}
.public-footer__brand {}
.public-footer__social {}
.public-footer--compact {}
```

For Tchalanet shared components, prefix classes with `tch-` when the component is generic:

```scss
.tch-brand {}
.tch-brand__image {}

.tch-nav {}
.tch-nav__list {}

.tch-sidebar {}
.tch-sidebar__section {}
```

For feature-specific components, use the feature/block name:

```scss
.public-home {}
.public-home__hero {}

.cashier-dashboard {}
.cashier-dashboard__summary {}
```

Avoid generic class names:

```text
.container
.wrapper
.content
.left
.right
.card
.title
```

unless scoped inside a clearly named block and not exported globally.

Preferred:

```scss
.public-header__inner
.public-footer__columns
.tenant-dashboard__kpis
```

---

## 4. Component-local variables

Reusable components must expose local `--comp-*` variables.

Pattern:

```scss
:host {
  --comp-footer-bg: var(--tch-color-primary);
  --comp-footer-fg: var(--tch-color-on-primary);
  --comp-footer-link: var(--tch-color-primary-fixed);
}

.public-footer {
  background: var(--comp-footer-bg);
  color: var(--comp-footer-fg);
}
```

Rules:

* `--tch-*` = global theme tokens.
* `--comp-*` = component-local extension points.
* `--comp-*` must fallback to `--tch-*`.
* `--comp-*` must not become new global theme roles.
* Do not use `--comp-*` from another component.

Correct:

```scss
:host {
  --comp-card-bg: var(--tch-color-surface);
  --comp-card-fg: var(--tch-color-on-surface);
  --comp-card-radius: var(--tch-radius-lg);
}

.tch-card {
  background: var(--comp-card-bg);
  color: var(--comp-card-fg);
  border-radius: var(--comp-card-radius);
}
```

Avoid:

```scss
.some-other-component {
  background: var(--comp-card-bg);
}
```

---

## 5. Token usage

Components consume tokens.

Preferred tokens:

```text
--tch-color-background
--tch-color-on-background
--tch-color-surface
--tch-color-on-surface
--tch-color-surface-container
--tch-color-on-surface-variant
--tch-color-primary
--tch-color-on-primary
--tch-color-secondary
--tch-color-secondary-container
--tch-color-on-secondary-container
--tch-color-outline
--tch-color-outline-variant
--tch-color-error
--tch-color-on-error
--tch-radius-sm
--tch-radius-md
--tch-radius-lg
--tch-radius-xl
--tch-radius-pill
--tch-elevation-1
--tch-elevation-2
--tch-elevation-3
--tch-focus-ring-width
--tch-focus-ring-offset
--tch-page-max
--tch-page-gutter
--tch-font-family
```

Hardcoded color values are allowed only as defensive fallback:

```scss
color: var(--tch-color-on-surface, #1a1c1e);
```

Avoid hardcoding brand decisions in components:

```scss
background: #1a1b4b;
color: #fff;
```

Instead:

```scss
background: var(--tch-color-primary);
color: var(--tch-color-on-primary);
```

---

## 6. Unit rules: rem, px, %, dvh

Use `rem` for typography and spacing that should scale with user settings.

Examples:

```scss
gap: 1rem;
padding: 1.5rem;
font-size: 0.875rem;
```

Use `px` for:

* hairline borders;
* icon sizes when matching Material icons;
* exact breakpoint definitions;
* small technical dimensions.

Examples:

```scss
border: 1px solid var(--tch-color-outline-variant);
width: 24px;
height: 24px;
```

Use `%`, `fr`, `min()`, `max()`, `clamp()` for layout.

Examples:

```scss
width: min(100% - 2 * var(--tch-page-gutter), var(--tch-page-max));
grid-template-columns: repeat(3, minmax(0, 1fr));
gap: clamp(1rem, 3vw, 2rem);
```

Use `dvh` for full-height mobile-safe overlays/shells:

```scss
min-height: 100dvh;
max-height: min(90dvh, 640px);
```

Avoid using `100vh` for mobile overlays unless there is a specific reason.

---

## 7. Container and centering

Use the standard page container pattern:

```scss
.page-section {
  width: min(100% - 2 * var(--tch-page-gutter, 16px), var(--tch-page-max, 1120px));
  margin-inline: auto;
}
```

Do not use random widths per component.

Avoid:

```scss
width: 90%;
max-width: 1193px;
margin-left: auto;
margin-right: auto;
```

Prefer:

```scss
width: min(100% - 2 * var(--tch-page-gutter), var(--tch-page-max));
margin-inline: auto;
```

For full-bleed sections:

```scss
.hero {
  width: 100%;
}
```

Then place an inner container:

```scss
.hero__inner {
  width: min(100% - 2 * var(--tch-page-gutter), var(--tch-page-max));
  margin-inline: auto;
}
```

---

## 8. Layout rules

Use CSS Grid for page-level layout and multi-column structures.

Examples:

```scss
.public-footer__inner {
  display: grid;
  grid-template-columns: minmax(16rem, 1.2fr) 2fr;
  gap: clamp(2rem, 6vw, 4rem);
}
```

Use Flexbox for one-dimensional alignment:

```scss
.public-header__actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
```

Avoid over-nesting layout wrappers.
Each wrapper must have a clear reason:

```text
section block
inner container
grid/list
item/card
```

Do not introduce wrappers only to “make CSS easier” unless they are named and reusable.

---

## 9. Card and surface pattern

For reusable cards, prefer a shared component or a local block using the same token pattern.

Base card pattern:

```scss
.tch-card {
  --comp-card-bg: var(--tch-color-surface);
  --comp-card-fg: var(--tch-color-on-surface);
  --comp-card-border: var(--tch-color-outline-variant);
  --comp-card-radius: var(--tch-radius-xl);
  --comp-card-padding: 1rem;
  --comp-card-shadow: var(--tch-elevation-1);

  background: var(--comp-card-bg);
  color: var(--comp-card-fg);
  border: 1px solid var(--comp-card-border);
  border-radius: var(--comp-card-radius);
  padding: var(--comp-card-padding);
  box-shadow: var(--comp-card-shadow);
}
```

Feature-specific cards should keep the same structure:

```scss
.tenant-dashboard-kpi {
  --comp-kpi-bg: var(--tch-color-surface);
  --comp-kpi-radius: var(--tch-radius-lg);

  background: var(--comp-kpi-bg);
  border-radius: var(--comp-kpi-radius);
}
```

Do not create a new card visual language in each feature.

---

## 10. Responsive rules

Use mobile-first styles.

Preferred:

```scss
.public-footer__columns {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1.5rem;
}

@media (min-width: 768px) {
  .public-footer__columns {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
```

Breakpoints:

```text
sm 480px
md 768px
lg 1024px
xl 1280px
```

SCSS media queries should use `libs/ui/styles` breakpoints once available:

```scss
@use '@tch/ui/styles' as ui;

@include ui.up(md) {
  ...
}
```

Angular runtime breakpoint logic should use `TchBreakpointService`, not duplicated ad hoc `matchMedia`.

---

## 11. Shared SCSS primitives

Create a shared SCSS primitive only if it is reused by multiple components or represents a stable design rule.

Good candidates:

```text
breakpoints
functions
mixins
typography helpers
focus visible
container
ellipsis
surface
outline
elevation
Material overrides
```

Do not create a generic mixin for one component.

Avoid:

```scss
@mixin public-footer-column {}
@mixin hero-ticket-card {}
```

unless the pattern is used across multiple surfaces.

---

## 12. Material overrides

Angular Material overrides must be centralized.

Allowed places:

```text
libs/ui/styles/src/lib/_material-overrides.scss
libs/ui/theme/src/scss/* when directly tied to M3 theme generation
```

Do not hack Material internals inside feature components.

Avoid:

```scss
.some-feature {
  ::ng-deep .mat-mdc-menu-panel {
    ...
  }
}
```

Allowed only when:

* no public API exists;
* the override is local and justified;
* a TODO explains how to remove it.

For overlay/menu/dialog defaults, prefer global override files.

---

## 13. `::ng-deep`

`::ng-deep` is discouraged.

Allowed only for:

* temporary compatibility with Angular Material internals;
* styling a projected child when there is no alternative;
* one-off migration from legacy components.

Rules:

* keep it scoped under `:host`;
* add a comment if it is not obvious;
* do not use it for ordinary component styling.

Example:

```scss
:host ::ng-deep .mat-mdc-menu-content {
  padding: 0.5rem;
}
```

---

## 14. Accessibility styles

Every interactive component must have visible focus.

Preferred:

```scss
:focus-visible {
  outline: var(--tch-focus-ring-width, 2px) solid currentColor;
  outline-offset: var(--tch-focus-ring-offset, 2px);
}
```

Do not remove focus outlines without replacing them.

Avoid:

```scss
button:focus {
  outline: none;
}
```

Touch targets should be at least:

```text
44px minimum
48px preferred
```

Use token if available:

```scss
min-height: var(--tch-touch-target, 48px);
```

---

## 15. Z-index

Do not use random z-index values.

Preferred token pattern:

```text
--tch-z-header
--tch-z-drawer
--tch-z-overlay
--tch-z-toast
```

Example:

```scss
.public-header {
  z-index: var(--tch-z-header, 30);
}

.tch-overlay-nav {
  z-index: var(--tch-z-overlay, 4000);
}
```

---

## 16. Global styles

Global styles are allowed only for:

* CSS reset/base document;
* typography base;
* theme token application;
* Material overrides;
* overlay behavior;
* utility classes explicitly documented.

Avoid global classes that compete with component classes.

Allowed:

```scss
html,
body {
  font-family: var(--tch-font-family), system-ui, sans-serif;
}
```

Avoid:

```scss
.card {}
.button {}
.title {}
.container {}
```

unless they are deliberate documented utilities.

---

## 17. Utilities

Prefer components and local classes over utility sprawl.

Small utilities are acceptable if stable and documented:

```text
.visually-hidden
.text-muted
.h1 / .h2 / .h3
```

Do not recreate Tailwind-like utilities manually.

---

## 18. File placement

Reusable SCSS primitives:

```text
libs/ui/styles/src/lib/
```

Reusable component styles:

```text
libs/ui/components/src/lib/<component>/<component>.scss
```

Feature/page-specific styles:

```text
apps/tch-portal/src/app/features/<surface>/<feature>/
```

Theme runtime SCSS:

```text
libs/ui/theme/src/scss/
```

---

## 19. Anti-patterns

Do not:

* hardcode theme colors in components;
* create new global CSS variables for one component;
* use `--comp-*` variables across components;
* duplicate breakpoints in multiple files;
* style Material internals in feature styles;
* create generic mixins for one-off patterns;
* use `px` for all spacing;
* use `100vh` for mobile overlays;
* remove focus outlines;
* create global `.card`, `.button`, `.container` classes casually;
* put runtime theme logic in `ui/styles`.

---

## 20. PR checklist

Before merging style changes:

* [ ] Class names are searchable and scoped.
* [ ] Reusable component exposes `--comp-*` variables.
* [ ] Component variables fallback to `--tch-*`.
* [ ] No new hardcoded brand colors.
* [ ] Responsive logic uses standard breakpoints.
* [ ] Layout uses standard container pattern.
* [ ] Focus-visible state exists for interactive elements.
* [ ] Material overrides are not hidden in feature styles.
* [ ] No new global utility without documentation.
* [ ] Theme rules were not duplicated from `theme-convention.md`.
