import { Environment } from './environment.types';

export const environment: Environment = {
  apiBase: 'https://api.stg.tchalanet.com/api',
  authUrl: 'https://auth.stg.tchalanet.com/realms/tchalanet',
  authClientId: 'tchalanet-web',
  apiVersion: 'v1',
  appVersion: '1',
  errorVersion: '1',
  apiBaseUrl: 'https://api.stg.tchalanet.com',
  appUrl: 'https://app.stg.tchalanet.com',
  analytics: {
    provider: 'ga',
    gaMeasurementId: '',
    autoTrack: true,
  },
  feature: {
    kind: 'unleash',
    url: 'https://flags.stg.tchalanet.com/api/frontend',
    clientKey: '',
    appName: 'tchalanet-web',
    environment: 'staging',
    refresh: 15,
    defaultValue: false,
  },
  tenant: 'public',
  lang: 'fr',
};
