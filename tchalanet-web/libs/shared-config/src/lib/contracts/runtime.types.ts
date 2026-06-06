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
