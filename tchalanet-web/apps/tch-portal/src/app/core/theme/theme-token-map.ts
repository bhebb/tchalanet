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
const BACKEND_TOKEN_TO_CSS_VAR: Readonly<Record<string, string>> = {
  'color.primary': '--tch-color-primary',
  'color.secondary': '--tch-color-secondary',
  'color.surface': '--tch-color-surface',
  'color.onSurface': '--tch-color-foreground',
  'shape.radius.md': '--tch-radius-control',
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
