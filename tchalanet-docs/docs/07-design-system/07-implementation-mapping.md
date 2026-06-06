# 07 Implementation Mapping

> Status: normative  
> Scope: Angular/Web and Flutter implementation names

## Web CSS variable naming

Tokens must be available as CSS variables.

```css
--tch-color-primary
--tch-color-on-primary
--tch-color-secondary
--tch-color-surface
--tch-color-background
--tch-color-outline
--tch-color-on-surface
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

Current Web ownership:

```text
tchalanet-web/libs/ui/theme/       runtime theme and token application
tchalanet-web/libs/ui/styles/      shared SCSS primitives and global overrides
tchalanet-web/libs/ui/components/  reusable token-consuming components
```

Global theme roles use `--tch-*`. Components expose local `--comp-*` variables with a `--tch-*`
fallback.

## Flutter target naming

Mobile/POS should use a `TchTheme` / `TchColors` abstraction when their dedicated migration is
implemented. This is a target mapping, not a statement about the current Flutter code.

```dart
TchColors.primary
TchColors.onPrimary
TchColors.secondary
TchColors.surface
TchColors.background
TchColors.outline
TchColors.onSurface
```

## Raw value rule

No component should hardcode hex values directly.

Correct:

```dart
color: TchColors.primary
```

Wrong:

```dart
color: Color(0xFF1A1B4B)
```

Correct:

```scss
background: var(--tch-color-primary);
```

Wrong:

```scss
background: #1A1B4B;
```
