export type ThemeMode = 'light' | 'dark' | 'system';

export interface ThemePreset {
  readonly id: string;
  readonly labelKey: string;
  readonly css: string;
}

export interface RuntimeTheme {
  readonly activePresetKey: string;
  readonly mode: ThemeMode;
  readonly effectiveMode: Exclude<ThemeMode, 'system'>;
  readonly tokens: Readonly<Record<string, string>>;
}
