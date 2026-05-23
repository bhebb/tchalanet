# 02 Design Tokens — Web, Mobile & POS

> Status: normative  
> Scope: colors, typography, spacing, radius, borders, effects  
> Source: normalized from current Figma/Stitch exploration

Do not define many one-off colors. Define roles, then use the roles everywhere.

## 1. Material-compatible ColorScheme

Tchalanet uses Material 3 color roles as naming contract and extends them with operational semantic tokens.

### Brand roles

| Material role | Token | Value | Usage |
| --- | --- | ---: | --- |
| Primary | `primary` | `#5E89EF` | Main product action: sell, confirm, validate. |
| Primary strong | `primaryStrong` | `#2457BA` | Header/top app bar, high-contrast brand surfaces. |
| On primary | `onPrimary` | `#FFFFFF` | Text/icons on primary buttons and critical action surfaces. |
| On primary soft | `onPrimarySoft` | `#F2F3F5` | Softer foreground on large primary/strong surfaces. |
| Primary container | `primaryContainer` | `#DAE2FF` | Soft primary selected backgrounds. |
| On primary container | `onPrimaryContainer` | `#001847` | Text/icons on primary container. |
| Secondary | `secondary` | `#6F5EEF` | Secondary brand/action: verify, active nav, visual support. |
| On secondary | `onSecondary` | `#FCFDFF` | Text/icons on secondary surfaces. |
| Secondary container | `secondaryContainer` | `#E4DFFF` | Soft purple surfaces. |
| On secondary container | `onSecondaryContainer` | `#160066` | Text/icons on secondary container. |
| Tertiary / Accent | `tertiary` | `#FF7844` | Marketing CTA, promo, financial attention when appropriate. |
| On tertiary | `onTertiary` | `#FCFDFF` | Text/icons on orange surfaces. |
| Tertiary container | `tertiaryContainer` | `#FFDBCF` | Soft orange/promo surfaces. |
| On tertiary container | `onTertiaryContainer` | `#380D00` | Text/icons on tertiary container. |

### Surface roles

| Role | Token | Value | Usage |
| --- | --- | ---: | --- |
| Background | `background` | `#F7F9FB` | General app background. |
| Surface | `surface` | `#F3EFF2` | Material soft surface, panels, muted blocks. |
| Surface bright | `surfaceBright` | `#FFFFFF` | Cards, important readable blocks. |
| Surface container | `surfaceContainer` | `#ECEEF0` | POS panels, muted operational zones. |
| Surface container high | `surfaceContainerHigh` | `#E8E7F0` | Grouped panels and navigation surfaces. |
| On surface | `onSurface` | `#1D1B20` | Main text on surfaces. |
| On surface variant | `onSurfaceVariant` | `#49454F` | Secondary text, labels, helper text. |
| Outline | `outline` | `#CAC4D0` | Standard borders. |
| Outline strong | `outlineStrong` | `#79767D` | Selected outline, stronger separators. |

### Semantic status colors

Semantic colors are not marketing colors.

| Role | Token | Value | Usage |
| --- | --- | ---: | --- |
| Success | `success` | `#006C49` | Sale accepted, session open, sync OK. |
| Success container | `successContainer` | `#DDFBEA` | Soft success badge/panel. |
| Warning | `warning` | `#B26A00` | Offline, limit near, confirmation required. |
| Warning container | `warningContainer` | `#FFF2D6` | Soft warning panel. |
| Error | `error` | `#BA1A1A` | Rejected, blocked, destructive action. |
| On error | `onError` | `#FFFFFF` | Text/icons on error. |
| Error container | `errorContainer` | `#FFEDEA` | Soft error panel. |

## 2. Usage rules

### Brand usage

| Color | Use for | Do not use for |
| --- | --- | --- |
| Blue primary | sell, confirm, validate, primary links | danger, warning, decorative noise |
| Purple secondary | verify, active nav, secondary strong actions | main sell action everywhere |
| Orange tertiary | landing CTA, promo, financial attention | normal POS actions |
| Green success | status OK only | normal action buttons |
| Red error | danger/reject/block | normal emphasis |

### POS orange rule

In POS, orange is allowed only for:

```text
financial attention
warning
money to pay / exposure / risk
```

