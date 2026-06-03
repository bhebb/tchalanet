# Theme Convention

> Status: DRAFT v0.1  
> Scope: runtime theme, presets, backend overrides, Material tokens

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

Material aliases are mapped from Tchalanet tokens where needed:

```text
--mat-sys-primary
--mat-sys-on-primary
--mat-sys-secondary
--mat-sys-surface
--mat-sys-background
--mat-sys-on-surface
--mat-sys-outline
```

## Registry Generation

The V1 registry may be compact and local. The target model is a generated Material-compatible registry, inspired by the previous backup flow.

Generation scripts may produce registry CSS, but generated output must stay reviewable and documented.

## Anti-Patterns

Do not:

- embed theme objects inside PageModel;
- hardcode colors in feature components;
- let components call theme APIs directly;
- treat local preset tokens as tenant truth;
- make tenant-specific CSS in shared UI components.
