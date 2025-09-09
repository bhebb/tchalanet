# Tchalanet Design System

_Last updated: 2025-09-15_

---

## 1) Theming Overview

**Stack**: Angular Material 20 (MDC) + SCSS.  
**Goal**: One master theme (`tchalanet.theme.scss`) + runtime JSON overrides per tenant.

### Principles
- Material palettes (`primary`, `accent`, `warn`) declared once.
- Light & dark variants.
- CSS custom properties exposed for runtime overrides (tenant themes + Theme Builder).
- No `!important`; mobile‑first; prefer CSS variables.

### Theme Files
```
libs/ui/styles/src/
├─ themes/
│  ├─ _surface-vars.scss        # exposes CSS vars from Material palettes (header/hero/footer/etc.)
│  ├─ tchalanet.theme.scss      # master Material theme (light/dark, accent-dot)
│  └─ … (other presets later handled by JSON, not SCSS)
├─ _tokens.scss                 # global CSS tokens (colors/typography/shape/elevation)
├─ _typography.scss             # Material typography + global font utilities
├─ _breakpoints.scss            # bp() map + up() mixin
├─ _mixins.scss                 # container(), surface(), outline(), rounded(), elevate(), focus-visible(), etc.
├─ _utilities.scss              # small utility classes built from mixins
└─ index.scss                   # imports themes + tokens + utilities once
```

### Key Exposed Variables (from `_tokens.scss`)
```scss
:root {
  /* Brand (overridable per tenant) */
  --color-primary: var(--tch-primary, #225EC7);
  --color-secondary: var(--tch-secondary, #134D9F);
  --color-tertiary: var(--tch-tertiary, #D84C51);

  /* Surfaces & text */
  --color-surface: var(--tch-surface, #FAFAFA);
  --color-surface-container: var(--tch-surface-container, #FFFFFF);
  --color-on-surface: var(--tch-on-surface, #0A1633);
  --color-outline: var(--tch-outline, #C9CED6);

  /* Accents */
  --accent-dot: var(--mdc-theme-accent);

  /* Shape & elevation */
  --radius: var(--tch-radius, 12px);
  --elev-1: 0 1px 2px rgba(0,0,0,.08), 0 1px 1px rgba(0,0,0,.06);
  --elev-2: 0 2px 6px rgba(0,0,0,.10), 0 2px 2px rgba(0,0,0,.06);
  --elev-3: 0 4px 10px rgba(0,0,0,.12), 0 3px 3px rgba(0,0,0,.06);

  /* Typography (mobile-first) */
  --tch-h1: clamp(1.6rem, 2.5vw + 1.2rem, 2.4rem);
  --tch-h2: clamp(1.25rem, 1.6vw + 1rem, 1.6rem);
  --tch-h3: clamp(1.05rem, 0.9vw + .9rem, 1.25rem);
  --tch-body: 1rem;
  --tch-muted: 0.9rem;
  --tch-weight-bold: 700;
  --tch-weight-semibold: 600;

  /* Header density tokens */
  --hdr-min-h: 64px;
  --hdr-pad-block: 10px 14px;
  --hdr-row-gap: 10px;
}
@media (min-width:768px){ :root{ --hdr-min-h:72px; } }
@media (min-width:1024px){ :root{ --hdr-min-h:64px; --hdr-pad-block:0 0; --hdr-row-gap:0; } }
```

### Surface Variables from Material Palettes (`_surface-vars.scss`)
```scss
@mixin tchl-surface-vars($primary, $accent, $warn, $mode: light, $dot: null) {
  --mat-primary-500: #{mat.m2-get-color-from-palette($primary, 500)};
  --mat-primary-600: #{mat.m2-get-color-from-palette($primary, 600)};
  --mat-primary-700: #{mat.m2-get-color-from-palette($primary, 700)};
  --mat-primary-contrast: #{mat.m2-get-color-from-palette($primary, '500-contrast')};

  --accent-dot: #{mat.m2-get-color-from-palette(if($dot==null, $warn, $dot), 500)};

  @if $mode == light {
    --header-bg: var(--mat-primary-600);
    --hero-start: color-mix(in oklab, var(--mat-primary-600) 92%, transparent);
    --hero-end:   color-mix(in oklab, var(--mat-primary-600) 12%, transparent);
    --footer-bg: var(--mat-primary-700);
    --header-fg: #fff; --footer-fg: #fff;
  } @else {
    --header-bg: var(--mat-primary-700);
    --hero-start: color-mix(in oklab, var(--mat-primary-700) 96%, transparent);
    --hero-end:   color-mix(in oklab, var(--mat-primary-700) 18%, transparent);
    --footer-bg: var(--mat-primary-700);
    --header-fg: #fff; --footer-fg: #fff;
  }
}
```

