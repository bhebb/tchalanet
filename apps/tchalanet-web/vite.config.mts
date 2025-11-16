/// <reference types='vitest' />
import angular from '@analogjs/vite-plugin-angular';
import { nxCopyAssetsPlugin } from '@nx/vite/plugins/nx-copy-assets.plugin';
import { nxViteTsPaths } from '@nx/vite/plugins/nx-tsconfig-paths.plugin';
import { defineConfig } from 'vite';

export default defineConfig(() => {
  const apiTarget = process.env['TCH_API_TARGET'] ?? 'http://localhost:8080';
  return {
    root: __dirname,
    cacheDir: '../../node_modules/.vite/apps/tchalanet-web',
    plugins: [angular(), nxViteTsPaths(), nxCopyAssetsPlugin(['*.md'])],
    define: {
      // allow injecting GA ID at build time via GA_MEASUREMENT_ID (compose.env) or VITE_GA_MEASUREMENT_ID
      'import.meta.env.VITE_GA_MEASUREMENT_ID': JSON.stringify(process.env['GA_MEASUREMENT_ID'] || process.env['VITE_GA_MEASUREMENT_ID'] || ''),
    },
    server: {
      proxy: {
        '/api': {
          target: apiTarget,
          changeOrigin: true,
          secure: false,
        },
      },
    },
    test: {
      name: 'tchalanet-web',
      watch: false,
      globals: true,
      environment: 'jsdom',
      include: ['{src,tests}/**/*.{test,spec}.{js,mjs,cjs,ts,mts,cts,jsx,tsx}'],
      setupFiles: ['src/test-setup.ts'],
      reporters: ['default'],
      coverage: {
        reportsDirectory: '../../coverage/apps/tchalanet-web',
        provider: 'v8' as const,
      },
    },
  };
});
