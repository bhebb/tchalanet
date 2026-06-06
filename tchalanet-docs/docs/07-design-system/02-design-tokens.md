# 02 Design Tokens — Web Reference

> Status: normative  
> Scope: shared semantic roles, with the current Web foundation as active reference
> Runtime source: `tchalanet-web/libs/ui/theme/src/scss/runtime-root.scss`

Define semantic roles, not one-off component colors. The values below describe the current Web
foundation. Mobile and POS adoption requires a separate Mobile-owned change.

## 1. Color roles

### Brand and action roles

| Role | Value | Usage |
| --- | ---: | --- |
| `primary` | `#1A1B4B` | Brand chrome, primary surfaces, strong navigation. |
| `primaryStrong` | `#15157D` | Strong emphasis where additional contrast is required. |
| `onPrimary` | `#FFFFFF` | Text and icons on primary surfaces. |
| `primaryContainer` | `#2E3192` | Selected or supporting indigo surfaces. |
| `onPrimaryContainer` | `#E1E0FF` | Text and icons on primary containers. |
| `secondary` | `#745B00` | Foreground role related to the action accent. |
| `secondaryContainer` | `#FECB00` | Secondary/action accent surfaces. |
| `onSecondaryContainer` | `#241A00` | Text and icons on the yellow action accent. |
| `orangeAccent` | `#F7931E` | Marketing or operational attention when semantically appropriate. |

### Surface roles

| Role | Value | Usage |
| --- | ---: | --- |
| `background` / `surface` | `#F9F9FC` | Page and standard surface background. |
| `surfaceContainerLowest` | `#FFFFFF` | Highest-contrast readable cards. |
| `surfaceContainer` | `#EDEEF1` | Grouped panels and muted zones. |
| `surfaceContainerHigh` | `#E8E8EB` | Navigation and stronger grouped surfaces. |
| `onSurface` | `#1A1C1E` | Main text. |
| `onSurfaceVariant` | `#464652` | Secondary text and labels. |
| `outline` | `#777683` | Standard borders and separators. |
| `outlineVariant` | `#C7C5D4` | Subtle borders and separators. |
| `error` | `#BA1A1A` | Rejected, blocked, destructive, or invalid states. |

Status colors remain semantic: success is green, warning is warning/amber, and error is red. Brand
colors must not replace status meaning.

## 2. Web CSS token contract

Components consume standardized global tokens:

```text
--tch-color-surface
--tch-color-on-surface
--tch-color-primary
--tch-color-on-primary
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

`--tch-*` defines global theme roles. A component exposes local `--comp-*` variables with fallbacks
to these global tokens. Do not introduce global aliases such as `--color-primary`, `--radius`,
`--header-bg`, or `--footer-bg`.

## 3. Typography

```text
Product font: Plus Jakarta Sans
Fallback: system-ui, -apple-system, Segoe UI, Roboto, Arial, sans-serif
```

Money totals, ticket codes, references, and operational values must remain more prominent than
their labels. Avoid thin weights on operational surfaces.

## 4. Spacing and dimensions

Use a 4 px spacing grid. Standard gaps are 8, 12, 16, 24, 32, 40, 48, and 64 px. Touch targets must
be at least 44 px.

Page width and gutter are controlled by `--tch-page-max` and `--tch-page-gutter`, not by duplicated
component constants.

## 5. Radius

| Token | Value | Usage |
| --- | ---: | --- |
| `radiusSm` | `4px` | Small controls and badges. |
| `radiusMd` | `8px` | Standard controls and cards. |
| `radiusLg` | `12px` | Large cards and panels. |
| `radiusXl` | `24px` | Large expressive blocks. |
| `radiusPill` | `9999px` | Pills and fully rounded controls. |

Temporary aliases such as a control radius must resolve to these standardized roles.

## 6. Effects and focus

Elevation supports hierarchy only. Use `--tch-elevation-1` for standard raised cards and
`--tch-elevation-2` for stronger shell surfaces; dialogs and overlays may define higher levels in
the theme.

Focus must remain visible and use `--tch-focus-ring-width` and `--tch-focus-ring-offset`. Components
must not remove focus indication.
