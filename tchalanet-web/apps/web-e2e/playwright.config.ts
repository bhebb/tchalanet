import { defineConfig, devices } from '@playwright/test';
import { nxE2EPreset } from '@nx/playwright/preset';
import { workspaceRoot } from '@nx/devkit';

const publicBaseURL = process.env['PUBLIC_BASE_URL'] || 'http://localhost:4301';
const adminBaseURL = process.env['ADMIN_BASE_URL'] || 'http://localhost:4302';
const platformBaseURL = process.env['PLATFORM_BASE_URL'] || 'http://localhost:4303';

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
  /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
  use: {
    /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
    trace: 'on-first-retry',
  },
  /* Run your local dev server before starting the tests */
  webServer: [
    {
      command: 'pnpm exec nx run public-portal:serve --port=4301',
      url: publicBaseURL,
      reuseExistingServer: true,
      cwd: workspaceRoot,
    },
    {
      command: 'pnpm exec nx run admin-portal:serve --port=4302',
      url: adminBaseURL,
      reuseExistingServer: true,
      cwd: workspaceRoot,
    },
    {
      command: 'pnpm exec nx run platform-portal:serve --port=4303',
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
