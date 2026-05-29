import { Environment } from './environment.types';

export const environment: Environment = {
  apiBase: 'https://api.localtest.me/api',
  authUrl: 'https://auth.localtest.me/realms/tchalanet',
  authClientId: 'tchalanet-web',
  apiVersion: 'v1',
  appVersion: '1',
  errorVersion: '1',
  apiBaseUrl: 'https://api.localtest.me',
  appUrl: 'https://app.localtest.me',
  analytics: {
    provider: 'ga',
    gaMeasurementId: '',
    autoTrack: true,
  },
  feature: {
    kind: 'unleash',
    url: 'https://flags.localtest.me/api/frontend',
    clientKey: '',
    appName: 'tchalanet-web',
    environment: 'development',
    refresh: 10,
    defaultValue: true,
  },
  tenant: 'public',
  lang: 'fr',
};
