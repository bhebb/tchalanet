import type { TchRuntimeConfig } from '@tch/shared-config';

export const environment = {
  production: true,
  runtimeConfigPath: '/assets/config/runtime.admin-portal.json',
  fallbackConfig: {
    appId: 'admin-portal',
    production: true,
    apiBaseUrl: '/api/v1',
    authBaseUrl: '/auth',
    assetsBaseUrl: '/assets',
    enableSandbox: false,
    firebaseAuthEmulatorUrl: null,
    firebase: {
      apiKey: 'AIzaSyBjzqh2M5H43VBVYEu5Qo0E8fnGj82tiLw',
      authDomain: 'tchalanet.firebaseapp.com',
      projectId: 'tchalanet',
      storageBucket: 'tchalanet.firebasestorage.app',
      messagingSenderId: '1050094456835',
      appId: '1:1050094456835:web:600c1fa5874aa0039a3e36',
      measurementId: 'G-8K6B6814J3',
    },
  } satisfies TchRuntimeConfig,
} as const;
