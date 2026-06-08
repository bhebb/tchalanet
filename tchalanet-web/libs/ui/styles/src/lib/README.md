# UI Styles

Shared SCSS primitives only: breakpoints, functions, mixins, typography, overlay helpers, and
global Material overrides.

This library never selects or mutates the runtime theme. Components consume global `--tch-*`
tokens and expose local `--comp-*` variables.

Import the primitives with `@use 'index'` after adding `libs/ui/styles/src/lib` to the consuming
application's SCSS include paths.

## Files

| File | What it provides |
|---|---|
| `_breakpoints.scss` | `$bps` map (M3 5-tier), `bp()` function, `up()` / `down()` / `between()` mixins |
| `_functions.scss` | SCSS utility functions |
| `_mixins.scss` | `surface`, `outline`, `rounded`, `elevate`, `focus-visible`, `text-ellipsis`, `transition`, `state-layer` |
| `_typography.scss` | Global `html/body` font-family reset; `.text-muted` utility |
| `_overlay.scss` | Overlay/drawer layout helpers |
| `_material-overrides.scss` | Global Angular Material component overrides (not theme tokens) |
| `_index.scss` | Re-exports all of the above |

## Breakpoints

M3 window-size classes — 5 tiers per `m3.material.io/foundations/layout/breakpoints`:

```text
compact      < 600px   (phones portrait)
medium      600–839px  (tablets portrait, foldables)
expanded   840–1199px  (tablets landscape, desktop)
large     1200–1599px  (large desktop)
extra-large  ≥ 1600px  (ultra-wide)
```

Keys in `$bps` are the **class that starts** at each value. Usage:

```scss
@use '@tch/ui/styles' as ui;

@include ui.up(medium)   { ... }   // ≥ 600px
@include ui.up(expanded) { ... }   // ≥ 840px
@include ui.between(medium, expanded) { ... }
```

Do not define breakpoint pixel values elsewhere — use `bp()` or the mixins.

## Key mixins

```scss
@use '@tch/ui/styles' as ui;

// Surface fill
@include ui.surface($bg, $fg);

// Focus ring — always use this, never remove focus outlines
@include ui.focus-visible;

// Elevation (levels 0–5)
@include ui.elevate(2);

// Responsive
@include ui.up(medium) { ... }
@include ui.between(medium, expanded) { ... }

// M3 motion — uses duration + easing tokens
// $family: emphasized (expressive) | standard (utility)  $phase: stay | enter | exit
@include ui.transition(opacity, emphasized, enter);
@include ui.transition(transform opacity, standard, exit);

// M3 state layer — hover 8% / focus·pressed 12% overlay
@include ui.state-layer(var(--tch-color-on-primary));
```

## Typography utilities

`_typography.scss` provides `.h1/.h2/.h3` backed by the M3 type scale tokens:

```text
.h1  →  --tch-font-size-display-lg   / --tch-line-height-display-lg   / 800
.h2  →  --tch-font-size-headline-lg  / --tch-line-height-headline-lg  / 800
.h3  →  --tch-font-size-headline-mobile / --tch-line-height-headline-mobile / 700
```
