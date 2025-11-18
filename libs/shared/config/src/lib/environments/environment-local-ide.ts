import { Environment } from './environment.types';

export const environment: Environment = {
  apiBase: 'http://localhost:8083/api',
  authUrl: 'https://auth.localtest.me/realms/tchalanet',
  authClientId: 'tchalanet-web',
  apiVersion: 'v1',
  appVersion: '1',
  errorVersion: '1',
  apiBaseUrl: 'http://localhost:8083',
  appUrl: 'https://app.localtest.me',
  analytics: {
    provider: 'ga',
    gaMeasurementId: '',
    autoTrack: true,
  },
  feature: {
    kind: 'unleash',
    url: 'https://flags.localtest.me/api/frontend',
    clientKey: '*:development.78ab7cdd8be7c7b39f1ccc9aae52911d40dd13122edc6fb14151bc88',
    appName: 'tchalanet-web',
    environment: 'development',
    refresh: 10,
    defaultValue: true,
  },
  tenant: 'public',
  lang: 'fr',
};