### Master Theme (`tchalanet.theme.scss`)
- Defines `$tch-primary`, `$tch-accent`, `$tch-warn`, `$tch-dot` (brand red).
- Builds `$tch-light` / `$tch-dark` (`mat.m2-define-*-theme`).
- Applies via `.mat-theme-tchalanet` and exposes surface vars for light/dark.

Usage (global import once):
```scss
/* index.scss */
@use '@angular/material' as mat;
@use './themes/tchalanet.theme' as tch;
@include tch.apply-tchalanet-class();
@use './_tokens';
@use './_typography';
@use './_utilities';
```

---

## 2) Layout Rules

### Breakpoints (`_breakpoints.scss`)
```scss
$bps: ( sm:480px, md:768px, lg:1024px, xl:1280px ) !default;
@function bp($n){ @return map-get($bps,$n); }
@mixin up($n){ @media (min-width: bp($n)) { @content; } }
```

### Container (`_mixins.scss`)
```scss
$container-max: 1200px !default;
$container-pad-min: 12px !default;
$container-pad-max: 24px !default;
@mixin container($max:$container-max, $pad-min:$container-pad-min, $pad-max:$container-pad-max){
  inline-size: min($max, 100%);
  margin-inline: auto;
  padding-inline: clamp($pad-min, 4vw, $pad-max);
}
```
Utilities:
```scss
.container      { @include container(); }
.container--wide{ @include container(1280px); }
.container--narrow{ @include container(960px); }
```

### Header (pattern adopté)
- **Mobile/Tablet** → 2 lignes (brand+burger / lang+cta).  
- **Desktop** → 1 ligne (brand | nav | lang | cta) avec `column-gap` et une colonne `1fr` pour espacer.

Points clés:
- Burger = `inline-grid; place-items:center; 40×40px` → centré au pixel.
- Logos `display:block` + `line-height:1` sur le label pour alignement parfait.
- Éviter `height` fixe, préférer tokens `--hdr-*` (min-height + paddings).

### Footer
- Grid responsive: 1 → 2 (≥600px) → 4 (≥900px) colonnes.  
- Logo + nom en `inline-flex` + icônes sociaux centrés via `place-items:center`.  
- Baseline centrée; fond via `--footer-bg`.

---

## 3) Utilities & Mixins

- `.surface`, `.surface-container`, `.outline`, `.rounded[-sm|‑lg|‑pill]`
- `.elevate-[1..5]`, `.focus-visible`, `.text-ellipsis[-2|-3]`
- `.sr-only`, `.btn`, `.badge`, `.card`, `.input`

Example:
```scss
.card {
  background: var(--color-surface-container);
  border: 1px solid var(--color-outline);
  border-radius: var(--radius);
  box-shadow: var(--elev-2);
}
```

---

## 4) Tenant Overrides (runtime)

- Backend fournit `/api/v1/configs/theme` et `/api/v1/configs/i18n` (ex.).
- Côté app, on applique le JSON dans `:root` (ou sur `body`) via `style` ou classe tenant.
- Pour Material, seules les **CSS vars** sont overridables en runtime; si besoin de vraies palettes Material → recompile SCSS (optionnel, rare).

```ts
applyTenantTheme(vars: Record<string,string>) {
  const root = document.documentElement;
  for (const [k,v] of Object.entries(vars)) root.style.setProperty(k, v);
}
```

---

## 5) Accessibility & Motion

- Focus visible custom via mixin unique (cohérent header/footer/cards).
- `@media (prefers-reduced-motion: reduce)` → réduire transitions/animations.
- Contraste AA/AAA à valider pour chaque tenant (Theme Builder intègrera un check).

---

## 6) Improvements Backlog

**Theming**
- Theme Builder (preview + export JSON + validation contrastes).
- Density presets (0 / -1 / -2) pour Angular Material.

**Layout**
- Polir les gaps header mobile (densité, marges).
- Uniformiser tailles logo/burger (32px/40px).
- Nav desktop: hover/focus plus smooth (`transition: background .2s ease`).

**Docs/Tooling**
- Storybook des composants avec `:root` vars switchables.
- Visual tests (Playwright) sur 360/768/1024/1440.

---

## 7) Quick Usage

```scss
/* app styles.scss */
@use 'libs/ui/styles/src/index.scss' as *;

/* in app root component template */
<div class="mat-theme-tchalanet">
  <!-- rest of app -->
</div>
```

```html
<!-- Typical page -->
<header class="app-header">…</header>
<main class="container">…</main>
<footer class="ftr">…</footer>
```

---

## 8) Conventions

- Mobile‑first; jamais de `!important`.
- SCSS: fichiers courts, export de mixins/fonctions; pas de styles globaux “magiques”.
- Composants: utiliser `panelClass` pour styliser les menus plutôt que `::ng-deep`.
- Tous les nombres récurrents → tokens (radius, elevations, gaps, fonts).

---

© Tchalanet — Design System v1
