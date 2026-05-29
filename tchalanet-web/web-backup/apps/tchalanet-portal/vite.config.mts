/// <reference types="vitest" />
import path from 'node:path';
import angular from '@analogjs/vite-plugin-angular';
import { nxCopyAssetsPlugin } from '@nx/vite/plugins/nx-copy-assets.plugin';
import { nxViteTsPaths } from '@nx/vite/plugins/nx-tsconfig-paths.plugin';
import { defineConfig } from 'vite';

export default defineConfig(({ mode }) => {
  const apiTarget = process.env['TCH_API_TARGET'] ?? 'http://localhost:8083';
  const authTarget = process.env['TCH_AUTH_TARGET'] ?? 'http://auth.localtest.me:8082';

  console.log('>>> tchalanet-portal Vite config.mts LOADED');
  console.log(`>>> Mode: ${mode}`);
  console.log(`>>> API target: ${apiTarget}`);
  console.log(`>>> Auth target: ${authTarget}`);

  return {
    root: __dirname,
    cacheDir: '../../node_modules/.vite/apps/tchalanet-portal',

    plugins: [
      angular(),
      nxViteTsPaths(),
      nxCopyAssetsPlugin(['*.md']),
    ],

    // Dossier public (apps/tchalanet-portal/public)
    publicDir: path.resolve(__dirname, 'public'),

    define: {
      'import.meta.env.VITE_GA_MEASUREMENT_ID': JSON.stringify(
        process.env['GA_MEASUREMENT_ID'] ||
        process.env['VITE_GA_MEASUREMENT_ID'] ||
        '',
      ),
    },

    // SCSS : on permet @use 'libs/...'
    css: {
      preprocessorOptions: {
        scss: {
          api: 'modern',
          // Depuis root: __dirname (apps/tchalanet-portal), remonter de 2 niveaux vers la racine du workspace web
          includePaths: [
            path.resolve(__dirname, '../../'),
            path.resolve(__dirname, '../../libs'),
          ],
        },
      },
    },

    // Alias TS/Sass pour 'libs/...'
    resolve: {
      alias: {
        'libs': path.resolve(__dirname, '../../libs'),
        '~libs': path.resolve(__dirname, '../../libs'),
      },
    },

    server: {
      host: '0.0.0.0',
      port: 4200,
      strictPort: true,
      allowedHosts: [
        'localhost',
        '127.0.0.1',
        '.localtest.me',
        'app.localtest.me',
        'mob.localtest.me',
      ],
      proxy: {
        '/api': {
          target: apiTarget,
          changeOrigin: true,
          secure: false,
          // Garder le préfixe /api (le backend l'attend)
          rewrite: undefined,
          configure: (proxy, _options) => {
            proxy.on('error', (err, _req, _res) => {
              console.log('[Vite Proxy] ❌ Error:', err.message);
            });
            proxy.on('proxyReq', (proxyReq, req, _res) => {
              console.log(`[Vite Proxy] → ${req.method} ${req.url} => ${apiTarget}${req.url}`);
            });
            proxy.on('proxyRes', (proxyRes, req, _res) => {
              console.log(`[Vite Proxy] ← ${req.method} ${req.url} [${proxyRes.statusCode}]`);
            });
          },
        },
        // Proxy ciblés pour Keycloak :
        // - /auth/realms/* -> /realms/* sur Keycloak
        // - /auth/resources/* -> /resources/* sur Keycloak (static assets)
        // On n'intercepte PAS /auth/callback (callback de l'app) pour laisser la SPA gérer cette route.
        '^/auth/realms': {
          target: authTarget,
          changeOrigin: true,
          secure: false,
          // enlever le préfixe /auth pour que Keycloak reçoive /realms/...
          rewrite: (path) => path.replace(/^\/auth/, ''),
          configure: (proxy, _options) => {
            proxy.on('error', (err, _req, _res) => {
              console.log('[Vite Proxy][AUTH][REALMS] ❌ Error:', err.message);
            });
            proxy.on('proxyReq', (proxyReq, req, _res) => {
              try {
                const targetHostname = new URL(authTarget).hostname;
                proxyReq.setHeader('host', targetHostname);
                proxyReq.setHeader('x-forwarded-proto', 'https');
                proxyReq.setHeader('x-forwarded-host', targetHostname);
                const targetPort = new URL(authTarget).port;
                if (targetPort) {
                  proxyReq.setHeader('x-forwarded-port', targetPort);
                }
              } catch {
                // ignore
              }
              console.log(`[Vite Proxy][AUTH][REALMS] → ${req.method} ${req.url} => ${authTarget}${req.url}`);
            });
            proxy.on('proxyRes', (proxyRes, req, _res) => {
              console.log(`[Vite Proxy][AUTH][REALMS] ← ${req.method} ${req.url} [${proxyRes.statusCode}]`);
            });
          },
        },
        '^/auth/resources': {
          target: authTarget,
          changeOrigin: true,
          secure: false,
          rewrite: (path) => path.replace(/^\/auth/, ''),
          configure: (proxy, _options) => {
            proxy.on('error', (err, _req, _res) => {
              console.log('[Vite Proxy][AUTH][RES] ❌ Error:', err.message);
            });
            proxy.on('proxyReq', (proxyReq, req, _res) => {
              try {
                const targetHostname = new URL(authTarget).hostname;
                proxyReq.setHeader('host', targetHostname);
                proxyReq.setHeader('x-forwarded-proto', 'https');
                proxyReq.setHeader('x-forwarded-host', targetHostname);
              } catch {
                // ignore
              }
              console.log(`[Vite Proxy][AUTH][RES] → ${req.method} ${req.url} => ${authTarget}${req.url}`);
            });
            proxy.on('proxyRes', (proxyRes, req, _res) => {
              console.log(`[Vite Proxy][AUTH][RES] ← ${req.method} ${req.url} [${proxyRes.statusCode}]`);
            });
          },
        },
        // Proxy global pour /resources (KeycloakStatic). Nécessaire car Keycloak renvoie souvent
        // des URLs absolues commençant par /resources/... et le navigateur réclame ces URLs sur
        // l'origine de l'app (localhost:4200) -> il faut les rediriger vers Keycloak.
        '/resources': {
          target: authTarget,
          changeOrigin: true,
          secure: false,
          // ne pas réécrire : Keycloak attend /resources/...
          rewrite: undefined,
          configure: (proxy, _options) => {
            proxy.on('error', (err, _req, _res) => {
              console.log('[Vite Proxy][RES] ❌ Error:', err.message);
            });
            proxy.on('proxyReq', (proxyReq, req, _res) => {
              try {
                const targetHostname = new URL(authTarget).hostname;
                proxyReq.setHeader('host', targetHostname);
                proxyReq.setHeader('x-forwarded-proto', 'https');
                proxyReq.setHeader('x-forwarded-host', targetHostname);
              } catch {
                // ignore
              }
              console.log(`[Vite Proxy][RES] → ${req.method} ${req.url} => ${authTarget}${req.url}`);
            });
            proxy.on('proxyRes', (proxyRes, req, _res) => {
              console.log(`[Vite Proxy][RES] ← ${req.method} ${req.url} [${proxyRes.statusCode}]`);
            });
          },
        },
      },
    },

    // Build: Nx écrit dans dist/apps/tchalanet-portal
    build: {
      outDir: path.resolve(__dirname, '../../dist/apps/tchalanet-portal'),
      emptyOutDir: true,
    },

    test: {
      name: 'tchalanet-portal',
      watch: false,
      globals: true,
      environment: 'jsdom',
      include: [
        '{src,tests}/**/*.{test,spec}.{js,mjs,cjs,ts,mts,cts,jsx,tsx}',
      ],
      setupFiles: ['src/test-setup.ts'],
      reporters: ['default'],
      coverage: {
        reportsDirectory: '../../coverage/apps/tchalanet-portal',
        provider: 'v8' as const,
      },
    },
  };
});
