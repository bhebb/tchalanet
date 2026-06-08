# Theme Convention

> Status: ACTIVE v0.3
> Scope: runtime theme, presets, backend overrides, token mapping
> Living doc — update in the same commit as any code that changes a rule here.

## Rule

Theme is a runtime capability. The active theme is resolved from backend runtime services, with a local preset registry used as fallback and development support.

The backend owns the active theme ID. The frontend owns how known preset IDs become CSS.

## Placement

```text
libs/ui/theme/
```

Use this area for:

- `ThemeApi` runtime calls;
- `ThemeRepository` local preset registry;
- `ThemeStore` active preset/mode state;
- DOM application of CSS variables and Material token aliases;
- small theme controls used by shell or dev surfaces.
- SCSS preset catalogs and runtime token bridges;
- the theme registry generator.

Boundaries:

- `libs/ui/theme/` owns runtime theme selection, token application, dark mode, presets, and
  `OverlayContainer` synchronization;
- `libs/ui/styles/` owns compile-time SCSS primitives and global Material overrides only. It never
  selects the current theme;
- `libs/ui/components/` owns reusable components. Components consume global `--tch-*` tokens and
  expose local `--comp-*` variables with `--tch-*` fallbacks.

## Runtime Shape

The theme runtime is split into two parts:

```text
presetCode -> stable backend/public ID
tokens     -> tenant/public CSS variable overrides
mode       -> light | dark | system
```

The frontend preset registry stores CSS by `id`:

```text
ThemePreset {
  id
  labelKey
  css
}
```

## Backend Source Of Truth

Public pages load public theme runtime.

Tenant/private pages load tenant theme runtime after auth.

If the backend returns an unknown `presetCode`, the frontend keeps that ID active and applies the default preset CSS under that ID until the preset registry catches up.

> Known alignment gap: `public.home.json` declares `theme.presetId = "tchalanet_default"`, but the
> seeded/default preset **code** is `"tchalanet"`. The web tolerates this (falls back to the default
> preset CSS), but the backend seed `presetId` should be aligned to a real preset code. Track until
> fixed.

## Backend token mapping (required)

`GET /api/v1/{public,tenant}/theme/runtime` (`ThemeRuntimeView.tokens`) returns **dotted, semantic**
token keys sourced from `theme_preset.config.tokens.<mode>` — e.g. `color.primary`, `color.secondary`,
`color.surface`, `color.onSurface`, `shape.radius.md`. These are **not** CSS variables. They MUST be
translated to the validated `--tch-*` set before being applied; applying them verbatim yields invalid
declarations like `--color.primary` that the browser silently ignores (backend theming never takes
effect).

The mapping lives in `libs/ui/theme/src/lib/theme-token-map.ts`
(`mapBackendThemeTokens`), applied in `ThemeApi`:

```text
color.primary    -> --tch-color-primary
color.secondary  -> --tch-color-secondary
color.surface    -> --tch-color-surface
color.onSurface  -> --tch-color-on-surface
shape.radius.md  -> --tch-radius-md
```

Backend overrides now target the **standard component-facing roles** (`--tch-color-on-surface`,
`--tch-radius-md`). The legacy aliases `--tch-color-foreground` and `--tch-radius-control` are
**derived from** those roles in `runtime-vars.scss`/`runtime-root.scss`
(`--tch-color-foreground: var(--tch-color-on-surface)`, `--tch-radius-control: var(--tch-radius-md)`),
so a tenant override cascades to both names without a double mapping. Prefer the standard role in new
code; the aliases remain only for existing consumers.

Rules: unmapped backend tokens are dropped (never emit invalid CSS); keys already shaped as `--tch-*`
pass through; tokens with no backend equivalent (`background`, `outline`, `primary-contrast`,
`surface-muted`) come from the active preset CSS. Extend the map **with a test** when the backend adds
a token the web needs — `theme-token-contract.spec.ts` fails if a mapping targets a token that is not
emitted.