Example:

```text
Gains à payer = orange acceptable
```

## 3. Typography

### Font

```text
Product font: Inter
Fallback: system sans-serif
Roboto: only temporary Material fallback if not themed yet
```

### Type scale

| Style | Size | Weight | Usage |
| --- | ---: | ---: | --- |
| `display` | 40-48 | 700/800 | Landing hero only. |
| `headlineLarge` | 32 | 700 | Page title, major dashboard title. |
| `headlineMedium` | 24 | 600/700 | Mobile screen title. |
| `titleLarge` | 22 | 600 | Card or section title. |
| `titleMedium` | 18 | 600 | Subsection, list header. |
| `bodyLarge` | 16 | 400/500 | Standard text. |
| `bodyMedium` | 14 | 400/500 | Secondary body text. |
| `labelLarge` | 14 | 600/700 | Buttons, tabs. |
| `labelMedium` | 12 | 600 | Section labels, chips. |
| `numericHuge` | 56-72 | 800 | POS daily total. |
| `numericLarge` | 32-40 | 700 | Important amounts. |
| `numericMedium` | 22-28 | 600/700 | Cards, transaction amounts. |
| `codeHero` | 48-64 | 800 | Ticket/client verification code. |

### Typography rules

- POS money totals use `numericHuge` or `numericLarge`.
- Amount labels must be less dominant than amount values.
- Avoid thin font weights in POS.
- Uppercase is allowed for POS primary action and compact labels, but not every normal sentence.

## 4. Spacing

Use a 4 px grid.

| Token | Value |
| --- | ---: |
| `space4` | 4 |
| `space8` | 8 |
| `space12` | 12 |
| `space16` | 16 |
| `space20` | 20 |
| `space24` | 24 |
| `space32` | 32 |
| `space40` | 40 |
| `space48` | 48 |
| `space64` | 64 |

### POS dimensions

| Element | Recommended |
| --- | --- |
| Screen margin | 16 px |
| Standard gap | 16-24 px |
| Header height | 64-80 px |
| Primary POS action height | 180-240 px |
| Secondary POS action height | 140-180 px |
| Sync action height | 80-96 px |
| Bottom nav height | 72-88 px |
| Touch target minimum | 44 px |

## 5. Radius

| Token | Value | Usage |
| --- | ---: | --- |
| `radiusXs` | 4 | Tiny badges. |
| `radiusSm` | 8 | Chips, small controls. |
| `radiusMd` | 12 | Buttons, cards. |
| `radiusLg` | 16 | POS panels, major cards. |
| `radiusXl` | 24 | Landing hero blocks. |
| `radiusPill` | 999 | Pills. |

## 6. Borders

| Token | Value |
| --- | --- |
| `borderHairline` | `1px solid outline` |
| `borderSelected` | `2px solid primary` |
| `borderWarning` | `2px solid tertiary` |
| `borderError` | `1px solid error` |

## 7. Effects

Effects must support hierarchy and feedback, not decorate the UI.

### Elevation

| Token | Value | Usage |
| --- | --- | --- |
| `elevation0` | `none` | Default POS surfaces. |
| `elevation1` | `0 1px 2px rgba(0,0,0,0.08)` | Small cards. |
| `elevation2` | `0 2px 6px rgba(0,0,0,0.10)` | Header, bottom action bar. |
| `elevation3` | `0 6px 16px rgba(0,0,0,0.12)` | Dialogs, sheets, popovers. |
| `elevation4` | `0 12px 28px rgba(0,0,0,0.14)` | Rare landing hero/overlay only. |

### State layers

| State | Token | Value |
| --- | --- | --- |
| Hover | `stateHover` | `rgba(29, 27, 32, 0.08)` |
| Pressed | `statePressed` | `rgba(29, 27, 32, 0.12)` |
| Focus | `stateFocus` | `rgba(94, 137, 239, 0.16)` |
| Disabled opacity | `disabledOpacity` | `0.38` |
| Disabled container opacity | `disabledContainerOpacity` | `0.12` |

### Focus ring

| Token | Value |
| --- | --- |
| `focusRingColor` | `#5E89EF` |
| `focusRingWidth` | `2px` |
| `focusRingOffset` | `2px` |

Focus must never be removed.
