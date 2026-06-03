export interface FeatureFlag {
  readonly key: string;
  readonly enabled: boolean;
  readonly description?: string;
}

export interface FeatureToggle {
  readonly key: string;
  readonly enabled: boolean;
  readonly source: 'default' | 'runtime';
}

export interface RuntimeSettings {
  readonly featureFlags: Readonly<Record<string, FeatureFlag>>;
  readonly values: Readonly<Record<string, unknown>>;
  readonly loadedAt?: string;
}

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