**Typography follows M3 like colours**: `--tch-font-size-*` / `--tch-line-height-*` /
`--tch-letter-spacing-*` are bridged from the M3 type scale (`--mat-sys-{role}-size/line-height/
tracking`) in `runtime-vars.scss`, with the `:root` px values as first-paint fallback. The web adopts
the M3 type scale (so `display-lg` is M3's display-large, not a custom compact size). `--tch-weight-*`
remain static.

**Font keywords → stacks**: a tenant `typography.fontFamily` override arrives as a keyword
(`system`/`roboto`/`poppins`/`inter`); `theme-token-map.ts` resolves it to a real font stack so the
applied `--tch-font-family` is valid. `Plus Jakarta Sans` stays the brand default.

## CSS Token Rules

The **canonical list** of `--tch-*` tokens is generated from the SCSS sources of truth
(`runtime-root.scss` + `runtime-vars.scss`) into
`libs/ui/theme/src/registry/token-manifest.generated.ts` (`npm run tokens:generate`). Do not maintain
a parallel hand-written list — `theme-token-contract.spec.ts` keeps the manifest, the SCSS, and
`theme-token-map.ts` in sync and fails on drift.

Components use CSS variables, not hardcoded colors. Representative subset (see the manifest for the
full set):

```text
--tch-color-background
--tch-color-on-background
--tch-color-primary
--tch-color-on-primary
--tch-color-secondary
--tch-color-surface
--tch-color-on-surface
--tch-color-outline
--tch-color-error
--tch-radius-sm
--tch-radius-md
--tch-radius-lg
--tch-radius-pill
--tch-elevation-1
--tch-elevation-2
--tch-focus-ring-width
--tch-focus-ring-offset
--tch-page-max
--tch-page-gutter
--tch-font-family
```

`--tch-*` variables are global theme tokens. `--comp-*` variables are component-local extension
points and must fall back to `--tch-*`; they are not new global theme roles.

The official `tchalanet` brand palette (light) — the exact hexes are **pinned** on both `--mat-sys-*`
and `--tch-*` in `runtime-vars.scss` (`.tch-theme:not(.dark)[data-preset='tchalanet']`), so Material
and app components never diverge. Roles not listed here derive from the `#1A1B4B`-seeded M3 palette.

```text
primary              #1A1B4B   deep navy — brand sections, titles, key fills
onPrimary            #FFFFFF
primaryContainer     #2E3192   lighter navy — highlight sections
accent (= tertiary)  #FECB00   gold — primary CTA / accents  (NOT secondary)
onAccent             #241A00
background           #F9F9FC
surfaceContainerLowest #FFFFFF  cards/widgets
onSurface            #1A1C1E
onSurfaceVariant     #464652
header               #FFFFFF bg / #1A1C1E text   (white top bar)
footer               #1A1B4B bg / #FFFFFF text
fontFamily           Plus Jakarta Sans
```

The gold lives **only** in the accent role (M3 `tertiary`); `secondary` derives to a muted indigo.
This is the current Web reference. Mobile/POS alignment requires a separate Mobile-owned change;
this convention does not claim that Flutter already implements these values.

## Material 3 is the theming system

Angular Material (`@angular/material` + `@angular/cdk` v21) is a dependency and **M3 is the chosen
theming model**. Follow Material 3 best practices; do not strip Material from salvaged components.

The `--tch-*` tokens are a thin **brand indirection layer on top of M3 system tokens**, not a
replacement for them. The token chain is:

```text
M3 tonal palettes (per preset)            // ng generate @angular/material:theme-color
  -> mat.theme(...) emits --mat-sys-*      // full M3 system tokens, light + dark
    -> brand mapping --tch-*-base          // e.g. --tch-header-bg-base: var(--mat-sys-primary)
      -> runtime-derived --tch-*           // component-facing, incl. dark color-mix post-processing
```

Components consume `--tch-*` (and may consume `--mat-sys-*` directly where appropriate). Map Material
system tokens from the brand seeds, not the other way around.

## Preset generation pipeline (frontend-owned)

Full M3 preset CSS is **generated at build time on the frontend**, not emitted by the backend. Ported
owned by `libs/ui/theme`:

- `libs/ui/theme/src/scss/_generate-theme.scss` — `tch-generate-theme($id, $primary, $tertiary)` mixin: calls
  `mat.theme()` and maps brand tokens, for `.tch-theme[data-preset=$id]` (light) and
  `.tch-theme.dark[data-preset=$id]` (dark).
- `libs/ui/theme/src/scss/theme-presets.scss` — the preset catalog (tchalanet brand + Material palette pairs).
- `libs/ui/theme/src/scss/tchalanet/_theme-colors.scss` — brand tonal palettes (regenerate via Material schematic; do
  not hand-edit).
- the generator `libs/ui/theme/tools/generate-theme-registry.mjs` (run via `npm run theme:generate`)
  compiles the catalog with the `sass` CLI, splits per `data-preset`, and writes
  `libs/ui/theme/src/registry/theme-presets.registry.ts` (`THEME_PRESETS: ThemePreset[]`, committed + reviewable).
  Edit the catalog and regenerate — never hand-edit the registry.

Preset **ids mirror the backend `theme_preset.code`** (V203 seed: `tchalanet`, `m3-blue`, `m3-purple`,
…) so a backend `presetCode` always resolves to a generated preset. The `m3-*` presets currently
approximate each seeded hue with the nearest Material named palette; exact per-preset brand-hex
regeneration is a follow-up.

`ThemeRepository.list()` exposes the supported presets (loaded from the registry); this is the "list
of supported themes". Preset label keys are `theme.presets.<id>` and must exist in fr/en/ht.

## Runtime resolution (public default → tenant theme)

1. **Startup / public**: apply the default public preset.
2. **Tenant member signs in**: apply the tenant's theme (`presetId`, `mode`, `density`, and tenant
   `overrides`). Each tenant will have its own theme; v3 lets a tenant build its own M3 theme.
