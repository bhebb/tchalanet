/**
 * Maps backend theme-runtime token keys to the web's validated CSS custom properties.
 *
 * The backend (`GET /api/v1/{public,tenant}/theme/runtime`, `ThemeRuntimeView.tokens`) emits
 * dotted, semantic token names sourced from the seeded `theme_preset.config.tokens.<mode>`
 * (e.g. `color.primary`, `color.onSurface`, `shape.radius.md`). The web styles exclusively
 * against the validated `--tch-*` token set. Without this translation the raw keys would be
 * applied verbatim as `--color.primary` — an invalid declaration that the browser ignores,
 * so backend-driven theming would silently never take effect.
 *
 * Only mapped keys are emitted. Unmapped backend tokens (e.g. `typography.fontFamily`,
 * `density.default`) are dropped so we never inject invalid CSS; the validated tokens they
 * don't cover (background, outline, primary-contrast, surface-muted) fall back to the active
 * preset CSS. Keys that already look like `--tch-*` are passed through untouched.
 */
export const BACKEND_TOKEN_TO_CSS_VAR: Readonly<Record<string, string>> = {
  'color.background': '--tch-color-background',
  'color.onBackground': '--tch-color-on-background',
  'color.primary': '--tch-color-primary',
  'color.primaryStrong': '--tch-color-primary-strong',
  'color.onPrimary': '--tch-color-on-primary',
  'color.primaryContainer': '--tch-color-primary-container',
  'color.onPrimaryContainer': '--tch-color-on-primary-container',
  'color.primaryFixed': '--tch-color-primary-fixed',
  'color.primaryFixedDim': '--tch-color-primary-fixed-dim',
  'color.onPrimaryFixed': '--tch-color-on-primary-fixed',
  'color.onPrimaryFixedVariant': '--tch-color-on-primary-fixed-variant',
  'color.secondary': '--tch-color-secondary',
  'color.onSecondary': '--tch-color-on-secondary',
  'color.secondaryContainer': '--tch-color-secondary-container',
  'color.onSecondaryContainer': '--tch-color-on-secondary-container',
  'color.secondaryFixed': '--tch-color-secondary-fixed',
  'color.secondaryFixedDim': '--tch-color-secondary-fixed-dim',
  'color.surface': '--tch-color-surface',
  'color.surfaceBright': '--tch-color-surface-bright',
  'color.surfaceContainerLowest': '--tch-color-surface-container-lowest',
  'color.surfaceContainerLow': '--tch-color-surface-container-low',
  'color.surfaceContainer': '--tch-color-surface-container',
  'color.surfaceContainerHigh': '--tch-color-surface-container-high',
  'color.surfaceContainerHighest': '--tch-color-surface-container-highest',
  'color.surfaceVariant': '--tch-color-surface-variant',
  'color.surfaceTonal': '--tch-color-surface-tonal',
  'color.onSurface': '--tch-color-on-surface',
  'color.onSurfaceVariant': '--tch-color-on-surface-variant',
  'color.outline': '--tch-color-outline',
  'color.outlineVariant': '--tch-color-outline-variant',
  'color.error': '--tch-color-error',
  'color.onError': '--tch-color-on-error',
  'color.errorContainer': '--tch-color-error-container',
  'color.onErrorContainer': '--tch-color-on-error-container',
  'color.statusReady': '--tch-color-status-ready',
  'color.statusWarning': '--tch-color-status-warning',
  'color.statusBlocked': '--tch-color-status-blocked',
  'color.statusMissing': '--tch-color-status-missing',
  'color.orangeAccent': '--tch-color-orange-accent',
  'typography.fontFamily': '--tch-font-family',
  'shape.radius.sm': '--tch-radius-sm',
  'shape.radius.md': '--tch-radius-md',
  'shape.radius.lg': '--tch-radius-lg',
  'shape.radius.xl': '--tch-radius-xl',
};

export function mapBackendThemeTokens(
  tokens: Readonly<Record<string, string>>,
): Readonly<Record<string, string>> {
  const mapped: Record<string, string> = {};

  for (const [key, value] of Object.entries(tokens)) {
    const target = key.startsWith('--tch-') ? key : BACKEND_TOKEN_TO_CSS_VAR[key];
    if (target) {
      mapped[target] = value;
    }
  }

  return mapped;
}
