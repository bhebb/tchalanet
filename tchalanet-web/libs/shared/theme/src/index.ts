// libs/shared/theme - runtime theme services, generated presets, and SCSS tokens.
// Import SCSS via stylePreprocessorOptions.includePaths in the consuming app's project.json:
//   "stylePreprocessorOptions": { "includePaths": ["libs/shared/theme/src"] }
// Then in styles.scss: @use 'scss/runtime-root'; @use 'scss/runtime-vars';

export * from './runtime/theme-api.service';
export * from './runtime/theme-dom.service';
export * from './runtime/theme-presets';
export * from './runtime/theme.repository';
export * from './runtime/theme-runtime.store';
export * from './runtime/theme-switcher.component';
export * from './runtime/theme-token-map';
export * from './runtime/theme.types';
