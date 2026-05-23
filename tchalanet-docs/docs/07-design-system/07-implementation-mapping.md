# 07 Implementation Mapping

> Status: normative  
> Scope: Angular/Web and Flutter implementation names

## Web CSS variable naming

Tokens must be available as CSS variables.

```css
--tch-color-primary
--tch-color-primary-strong
--tch-color-on-primary
--tch-color-secondary
--tch-color-tertiary
--tch-color-surface
--tch-color-surface-bright
--tch-color-background
--tch-color-outline
--tch-color-on-surface
```

## Flutter naming

Use a `TchTheme` / `TchColors` abstraction.

```dart
TchColors.primary
TchColors.primaryStrong
TchColors.onPrimary
TchColors.secondary
TchColors.tertiary
TchColors.surface
TchColors.surfaceBright
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
color: Color(0xFF5E89EF)
```

Correct:

```scss
background: var(--tch-color-primary);
```

Wrong:

```scss
background: #5E89EF;
```
