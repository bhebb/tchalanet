import type { TchRuntimeConfig } from '@tch/shared-config';

export const environment = {
  production: true,
  runtimeConfigPath: '/assets/config/runtime.public-portal.json',
  fallbackConfig: {
    appId: 'public-portal',
    production: true,
    apiBaseUrl: '/api/v1',
    authBaseUrl: '/auth',
    assetsBaseUrl: '/assets',
    enableSandbox: false,
    firebaseAuthEmulatorUrl: null,
    firebase: {
      apiKey: 'AIzaSyCbR2gsuZioYJUVeGk7AvtLDgWHveiHYnc',
      authDomain: 'tchalanet-39115.firebaseapp.com',
      projectId: 'tchalanet-39115',
      storageBucket: 'tchalanet-39115.firebasestorage.app',
      messagingSenderId: '768000918177',
      appId: '1:768000918177:web:5b5b4339a9e41f089aa6e0',
      measurementId: 'G-HNNV2ZRWMJ',
    },
  } satisfies TchRuntimeConfig,
} as const;
