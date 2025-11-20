/// <reference path="../../vite-env.d.ts" />

import { Environment } from './environment.types';

// Helper pour convertir string en boolean
const toBool = (val: string | undefined, defaultVal: boolean): boolean => {
  if (!val) return defaultVal;
  return val === 'true' || val === '1';
};

export const environment: Environment = {
  apiBase: import.meta.env.VITE_API_BASE || 'https://api.tchalanet.com/api',
  authUrl: import.meta.env.VITE_AUTH_URL || 'https://auth.tchalanet.com/realms/tchalanet',
  authClientId: import.meta.env.VITE_AUTH_CLIENT_ID || 'tchalanet-web',
  apiVersion: import.meta.env.VITE_API_VERSION || 'v1',
  appVersion: import.meta.env.VITE_APP_VERSION || '1',
  errorVersion: import.meta.env.VITE_ERROR_VERSION || '1',
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL || 'https://api.tchalanet.com',
  appUrl: import.meta.env.VITE_APP_URL || 'https://app.tchalanet.com',
  analytics: {
    provider: (import.meta.env.VITE_ANALYTICS_PROVIDER as 'ga' | 'umami') || 'ga',
    gaMeasurementId: import.meta.env.VITE_GA_MEASUREMENT_ID || '',
    autoTrack: toBool(import.meta.env.VITE_ANALYTICS_AUTO_TRACK, true),
  },
  feature: {
    kind: (import.meta.env.VITE_FEATURE_KIND as 'memory' | 'unleash') || 'unleash',
    url: import.meta.env.VITE_FEATURE_URL || '',
    clientKey: import.meta.env.VITE_FEATURE_CLIENT_KEY || '',
    appName: import.meta.env.VITE_FEATURE_APP_NAME || 'tchalanet-web',
    environment: import.meta.env.VITE_FEATURE_ENVIRONMENT || 'production',
    refresh: Number(import.meta.env.VITE_FEATURE_REFRESH) || 30,
    defaultValue: toBool(import.meta.env.VITE_FEATURE_DEFAULT_VALUE, false),
  },
  tenant: import.meta.env.VITE_TENANT || 'public',
  lang: import.meta.env.VITE_LANG || 'fr',
};
