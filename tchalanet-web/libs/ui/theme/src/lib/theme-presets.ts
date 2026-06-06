import { THEME_PRESETS } from '../registry/theme-presets.registry';
import { ThemePreset } from './theme-types';

export const defaultThemePresetId = 'tchalanet';

/**
 * Material 3 presets generated from the SCSS catalog.
 * Source: libs/ui/theme/src/scss/theme-presets.scss → tools/generate-theme-registry.mjs.
 * Ids mirror backend `theme_preset.code` (V203 seed).
 */
export const themePresets: readonly ThemePreset[] = THEME_PRESETS;
