export interface FeatureConfig {
  kind: 'unleash' | 'memory';
  url: string;
  clientKey: string;
  appName: string;
  environment: string;
  refresh: number;
  defaultValue: boolean;
}

export interface AnalyticsConfig {
  provider: 'ga' | 'umami';
  gaMeasurementId: string;
  autoTrack: boolean;
}

export interface Environment {
  apiBase?: string;
  authUrl: string;
  authClientId: string;
  apiVersion: string;
  appVersion: string;
  errorVersion: string;
  apiBaseUrl: string;
  appUrl: string;
  analytics: AnalyticsConfig;
  feature: FeatureConfig;
  tenant: string;
  lang: string;
}