3. Apply order in the DOM (owned by `ThemeDomApplier`): generated preset CSS
   (`<style id=tch-theme-preset>`) → runtime state (`.tch-theme`/`.dark` classes, `data-preset` and
   `data-theme-density` on root/body/overlay, plus the **density** class
   `.tch-density-compact`/`.tch-density-dense`) → tenant **overrides**
   (`<style id=tch-theme-overrides>`). `toOverrideCss` emits the resolved tenant token map as plain
   `--tch-*: value` declarations scoped to `.tch-theme[data-preset='…']` — it does **not** yet support
   `--dark:`-prefixed variants or a custom `fontHref`. Those are a **future enhancement** (needed for
   tenant custom fonts), not current behavior.

### Density (runtime)

Density is **decoupled from presets** (M3 density only affects component metric tokens, so a single
set of classes serves every preset). The preset generation no longer bakes `density`; instead
`scss/density.scss` emits `mat.theme((density: -2|-4))` scoped to `.tch-density-compact` /
`.tch-density-dense`, and `ThemeDomApplier` toggles the class from the runtime `density`
(`comfortable` = default, no class). The backend `density.default` token flows through
`ThemeApi → RuntimeTheme.density` and is persisted like mode/preset.

### Backend's role (and where its tokens fit)

The backend does **not** generate full M3 token sets. It owns:

- which preset is active (`ThemeRuntimeView.presetCode` / `PageModelDoc.theme.presetId`), the default
  public preset (`isDefault`), and per-tenant selection;
- the **tenant-editable override subset** — the seeded preset `config.editableTokens`
  (`color.primary`, `color.secondary`, `shape.radius.md`, `typography.fontFamily`, `density.default`).
  `ThemeRuntimeView.tokens` carries the tenant's customized values for these, applied as **overrides**
  on top of the generated preset — they are not the whole palette.

`libs/ui/theme/src/lib/theme-token-map.ts` maps those editable backend keys to the CSS
variables the generated preset exposes. Keep it aligned with `editableTokens`; extend with a test
when the set grows.

## Anti-Patterns

Do not:

- embed theme objects inside PageModel;
- hardcode colors in feature components;
- let components call theme APIs directly;
- treat local preset tokens as tenant truth;
- make tenant-specific CSS in shared UI components.
