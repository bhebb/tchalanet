export type AnalyticsProvider = 'ga' | 'console';

export interface AnalyticsConfig {
  provider: AnalyticsProvider;
  // Google Analytics (gtag)
  ga?: {
    measurementId?: string; // ex: G-XXXXXXX
    autoTrack?: boolean;
  };
  // global
  debug?: boolean; // log console en plus
}

export interface AnalyticsEvent {
  name: string;
  data?: Record<string, unknown>;
}
