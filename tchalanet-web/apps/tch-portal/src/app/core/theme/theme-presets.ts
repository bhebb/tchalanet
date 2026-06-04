import { ThemePreset } from '../../shared/types';
import { THEME_PRESETS } from './theme-presets.registry';

export const defaultThemePresetId = 'tchalanet';

/**
 * Material 3 presets generated from the SCSS catalog.
 * Source: core/theme/scss/theme-presets.scss → tools/generate-theme-registry.mjs.
 * Ids mirror backend `theme_preset.code` (V203 seed).
 */
export const themePresets: readonly ThemePreset[] = THEME_PRESETS;
