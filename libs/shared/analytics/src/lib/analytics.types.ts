export type AnalyticsProvider = 'umami' | 'console';

export interface AnalyticsConfig {
  provider: AnalyticsProvider;
  // Umami
  umami?: {
    host: string; // ex: http://localhost:3300
    websiteId: string; // ex: 'XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX'
    autoTrack?: boolean; // false (on gère nous-mêmes)
  };
  // global
  debug?: boolean; // log console en plus
}

export interface AnalyticsEvent {
  name: string;
  data?: Record<string, unknown>;
}
