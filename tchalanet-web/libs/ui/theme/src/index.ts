// libs/ui/theme - runtime theme services, generated presets, and SCSS tokens.
// Import SCSS via stylePreprocessorOptions.includePaths in the consuming app's project.json:
//   "stylePreprocessorOptions": { "includePaths": ["libs/ui/theme/src"] }
// Then in styles.scss: @use 'scss/runtime-root'; @use 'scss/runtime-vars';

export * from './lib/theme-api';
export * from './lib/theme-dom-applier';
export * from './lib/theme-presets';
export * from './lib/theme.repository';
export * from './lib/theme-store';
export * from './lib/theme-switcher';
export * from './lib/theme-token-map';
export * from './lib/theme-types';
