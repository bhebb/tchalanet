import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'io.tchalanet.mobile',
  appName: 'tchalanet-mobile',
  webDir: '../../dist/apps/tchalanet-mobile/browser',
  server: {
    androidScheme: 'https'
  }
};

export default config;
