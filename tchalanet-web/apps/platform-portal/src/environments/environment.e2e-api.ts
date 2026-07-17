import type { TchRuntimeConfig } from '@tch/shared-config';

// web-e2e API build: Firebase Auth SDK uses the local emulator, while runtime
// config is loaded from assets so REST calls can target the Docker API profile.
export const environment = {
  production: false,
  runtimeConfigPath: '/assets/config/runtime.platform-portal.json',
  fallbackConfig: {
    appId: 'platform-portal',
    production: false,
    apiBaseUrl: '/api/v1',
    authBaseUrl: '/auth',
    assetsBaseUrl: '/assets',
    portalBaseUrls: {
      'admin-portal': 'http://localhost:4302',
      'platform-portal': 'http://localhost:4303',
    },
    enableSandbox: true,
    firebaseAuthEmulatorUrl: 'http://localhost:9099',
    firebase: {
      apiKey: 'demo-emulator-key',
      authDomain: 'demo-tchalanet-local.firebaseapp.com',
      projectId: 'demo-tchalanet-local',
      storageBucket: 'demo-tchalanet-local.firebasestorage.app',
      messagingSenderId: '768000918177',
      appId: '1:768000918177:web:5b5b4339a9e41f089aa6e0',
      measurementId: 'G-HNNV2ZRWMJ',
    },
  } satisfies TchRuntimeConfig,
} as const;
