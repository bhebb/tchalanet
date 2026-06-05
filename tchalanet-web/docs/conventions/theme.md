# Theme Convention

> Status: DRAFT v0.2
> Scope: runtime theme, presets, backend overrides, token mapping
> Living doc — update in the same commit as any code that changes a rule here.

## Rule

Theme is a runtime capability. The active theme is resolved from backend runtime services, with a local preset registry used as fallback and development support.

The backend owns the active theme ID. The frontend owns how known preset IDs become CSS.

## Placement

```text
apps/tch-portal/src/app/core/theme/
```

Use this area for:

- `ThemeApi` runtime calls;
- `ThemeRepository` local preset registry;
- `ThemeRuntimeStore` active preset/mode state;
- DOM application of CSS variables and Material token aliases;
- small theme controls used by shell or dev surfaces.

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

The mapping lives in `core/theme/theme-token-map.ts` (`mapBackendThemeTokens`), applied in
`ThemeApi`:

```text
color.primary    -> --tch-color-primary
color.secondary  -> --tch-color-secondary
color.surface    -> --tch-color-surface
color.onSurface  -> --tch-color-foreground
shape.radius.md  -> --tch-radius-control
```

Rules: unmapped backend tokens are dropped (never emit invalid CSS); keys already shaped as `--tch-*`
pass through; tokens with no backend equivalent (`background`, `outline`, `primary-contrast`,
`surface-muted`) come from the active preset CSS. Extend the map (with a test) when the backend adds a
token the web needs.

## CSS Token Rules

Components use CSS variables, not hardcoded colors:

```text
--tch-color-background
--tch-color-foreground
--tch-color-primary
--tch-color-primary-contrast
--tch-color-secondary
--tch-color-surface
--tch-color-surface-muted
--tch-color-outline
--tch-radius-control
```

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
from `web-backup/libs/ui/theme`:

- `scss/_theme-presets.scss` — `tch-generate-theme($id, $primary, $tertiary)` mixin: calls
  `mat.theme()` and maps brand tokens, for `.tch-theme[data-preset=$id]` (light) and
  `.tch-theme.dark[data-preset=$id]` (dark).
- `scss/theme-presets.scss` — the preset catalog (tchalanet brand + Material palette pairs).
- `scss/tchalanet/_theme-colors.scss` — brand tonal palettes (regenerate via Material schematic; do
  not hand-edit).
- the generator `apps/tch-portal/tools/generate-theme-registry.mjs` (run via `npm run theme:generate`)
  compiles the catalog with the `sass` CLI, splits per `data-preset`, and writes
  `core/theme/theme-presets.registry.ts` (`THEME_PRESETS: ThemePreset[]`, committed + reviewable).
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
3. Apply order in the DOM: generated preset CSS (`<style id=tch-theme-base>`, `data-preset` attr)
   → runtime state (`.dark` class, `mat-density-*`) → tenant **overrides** (`<style id=tch-theme-overrides>`,
   supports `--dark:`-prefixed vars + custom `fontHref`).

### Backend's role (and where its tokens fit)

The backend does **not** generate full M3 token sets. It owns:

- which preset is active (`ThemeRuntimeView.presetCode` / `PageModelDoc.theme.presetId`), the default
  public preset (`isDefault`), and per-tenant selection;
- the **tenant-editable override subset** — the seeded preset `config.editableTokens`
  (`color.primary`, `color.secondary`, `shape.radius.md`, `typography.fontFamily`, `density.default`).
  `ThemeRuntimeView.tokens` carries the tenant's customized values for these, applied as **overrides**
  on top of the generated preset — they are not the whole palette.

`core/theme/theme-token-map.ts` maps those editable backend keys to the CSS variables the generated
preset exposes. Keep it aligned with `editableTokens`; extend with a test when the set grows.

## Anti-Patterns

Do not:

- embed theme objects inside PageModel;
- hardcode colors in feature components;
- let components call theme APIs directly;
- treat local preset tokens as tenant truth;
- make tenant-specific CSS in shared UI components.
