import { Environment } from './environment.types';

export const environment: Environment = {
  apiBase: 'https://api.tchalanet.com/api',
  authUrl: 'https://auth.tchalanet.com/realms/tchalanet',
  authClientId: 'tchalanet-web',
  apiVersion: 'v1',
  appVersion: '1',
  errorVersion: '1',
  apiBaseUrl: 'https://api.tchalanet.com',
  appUrl: 'https://app.tchalanet.com',
  analytics: {
    provider: 'ga',
    gaMeasurementId: '',
    autoTrack: true,
  },
  umami: {
    host: '',
    websiteId: '',
  },
  feature: {
    kind: 'unleash',
    url: 'https://flags.tchalanet.com/api/frontend',
    clientKey: '',
    appName: 'tchalanet-web',
    environment: 'production',
    refresh: 30,
    defaultValue: false,
  },
  tenant: 'public',
  lang: 'fr',
};
