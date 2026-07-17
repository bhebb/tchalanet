import type { TchRuntimeConfig } from '@tch/shared-config';

// web-e2e emulator build: connect the Firebase Auth SDK to the local emulator
// (:9099, project demo-tchalanet-local). Used by `nx serve … --configuration=emulator`.
export const environment = {
  production: false,
  runtimeConfigPath: '/assets/config/runtime.public-portal.json',
  fallbackConfig: {
    appId: 'public-portal',
    production: false,
    apiBaseUrl: '/api/v1',
    authBaseUrl: '/auth',
    assetsBaseUrl: '/assets',
    portalBaseUrls: {
      'admin-portal': 'http://localhost:4302',
      'platform-portal': 'http://localhost:4202',
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
