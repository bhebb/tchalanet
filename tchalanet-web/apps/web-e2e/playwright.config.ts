import { defineConfig, devices } from '@playwright/test';
import { nxE2EPreset } from '@nx/playwright/preset';
import { workspaceRoot } from '@nx/devkit';

const publicBaseURL = process.env['PUBLIC_BASE_URL'] || 'http://localhost:4301';
const adminBaseURL = process.env['ADMIN_BASE_URL'] || 'http://localhost:4302';
const platformBaseURL = process.env['PLATFORM_BASE_URL'] || 'http://localhost:4303';

// CI runs against already-deployed portals (staging): skip the local dev servers
// and point the base URLs at the deployed origins via env. Local runs (unset)
// keep serving the three portals with `nx serve`.
const externalTargets = process.env['WEB_E2E_EXTERNAL'] === '1';

// Emulator run (variant A): serve the portals with their `emulator` configuration
// so the Firebase Auth SDK connects to the local emulator (:9099). Otherwise the
// default `serve` (development) targets the real Firebase.
const serveTarget = process.env['WEB_E2E_EMULATOR'] === '1' ? 'serve:emulator' : 'serve';

/**
 * Read environment variables from file.
 * https://github.com/motdotla/dotenv
 */
// require('dotenv').config();

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
  ...nxE2EPreset(__filename, { testDir: './src' }),
  fullyParallel: true,
  // Emulator run only: ensure the auth emulator is up and seed users (no-op
  // unless WEB_E2E_EMULATOR=1).
  globalSetup: './src/support/global-setup.ts',
  /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
  use: {
    /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
    trace: 'on-first-retry',
    ...(process.env['PW_CHROMIUM_PATH']
      ? { launchOptions: { executablePath: process.env['PW_CHROMIUM_PATH'] } }
      : {}),
  },
  /* Run the local dev servers before the tests — unless targeting deployed
   * portals (WEB_E2E_EXTERNAL=1), in which case the base URLs are already live. */
  webServer: externalTargets
    ? undefined
    : [
        {
          command: `pnpm exec nx run public-portal:${serveTarget} --port=4301`,
          url: publicBaseURL,
          reuseExistingServer: true,
          cwd: workspaceRoot,
        },
        {
          command: `pnpm exec nx run admin-portal:${serveTarget} --port=4302`,
          url: adminBaseURL,
          reuseExistingServer: true,
          cwd: workspaceRoot,
        },
        {
          command: `pnpm exec nx run platform-portal:${serveTarget} --port=4303`,
          url: platformBaseURL,
          reuseExistingServer: true,
          cwd: workspaceRoot,
        },
      ],
  projects: [
    {
      name: 'public-portal',
      testMatch: /public-portal\/.*\.spec\.ts/,
      use: { ...devices['Desktop Chrome'], baseURL: publicBaseURL },
    },
    {
      name: 'admin-portal',
      testMatch: /admin-portal\/.*\.spec\.ts/,
      use: { ...devices['Desktop Chrome'], baseURL: adminBaseURL },
    },
    {
      name: 'platform-portal',
      testMatch: /platform-portal\/.*\.spec\.ts/,
      use: { ...devices['Desktop Chrome'], baseURL: platformBaseURL },
    },
  ],
});
